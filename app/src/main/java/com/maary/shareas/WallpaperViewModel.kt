package com.maary.shareas

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import ai.onnxruntime.providers.NNAPIFlags
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.hoko.blur.HokoBlur
import com.hoko.blur.task.AsyncBlurTask
import com.maary.shareas.data.ViewerBitmap
import com.maary.shareas.helper.SuperResPerformer
import com.maary.shareas.helper.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.EnumSet

class WallpaperViewModel : ViewModel() {
    init {
        Log.v("WVM", "INIT")
    }

    private var bakBitmap = ViewerBitmap()

    private var bitmapRaw: Bitmap? = null

    private var bitmap: Bitmap? = null
        set(value) {
            Log.v("WVM", "BITMAP SET")
            field = value
            bakBitmap.bitmapHome = value
            bakBitmap.bitmapLock = value
            _viewerState.value = bakBitmap
        }

    private var inEditor = false
        set(value) {
            field = value
            _inEditor.value = value
        }

    val HOME = 1
    val LOCK = 0

    var currentBitmap = HOME
        set(value) {
            field = value
            _currentBitmapState.value = value
        }

    var upscaleToggle = false
        set(value) {
            field = value
            _upscaleToggleState.value = value
        }

    private val _currentBitmapState = MutableStateFlow(currentBitmap)
    val currentBitmapState: StateFlow<Int?> = _currentBitmapState.asStateFlow()
    val currentBitmapStateLiveData: LiveData<Int?> = _currentBitmapState.asLiveData()

    private val _viewerState = MutableStateFlow(ViewerBitmap())
    val viewerState = _viewerState.asStateFlow()
    val viewerStateLiveData = _viewerState.asLiveData()

    private val _inEditor: MutableStateFlow<Boolean> = MutableStateFlow(inEditor)
    val inEditorState: StateFlow<Boolean> = _inEditor.asStateFlow()
    val inEditorLiveData: LiveData<Boolean> = _inEditor.asLiveData()

    private val _upscaleProgressState = MutableStateFlow(0)
    val upscaleProgressState = _upscaleProgressState.asStateFlow()

    private val _upscaleToggleState = MutableStateFlow(upscaleToggle)
    val upscaleToggleState = _upscaleToggleState.asStateFlow()

    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var ortSession: OrtSession


    fun setBitmapRaw(value: Bitmap, context: Context) {
        bitmapRaw = value
        val _bitmap = fitBitmapToScreen(value, context)
        bitmap = _bitmap
    }
    private fun fitBitmapToScreen(value: Bitmap, context: Context): Bitmap {
        val deviceBounds = Util.getDeviceBounds(context)
        val deviceHeight = deviceBounds.y
        val deviceWidth = deviceBounds.x

        //image ratio > device ratio?
        val isVertical = Util.isVertical(deviceHeight, deviceWidth, value)

        //show image to imageview
        val bitmapFullWidth = value.width
        val bitmapFullHeight = value.height
        val desiredWidth: Int
        val desiredHeight: Int

        if (isVertical) {
            desiredWidth = deviceWidth
            val scale = deviceWidth.toFloat() / bitmapFullWidth
            desiredHeight = (scale * bitmapFullHeight).toInt()
        } else {
            desiredHeight = deviceHeight
            val scale = deviceHeight.toFloat() / bitmapFullHeight
            desiredWidth = (scale * bitmapFullWidth).toInt()
        }

        return Bitmap.createScaledBitmap(value, desiredWidth, desiredHeight, true)

    }

    fun getBitmapHome(): Bitmap? {
        return _viewerState.value.bitmapHome
    }

    fun getBitmapLock(): Bitmap? {
        return _viewerState.value.bitmapLock
    }

    // 丢弃所有可能的修改
    fun restoreChanges() {
        bakBitmap.bitmapLock = bitmap
        bakBitmap.bitmapHome = bitmap
        _viewerState.update { currentState ->
            currentState.copy(
                bitmapHome = bitmap,
                bitmapLock = bitmap
            )
        }
    }

