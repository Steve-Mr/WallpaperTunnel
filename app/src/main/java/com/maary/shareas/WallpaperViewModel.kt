package com.maary.shareas

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.hoko.blur.HokoBlur
import com.hoko.blur.task.AsyncBlurTask
import com.maary.shareas.data.ViewerBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WallpaperViewModel: ViewModel() {

    private var bakBitmap = ViewerBitmap()

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

    fun saveEdit() {
        bakBitmap.bitmapHome = _viewerState.value.bitmapHome
        bakBitmap.bitmapLock = _viewerState.value.bitmapLock
    }
}