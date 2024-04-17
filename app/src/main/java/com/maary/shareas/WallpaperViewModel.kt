package com.maary.shareas

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import ai.onnxruntime.providers.NNAPIFlags
import android.app.Activity
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
import com.maary.shareas.helper.Result
import com.maary.shareas.helper.SuperResPerformer
import com.maary.shareas.helper.Util
import com.maary.shareas.helper.Util_Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.EnumSet

class WallpaperViewModel: ViewModel() {
    init {
        Log.v("WVM", "INIT")
    }

    private var bakBitmap = ViewerBitmap()

    var bitmapRaw: Bitmap? = null

    var bitmap: Bitmap? = null
        set(value) {
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

    private val _currentBitmapState = MutableStateFlow(currentBitmap)
    val currentBitmapState: StateFlow<Int?> = _currentBitmapState.asStateFlow()
    val currentBitmapStateLiveData: LiveData<Int?> = _currentBitmapState.asLiveData()

    private val _viewerState = MutableStateFlow(ViewerBitmap())
    val viewerState= _viewerState.asStateFlow()
    val viewerStateLiveData= _viewerState.asLiveData()

    private val _inEditor: MutableStateFlow<Boolean> = MutableStateFlow(inEditor)
    val inEditorState: StateFlow<Boolean> = _inEditor.asStateFlow()
    val inEditorLiveData: LiveData<Boolean> = _inEditor.asLiveData()

    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var ortSession: OrtSession

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
        if (currentBitmap == HOME){
            return _viewerState.value.bitmapHome
        } else if (currentBitmap == LOCK) {
            return _viewerState.value.bitmapLock
        }
        return bitmap
    }

