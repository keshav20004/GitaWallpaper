package com.gita.wallpaper.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gita.wallpaper.R
import com.gita.wallpaper.service.UnlockListenerService
import com.gita.wallpaper.service.WallpaperUpdateService
import com.gita.wallpaper.util.SettingsManager
import com.google.android.material.button.MaterialButton
import android.widget.TextView

/**
 * MainActivity - Main entry point of the Gita Wallpaper app.
 *
 * This activity:
 * 1. Shows app title and description
 * 2. Displays current status (service active, last update time)
 * 3. Shows current language preference
 * 4. Provides button to manually change wallpaper
 * 5. Links to settings for language configuration
 * 6. Starts UnlockListenerService for persistent unlock detection
 *
 * The wallpaper change functionality works automatically via UnlockListenerService,
 * but this screen allows manual triggering and configuration.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var settings: SettingsManager
    
    // UI Components
    private lateinit var lastUpdatedText: TextView
    private lateinit var currentLanguageText: TextView
    private lateinit var btnChangeWallpaper: MaterialButton
    private lateinit var btnSettings: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        settings = SettingsManager.getInstance(this)
        
        initViews()
        setupClickListeners()
        
        // Start the persistent unlock listener service
        // This service runs in foreground and reliably detects unlock events
        startUnlockListenerService()
    }

    override fun onResume() {
        super.onResume()
        // Refresh status when returning from settings
        updateStatusDisplay()
    }

    /**
     * Start the persistent unlock listener service.
     * This service runs as a foreground service and reliably detects unlock events.
     */
    private fun startUnlockListenerService() {
        UnlockListenerService.start(this)
        android.util.Log.d("MainActivity", "UnlockListenerService started")
    }

    /**
     * Initialize view references.
     */
    private fun initViews() {
        lastUpdatedText = findViewById(R.id.lastUpdatedText)
        currentLanguageText = findViewById(R.id.currentLanguageText)
        btnChangeWallpaper = findViewById(R.id.btnChangeWallpaper)
        btnSettings = findViewById(R.id.btnSettings)
    }

    /**
     * Set up click listeners for buttons.
     */
    private fun setupClickListeners() {
        // Manual wallpaper change
        btnChangeWallpaper.setOnClickListener {
            changeWallpaperNow()
        }
        
        // Open settings
        btnSettings.setOnClickListener {
            openSettings()
        }
    }

    /**
     * Update the status display with current information.
     */
    private fun updateStatusDisplay() {
        // Last updated time
        val lastUpdate = settings.getTimeSinceLastUpdate()
        lastUpdatedText.text = if (lastUpdate != null) {
            getString(R.string.status_last_updated, lastUpdate)
        } else {
            getString(R.string.status_never_updated)
        }
        
        // Current language setting
        currentLanguageText.text = getString(
            R.string.current_language,
            settings.secondaryLanguageLabel
        )
    }

    /**
     * Manually trigger a wallpaper change.
     * Bypasses throttling for user-initiated changes.
     */
    private fun changeWallpaperNow() {
        Toast.makeText(
            this,
            "Changing lock screen wallpaper...",
            Toast.LENGTH_SHORT
        ).show()
        
        // Start the wallpaper update service directly
        val serviceIntent = Intent(this, WallpaperUpdateService::class.java)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        // Show success message after a delay (service runs async)
        btnChangeWallpaper.postDelayed({
            updateStatusDisplay()
            Toast.makeText(
                this,
                getString(R.string.success_wallpaper_set),
                Toast.LENGTH_SHORT
            ).show()
        }, 2000)
    }

    /**
     * Open settings activity.
     */
    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }
}

