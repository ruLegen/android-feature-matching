package com.mag.featurematching.activities

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.mag.featurematching.camera.ManagedCamera
import com.mag.featurematching.databinding.ActivityCameraViewerBinding
import com.mag.featurematching.interfaces.CameraState
import com.mag.featurematching.interfaces.ManagedCameraStatus
import com.mag.featurematching.utils.PermissionHelper
import com.mag.featurematching.viewmodels.CameraViewerViewModel
import com.mag.imageprocessor.CornerDetectionResult
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference

class CameraViewerActivity : AppCompatActivity() {

    private lateinit var camera0: ManagedCamera
    private lateinit var camera1: ManagedCamera


    lateinit var binding: ActivityCameraViewerBinding
    lateinit var permissionHelper : PermissionHelper

    var cameraStateListener = object : ManagedCameraStatus {
        override fun cameraFPSchanged(camera: ManagedCamera, fps: Int) {
            binding?.vm?.inputFPS = fps;
        }

        override fun processFPSchanged(camera: ManagedCamera, fps: Int) {
            binding?.vm?.outputFPS = fps;
        }

        override fun cameraStateChanged(camera: ManagedCamera, state: CameraState) {

        }

        override fun cameraSavedPhoto(camera: ManagedCamera, filePath: File) {
            TODO("Not yet implemented")
        }

    }
    override fun onResumeFragments() {
        super.onResumeFragments()
        Timber.i("onResumeFragments")
        camera0?.initCamera()
        camera1?.initCamera()
    }

    override fun onPause() {
        super.onPause()
        Timber.i("onPause Activity")
        camera0?.releaseResources()
        camera1?.releaseResources()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraViewerBinding.inflate(layoutInflater)
        binding.vm = ViewModelProvider(this).get(CameraViewerViewModel::class.java);
        setContentView(binding.root)

        permissionHelper = PermissionHelper(WeakReference(this), arrayOf(Manifest.permission.CAMERA))
        permissionHelper.requestPermissions(this::onPermissionResult)

    }

    fun onPermissionResult(granted:Boolean){
        if(!granted)
            return;
        initCamera()
    }

    private fun initCamera() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        Timber.i("Available cameras: %s", manager.cameraIdList.contentToString())
        if (manager.cameraIdList.isEmpty()) {
            AlertDialog.Builder(this)
                .setMessage("No cameras found")
                .setNeutralButton("Exit") { _, _ ->
                    finish() //Finish the activity because there are no cameras
                }
                .create()
                .show()
        }

        val camera0Id = manager.cameraIdList[2]
        var camera1Id = manager.cameraIdList[0]
        camera0 = ManagedCamera(camera0Id,"${camera0Id}-thread",binding.viewFinderLeft, cameraStateListener, this::onBitmapResult )
        camera1 = ManagedCamera(camera1Id,"${camera1Id}-thread",binding.viewFinderRight, cameraStateListener, this::onBitmapResult )
        camera0?.isPreviewing = true // Default to showing the first camera, it will automatically start when ready
        camera0?.updatePreviewStatus()
    }

    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper.onRequestPermissionsResult(requestCode,permissions,grantResults)
    }
    fun onBitmapResult(result:CornerDetectionResult) {
        binding.imageProcessed.setImageBitmap(result.bitmap)
        val vm = binding.vm!!
        vm.cornerCount = result.cornerCount
        vm.outSize = Size(result.bitmap.width,result.bitmap.height)
    }

}

