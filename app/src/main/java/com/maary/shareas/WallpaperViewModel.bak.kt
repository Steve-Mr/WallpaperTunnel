//package com.maary.shareas
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.util.Log
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.asLiveData
//import com.hoko.blur.HokoBlur
//import com.hoko.blur.task.AsyncBlurTask
//import com.maary.shareas.data.WallpaperBitmap
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.update
//
//class WallpaperViewModel: ViewModel() {
////
////    var bitmapRaw: Bitmap? = null
////    var bitmap: Bitmap? = null
////    var bitmapHome: Bitmap? = null
////    var bitmapLock: Bitmap? = null
////    var bitmapEdit: Bitmap? = null
//
//    var inEditor = false
//        set(value) {
//            field = value
//            _inEditor.value = value
//        }
//
//    private val HOME = 1
//    private val LOCK = 0
//
//    private var currentBitmap = HOME
//
//    private val _bitmapHome = MutableLiveData<Bitmap?>()
//
//    private val _bitmapLock = MutableLiveData<Bitmap?>()
//
//    private val _currentBitmapState = MutableStateFlow(currentBitmap)
//    val currentBitmapState: StateFlow<Int?> = _currentBitmapState.asStateFlow()
//    val currentBitmapStateLiveData: LiveData<Int?> = _currentBitmapState.asLiveData()
//
//    private val _bitmapState = MutableStateFlow(WallpaperBitmap())
//    val bitmapState: StateFlow<WallpaperBitmap> = _bitmapState.asStateFlow()
//    val bitmapStateLiveData: LiveData<WallpaperBitmap> = _bitmapState.asLiveData()
//
//
//    private val _inEditor: MutableStateFlow<Boolean> = MutableStateFlow(inEditor)
//    val inEditorState: StateFlow<Boolean> = _inEditor.asStateFlow()
//    val inEditorLiveData: LiveData<Boolean> = _inEditor.asLiveData()
//
//    fun setBitmapRaw(bitmap: Bitmap) {
//        _bitmapState.value.bitmapRaw = bitmap
//    }
//
//    fun setBitmap(bitmap: Bitmap) {
//        _bitmapState.value.setDefaultBitmap(bitmap)
//    }
//
//    fun getBitmap(): Bitmap? {
//        return _bitmapState.value.bitmap
//    }
//
//    fun getBitmapHome(): Bitmap? {
//        return _bitmapState.value.bitmapHome
//    }
//
//    fun getBitmapLock(): Bitmap? {
//        return _bitmapState.value.bitmapLock
//    }
//
//    fun restoreChanges() {
//        _bitmapState.update {
//            it.copy(
//                bitmapHome = it.bitmap,
//                bitmapLock =  it.bitmap,
//                bitmapEdit =  it.bitmap)
//        }
//    }
//
//    fun startEditing() {
//        inEditor = true
//    }
//
//    fun finishEditing() {
//        inEditor = false
//    }
//
//    fun getDisplayBitmap(): Bitmap? {
//        if (inEditor) {
//            Log.v("WVM-E", _bitmapState.value.toString())
//            return _bitmapState.value.bitmapEdit
//        }
//        else if (currentBitmap == HOME){
//            return _bitmapState.value.bitmapHome
//        } else if (currentBitmap == LOCK) {
//            return _bitmapState.value.bitmapLock
//        }
//        return _bitmapState.value.bitmap
//    }
//
//    fun currentBitmapToggle(): Int {
//        if (currentBitmap == HOME) {
//            currentBitmap = LOCK
//            return R.drawable.ic_lockscreen
//        }
//        if (currentBitmap == LOCK) {
//            currentBitmap = HOME
//            return R.drawable.ic_vertical
//        }
//        return R.drawable.ic_vertical
//    }
//
//    fun editBlur(context: Context, value: Float) {
//        HokoBlur.with(context)
//            .radius(value.toInt())
//            .sampleFactor(1.0f)
//            .forceCopy(true)
//            .asyncBlur(_bitmapState.value.bitmap, object : AsyncBlurTask.Callback {
//                override fun onBlurSuccess(bitmap: Bitmap) {
//                    _bitmapState.value.setEdit(bitmap)
////                    Log.v("WVM-B", _bitmapState.value.setEdit(bitmap))
//                    Log.v("WVM-B", _bitmapState.value.toString())
//                    Log.v("WVM-B2", getDisplayBitmap().toString())
//
//
//                }
//
//                override fun onBlurFailed(error: Throwable) {}
//            })
//    }
//
//    fun editBitmap(bitmap: Bitmap) {
////        _bitmapState.update { currentState ->
////            Log.v("WVM-B",currentState.toString())
////            currentState.copy(bitmapEdit = bitmap)
////        }
//        var _wBitmap: WallpaperBitmap? = _bitmapState.value
//        _wBitmap?.bitmapEdit = bitmap
//        if (_wBitmap != null) {
//            _bitmapState.value = _wBitmap
//        }
//        _wBitmap = null
//    }
//
//    fun applyToHome() {
//        _bitmapState.update {
//            it.copy(bitmapHome = it.bitmapEdit)
//        }
//    }
//
//    fun applyToLock() {
//        _bitmapState.update {
//            it.copy(bitmapLock = it.bitmapEdit)
//        }
//    }
//
//
//
//
//
//}