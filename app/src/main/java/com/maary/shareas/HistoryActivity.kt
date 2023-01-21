package com.maary.shareas

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Settings.Global
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.IntentSenderRequest
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.*
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maary.shareas.databinding.ActivityHistoryBinding
import kotlinx.coroutines.*


/** The request code for requesting [Manifest.permission.READ_EXTERNAL_STORAGE] permission. */
private const val READ_EXTERNAL_STORAGE_REQUEST = 0x1045

/**
 * Code used with [IntentSender] to request user permission to delete an image with scoped storage.
 */
private const val DELETE_PERMISSION_REQUEST = 0x1033

class HistoryActivity : AppCompatActivity(){

    private val viewModel: HistoryActivityViewModel by viewModels()
    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_history)

        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> {
                recreate()
                binding.appBarContainer.setBackgroundColor(getColor(R.color.semiTransparent))
                window.decorView.windowInsetsController?.setSystemBarsAppearance(
                    APPEARANCE_LIGHT_STATUS_BARS, APPEARANCE_LIGHT_STATUS_BARS)
            } // Night mode is not active, we're using the light theme
            Configuration.UI_MODE_NIGHT_YES -> {
                recreate()
                binding.appBarContainer.setBackgroundColor(getColor(R.color.semiBlack))
                window.decorView.windowInsetsController?.setSystemBarsAppearance(
                    0, APPEARANCE_LIGHT_STATUS_BARS)
            } // Night mode is active, we're using dark theme
        }

        val typedValue = TypedValue()
        var actionBarHeight = 0
        if(theme.resolveAttribute(R.attr.actionBarSize, typedValue, true)){
            actionBarHeight = TypedValue.complexToDimensionPixelOffset(
                typedValue.data, resources.displayMetrics)
        }

        binding.gallery.setOnApplyWindowInsetsListener { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsets.Type.systemBars())
            view.updatePadding(top = insets.top + actionBarHeight, bottom = insets.bottom * 2)
            WindowInsets.CONSUMED
        }

        val galleryAdapter = GalleryAdapter (
            onClick = {image -> openImage(image)},
            onLongClick = {image -> deleteImage(image)}
        )

        binding.gallery.also { view ->
            view.layoutManager = GridLayoutManager(this, 3)
            view.adapter = galleryAdapter
        }

        viewModel.images.observe(this, Observer<List<MediaStoreImage>> { images ->
            galleryAdapter.submitList(images)
        })

        viewModel.permissionNeededForDelete.observe(this, Observer { intentSender ->
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
        })

        binding.appBarContainer.elevation = 8.0f

        binding.closeActivity.setOnClickListener {
            finish()
        }

        binding.buttonClearAll.setOnClickListener {
            deleteAllImages()

        }

        if (!haveStoragePermission()) {
//            binding.welcomeView.visibility = View.VISIBLE
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
                } else {
                    // If we weren't granted the permission, check to see if we should show
                    // rationale for the permission.
                    val showRationale =
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        )

                    /**
                     * If we should show the rationale for requesting storage permission, then
                     * we'll show [ActivityMainBinding.permissionRationaleView] which does this.
                     *
                     * If `showRationale` is false, this means the user has not only denied
                     * the permission, but they've clicked "Don't ask again". In this case
                     * we send the user to the settings page for the app so they can grant
                     * the permission (Yay!) or uninstall the app.
                     */
                    if (showRationale) {
//                        showNoAccess()
                        //TODO: do something
                    } else {
                        goToSettings()
                    }
                }
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == DELETE_PERMISSION_REQUEST) {
            viewModel.deletePendingImage()
        }
    }

    private fun showImages() {
        viewModel.loadImages()
    }

    private fun openMediaStore() {
        if (haveStoragePermission()) {
            showImages()
        } else {
            requestPermission()
        }
    }

    private fun goToSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also { intent ->
            startActivity(intent)
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
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            ActivityCompat.requestPermissions(this, permissions, READ_EXTERNAL_STORAGE_REQUEST)
        }
    }

    private fun deleteImage(image: MediaStoreImage): Boolean {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_dialog_title)
            .setMessage(getString(R.string.delete_dialog_message, image.displayName))
            .setPositiveButton(R.string.delete_dialog_positive) { _: DialogInterface, _: Int ->
//                viewModel.deleteImage(image)

                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, arrayListOf(image.contentUri))
                val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                startIntentSenderForResult(pendingIntent.intentSender, 42, null, 0, 0, 0, null)
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
                    val pendingIntent = MediaStore.createDeleteRequest(contentResolver, list)
                    startIntentSenderForResult(pendingIntent.intentSender, 42, null, 0, 0, 0, null)
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
                .thumbnail(0.33f)
                .centerCrop()
                .into(holder.imageView)
        }
    }

    private fun openImage(image: MediaStoreImage){

        val intent = Intent(application, MainActivity::class.java).apply {
            action = Intent.ACTION_SEND
            setDataAndType(image.contentUri, "image/*")
            putExtra("mimeType", "image/*")
            putExtra(Intent.EXTRA_STREAM, image.contentUri);
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