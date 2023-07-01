package com.mag.featurematching.viewmodels

import android.util.Log
import android.view.View
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.lifecycle.ViewModel
import com.mag.featurematching.BR
import com.mag.featurematching.camera.ManagedCamera
import timber.log.Timber

class CameraViewerViewModel : ViewModel() {

    fun OnLeftCameraButtonClicked(view: View){
        Log.d("Awdawd","Left camera clicked")
    }

    fun OnRightCameraClicked(view: View){
        Log.d("Awdawd","Right camera clicked")
    }

    fun cameraInitialized(camera0: ManagedCamera,camera1: ManagedCamera){

    }
}