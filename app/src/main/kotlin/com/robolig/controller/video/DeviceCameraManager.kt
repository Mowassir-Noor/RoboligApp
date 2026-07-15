package com.robolig.controller.video

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import com.robolig.controller.core.AppLogger
import com.robolig.controller.core.ApplicationScope
import com.robolig.controller.core.LogTag
import com.robolig.controller.core.MainDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceCameraManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val logger: AppLogger,
        @ApplicationScope private val applicationScope: CoroutineScope,
        @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    ) {
        private val _frameBytes = MutableStateFlow<ByteArray?>(null)
        val frameBytes: StateFlow<ByteArray?> = _frameBytes.asStateFlow()

        private val _isBound = MutableStateFlow(false)
        val isBound: StateFlow<Boolean> = _isBound.asStateFlow()

        private var cameraProvider: ProcessCameraProvider? = null
        private var imageAnalysis: ImageAnalysis? = null
        private var boundLifecycleOwner: LifecycleOwner? = null
        private var bindJob: Job? = null
        private var analyzerExecutor: ExecutorService? = null

        fun start(lifecycleOwner: LifecycleOwner) {
            if (boundLifecycleOwner === lifecycleOwner && _isBound.value) {
                return
            }
            bindJob?.cancel()
            bindJob =
                applicationScope.launch {
                    runCatching {
                        bindInternal(lifecycleOwner)
                    }.onFailure { throwable ->
                        logger.w(LogTag.VIDEO, "Device camera bind failed: ${throwable.message}")
                        unbindInternal()
                    }
                }
        }

        fun stop() {
            bindJob?.cancel()
            bindJob = null
            applicationScope.launch { unbindInternal() }
        }

        private suspend fun bindInternal(lifecycleOwner: LifecycleOwner) {
            withContext(mainDispatcher) {
                val provider = ProcessCameraProvider.getInstance(context).await()
                cameraProvider = provider
                val executor = Executors.newSingleThreadExecutor().also { analyzerExecutor = it }
                val analysis =
                    ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .setResolutionSelector(
                            ResolutionSelector.Builder()
                                .setResolutionStrategy(
                                    ResolutionStrategy(
                                        Size(TARGET_WIDTH_PX, TARGET_HEIGHT_PX),
                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                                    ),
                                ).build(),
                        )
                        .build()
                        .apply {
                            setAnalyzer(
                                executor,
                                JpegFrameAnalyzer { jpeg -> _frameBytes.value = jpeg },
                            )
                        }
                imageAnalysis = analysis
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    analysis,
                )
                boundLifecycleOwner = lifecycleOwner
                _isBound.value = true
                logger.i(LogTag.VIDEO, "Device camera bound")
            }
        }

        private suspend fun unbindInternal() {
            withContext(mainDispatcher) {
                cameraProvider?.unbindAll()
                cameraProvider = null
                imageAnalysis = null
                boundLifecycleOwner = null
                _isBound.value = false
                _frameBytes.value = null
                analyzerExecutor?.shutdown()
                analyzerExecutor = null
                logger.i(LogTag.VIDEO, "Device camera unbound")
            }
        }
    }

private class JpegFrameAnalyzer(
    private val onJpeg: (ByteArray) -> Unit,
) : ImageAnalysis.Analyzer {
    private var frameCount = 0

    override fun analyze(image: ImageProxy) {
        try {
            val jpeg = image.toJpegByteArray()
            if (jpeg != null) {
                frameCount++
                onJpeg(jpeg)
            }
        } finally {
            image.close()
        }
    }
}

private fun ImageProxy.toJpegByteArray(): ByteArray? {
    val imageFormat = format
    if (imageFormat !in setOf(ImageFormat.YUV_420_888, ImageFormat.YUV_422_888, ImageFormat.YUV_444_888)) {
        return null
    }
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)
    val yuv = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuv.compressToJpeg(Rect(0, 0, width, height), JPEG_QUALITY, out)
    return out.toByteArray()
}

private const val TARGET_WIDTH_PX = 640
private const val TARGET_HEIGHT_PX = 480
private const val JPEG_QUALITY = 60
