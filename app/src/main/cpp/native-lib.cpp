#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>
#include <android/log.h>
#include "node.h"

#define LOG_TAG "NODEJS-MOBILE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
    setvbuf(stderr, nullptr, _IONBF, 0);
    if (pipe(pfd) != 0) return;
    dup2(pfd[1], STDOUT_FILENO);
    dup2(pfd[1], STDERR_FILENO);
    if (pthread_create(&log_thread, nullptr, log_thread_fn, nullptr) == 0) {
        pthread_detach(log_thread);
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_disparador_app_node_NodeBridge_nativeStart(JNIEnv* env, jobject, jobjectArray arguments) {
    // Obtém o número de argumentos passados pelo Kotlin
Java_com_disparador_app_node_NodeBridge_nativeStart(JNIEnv *env, jobject /* this */, jobjectArray arguments) {
    redirect_stdio_to_logcat();

    jsize argc = env->GetArrayLength(arguments);
    
    // Converte os argumentos de Java String Array para char** de C
    char** argv = (char**)calloc(argc, sizeof(char*));
    for (int i = 0; i < argc; i++) {
        jstring s = (jstring)env->GetObjectArrayElement(arguments, i);
        const char* c = env->GetStringUTFChars(s, 0);
        argv[i] = strdup(c);

    // O Node/libuv exigem que os argumentos fiquem contíguos na memória.
    size_t total = 0;
    for (jsize i = 0; i < argc; i++) {
        jstring s = (jstring) env->GetObjectArrayElement(arguments, i);
        const char *c = env->GetStringUTFChars(s, nullptr);
        total += strlen(c) + 1;
        env->ReleaseStringUTFChars(s, c);
        env->DeleteLocalRef(s);
    }
    
    LOGI("A iniciar o motor Node.js embutido...");
    
    // Executa o Node.js enviando os argumentos (node, bootstrap.js, dataDir, tmpDir)
    return jint(node::Start(argc, argv));

    char *buf = (char *) calloc(total + 1, sizeof(char));
    char **argv = (char **) calloc(argc + 1, sizeof(char *));
    char *cur = buf;
    for (jsize i = 0; i < argc; i++) {
        jstring s = (jstring) env->GetObjectArrayElement(arguments, i);
        const char *c = env->GetStringUTFChars(s, nullptr);
        size_t len = strlen(c);
        memcpy(cur, c, len);
        cur[len] = '\0';
        argv[i] = cur;
        cur += len + 1;
        env->ReleaseStringUTFChars(s, c);
        env->DeleteLocalRef(s);
    }

    // buf e argv NÃO são liberados: o Node usa esses ponteiros enquanto o processo viver.
    return (jint) node::Start((int) argc, argv);
    }
