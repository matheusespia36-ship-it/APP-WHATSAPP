#include <jni.h>
#include <string>
#include <vector>
#include <node.h>

extern "C" JNIEXPORT jint JNICALL
Java_com_disparador_app_MainActivity_startNodeWithArguments(
        JNIEnv* env,
        jobject /* this */,
        jobjectArray arguments) {

    jsize argument_count = env->GetArrayLength(arguments);
    std::vector<std::string> args;
    std::vector<char*> cargs;

    for (int i = 0; i < argument_count; i++) {
        jstring js = (jstring) env->GetObjectArrayElement(arguments, i);
        const char* cstr = env->GetStringUTFChars(js, nullptr);
        args.push_back(std::string(cstr));
        env->ReleaseStringUTFChars(js, cstr);
    }

    for (size_t i = 0; i < args.size(); i++) {
        cargs.push_back(const_cast<char*>(args[i].c_str()));
    }

    return node::Start(cargs.size(), cargs.data());
}
