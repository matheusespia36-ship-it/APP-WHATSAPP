#include <jni.h>
#include <string>
#include <cstdlib>
#include <pthread.h>
#include <unistd.h>
#include <android/log.h>
#include "node/node.h"

// Redireciona stdout e stderr do Node.js para o Logcat do Android
static int pfd[2];
static pthread_t log_thread;

static void *log_thread_fn(void *arg) {
    ssize_t read_size;
    char buf[1024];
    while ((read_size = read(pfd[0], buf, sizeof(buf) - 1)) > 0) {
        if (buf[read_size - 1] == '\n') --read_size;
        buf[read_size] = 0;
        __android_log_write(ANDROID_LOG_INFO, "NODEJS-MOBILE", buf);
    }
    return nullptr;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_disparador_app_node_NodeBridge_nativeStart(JNIEnv *env, jobject /* this */, jobjectArray args) {
    
    // Configura os pipes de log
    setvbuf(stderr, nullptr, _IONBF, 0);
    if (pipe(pfd) == 0) {
        dup2(pfd[1], STDOUT_FILENO);
        dup2(pfd[1], STDERR_FILENO);
        if (pthread_create(&log_thread, nullptr, log_thread_fn, nullptr) != 0) {
            __android_log_write(ANDROID_LOG_ERROR, "NODEJS-MOBILE", "Falha ao criar thread de log");
        }
    }

    // Processa os argumentos enviados pelo Kotlin
    int argc = env->GetArrayLength(args);
    char **argv = (char **) calloc(argc, sizeof(char *));

    for (int i = 0; i < argc; i++) {
        jstring str = (jstring) env->GetObjectArrayElement(args, i);
        const char *cstr = env->GetStringUTFChars(str, nullptr);
        argv[i] = strdup(cstr);
        env->ReleaseStringUTFChars(str, cstr);
    }

    // Inicia a engine Node.js
    int result = node::Start(argc, argv);

    // Libera a memoria dos argumentos
    for (int i = 0; i < argc; i++) {
        free(argv[i]);
    }
    free(argv);

    return result;
}
