package com.example.silent

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build

class SilentModeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (!notificationManager.isNotificationPolicyAccessGranted) {
                // Prompt user to grant DND access (optional, handle in Activity if needed)
                return
            }

            val action = intent.getStringExtra("action")
            val policy = when (action) {
                "SILENT" -> NotificationManager.Policy(
                    NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS or NotificationManager.Policy.PRIORITY_CATEGORY_CALLS,
                    0, // Priority senders
                    NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_OFF
                )
                "VIBRATE" -> NotificationManager.Policy(
                    NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS or NotificationManager.Policy.PRIORITY_CATEGORY_CALLS,
                    0, // Priority senders
                    NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_OFF
                )
                "NORMAL" -> NotificationManager.Policy(
                    NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS or NotificationManager.Policy.PRIORITY_CATEGORY_CALLS,
                    0, // Priority senders
                    0  // No suppressed effects
                )
                else -> return
            }

            notificationManager.setNotificationPolicy(policy)

            // Set ringer mode
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            when (action) {
                "SILENT" -> audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                "VIBRATE" -> audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                "NORMAL" -> audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
        } else {
            // Fallback for devices running API levels below 26
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            when (intent.getStringExtra("action")) {
                "SILENT" -> audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                "VIBRATE" -> audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                "NORMAL" -> audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
        }
    }
}