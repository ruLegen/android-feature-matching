package com.mag.featurematching.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.media.Image
import androidx.core.graphics.get
import androidx.core.graphics.set
import timber.log.Timber

object ImageProcessor {
    fun processImage(image:Bitmap):Bitmap{
        val bitmap = image.copy(Bitmap.Config.ARGB_8888,true)
        for (i in 0 until bitmap.width) {
            for (j in 0 until bitmap.height){
                val clr = bitmap.get(i,j)
                val r = Color.red(clr)/2
                val g = Color.green(clr)/2
                val b = Color.blue(clr)/2

                bitmap.set(i,j,Color.argb(255,r,g,b))
            }
        }
        return  bitmap
    }
}