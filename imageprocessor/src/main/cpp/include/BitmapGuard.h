#ifndef FEATUREMATCHING_BITMAPGUARD_H
#define FEATUREMATCHING_BITMAPGUARD_H
#include <jni.h>
#include <android/bitmap.h>

class BitmapGuard {
private:
    JNIEnv* env;
    jobject bitmap;
    AndroidBitmapInfo info;
    int bytesPerPixel;
    void* bytes;
    bool valid;

public:
    BitmapGuard(JNIEnv* env, jobject jBitmap);
    ~BitmapGuard();
    uint8_t* get() const;
    int width() const;
    int height() const;
    int vectorSize() const;
};

#endif //FEATUREMATCHING_BITMAPGUARD_H
