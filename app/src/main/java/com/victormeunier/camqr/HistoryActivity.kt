package com.victormeunier.camqr

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.camqr.R
import kotlinx.android.synthetic.main.activity_history.*
import org.json.JSONArray
import org.json.JSONObject

class HistoryActivity : AppCompatActivity() {

    private lateinit var listItems: JSONArray
    private lateinit var adapter: CodesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        history_list_view.emptyView = empty_element

        val items = getHistoryItems()
        if(items.length() > 0){
            displayHistoryItems(items)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.history_menu, menu)
        return true
    }

    private fun getHistoryItems(): JSONArray {
        val sharedPref = getSharedPreferences("appData", Context.MODE_PRIVATE)
        var json = JSONArray()
        // Retrieve values from preferences
        val str: String? = sharedPref.getString("history", null)
        if(str != null) json = JSONArray(str)
        return json
    }

    private fun displayHistoryItems(items:JSONArray) {
        listItems = JSONArray()
        for (i in items.length() - 1 downTo 0) {
            val item: JSONObject = items.getJSONObject(i)
            listItems.put(item)
        }

        adapter = CodesAdapter(this, listItems)
        history_list_view.adapter = adapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item?.itemId) {
            R.id.action_options -> {
                Log.d("MENU", "OPTIONS")
                val myIntent = Intent(applicationContext, SettingsActivity::class.java)
                startActivityForResult(myIntent, 0)
                true
            }
            R.id.action_erase -> {
                Log.d("MENU", "HISTORY")
                AlertDialog.Builder(this)
                    .setIcon(R.drawable.alert_24)
                    .setTitle(resources.getString(R.string.erase_history))
                    .setMessage(resources.getString(R.string.msg_erase_history))
                    .setPositiveButton(resources.getString(R.string.yes)
                    ) { dialog, which ->
                        run {
                            clearHistory()
                        }}
                    .setNegativeButton(resources.getString(R.string.no), null)
                    .show()
                true
            }
            R.id.action_about -> {
                val myIntent = Intent(applicationContext, AboutActivity::class.java)
                startActivityForResult(myIntent, 0)
                true
            }
            R.id.action_rate -> {
                rateMyApp()
                true
            }
            else -> false
        }
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

    private fun clearHistory(selected: ArrayList<Int>? = null) {
        val sharedPreferences = getDefaultSharedPreferences(this /* Activity context */)
        if (selected != null) {
            // get the items as JSONArray
            val arr = getHistoryItems()
            var size = arr.length()
            var newArr = JSONArray()

            // Delete selected items
            // Delete selected items (careful: position is in reverse compared
            // to the JSONArray)
            for (i in (size-1) downTo 0) {
                val item = arr.get(i) as JSONObject
                if ((size-1)-i !in selected){
                    newArr.put(item)
                }
            }

            // Put back in sharedpreference
            val sharedPref = getSharedPreferences("appData", Context.MODE_PRIVATE)
            val prefEditor = sharedPref.edit()
            prefEditor.putString("history", newArr.toString())
            prefEditor.apply() // handle writing in the background


            var newListItems = JSONArray()
            size = listItems.length()
            // Update data

            for (i in 0 until size) {
                val item = listItems.get(i)
                if (i !in selected) {
                    newListItems.put(item)
                }
            }

            listItems = JSONArray(newListItems.toString())
            adapter = CodesAdapter(this, listItems)
            history_list_view.adapter = adapter
            adapter.notifyDataSetChanged()
        }
        else {
            history_list_view.adapter = null
            val sharedPref = getSharedPreferences("appData", Context.MODE_PRIVATE)
            var editPref = sharedPref.edit()

            val arr = getHistoryItems()
            var size = arr.length()

            // Delete selected items
            editPref.remove("history")
            editPref.apply()
        }
        history_list_view.emptyView = empty_element
    }
}