/**
 * Pikafish JNI Bridge — Native Interface
 *
 * 话唠棋王 B+ 方案：通过 JNI 与 libpikafish.so 通信，使用 UCI 协议。
 * 采用 pipe + fork 模式，将引擎的 stdin/stdout 重定向到管道的读写端。
 *
 * JNI 函数命名规则：Java_com_hualao_qiwang_ai_PikafishEngine_*
 */

#include <jni.h>
#include <string>
#include <unistd.h>
#include <sys/types.h>
#include <android/log.h>

#define LOG_TAG "PikafishJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// 管道文件描述符
static int g_stdin_pipe[2] = {-1, -1};   // 写入 -> 引擎 stdin
static int g_stdout_pipe[2] = {-1, -1};  // 引擎 stdout -> 读取
static pid_t g_engine_pid = -1;
static bool g_initialized = false;

/**
 * 初始化引擎：创建管道，fork 进程
 * 参数: nnuePath - pikafish.nnue 文件路径
 */
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_hualao_qiwang_ai_PikafishEngine_nativeInit(
        JNIEnv *env,
        jobject /* this */,
        jstring nnuePath,
        jstring enginePath) {

    if (g_initialized) {
        LOGD("Engine already initialized");
        return JNI_TRUE;
    }

    const char *nnue = env->GetStringUTFChars(nnuePath, nullptr);
    const char *engine = env->GetStringUTFChars(enginePath, nullptr);

    LOGD("Initializing Pikafish engine: %s", engine);
    LOGD("NNUE file: %s", nnue);

    // 创建管道
    if (pipe(g_stdin_pipe) < 0 || pipe(g_stdout_pipe) < 0) {
        LOGE("Failed to create pipes");
        env->ReleaseStringUTFChars(nnuePath, nnue);
        env->ReleaseStringUTFChars(enginePath, engine);
        return JNI_FALSE;
    }

    // Fork 引擎进程
    pid_t pid = fork();
    if (pid < 0) {
        LOGE("Fork failed");
        env->ReleaseStringUTFChars(nnuePath, nnue);
        env->ReleaseStringUTFChars(enginePath, engine);
        return JNI_FALSE;
    }

    if (pid == 0) {
        // ─── 子进程：运行 Pikafish 引擎 ───

        // 重定向 stdin/stdout 到管道
        dup2(g_stdin_pipe[0], STDIN_FILENO);
        dup2(g_stdout_pipe[1], STDOUT_FILENO);

        // 关闭不需要的管道端
        close(g_stdin_pipe[1]);
        close(g_stdout_pipe[0]);

        // 设置环境变量指定 NNUE 文件
        setenv("PIKAFISH_NNUE", nnue, 1);

        // 启动引擎
        execl(engine, "pikafish", nullptr);

        // execl 失败
        LOGE("Failed to exec pikafish engine");
        _exit(1);
    }

    // ─── 父进程 ───
    g_engine_pid = pid;
    g_initialized = true;

    // 关闭不需要的管道端
    close(g_stdin_pipe[0]);
    close(g_stdout_pipe[1]);

    env->ReleaseStringUTFChars(nnuePath, nnue);
    env->ReleaseStringUTFChars(enginePath, engine);

    LOGD("Pikafish engine started, pid=%d", pid);
    return JNI_TRUE;
}

/**
 * 发送 UCI 命令到引擎
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_hualao_qiwang_ai_PikafishEngine_nativeSend(
        JNIEnv *env,
        jobject /* this */,
        jstring command) {

    if (!g_initialized) {
        LOGE("Engine not initialized");
        return;
    }

    const char *cmd = env->GetStringUTFChars(command, nullptr);
    LOGD("Sending: %s", cmd);

    std::string cmdStr(cmd);
    cmdStr += "\n";
    write(g_stdin_pipe[1], cmdStr.c_str(), cmdStr.length());

    env->ReleaseStringUTFChars(command, cmd);
}

/**
 * 从引擎读取一行响应
 * 返回: 响应字符串，超时返回空字符串
 */
extern "C"
JNIEXPORT jstring JNICALL
Java_com_hualao_qiwang_ai_PikafishEngine_nativeReadLine(
        JNIEnv *env,
        jobject /* this */) {

    if (!g_initialized) {
        LOGE("Engine not initialized");
        return env->NewStringUTF("");
    }

    char buffer[4096];
    int pos = 0;

    while (pos < 4095) {
        char c;
        ssize_t n = read(g_stdout_pipe[0], &c, 1);
        if (n <= 0) {
            break;
        }
        if (c == '\n') {
            break;
        }
        buffer[pos++] = c;
    }
    buffer[pos] = '\0';

    LOGD("Received: %s", buffer);
    return env->NewStringUTF(buffer);
}

/**
 * 销毁引擎：发送 quit 命令，等待进程退出，清理资源
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_hualao_qiwang_ai_PikafishEngine_nativeDestroy(
        JNIEnv *env,
        jobject /* this */) {

    if (!g_initialized) {
        return;
    }

    LOGD("Destroying Pikafish engine");

    // 发送 quit 命令
    const char *quitCmd = "quit\n";
    write(g_stdin_pipe[1], quitCmd, strlen(quitCmd));

    // 关闭管道
    close(g_stdin_pipe[1]);
    close(g_stdout_pipe[0]);

    g_stdin_pipe[0] = g_stdin_pipe[1] = -1;
    g_stdout_pipe[0] = g_stdout_pipe[1] = -1;
    g_engine_pid = -1;
    g_initialized = false;

    LOGD("Pikafish engine destroyed");
}
