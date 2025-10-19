package com.example.webreceiver

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import okhttp3.*

class FlaskWebSocketService : Service() {

    companion object {
        const val NOTIF_ID = 1001
        const val ACTION_STOP_SOUND = "com.example.webreceiver.action.STOP_SOUND"
        const val REQUEST_STOP_SOUND = 2001
        val messageLiveData = MutableLiveData<String>()
    }

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private var player: MediaPlayer? = null
    private var currentWsUrl: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // Handle Stop Sound action
        if (intent?.action == ACTION_STOP_SOUND) {
            stopAlarm()
            updateMainNotification("Sound stopped. Still connected.")
            return START_STICKY
        }

        // Read URL from intent
        val wsUrl = intent?.getStringExtra("url") ?: currentWsUrl
        wsUrl?.let {
            if (it != currentWsUrl) {
                // Only reconnect if URL changed
                currentWsUrl = it
                connectToFlask(it)
            }
        }

        startAsForeground()
        return START_STICKY
    }

    private fun startAsForeground() {
        val channelId = "flask_channel"
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            nm.getNotificationChannel(channelId) == null
        ) {
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Flask WebSocket Channel",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        val stopSoundIntent = Intent(this, FlaskWebSocketService::class.java).apply {
            action = ACTION_STOP_SOUND
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            REQUEST_STOP_SOUND,
            stopSoundIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Connected to Flask")
            .setContentText("Receiving data stream...")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .addAction(android.R.drawable.ic_media_pause, "Stop Sound", stopPendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun connectToFlask(url: String) {
        webSocket?.close(1000, "Reconnecting")

        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                messageLiveData.postValue("Connected!")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                messageLiveData.postValue("From Flask: $text")
                if (text == "FALLDETECTED") playAlarm()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                messageLiveData.postValue("Error: ${t.localizedMessage}")
            }
        })
    }

    private fun playAlarm() {
        stopAlarm()
        player = MediaPlayer.create(this, R.raw.myalarmsound).apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            isLooping = true
            start()
        }
    }

    private fun stopAlarm() {
        player?.stop()
        player?.release()
        player = null
    }

    private fun updateMainNotification(text: String) {
        val channelId = "flask_channel"
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Connected to Flask")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, notif)
    }

    override fun onDestroy() {
        stopAlarm()
        webSocket?.close(1000, "Service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
