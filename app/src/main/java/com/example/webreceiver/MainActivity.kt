package com.example.webreceiver

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var startBtn: Button

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        startBtn = findViewById(R.id.startBtn)

        startBtn.setOnClickListener {
            val intent = Intent(this, FlaskWebSocketService::class.java)
            // Required for Android 14+: must be foreground start, not background
            ContextCompat.startForegroundService(this, intent)
        }

        FlaskWebSocketService.messageLiveData.observe(this, Observer {
            status.text = it
        })
    }
}
