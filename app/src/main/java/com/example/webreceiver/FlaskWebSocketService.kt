package com.example.webreceiver

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import okhttp3.*

class FlaskWebSocketService : Service() {

    companion object {
        val messageLiveData = MutableLiveData<String>()
    }

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Connected to Flask")
            .setContentText("Receiving data stream...")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .build()

        startForeground(1001, notification)
    }

    private fun connectToFlask() {
        val request = Request.Builder()
            .url("ws://192.168.1.100:5000/ws")   // change to your LAN IP
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                messageLiveData.postValue("Connected!")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                messageLiveData.postValue("From Flask: $text")
                if (text == "20"){
                    val intent = Intent(applicationContext, Alarmscreen::class.java)
                    intent.putExtra("msg", "Hello from MainActivity!") // optional data
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                messageLiveData.postValue("Error: ${t.localizedMessage}")
            }
        })
    }

    override fun onDestroy() {
        webSocket?.close(1000, "Service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}