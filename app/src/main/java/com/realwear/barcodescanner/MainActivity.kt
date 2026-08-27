package com.realwear.barcodescanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var txtResult: TextView
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null
    private var currentZoomRatio = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        txtResult = findViewById(R.id.txtResult)

        val btnZoomIn = findViewById<Button>(R.id.btnZoomIn)
        val btnZoomOut = findViewById<Button>(R.id.btnZoomOut)

        btnZoomIn.setOnClickListener {
            cameraInfo?.zoomState?.value?.let { zoomState ->
                if (currentZoomRatio < zoomState.maxZoomRatio) {
                    currentZoomRatio = (currentZoomRatio + 1.5f).coerceAtMost(zoomState.maxZoomRatio)
                    cameraControl?.setZoomRatio(currentZoomRatio)
                }
            }
        }

        btnZoomOut.setOnClickListener {
            if (currentZoomRatio > 1.0f) {
                currentZoomRatio = (currentZoomRatio - 1.5f).coerceAtLeast(1.0f)
                cameraControl?.setZoomRatio(currentZoomRatio)
            }
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val scanner = BarcodeScanning.getClient()
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                @Suppress("UnsafeOptInUsageError")
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            for (barcode in barcodes) {
                                txtResult.text = "Scanned: " + (barcode.rawValue ?: "")
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
                cameraControl = camera.cameraControl
                cameraInfo = camera.cameraInfo
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
