package com.robolig.controller.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.robolig.controller.core.AppLogger
import com.robolig.controller.core.ApplicationScope
import com.robolig.controller.core.DefaultDispatcher
import com.robolig.controller.core.LogTag
import com.robolig.controller.domain.model.CubeDetection
import com.robolig.controller.utils.ControllerPreferences
import com.robolig.controller.video.DeviceCameraManager
import com.robolig.controller.video.VideoStreamManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class CubeDetector
    @Inject
    constructor(
        @ApplicationContext private val context: android.content.Context,
        private val videoStreamManager: VideoStreamManager,
        private val deviceCameraManager: DeviceCameraManager,
        private val controllerPreferences: ControllerPreferences,
        private val logger: AppLogger,
        @ApplicationScope private val applicationScope: CoroutineScope,
        @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    ) {
        private val _detections = MutableStateFlow<List<CubeDetection>>(emptyList())
        val detections: StateFlow<List<CubeDetection>> = _detections.asStateFlow()

        private var collectJob: Job? = null

        init {
            collectJob =
                applicationScope.launch(defaultDispatcher) {
                    combine(
                        controllerPreferences.cubeDetectionEnabled,
                        controllerPreferences.useDeviceCamera,
                        videoStreamManager.cameraState,
                        deviceCameraManager.frameBytes,
                    ) { enabled, useDevice, mjpegState, deviceFrame ->
                        Quad(enabled, useDevice, mjpegState.frameBytes, deviceFrame)
                    }.collectLatest { quad ->
                        if (!quad.enabled) {
                            _detections.value = emptyList()
                            return@collectLatest
                        }
                        val activeFrame = if (quad.useDevice) quad.deviceFrame else quad.mjpegFrame
                        if (activeFrame == null) {
                            _detections.value = emptyList()
                            return@collectLatest
                        }
                        _detections.value = runCatching { detect(activeFrame) }
                            .onSuccess { detections ->
                                if (detections.isNotEmpty()) {
                                    logger.i(LogTag.VIDEO, "Cube detection: ${detections.size} cube(s) found")
                                }
                            }
                            .onFailure { logger.w(LogTag.VIDEO, "Cube detection failed: ${it.message}") }
                            .getOrDefault(emptyList())
                    }
                }
        }

        private fun detect(jpegBytes: ByteArray): List<CubeDetection> {
            val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size) ?: return emptyList()
            val originalWidth = bitmap.width
            val originalHeight = bitmap.height
            if (originalWidth <= 0 || originalHeight <= 0) {
                bitmap.recycle()
                return emptyList()
            }

            val scale =
                min(
                    1f,
                    min(
                        MAX_DETECTION_DIM.toFloat() / originalWidth,
                        MAX_DETECTION_DIM.toFloat() / originalHeight,
                    ),
                )
            val scaledBitmap =
                if (scale < 1f) {
                    val sw = max(1, (originalWidth * scale).toInt())
                    val sh = max(1, (originalHeight * scale).toInt())
                    Bitmap.createScaledBitmap(bitmap, sw, sh, true)
                } else {
                    bitmap
                }

            val rgba = Mat()
            val gray = Mat()
            val blurred = Mat()
            val edges = Mat()
            val hierarchy = Mat()
            val contours = mutableListOf<MatOfPoint>()

            return try {
                Utils.bitmapToMat(scaledBitmap, rgba)
                Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
                Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 1.0)
                Imgproc.Canny(blurred, edges, 50.0, 150.0)
                Imgproc.findContours(
                    edges,
                    contours,
                    hierarchy,
                    Imgproc.RETR_EXTERNAL,
                    Imgproc.CHAIN_APPROX_SIMPLE,
                )

                val detections = mutableListOf<CubeDetection>()
                for (contour in contours) {
                    val area = Imgproc.contourArea(contour)
                    if (area < MIN_CONTOUR_AREA_PX || area > MAX_CONTOUR_AREA_PX) continue

                    val contour2f = MatOfPoint2f(*contour.toArray())
                    val peri = Imgproc.arcLength(contour2f, true)
                    val approx = MatOfPoint2f()
                    Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true)
                    val vertices = approx.toArray()
                    approx.release()
                    contour2f.release()
                    if (vertices.size != 4) continue

                    val rect = Imgproc.boundingRect(contour)
                    if (rect.width <= 0 || rect.height <= 0) continue
                    val aspect = rect.width.toFloat() / rect.height.toFloat()
                    if (aspect < MIN_ASPECT || aspect > MAX_ASPECT) continue

                    val fillRatio = area / (rect.width.toFloat() * rect.height.toFloat())
                    if (fillRatio < MIN_FILL_RATIO) continue

                    val moments = Imgproc.moments(contour)
                    val cx =
                        if (moments.m00 != 0.0) (moments.m10 / moments.m00).toFloat()
                        else (rect.x + rect.width / 2f)
                    val cy =
                        if (moments.m00 != 0.0) (moments.m01 / moments.m00).toFloat()
                        else (rect.y + rect.height / 2f)

                    val invScale = 1f / scale
                    val detection =
                        CubeDetection(
                            x = (rect.x * invScale).toInt(),
                            y = (rect.y * invScale).toInt(),
                            width = (rect.width * invScale).toInt(),
                            height = (rect.height * invScale).toInt(),
                            centroidX = cx * invScale,
                            centroidY = cy * invScale,
                            areaPx = area.toInt(),
                            aspectRatio = aspect,
                        )
                    detections.add(detection)
                    if (detections.size >= MAX_DETECTIONS_PER_FRAME) break
                }
                detections
            } finally {
                hierarchy.release()
                edges.release()
                blurred.release()
                gray.release()
                rgba.release()
                contours.forEach { it.release() }
                if (scaledBitmap !== bitmap) scaledBitmap.recycle()
                bitmap.recycle()
            }
        }

        private data class Quad(
            val enabled: Boolean,
            val useDevice: Boolean,
            val mjpegFrame: ByteArray?,
            val deviceFrame: ByteArray?,
        )
    }

private const val MAX_DETECTION_DIM = 320
private const val MIN_CONTOUR_AREA_PX = 800.0
private const val MAX_CONTOUR_AREA_PX = 200_000.0
private const val MIN_ASPECT = 0.7f
private const val MAX_ASPECT = 1.4f
private const val MIN_FILL_RATIO = 0.65f
private const val MAX_DETECTIONS_PER_FRAME = 5
