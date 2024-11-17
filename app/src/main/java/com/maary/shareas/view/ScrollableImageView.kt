package com.maary.shareas.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.ScrollView

class ScrollableImageView  @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    private lateinit var horizontalView: CustomHorizontalView
    private lateinit var imageView: ImageView

    init {
        initHorizontalView(context)
    }

    private fun initHorizontalView(context: Context) {
        horizontalView = CustomHorizontalView(context)
        addView(horizontalView)
        imageView = ImageView(context)
        horizontalView.addView(imageView)
    }

    fun setImageBitmap(bitmap: Bitmap) {
        imageView.setImageBitmap(bitmap)
        imageView.scaleType = ImageView.ScaleType.FIT_XY
    }

    fun setImageUri(uri: Uri) {
        imageView.setImageURI(uri)
        imageView.scaleType = ImageView.ScaleType.FIT_XY
    }

    fun setOnImageClickListener(listener: OnClickListener) {
        imageView.setOnClickListener(listener)
    }

    fun getVisibleBitmap(): Bitmap {
        // 获取 ScrollView 和 HorizontalView 的滚动距离
        val scrollX = scrollX
        val scrollY = scrollY
        val horizontalScrollX = horizontalView.scrollX
        val horizontalScrollY = horizontalView.scrollY

        // 计算可视部分的矩形区域
        val visibleRect = Rect(scrollX + horizontalScrollX, scrollY + horizontalScrollY,
            scrollX + horizontalScrollX + width, scrollY + horizontalScrollY + height)

        // 创建与可视部分尺寸相同的 Bitmap
        val bitmap = Bitmap.createBitmap(visibleRect.width(), visibleRect.height(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 将 Canvas 移动到 ScrollView 和 HorizontalView 的滚动位置
        canvas.translate(-visibleRect.left.toFloat(), -visibleRect.top.toFloat())

        // 绘制 ScrollView 和 HorizontalView 可视部分的内容
        draw(canvas)

        return bitmap
    }

    fun getVisibleRect(): Rect {
        // 获取 ScrollView 和 HorizontalView 的滚动距离
        val scrollX = scrollX
        val scrollY = scrollY
        val horizontalScrollX = horizontalView.scrollX
        val horizontalScrollY = horizontalView.scrollY

        // 计算可视部分的矩形区域
        return Rect(scrollX + horizontalScrollX, scrollY + horizontalScrollY,
            scrollX + horizontalScrollX + width, scrollY + horizontalScrollY + height)

    }

    fun getVisibleRectStart(): Int {
        // 获取 ScrollView 和 HorizontalView 的滚动距离
        val scrollX = scrollX
        val horizontalScrollX = horizontalView.scrollX

        // 计算可视部分的矩形区域
        return scrollX + horizontalScrollX

    }

    fun getVisibleRectEnd(): Int {
        // 获取 ScrollView 和 HorizontalView 的滚动距离
        val scrollX = scrollX
        val horizontalScrollX = horizontalView.scrollX

        // 计算可视部分的矩形区域
        return scrollX + horizontalScrollX + width

    }

    fun scrollImageTo(target: Int, x: Int, y: Int) {
        if (target == VERTICAL) this.smoothScrollTo(x, y)
        if (target == HORIZONTAL) horizontalView.smoothScrollTo(x, y)
    }

    inner class CustomHorizontalView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : HorizontalScrollView(context, attrs, defStyleAttr) {

        init {
            isHorizontalScrollBarEnabled = false
        }

    }

    companion object {
        val VERTICAL = 0
        val HORIZONTAL = 1
    }
}