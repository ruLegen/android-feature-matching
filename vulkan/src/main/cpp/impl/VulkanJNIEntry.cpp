#include <jni.h>
#include "VulkanInteractor.h"
#include "android/asset_manager.h"
#include "android/asset_manager_jni.h"

extern "C"
JNIEXPORT void JNICALL
Java_com_mag_vulkan_Vulkan_finish(JNIEnv *env, jobject thiz, jlong m_ptr) {
    VulkanInteractor* interactor = reinterpret_cast<VulkanInteractor*>(m_ptr);
    delete interactor;
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_mag_vulkan_Vulkan_init(JNIEnv *env, jobject thiz) {
    auto ptr = new VulkanInteractor();

    ptr->init();
    return reinterpret_cast<jlong>(ptr);
}