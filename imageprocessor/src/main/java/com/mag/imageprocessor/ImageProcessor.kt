package com.mag.imageprocessor

import android.graphics.Bitmap

object ImageProcessor {
    private var handler: Long = 0
    // Used to load the 'imageprocessor' library on application startup.
    init {
        System.loadLibrary("imageprocessor")
        handler = createImageProcessorNative()
    }

    fun adjustBrightness(input:Bitmap,scale:Float):Bitmap{
        val output = createCompatibleBitmap(input);
        adjustBrightnessNative(handler,input,output,scale)
        return output;
    }



    private fun createCompatibleBitmap(inputBitmap: Bitmap) =
        Bitmap.createBitmap(inputBitmap.width, inputBitmap.height, inputBitmap.config)


    private  external fun createImageProcessorNative() : Long
    private  external fun disposeNative(handler:Long)
    private external fun adjustBrightnessNative(handler:Long,input: Bitmap, output: Bitmap, scale: Float)
}