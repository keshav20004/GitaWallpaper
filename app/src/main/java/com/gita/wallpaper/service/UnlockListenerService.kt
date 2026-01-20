package com.gita.wallpaper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.gita.wallpaper.R
import com.gita.wallpaper.ui.MainActivity
import com.gita.wallpaper.util.SettingsManager

/**
 * UnlockListenerService - A persistent foreground service that listens for unlock events.
 *
 * WHY THIS SERVICE EXISTS:
 * - Android aggressively kills background processes to save battery
 * - Manifest-declared BroadcastReceivers may not receive intents if app is killed
 * - A foreground service keeps the app alive and can reliably receive broadcasts
 *
 * HOW IT WORKS:
 * 1. Starts as a foreground service with a persistent notification
 * 2. Dynamically registers a BroadcastReceiver for ACTION_USER_PRESENT and ACTION_SCREEN_ON
 * 3. When unlock is detected, starts WallpaperUpdateService to change wallpaper
 * 4. Runs indefinitely until user stops it or uninstalls app
 */
class UnlockListenerService : Service() {

    private var unlockReceiver: BroadcastReceiver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "UnlockListenerService created")
        
        // Start as foreground service immediately
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Register the unlock receiver
        registerUnlockReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "UnlockListenerService started")
        
        // Return STICKY so Android restarts service if killed
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "UnlockListenerService destroyed")
        
        // Unregister receiver
        unregisterUnlockReceiver()
    }

    /**
     * Register broadcast receiver for unlock events.
     */
    private fun registerUnlockReceiver() {
        if (unlockReceiver == null) {
            unlockReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    Log.d(TAG, "Received broadcast: ${intent.action}")
                    
                    when (intent.action) {
                        Intent.ACTION_USER_PRESENT -> {
                            Log.d(TAG, "User present - checking if should update wallpaper")
                            handleUnlock()
                        }
                        Intent.ACTION_SCREEN_ON -> {
                            Log.d(TAG, "Screen on - checking if should update wallpaper")
                            handleUnlock()
                        }
                    }
                }
            }
            
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(unlockReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(unlockReceiver, filter)
            }
            
            Log.d(TAG, "Unlock receiver registered")
        }
    }

    /**
     * Unregister the broadcast receiver.
     */
    private fun unregisterUnlockReceiver() {
        unlockReceiver?.let {
            try {
                unregisterReceiver(it)
                Log.d(TAG, "Unlock receiver unregistered")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister receiver", e)
            }
            unlockReceiver = null
        }
    }

    /**
     * Handle unlock event - check throttle and trigger wallpaper update.
     */
    private fun handleUnlock() {
        val settings = SettingsManager.getInstance(this)
        
        // Check throttling
        if (!settings.canUpdateWallpaper()) {
            val timeSince = settings.getTimeSinceLastUpdate() ?: "recently"
            Log.d(TAG, "Throttled: Last update was $timeSince, skipping")
            return
        }
        
        Log.d(TAG, "Triggering wallpaper update")
        
        // Start wallpaper update service
        val serviceIntent = Intent(this, WallpaperUpdateService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    /**
     * Create the persistent notification for foreground service.
     */
    private fun createNotification(): Notification {
        createNotificationChannel()
        
        // Intent to open app when notification is tapped
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Gita Wallpaper Active")
            .setContentText("Wallpaper will change on unlock")
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    /**
     * Create notification channel for Android 8.0+.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Unlock Listener",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the app running to detect screen unlocks"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "UnlockListenerService"
        private const val CHANNEL_ID = "unlock_listener_channel"
        private const val NOTIFICATION_ID = 1002

        /**
         * Start the unlock listener service.
         */
        fun start(context: Context) {
            val intent = Intent(context, UnlockListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stop the unlock listener service.
         */
        fun stop(context: Context) {
            val intent = Intent(context, UnlockListenerService::class.java)
            context.stopService(intent)
        }
    }
}
