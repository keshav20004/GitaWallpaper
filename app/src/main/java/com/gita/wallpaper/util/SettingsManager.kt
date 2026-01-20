package com.gita.wallpaper.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * SettingsManager - Manages user preferences using SharedPreferences.
 *
 * This class handles:
 * 1. Secondary language preference (Hindi or English)
 * 2. Last wallpaper update timestamp (for throttling)
 * 3. Service enabled/disabled state
 *
 * Design decisions:
 * - Uses SharedPreferences for simple, reliable persistence
 * - Thread-safe through SharedPreferences' built-in synchronization
 * - Singleton pattern for consistent access across the app
 *
 * @property context Application context for SharedPreferences access
 */
class SettingsManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Whether to use Hindi as the secondary language.
     * If false, English is used.
     * Default: true (Hindi)
     */
    var useHindi: Boolean
        get() = prefs.getBoolean(KEY_USE_HINDI, true)
        set(value) = prefs.edit { putBoolean(KEY_USE_HINDI, value) }

    /**
     * Human-readable label for the current secondary language.
     */
    val secondaryLanguageLabel: String
        get() = if (useHindi) "Hindi" else "English"

    /**
     * Timestamp of the last wallpaper update (in milliseconds).
     * Used for throttling to prevent excessive updates.
     */
    var lastUpdateTimestamp: Long
        get() = prefs.getLong(KEY_LAST_UPDATE, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_UPDATE, value) }

    /**
     * Minimum interval between wallpaper updates (in milliseconds).
     * Default: 30 seconds
     *
     * This prevents excessive wallpaper changes if the user unlocks
     * their phone multiple times in quick succession.
     */
    val throttleIntervalMs: Long
        get() = prefs.getLong(KEY_THROTTLE_INTERVAL, DEFAULT_THROTTLE_MS)

    /**
     * Check if enough time has passed since the last wallpaper update.
     * Used to implement throttling.
     *
     * @return true if wallpaper can be updated, false if should wait
     */
    fun canUpdateWallpaper(): Boolean {
        val now = System.currentTimeMillis()
        val elapsed = now - lastUpdateTimestamp
        return elapsed >= throttleIntervalMs
    }

    /**
     * Record that a wallpaper update just occurred.
     * Should be called after successfully setting wallpaper.
     */
    fun recordWallpaperUpdate() {
        lastUpdateTimestamp = System.currentTimeMillis()
    }

    /**
     * Get elapsed time since last update in a human-readable format.
     * Returns null if never updated.
     */
    fun getTimeSinceLastUpdate(): String? {
        val lastUpdate = lastUpdateTimestamp
        if (lastUpdate == 0L) return null

        val elapsed = System.currentTimeMillis() - lastUpdate
        return when {
            elapsed < 60_000 -> "Just now"
            elapsed < 3600_000 -> "${elapsed / 60_000} minutes ago"
            elapsed < 86400_000 -> "${elapsed / 3600_000} hours ago"
            else -> "${elapsed / 86400_000} days ago"
        }
    }

    companion object {
        private const val PREFS_NAME = "gita_wallpaper_prefs"
        private const val KEY_USE_HINDI = "use_hindi"
        private const val KEY_LAST_UPDATE = "last_update_timestamp"
        private const val KEY_THROTTLE_INTERVAL = "throttle_interval"

        // Default throttle: 30 seconds between updates
        private const val DEFAULT_THROTTLE_MS = 30_000L

        @Volatile
        private var instance: SettingsManager? = null

        /**
         * Get singleton instance of SettingsManager.
         */
        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
