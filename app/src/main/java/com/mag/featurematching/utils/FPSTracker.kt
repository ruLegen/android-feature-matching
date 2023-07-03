package com.mag.featurematching.utils

import android.os.SystemClock
import kotlin.math.roundToInt

class FPSTracker (val fpsChanged:((fps:Int)->Unit)?) {

    private var lastMillis = SystemClock.elapsedRealtime()

    var lastFPS:Int = 0
        private set

    fun track(){
        val currentMillis = SystemClock.elapsedRealtime()
        val currentFPS = (1000.toFloat() / (currentMillis - lastMillis).toFloat()).roundToInt()
        if (currentFPS != lastFPS) {
            fpsChanged?.invoke(currentFPS)
        }
        lastMillis = currentMillis
        lastFPS = currentFPS
    }
}