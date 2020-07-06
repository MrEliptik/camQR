package com.example.camqr

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.ExifInterface
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.android.synthetic.main.activity_image.*
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.random.Random


class MainActivity : AppCompatActivity(), GestureDetector.OnGestureListener {

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    lateinit var scanner: BarcodeScanner

    private var listCodes: JSONArray = JSONArray()
    private lateinit var adapter: CodesAdapter

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var analyser: QRAnalyser

    enum class FLASH_MODE {
        ON,
        OFF
    }
    private var flashState = FLASH_MODE.OFF

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

        clear_btn.setOnClickListener {
            analyser.clearList()
        }

        exit_btn.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id ->
                    val homeIntent = Intent(Intent.ACTION_MAIN)
                    homeIntent.addCategory(Intent.CATEGORY_HOME)
                    homeIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(homeIntent)
                }
                .setNegativeButton("No", null)
                .show()
        }

        flash_btn.setOnClickListener {
            if (flashState == FLASH_MODE.OFF) {
                flashState = FLASH_MODE.ON
                flash_btn.setImageResource(R.drawable.flash_on_24)
                flash_btn.invalidate()
                if (camera!!.cameraInfo.hasFlashUnit()) {
                    camera!!.cameraControl.enableTorch(true)
                }
            }
            else if(flashState == FLASH_MODE.ON){
                flashState = FLASH_MODE.OFF
                flash_btn.setImageResource(R.drawable.flash_off_24)
                flash_btn.invalidate()
                if (camera!!.cameraInfo.hasFlashUnit()) {
                    camera!!.cameraControl.enableTorch(false)
                }
            }
        }

        gallery_btn.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_SELECT_IMAGE_IN_ALBUM)
            }
            else{
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                if (intent.resolveActivity(packageManager) != null) {
                    startActivityForResult(intent, REQUEST_SELECT_IMAGE_IN_ALBUM)
                }
            }
        }

        val detector = GestureDetector(this, this)
        codes_list_view.setOnTouchListener { view, e ->
            detector.onTouchEvent(e)
            false
        }
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setMessage("Are you sure you want to exit?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                val homeIntent = Intent(Intent.ACTION_MAIN)
                homeIntent.addCategory(Intent.CATEGORY_HOME)
                homeIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(homeIntent)
            }
            .setNegativeButton("No", null)
            .show()
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

            analyser = QRAnalyser(this, viewFinder, drawArea, codes_list_view, adapter, listCodes, scanner)

            // Image analysis
            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                //.setTargetResolution(Size(1280, 720))
                .setTargetResolution(Size(1080, 1920)) // width, height
                .setTargetRotation(viewFinder.display.rotation)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, analyser)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_SELECT_IMAGE_IN_ALBUM){
            // Switch to resultActivity
            val i = Intent(applicationContext, ImageActivity::class.java)
            i.putExtra("imageUri", data?.data.toString())
            startActivity(i)
        }
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val REQUEST_SELECT_IMAGE_IN_ALBUM = 5
        private const val SWIPE_MIN_DISTANCE = 120
        private const val SWIPE_MAX_OFF_PATH = 250
        private const val SWIPE_THRESHOLD_VELOCITY = 200
    }

    private class QRAnalyser(private val c: Context, private val viewFinder:PreviewView, private val drawArea:DrawView,
                             private var codes_list_view: ListView, private var adapter: CodesAdapter,
                             private var listCodes: JSONArray, private val scanner: BarcodeScanner)
        : ImageAnalysis.Analyzer {

        private var barcodesList: ArrayList<Int> = ArrayList()

        @SuppressLint("UnsafeExperimentalUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage: Image? = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                // Pass image to an ML Kit Vision API
                val result = scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            val textView: TextView = (c as Activity).findViewById<View>(R.id.helper) as TextView

                            // Clear previous rectangles and codes
                            drawArea.clearRectangles()
                            //listCodes = JSONArray()

                            if (barcodes.size > 0) {
                                textView.visibility = View.INVISIBLE
                            }
                            else {
                                textView.visibility = View.VISIBLE
                            }

                            // Task completed successfully
                            for (barcode in barcodes) {

                                val color = Color.argb(
                                    255,
                                    Random.nextInt(256),
                                    Random.nextInt(256),
                                    Random.nextInt(256)
                                )

                                val bounds = barcode.boundingBox

                                val rawValue = barcode.rawValue

                                val hash = (decodeFormat(barcode.format) + rawValue).hashCode()
                                if (hash !in barcodesList) {
                                    val entry = JSONObject()
                                    entry.put("Hash", hash)
                                    entry.put("Type", barcode.format)
                                    entry.put("QRType", barcode.valueType)
                                    entry.put("Value", barcode.rawValue)
                                    entry.put("Color", color)

                                    // See API reference for complete list of supported types
                                    when (barcode.valueType) {
                                        Barcode.TYPE_WIFI -> {
                                            val ssid = barcode.wifi!!.ssid
                                            val password = barcode.wifi!!.password
                                            val type = barcode.wifi!!.encryptionType
                                            entry.put("SSID", ssid)
                                            entry.put("Password", password)
                                            entry.put("Encryption", type)
                                        }
                                        Barcode.TYPE_URL -> {
                                            val title = barcode.url!!.title
                                            val url = barcode.url!!.url
                                            entry.put("Title", title)
                                            entry.put("Url", url)
                                        }
                                        Barcode.TYPE_CONTACT_INFO -> {
                                            val addr = barcode.contactInfo!!.addresses
                                            val emails = barcode.contactInfo!!.emails
                                            val name = barcode.contactInfo!!.name
                                            val org = barcode.contactInfo!!.organization
                                            val phones = barcode.contactInfo!!.phones
                                            val title = barcode.contactInfo!!.title
                                            val urls = barcode.contactInfo!!.urls
                                            entry.put("Addresses", addr)
                                            entry.put("Emails", emails)
                                            entry.put("Name", name)
                                            entry.put("Organization", org)
                                            entry.put("Phones", phones)
                                            entry.put("Title", title)
                                            entry.put("Urls", urls)
                                        }
                                        Barcode.TYPE_CALENDAR_EVENT -> {
                                            val desc = barcode.calendarEvent!!.description
                                            val start = barcode.calendarEvent!!.start
                                            val end = barcode.calendarEvent!!.end
                                            val loc = barcode.calendarEvent!!.location
                                            val org = barcode.calendarEvent!!.organizer
                                            val stat = barcode.calendarEvent!!.status
                                            val sum = barcode.calendarEvent!!.summary
                                            entry.put("Description", desc)
                                            entry.put("Start", start)
                                            entry.put("End", end)
                                            entry.put("Location", loc)
                                            entry.put("Organizer", org)
                                            entry.put("Status", stat)
                                            entry.put("Summary", sum)
                                        }
                                        Barcode.TYPE_EMAIL -> {
                                            val addr = barcode.email!!.address
                                            val body = barcode.email!!.body
                                            val sub = barcode.email!!.subject
                                            val type = barcode.email!!.type // HOME, UNKNOWN, WORK
                                            entry.put("Address", addr)
                                            entry.put("Body", body)
                                            entry.put("Subject", sub)
                                            entry.put("Type", type)
                                        }
                                        Barcode.TYPE_GEO -> {
                                            val lat = barcode.geoPoint!!.lat
                                            val lng = barcode.geoPoint!!.lng
                                            entry.put("Latitude", lat)
                                            entry.put("Longitude", lng)
                                        }
                                        Barcode.TYPE_PHONE -> {
                                            val num = barcode.phone!!.number
                                            val type = barcode.phone!!.type
                                            entry.put("Number", num)
                                            entry.put("Type", type)
                                        }
                                        Barcode.TYPE_SMS -> {
                                            val msg = barcode.sms!!.message
                                            val num = barcode.sms!!.phoneNumber
                                            entry.put("Message", msg)
                                            entry.put("Number", num)
                                        }
                                        Barcode.TYPE_TEXT, Barcode.TYPE_PRODUCT -> {
                                            val txt = barcode.rawValue
                                        }
                                    }

                                    // CameraX api should always return image as YUV_420_888
                                    if (mediaImage.format == ImageFormat.YUV_420_888) {

                                        val bitmap = mediaImage.toBitmap()
                                        // -> Some device have the sensor in landscape mode
                                        // meaning the image should be rotated for OCR
                                        var rotatedBitmap : Bitmap? = null
                                        when (imageProxy.imageInfo.rotationDegrees) {
                                            90 -> rotatedBitmap = rotateImage(bitmap, 90F)
                                            180 -> rotatedBitmap = rotateImage(bitmap, 180F)
                                            270 -> rotatedBitmap = rotateImage(bitmap, 270F)
                                            else -> rotatedBitmap = bitmap
                                        }

                                        val croppedBmp: Bitmap = Bitmap.createBitmap(
                                            rotatedBitmap!!,
                                            bounds!!.left,
                                            bounds!!.top,
                                            bounds!!.width(),
                                            bounds!!.height()
                                        )

                                        entry.put("Image", croppedBmp)
                                    }

                                    listCodes.put(entry)
                                    barcodesList.add(hash)
                                    adapter.notifyDataSetChanged()
                                    val imageButton: ImageButton = c.findViewById<View>(R.id.clear_btn) as ImageButton
                                    imageButton.visibility = View.VISIBLE
                                }

                                if (bounds != null) {
                                    val scaledRect = scaleRect(bounds, image, offset = true)
                                    drawArea.setRectangles(scaledRect, color)
                                }
                            }

                            imageProxy.close()
                        }
                        .addOnFailureListener {
                            // Task failed with an exception
                            Log.d("CODE", it.toString())
                            val textView: TextView = (c as Activity).findViewById<View>(R.id.helper) as TextView
                            textView.visibility = View.VISIBLE
                            val imageButton: ImageButton = (c as Activity).findViewById<View>(R.id.clear_btn) as ImageButton
                            imageButton.visibility = View.INVISIBLE
                            imageProxy.close()
                        }
            }
        }

        private fun rotateImage(source: Bitmap, angle: Float): Bitmap? {
            val matrix = Matrix()
            matrix.postRotate(angle)
            return Bitmap.createBitmap(
                source, 0, 0, source.width, source.height,
                matrix, true
            )
        }

        fun Image.toBitmap(): Bitmap {
            val yBuffer = planes[0].buffer // Y
            val uBuffer = planes[1].buffer // U
            val vBuffer = planes[2].buffer // V

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            //U and V are swapped
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
            val imageBytes = out.toByteArray()
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
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

        private fun scaleRectForBitmap(rect: Rect, image: InputImage): RectF {

            val scaledRect = RectF(rect)

            return scaledRect
        }

        private fun scaleRectForDrawing(rect: Rect, image: InputImage): RectF {

            val scaledRect = RectF(rect)

            return scaledRect
        }

        private fun scaleRect(rect: Rect, image: InputImage, offset: Boolean = false): RectF {
            val wFactor = drawArea.width.toFloat()/image.height
            val hFactor = drawArea.height.toFloat()/image.width

            val scaledRect = RectF(rect)
            if(offset){
                scaledRect.left = (rect.left * wFactor) - OFFSET
                scaledRect.top = (rect.top * hFactor) - OFFSET
                scaledRect.right = (rect.right * wFactor) + OFFSET
                scaledRect.bottom = (rect.bottom * hFactor) + OFFSET
            }
            else {
                scaledRect.left = (rect.left * wFactor)
                scaledRect.top = (rect.top * hFactor)
                scaledRect.right = (rect.right * wFactor)
                scaledRect.bottom = (rect.bottom * hFactor)
            }


            return scaledRect
        }

        fun clearList() {
            listCodes = JSONArray()
            adapter = CodesAdapter(c, listCodes)
            codes_list_view.adapter = adapter
            adapter.notifyDataSetChanged()

            barcodesList.clear()

            val imageButton: ImageButton = (c as Activity).findViewById<View>(R.id.clear_btn) as ImageButton
            imageButton.visibility = View.INVISIBLE
        }

        fun clearElement(pos: Int, view: View?, direction: String) {
            var animation: Animation
            if (direction == "left"){
                animation = AnimationUtils.loadAnimation(c, R.anim.slide_left)
            }
            else{
                animation = AnimationUtils.loadAnimation(c, R.anim.slide_right)
            }
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    listCodes.remove(pos)
                    // Use removeAt instead of remove, as remove can be used with an object
                    // and an Int is an object compared to primitive type int
                    barcodesList.removeAt(pos)
                    adapter.notifyDataSetChanged()
                    if (barcodesList.size == 0) {
                        val textView: TextView = (c as Activity).findViewById<View>(R.id.helper) as TextView
                        textView.visibility = View.VISIBLE
                        val imageButton: ImageButton = (c as Activity).findViewById<View>(R.id.clear_btn) as ImageButton
                        imageButton.visibility = View.INVISIBLE
                    }
                }
            })
            view?.startAnimation(animation)
        }

        fun addElement(item: JSONObject) {
            listCodes.put(item)
            barcodesList.add(item.get("Hash") as Int)
            adapter.notifyDataSetChanged()
            if (barcodesList.size > 0) {
                val textView: TextView = (c as Activity).findViewById<View>(R.id.helper) as TextView
                textView.visibility = View.INVISIBLE
                val imageButton: ImageButton = (c as Activity).findViewById<View>(R.id.clear_btn) as ImageButton
                imageButton.visibility = View.VISIBLE
            }
        }

        companion object {
            private const val OFFSET = 20
        }
    }

    override fun onShowPress(p0: MotionEvent?) {
        //Toast.makeText(this, "onShowPress", Toast.LENGTH_SHORT).show()
    }

    override fun onSingleTapUp(p0: MotionEvent?): Boolean {
        //Toast.makeText(this, "onSingleTapUp", Toast.LENGTH_SHORT).show()
        return false;
    }

    override fun onDown(p0: MotionEvent?): Boolean {
        //Toast.makeText(this, "onDown", Toast.LENGTH_SHORT).show()
        return false
    }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        try {
            val idx = codes_list_view.pointToPosition(Math.round(p0!!.x), Math.round(p1!!.y))
            val item = adapter.getItem(idx) as JSONObject
            // right to left swipe
            if (p0!!.x - p1!!.x > SWIPE_MIN_DISTANCE
                && Math.abs(p2) > SWIPE_THRESHOLD_VELOCITY) {
                analyser.clearElement(idx, getViewByPosition(idx, codes_list_view), "left")
            }
            // left to right swipe
            else if (p1.x - p0.x > SWIPE_MIN_DISTANCE
                && Math.abs(p2) > SWIPE_THRESHOLD_VELOCITY) {
                analyser.clearElement(idx, getViewByPosition(idx, codes_list_view), "right")
            }

            Snackbar
                .make(mainLayout, "Item deleted", Snackbar.LENGTH_LONG)
                .setAction("Undo", View.OnClickListener {
                    analyser.addElement(item)
                    adapter.notifyDataSetChanged()
                    Snackbar.make(mainLayout, "Action undone", Snackbar.LENGTH_SHORT).show()
                }).show()
            // return super.onFling();
        } catch (e: java.lang.Exception) {
            // do nothing
        }
        return false

    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        //Toast.makeText(this, "onScroll", Toast.LENGTH_SHORT).show()
        return false
    }

    override fun onLongPress(p0: MotionEvent?) {
        //Toast.makeText(this, "onLongPress", Toast.LENGTH_SHORT).show()
    }

    fun getViewByPosition(pos: Int, listView: ListView): View? {
        val firstListItemPosition = listView.firstVisiblePosition
        val lastListItemPosition = firstListItemPosition + listView.childCount - 1
        return if (pos < firstListItemPosition || pos > lastListItemPosition) {
            listView.adapter.getView(pos, null, listView)
        } else {
            val childIndex = pos - firstListItemPosition
            listView.getChildAt(childIndex)
        }
    }
}

