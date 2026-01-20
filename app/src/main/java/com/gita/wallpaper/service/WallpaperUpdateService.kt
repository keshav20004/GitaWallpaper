package com.gita.wallpaper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.gita.wallpaper.R
import com.gita.wallpaper.data.VerseRepository
import com.gita.wallpaper.util.SettingsManager
import com.gita.wallpaper.util.WallpaperRenderer
import java.io.IOException
import kotlin.concurrent.thread

/**
 * WallpaperUpdateService - Foreground service that performs wallpaper updates.
 *
 * WHY A FOREGROUND SERVICE:
 * - Android 8.0+ restricts background execution significantly
 * - Using foreground service ensures our wallpaper update completes reliably
 * - The service is short-lived: starts, updates wallpaper, stops itself
 * - User sees a brief notification during the update (required for foreground)
 *
 * WHAT IT DOES:
 * 1. Starts as foreground service with notification
 * 2. Selects a random Gita verse from repository
 * 3. Renders the verse as a bitmap using WallpaperRenderer
 * 4. Sets the bitmap as system wallpaper via WallpaperManager
 * 5. Records update timestamp for throttling
 * 6. Stops itself when complete
 *
 * ERROR HANDLING:
 * - Gracefully handles rendering failures
 * - Logs errors but doesn't crash the app
 * - Always stops itself, even on failure
 */
class WallpaperUpdateService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Called when the service is started.
     * Performs the wallpaper update in a background thread.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "WallpaperUpdateService started")
        
        // Start foreground immediately to avoid ANR
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Perform update in background thread
        thread {
            try {
                updateWallpaper()
            } catch (e: Exception) {
                Log.e(TAG, "Wallpaper update failed", e)
            } finally {
                // Always stop service when done
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }
        
        // Don't restart if killed - next unlock will trigger new service
        return START_NOT_STICKY
    }

    /**
     * Main wallpaper update logic.
     * - Gets a random verse
     * - Renders it to bitmap
     * - Sets as system wallpaper
     */
    private fun updateWallpaper() {
        val settings = SettingsManager.getInstance(this)
        val repository = VerseRepository.getInstance(this)
        val renderer = WallpaperRenderer(this)
        
        // Get a random verse
        val verse = try {
            repository.getRandomVerse()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get random verse", e)
            return
        }
        
        Log.d(TAG, "Selected verse: Chapter ${verse.chapter}, Verse ${verse.verse}")
        
        // Render verse to bitmap
        val useHindi = settings.useHindi
        val bitmap = try {
            renderer.renderVerse(verse, useHindi)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to render verse", e)
            return
        }
        
        Log.d(TAG, "Rendered bitmap: ${bitmap.width}x${bitmap.height}")
        
        // Set wallpaper on BOTH home screen AND lock screen
        try {
            val wallpaperManager = WallpaperManager.getInstance(this)
            
            // Use FLAG_SYSTEM | FLAG_LOCK to set both home and lock screen wallpaper
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.setBitmap(
                    bitmap,
                    null, // visibleCropHint - null means use full bitmap
                    true, // allowBackup
                    WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK // Both screens
                )
            } else {
                // Fallback for older devices - sets both automatically
                wallpaperManager.setBitmap(bitmap)
            }
            
            // Record successful update timestamp
            settings.recordWallpaperUpdate()
            
            Log.d(TAG, "Wallpaper updated on both home and lock screen")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to set wallpaper", e)
        } finally {
            // Clean up bitmap memory
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    /**
     * Create notification for foreground service.
     * Required on Android 8.0+ for foreground services.
     */
    private fun createNotification(): Notification {
        createNotificationChannel()
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_gallery) // Using system icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    /**
     * Create notification channel for Android 8.0+.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "WallpaperUpdateService destroyed")
    }

    companion object {
        private const val TAG = "WallpaperUpdateService"
        private const val CHANNEL_ID = "wallpaper_update_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
