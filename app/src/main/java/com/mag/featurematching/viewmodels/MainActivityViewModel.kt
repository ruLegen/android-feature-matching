package com.mag.featurematching.viewmodels

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.Observable
import com.mag.featurematching.BR

class MainActivityViewModel : BaseObservable() {
    @get:Bindable
    var text :String = "awdawd"
        set(value){
            field = value
            notifyPropertyChanged(BR.text)
        }


    @get:Bindable
    var repositoryName : String = ""
        set(value) {
            field = value
            notifyPropertyChanged(BR.repositoryName)
        }
}