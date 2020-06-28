package com.example.camqr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer


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

        // Get subtitle element
        val valueTextView = rowView.findViewById(R.id.history_list_value) as TextView

        // Get color elem
        val colorBarView = rowView.findViewById(R.id.color_bar_view) as View

        // Get buttons
        val copyBtn = rowView.findViewById(R.id.copy_btn) as Button
        val shareBtn = rowView.findViewById(R.id.share_btn) as Button
        val gotoBtn = rowView.findViewById(R.id.goto_btn) as Button

        val item = getItem(position) as JSONObject

        val image = item.get("Image") as Bitmap?

        if (image != null) imageThumbnail.setImageBitmap(image)
        typeTextView.text = item.get("Type").toString()
        valueTextView.text = item.get("Value").toString()
        //colorBarView.setBackgroundColor(item.get("Color") as Int)

        copyBtn.setOnClickListener {
            // Get the clipboard system service
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Code", valueTextView.text)
            clipboard.setPrimaryClip(clip)


            val imgResource: Int = R.drawable.copy_done_24
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
            val intent = Intent(context, WebviewActivity::class.java)
            val b = Bundle()
            b.putString("url", item.get("Value").toString()) //Your id
            intent.putExtras(b) //Put your id to your next Intent

            context.startActivity(intent)
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
}