/**
 * Pikafish JNI Bridge — In-Process Android Integration
 *
 * 话唠棋王 B+ 方案：通过 pikafish_wrapper 与 Pikafish 引擎进程内通信。
 * 不再使用 fork/exec（Android 不支持），改为线程 + 管道模式。
 *
 * JNI 函数命名规则：Java_com_hualao_qiwang_ai_PikafishEngine_*
 */

#include <jni.h>
#include <string>
#include <android/log.h>

#include "pikafish_wrapper.h"

#define LOG_TAG "PikafishJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool g_initialized = false;

/**
 * 初始化引擎：启动 Pikafish 线程，完成 UCI 握手。
 *
 * @param nnuePath   pikafish.nnue 权重文件完整路径
 * @param enginePath 保留参数（进程内模式不需要二进制路径）
 */
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_hualao_qiwang_ai_PikafishEngine_nativeInit(
        JNIEnv *env,
        jobject /* this */,
        jstring nnuePath) {

    if (g_initialized) {
        LOGD("Engine already initialized");
        return JNI_TRUE;
    }

    const char *nnue = nnuePath != nullptr
        ? env->GetStringUTFChars(nnuePath, nullptr)
        : nullptr;

    LOGD("Initializing Pikafish engine (in-process)");
    LOGD("NNUE file: %s", nnue ? nnue : "(none/embedded)");

    bool success = pikafish_init(nnue ? nnue : "");

    if (nnue) {
        env->ReleaseStringUTFChars(nnuePath, nnue);
    }

    if (success) {
        g_initialized = true;
        LOGD("Pikafish engine initialized successfully");
    } else {
        LOGE("Pikafish engine initialization failed");
    }

    return success ? JNI_TRUE : JNI_FALSE;
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

    pikafish_send(cmd);

    env->ReleaseStringUTFChars(command, cmd);
}

/**
 * 从引擎读取一行响应（阻塞）
 * 返回: 响应字符串，无数据时返回空字符串
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

    char *line = pikafish_read_line();
    if (!line) {
        return env->NewStringUTF("");
    }

    jstring result = env->NewStringUTF(line);
    free(line);
    return result;
}

/**
 * 销毁引擎：发送 quit，等待线程退出，清理资源
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
    pikafish_destroy();

    g_initialized = false;
    LOGD("Pikafish engine destroyed");
}
