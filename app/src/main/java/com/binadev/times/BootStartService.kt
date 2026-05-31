package com.binadev.times

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Handler
import android.os.Looper

class BootStartService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "boot_start"
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "הפעלה", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("מתחיל...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        startForeground(1, notification)

        // Small delay to let the system settle after boot
        Handler(Looper.getMainLooper()).postDelayed({
            val launch = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(launch)
            stopSelf()
        }, 3000)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
