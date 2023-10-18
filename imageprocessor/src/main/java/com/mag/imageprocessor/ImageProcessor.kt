package com.mag.imageprocessor

import android.graphics.Bitmap
import android.util.Log
import android.util.Log.DEBUG

object ImageProcessor {
    private var handler: Long = 0
    // Used to load the 'imageprocessor' library on application startup.
    init {
        System.loadLibrary("imageprocessor")
        handler = createImageProcessorNative()
    }

    fun detectCorners(input:Bitmap, threshold:UByte):CornerDetectionResult{
        val output = createCompatibleBitmap(input);
        val cornerCount = detectCornersNative(handler,input,output,threshold.toByte())
        return CornerDetectionResult(output,cornerCount);
    }


    private fun createCompatibleBitmap(inputBitmap: Bitmap) =
        Bitmap.createBitmap(inputBitmap.width, inputBitmap.height, inputBitmap.config)

    private  external fun createImageProcessorNative() : Long
    private  external fun disposeNative(handler:Long)
    private external fun detectCornersNative(handler:Long,input: Bitmap, output: Bitmap, scale: Byte) :Int
}