package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.Hadith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class HadithCardTheme(
    val title: String,
    val startColor: String,
    val endColor: String,
    val frameColor: String,
    val textColor: String,
    val accentColor: String
) {
    EMERALD_GOLD("Zümrüt & Altın", "#064e3b", "#022c22", "#D4AF37", "#FFFFFF", "#fef08a"),
    SAPPHIRE_SILVER("Safir & Gümüş", "#1e3a8a", "#172554", "#CBD5E1", "#FFFFFF", "#93c5fd"),
    MIDNIGHT_ONYX("Siyah Asalet", "#18181b", "#09090b", "#C5A028", "#FFFFFF", "#fef08a"),
    ROSE_GOLD("Gül Bahçesi", "#881337", "#4c0519", "#D4AF37", "#FFFFFF", "#fecdd3"),
    WARM_TERRACOTTA("Toprak Sıcaklığı", "#7c2d12", "#431407", "#FDE68A", "#FFFFFF", "#fef3c7")
}

object HadithCardGenerator {

    suspend fun shareHadithCard(
        context: Context,
        hadith: Hadith,
        theme: HadithCardTheme,
        textSizeSp: Float,
        includeArabic: Boolean
    ) = withContext(Dispatchers.IO) {
        try {
            val bitmap = generateBitmap(hadith, theme, textSizeSp, includeArabic)
            
            // Save to cache dir for sharing
            val cachePath = File(context.cacheDir, "hadith_cards")
            cachePath.mkdirs()
            val file = File(cachePath, "hadis_kart_${hadith.id}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(uri, context.contentResolver.getType(uri))
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "“${hadith.text}” - ${hadith.source}")
            }
            
            withContext(Dispatchers.Main) {
                context.startActivity(Intent.createChooser(shareIntent, "Hadis Kartını Paylaş"))
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Paylaşım başarısız: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateBitmap(
        hadith: Hadith,
        theme: HadithCardTheme,
        textSizeSp: Float,
        includeArabic: Boolean
    ): Bitmap {
        val width = 1080
        val height = 1080 // Square format
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 1. Draw Background Gradient
        val bgPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                android.graphics.Color.parseColor(theme.startColor),
                android.graphics.Color.parseColor(theme.endColor),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        
        // Colors from theme
        val colorFrame = android.graphics.Color.parseColor(theme.frameColor)
        val colorText = android.graphics.Color.parseColor(theme.textColor)
        val colorAccent = android.graphics.Color.parseColor(theme.accentColor)
        
        // 2. Draw Islamic Outer Frame Border
        val borderPaint = Paint().apply {
            color = colorFrame
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        val thinBorderPaint = Paint().apply {
            color = colorFrame
            alpha = 130
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        
        val inset = 50f
        val innerInset = 62f
        
        // Draw double line border
        canvas.drawRect(inset, inset, width - inset, height - inset, borderPaint)
        canvas.drawRect(innerInset, innerInset, width - innerInset, height - innerInset, thinBorderPaint)
        
        // 3. Draw Islamic Corner Ornaments
        val cornerLength = 45f
        // Top-Left corner
        canvas.drawLine(innerInset, innerInset + cornerLength, innerInset + cornerLength, innerInset, borderPaint)
        // Top-Right corner
        canvas.drawLine(width - innerInset, innerInset + cornerLength, width - innerInset - cornerLength, innerInset, borderPaint)
        // Bottom-Left corner
        canvas.drawLine(innerInset, height - innerInset - cornerLength, innerInset + cornerLength, height - innerInset, borderPaint)
        // Bottom-Right corner
        canvas.drawLine(width - innerInset, height - innerInset - cornerLength, width - innerInset - cornerLength, height - innerInset, borderPaint)
        
        // 4. Draw "Hadis-i Şerif" Header Banner
        val headerPaint = Paint().apply {
            color = colorFrame
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val headerRect = RectF(width / 2f - 180f, innerInset - 10f, width / 2f + 180f, innerInset + 40f)
        canvas.drawRoundRect(headerRect, 10f, 10f, headerPaint)
        
        val headerTextPaint = TextPaint().apply {
            color = android.graphics.Color.parseColor(theme.endColor)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("HADİS-İ ŞERİF", width / 2f, innerInset + 24f, headerTextPaint)
        
        // 5. Draw Content
        val contentWidth = width - 200
        var currentY = 180f
        
        // Draw Arabic Text (if requested and exists)
        if (includeArabic && !hadith.arabicText.isNullOrEmpty()) {
            val arabicTextPaint = TextPaint().apply {
                color = colorAccent
                textSize = 34f
                typeface = Typeface.create("serif", Typeface.BOLD)
                isAntiAlias = true
            }
            
            val builder = StaticLayout.Builder.obtain(hadith.arabicText, 0, hadith.arabicText.length, arabicTextPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1.3f)
                .setIncludePad(false)
            val staticLayout = builder.build()
            
            canvas.save()
            canvas.translate(100f, currentY)
            staticLayout.draw(canvas)
            canvas.restore()
            
            currentY += staticLayout.height + 40f
            
            // Decorative Divider line
            val divPaint = Paint().apply {
                color = colorFrame
                alpha = 80
                strokeWidth = 2f
                isAntiAlias = true
            }
            canvas.drawLine(width / 2f - 100f, currentY, width / 2f + 100f, currentY, divPaint)
            currentY += 40f
        } else {
            currentY += 80f // Offset if no Arabic is shown
        }
        
        // Calculate dynamic Turkish text size or use slider size
        val textPaint = TextPaint().apply {
            color = colorText
            textSize = textSizeSp * 2.2f // scale sp to match 1080px canvas context
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        
        val turkishText = "“${hadith.text}”"
        val turkishBuilder = StaticLayout.Builder.obtain(turkishText, 0, turkishText.length, textPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.25f)
            .setIncludePad(false)
        val turkishLayout = turkishBuilder.build()
        
        // Center text vertically if space allows
        val remainingSpaceHeight = (height - innerInset - 100f) - currentY
        if (remainingSpaceHeight > turkishLayout.height) {
            val offset = (remainingSpaceHeight - turkishLayout.height) / 2.5f
            currentY += offset
        }
        
        canvas.save()
        canvas.translate(100f, currentY)
        turkishLayout.draw(canvas)
        canvas.restore()
        
        currentY += turkishLayout.height + 35f
        
        // 6. Draw Source Book Ribbon/Text
        val sourcePaint = TextPaint().apply {
            color = colorAccent
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        canvas.drawText(hadith.source, width / 2f, height - innerInset - 50f, sourcePaint)
        
        return bitmap
    }
}
