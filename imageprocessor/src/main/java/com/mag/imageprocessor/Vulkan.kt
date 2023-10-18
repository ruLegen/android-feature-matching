package com.mag.imageprocessor

import android.content.res.AssetManager

class Vulkan() {
    private var mPtr: Long = 0

    init {
        System.loadLibrary("imageprocessor")
        mPtr = init()
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        finish(mPtr)

    }

    fun testVulkan() {
        testVulkan(mPtr)
    }

    private external fun init(): Long
    private external fun testVulkan(mPtr: Long)
    private external fun finish(mPtr: Long)
}