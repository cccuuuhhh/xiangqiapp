/**
 * Pikafish Engine Wrapper — In-process UCI engine for Android.
 *
 * Spawns a dedicated thread running Pikafish's UCI::loop(),
 * communicating via pipe-based stdin/stdout redirection.
 *
 * This avoids fork() which is broken/deprecated on Android.
 */

#ifndef PIKAFISH_WRAPPER_H
#define PIKAFISH_WRAPPER_H

#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Initialize the Pikafish engine.
 *
 * @param nnue_path  Full path to the pikafish.nnue weight file.
 * @return           true on success, false on failure.
 */
bool pikafish_init(const char* nnue_path);

/**
 * Send a UCI command to the engine.
 *
 * @param command  The command string (without trailing newline).
 */
void pikafish_send(const char* command);

/**
 * Read one line of output from the engine.
 *
 * This is a blocking call. Returns NULL if the engine has exited.
 *
 * @return  The output line (caller must free with free()), or NULL.
 */
char* pikafish_read_line(void);

/**
 * Check if the engine is currently running and responsive.
 */
bool pikafish_is_running(void);

/**
 * Shut down the engine and free all resources.
 */
void pikafish_destroy(void);

#ifdef __cplusplus
}
#endif

#endif /* PIKAFISH_WRAPPER_H */
