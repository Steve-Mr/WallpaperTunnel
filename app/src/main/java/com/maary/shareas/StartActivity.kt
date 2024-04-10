package com.maary.shareas

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.color.DynamicColors
import com.maary.shareas.databinding.ActivityStartBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class StartActivity : AppCompatActivity(){

    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_start)

        val SETTINGS_FINISHED = booleanPreferencesKey("SETTING_FINISHED")
        val settingsFinishedFlow: Flow<Boolean> = applicationContext.dataStore.data
            .map { preferences ->
                // No type safety.
                preferences[SETTINGS_FINISHED] ?: false
            }

        runBlocking {
            val settingsFinished = settingsFinishedFlow.first()
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
            pickerBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            insets
        }



        val wallpaperManager: WallpaperManager = WallpaperManager.getInstance(this)
        val homePFD:ParcelFileDescriptor?
        var lockPFD:ParcelFileDescriptor?
        val homeBitmap: Bitmap
        var lockBitmap: Bitmap?

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
                lockBitmap = (wallpaperManager.getBuiltInDrawable(WallpaperManager.FLAG_LOCK)).toBitmapOrNull()
                if (lockBitmap == null){
                    lockBitmap = homeBitmap
                }
            }
            binding.homeContainer.setImageBitmap(homeBitmap)
            binding.lockContainer.setImageBitmap(lockBitmap)

            var isHomeSaved = false
            var isLockSaved = false

            lifecycleScope.launch {
                isHomeSaved = saveBitmapAsync(homeBitmap, "home")
                isLockSaved = lockBitmap?.let { saveBitmapAsync(it, "lock") } == true
            }

            binding.homeContainer.setOnClickListener {
                while (!isHomeSaved){}
                shareImg("home")
            }

            binding.lockContainer.setOnClickListener {
                if (lockBitmap != null) {
//                    shareBitmap(lockBitmap)
                    while (!isLockSaved){}
                    shareImg("lock")
                }
            }

        } else {
            binding.systemWallpContainer.visibility = View.INVISIBLE
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

    private fun shareBitmap(bitmap: Bitmap) {

        //---Save bitmap to external cache directory---//
        //get cache directory
        val cachePath = File(externalCacheDir, "my_images/")
        cachePath.mkdirs()

        //create png file
        val file = File(cachePath, "Image_123.png")
        val fileOutputStream: FileOutputStream
        try {
            fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

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