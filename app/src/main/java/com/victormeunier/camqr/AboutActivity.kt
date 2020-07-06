package com.victormeunier.camqr

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.camqr.R
import kotlinx.android.synthetic.main.activity_about.*

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        contact.setOnClickListener { emailContact() }
        rate.setOnClickListener { rateMyApp() }
        share.setOnClickListener { shareMyApp() }
        feedback.setOnClickListener { emailFeedBack() }
        coffee.setOnClickListener { followLink("https://buymeacoff.ee/mreliptik") }
        paypal.setOnClickListener { followLink("https://paypal.me/VictorMeunier") }

        val pInfo: PackageInfo = applicationContext.packageManager.getPackageInfo(packageName, 0)
        version_number.text = pInfo.versionName
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun rateMyApp() {
        val uri: Uri = Uri.parse("market://details?id=" + applicationContext.packageName)
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(
            Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        try {
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + applicationContext.packageName)
                )
            )
        }
    }

    private fun shareMyApp() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "CamText, photo OCR app")
        val appName = applicationContext.packageName
        sharingIntent.putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.checkout_app) + ": http://play.google.com/store/apps/details?id=$appName")
        startActivity(Intent.createChooser(sharingIntent, "Share via"))
    }

    private fun emailContact() {
        val emailIntent = Intent(
            Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "victormeunier.dev@gmail.com", null
            )
        )
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "I'm contacting your from CamText..")
        emailIntent.putExtra(Intent.EXTRA_TEXT, "")
        startActivity(Intent.createChooser(emailIntent, "Send email"))
    }

    private fun emailFeedBack() {

        val emailIntent = Intent(
            Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "victormeunier.dev@gmail.com", null
            )
        )
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback on CamText Android app")
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Hey, here's some feedback on your CamText Android app..")
        startActivity(Intent.createChooser(emailIntent, "Send email"))
    }

    private fun followLink(url: String) {
        val uriUrl = Uri.parse(url)
        val launchBrowser = Intent(Intent.ACTION_VIEW, uriUrl)
        startActivity(launchBrowser)
    }
}