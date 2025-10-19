package com.example.webreceiver

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var tvSelectedUrl: TextView
    private lateinit var btnEnterUrl: Button
    private lateinit var webView: WebView

    private val prefs by lazy { getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    private val keyLastUrl = "last_url"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvSelectedUrl = findViewById(R.id.tvSelectedUrl)
        btnEnterUrl = findViewById(R.id.btnEnterUrl)
        webView = findViewById(R.id.webView)

        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true

        // Load last URL if exists
        val lastUrl = prefs.getString(keyLastUrl, null)
        val camUrl = getCamUrl(lastUrl.toString())
        webView.loadUrl(camUrl)

        lastUrl?.let {
            tvSelectedUrl.text = it
            webView.loadUrl(it)
            // Start service if not running
            val intent = Intent(this, FlaskWebSocketService::class.java)
            intent.putExtra("url", convertHttpToWs(it))
            startForegroundService(intent)
        }

        btnEnterUrl.setOnClickListener {
            showUrlDialog()
        }
    }

    private fun showUrlDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.urldailog, null)
        val editText = dialogView.findViewById<EditText>(R.id.etUrl)
        val btnNext = dialogView.findViewById<Button>(R.id.btnNext)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        // Pre-fill with current URL
        editText.setText(tvSelectedUrl.text.toString())

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnNext.setOnClickListener {
            val httpUrl = editText.text.toString().trim()
            if (httpUrl.isNotEmpty()) {
                tvSelectedUrl.text = httpUrl
                webView.loadUrl(httpUrl)

                // Save last URL
                prefs.edit().putString(keyLastUrl, httpUrl).apply()

                // Start / update service with WS URL
                val wsUrl = convertHttpToWs(httpUrl)
                val intent = Intent(this, FlaskWebSocketService::class.java)
                intent.putExtra("url", wsUrl)
                startForegroundService(intent)

                dialog.dismiss()
            } else {
                editText.error = "Please enter a valid URL"
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun convertHttpToWs(httpUrl: String): String {
        var url = httpUrl.trim()
        url = when {
            url.startsWith("https://") -> url.replaceFirst("https://", "wss://")
            url.startsWith("http://") -> url.replaceFirst("http://", "ws://")
            else -> "ws://$url"
        }
        if (!url.endsWith("/ws")) url += "/ws"
        return url
    }
    private fun getCamUrl(httpUrl: String): String {
        var url = httpUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url" // fallback
        }
        if (!url.endsWith("/")) url += "/" // ensure trailing slash
        return url + "video_feed"
    }
}
