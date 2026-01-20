package com.gita.wallpaper.data

import android.content.Context
import org.json.JSONArray
import java.io.IOException

/**
 * VerseRepository - Handles loading and accessing Bhagavad Gita verses.
 *
 * This repository:
 * 1. Loads verses from the bundled assets/verses.json file
 * 2. Provides random verse selection for wallpaper updates
 * 3. Caches loaded verses in memory to avoid repeated I/O
 *
 * Design decisions:
 * - Uses Android's built-in org.json instead of external libraries (Gson/Moshi)
 *   to keep the app dependency-free and lightweight
 * - Verses are loaded lazily on first access
 * - Thread-safe lazy initialization using synchronized block
 *
 * @property context Application context for asset access
 */
class VerseRepository(private val context: Context) {

    // Cached list of verses, loaded from JSON on first access
    private var verses: List<GitaVerse>? = null
    
    // Lock object for thread-safe initialization
    private val lock = Any()

    /**
     * Get all verses, loading from assets if not already cached.
     * Thread-safe lazy initialization.
     */
    fun getAllVerses(): List<GitaVerse> {
        return verses ?: synchronized(lock) {
            verses ?: loadVersesFromAssets().also { verses = it }
        }
    }

    /**
     * Get a random verse for wallpaper display.
     * This is the primary method used by WallpaperService.
     *
     * @return A randomly selected GitaVerse
     * @throws IllegalStateException if no verses are available
     */
    fun getRandomVerse(): GitaVerse {
        val allVerses = getAllVerses()
        require(allVerses.isNotEmpty()) { "No verses available" }
        return allVerses.random()
    }

    /**
     * Get total number of verses in the repository.
     */
    fun getVerseCount(): Int = getAllVerses().size

    /**
     * Load verses from assets/verses.json file.
     *
     * The JSON structure is an array of verse objects:
     * [
     *   {
     *     "chapter": 2,
     *     "verse": 47,
     *     "sanskritText": "...",
     *     "hindiTranslation": "...",
     *     "englishTranslation": "..."
     *   },
     *   ...
     * ]
     */
    private fun loadVersesFromAssets(): List<GitaVerse> {
        return try {
            // Read JSON file from assets folder
            val jsonString = context.assets.open("verses.json")
                .bufferedReader()
                .use { it.readText() }

            // Parse JSON array and map to GitaVerse objects
            val jsonArray = JSONArray(jsonString)
            val verseList = mutableListOf<GitaVerse>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val verse = GitaVerse(
                    chapter = jsonObject.getInt("chapter"),
                    verse = jsonObject.getInt("verse"),
                    sanskritText = jsonObject.getString("sanskritText"),
                    hindiTranslation = jsonObject.getString("hindiTranslation"),
                    englishTranslation = jsonObject.getString("englishTranslation")
                )
                verseList.add(verse)
            }

            verseList
        } catch (e: IOException) {
            // Log error and return empty list rather than crashing
            android.util.Log.e(TAG, "Failed to load verses from assets", e)
            emptyList()
        } catch (e: Exception) {
            // Handle JSON parsing errors
            android.util.Log.e(TAG, "Failed to parse verses JSON", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "VerseRepository"

        // Singleton instance for app-wide access
        @Volatile
        private var instance: VerseRepository? = null

        /**
         * Get singleton instance of VerseRepository.
         * Uses double-checked locking for thread safety.
         */
        fun getInstance(context: Context): VerseRepository {
            return instance ?: synchronized(this) {
                instance ?: VerseRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
