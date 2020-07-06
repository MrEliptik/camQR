package com.victormeunier.camqr

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.example.camqr.R
import kotlinx.android.synthetic.main.activity_webview.*


class WebviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        supportActionBar?.hide();

        //check that link is not null
        //or that you opened app from deep link
        if (intent != null) {
            val intentUri: Uri? = Uri.parse(intent.extras?.get("url").toString()) //get link
            webview.settings.javaScriptEnabled = true
            webview.loadUrl(intentUri.toString()) //open it in webView
        }
    }
}