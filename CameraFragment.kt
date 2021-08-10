package com.example.animalApp.camera_screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import com.example.animalApp.R
import com.example.animalApp.databinding.FragmentCameraBinding
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment(), CameraProvider.ViewProvider,
    ScaleGestureDetector.OnScaleGestureListener {

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private lateinit var mBinding: FragmentCameraBinding
    private lateinit var mCameraProvider: ProcessCameraProvider
    private lateinit var mCamera: Camera
    private lateinit var mPresenter: CameraPresenter
    private var imageCapture: ImageCapture? = null
    private lateinit var mCameraExecutor: ExecutorService
    private lateinit var mScaleGestureDetector: ScaleGestureDetector

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_camera, container, false)
        return mBinding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mPresenter = CameraPresenter(requireContext(), this)
        mBinding.presenter = mPresenter

        mBinding.toolCamera.setupWithNavController(
            findNavController(),
            AppBarConfiguration(findNavController().graph)
        )
        mBinding.toolCamera.setNavigationIcon(R.drawable.ic_close)

        mCameraExecutor = Executors.newSingleThreadExecutor()
        mScaleGestureDetector = ScaleGestureDetector(requireContext(), this)
        mPresenter.onPermissionGranted(allPermissionsGranted())

        mBinding.viewFinder.setOnTouchListener { _, event ->
            mScaleGestureDetector.onTouchEvent(event)
            return@setOnTouchListener true
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            mPresenter.onUserSetPermission(grantResults[0] == PackageManager.PERMISSION_GRANTED)
        }
    }

    override fun capturePhoto(outputDirectory: File) {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    showError(exc.localizedMessage!!)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    mPresenter.onImageSaved(savedUri)
                }
            })
    }


    override fun initCamera(isBack: Boolean) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            mCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(mBinding.viewFinder.createSurfaceProvider())
                }
            imageCapture = ImageCapture.Builder()
                .build()
            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(mCameraExecutor, LuminosityAnalyzer { luma ->
                        Log.d("TAG", "Average luminosity: $luma")
                    })
                }

            val cameraSelector =
                if (isBack) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                mCameraProvider.unbindAll()

                mCamera = mCameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("TAG", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun stopCamera() {
        if (this::mCameraExecutor.isInitialized) {
            mCameraExecutor.shutdown()
        }
    }

    override fun showPreviousScreen() {
        activity?.runOnUiThread {
            findNavController().popBackStack()
        }
    }

    override fun showCalculateScreen(path: String, indexFrom: Int) {
        activity?.runOnUiThread {
            findNavController().navigate(
                CameraFragmentDirections.actionCameraFragmentToConfirmationFragment(
                    path,
                    indexFrom
                )
            )
        }
    }

    override fun showError(error: String) {
        showMessage(error)
    }

    override fun showMessage(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun showAnimation() {}

    override fun hideAnimation() {}

    override fun onDestroy() {
        super.onDestroy()
        if (this::mCameraExecutor.isInitialized) {
            mCameraExecutor.shutdown()
        }
    }


    private class LuminosityAnalyzer(private val listener: (luma: Double) -> Unit) :
        ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()
            val data = ByteArray(remaining())
            get(data)
            return data
        }

        override fun analyze(image: ImageProxy) {
            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()
            listener(luma)
            image.close()
        }
    }

    override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector?) {

    }

    @SuppressLint("RestrictedApi")
    override fun onScale(detector: ScaleGestureDetector?): Boolean {
        val scale = mCamera.cameraInfo.zoomState.value!!.zoomRatio * detector!!.scaleFactor
        mCamera.cameraControl.setZoomRatio(scale)
        return true
    }
}