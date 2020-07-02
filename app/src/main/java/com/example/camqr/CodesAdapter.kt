package com.example.camqr

import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.net.*
import android.net.ConnectivityManager.NetworkCallback
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.os.Bundle
import android.os.PatternMatcher
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.Barcode.WiFi.*
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception


class CodesAdapter(private val context: Context,
                     private val dataSource: JSONArray
) : BaseAdapter() {

    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        // Get view for row item
        val rowView = inflater.inflate(R.layout.list_item_decoded, parent, false)

        // Get Image element
        val imageThumbnail = rowView.findViewById(R.id.thumbnail) as ImageView

        // Get title element
        val typeTextView = rowView.findViewById(R.id.history_list_type) as TextView

        // Get qr type element
        val qrTypeTextView = rowView.findViewById(R.id.history_list_qrtype) as TextView

        // Get subtitle element
        val valueTextView = rowView.findViewById(R.id.history_list_value) as TextView

        // Get color elem
        val colorBarView = rowView.findViewById(R.id.color_bar_view) as View

        // Get buttons
        val copyBtn = rowView.findViewById(R.id.copy_btn) as Button
        val shareBtn = rowView.findViewById(R.id.share_btn) as Button
        val gotoBtn = rowView.findViewById(R.id.goto_btn) as Button

        // Get QR type icon
        val qrTypeIcon = rowView.findViewById(R.id.qr_type_icon) as ImageView

        val item = getItem(position) as JSONObject

        try{
            val image = item.get("Image") as Bitmap?
            imageThumbnail.setImageBitmap(image)
        }
        catch (e: Exception){
            imageThumbnail.setImageResource(R.drawable.gallery_24)
        }
        val formatType = item.get("Type") as Int
        val qrType = item.get("QRType") as Int
        typeTextView.text = decodeFormat(formatType)
        qrTypeTextView.text = decodeQRType(qrType)
        valueTextView.text = item.get("Value").toString()

        when (qrType) {
            Barcode.TYPE_WIFI -> {
                gotoBtn.text = "Connect"
                qrTypeIcon.setImageResource(R.drawable.wifi_24)
            }
            Barcode.TYPE_URL -> {
                gotoBtn.text = "Browse"
                qrTypeIcon.setImageResource(R.drawable.url_24)
            }
            Barcode.TYPE_CONTACT_INFO -> {
                gotoBtn.text = "Add contact"
                qrTypeIcon.setImageResource(R.drawable.contact_24)
            }
            Barcode.TYPE_CALENDAR_EVENT -> {
                gotoBtn.text = "Add event"
                qrTypeIcon.setImageResource(R.drawable.calendar_24)
            }
            Barcode.TYPE_EMAIL -> {
                gotoBtn.text = "Send email"
                qrTypeIcon.setImageResource(R.drawable.email_24)
            }
            Barcode.TYPE_GEO -> {
                gotoBtn.text = "Navigate"
                qrTypeIcon.setImageResource(R.drawable.geo_24)
            }
            Barcode.TYPE_PHONE -> {
                gotoBtn.text = "Dial"
                qrTypeIcon.setImageResource(R.drawable.phone_24)
            }
            Barcode.TYPE_SMS -> {
                gotoBtn.text = "SMS"
                qrTypeIcon.setImageResource(R.drawable.sms_24)
            }
            Barcode.TYPE_TEXT, Barcode.TYPE_PRODUCT -> {
                gotoBtn.text = "Search web"
                qrTypeIcon.setImageResource(R.drawable.text_24)
            }
        }

        copyBtn.setOnClickListener {
            // Get the clipboard system service
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Code", valueTextView.text)
            clipboard.setPrimaryClip(clip)


            val imgResource: Int = R.drawable.copied
            copyBtn.setCompoundDrawablesWithIntrinsicBounds(0, imgResource, 0, 0)
            copyBtn.text = "Copied!"
            copyBtn.invalidate()


            Snackbar.make(rowView, "Copied to clipboard!", Snackbar.LENGTH_SHORT).show()
        }

        shareBtn.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, item.get("Value").toString())
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            context.startActivity(shareIntent)
        }

        gotoBtn.setOnClickListener {
            when (qrType) {
                Barcode.TYPE_WIFI -> {
                    val ssid = item.get("SSID") as String
                    val password = item.get("Password") as String
                    val type = item.get("Encryption") as Int

                    // only for Q and newer versions
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        Toast.makeText(context, "Unable to connect to WiFi using Android Q or above", Toast.LENGTH_LONG).show()
                        /*
                        val specifier: NetworkSpecifier = WifiNetworkSpecifier.Builder()
                                .setSsidPattern(PatternMatcher(ssid, PatternMatcher.PATTERN_PREFIX))
                                .setWpa2Passphrase(password)
                                .build()
                        val request: NetworkRequest =
                            NetworkRequest.Builder()
                                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                                .setNetworkSpecifier(specifier)
                                .build()
                        val connectivityManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                        var networkCallback = object : NetworkCallback() {
                            override fun onUnavailable() {
                                super.onUnavailable()
                            }

                            override fun onLosing(network: Network, maxMsToLive: Int) {
                                super.onLosing(network, maxMsToLive)

                            }

                            override fun onAvailable(network: Network) {
                                super.onAvailable(network)
                                connectivityManager?.bindProcessToNetwork(network)

                                Toast.makeText(context, "Successfully connected to: $ssid", Toast.LENGTH_SHORT).show()

                                val builder = WifiNetworkSuggestion.Builder()
                                    .setSsid(ssid)
                                    .setWpa2Passphrase(password)
                                val suggestion: WifiNetworkSuggestion = builder.build()

                                val list: ArrayList<WifiNetworkSuggestion> = ArrayList()
                                list.add(suggestion)

                                val manager = context.getSystemService(WIFI_SERVICE) as WifiManager?
                                val status = manager!!.addNetworkSuggestions(list)

                                if (status == STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                                    //We have successfully added our wifi for the system to consider
                                }
                            }

                            override fun onLost(network: Network) {
                                super.onLost(network)
                            }
                        }
                        connectivityManager.requestNetwork(request, networkCallback)
                        */
                    }
                    else {
                        val wifiConfig = WifiConfiguration()
                        wifiConfig.SSID = java.lang.String.format("\"%s\"", ssid)

                        // Check password type
                        when(type) {
                            TYPE_OPEN -> {
                                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                            }
                            TYPE_WPA -> {
                                wifiConfig.preSharedKey = String.format("\"%s\"", password)
                            }
                            TYPE_WEP -> {
                                wifiConfig.wepKeys[0] = "\"" + password + "\"";
                                wifiConfig.wepTxKeyIndex = 0;
                                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                            }
                        }

                        val wifiManager =
                            context.getSystemService(WIFI_SERVICE) as WifiManager?
                        //remember id
                        val netId = wifiManager!!.addNetwork(wifiConfig)
                        wifiManager!!.disconnect()
                        wifiManager!!.enableNetwork(netId, true)
                        wifiManager!!.reconnect()
                    }
                }
                Barcode.TYPE_URL -> {
                    gotoBtn.text = "Browse"

                    val title = item.get("Title") as String
                    val url = item.get("Url") as String

                    val intent = Intent(context, WebviewActivity::class.java)
                    val b = Bundle()
                    b.putString("url", url) //Your id
                    intent.putExtras(b) //Put your id to your next Intent

                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
                    else{
                        Toast.makeText(context, "No application available for that", Toast.LENGTH_LONG)
                    }
                }
                Barcode.TYPE_CONTACT_INFO -> {
                    gotoBtn.text = "Add contact"

                    val addr = item.get("Addresses") as List<Barcode.Address>
                    val emails = item.get("Emails") as List<Barcode.Email>
                    val name = item.get("Name") as Barcode.PersonName
                    val org = item.get("Organization") as String
                    val phones = item.get("Phones") as List<Barcode.Phone>
                    val title = item.get("Title") as String
                    val urls = item.get("Urls") as List<String>

                    val intent = Intent(Intent.ACTION_INSERT)
                    intent.type = ContactsContract.Contacts.CONTENT_TYPE

                    intent.putExtra(ContactsContract.Intents.Insert.FULL_MODE, true)
                    intent.putExtra(ContactsContract.Intents.Insert.NAME, name.formattedName)
                    intent.putExtra(ContactsContract.Intents.Insert.PHONE, phones[0].number)
                    intent.putExtra(ContactsContract.Intents.Insert.EMAIL, emails[0].address)
                    intent.putExtra(ContactsContract.Intents.Insert.POSTAL, addr[0].addressLines)
                    intent.putExtra(ContactsContract.Intents.Insert.POSTAL_ISPRIMARY, true)
                    intent.putExtra(ContactsContract.Intents.Insert.COMPANY, org)
                    intent.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, title)

                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
                    else{
                        Toast.makeText(context, "No application available for that", Toast.LENGTH_LONG)
                    }
                }
                Barcode.TYPE_CALENDAR_EVENT -> {
                    gotoBtn.text = "Add event"

                    val desc = item.get("Description") as String
                    val start = item.get("Start") as Barcode.CalendarDateTime
                    val end = item.get("End") as Barcode.CalendarDateTime

                    val loc = item.get("Location") as String
                    val org = item.get("Organizer") as String
                    val stat = item.get("Status") as String
                    val sum = item.get("Summary") as String


                    val intent = Intent(Intent.ACTION_INSERT)
                    intent.type = "vnd.android.cursor.item/event"

                    intent.putExtra(CalendarContract.Events.DESCRIPTION, desc)
                    intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.rawValue)
                    intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end.rawValue)
                    intent.putExtra(CalendarContract.Events.ALL_DAY, false)
                    intent.putExtra(CalendarContract.Events.EVENT_LOCATION, loc)

                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
                    else{
                        Toast.makeText(context, "No application available for that", Toast.LENGTH_LONG)
                    }
                }
                Barcode.TYPE_EMAIL -> {
                    gotoBtn.text = "Send email"

                    val addr = item.get("Address") as String
                    val body = item.get("Body") as String
                    val sub = item.get("Subject") as String
                    val emailType = item.get("Type")

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "*/*"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(addr))
                        putExtra(Intent.EXTRA_SUBJECT, sub)
                        putExtra(Intent.EXTRA_TEXT, body)
                    }

                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
                    else{
                        Toast.makeText(context, "No application available for that", Toast.LENGTH_LONG)
                    }
                }
                Barcode.TYPE_GEO -> {
                    gotoBtn.text = "Navigate"

                    val lat = item.get("Latitude") as Double
                    val lng = item.get("Longitude") as Double

                    //val navigationIntentUri: Uri = Uri.parse("google.navigation:q=$lat,$lng") //creating intent with latlng
                    //val navigationIntentUri: Uri = Uri.parse("geo:$lat,$lng") //creating intent with latlng
                    val navigationIntentUri: Uri = Uri.parse("geo:$lat,$lng?q=${Uri.encode("$lat,$lng")}")

                    val mapIntent = Intent(Intent.ACTION_VIEW, navigationIntentUri)
                    //mapIntent.setPackage("com.google.android.apps.maps")

                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(mapIntent)
                    }
                    else{
                        Toast.makeText(context, "No application available for that", Toast.LENGTH_LONG)
                    }

                }
                Barcode.TYPE_PHONE -> {
                    gotoBtn.text = "Dial"

                    val num = item.get("Number").toString()
                    val type = item.get("Type") as Int
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))

                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
                    else{
                        Toast.makeText(context, "No application available for that", Toast.LENGTH_LONG)
                    }
                }
                Barcode.TYPE_SMS -> {
                    gotoBtn.text = "SMS"

                    val msg = item.get("Message") as String
                    val num = item.get("Number") as String

                    val uri = Uri.parse("smsto:$num")
                    val it = Intent(Intent.ACTION_SENDTO, uri)
                    it.putExtra("sms_body", msg)

                    if (it.resolveActivity(context.packageManager) != null) {
                        context.startActivity(it)
                    }
                    else{
                        Toast.makeText(context, "No application available for that", Toast.LENGTH_LONG)
                    }
                }
                Barcode.TYPE_TEXT, Barcode.TYPE_PRODUCT -> {
                    gotoBtn.text = "Search web"

                    val txt = item.get("Value") as String
                    val intent = Intent(Intent.ACTION_WEB_SEARCH)
                    intent.putExtra(SearchManager.QUERY, txt) // query contains search string

                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
                    else{
                        Toast.makeText(context, "No application available for that", Toast.LENGTH_LONG)
                    }
                }
            }
        }

        return rowView
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return dataSource.length()
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

    private fun decodeQRType(format: Int): String? {
        return when (format) {
            Barcode.TYPE_WIFI -> "WIFI"
            Barcode.TYPE_URL -> "URL"
            Barcode.TYPE_CONTACT_INFO -> "CONTACT INFO"
            Barcode.TYPE_CALENDAR_EVENT -> "CALENDAR EVENT"
            Barcode.TYPE_EMAIL -> "EMAIL"
            Barcode.TYPE_GEO -> "GEO"
            Barcode.TYPE_PHONE -> "PHONE"
            Barcode.TYPE_SMS -> "SMS"
            Barcode.TYPE_TEXT -> "TEXT"
            Barcode.TYPE_PRODUCT -> "PRODUCT"
            else -> ""
        }
    }
}