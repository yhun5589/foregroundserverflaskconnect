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
        const val ACTION_STOP = "com.example.webreceiver.action.STOP"
        const val REQUEST_STOP_SERVICE = 2001
        val messageLiveData = MutableLiveData<String>()
    }

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private var player: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAlarm()
            webSocket?.close(1000, "Stopped by user")
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForeground()
        connectToFlask()
        return START_STICKY
    }

    private fun startAsForeground() {
        val channelId = "flask_channel"
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            nm.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId,
                "Flask WebSocket Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, FlaskWebSocketService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            REQUEST_STOP_SERVICE,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Connected to Flask")
            .setContentText("Waiting for trigger...")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .addAction(android.R.drawable.ic_media_pause, "Stop Sound", stopPendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun connectToFlask() {
        val request = Request.Builder()
            .url("ws://192.168.1.100:5000/ws")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                messageLiveData.postValue("Connected!")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                messageLiveData.postValue("From Flask: $text")
                if (text == "20") {
                    playAlarm()
                    showAlarmNotification()
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                messageLiveData.postValue("Error: ${t.localizedMessage}")
            }
        })
    }

    private fun playAlarm() {
        stopAlarm() // ensure not duplicated

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

    private fun showAlarmNotification() {
        val channelId = "alarm_channel"
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            nm.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId,
                "Alarm Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, FlaskWebSocketService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            REQUEST_STOP_SERVICE,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("⚠️ Alert Triggered")
            .setContentText("Tap Stop to silence the alarm")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .addAction(android.R.drawable.ic_media_pause, "Stop Sound", stopPendingIntent)
            .build()

        getSystemService(NotificationManager::class.java).notify(2002, notif)
    }

    override fun onDestroy() {
        stopAlarm()
        webSocket?.close(1000, "Service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
