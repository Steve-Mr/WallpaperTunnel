package com.maary.shareas.activity

import android.Manifest
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.color.DynamicColors
import com.maary.shareas.R
import com.maary.shareas.databinding.ActivityStartBinding
import com.maary.shareas.helper.PreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class StartActivity : AppCompatActivity(){

    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_start)

        runBlocking {
            val preferencesHelper = PreferencesHelper(applicationContext)
            val settingsFinished = preferencesHelper.getSettingsFinished().first()
            if (!settingsFinished){
                val intent = Intent(applicationContext, WelcomeActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        val pickerBottomSheetBehavior = BottomSheetBehavior.from(binding.pickerBottomSheet)
        pickerBottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED

        ViewCompat.setOnApplyWindowInsetsListener(binding.buttonPicker) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = systemBars.bottom
            }

            // some dirty hack
            pickerBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            pickerBottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED

            insets
        }

        val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri->
            if (uri != null) {
                val intent = Intent(application, MainActivity::class.java).apply {
                    action = Intent.ACTION_SEND
                    setDataAndType(uri, "image/*")
                    putExtra("mimeType", "image/*")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            }

        }

        val wallpaperManager: WallpaperManager = WallpaperManager.getInstance(this)
        val homePFD:ParcelFileDescriptor?
        var lockPFD:ParcelFileDescriptor?
        val homeBitmap: Bitmap
        var lockBitmap: Bitmap

        if (checkPermission()){
            homePFD = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)
            if (homePFD != null) {
                lockPFD = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_LOCK)
                if (lockPFD == null){
                    lockPFD = homePFD
                }

                homeBitmap = BitmapFactory.decodeFileDescriptor(homePFD.fileDescriptor)
                lockBitmap = BitmapFactory.decodeFileDescriptor(lockPFD.fileDescriptor)

            }else{
                homeBitmap = (wallpaperManager.getBuiltInDrawable(WallpaperManager.FLAG_SYSTEM)).toBitmap()
                /**
                 * 持续产生
                 * java.lang.NullPointerException: wallpaperManager.getBuil…llpaperManager.FLAG_LOCK) must not be null
                 * 问题
                 * */
//                lockBitmap = (wallpaperManager.getBuiltInDrawable(WallpaperManager.FLAG_LOCK)).toBitmap()
//                if (lockBitmap == null){
                    lockBitmap = homeBitmap
//                }
            }
            binding.homeContainer.setImageBitmap(homeBitmap)
            binding.lockContainer.setImageBitmap(lockBitmap)

            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

            var isHomeSaved = false
            var isLockSaved = false

            lifecycleScope.launch {
                isHomeSaved = saveBitmapAsync(homeBitmap, "home")
                isLockSaved = lockBitmap?.let { saveBitmapAsync(it, "lock") } == true
            }

            binding.homeContainer.setOnClickListener {
                if (isHomeSaved) {
                    shareImg("home")
                }
            }

            binding.lockContainer.setOnClickListener {
                if (lockBitmap != null) {
                    if (isLockSaved) {
                        shareImg("lock")
                    }
                }
            }

        } else {
            binding.systemWallpContainer.visibility = View.INVISIBLE
        }

        binding.buttonPicker.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }


    }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            (Environment.isExternalStorageManager()
                    &&  ContextCompat.checkSelfPermission(application,
                Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED)
        } else {
            ContextCompat.checkSelfPermission(application,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        }
    }

    // Define a suspend function to save the bitmap asynchronously
    private suspend fun saveBitmapAsync(bitmap: Bitmap, name: String): Boolean {
        return withContext(Dispatchers.IO) {
            val cachePath = File(externalCacheDir, "my_images/")
            cachePath.mkdirs()

            //create png file
            val file = File(cachePath, "$name.png")
            try {
                val fileOutputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                fileOutputStream.flush()
                fileOutputStream.close()
                return@withContext true // Return true if saving succeeds
            } catch (e: IOException) {
                e.printStackTrace()
                return@withContext false // Return false if saving fails
            }
        }
    }

    private fun shareImg(name: String) {

        //---Save bitmap to external cache directory---//
        //get cache directory
        val cachePath = File(externalCacheDir, "my_images/")
        cachePath.mkdirs()

        //create png file
        val file = File(cachePath, "$name.png")

        //---Share File---//
        //get file uri
        val myImageFileUri =
            FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", file)

        val intent = Intent(application, MainActivity::class.java).apply {
            action = Intent.ACTION_SEND
            setDataAndType(myImageFileUri, "image/*")
            putExtra("mimeType", "image/*")
            putExtra(Intent.EXTRA_STREAM, myImageFileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }


}