    // 丢弃当前的修改
    fun abortEdit() {
        _viewerState.update { current ->
            current.copy(
                bitmapHome = bakBitmap.bitmapHome,
                bitmapLock = bakBitmap.bitmapLock
            )
        }
    }

    fun abortEditHome() {
        _viewerState.update { current ->
            current.copy(
                bitmapHome = bakBitmap.bitmapHome
            )
        }
    }

    fun abortEditLock() {
        _viewerState.update { current ->
            current.copy(
                bitmapLock = bakBitmap.bitmapLock
            )
        }
    }

    fun startEditing() {
        inEditor = true
    }

    fun finishEditing() {
        inEditor = false
    }

    fun getDisplayBitmap(): Bitmap? {
        if (currentBitmap == HOME) {
            return _viewerState.value.bitmapHome
        } else if (currentBitmap == LOCK) {
            return _viewerState.value.bitmapLock
        }
        return bitmap
    }

    fun currentBitmapToggle() {
        if (currentBitmap == HOME) {
            currentBitmap = LOCK
        } else if (currentBitmap == LOCK) {
            currentBitmap = HOME
        }
    }

    fun getFabResource(): Int {
        if (currentBitmap == HOME) {
            return R.drawable.ic_vertical
        }
        if (currentBitmap == LOCK) {
            return R.drawable.ic_lockscreen
        }
        return R.drawable.ic_vertical
    }

    fun editBlur(context: Context, value: Float) {
        HokoBlur.with(context)
            .radius(value.toInt())
            .forceCopy(true)
            .asyncBlur(bakBitmap.bitmapHome, object : AsyncBlurTask.Callback {
                override fun onBlurSuccess(bitmap: Bitmap) {
                    _viewerState.update { current ->
                        current.copy(bitmapHome = bitmap)
                    }
                    HokoBlur.with(context)
                        .radius(value.toInt())
                        .forceCopy(true)
                        .asyncBlur(bakBitmap.bitmapLock, object : AsyncBlurTask.Callback {
                            override fun onBlurSuccess(bitmap: Bitmap) {
                                _viewerState.update { current ->
                                    current.copy(bitmapLock = bitmap)
                                }
                            }

                            override fun onBlurFailed(error: Throwable) {}
                        })
                }

                override fun onBlurFailed(error: Throwable) {}
            })
    }

