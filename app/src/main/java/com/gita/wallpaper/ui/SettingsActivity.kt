package com.gita.wallpaper.ui

import android.os.Bundle
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gita.wallpaper.R
import com.gita.wallpaper.util.SettingsManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.radiobutton.MaterialRadioButton

/**
 * SettingsActivity - Settings screen for configuring language preference.
 *
 * Allows the user to choose between Hindi and English as the secondary
 * language displayed alongside Sanskrit verses.
 *
 * Design decisions:
 * - Simple RadioGroup for clear binary choice
 * - Settings saved immediately on button press
 * - Returns to MainActivity after save
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var settings: SettingsManager
    
    // UI Components
    private lateinit var toolbar: MaterialToolbar
    private lateinit var languageRadioGroup: RadioGroup
    private lateinit var radioHindi: MaterialRadioButton
    private lateinit var radioEnglish: MaterialRadioButton
    private lateinit var btnSave: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        settings = SettingsManager.getInstance(this)
        
        initViews()
        setupToolbar()
        loadCurrentSettings()
        setupClickListeners()
    }

    /**
     * Initialize view references.
     */
    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        languageRadioGroup = findViewById(R.id.languageRadioGroup)
        radioHindi = findViewById(R.id.radioHindi)
        radioEnglish = findViewById(R.id.radioEnglish)
        btnSave = findViewById(R.id.btnSave)
    }

    /**
     * Set up toolbar with back navigation.
     * Replaces the default ActionBar with our custom MaterialToolbar.
     */
    private fun setupToolbar() {
        // Replace default ActionBar with custom toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false) // We set title in XML
        
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    /**
     * Load current settings and update UI.
     */
    private fun loadCurrentSettings() {
        if (settings.useHindi) {
            radioHindi.isChecked = true
        } else {
            radioEnglish.isChecked = true
        }
    }

    /**
     * Set up click listeners.
     */
    private fun setupClickListeners() {
        btnSave.setOnClickListener {
            saveSettings()
        }
    }

    /**
     * Save selected language preference and finish.
     */
    private fun saveSettings() {
        val useHindi = radioHindi.isChecked
        settings.useHindi = useHindi
        
        Toast.makeText(
            this,
            getString(R.string.settings_saved),
            Toast.LENGTH_SHORT
        ).show()
        
        // Return to main activity
        finish()
    }
}
