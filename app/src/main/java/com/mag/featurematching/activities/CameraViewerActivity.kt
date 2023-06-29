package com.mag.featurematching.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mag.featurematching.databinding.ActivityCameraViewerBinding
import com.mag.featurematching.viewmodels.CameraViewerViewModel

class CameraViewerActivity : AppCompatActivity() {
    lateinit var binding: ActivityCameraViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraViewerBinding.inflate(layoutInflater)
        binding.vm = CameraViewerViewModel()

        setContentView(binding.root)
    }
}