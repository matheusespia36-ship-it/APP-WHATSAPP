    setvbuf(stderr, nullptr, _IONBF, 0);
    if (pipe(pfd) != 0) return;
    dup2(pfd[1], STDOUT_FILENO);
    dup2(pfd[1], STDERR_FILENO);
    if (pthread_create(&log_thread, nullptr, log_thread_fn, nullptr) == 0) {
        pthread_detach(log_thread);
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_disparador_app_node_NodeBridge_nativeStart(JNIEnv *env, jobject /* this */, jobjectArray arguments) {
    redirect_stdio_to_logcat();

    jsize argc = env->GetArrayLength(arguments);

    // O Node/libuv exigem que os argumentos fiquem contíguos na memória.
    size_t total = 0;
    for (jsize i = 0; i < argc; i++) {
        jstring s = (jstring) env->GetObjectArrayElement(arguments, i);
        const char *c = env->GetStringUTFChars(s, nullptr);
        total += strlen(c) + 1;
        env->ReleaseStringUTFChars(s, c);
        env->DeleteLocalRef(s);
    }

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
