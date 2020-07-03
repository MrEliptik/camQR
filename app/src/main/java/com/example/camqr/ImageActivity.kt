package com.example.camqr

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
import kotlin.random.Random

class ImageActivity : AppCompatActivity(), GestureDetector.OnGestureListener {
    lateinit var scanner: BarcodeScanner

    private var listCodes: JSONArray = JSONArray()
    private lateinit var adapter: CodesAdapter

    private var barcodesList: ArrayList<Int> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        actionBar?.setDisplayHomeAsUpEnabled(true)

        setContentView(R.layout.activity_image)

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
        codes_list_view_image.adapter = adapter

        when (intent?.action) {
            // When coming from share event
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("image/") == true) {
                    handleSendImage(intent) // Handle single image being sent
                }
            }
            // When coming from other activity
            else -> {
                val extras = intent.extras
                if (extras != null) {
                    var imageUri = Uri.parse(extras.getString("imageUri"))
                    try {
                        image_view.setImageURI(imageUri)
                        scanImage(imageUri)
                    } catch (e: Throwable) {
                        image_view.setImageResource(R.drawable.gallery_24)
                    }
                }
            }
        }

        val detector = GestureDetector(this, this)
        codes_list_view_image.setOnTouchListener { view, e ->
            detector.onTouchEvent(e)
            false
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun handleSendImage(intent: Intent) {
        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
            // Update UI to reflect image being shared
            image_view.setImageURI(it)
            scanImage(it)
        }
    }

    private fun scanImage(uri: Uri){
        val image = InputImage.fromFilePath(this, uri)
        val result = scanner.process(image)
            .addOnSuccessListener { barcodes ->

                // Clear previous rectangles and codes
                drawAreaImage.clearRectangles()

                if (barcodes.size > 0) {

                }
                else {

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

                        //val scaledRect = scaleRect(bounds!!, image, offset = false)
                        val posInImageView = getBitmapPositionInsideImageView(image_view)
                        val scaledRect = scaleRectForBitmap(bounds!!, posInImageView!!)
                        val croppedBmp: Bitmap = Bitmap.createBitmap(
                            image.bitmapInternal!!,
                            scaledRect!!.left.toInt(),
                            scaledRect!!.top.toInt(),
                            scaledRect.width().toInt(),
                            scaledRect.height().toInt()
                        )

                        //entry.put("Image", image.bitmapInternal)
                        entry.put("Image", croppedBmp)

                        listCodes.put(entry)
                        barcodesList.add(hash)
                        adapter.notifyDataSetChanged()
                        //val imageButton: ImageButton = (c as Activity).findViewById<View>(R.id.clear_btn) as ImageButton
                        //imageButton.visibility = View.VISIBLE
                    }

                    if (bounds != null) {
                        val posInImageView = getBitmapPositionInsideImageView(image_view)
                        val scaledRect = scaleRectForDrawing(bounds!!, posInImageView!!)
                        val offsetScaledRect = RectF(
                            (scaledRect.left + posInImageView!![0]),
                            (scaledRect.top + posInImageView!![1]),
                            (scaledRect.right + posInImageView!![0]),
                            (scaledRect.bottom + posInImageView!![1])
                        )

                        drawAreaImage.setRectangles(offsetScaledRect, color)
                    }
                }
            }
            .addOnFailureListener {
                Log.d("DEBUG", "FAIL")
                // Task failed with an exception
            }
    }

    fun clearElement(pos: Int, view: View?, direction: String) {
        var animation: Animation
        if (direction == "left"){
            animation = AnimationUtils.loadAnimation(this, R.anim.slide_left)
        }
        else{
            animation = AnimationUtils.loadAnimation(this, R.anim.slide_right)
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
            }
        })
        view?.startAnimation(animation)
    }

    fun addElement(item: JSONObject) {
        listCodes.put(item)
        barcodesList.add(item.get("Hash") as Int)
        adapter.notifyDataSetChanged()
    }

    /**
     * Returns the bitmap position inside an imageView.
     * @param imageView source ImageView
     * @return 0: left, 1: top, 2: width, 3: height
     */
    fun getBitmapPositionInsideImageView(imageView: ImageView?): IntArray? {
        val ret = IntArray(4)
        if (imageView?.drawable == null) return ret

        // Get image dimensions
        // Get image matrix values and place them in an array
        val f = FloatArray(9)
        imageView.imageMatrix.getValues(f)

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        val scaleX = f[Matrix.MSCALE_X]
        val scaleY = f[Matrix.MSCALE_Y]

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        val d: Drawable = imageView.drawable
        val origW: Int = d.intrinsicWidth
        val origH: Int = d.intrinsicHeight

        // Calculate the actual dimensions
        val actW = Math.round(origW * scaleX)
        val actH = Math.round(origH * scaleY)
        ret[2] = actW
        ret[3] = actH

        // Get image position
        // We assume that the image is centered into ImageView
        val imgViewW: Int = imageView.width
        val imgViewH: Int = imageView.height
        val top = (imgViewH - actH) / 2
        val left = (imgViewW - actW) / 2
        ret[0] = left
        ret[1] = top
        return ret
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

    private fun scaleRectForBitmap(rect: Rect, pos: IntArray): RectF {
        val imViewWidth = pos[2]
        val imViewHeight = pos[3]

        val imRealWidth = image_view.drawable.intrinsicWidth
        val imRealHeight = image_view.drawable.intrinsicHeight

        if (imViewHeight < imRealHeight && imViewWidth < imRealWidth){
            if (rect.left < 0) rect.left = 0
            if (rect.top < 0) rect.top = 0
            if (rect.right < 0) rect.right = 0
            if (rect.bottom < 0) rect.bottom = 0
            return RectF(rect)
        }

        val wFactor = imRealWidth.toFloat()/imViewWidth
        val hFactor = imRealHeight.toFloat()/imViewHeight

        val scaledRect = RectF(rect)

        scaledRect.left = (rect.left * wFactor)
        scaledRect.top = (rect.top * hFactor)
        scaledRect.right = (rect.right * wFactor)
        scaledRect.bottom = (rect.bottom * hFactor)

        if (wFactor > 1) {
            scaledRect.left = (rect.left * (1/wFactor))
            scaledRect.right = (rect.right * (1/wFactor))
        }
        if (hFactor > 1) {
            scaledRect.top = (rect.top * (1/hFactor))
            scaledRect.bottom = (rect.bottom * (1/hFactor))
        }
        if (scaledRect.left < 0) scaledRect.left = 0F
        if (scaledRect.top < 0) scaledRect.top = 0F
        if (scaledRect.right < 0) scaledRect.right = 0F
        if (scaledRect.bottom < 0) scaledRect.bottom = 0F

        return scaledRect
    }

    private fun scaleRectForDrawing(rect: Rect, pos: IntArray): RectF {
        val imViewWidth = pos[2]
        val imViewHeight = pos[3]

        val imRealWidth = image_view.drawable.intrinsicWidth
        val imRealHeight = image_view.drawable.intrinsicHeight

        if (imViewHeight > imRealHeight && imViewWidth > imRealWidth){
            if (rect.left < 0) rect.left = 0
            if (rect.top < 0) rect.top = 0
            if (rect.right < 0) rect.right = 0
            if (rect.bottom < 0) rect.bottom = 0
            return RectF(rect)
        }

        val wFactor = imRealWidth.toFloat()/imViewWidth
        val hFactor = imRealHeight.toFloat()/imViewHeight

        val scaledRect = RectF(rect)

        scaledRect.left = (rect.left * wFactor)
        scaledRect.top = (rect.top * hFactor)
        scaledRect.right = (rect.right * wFactor)
        scaledRect.bottom = (rect.bottom * hFactor)

        if (wFactor > 1) {
            scaledRect.left = (rect.left * (1/wFactor))
            scaledRect.right = (rect.right * (1/wFactor))
        }
        if (hFactor > 1) {
            scaledRect.top = (rect.top * (1/hFactor))
            scaledRect.bottom = (rect.bottom * (1/hFactor))
        }
        if (scaledRect.left < 0) scaledRect.left = 0F
        if (scaledRect.top < 0) scaledRect.top = 0F
        if (scaledRect.right < 0) scaledRect.right = 0F
        if (scaledRect.bottom < 0) scaledRect.bottom = 0F

        return scaledRect
    }

    private fun scaleRect(rect: Rect, image: InputImage, offset: Boolean = false): RectF {
        if (image.height < image_view.width && image.width < image_view.height){
            if (rect.left < 0) rect.left = 0
            if (rect.top < 0) rect.top = 0
            if (rect.right < 0) rect.right = 0
            if (rect.bottom < 0) rect.bottom = 0
            return RectF(rect)
        }

        val wFactor = image_view.width.toFloat()/image.width
        val hFactor = image_view.height.toFloat()/image.height

        val scaledRect = RectF(rect)
        if(offset){
            scaledRect.left = (rect.left * wFactor) - OFFSET
            scaledRect.right = (rect.right * wFactor) + OFFSET
            scaledRect.top = (rect.top * hFactor) - OFFSET
            scaledRect.bottom = (rect.bottom * hFactor) + OFFSET
            if (wFactor > 1) {
                scaledRect.left = (rect.left * (1/wFactor)) - OFFSET
                scaledRect.right = (rect.right * (1/wFactor)) + OFFSET
            }
            if (hFactor > 1) {
                scaledRect.top = (rect.top * (1/hFactor)) - OFFSET
                scaledRect.bottom = (rect.bottom * (1/hFactor)) + OFFSET
            }

        }
        else {
            scaledRect.left = (rect.left * wFactor)
            scaledRect.top = (rect.top * hFactor)
            scaledRect.right = (rect.right * wFactor)
            scaledRect.bottom = (rect.bottom * hFactor)

            if (wFactor > 1) {
                scaledRect.left = (rect.left * (1/wFactor)) - OFFSET
                scaledRect.right = (rect.right * (1/wFactor)) + OFFSET
            }
            if (hFactor > 1) {
                scaledRect.top = (rect.top * (1/hFactor)) - OFFSET
                scaledRect.bottom = (rect.bottom * (1/hFactor)) + OFFSET
            }
            if (scaledRect.left < 0) scaledRect.left = 0F
            if (scaledRect.top < 0) scaledRect.top = 0F
            if (scaledRect.right < 0) scaledRect.right = 0F
            if (scaledRect.bottom < 0) scaledRect.bottom = 0F
        }


        return scaledRect
    }

    companion object {
        private const val OFFSET = 20
        private const val SWIPE_MIN_DISTANCE = 120
        private const val SWIPE_MAX_OFF_PATH = 250
        private const val SWIPE_THRESHOLD_VELOCITY = 200
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
            val idx = codes_list_view_image.pointToPosition(Math.round(p0!!.x), Math.round(p1!!.y))
            val item = adapter.getItem(idx) as JSONObject
            // right to left swipe
            if (p0!!.x - p1!!.x > SWIPE_MIN_DISTANCE
                && Math.abs(p2) > SWIPE_THRESHOLD_VELOCITY
            ) {
                clearElement(idx, getViewByPosition(idx, codes_list_view_image), "left")
            }
            // left to right swipe
            else if (p1.x - p0.x > SWIPE_MIN_DISTANCE
                && Math.abs(p2) > SWIPE_THRESHOLD_VELOCITY
            ) {
                clearElement(idx, getViewByPosition(idx, codes_list_view_image), "right")
            }

            Snackbar
                .make(main_image_layout, "Item deleted", Snackbar.LENGTH_LONG)
                .setAction("Undo", View.OnClickListener {
                    addElement(item)
                    adapter.notifyDataSetChanged()
                    Snackbar.make(main_image_layout, "Action undone", Snackbar.LENGTH_SHORT).show()
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