package com.example.webreceiver

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Alarmscreen : AppCompatActivity() {
    lateinit var player:MediaPlayer
    lateinit var closebtb: Button
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.alarmscr)
        closebtb = findViewById(R.id.closebtn)

        playsound()

        closebtb.setOnClickListener {
            finish()
        }
    }
    fun playsound(){
        player = MediaPlayer.create(this, R.raw.myalarmsound)
        player.isLooping = true
        player.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.stop()
    }
}