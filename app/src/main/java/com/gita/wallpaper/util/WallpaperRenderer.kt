package com.gita.wallpaper.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.DisplayMetrics
import android.view.WindowManager
import com.gita.wallpaper.data.GitaVerse

/**
 * WallpaperRenderer - Creates beautiful wallpaper bitmaps from Gita verses.
 *
 * This class is responsible for:
 * 1. Generating screen-sized bitmaps with gradient backgrounds
 * 2. Rendering Sanskrit text in large Devanagari font
 * 3. Rendering translation (Hindi/English) in smaller font below
 * 4. Adding verse reference at the bottom
 *
 * Design decisions:
 * - Uses Canvas and Paint for pure Android rendering (no external libs)
 * - Gradient background for visual appeal
 * - Golden/wheat colors for text to match spiritual aesthetic
 * - Text wrapping handled by StaticLayout for proper line breaks
 *
 * @property context Application context for display metrics and resources
 */
class WallpaperRenderer(private val context: Context) {

    /**
     * Render a Gita verse into a wallpaper bitmap.
     *
     * @param verse The verse to render
     * @param useHindi Whether to show Hindi (true) or English (false) translation
     * @return Bitmap suitable for setting as system wallpaper
     */
    fun renderVerse(verse: GitaVerse, useHindi: Boolean): Bitmap {
        val (width, height) = getScreenDimensions()
        
        // Create bitmap with ARGB_8888 for high quality
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw gradient background
        drawGradientBackground(canvas, width, height)
        
        // Calculate text areas - increased padding for better margins
        val padding = width * 0.15f // 15% padding on each side to prevent overflow
        val textWidth = width - (padding * 2)
        val centerX = width / 2f
        
        // Draw decorative element at top
        drawDecorativeElement(canvas, centerX, height * 0.12f)
        
        // Draw Sanskrit text (primary, larger)
        val sanskritPaint = createSanskritPaint(width)
        val sanskritLayout = createStaticLayout(verse.sanskritText, sanskritPaint, textWidth.toInt())
        
        // Position Sanskrit text in upper portion
        val sanskritY = height * 0.18f
        canvas.save()
        canvas.translate(padding, sanskritY)
        sanskritLayout.draw(canvas)
        canvas.restore()
        
        // Draw translation (secondary, smaller)
        val translation = verse.getTranslation(useHindi)
        val translationPaint = createTranslationPaint(width)
        val translationLayout = createStaticLayout(translation, translationPaint, textWidth.toInt())
        
        // Position translation below Sanskrit
        val translationY = sanskritY + sanskritLayout.height + (height * 0.06f)
        canvas.save()
        canvas.translate(padding, translationY)
        translationLayout.draw(canvas)
        canvas.restore()
        
        // Draw verse reference at bottom
        drawVerseReference(canvas, verse, width.toFloat(), height.toFloat(), useHindi)
        
        // Draw decorative element at bottom
        drawDecorativeElement(canvas, centerX, height * 0.92f)
        
        return bitmap
    }

    /**
     * Get device screen dimensions for wallpaper sizing.
     * IMPORTANT: Uses actual screen size (not wallpaper desired size) to ensure
     * the wallpaper fits the lock screen without horizontal scrolling.
     */
    private fun getScreenDimensions(): Pair<Int, Int> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        
        // Use actual screen dimensions for proper lock screen fit
        // Don't use WallpaperManager.desiredMinimumWidth as it can be wider (for scrolling wallpapers)
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        
        return Pair(width, height)
    }

    /**
     * Draw a vertical gradient background.
     * Dark maroon/brown gradient for spiritual, elegant look.
     */
    private fun drawGradientBackground(canvas: Canvas, width: Int, height: Int) {
        val gradient = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(
                Color.parseColor("#1A0A0A"),  // Dark top
                Color.parseColor("#2D1810"),  // Warm middle
                Color.parseColor("#1A0A0A")   // Dark bottom
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        
        val paint = Paint().apply {
            shader = gradient
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        // Add subtle texture overlay
        addTextureOverlay(canvas, width, height)
    }

    /**
     * Add subtle noise texture to background for depth.
     */
    private fun addTextureOverlay(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint().apply {
            color = Color.parseColor("#10FFFFFF") // Very subtle white overlay
            style = Paint.Style.FILL
        }
        
        // Draw subtle radial gradient from center for vignette effect
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = maxOf(width, height) * 0.8f
        
        val radialGradient = RadialGradient(
            centerX, centerY, radius,
            intArrayOf(
                Color.parseColor("#15FFD700"), // Subtle golden center
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        
        paint.shader = radialGradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    /**
     * Create TextPaint for Sanskrit text (primary, larger).
     * Uses default system font which supports Devanagari.
     */
    private fun createSanskritPaint(screenWidth: Int): TextPaint {
        return TextPaint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FFD700") // Golden
            textSize = screenWidth * 0.05f // 5% of screen width - slightly larger for readability
            textAlign = Paint.Align.LEFT
            // Use serif typeface for more traditional look
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            // Add subtle shadow for depth
            setShadowLayer(4f, 2f, 2f, Color.parseColor("#40000000"))
        }
    }

    /**
     * Create TextPaint for translation text (secondary, smaller).
     */
    private fun createTranslationPaint(screenWidth: Int): TextPaint {
        return TextPaint().apply {
            isAntiAlias = true
            color = Color.parseColor("#F5DEB3") // Wheat
            textSize = screenWidth * 0.036f // 3.6% of screen width - slightly larger for readability
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            // Subtle shadow
            setShadowLayer(3f, 1f, 1f, Color.parseColor("#30000000"))
        }
    }

    /**
     * Create StaticLayout for text wrapping.
     * Handles multi-line text properly with line spacing.
     */
    @Suppress("DEPRECATION")
    private fun createStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(8f, 1.3f) // Extra line spacing for readability
                .setIncludePad(true)
                .build()
        } else {
            StaticLayout(
                text, paint, width,
                Layout.Alignment.ALIGN_CENTER,
                1.3f, 8f, true
            )
        }
    }

    /**
     * Draw verse reference (chapter and verse number) at bottom.
     */
    private fun drawVerseReference(
        canvas: Canvas,
        verse: GitaVerse,
        width: Float,
        height: Float,
        useHindi: Boolean
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#B8860B") // Dark golden
            textSize = width * 0.032f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
        
        val referenceText = if (useHindi) {
            verse.getSanskritReference()
        } else {
            verse.getReference()
        }
        
        canvas.drawText(referenceText, width / 2f, height * 0.88f, paint)
    }

    /**
     * Draw decorative separator/ornament.
     * Simple elegant line with dots.
     */
    private fun drawDecorativeElement(canvas: Canvas, centerX: Float, y: Float) {
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#80FFD700") // Semi-transparent gold
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        
        val lineWidth = 100f
        
        // Draw center dot
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, y, 4f, paint)
        
        // Draw side dots
        canvas.drawCircle(centerX - lineWidth / 2, y, 3f, paint)
        canvas.drawCircle(centerX + lineWidth / 2, y, 3f, paint)
        
        // Draw connecting lines
        paint.style = Paint.Style.STROKE
        canvas.drawLine(centerX - lineWidth / 2 + 8, y, centerX - 10, y, paint)
        canvas.drawLine(centerX + 10, y, centerX + lineWidth / 2 - 8, y, paint)
    }

    companion object {
        private const val TAG = "WallpaperRenderer"
    }
}
