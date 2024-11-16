package com.maary.shareas

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import ai.onnxruntime.providers.NNAPIFlags
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.hoko.blur.HokoBlur
import com.hoko.blur.task.AsyncBlurTask
import com.maary.shareas.data.ViewerBitmap
import com.maary.shareas.helper.SuperResPerformer
import com.maary.shareas.helper.Util
import com.maary.shareas.view.ScrollableImageView
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
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random


class WallpaperViewModel : ViewModel() {
    init {
        Log.v("WVM", "INIT")
    }

    companion object {
        const val HOME = 1
        const val LOCK = 0

        const val TOP = 0
        const val BOTTOM = 1
        const val LEFT = 2
        const val RIGHT = 3
        const val CENTER = 4
    }

    private var bakBitmap = ViewerBitmap()

    private var bitmapRaw: Bitmap? = null

    //    private var bitmapFit: Bitmap? = null
    private var background: Bitmap? = null

    var processHome = true
    var processLock = true

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


    var primary: Int? = null
    private var secondary: Int? = null
    private var tertiary: Int? = null
    private var primaryDark: Int? = null
    private var secondaryDark: Int? = null
    private var tertiaryDark: Int? = null

    private val _currentBitmapState = MutableStateFlow(currentBitmap)
    val currentBitmapState: StateFlow<Int?> = _currentBitmapState.asStateFlow()
    val currentBitmapStateLiveData: LiveData<Int?> = _currentBitmapState.asLiveData()

    private val _viewerState = MutableStateFlow(ViewerBitmap())
    val viewerStateLiveData = _viewerState.asLiveData()

    private val _inEditor: MutableStateFlow<Boolean> = MutableStateFlow(inEditor)
    val inEditorLiveData: LiveData<Boolean> = _inEditor.asLiveData()

    private val _upscaleProgressState = MutableStateFlow(0)
    val upscaleProgressState = _upscaleProgressState.asStateFlow()

    fun getUpscaleProgress(): Int {
        return _upscaleProgressState.value
    }

    fun clearUpscaleProgress() {
        _upscaleProgressState.value = 0
    }

    private val _upscaleToggleState = MutableStateFlow(upscaleToggle)
    val upscaleToggleState = _upscaleToggleState.asStateFlow()

    private val _primaryColorState = MutableStateFlow(Color.TRANSPARENT)
    val primaryColorState = _primaryColorState.asStateFlow()

    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var ortSession: OrtSession

    fun startEditing() {
        inEditor = true
    }

    fun finishEditing() {
        inEditor = false
    }

    fun setBitmapRaw(value: Bitmap, context: Context) {
        bitmapRaw = value
        bitmap = fitBitmapToScreen(value, context)
        val deviceBounds = Util.getDeviceBounds(context)
        background = Bitmap.createBitmap(deviceBounds.x, deviceBounds.y, Bitmap.Config.ARGB_8888)
        val colors = extractColorsFromPalette()
        primary = adjustColorToBlack(colors[0])
        secondary = adjustColorToBlack(colors[1])
        tertiary = adjustColorToBlack(colors[2])
        primaryDark = adjustColorToWhite(colors[0])
        secondaryDark = adjustColorToWhite(colors[1])
        tertiaryDark = adjustColorToWhite(colors[2])
        _primaryColorState.value += 1
        processHome = true
        processLock = true
    }

    fun getBitmapHome(): Bitmap? {
        return _viewerState.value.bitmapHome
    }

    fun getBitmapLock(): Bitmap? {
        return _viewerState.value.bitmapLock
    }

    fun getDisplayBitmap(): Bitmap? {
        return when (currentBitmap) {
            HOME -> _viewerState.value.bitmapHome
            LOCK -> _viewerState.value.bitmapLock
            else -> bitmap
        }
    }

    fun currentBitmapToggle() {
        currentBitmap = if (currentBitmap == HOME) LOCK else HOME
    }

    /**
     * 编辑器功能
     * */
    fun editBlur(context: Context, value: Float) {
        if (processHome) {
            blurBitmap(input = bakBitmap.bitmapHome, value, context) { homeOut ->
                _viewerState.update { it.copy(bitmapHome = homeOut) }
            }
        }
        if (processLock) {
            blurBitmap(input = bakBitmap.bitmapLock, value, context) { bitmap ->
                _viewerState.update { it.copy(bitmapLock = bitmap) }
            }
        }
    }

