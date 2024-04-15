package com.maary.shareas.data

import android.graphics.Bitmap
import com.maary.shareas.WallpaperViewModel

data class WallpaperBitmap(
    var bitmapRaw: Bitmap? = null,
    var bitmap: Bitmap? = null,
    var bitmapHome: Bitmap? = bitmap,
    var bitmapLock: Bitmap? = bitmap,
    var bitmapEdit: Bitmap? = bitmap
) {

    fun setDefaultBitmap(bitmap: Bitmap) {
        this.bitmap = bitmap
        this.bitmapHome = bitmap
        this.bitmapLock = bitmap
        this.bitmapEdit = bitmap
    }

    fun setEdit(bitmap: Bitmap) {
        this.bitmapEdit = bitmap
    }

    fun restore() {
        bitmapHome = bitmap
        bitmapLock = bitmap
        bitmapEdit = bitmap
    }

    fun applyToHome() {
        bitmapHome = bitmapEdit
    }

    fun applyToLock() {
        bitmapLock = bitmapEdit
    }

    override fun equals(other: Any?): Boolean {
        return false
    }

    override fun hashCode(): Int {
        return (bitmapRaw?.hashCode() ?: 0) +
                (bitmap?.hashCode() ?: 0) +
                (bitmapHome?.hashCode() ?: 0) +
                (bitmapLock?.hashCode() ?: 0) +
                (bitmapEdit?.hashCode() ?: 0)
    }
}