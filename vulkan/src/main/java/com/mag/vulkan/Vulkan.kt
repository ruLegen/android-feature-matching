package com.mag.vulkan

class Vulkan {
    private var mPtr: Long = 0

    init {
        System.loadLibrary("vulkan_interactor")
        mPtr = init()
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        finish(mPtr)
    }
    private external fun init(): Long
    private external fun finish(mPtr: Long)
}