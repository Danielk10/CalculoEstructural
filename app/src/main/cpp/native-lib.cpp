#include <jni.h>
#include <string>
#include "AnalysisModel.hpp"

extern "C" JNIEXPORT jstring JNICALL
Java_com_diamon_civil_ui_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    FEA::AnalysisModel model;
    std::string hello = "CalculiX Bridge Ready. Model Nodes: " + std::to_string(model.nodes.size());
    return env->NewStringUTF(hello.c_str());
}