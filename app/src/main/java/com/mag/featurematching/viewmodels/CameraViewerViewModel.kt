package com.mag.featurematching.viewmodels

import android.util.Log
import android.view.View
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.mag.featurematching.BR
import timber.log.Timber

class CameraViewerViewModel : BaseObservable() {

    @get: Bindable
    var name : String = ""
    set(value) {
        field = value
        notifyPropertyChanged(BR.name)
    }
    @get: Bindable
    var text : String = ""
        set(value) {
            field = value
            notifyPropertyChanged(BR.text)
        }

    fun OnLeftCameraButtonClicked(view: View){
        Log.d("Awdawd","Left camera clicked")
    }

    fun OnRightCameraClicked(view: View){
        Log.d("Awdawd","Right camera clicked")
    }
}