package com.mag.featurematching.viewmodels

import android.util.Log
import android.util.Size
import android.view.View
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.lifecycle.ViewModel
import com.mag.featurematching.BR
import com.mag.featurematching.camera.ManagedCamera
import com.mag.featurematching.utils.ObservableViewModel
import timber.log.Timber

class CameraViewerViewModel : ObservableViewModel() {

    @get:Bindable
    var inputFPS: Int = 0
        set(value) {
            field = value
            notifyChange(BR.inputFPS)
        }

    @get:Bindable
    var outputFPS: Int = 0
        set(value) {
            field = value
            notifyChange(BR.outputFPS)
        }
    @get:Bindable
    var cornerCount: Int = 0
        set(value) {
            field = value
            notifyChange(BR.cornerCount)
        }

    @get:Bindable
    var outSize: Size = Size(0, 0)
        set(value) {
            if (field == value)
                return
            field = value
            notifyChange(BR.outSize)
        }

    fun OnLeftCameraButtonClicked(view: View) {
        Log.d("Awdawd", "Left camera clicked")
    }

    fun OnRightCameraClicked(view: View) {
        Log.d("Awdawd", "Right camera clicked")
    }

    fun cameraInitialized(camera0: ManagedCamera, camera1: ManagedCamera) {

    }
}