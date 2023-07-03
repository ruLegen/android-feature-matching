package com.mag.featurematching.activities

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.mag.featurematching.camera.ManagedCamera
import com.mag.featurematching.databinding.ActivityCameraViewerBinding
import com.mag.featurematching.utils.PermissionHelper
import com.mag.featurematching.viewmodels.CameraViewerViewModel
import timber.log.Timber
import java.lang.ref.WeakReference

class CameraViewerActivity : AppCompatActivity(){

    lateinit var binding: ActivityCameraViewerBinding
    lateinit var permissionHelper : PermissionHelper



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


        binding = ActivityCameraViewerBinding.inflate(layoutInflater)
        var viewmodel = ViewModelProvider(this).get(CameraViewerViewModel::class.java);
        viewmodel.onCurrentCameraChanged = this::onCurrentCameraChanged
        binding.vm = viewmodel
        lifecycle.addObserver(viewmodel)
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
            return
        }


        binding.vm?.initCameras()


        //camera1 = ManagedCamera(camera1Id,"${camera1Id}-thread",binding.viewFinderRight, cameraStateListener, this::onBitmapResult )
    }
    private fun onCurrentCameraChanged(cameraId: String) {
        if(cameraId.isEmpty())
            return
        val camera = ManagedCamera(cameraId,"${cameraId}-thread",binding.viewFinderLeft);
        binding.vm?.cameraCreated(camera)
    }


    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper.onRequestPermissionsResult(requestCode,permissions,grantResults)
    }


}

