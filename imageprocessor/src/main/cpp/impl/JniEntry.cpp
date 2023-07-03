#include <jni.h>
#include <string>
#include "ImageProcessor.h"
#include "BitmapGuard.h"
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_com_mag_imageprocessor_ImageProcessor_createImageProcessorNative(JNIEnv *env, jobject thiz) {
    return  reinterpret_cast<jlong>(new ImageProcessor());
}

JNIEXPORT void JNICALL
Java_com_mag_imageprocessor_ImageProcessor_disposeNative(JNIEnv *env, jobject thiz, jlong handler) {
    auto* processor = reinterpret_cast<ImageProcessor*>(handler);
    delete processor;
}
JNIEXPORT void JNICALL
Java_com_mag_imageprocessor_ImageProcessor_adjustBrightnessNative(JNIEnv *env, jobject thiz,
                                                                  jlong handler,jobject input,
                                                                  jobject output,jfloat scale) {

    auto* processor = reinterpret_cast<ImageProcessor*>(handler);
    if(processor == nullptr)
        return;

    BitmapGuard inputGuard = BitmapGuard(env,input);
    BitmapGuard outputGuard = BitmapGuard(env,output);
    processor->adjustBrightness(inputGuard.get(),outputGuard.get(),inputGuard.width(),inputGuard.height(), inputGuard.vectorSize(),scale);
}



#ifdef __cplusplus
}
#endif

