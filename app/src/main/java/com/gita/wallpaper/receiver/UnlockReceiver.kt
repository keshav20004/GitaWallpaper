package com.gita.wallpaper.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.gita.wallpaper.service.WallpaperUpdateService
import com.gita.wallpaper.util.SettingsManager

/**
 * UnlockReceiver - BroadcastReceiver that listens for device unlock events.
 *
 * WHAT IT DOES:
 * - Receives ACTION_USER_PRESENT broadcast when user unlocks their device
 * - Checks throttling to prevent excessive wallpaper changes
 * - Starts WallpaperUpdateService to change the wallpaper
 *
 * WHY WE USE ACTION_USER_PRESENT:
 * - ACTION_USER_PRESENT is broadcast when the user dismisses the keyguard
 * - This is more reliable than screen-on broadcasts for detecting actual unlocks
 * - It works even when there's no lock screen set (immediate unlock)
 *
 * THROTTLING:
 * - We implement throttling to prevent battery drain and user annoyance
 * - By default, we only change wallpaper if 30+ seconds have passed since last change
 * - This handles rapid unlock/lock cycles gracefully
 *
 * ANDROID VERSION CONSIDERATIONS:
 * - This receiver is registered in manifest, which is allowed for ACTION_USER_PRESENT
 * - Starting from Android 10, we need to use foreground service for reliability
 */
class UnlockReceiver : BroadcastReceiver() {

    /**
     * Called when the receiver gets an intent broadcast.
     *
     * @param context The Context in which the receiver is running
     * @param intent The Intent being received (ACTION_USER_PRESENT or BOOT_COMPLETED)
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_USER_PRESENT -> handleUnlock(context)
            Intent.ACTION_SCREEN_ON -> handleScreenOn(context)
            Intent.ACTION_BOOT_COMPLETED -> handleBootCompleted(context)
        }
    }

    /**
     * Handle device unlock event.
     * This is the main trigger for changing wallpaper.
     */
    private fun handleUnlock(context: Context) {
        Log.d(TAG, "ACTION_USER_PRESENT received - user unlocked device")
        triggerWallpaperUpdate(context)
    }
    
    /**
     * Handle screen on event as fallback.
     * Some devices don't reliably send ACTION_USER_PRESENT.
     */
    private fun handleScreenOn(context: Context) {
        Log.d(TAG, "ACTION_SCREEN_ON received")
        // Also trigger on screen on as a fallback
        triggerWallpaperUpdate(context)
    }
    
    /**
     * Common wallpaper update trigger with throttling.
     */
    private fun triggerWallpaperUpdate(context: Context) {
        val settings = SettingsManager.getInstance(context)
        
        // Check throttling - don't update too frequently
        if (!settings.canUpdateWallpaper()) {
            val timeSince = settings.getTimeSinceLastUpdate() ?: "recently"
            Log.d(TAG, "Throttled: Last update was $timeSince, skipping this unlock")
            return
        }
        
        Log.d(TAG, "Unlock detected, starting wallpaper update service")
        
        // Start the wallpaper update service
        // Using foreground service for reliability on Android 10+
        startWallpaperService(context)
    }

    /**
     * Handle device boot completed.
     * We don't change wallpaper on boot, just log that receiver is active.
     */
    private fun handleBootCompleted(context: Context) {
        Log.d(TAG, "Boot completed, UnlockReceiver is ready")
        // No action needed - receiver will now catch future unlock events
    }

    /**
     * Start the WallpaperUpdateService to perform the actual wallpaper change.
     * Uses foreground service for reliability on modern Android versions.
     */
    private fun startWallpaperService(context: Context) {
        val serviceIntent = Intent(context, WallpaperUpdateService::class.java)
        
        try {
            // Use startForegroundService for Android O+ to avoid background limitations
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start WallpaperUpdateService", e)
        }
    }

    companion object {
        private const val TAG = "UnlockReceiver"
    }
}