    fun editBrightness(value: Float) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                val homeA = Util.adjustBrightness(_viewerState.value.bitmapHome, value)
                val lockA = Util.adjustBrightness(_viewerState.value.bitmapLock, value)
                _viewerState.update { current ->
                    current.copy(
                        bitmapHome = homeA,
                        bitmapLock = lockA
                    )
                }
            }
        }
    }

    fun saveEdit() {
        bakBitmap.bitmapHome = _viewerState.value.bitmapHome
        bakBitmap.bitmapLock = _viewerState.value.bitmapLock
    }

    fun getBitmapUri(context: Context, cacheDir: File): Uri? {
        //---Save bitmap to external cache directory---//
        //get cache directory

        val cachePath = File(cacheDir, "my_images/")
        cachePath.mkdirs()

        //create png file
        val file = File(cachePath, "Image_123.png")
        val fileOutputStream: FileOutputStream
        try {
            fileOutputStream = FileOutputStream(file)
            _viewerState.value.bitmapHome?.compress(
                Bitmap.CompressFormat.PNG,
                100,
                fileOutputStream
            )
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            file
        )
    }

    private fun readModel(context: Context, modelID: Int): ByteArray {
        Log.e("WVM", "READ MODEL")
        return context.resources.openRawResource(modelID).readBytes()
    }

    private fun performSuperResolution(ortSession: OrtSession, bitmap: Bitmap): Bitmap? {
        val superResPerformer = SuperResPerformer()
        Log.v("WVM", "STARTED")

        val result = superResPerformer.upscale(bitmapToInputStream(bitmap), ortEnv, ortSession)
        return result.outputBitmap
    }

    private fun bitmapToInputStream(bitmap: Bitmap?): InputStream {
        val outputStream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return ByteArrayInputStream(outputStream.toByteArray())
    }

    private suspend fun process(tileBitmap: Bitmap): Bitmap? {
        // 实现对图片的超分辨率处理
        return viewModelScope.async(Dispatchers.IO) {
            performSuperResolution(ortSession, tileBitmap)
        }.await()
    }

    private fun scaleBitmapTo(originalBitmap: Bitmap, scale: Float): Bitmap {
        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height
        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()
        return Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
    }

    fun upscale(context: Context, modelName: String) {
        Log.v("WVM", modelName)

        var model = R.raw.realesrgan_anime
        var scale = 4
        val modelArray = context.resources.getStringArray(R.array.model_names)
        when (modelName) {
            modelArray[0] -> {
                model = R.raw.realesrgan_x2plus
                scale = 2
            }

            modelArray[1] -> {
                model = R.raw.realesrgan_x4plus
                scale = 4
                scaleBitmapTo(bitmapRaw!!, 0.5f)
            }

            modelArray[2] -> {
                model = R.raw.realesrgan_anime
                scaleBitmapTo(bitmapRaw!!, 0.5f)
            }
        }

        val sessionOptions: OrtSession.SessionOptions = OrtSession.SessionOptions()
        sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())
        sessionOptions.addNnapi(
            EnumSet.of(
                NNAPIFlags.USE_FP16,
                NNAPIFlags.CPU_DISABLED
            )
        )
        ortSession = ortEnv.createSession(readModel(context, model), sessionOptions)

        // 1. 输入是一个 bimap
        val originalWidth = bitmapRaw!!.width
        val originalHeight = bitmapRaw!!.height

        // 2. 将 bitmap 分割为多个 tile，记录 tile 的相对位置
        val tileSize = 256
        val numTilesX = (originalWidth + tileSize - 1) / tileSize
        val numTilesY = (originalHeight + tileSize - 1) / tileSize
        val sub = numTilesX * numTilesY

        // 存储处理后的 tile
        val processedTiles = mutableListOf<Bitmap>()

        val processedTilesDeferred = mutableListOf<Deferred<Bitmap?>>()


        viewModelScope.launch {
            var numFinished = 0
            var progress = 0
            for (y in 0 until numTilesY) {
                for (x in 0 until numTilesX) {

                    if (!_upscaleToggleState.value) break

                    val tileX = x * tileSize
                    val tileY = y * tileSize
                    val tileWidth = minOf(tileSize, originalWidth - tileX)
                    val tileHeight = minOf(tileSize, originalHeight - tileY)

                    val tileBitmap =
                        Bitmap.createBitmap(bitmapRaw!!, tileX, tileY, tileWidth, tileHeight)

                    val deferred = async { process(tileBitmap) }
                    processedTilesDeferred.add(deferred)
                    val processedTile = deferred.await()

                    numFinished++
                    Log.e("WVM", numFinished.toString())
                    if (sub != 0) {
                        progress = (String.format("%.2f", (numFinished.toDouble() / sub.toDouble()))
                            .toDouble() * 100).toInt()
                    }
                    _upscaleProgressState.value = progress

                    if (processedTile != null) {
                        processedTiles.add(processedTile)
                    }

                    System.gc()
                }
            }

            val resultWidth = originalWidth * scale
            val resultHeight = originalHeight * scale
            var resultBitmap = Bitmap.createBitmap(resultWidth, resultHeight, bitmapRaw!!.config)
            val canvas = Canvas(resultBitmap)
            var currentX = 0
            var currentY = 0

            for (processedTileDeferred in processedTilesDeferred) {
                val processedTile = processedTileDeferred.await()
                if (processedTile != null) {
                    canvas.drawBitmap(processedTile, currentX.toFloat(), currentY.toFloat(), null)
                    currentX += processedTile.width
                    if (currentX >= resultWidth) {
                        currentX = 0
                        currentY += processedTile.height
                    }
                }
            }

            resultBitmap = fitBitmapToScreen(resultBitmap, context)

            _viewerState.update { current ->
                current.copy(
                    bitmapHome = resultBitmap,
                    bitmapLock = resultBitmap
                )
            }
        }

    }
}