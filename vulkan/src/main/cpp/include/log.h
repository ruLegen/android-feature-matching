#ifndef FEATUREMATCHING_LOG_H
#define FEATUREMATCHING_LOG_H

#include <android/log.h>

#define TAG "VULKAN_CPP"

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,    TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,     TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,     TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,    TAG, __VA_ARGS__)

#endif //FEATUREMATCHING_LOG_H
