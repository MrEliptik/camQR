package com.example.camqr

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    lateinit var scanner: BarcodeScanner

    private var listCodes: JSONArray = JSONArray()
    private lateinit var adapter: CodesAdapter

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private var barcodesList: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        //set content view AFTER ABOVE sequence (to avoid crash)
        setContentView(R.layout.activity_main)

        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post {
                startCamera()
            }
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Chose what format will be discovered
        val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_AZTEC,
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_CODE_39,
                        Barcode.FORMAT_CODE_93,
                        Barcode.FORMAT_CODABAR,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_ITF,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_PDF417,
                        Barcode.FORMAT_DATA_MATRIX
                )
                .build()

        scanner = BarcodeScanning.getClient(options)

        adapter = CodesAdapter(this, listCodes)
        codes_list_view.adapter = adapter
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults:
            IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post {
                    startCamera()
                }
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(viewFinder.display.rotation)
                .build()

            // Image analysis
            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                //.setTargetResolution(Size(1280, 720))
                .setTargetResolution(Size(1080, 1920)) // width, height
                .setTargetRotation(viewFinder.display.rotation)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QRAnalyser(this, viewFinder, drawArea, codes_list_view, adapter, listCodes, scanner))
                }

            // Select back camera
            val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalyzer)
                //preview?.setSurfaceProvider(viewFinder.createSurfaceProvider(camera?.cameraInfo))
                preview?.setSurfaceProvider(viewFinder.createSurfaceProvider())
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private class QRAnalyser(private val c: Context, private val viewFinder:PreviewView, private val drawArea:DrawView,
                             private var codes_list_view: ListView, private var adapter: CodesAdapter,
                             private var listCodes: JSONArray, private val scanner: BarcodeScanner)
        : ImageAnalysis.Analyzer {

        @SuppressLint("UnsafeExperimentalUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage: Image? = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                // Pass image to an ML Kit Vision API
                val result = scanner.process(image)
                        .addOnSuccessListener { barcodes ->

                            // Clear previous rectangles and codes
                            drawArea.clearRectangles()
                            listCodes = JSONArray()

                            // Task completed successfully
                            for (barcode in barcodes) {

                                val bounds = barcode.boundingBox
                                val corners = barcode.cornerPoints

                                val rawValue = barcode.rawValue

                                val color = Color.argb(
                                    255,
                                    Random.nextInt(256),
                                    Random.nextInt(256),
                                    Random.nextInt(256)
                                )

                                if (bounds != null) {
                                    val scaledRect = scaleRect(bounds, image)
                                    drawArea.setRectangles(scaledRect, color)
                                }

                                val entry = JSONObject()
                                entry.put("Type", decodeFormat(barcode.format))
                                entry.put("Value", barcode.rawValue)
                                entry.put("Color", color)

                                entry.put("Image", viewFinder.bitmap)
                                listCodes.put(entry)

                                // See API reference for complete list of supported types
                                when (barcode.valueType) {
                                    Barcode.TYPE_WIFI -> {
                                        val ssid = barcode.wifi!!.ssid
                                        val password = barcode.wifi!!.password
                                        val type = barcode.wifi!!.encryptionType
                                    }
                                    Barcode.TYPE_URL -> {
                                        val title = barcode.url!!.title
                                        val url = barcode.url!!.url
                                        Log.d("CODE", title.toString() + " " + url.toString())
                                    }
                                }
                            }

                            adapter = CodesAdapter(c, listCodes)
                            codes_list_view.adapter = adapter

                            adapter.notifyDataSetChanged()

                            imageProxy.close()
                        }
                        .addOnFailureListener {
                            // Task failed with an exception
                            Log.d("CODE", it.toString())
                            imageProxy.close()
                        }
                //
            }
        }

        private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
            val buffer: ByteBuffer = image.planes[0].buffer
            buffer.rewind()
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

        private fun decodeFormat(format: Int): String? {
            return when (format) {
                Barcode.FORMAT_CODE_128 -> "CODE 128"
                Barcode.FORMAT_CODE_39 -> "CODE 39"
                Barcode.FORMAT_CODE_93 -> "CODE 93"
                Barcode.FORMAT_CODABAR -> "CODABAR"
                Barcode.FORMAT_DATA_MATRIX -> "DATA MATRIX"
                Barcode.FORMAT_EAN_13 -> "EAN13"
                Barcode.FORMAT_EAN_8 -> "EAN8"
                Barcode.FORMAT_ITF -> "ITF"
                Barcode.FORMAT_QR_CODE -> "QR CODE"
                Barcode.FORMAT_UPC_A -> "UPCA"
                Barcode.FORMAT_UPC_E -> "UPCE"
                Barcode.FORMAT_PDF417 -> "PDF417"
                Barcode.FORMAT_AZTEC -> "AZTEC"
                else -> ""
            }
        }

        private fun scaleRect(rect: Rect, image: InputImage): RectF {
            val w = drawArea.width
            val h = drawArea.height
            val wFactor = drawArea.width.toFloat()/image.height
            val hFactor = drawArea.height.toFloat()/image.width

            val scaledRect = RectF(rect)
            scaledRect.left = (rect.left * wFactor).toFloat()
            scaledRect.top = (rect.top * hFactor).toFloat()
            scaledRect.right = (rect.right * wFactor).toFloat()
            scaledRect.bottom = (rect.bottom * hFactor).toFloat()

            return scaledRect
        }
    }
}

