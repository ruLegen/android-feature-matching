package com.mag.featurematching.camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import androidx.core.content.ContextCompat
import com.google.android.renderscript.Toolkit
import com.mag.featurematching.interfaces.*
import com.mag.featurematching.utils.CompareSizesByArea
import com.mag.featurematching.utils.BitmapPreprocessor
import com.mag.featurematching.utils.DispatchableThread
import com.mag.featurematching.utils.FPSTracker
import com.mag.featurematching.views.AutoFitTextureView
import com.mag.imageprocessor.CornerDetectionResult
import com.mag.imageprocessor.ImageProcessor
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * @param systemId holds the cameraId that maps this camera instance to the system's camera device number.
 * @param threadName Name of the thread that this camera object will use
 * @param textureView An [AutoFitTextureView] for camera preview.
 * @param listener Listener to observe notifications from camera state and FPS changeΩs
 */

 class ManagedCamera(val systemId: String, val threadName: String, val textureView: AutoFitTextureView) {

    var isInited: Boolean = false
        private set

    var listener: ManagedCameraStatus? = null
    var bitmapListener:((b:CornerDetectionResult)->Unit)? = null

    var detectThreshold: UByte = 10u

    var isPreviewing = false

    val inputStreamFPSTracker: FPSTracker = FPSTracker(this::OnInputFPSChanged)
    val outputStreamFPSTracker: FPSTracker = FPSTracker(this::OnOutputFPSChanged)



    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var imagePreprocessorThread: DispatchableThread? = null

    private var imageReader: ImageReader? = null
    private var imagePreprocessor: BitmapPreprocessor? = null

    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val cameraOpenCloseLock = Semaphore(1)

    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private lateinit var previewRequest: CaptureRequest


    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var sensorOrientation = 0
    private lateinit var previewSize: Size


    private fun OnInputFPSChanged(fps:Int) {
        textureView.post { listener?.cameraFPSchanged(this@ManagedCamera, fps) }
    }
    private fun OnOutputFPSChanged(fps:Int) {
        textureView.post { listener?.processFPSchanged(this@ManagedCamera, fps) }
    }

    /**
     * This a callback object for the [ImageReader]. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {
        val image = it.acquireLatestImage() ?: return@OnImageAvailableListener

        val bitmap = imagePreprocessor?.preprocessImage(image)
        image.close()
        bitmap?: return@OnImageAvailableListener

        imagePreprocessorThread?.dispatch {
            val grayScaled = Toolkit.colorMatrix(bitmap, Toolkit.greyScaleColorMatrix)
            val processed = ImageProcessor.detectCorners(grayScaled, detectThreshold)
            textureView.post{ bitmapListener?.invoke(processed)}
            outputStreamFPSTracker.track()
        }
    }
    /**
     * The current state of camera state for taking pictures.
     */
    var cameraState : CameraState = CameraState.CAMERASTATE_IDLE
        set(value) { //Use Custom setter to track changes
            field = value
            textureView.post { listener?.cameraStateChanged(this@ManagedCamera, value) } // Send on UI Thread
        }


    val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(p0: SurfaceTexture, width: Int, height: Int) {
            Timber.i("onSurfaceTextureAvailable")
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, width: Int, height: Int) {
            Timber.i("onSurfaceTextureSizeChanged")
            configureTransform(width, height)
        }

        override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
           inputStreamFPSTracker.track()
        }

        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
            Timber.i("onSurfaceTextureDestroyed")
            return true
        }
    }

    /**
     * [CameraDevice.StateCallback] is called when [CameraDevice] changes its state.
     */
    private val cameraStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(device: CameraDevice) {
            Timber.i("Opened")
            cameraOpenCloseLock.release()
            cameraDevice = device
            createCameraPreviewSession()
        }

        override fun onClosed(camera: CameraDevice) {
            super.onClosed(camera)
            Timber.i("Closed")
        }

        override fun onDisconnected(device: CameraDevice) {
            Timber.i("Disconnected")
            cameraOpenCloseLock.release()
            cameraDevice?.close()
            cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            Timber.i("Error: $error for Camera: $cameraDevice")
            onDisconnected(cameraDevice)
        }

    }

    /**
     * A [CameraCaptureSession.CaptureCallback] that handles events related to JPEG capture.
     */
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        private fun process(result: CaptureResult) {
            when (cameraState) {
                CameraState.CAMERASTATE_PREVIEW -> Unit // Do nothing when the camera preview is working normally.
                CameraState.CAMERASTATE_WAITING_LOCK -> Unit
                CameraState.CAMERASTATE_WAITING_PRECAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null ||
                        aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED
                    ) {
                        cameraState = CameraState.CAMERASTATE_WAITING_NON_PRECAPTURE
                    }
                }
                CameraState.CAMERASTATE_WAITING_NON_PRECAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        cameraState = CameraState.CAMERASTATE_PICTURE_TAKEN
                    }
                }
                else -> {}
            }
        }


        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            process(partialResult)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            process(result)
        }

    }
    val activity: Activity
        get() = textureView.context as Activity

    private fun setUpCameraOutputs(width: Int, height: Int) {
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                val map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                ) ?: continue

                // For still image captures, we use the largest available size.
                val largest = Collections.max(
                    Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)),
                    CompareSizesByArea()
                )


                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                val displayRotation = activity.windowManager.defaultDisplay.rotation

                sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
                val swappedDimensions = areDimensionsSwapped(displayRotation)

                val displaySize = Point()
                activity.windowManager.defaultDisplay.getSize(displaySize)
                val rotatedPreviewWidth = if (swappedDimensions) height else width
                val rotatedPreviewHeight = if (swappedDimensions) width else height
                var maxPreviewWidth = if (swappedDimensions) displaySize.y else displaySize.x
                var maxPreviewHeight = if (swappedDimensions) displaySize.x else displaySize.y

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) maxPreviewWidth = MAX_PREVIEW_WIDTH
                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) maxPreviewHeight = MAX_PREVIEW_HEIGHT

                previewSize = chooseOptimalSize(
                    map.getOutputSizes(SurfaceTexture::class.java),
                    rotatedPreviewWidth, rotatedPreviewHeight,
                    maxPreviewWidth, maxPreviewHeight,
                    largest
                )
                imagePreprocessor = BitmapPreprocessor(previewSize.width,previewSize.height)
                imageReader = ImageReader.newInstance(previewSize.width, previewSize.height,ImageFormat.JPEG, 2).apply {
                    setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
                }
                // We are *always* in landscape orientation.
                textureView.setAspectRatio(previewSize.width, previewSize.height)

                return
            }
        } catch (e: CameraAccessException) {
            Timber.e(e)
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            Timber.e(e)
        }

    }

    private fun areDimensionsSwapped(displayRotation: Int): Boolean {
        var swappedDimensions = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true
                }
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true
                }
            }
            else -> {
                Timber.e("Display rotation is invalid: $displayRotation")
            }
        }
        return swappedDimensions
    }

    private fun openCamera(width: Int, height: Int) {
        val permission = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            Timber.e("We don't have camera permission yet, cannot proceed!")
            return
        }
        setUpCameraOutputs(width, height)
        configureTransform(width, height)
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            // Wait for camera to open - 2.5 seconds is sufficient
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            manager.openCamera(systemId, cameraStateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Timber.e(e.toString())
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }

    }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }
    private fun createCameraPreviewSession() {
        try {
            val texture = textureView.surfaceTexture
            texture?.setDefaultBufferSize(previewSize.width, previewSize.height)

            val surface = Surface(texture)

            previewRequestBuilder = cameraDevice!!.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW
            )
            previewRequestBuilder.apply {
                addTarget(surface)
                addTarget(imageReader?.surface!!)
            }

            // Here, we create a CameraCaptureSession for camera preview.
            cameraDevice?.createCaptureSession(
                Arrays.asList(surface, imageReader?.surface),
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        // The camera is already closed
                        if (cameraDevice == null) return

                        // When the session is ready, we start displaying the preview.
                        captureSession = cameraCaptureSession
                        try {
                            // Auto focus should be continuous for camera preview.
                            previewRequestBuilder.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )

                            // Finally, we start displaying the camera preview.
                            previewRequest = previewRequestBuilder.build()
                            updatePreviewStatus()
                        } catch (e: CameraAccessException) {
                            Timber.e(e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Timber.e("Failed to configure preview session!")
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            Timber.e(e)
        }

    }


    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        activity ?: return
        val rotation = activity.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            val scale = Math.max(
                viewHeight.toFloat() / previewSize.height,
                viewWidth.toFloat() / previewSize.width
            )
            with(matrix) {
                setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        textureView.setTransform(matrix)

    }

    fun initCamera() {
        if(isInited) return

        imagePreprocessorThread = DispatchableThread().apply { start() }

        backgroundThread = HandlerThread(threadName).also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
        Timber.i("Started background thread: ${threadName}")
        textureView.surfaceTextureListener = surfaceTextureListener
        if (textureView.isAvailable) {
            openCamera(textureView.width, textureView.height)
        }
        isInited = true
    }

    fun updatePreviewStatus() {
        if (isPreviewing) {
            captureSession?.setRepeatingRequest(
                previewRequest,
                captureCallback, backgroundHandler
            )
            cameraState = CameraState.CAMERASTATE_PREVIEW
        } else {
            captureSession?.stopRepeating()
            cameraState = CameraState.CAMERASTATE_IDLE
        }
    }

    fun releaseResources() {
        closeCamera()
        backgroundThread?.quitSafely()
        imagePreprocessorThread?.close()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
            imagePreprocessorThread = null

            Timber.i("Released background thread: ${threadName}")
        } catch (e: InterruptedException) {
            Timber.e(e)
        }
    }


    companion object {
        /**
         * Conversion from screen rotation to JPEG orientation.
         */
        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }

        /**
         * Max preview width that is guaranteed by Camera2 API
         */
        private val MAX_PREVIEW_WIDTH = 1920
        /**
         * Max preview height that is guaranteed by Camera2 API
         */
        private val MAX_PREVIEW_HEIGHT = 1080

        @JvmStatic
        private fun chooseOptimalSize(
            choices: Array<Size>,
            textureViewWidth: Int,
            textureViewHeight: Int,
            maxWidth: Int,
            maxHeight: Int,
            aspectRatio: Size
        ): Size {

            // Collect the supported resolutions that are at least as big as the preview Surface
            val bigEnough = ArrayList<Size>()
            // Collect the supported resolutions that are smaller than the preview Surface
            val notBigEnough = ArrayList<Size>()
            val w = aspectRatio.width
            val h = aspectRatio.height
            for (option in choices) {
                if (option.width <= maxWidth && option.height <= maxHeight &&
                    option.height == option.width * h / w
                ) {
                    if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }

            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // largest of those not big enough.
            if (bigEnough.size > 0) {
                return Collections.min(bigEnough, CompareSizesByArea())
            } else if (notBigEnough.size > 0) {
                return Collections.max(notBigEnough, CompareSizesByArea())
            } else {
                Timber.e("Couldn't find any suitable preview size")
                return choices[0]
            }
        }
    }
}