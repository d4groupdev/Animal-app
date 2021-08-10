package com.example.animalApp.choose_photo_screen

import android.annotation.SuppressLint
import android.app.Activity
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.impl.utils.Exif
import com.example.animalApp.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


class ChoosePhotoPresenter(activity: Activity, viewProvider: ChoosePhotoProvider.ViewProvider) {

    private val mViewProvider = viewProvider
    private val mActivity: Activity = activity
    private val mImagesPath = ArrayList<String>()
    private var mItemsShownCount = 0
    private var mPhotoImageCount = 0

    fun onShowCameraOpen() {
        mViewProvider.showCamera()
    }

    fun onSettingsClick() {
        mViewProvider.showSetting()
    }

    private fun getImagesPath() {
        mImagesPath.clear()
        val file = getOutputDirectory()
        if (file.exists()) {
            for (item in file.listFiles()!!) {
                mImagesPath.add(item.absolutePath)
            }
        }
        mPhotoImageCount = mImagesPath.size

        val projection = arrayOf(
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
            MediaStore.Images.ImageColumns.MIME_TYPE
        )
        val cursor: Cursor? = mActivity.contentResolver
            .query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC"
            )

        while (cursor!!.moveToNext()) {
            val imageLocation = cursor.getString(1)
            if (!mImagesPath.contains(imageLocation)) {
                val imageFile = File(imageLocation)
                if (imageFile.exists()) {
                    mImagesPath.add(imageFile.absolutePath)
                }
            }
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = mActivity.externalMediaDirs?.firstOrNull().let {
            File(it, mActivity.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir.exists())
            mediaDir else mActivity.filesDir
    }

    @SuppressLint("RestrictedApi")
    fun updateRecyclerView() {
        val imageList: ArrayList<Bitmap> = ArrayList()
        if (mImagesPath.size % 2 != 0 && mItemsShownCount == mImagesPath.lastIndex) {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true

            var bmp = BitmapFactory.decodeFile(mImagesPath[mImagesPath.lastIndex])
            imageList.add(Bitmap.createScaledBitmap(bmp, 320, 240, false))
            mViewProvider.setListViews(imageList)
            mItemsShownCount++
            return
        }
        var count = 0
        for (index in mItemsShownCount..mImagesPath.lastIndex) {
            Log.d("Path", "Image path: ${mImagesPath[index]}")
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            var bmp = BitmapFactory.decodeFile(mImagesPath[index])


            if (bmp == null || bmp.width <= 0) continue
            bmp = Bitmap.createScaledBitmap(bmp, 320, 240, false)
            val exif = Exif.createFromFile(File(mImagesPath[index]))
            val rotation = exif.rotation
            bmp = rotateImage(bmp, rotation.toFloat())

            imageList.add(bmp)
            count++
            if (count >= 5) {
                mItemsShownCount += 5
                break
            }
        }
        mViewProvider.setListViews(imageList)

    }

    fun onLastItemShowed() {
        if (mItemsShownCount >= mImagesPath.size) return
        GlobalScope.launch {
            mViewProvider.showAnimation()
            updateRecyclerView()
            mViewProvider.hideAnimation()
        }
    }

    fun onImageClick(position: Int) {
        mViewProvider.showNext(mImagesPath[position], if (mPhotoImageCount - 1 < position) 1 else 2)
    }

    fun onResume() {
        GlobalScope.launch {
            mViewProvider.showAnimation()
            mImagesPath.clear()
            mItemsShownCount = 0
            mViewProvider.clearRecyclerView()
            getImagesPath()
            updateRecyclerView()
            mViewProvider.hideAnimation()
        }
    }

    private fun rotateImage(img: Bitmap, degree: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degree)
        val rotatedImg =
            Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }
}