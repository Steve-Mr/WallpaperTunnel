package com.maary.shareas

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.RecoverableSecurityException
import android.content.*
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class HistoryActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val _images = MutableLiveData<List<MediaStoreImage>>()
    val images: LiveData<List<MediaStoreImage>> get() = _images

    private var contentObserver: ContentObserver? = null

    private var pendingDeleteImage: MediaStoreImage? = null
    private val _permissionNeededForDelete = MutableLiveData<IntentSender?>()
    val permissionNeededForDelete: LiveData<IntentSender?> = _permissionNeededForDelete

    /**
     * Performs a one shot load of images from [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] into
     * the [_images] [LiveData] above.
     */
    fun loadImages() {
        viewModelScope.launch {
            val imageList = queryImages()
            _images.postValue(imageList)

            if (contentObserver == null) {
                contentObserver = getApplication<Application>().contentResolver.registerObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                ) {
                    loadImages()
                }
            }
        }
    }

    fun getUriList(): ArrayList<Uri> {
        val uriList = ArrayList<Uri>()
        viewModelScope.launch {
            val images = queryImages()

            for (image in images) uriList.add(image.contentUri)

        }
        return uriList
    }

    fun deleteImage(image: MediaStoreImage) {
        viewModelScope.launch {
            performDeleteImage(image)
        }
    }

    fun deletePendingImage() {
        pendingDeleteImage?.let { image ->
            pendingDeleteImage = null
            deleteImage(image)
        }
    }

    suspend fun queryImages(): List<MediaStoreImage> {
        val images = mutableListOf<MediaStoreImage>()

        /**
         * Working with [ContentResolver]s can be slow, so we'll do this off the main
         * thread inside a coroutine.
         */
        withContext(Dispatchers.IO) {

            /**
             * A key concept when working with Android [ContentProvider]s is something called
             * "projections". A projection is the list of columns to request from the provider,
             * and can be thought of (quite accurately) as the "SELECT ..." clause of a SQL
             * statement.
             *
             * It's not _required_ to provide a projection. In this case, one could pass `null`
             * in place of `projection` in the call to [ContentResolver.query], but requesting
             * more data than is required has a performance impact.
             *
             * For this sample, we only use a few columns of data, and so we'll request just a
             * subset of columns.
             */
            /**
             * A key concept when working with Android [ContentProvider]s is something called
             * "projections". A projection is the list of columns to request from the provider,
             * and can be thought of (quite accurately) as the "SELECT ..." clause of a SQL
             * statement.
             *
             * It's not _required_ to provide a projection. In this case, one could pass `null`
             * in place of `projection` in the call to [ContentResolver.query], but requesting
             * more data than is required has a performance impact.
             *
             * For this sample, we only use a few columns of data, and so we'll request just a
             * subset of columns.
             */
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT
            )

            /**
             * The `selection` is the "WHERE ..." clause of a SQL statement. It's also possible
             * to omit this by passing `null` in its place, and then all rows will be returned.
             * In this case we're using a selection based on the date the image was taken.
             *
             * Note that we've included a `?` in our selection. This stands in for a variable
             * which will be provided by the next variable.
             */
            /**
             * The `selection` is the "WHERE ..." clause of a SQL statement. It's also possible
             * to omit this by passing `null` in its place, and then all rows will be returned.
             * In this case we're using a selection based on the date the image was taken.
             *
             * Note that we've included a `?` in our selection. This stands in for a variable
             * which will be provided by the next variable.
             */
            val selection = "${MediaStore.Images.ImageColumns.RELATIVE_PATH} like ?"

            /**
             * The `selectionArgs` is a list of values that will be filled in for each `?`
             * in the `selection`.
             */
            val selectionArgs = arrayOf(
                "Pictures/Wallpaper History/%"
            )

            /**
             * Sort order to use. This can also be null, which will use the default sort
             * order. For [MediaStore.Images], the default sort order is ascending by date taken.
             */
            /**
             * Sort order to use. This can also be null, which will use the default sort
             * order. For [MediaStore.Images], the default sort order is ascending by date taken.
             */
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            getApplication<Application>().contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->

                /**
                 * In order to retrieve the data from the [Cursor] that's returned, we need to
                 * find which index matches each column that we're interested in.
                 *
                 * There are two ways to do this. The first is to use the method
                 * [Cursor.getColumnIndex] which returns -1 if the column ID isn't found. This
                 * is useful if the code is programmatically choosing which columns to request,
                 * but would like to use a single method to parse them into objects.
                 *
                 * In our case, since we know exactly which columns we'd like, and we know
                 * that they must be included (since they're all supported from API 1), we'll
                 * use [Cursor.getColumnIndexOrThrow]. This method will throw an
                 * [IllegalArgumentException] if the column named isn't found.
                 *
                 * In either case, while this method isn't slow, we'll want to cache the results
                 * to avoid having to look them up for each row.
                 */
                /**
                 * In order to retrieve the data from the [Cursor] that's returned, we need to
                 * find which index matches each column that we're interested in.
                 *
                 * There are two ways to do this. The first is to use the method
                 * [Cursor.getColumnIndex] which returns -1 if the column ID isn't found. This
                 * is useful if the code is programmatically choosing which columns to request,
                 * but would like to use a single method to parse them into objects.
                 *
                 * In our case, since we know exactly which columns we'd like, and we know
                 * that they must be included (since they're all supported from API 1), we'll
                 * use [Cursor.getColumnIndexOrThrow]. This method will throw an
                 * [IllegalArgumentException] if the column named isn't found.
                 *
                 * In either case, while this method isn't slow, we'll want to cache the results
                 * to avoid having to look them up for each row.
                 */
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateModifiedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val displayNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val widthColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

                Log.i(TAG, "Found ${cursor.count} images")
                while (cursor.moveToNext()) {

                    // Here we'll use the column indexs that we found above.
                    val id = cursor.getLong(idColumn)
                    val dateModified =
                        Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateModifiedColumn)))
                    val displayName = cursor.getString(displayNameColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)

                    /**
                     * This is one of the trickiest parts:
                     *
                     * Since we're accessing images (using
                     * [MediaStore.Images.Media.EXTERNAL_CONTENT_URI], we'll use that
                     * as the base URI and append the ID of the image to it.
                     *
                     * This is the exact same way to do it when working with [MediaStore.Video] and
                     * [MediaStore.Audio] as well. Whatever `Media.EXTERNAL_CONTENT_URI` you
                     * query to get the items is the base, and the ID is the document to
                     * request there.
                     */
                    /**
                     * This is one of the trickiest parts:
                     *
                     * Since we're accessing images (using
                     * [MediaStore.Images.Media.EXTERNAL_CONTENT_URI], we'll use that
                     * as the base URI and append the ID of the image to it.
                     *
                     * This is the exact same way to do it when working with [MediaStore.Video] and
                     * [MediaStore.Audio] as well. Whatever `Media.EXTERNAL_CONTENT_URI` you
                     * query to get the items is the base, and the ID is the document to
                     * request there.
                     */
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val image = MediaStoreImage(id, displayName, dateModified, contentUri, width = width, height = height)

                    Log.v("WLAP", width.toString() + " " + height.toString())
                    images += image

                    // For debugging, we'll output the image objects we create to logcat.
                    Log.v(TAG, "Added image: $image")
                }
            }
        }

        Log.v(TAG, "Found ${images.size} images")
        return images
    }

    private suspend fun performDeleteImage(image: MediaStoreImage) {
        withContext(Dispatchers.IO) {
            try {
                /**
                 * In [Build.VERSION_CODES.Q] and above, it isn't possible to modify
                 * or delete items in MediaStore directly, and explicit permission
                 * must usually be obtained to do this.
                 *
                 * The way it works is the OS will throw a [RecoverableSecurityException],
                 * which we can catch here. Inside there's an [IntentSender] which the
                 * activity can use to prompt the user to grant permission to the item
                 * so it can be either updated or deleted.
                 */
                /**
                 * In [Build.VERSION_CODES.Q] and above, it isn't possible to modify
                 * or delete items in MediaStore directly, and explicit permission
                 * must usually be obtained to do this.
                 *
                 * The way it works is the OS will throw a [RecoverableSecurityException],
                 * which we can catch here. Inside there's an [IntentSender] which the
                 * activity can use to prompt the user to grant permission to the item
                 * so it can be either updated or deleted.
                 */

                val pendingIntent = MediaStore.createDeleteRequest(getApplication<Application>().contentResolver, arrayListOf(image.contentUri))
//                getApplication<Activity>().startIntentSenderForResult(pendingIntent.intentSender, null, 0, 0, 0)

//                getApplication<Application>().contentResolver.delete(
//                    image.contentUri,
//                    "${MediaStore.Images.Media._ID} = ?",
//                    arrayOf(image.id.toString())
//                )
            } catch (securityException: SecurityException) {
                val recoverableSecurityException =
                    securityException as? RecoverableSecurityException
                        ?: throw securityException

                // Signal to the Activity that it needs to request permission and
                // try the delete again if it succeeds.
                pendingDeleteImage = image
                _permissionNeededForDelete.postValue(
                    recoverableSecurityException.userAction.actionIntent.intentSender
                )
            }
        }
    }

    /**
     * Convenience method to convert a day/month/year date into a UNIX timestamp.
     *
     * We're suppressing the lint warning because we're not actually using the date formatter
     * to format the date to display, just to specify a format to use to parse it, and so the
     * locale warning doesn't apply.
     */
    @Suppress("SameParameterValue")
    @SuppressLint("SimpleDateFormat")
    private fun dateToTimestamp(day: Int, month: Int, year: Int): Long =
        SimpleDateFormat("dd.MM.yyyy").let { formatter ->
            TimeUnit.MICROSECONDS.toSeconds(formatter.parse("$day.$month.$year")?.time ?: 0)
        }

    /**
     * Since we register a [ContentObserver], we want to unregister this when the `ViewModel`
     * is being released.
     */
    override fun onCleared() {
        contentObserver?.let {
            getApplication<Application>().contentResolver.unregisterContentObserver(it)
        }
    }
}

/**
 * Convenience extension method to register a [ContentObserver] given a lambda.
 */
private fun ContentResolver.registerObserver(
    uri: Uri,
    observer: (selfChange: Boolean) -> Unit
): ContentObserver {
    val contentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            observer(selfChange)
        }
    }
    registerContentObserver(uri, true, contentObserver)
    return contentObserver
}


private const val TAG = "HistoryActivityVM"