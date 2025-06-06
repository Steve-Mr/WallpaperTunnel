package com.maary.shareas.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import android.widget.ImageView
import androidx.activity.result.IntentSenderRequest
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maary.shareas.HistoryActivityViewModel
import com.maary.shareas.R
import com.maary.shareas.data.MediaStoreImage
import com.maary.shareas.databinding.ActivityHistoryBinding
import com.maary.shareas.helper.Util
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/** The request code for requesting [Manifest.permission.READ_EXTERNAL_STORAGE] permission. */
private const val READ_EXTERNAL_STORAGE_REQUEST = 0x1045

/**
 * Code used with [IntentSender] to request user permission to delete an image with scoped storage.
 */
private const val DELETE_PERMISSION_REQUEST = 0x1033

class HistoryActivity : AppCompatActivity(){

    private val viewModel: HistoryActivityViewModel by viewModels()
    private lateinit var binding: ActivityHistoryBinding
    private val galleryAdapter = GalleryAdapter (
        onClick = {image -> openImage(image)},
        onLongClick = {image -> deleteImage(image)}
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_history)

        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> {
                binding.appBarContainer.setBackgroundColor(getColor(R.color.semiTransparent))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.decorView.windowInsetsController?.setSystemBarsAppearance(
                        APPEARANCE_LIGHT_STATUS_BARS, APPEARANCE_LIGHT_STATUS_BARS)
                }else {
                    window.statusBarColor = ContextCompat.getColor(this, R.color.semiTransparent)
                }
            } // Night mode is not active, we're using the light theme
            Configuration.UI_MODE_NIGHT_YES -> {
                binding.appBarContainer.setBackgroundColor(getColor(R.color.semiBlack))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.decorView.windowInsetsController?.setSystemBarsAppearance(
                        0, APPEARANCE_LIGHT_STATUS_BARS)
                }
                else {
                    window.statusBarColor = ContextCompat.getColor(this, R.color.semiBlack)
                }
            } // Night mode is active, we're using dark theme
        }

        val typedValue = TypedValue()
        var actionBarHeight = 0
        if(theme.resolveAttribute(androidx.appcompat.R.attr.actionBarSize, typedValue, true)){
            actionBarHeight = TypedValue.complexToDimensionPixelOffset(
                typedValue.data, resources.displayMetrics)
        }

        binding.gallery.setOnApplyWindowInsetsListener { view, windowInsets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val insets = windowInsets.getInsets(WindowInsets.Type.systemBars())
                view.updatePadding(top = insets.top + actionBarHeight, bottom = insets.bottom * 2)
                WindowInsets.CONSUMED
            } else {
                view.updatePadding(top = windowInsets.systemWindowInsetTop + actionBarHeight, bottom =  windowInsets.systemWindowInsetBottom * 2)
                windowInsets
            }

        }

        binding.gallery.also { view ->
            view.layoutManager = GridLayoutManager(this, 3)
            view.adapter = galleryAdapter
        }

        viewModel.images.observe(this) { images ->
            galleryAdapter.submitList(images)
        }

        viewModel.permissionNeededForDelete.observe(this) { intentSender ->
            intentSender?.let {
                // On Android 10+, if the app doesn't have permission to modify
                // or delete an item, it returns an `IntentSender` that we can
                // use here to prompt the user to grant permission to delete (or modify)
                // the image.
                startIntentSenderForResult(
                    intentSender,
                    DELETE_PERMISSION_REQUEST,
                    null,
                    0,
                    0,
                    0,
                    null
                )
            }
        }

        binding.appBarContainer.elevation = 8.0f

        binding.closeActivity.setOnClickListener {
            finish()
        }

        binding.buttonClearAll.setOnClickListener {
            deleteAllImages()

        }

        if (!haveStoragePermission()) {
            requestPermission()
        } else {
            showImages()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_EXTERNAL_STORAGE_REQUEST -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showImages()
                }
                return
            }
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      " +
            "which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      " +
            "contracts for common intents available in\n      " +
            "{@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      " +
            "testing, and allow receiving results in separate, testable classes independent from your\n      " +
            "activity. Use\n      " +
            "{@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      " +
            "with the appropriate {@link ActivityResultContract} and handling the result in the\n      " +
            "{@link ActivityResultCallback#onActivityResult(Object) callback}.")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == DELETE_PERMISSION_REQUEST) {
            viewModel.deletePendingImage()
        }
    }

    private fun showImages() {
        viewModel.loadImages()

        if (viewModel.images.value?.isEmpty() == true){
            binding.layoutNoHistory.visibility = View.VISIBLE
            binding.buttonClearAll.visibility = View.GONE
        }else {
            binding.layoutNoHistory.visibility = View.INVISIBLE
            binding.buttonClearAll.visibility = View.VISIBLE
        }
    }

    /**
     * Convenience method to check if [Manifest.permission.READ_EXTERNAL_STORAGE] permission
     * has been granted to the app.
     */
    private fun haveStoragePermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Convenience method to request [Manifest.permission.READ_EXTERNAL_STORAGE] permission.
     */
    private fun requestPermission() {
        if (!haveStoragePermission()) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                val permissionsT = arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                )
                ActivityCompat.requestPermissions(this, permissionsT, READ_EXTERNAL_STORAGE_REQUEST)
            }else{
            ActivityCompat.requestPermissions(this, permissions, READ_EXTERNAL_STORAGE_REQUEST)
            }
        }
    }

    private fun deleteImage(image: MediaStoreImage): Boolean {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_dialog_title)
            .setMessage(getString(R.string.delete_dialog_message, image.displayName))
            .setPositiveButton(R.string.delete_dialog_positive) { _: DialogInterface, _: Int ->
//                viewModel.deleteImage(image)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val pendingIntent = MediaStore.createDeleteRequest(contentResolver, arrayListOf(image.contentUri))
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                    startIntentSenderForResult(pendingIntent.intentSender, 42, null, 0, 0, 0, null)
                } else {
                    application.contentResolver.delete(
                        image.contentUri,
                        "${MediaStore.Images.Media._ID} = ?",
                        arrayOf(image.id.toString())
                    )

                }
            }
            .setNegativeButton(R.string.delete_dialog_negative) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .show()
        return true
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun deleteAllImages() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_dialog_title)
            .setMessage(getString(R.string.delete_all_dialog_message))
            .setPositiveButton(R.string.delete_dialog_positive) { _: DialogInterface, _: Int ->
//                viewModel.deleteImage(image)
                GlobalScope.launch {
                    val list = withContext(Dispatchers.IO){
                        getUriList()
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val pendingIntent = MediaStore.createDeleteRequest(contentResolver, list)
                        startIntentSenderForResult(pendingIntent.intentSender, 42, null, 0, 0, 0, null)
                    } else {
                        val imageCollection = viewModel.queryImages()
                        for (image in imageCollection){
                            application.contentResolver.delete(
                                image.contentUri,
                                "${MediaStore.Images.Media._ID} = ?",
                                arrayOf(image.id.toString())
                            )
                        }

                    }
                }
//                val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            }
            .setNegativeButton(R.string.delete_dialog_negative) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * A [ListAdapter] for [MediaStoreImage]s.
     */
    private inner class GalleryAdapter(val onClick: (MediaStoreImage) -> Unit, val onLongClick: (MediaStoreImage) -> Boolean) :
        ListAdapter<MediaStoreImage, ImageViewHolder>(MediaStoreImage.DiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val view = layoutInflater.inflate(R.layout.gallery_layout, parent, false)

            val imageView:ImageView = view.findViewById(R.id.image)
            val sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
            val deviceHeight = sharedPreferences.getInt(
                getString(R.string.device_height), Util.getDeviceBounds(this@HistoryActivity).y)
            val deviceWidth = sharedPreferences.getInt(
                getString(R.string.device_width), Util.getDeviceBounds(this@HistoryActivity).x)
            val ratio = String.format("%d:%d", deviceWidth, deviceHeight)
            (imageView.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio = ratio
            imageView.clipToOutline = true

            return ImageViewHolder(view, onClick, onLongClick)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val mediaStoreImage = getItem(position)
            holder.rootView.tag = mediaStoreImage

            if (mediaStoreImage.width > mediaStoreImage.height){
                holder.labelView.visibility = View.VISIBLE
            }else{
                holder.labelView.visibility = View.INVISIBLE
            }

            Glide.with(holder.imageView)
                .load(mediaStoreImage.contentUri)
                .centerCrop()
                .into(holder.imageView)
        }
    }

    private fun openImage(image: MediaStoreImage){

        val intent = Intent(application, MainActivity::class.java).apply {
            action = Intent.ACTION_SEND
            setDataAndType(image.contentUri, "image/*")
            putExtra("mimeType", "image/*")
            putExtra(Intent.EXTRA_STREAM, image.contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }

    private suspend fun getUriList(): ArrayList<Uri> {
        val uriList = ArrayList<Uri>()
        val images = viewModel.queryImages()
        for (image in images) uriList.add(image.contentUri)
        return uriList
    }
}

/**
 * Basic [RecyclerView.ViewHolder] for our gallery.
 */
private class ImageViewHolder(view: View, onClick: (MediaStoreImage) -> Unit, onLongClick: (MediaStoreImage) -> Boolean) :
    RecyclerView.ViewHolder(view) {
    val rootView = view
    val imageView: ImageView = view.findViewById(R.id.image)
    val labelView: ImageView = view.findViewById(R.id.image_label_horizontal)

    init {

        imageView.setOnClickListener {
            val image = rootView.tag as? MediaStoreImage ?: return@setOnClickListener
            onClick(image)
        }
        imageView.setOnLongClickListener {
            val image = rootView.tag as? MediaStoreImage ?: return@setOnLongClickListener true
            onLongClick(image)
        }

    }
}