    fun currentBitmapToggle() {
        if (currentBitmap == HOME) {
            currentBitmap = LOCK
        }else if (currentBitmap == LOCK) {
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
//        GlobalScope.launch {
//            // 启动第一个命令
//            val result1 = async {
//                HokoBlur.with(context)
//                    .radius(value.toInt())
////            .sampleFactor(1.0f)
//                    .forceCopy(true)
//                    .asyncBlur(bakBitmap.bitmapHome, object : AsyncBlurTask.Callback {
//                        override fun onBlurSuccess(bitmap: Bitmap) {
//                            _viewerState.update { current ->
//                                current.copy(bitmapHome = bitmap)
//                            }
//                        }
//
//                        override fun onBlurFailed(error: Throwable) {}
//                    })
//            }
//
//            // 启动第二个命令
//            val result2 = async {
//                HokoBlur.with(context)
//                    .radius(value.toInt())
////            .sampleFactor(1.0f)
//                    .forceCopy(true)
//                    .asyncBlur(bakBitmap.bitmapLock, object : AsyncBlurTask.Callback {
//                        override fun onBlurSuccess(bitmap: Bitmap) {
//                            _viewerState.update { current ->
//                                current.copy(bitmapLock = bitmap)
//                            }
//                        }
//
//                        override fun onBlurFailed(error: Throwable) {}
//                    })
//            }
//
//            // 等待两个命令执行完毕，并获取结果
//            val resultHome = result1.await()
//            val resultLock = result2.await()
//        }
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
            _viewerState.value.bitmapHome?.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
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

//    fun performUpscale(context: Context) {
//        viewModelScope.launch(Dispatchers.IO) {
//            upscale(context)
//        }
//    }

//    fun upscale(context: Context) {
//        val sessionOptions: OrtSession.SessionOptions = OrtSession.SessionOptions()
//        sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())
//        sessionOptions.addNnapi(EnumSet.of(
//            NNAPIFlags.USE_FP16,
//            NNAPIFlags.CPU_DISABLED))
//        ortSession = ortEnv.createSession(readModel(context), sessionOptions)
//        performSuperResolution(ortSession)
//    }

    private fun readModel(context: Context): ByteArray {
        Log.e("WVM", "READ MODEL")
        val modelID = R.raw.realesrgan_x2plus
        return context.resources.openRawResource(modelID).readBytes()
    }

    fun performSuperResolution(ortSession: OrtSession, bitmap: Bitmap): Bitmap? {
        var superResPerformer = SuperResPerformer()
        Log.v("WVM", "STARTED")

        var result = superResPerformer.upscale(bitmapToInputStream(bitmap), ortEnv, ortSession)
//        _viewerState.update { current ->
//            Log.v("WVM", "FINISHED")
//            current.copy(
//                bitmapHome = result.outputBitmap
//            )
//        }
        return result.outputBitmap
    }

    fun bitmapToInputStream(bitmap: Bitmap?): InputStream {
        val outputStream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return ByteArrayInputStream(outputStream.toByteArray())
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun processBitmap(context: Context){

        val sessionOptions: OrtSession.SessionOptions = OrtSession.SessionOptions()
        sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())
        sessionOptions.addNnapi(EnumSet.of(
            NNAPIFlags.USE_FP16,
            NNAPIFlags.CPU_DISABLED))
        ortSession = ortEnv.createSession(readModel(context), sessionOptions)

//        bitmapRaw = scaleBitmapToHalf(bitmapRaw!!)

        // 1. 输入是一个 bimap
        val originalWidth = bitmapRaw!!.width
        val originalHeight = bitmapRaw!!.height

        // 2. 将 bitmap 分割为多个 tile，记录 tile 的相对位置
        val tileSize = 256
        val numTilesX = (originalWidth + tileSize - 1) / tileSize
        val numTilesY = (originalHeight + tileSize - 1) / tileSize

        // 存储处理后的 tile
        val processedTiles = mutableListOf<Bitmap>()
//
//        var i = 0
//
//        for (y in 0 until numTilesY) {
//            for (x in 0 until numTilesX) {
//                // 计算当前 tile 的位置
//                val tileX = x * tileSize
//                val tileY = y * tileSize
//                val tileWidth = minOf(tileSize, originalWidth - tileX)
//                val tileHeight = minOf(tileSize, originalHeight - tileY)
//
//                // 获取当前 tile
//                val tileBitmap = Bitmap.createBitmap(bitmapRaw!!, tileX, tileY, tileWidth, tileHeight)
//
//                Log.v("WVM", "TILE INPUT")
//                // 3. 将 tile 输入 process() 函数
//                val processedTile = runBlocking { process(tileBitmap) }
//
//                // 将处理后的 tile 存入列表
//                if (processedTile != null) {
//                    processedTiles.add(processedTile)
//                }
//
//                System.gc()
//                i++
//                Log.v("WVM", i.toString())
//
//            }
//        }
//
//        Log.v("WVM", "TILE FINISHED")
//
//
//        // 4. 拼接处理后的 tile
//        val resultWidth = originalWidth * 4
//        val resultHeight = originalHeight * 4
//        val resultBitmap = Bitmap.createBitmap(resultWidth, resultHeight, bitmapRaw!!.config)
//        val canvas = Canvas(resultBitmap)
//        var currentX = 0
//        var currentY = 0
//
//        for (processedTile in processedTiles) {
//            canvas.drawBitmap(processedTile, currentX.toFloat(), currentY.toFloat(), null)
//            currentX += processedTile.width
//            if (currentX >= resultWidth) {
//                currentX = 0
//                currentY += processedTile.height
//            }
//        }

        val processedTilesDeferred = mutableListOf<Deferred<Bitmap?>>()


        GlobalScope.launch {
            var i = 0
            for (y in 0 until numTilesY) {
                for (x in 0 until numTilesX) {
                    val tileX = x * tileSize
                    val tileY = y * tileSize
                    val tileWidth = minOf(tileSize, originalWidth - tileX)
                    val tileHeight = minOf(tileSize, originalHeight - tileY)

                    val tileBitmap =
                        Bitmap.createBitmap(bitmapRaw!!, tileX, tileY, tileWidth, tileHeight)

                    val deferred = async { process(tileBitmap) }
                    processedTilesDeferred.add(deferred)
                    val processedTile = deferred.await()

                    i++
                    Log.e("WVM", i.toString())

                    if (processedTile != null) {
                        processedTiles.add(processedTile)
//                        processedTiles.add(scaleBitmapToHalf(processedTile))
                    }

                    System.gc()
                }
            }

//            val resultWidth = originalWidth * 4
//            val resultHeight = originalHeight * 4
            val resultWidth = originalWidth * 2
            val resultHeight = originalHeight * 2
            val resultBitmap = Bitmap.createBitmap(resultWidth, resultHeight, bitmapRaw!!.config)
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

            Util_Files.saveWallpaper(resultBitmap, context as Activity)

            // 5. 返回处理后的图片
//        return resultBitmap
            _viewerState.update { current ->
                Log.v("WVM", "FINISHED")
                current.copy(
                    bitmapHome = resultBitmap
                )
            }
        }



    }

    suspend fun process(tileBitmap: Bitmap): Bitmap? {
        // 实现对图片的超分辨率处理
        return viewModelScope.async(Dispatchers.IO) {
            performSuperResolution(ortSession, tileBitmap)
        }.await()
    }

    fun scaleBitmapToHalf(originalBitmap: Bitmap): Bitmap {
        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height
        val scaledWidth = originalWidth / 2
        val scaledHeight = originalHeight / 2
        return Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
    }




//    data class Tile(val bitmap: Bitmap, val startX: Int, val startY: Int)
//
//    fun splitBitmap(bitmap: Bitmap): List<Tile> {
//        val tiles = mutableListOf<Tile>()
//
//        // 计算短边长度 * 0.1 作为 tile 大小
////        val shortEdge = minOf(bitmap.width, bitmap.height)
//        val tileSize = 256
//
//        // 计算行列数
//        val rows = bitmap.height / tileSize
//        val cols = bitmap.width / tileSize
//
//        // 分割 bitmap 并记录位置
//        for (row in 0 until rows) {
//            for (col in 0 until cols) {
//                val startX = col * tileSize
//                val startY = row * tileSize
//                val tileBitmap = Bitmap.createBitmap(bitmap, startX, startY, tileSize, tileSize)
//                tiles.add(Tile(tileBitmap, startX, startY))
//            }
//        }
//
//        return tiles
//    }
//
//
//
//    fun mergeTiles(tiles: List<Tile>, originalWidth: Int, originalHeight: Int): Bitmap {
//        val resultBitmap = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888)
//        val canvas = android.graphics.Canvas(resultBitmap)
//
//        // 将处理后的 tile 拼接成完整图片
//        for (tile in tiles) {
//            canvas.drawBitmap(process(tile), tile.startX.toFloat(), tile.startY.toFloat(), null)
//        }
//
//        return resultBitmap
//    }
//
//    fun processBitmap(bitmap: Bitmap): Bitmap {
//        val tiles = splitBitmap(bitmap)
//        val processedTiles = tiles.map { process(it) }
//        return mergeTiles(tiles, bitmap.width, bitmap.height)
//    }

}