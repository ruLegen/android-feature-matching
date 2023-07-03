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
import com.mag.featurematching.views.AutoFitTextureView
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

 class ManagedCamera(val systemId: String, val threadName: String, val textureView: AutoFitTextureView, val listener: ManagedCameraStatus, val bitmapListener:((b:Bitmap)->Unit)?) {



    // A flag to match the preview playing status of the camera. If initially set to true, then the [ManagedCamera]
    // instance will automatically start the preview when it becomes possible
    var isPreviewing = false

    // Capture the last milliseconds to allow fps calculation for this camera view
    var lastMillis = SystemClock.elapsedRealtime()
    var lastFPS = 0

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var imagePreprocessorThread: DispatchableThread? = null
    /**
     * An [ImageReader] that handles still image capture.
     */
    private var imageReader: ImageReader? = null

    private var imagePreprocessor: BitmapPreprocessor? = null

    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val cameraOpenCloseLock = Semaphore(1)


    /**
     * [CaptureRequest.Builder] for the camera preview
     */
    private lateinit var previewRequestBuilder: CaptureRequest.Builder

    /**
     * [CaptureRequest] generated by [.previewRequestBuilder]
     */
    private lateinit var previewRequest: CaptureRequest


    /**
     * A reference to the opened [CameraDevice].
     */
    private var cameraDevice: CameraDevice? = null


    /**
     * A [CameraCaptureSession] for camera preview.
     */
    private var captureSession: CameraCaptureSession? = null


    /**
     * Orientation of the camera sensor
     */
    private var sensorOrientation = 0

    /**
     * The [android.util.Size] of camera preview.
     */
    private lateinit var previewSize: Size


    /**
     * Whether the current camera device supports Flash or not.
     */
    private var flashSupported = false



    /**
     * The current state of camera state for taking pictures.
     */
    var cameraState : CameraState = CameraState.CAMERASTATE_IDLE
        set(value) { //Use Custom setter to track changes
            field = value
            textureView.post { listener.cameraStateChanged(this@ManagedCamera, value) } // Send on UI Thread
        }

    /////////////////////////////////////////////////// Callback Instance Variables ///////////////////////////////////

    val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(p0: SurfaceTexture, width: Int, height: Int) {
            Timber.i("onSurfaceTextureAvailable")
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, width: Int, height: Int) {
            Timber.i("onSurfaceTextureSizeChanged")
            configureTransform(width, height)
        }

        /**
         * Every time a new frame is drawn on the [SurfaceTexture] this callback is triggered. We use this fact to
         * calculate the FPS by calculating the diff in millis that have passed since the last frame was drawn.
         * Calculations are done in Float and then rounded to Int for debouncing to consumer UI
         */
        override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
            val currentMillis = SystemClock.elapsedRealtime()
            val currentFPS = (1000.toFloat() / (currentMillis - lastMillis).toFloat()).roundToInt()
            if (currentFPS != lastFPS) {
                textureView.post { listener.cameraFPSchanged(this@ManagedCamera, currentFPS) }
            }
            lastMillis = currentMillis
            lastFPS = currentFPS
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
            val processed = ImageProcessor.adjustBrightness(grayScaled,1.2f)
            textureView.post{ bitmapListener?.invoke(processed)}
        }

        //Timber.i("${image.width}x${image.height}#${image.format}")
    }


    /////////////////////////////////// Computed properties /////////////////////////////


    val activity: Activity
        get() = textureView.context as Activity


    /////////////////////////////////// Implementation ////////////////////////////////////

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
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

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
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

                // Check if the flash is supported.
                flashSupported =
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

                // We've found a viable camera and finished setting up member variables,
                // so we don't need to iterate through other available cameras.
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

    /**
     * Determines if the dimensions are swapped given the phone's current rotation.
     *
     * @param displayRotation The current rotation of the display
     *
     * @return true if the dimensions are swapped, false otherwise.
     */
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


    /**
     * Opens the camera specified by [Camera2BasicFragment.cameraId].
     */
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

    /**
     * Closes the current [CameraDevice].
     */
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

    /**
     * Creates a new [CameraCaptureSession] for camera preview.
     */
    private fun createCameraPreviewSession() {
        try {
            val texture = textureView.surfaceTexture
            // We configure the size of default buffer to be the size of camera preview we want.
            texture?.setDefaultBufferSize(previewSize.width, previewSize.height)

            // This is the output Surface we need to start preview.
            val surface = Surface(texture)

            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW
            )
            previewRequestBuilder.apply {
                addTarget(surface)
                addTarget(imageReader?.surface!!)
            }

            // Here, we create a CameraCaptureSession for camera preview.
 //           SessionConfiguration(SessionConfiguration.SESSION_REGULAR, listOf(surface,imageReader?.surface),backgroundThread)
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
                            // Flash is automatically enabled when necessary.
                            setAutoFlash(previewRequestBuilder)

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


    /**
     * Configures the necessary [android.graphics.Matrix] transformation to `textureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `textureView` is fixed.
     *
     * @param viewWidth  The width of `textureView`
     * @param viewHeight The height of `textureView`
     */
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


    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private fun unlockFocus() {
        try {
            // Reset the auto-focus trigger
            previewRequestBuilder.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
            )
            setAutoFlash(previewRequestBuilder)
            captureSession?.capture(
                previewRequestBuilder.build(), captureCallback,
                backgroundHandler
            )
            // After this, the camera will go back to the normal state of preview.
            cameraState = CameraState.CAMERASTATE_PREVIEW
            captureSession?.setRepeatingRequest(
                previewRequest, captureCallback,
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Timber.e(e)
        }

    }


    private fun setAutoFlash(requestBuilder: CaptureRequest.Builder) {
        if (flashSupported) {
            requestBuilder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            )
        }
    }


    //////////////////////////// Public / Exposed functions ////////////////////////////////////

    /**
     * The Consumer should call this method when it wants the [ManagedCamera] to become ready to function
     * If [isPreviewing] is set to true before calling this function, then the camera instance will automatically
     * start the Camera preview in the supplied TextureView as well
     */
    fun initCamera() {
        imagePreprocessorThread = DispatchableThread().apply { start() }

        backgroundThread = HandlerThread(threadName).also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
        Timber.i("Started background thread: ${threadName}")
        textureView.surfaceTextureListener = surfaceTextureListener
        if (textureView.isAvailable) {
            openCamera(textureView.width, textureView.height)
        }
    }

    /**
     * Is called whenever the implementation should be made to match up with the state of [isPreviewing].
     * [cameraState] is also set to IDLE or PREVIEW to match with the implementation, here.
     */
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



    /**
     * The Consumer should call this method when it wants the [ManagedCamera] to release camera resources, and shut down
     * any ongoing previews.
     */
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


    /**
     * Lock the focus as the first step for a still image capture.
     */
    fun lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            previewRequestBuilder.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START
            )
            // Tell #captureCallback to wait for the lock.
            cameraState = CameraState.CAMERASTATE_WAITING_LOCK
            captureSession?.capture(
                previewRequestBuilder.build(), captureCallback,
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Timber.e(e)
        }

    }


    companion object {

        val filenameFormat = SimpleDateFormat("yyyy-MM-dd HH.mm.ss.SSSS")


        /**
         * Conversion from screen rotation to JPEG orientation.
         */
        private val ORIENTATIONS = SparseIntArray()
        private val FRAGMENT_DIALOG = "dialog"

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

        /**
         * Given `choices` of `Size`s supported by a camera, choose the smallest one that
         * is at least as large as the respective texture view size, and that is at most as large as
         * the respective max size, and whose aspect ratio matches with the specified value. If such
         * size doesn't exist, choose the largest one that is at most as large as the respective max
         * size, and whose aspect ratio matches with the specified value.
         *
         * @param choices           The list of sizes that the camera supports for the intended
         *                          output class
         * @param textureViewWidth  The width of the texture view relative to sensor coordinate
         * @param textureViewHeight The height of the texture view relative to sensor coordinate
         * @param maxWidth          The maximum width that can be chosen
         * @param maxHeight         The maximum height that can be chosen
         * @param aspectRatio       The aspect ratio
         * @return The optimal `Size`, or an arbitrary one if none were big enough
         */
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