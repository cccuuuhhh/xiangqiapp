/**
 * Pikafish Wrapper Implementation
 *
 * Runs Pikafish's UCI engine loop in a dedicated thread with pipe-based I/O.
 * Redirects std::cin/std::cout to pipes for communication with the JNI bridge.
 */

#include "pikafish_wrapper.h"

#include <atomic>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <memory>
#include <mutex>
#include <string>
#include <thread>
#include <unistd.h>

// Pikafish headers
#include "attacks.h"
#include "misc.h"
#include "position.h"
#include "tune.h"
#include "uci.h"

using namespace Stockfish;

// ==================== Globals ====================

static std::unique_ptr<UCIEngine> g_uci   = nullptr;
static std::thread                g_thread;
static std::atomic<bool>          g_running{false};
static std::mutex                 g_mutex;

// Pipe file descriptors
static int g_stdin_pipe[2]  = {-1, -1};  // [0]=read, [1]=write
static int g_stdout_pipe[2] = {-1, -1};  // [0]=read, [1]=write

// Saved original stdio buffers (for restoration)
static std::streambuf* g_cin_buf  = nullptr;
static std::streambuf* g_cout_buf = nullptr;

// Buffered output from engine
static std::string g_output_buffer;
static std::mutex  g_output_mutex;

// ==================== Custom Streambuf for Pipe I/O ====================

/**
 * Input streambuf that reads from a pipe fd.
 */
class PipeInputBuf : public std::streambuf {
   public:
    explicit PipeInputBuf(int fd) : m_fd(fd) {}

   protected:
    int underflow() override {
        if (gptr() < egptr())
            return traits_type::to_int_type(*gptr());

        ssize_t n = read(m_fd, m_buffer, sizeof(m_buffer));
        if (n <= 0)
            return traits_type::eof();

        setg(m_buffer, m_buffer, m_buffer + n);
        return traits_type::to_int_type(*m_buffer);
    }

   private:
    int   m_fd;
    char  m_buffer[4096];
};

/**
 * Output streambuf that writes to a buffer protected by mutex.
 */
class PipeOutputBuf : public std::streambuf {
   public:
    explicit PipeOutputBuf(std::string& buf, std::mutex& mtx)
        : m_buffer(buf), m_mutex(mtx) {}

   protected:
    int overflow(int c) override {
        std::lock_guard<std::mutex> lock(m_mutex);
        if (c != traits_type::eof()) {
            if (c == '\n')
                m_buffer += '\n';
            else
                m_buffer += static_cast<char>(c);
        }
        return c;
    }

    std::streamsize xsputn(const char* s, std::streamsize n) override {
        std::lock_guard<std::mutex> lock(m_mutex);
        m_buffer.append(s, static_cast<size_t>(n));
        return n;
    }

   private:
    std::string& m_buffer;
    std::mutex&  m_mutex;
};

// ==================== Engine Thread ====================

static void engine_thread_func() {
    // Redirect std::cin to read from the input pipe
    PipeInputBuf  in_buf(g_stdin_pipe[0]);
    PipeOutputBuf out_buf(g_output_buffer, g_output_mutex);

    std::cin.rdbuf(&in_buf);
    std::cout.rdbuf(&out_buf);

    // Initialize Pikafish
    Attacks::init();
    Position::init();

    // Create UCI engine with empty CommandLine (argc=0, argv=nullptr)
    char*       argv_dummy[1] = {nullptr};
    CommandLine cli(0, argv_dummy);
    g_uci = std::make_unique<UCIEngine>(std::move(cli));

    // Initialize tuning (required for engine options)
    Tune::init(g_uci->engine_options());

    // Print engine info (same as main.cpp)
    std::cout << engine_info() << std::endl;

    // Run the UCI loop (blocks until "quit" received)
    g_uci->loop();

    // Cleanup
    g_uci.reset();
    g_running = false;
}

// ==================== Public API ====================

bool pikafish_init(const char* nnue_path) {
    if (g_running)
        return true;  // Already initialized

    // Create pipes
    if (pipe(g_stdin_pipe) < 0 || pipe(g_stdout_pipe) < 0) {
        return false;
    }

    // Start engine thread
    g_running = true;
    g_thread  = std::thread(engine_thread_func);

    // Wait for engine info line (indicates engine is ready)
    char* info_line = pikafish_read_line();
    if (info_line) {
        free(info_line);
    }

    // Send UCI handshake
    pikafish_send("uci");

    // Read until uciok
    while (g_running) {
        char* line = pikafish_read_line();
        if (!line) break;
        bool is_uciok = (strcmp(line, "uciok") == 0);
        free(line);
        if (is_uciok) break;
    }

    if (!g_running)
        return false;

    // Set engine options
    pikafish_send("setoption name Threads value 2");
    pikafish_send("setoption name Hash value 128");

    // Set NNUE file path if provided
    if (nnue_path && nnue_path[0] != '\0') {
        char cmd[1024];
        snprintf(cmd, sizeof(cmd), "setoption name EvalFile value %s", nnue_path);
        pikafish_send(cmd);
    }

    // Confirm ready
    pikafish_send("isready");

    // Read until readyok
    while (g_running) {
        char* line = pikafish_read_line();
        if (!line) break;
        bool is_readyok = (strcmp(line, "readyok") == 0);
        free(line);
        if (is_readyok) break;
    }

    return g_running;
}

void pikafish_send(const char* command) {
    if (!g_running || g_stdin_pipe[1] < 0)
        return;

    std::string cmd(command);
    cmd += '\n';
    write(g_stdin_pipe[1], cmd.c_str(), cmd.length());
}

char* pikafish_read_line(void) {
    if (!g_running || g_stdout_pipe[1] < 0)
        return nullptr;

    // Read character by character until newline or EOF
    char  buf[8192];
    int   pos = 0;

    while (pos < 8191) {
        char c;
        ssize_t n = read(g_stdout_pipe[0], &c, 1);
        if (n <= 0) {
            if (pos == 0) return nullptr;
            break;
        }
        if (c == '\n')
            break;
        buf[pos++] = c;
    }
    buf[pos] = '\0';

    return strdup(buf);
}

bool pikafish_is_running(void) {
    return g_running;
}

void pikafish_destroy(void) {
    if (!g_running)
        return;

    pikafish_send("quit");

    // Wait for thread to finish
    if (g_thread.joinable())
        g_thread.join();

    // Close pipes
    if (g_stdin_pipe[0] >= 0)  { close(g_stdin_pipe[0]);  g_stdin_pipe[0]  = -1; }
    if (g_stdin_pipe[1] >= 0)  { close(g_stdin_pipe[1]);  g_stdin_pipe[1]  = -1; }
    if (g_stdout_pipe[0] >= 0) { close(g_stdout_pipe[0]); g_stdout_pipe[0] = -1; }
    if (g_stdout_pipe[1] >= 0) { close(g_stdout_pipe[1]); g_stdout_pipe[1] = -1; }

    g_running = false;
}
