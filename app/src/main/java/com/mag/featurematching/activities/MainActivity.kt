package com.mag.featurematching.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.mag.featurematching.databinding.ActivityMainBinding
import com.mag.featurematching.viewmodels.MainActivityViewModel

class MainActivity : AppCompatActivity() {
    lateinit var binding : ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        var vm = MainActivityViewModel()
        binding.vm = vm
    }
}