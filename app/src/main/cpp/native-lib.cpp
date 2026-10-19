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

extern "C" JNIEXPORT jint JNICALL
Java_com_disparador_app_node_NodeBridge_nativeStart(JNIEnv* env, jobject, jobjectArray arguments) {
    // Obtém o número de argumentos passados pelo Kotlin
    jsize argc = env->GetArrayLength(arguments);
    
    // Converte os argumentos de Java String Array para char** de C
    char** argv = (char**)calloc(argc, sizeof(char*));
    for (int i = 0; i < argc; i++) {
        jstring s = (jstring)env->GetObjectArrayElement(arguments, i);
        const char* c = env->GetStringUTFChars(s, 0);
        argv[i] = strdup(c);
        env->ReleaseStringUTFChars(s, c);
    }
    
    LOGI("A iniciar o motor Node.js embutido...");
    
    // Executa o Node.js enviando os argumentos (node, bootstrap.js, dataDir, tmpDir)
    return jint(node::Start(argc, argv));
}
