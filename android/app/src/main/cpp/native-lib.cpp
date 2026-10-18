#include <jni.h>
#include <string.h>
#include <stdlib.h>

extern "C" {
  int node_start(int argc, char *argv[]);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_disparador_app_MainActivity_startNodeWithArguments(JNIEnv* env, jobject, jobjectArray arguments) {
  jsize argc = env->GetArrayLength(arguments);
  size_t total = 0;
  for (int i = 0; i < argc; i++) {
    jstring s = (jstring)env->GetObjectArrayElement(arguments, i);
    const char* c = env->GetStringUTFChars(s, 0);
    total += strlen(c) + 1;
    env->ReleaseStringUTFChars(s, c);
  }
  char* buf = (char*)calloc(total, sizeof(char));
  char** argv = (char**)calloc(argc, sizeof(char*));
  char* cur = buf;
  for (int i = 0; i < argc; i++) {
    jstring s = (jstring)env->GetObjectArrayElement(arguments, i);
    const char* c = env->GetStringUTFChars(s, 0);
    strncpy(cur, c, strlen(c));
    argv[i] = cur;
    cur += strlen(c) + 1;
    env->ReleaseStringUTFChars(s, c);
  }
  return jint(0);
}