    fun editBrightness(value: Float) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                val homeA =
                    if (processHome) Util.adjustBrightness(bakBitmap.bitmapHome, value) else null
                val lockA =
                    if (processLock) Util.adjustBrightness(bakBitmap.bitmapLock, value) else null

                updateViewerState(homeA, lockA)
            }
        }
    }

    fun paintColor(context: Context, position: Int, color: Int, scale: Float = 1f) {

        val deviceBounds = Util.getDeviceBounds(context)
        val bHeight = deviceBounds.y
        val bWidth = deviceBounds.x
        val _background = Bitmap.createBitmap(bWidth, bHeight, Bitmap.Config.ARGB_8888)

        performPaintColor(
            foreground = getInputBitmap()!!,
            background = _background,
            context, position, color, scale
        )
    }

    fun paintBlur(position: Int, blur: Int, context: Context, scale: Float = 1f) {
        performPaintBlur(
            foreground = getInputBitmap()!!,
            background = background!!,
            position, blur, context, scale
        )
    }

    fun upscale(context: Context, modelName: String, tileSize: Int = 128, fp16: Boolean = false, cpuDisabled: Boolean = false) {
        performUpscale(input = getDisplayBitmap()!!, modelName, context, tileSize, fp16, cpuDisabled)
    }

    /**
     * 修改内容后的保存和丢弃
     * */
    fun saveEdit() {
        if (processHome && processLock) {
            bakBitmap = _viewerState.value
        } else if (processHome) {
            bakBitmap.bitmapHome = _viewerState.value.bitmapHome
        } else if (processLock) {
            bakBitmap.bitmapLock = _viewerState.value.bitmapLock
        }
    }

    fun abortEdit(option: Int?) {
        val homeBitmap =
            if (option == null || option == HOME) bakBitmap.bitmapHome else null
        val lockBitmap =
            if (option == null || option == LOCK) bakBitmap.bitmapLock else null

        _viewerState.update { current ->
            current.copy(
                bitmapHome = homeBitmap ?: current.bitmapHome,
                bitmapLock = lockBitmap ?: current.bitmapLock
            )
        }
    }

    fun restoreChanges() {
        _viewerState.update { currentState ->
            currentState.copy(
                bitmapHome = bitmap,
                bitmapLock = bitmap
            )
        }
        bakBitmap.bitmapLock = bitmap
        bakBitmap.bitmapHome = bitmap
    }

    /**
     * 功能函数
     * */
    fun extractTopColorsFromBitmap(): List<Int> {

        val _bitmap = Bitmap.createScaledBitmap(getDisplayBitmap()!!, 128, 128, true)
        val colorMap = mutableMapOf<Int, Int>()

        // 遍历图片的每个像素，并统计每种颜色的出现次数
        for (x in 0 until _bitmap.width) {
            for (y in 0 until _bitmap.height) {
                val pixel = _bitmap.getPixel(x, y)
                val colorCount = colorMap.getOrDefault(pixel, 0)
                colorMap[pixel] = colorCount + 1
            }
        }

        // 按颜色出现次数排序，并取前五个颜色
        val sortedColors = colorMap.toList().sortedByDescending { it.second }.take(5)
        return sortedColors.map { it.first }
    }

    fun getPrimaryColor(context: Context): Int {
        return if (isDarkMode(context)) primaryDark!!
        else primary!!
    }

    fun getPrimaryColorAlt(context: Context): Int {
        return if (!isDarkMode(context)) primaryDark!!
        else primary!!
    }

    fun getSecondaryColor(context: Context): Int {
        return if (isDarkMode(context)) secondaryDark!!
        else secondary!!
    }

    fun getTertiaryColor(context: Context): Int {
        return if (isDarkMode(context)) tertiaryDark!!
        else tertiary!!
    }

    fun getTertiaryColorAlt(context: Context): Int {
        return if (!isDarkMode(context)) tertiaryDark!!
        else tertiary!!
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

    fun isAlignmentNeeded(context: Context): Boolean {
        val deviceBounds = Util.getDeviceBounds(context)
        val deviceHeight = deviceBounds.y
        val deviceWidth = deviceBounds.x
        return !(bitmap!!.width == deviceWidth && bitmap!!.height == deviceHeight)
    }

    fun getAlignmentIconResource(context: Context): Int {
        val deviceBounds = Util.getDeviceBounds(context)
        val deviceHeight = deviceBounds.y
        val deviceWidth = deviceBounds.x
        return if (deviceWidth < bitmap!!.width) {
            R.drawable.ic_center_horizontal
        } else if (deviceHeight < bitmap!!.height) {
            R.drawable.ic_center_vertical
        } else {
            R.drawable.ic_center_horizontal
        }
    }

    fun getCenterAlignParam(context: Context): Triple<Int, Int, Int>?{
        val deviceBounds = Util.getDeviceBounds(context)
        val deviceHeight = deviceBounds.y
        val deviceWidth = deviceBounds.x
        if (deviceWidth < bitmap!!.width) {
            return Triple(
                ScrollableImageView.HORIZONTAL,
                (bitmap!!.width - deviceWidth)/2,
                0)
        }
        if (deviceHeight < bitmap!!.height) {
            return Triple(
                ScrollableImageView.VERTICAL,
                0,
                (bitmap!!.height - deviceHeight)/2)
        }
        return null
    }

    private fun getInputBitmap(): Bitmap? {
        return when (currentBitmap) {
            HOME -> bakBitmap.bitmapHome
            LOCK -> bakBitmap.bitmapLock
            else -> bitmap
        }
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

    private fun fitBitmapToScreenAlt(value: Bitmap, context: Context): Bitmap {
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
            desiredHeight = deviceHeight
            val scale = deviceHeight.toFloat() / bitmapFullHeight
            desiredWidth = (scale * bitmapFullWidth).toInt()
        } else {
            desiredWidth = deviceWidth
            val scale = deviceWidth.toFloat() / bitmapFullWidth
            desiredHeight = (scale * bitmapFullHeight).toInt()
        }

        return Bitmap.createScaledBitmap(value, desiredWidth, desiredHeight, true)
    }

    private fun adjustColorToBlack(value: Int): Int {
        val blackColor = Color.BLACK

        val offset = Random.nextInt(-80, 80) // 随机偏移量
        val newRed = (Color.red(value) + offset).coerceIn(0, 255)
        val newGreen = (Color.green(value) + offset).coerceIn(0, 255)
        val newBlue = (Color.blue(value) + offset).coerceIn(0, 255)
        val color = Color.rgb(newRed, newGreen, newBlue)

        // 计算颜色与黑色的差异程度
        val colorDiff = abs(Color.red(color) - Color.red(blackColor)) +
                abs(Color.green(color) - Color.green(blackColor)) +
                abs(Color.blue(color) - Color.blue(blackColor))

        // 如果颜色过深或者差异程度不够大，则调整颜色
        val threshold = 400 // 可以根据需要调整阈值
        if (colorDiff < threshold) {
            // 计算需要调整的亮度
            val adjustedRed = 255.coerceAtMost(Color.red(color) + 100)
            val adjustedGreen = 255.coerceAtMost(Color.green(color) + 100)
            val adjustedBlue = 255.coerceAtMost(Color.blue(color) + 100)

            // 构造新的颜色并返回
            return Color.rgb(adjustedRed, adjustedGreen, adjustedBlue)
        }

        // 如果差异程度足够大，则不需要调整，直接返回原始颜色
        return color
    }

    private fun adjustColorToWhite(value: Int): Int {
        val whiteColor = Color.WHITE

        val offset = Random.nextInt(-80, 80) // 随机偏移量
        val newRed = (Color.red(value) + offset).coerceIn(0, 255)
        val newGreen = (Color.green(value) + offset).coerceIn(0, 255)
        val newBlue = (Color.blue(value) + offset).coerceIn(0, 255)
        val color = Color.rgb(newRed, newGreen, newBlue)

        // 计算颜色与白色的差异程度
        val colorDiff = abs(Color.red(color) - Color.red(whiteColor)) +
                abs(Color.green(color) - Color.green(whiteColor)) +
                abs(Color.blue(color) - Color.blue(whiteColor))

        // 如果颜色过浅或者差异程度不够大，则调整颜色
        val threshold = 400 // 可以根据需要调整阈值
        if (colorDiff < threshold) {
            // 计算需要调整的亮度
            val adjustedRed = 0.coerceAtLeast(Color.red(color) - 100)
            val adjustedGreen = 0.coerceAtLeast(Color.green(color) - 100)
            val adjustedBlue = 0.coerceAtLeast(Color.blue(color) - 100)

            // 构造新的颜色并返回
            return Color.rgb(adjustedRed, adjustedGreen, adjustedBlue)
        }

        // 如果差异程度足够大，则不需要调整，直接返回原始颜色
        return color
    }

    private fun blurBitmap(
        input: Bitmap?,
        value: Float,
        context: Context,
        callback: (Bitmap) -> Unit
    ) {
        HokoBlur.with(context)
            .radius(value.toInt())
            .forceCopy(true)
            .asyncBlur(input, object : AsyncBlurTask.Callback {
                override fun onBlurSuccess(bitmap: Bitmap) {
                    callback(bitmap)
                }

                override fun onBlurFailed(error: Throwable) {}
            })
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


    private fun extractColorsFromPalette(): List<Int> {
        val palette = Palette.from(bitmap!!).generate()

        // 获取所有的颜色 swatch，并根据 population 属性进行排序
        val sortedSwatches = palette.swatches.sortedByDescending { it.population }

        // 提取前五个颜色
        return sortedSwatches.take(3).map { it.rgb }
    }

    private fun performPaintColor(
        foreground: Bitmap,
        background: Bitmap,
        context: Context,
        position: Int,
        color: Int,
        scale: Float
    ) {

        val target = scaleBitmapTo(
            fitBitmapToScreenAlt(foreground, context),
            scale
        )

        val (left, top) = calculatePosition(
            position,
            background.width,
            background.height,
            target.width,
            target.height
        )

        val canvas = Canvas(background)
        canvas.drawColor(color)
        canvas.drawBitmap(target, left.toFloat(), top.toFloat(), null)

        updateViewerState(background, background)
    }

    private fun addShadow(image: Bitmap, zoom: Float, offset: Int = 32): Bitmap {
        val width = image.width
        val height = image.height

        // 创建新的背景图片
        val _background =
            Bitmap.createBitmap(width + offset * 2, height + offset * 2, Bitmap.Config.ARGB_8888)

        if (_background.width > width / zoom || _background.height > height / zoom) return image

        val canvas = Canvas(_background)

        // 创建绘制阴影的 Paint 对象
        val shadowPaint = Paint().apply {
            color = Color.BLACK
            alpha = 255
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_ATOP)
        }

        // 绘制阴影
        for (i in 0 until offset) {
            val alpha = (255 * (1.3.pow(i) / 1.3.pow(offset))).toInt() // 根据当前距离计算透明度
            shadowPaint.alpha = alpha
            val rect = RectF(
                i.toFloat(),
                i.toFloat(),
                (width + offset * 2 - i).toFloat(),
                (height + offset * 2 - i).toFloat()
            )
            canvas.drawRect(rect, shadowPaint)
        }

        // 将原始图片绘制在背景上
        canvas.drawBitmap(image, offset.toFloat(), offset.toFloat(), null)

        return _background
    }


    private fun performPaintBlur(
        foreground: Bitmap,
        background: Bitmap,
        position: Int,
        blur: Int,
        context: Context,
        scale: Float = 1f
    ) {

        val bHeight = background.height
        val bWidth = background.width

        val _bitmapFit = scaleBitmapTo(
            fitBitmapToScreenAlt(foreground, context),
            scale
        )
        val target = addShadow(_bitmapFit, scale)

        val canvasBack = Canvas(background)
        val srcLeft = (bitmap!!.width - bWidth) / 2
        val srcTop = (bitmap!!.height - bHeight) / 2
        val srcRect = Rect(
            srcLeft,
            srcTop,
            srcLeft + bWidth,
            srcTop + bHeight
        )

        // 定义目标区域
        val destRect = Rect(0, 0, bWidth, bHeight)

        // 在 Canvas 上绘制裁剪后的图像
        canvasBack.drawBitmap(bitmap!!, srcRect, destRect, null)

        val (left, top) = calculatePosition(
            position,
            background.width,
            background.height,
            target.width,
            target.height
        )

        HokoBlur.with(context)
            .radius(blur)
            .forceCopy(true)
            .asyncBlur(background, object : AsyncBlurTask.Callback {
                override fun onBlurSuccess(bitmap: Bitmap) {
                    val canvas = Canvas(bitmap)
                    canvas.drawBitmap(target, left.toFloat(), top.toFloat(), null)
                    updateViewerState(bitmap, bitmap)
                }

                override fun onBlurFailed(error: Throwable) {}
            })
    }

    private fun calculatePosition(
        position: Int,
        backgroundWidth: Int,
        backgroundHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Pair<Int, Int> {
        return when (position) {
            TOP -> Pair((backgroundWidth - targetWidth) / 2, 0)
            BOTTOM -> Pair((backgroundWidth - targetWidth) / 2, backgroundHeight - targetHeight)
            LEFT -> Pair(0, (backgroundHeight - targetHeight) / 2)
            RIGHT -> Pair(backgroundWidth - targetWidth, (backgroundHeight - targetHeight) / 2)
            CENTER -> Pair(
                (backgroundWidth - targetWidth) / 2,
                (backgroundHeight - targetHeight) / 2
            )

            else -> Pair(0, 0)
        }
    }


    private fun performUpscale(input: Bitmap, modelName: String, context: Context, tileSize: Int, fp16: Boolean, cpuDisabled: Boolean) {

        Log.v("WVM", "UPSCALE $upscaleToggle")

        var model = R.raw.realesrgan_anime
        var scale = 4

        val inputBitmap: Bitmap = if (input == bitmap) {
            bitmapRaw!!
        } else {
            if (input.width / bitmap!!.width
                == input.height / bitmap!!.height) {
                scaleBitmapTo(
                    input,
                    bitmapRaw!!.width.toFloat() / input.width)
            } else {
                input
            }
        }


        val modelArray = context.resources.getStringArray(R.array.model_names)
        when (modelName) {
            modelArray[0] -> {
                model = R.raw.realesrgan_x2plus
                scale = 2
            }

            modelArray[1] -> {
                model = R.raw.realesrgan_x4plus
                scale = 4
                scaleBitmapTo(inputBitmap, 0.5f)
            }

            modelArray[2] -> {
                model = R.raw.realesrgan_anime
                scaleBitmapTo(inputBitmap, 0.5f)
            }
        }


        val sessionOptions: OrtSession.SessionOptions = OrtSession.SessionOptions()
        sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())

        val nnapiFlags = EnumSet.noneOf(NNAPIFlags::class.java)
        if (fp16) nnapiFlags.add(NNAPIFlags.USE_FP16)
        if (cpuDisabled) nnapiFlags.add(NNAPIFlags.CPU_DISABLED)
        if (nnapiFlags.isNotEmpty()) sessionOptions.addNnapi(nnapiFlags)

        Log.v("WVM", "OPTIONS $sessionOptions")

        ortSession = ortEnv.createSession(readModel(context, model), sessionOptions)

        // 1. 输入是一个 bimap
        val originalWidth = inputBitmap.width
        val originalHeight = inputBitmap.height

        Log.v("WVM", "TILE SIZE $tileSize")
        // 2. 将 bitmap 分割为多个 tile，记录 tile 的相对位置
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
                        Bitmap.createBitmap(inputBitmap, tileX, tileY, tileWidth, tileHeight)

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
            var resultBitmap =
                bitmapRaw?.config?.let { Bitmap.createBitmap(resultWidth, resultHeight, it) }
            val canvas = resultBitmap?.let { Canvas(it) }
            var currentX = 0
            var currentY = 0

            for (processedTileDeferred in processedTilesDeferred) {
                val processedTile = processedTileDeferred.await()
                if (processedTile != null) {
                    if (canvas != null) {
                        canvas.drawBitmap(processedTile, currentX.toFloat(), currentY.toFloat(), null)
                    }
                    currentX += processedTile.width
                    if (currentX >= resultWidth) {
                        currentX = 0
                        currentY += processedTile.height
                    }
                }
            }

            resultBitmap = resultBitmap?.let { fitBitmapToScreen(it, context) }

            _viewerState.update { current ->
                current.copy(
                    bitmapHome = resultBitmap,
                    bitmapLock = resultBitmap
                )
            }
        }
    }

    private fun updateViewerState(home: Bitmap?, lock: Bitmap?) {
        _viewerState.update { current ->
            current.copy(
                bitmapHome = if (processHome) home ?: current.bitmapHome else current.bitmapHome,
                bitmapLock = if (processLock) lock ?: current.bitmapLock else current.bitmapLock
            )
        }
    }


    private fun isDarkMode(context: Context): Boolean {
        when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> {
                return false
            } // Night mode is not active, we're using the light theme
            Configuration.UI_MODE_NIGHT_YES -> {
                return true
            } // Night mode is active, we're using dark theme
        }
        return false
    }
}