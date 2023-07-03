package com.mag.featurematching.interfaces

import com.mag.featurematching.camera.ManagedCamera
import java.io.File

enum class CameraState(val state:Int){
//Idle / Not started.
CAMERASTATE_IDLE(-1),
//Showing camera preview.
 CAMERASTATE_PREVIEW(0),
// Camera state: Waiting for the focus to be locked.
 CAMERASTATE_WAITING_LOCK(1),
// Camera state: Waiting for the exposure to be precapture state.
 CAMERASTATE_WAITING_PRECAPTURE(2),
// Camera state: Waiting for the exposure state to be something other than precapture.
 CAMERASTATE_WAITING_NON_PRECAPTURE(3),
// Camera state: Picture was taken.
 CAMERASTATE_PICTURE_TAKEN(4)
}

/**
 * Consumers of [ManagedCamera] need to supply an implementation of this interface to allow the camera instance to
 * communicate back as required
 */
interface ManagedCameraStatus {
    fun cameraFPSchanged(camera: ManagedCamera, fps: Int)
    fun cameraStateChanged(camera: ManagedCamera, state: CameraState)
    fun cameraSavedPhoto(camera: ManagedCamera, filePath:File)
    fun processFPSchanged(camera: ManagedCamera, fps: Int)
}