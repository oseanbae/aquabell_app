package com.capstone.aquabell.ui.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.capstone.aquabell.MainActivity
import com.capstone.aquabell.R
import com.capstone.aquabell.data.model.AlertEntry
import android.graphics.BitmapFactory

object NotificationUtils {

    const val ALERTS_CHANNEL_ID = "alerts_channel"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERTS_CHANNEL_ID,
                "AquaBell Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new AquaBell alerts"
                setShowBadge(true) // Enable app icon badge
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showAlertNotification(context: Context, alert: AlertEntry) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate", "alerts")
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(context, 1001, intent, pendingFlags)

        // Use colors from Color.kt (lines 15-18)
        val accentColor = when (alert.type.lowercase()) {
            "critical" -> 0xFFEF5350.toInt() // AccentDanger
            "caution" -> 0xFFFFB020.toInt()   // AccentWarning
            else -> 0xFF2BB673.toInt()        // AccentSuccess
        }

        // Use launcher icon from mipmap as app logo
        val logoBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        } catch (_: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(context, ALERTS_CHANNEL_ID)
            // Use a proper monochrome small icon in drawable to ensure delivery across Android versions
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(accentColor)
            .setColorized(true)
        
        // Set large icon only if bitmap was successfully decoded
        logoBitmap?.let {
            builder.setLargeIcon(it)
        }

        val notificationId = alert.alertId.hashCode()
        val manager = NotificationManagerCompat.from(context)

        // === FIX: check runtime permission safely ===
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            manager.notify(notificationId, builder.build())
        } else {
            // Permission denied or not yet granted
            android.util.Log.w("NotificationUtils", "POST_NOTIFICATIONS permission not granted.")
        }
    }
}


