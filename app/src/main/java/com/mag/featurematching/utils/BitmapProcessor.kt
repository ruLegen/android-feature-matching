package com.mag.featurematching.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.experimental.and

class BitmapPreprocessor(val imageWidth: Int, val imageHeight:Int) {
    private var rgbFrameBitmap = Bitmap.createBitmap(imageWidth,imageHeight,Bitmap.Config.ARGB_8888,)

    fun preprocessImage(image: Image?): Bitmap? {
        if (image == null) {
            return null
        }

        try {
            check(rgbFrameBitmap!!.width ==  image.width, { "Invalid size width" })
            check(rgbFrameBitmap!!.height == image.height, { "Invalid size height" })
        }catch (_:Exception){
            return null
        }

        if (rgbFrameBitmap != null) {
            val bb = image.planes[0].buffer
            rgbFrameBitmap = BitmapFactory.decodeStream(ByteBufferBackedInputStream(bb))
        }

        return rgbFrameBitmap
    }

    private class ByteBufferBackedInputStream(internal var buf: ByteBuffer) : InputStream() {

        @Throws(IOException::class)
        override fun read(): Int {
            return if (!buf.hasRemaining()) {
                -1
            } else (buf.get() and 0xFF.toByte()).toInt()
        }

        @Throws(IOException::class)
        override fun read(bytes: ByteArray, off: Int, len: Int): Int {
            var len = len
            if (!buf.hasRemaining()) {
                return -1
            }

            len = Math.min(len, buf.remaining())
            buf.get(bytes, off, len)
            return len
        }
    }
}