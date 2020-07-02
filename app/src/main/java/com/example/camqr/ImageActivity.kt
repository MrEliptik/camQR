package com.example.camqr

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Half.EPSILON
import android.util.Half.MIN_VALUE
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
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
                        /*
                        val scaledRect = scaleRect(bounds!!, image, offset = false)
                        val croppedBmp: Bitmap = Bitmap.createBitmap(
                            image.bitmapInternal!!,
                            scaledRect!!.left.toInt(),
                            scaledRect!!.top.toInt(),
                            scaledRect.right.toInt(),
                            scaledRect.bottom.toInt()
                        )
                        */

                        entry.put("Image", image.bitmapInternal)


                        listCodes.put(entry)
                        barcodesList.add(hash)
                        adapter.notifyDataSetChanged()
                        //val imageButton: ImageButton = (c as Activity).findViewById<View>(R.id.clear_btn) as ImageButton
                        //imageButton.visibility = View.VISIBLE
                    }

                    if (bounds != null) {
                        val scaledRect = scaleRect(bounds, image, offset = false)
                        drawAreaImage.setRectangles(scaledRect, color)
                    }
                }
            }
            .addOnFailureListener {
                Log.d("DEBUG", "FAIL")
                // Task failed with an exception
                /*
                Log.d("CODE", it.toString())
                val textView: TextView = (c as Activity).findViewById<View>(R.id.helper) as TextView
                textView.visibility = View.VISIBLE
                val imageButton: ImageButton = (c as Activity).findViewById<View>(R.id.clear_btn) as ImageButton
                imageButton.visibility = View.INVISIBLE
                */
            }
    }

    fun clearElement(pos: Int) {
        listCodes.remove(pos)
        // Use removeAt instead of remove, as remove can be used with an object
        // and an Int is an object compared to primitive type int
        barcodesList.removeAt(pos)
        adapter.notifyDataSetChanged()
    }

    fun addElement(item: JSONObject) {
        listCodes.put(item)
        barcodesList.add(item.get("Hash") as Int)
        adapter.notifyDataSetChanged()
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

    private fun scaleRect(rect: Rect, image: InputImage, offset: Boolean = false): RectF {
        val wFactor = image_view.width.toFloat()/image.height
        val hFactor = image_view.height.toFloat()/image.width

        val scaledRect = RectF(rect)
        if(offset){
            scaledRect.left = (rect.left * wFactor) - OFFSET
            scaledRect.top = (rect.top * hFactor) - OFFSET
            scaledRect.right = (rect.right * wFactor) + OFFSET
            scaledRect.bottom = (rect.bottom * hFactor) + OFFSET
        }
        else {
            scaledRect.left = (rect.left * wFactor)
            if (scaledRect.left < 0) scaledRect.left = 0F
            scaledRect.top = (rect.top * hFactor)
            if (scaledRect.top < 0) scaledRect.top = 0F
            scaledRect.right = (rect.right * wFactor)
            if (scaledRect.right < 0) scaledRect.right = 0F
            scaledRect.bottom = (rect.bottom * hFactor)
            if (scaledRect.bottom < 0) scaledRect.bottom = 0F
        }


        return scaledRect
    }

    companion object {
        private const val OFFSET = 20
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
            clearElement(idx)
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
}