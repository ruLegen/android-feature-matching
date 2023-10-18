package com.mag.featurematching.viewmodels

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Size
import android.view.View
import android.widget.PopupMenu
import androidx.databinding.Bindable
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mag.featurematching.BR
import com.mag.featurematching.camera.ManagedCamera
import com.mag.featurematching.interfaces.CameraState
import com.mag.featurematching.interfaces.ManagedCameraStatus
import com.mag.featurematching.utils.ObservableViewModel
import com.mag.imageprocessor.CornerDetectionResult
import java.io.File


class CameraViewerViewModel(application: Application) : ObservableViewModel(application), DefaultLifecycleObserver {
    private var currentCameraId: String = ""
    private var camera: ManagedCamera? = null

    var cameraListId:Array<String>? = null
    var onCurrentCameraChanged : ((cameraId:String)->Unit)? = null

    @get:Bindable
    var threshold: Int = 10
    set(value) {
        field = value
        updateCameraThreshold(value)
        notifyChange(BR.threshold)
    }


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
    @get:Bindable
    var processedBitmap: Bitmap? = null
    set(value) {
        field = value
        notifyChange(BR.processedBitmap)
    }

    private val cameraStateListener = object : ManagedCameraStatus {
        override fun cameraFPSchanged(camera: ManagedCamera, fps: Int) {
            inputFPS = fps;
        }

        override fun processFPSchanged(camera: ManagedCamera, fps: Int) {
            outputFPS = fps;
        }

        override fun cameraStateChanged(camera: ManagedCamera, state: CameraState) {

        }

        override fun cameraSavedPhoto(camera: ManagedCamera, filePath: File) {
            TODO("Not yet implemented")
        }

    }

    fun onBitmapResult(result: CornerDetectionResult) {
        processedBitmap = result.bitmap
        cornerCount = result.cornerCount
        outSize = Size(result.bitmap.width,result.bitmap.height)
    }

    init {
    }

    override fun onResume(owner: LifecycleOwner) {
        camera?.initCamera()
    }
    override fun onPause(owner: LifecycleOwner) {
        camera?.releaseResources()
    }

    fun initCameras() {
        val manager = getApplication<Application>().getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val defaultCamera = if(2 <= manager.cameraIdList.size-1) 2 else manager.cameraIdList.size-1
        cameraListId = manager.cameraIdList
        val cameraId = manager.cameraIdList[defaultCamera]
        onCurrentCameraChanged?.invoke(cameraId)
    }

    fun cameraCreated(camera0: ManagedCamera){
        camera?.isPreviewing = false
        camera?.releaseResources()

        camera = camera0;
        camera!!.listener = cameraStateListener
        camera!!.bitmapListener = this::onBitmapResult
        camera!!.isPreviewing = true
        camera!!.initCamera()
        camera!!.updatePreviewStatus()
        currentCameraId = camera!!.systemId
    }
    fun onChangeCameraClicked(view:View){
        cameraListId ?: return
        camera?.vulkanTest();
        return
        val manager = getApplication<Application>().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val menu = PopupMenu(getApplication(), view)
        menu.menu.apply {
            for (cameraIndex in cameraListId!!.indices){
                val cameraId = cameraListId!![cameraIndex]
                try {
                    val cameraCharacteristics = manager.getCameraCharacteristics(cameraId)
                    val side = if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
                        "Front" else "Back"

                    val cameraTitle = "$side $cameraId"
                    val isCurrent = cameraId == currentCameraId
                    add(0,cameraIndex,cameraIndex,"${if(isCurrent) "[+]" else ""} ${cameraTitle}")

                }catch (_:Exception){

                }
            }
        }

        menu.setOnMenuItemClickListener { item ->
            var cameraId = cameraListId?.get(item.itemId)
            onCurrentCameraChanged?.invoke(cameraId?:"")
            false
        }
        menu.show()
    }
    private fun updateCameraThreshold(value: Int) {
        camera?.detectThreshold = value.toUByte()
    }




}