#include <jni.h>
#include <string>
#include "ImageProcessor.h"
#include "BitmapGuard.h"
#include "VulkanInteractor.h"

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
JNIEXPORT jint JNICALL
Java_com_mag_imageprocessor_ImageProcessor_detectCornersNative(JNIEnv *env, jobject thiz,
                                                                  jlong handler,jobject input,
                                                                  jobject output,jbyte threshold) {

    auto* processor = reinterpret_cast<ImageProcessor*>(handler);
    if(processor == nullptr)
        return 0;

    BitmapGuard inputGuard = BitmapGuard(env,input);
    BitmapGuard outputGuard = BitmapGuard(env,output);
    return processor->detectCorners(inputGuard.get(), outputGuard.get(), inputGuard.width(),
                             inputGuard.height(), inputGuard.vectorSize(), threshold);
}



#ifdef __cplusplus
}
#endif


extern "C"
JNIEXPORT jlong JNICALL
Java_com_mag_imageprocessor_Vulkan_init(JNIEnv *env, jobject thiz) {
    auto ptr = new VulkanInteractor();
    return reinterpret_cast<jlong>(ptr);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_mag_imageprocessor_VulkanKt_finish(JNIEnv *env, jclass clazz, jlong m_ptr) {
    VulkanInteractor* interactor = reinterpret_cast<VulkanInteractor*>(m_ptr);
    delete interactor;

}
extern "C"
JNIEXPORT void JNICALL
Java_com_mag_imageprocessor_Vulkan_testVulkan(JNIEnv *env, jobject thiz,jlong m_ptr) {
    VulkanInteractor* interactor = reinterpret_cast<VulkanInteractor*>(m_ptr);
    interactor->RunCmd();
}