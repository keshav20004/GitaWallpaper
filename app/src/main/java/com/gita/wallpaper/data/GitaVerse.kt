package com.gita.wallpaper.data

/**
 * GitaVerse - Data class representing a single verse from the Bhagavad Gita.
 *
 * This model stores the verse in multiple languages:
 * - Sanskrit (Devanagari script) - Always displayed as the primary text
 * - Hindi translation - Optional, displayed if user selects Hindi
 * - English translation - Optional, displayed if user selects English
 *
 * The app displays Sanskrit + one translation language (user's choice),
 * never all three simultaneously.
 *
 * @property chapter Chapter number (1-18) in the Bhagavad Gita
 * @property verse Verse number within the chapter
 * @property sanskritText The original Sanskrit text in Devanagari script
 * @property hindiTranslation Hindi translation of the verse
 * @property englishTranslation English translation of the verse
 */
data class GitaVerse(
    val chapter: Int,
    val verse: Int,
    val sanskritText: String,
    val hindiTranslation: String,
    val englishTranslation: String
) {
    /**
     * Returns a formatted reference string like "Chapter 2, Verse 47"
     */
    fun getReference(): String = "Chapter $chapter, Verse $verse"
    
    /**
     * Returns Sanskrit reference like "अध्याय २, श्लोक ४७"
     */
    fun getSanskritReference(): String {
        val chapterDevanagari = chapter.toDevanagariNumeral()
        val verseDevanagari = verse.toDevanagariNumeral()
        return "अध्याय $chapterDevanagari, श्लोक $verseDevanagari"
    }
    
    /**
     * Get the translation based on language preference
     * @param useHindi true for Hindi, false for English
     */
    fun getTranslation(useHindi: Boolean): String =
        if (useHindi) hindiTranslation else englishTranslation
    
    companion object {
        /**
         * Convert an integer to Devanagari numerals
         */
        private fun Int.toDevanagariNumeral(): String {
            val devanagariDigits = charArrayOf('०', '१', '२', '३', '४', '५', '६', '७', '८', '९')
            return this.toString().map { devanagariDigits[it - '0'] }.joinToString("")
        }
    }
}
