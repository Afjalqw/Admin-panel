package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.core.content.FileProvider
import com.example.model.OverlayText
import com.example.model.OverlayShape
import com.example.model.OverlaySignature
import com.example.model.ShapeType
import com.example.model.SelectedImagePage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PDFCompiler {

    // A4 Standard Dimensions at 72 PPI
    const val A4_WIDTH = 595
    const val A4_HEIGHT = 842

    suspend fun compile(
        context: Context,
        pages: List<SelectedImagePage>,
        pdfTitle: String
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()

        for ((index, pageData) in pages.withIndex()) {
            val isLandscape = pageData.isLandscape
            val pageWidth = if (isLandscape) A4_HEIGHT else A4_WIDTH
            val pageHeight = if (isLandscape) A4_WIDTH else A4_HEIGHT

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // Draw clean white background for non-image parts
            paint.color = 0xFFFFFFFF.toInt()
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paint)

            // 1. Load and compress bitmap
            val originalBitmap = try {
                context.contentResolver.openInputStream(pageData.uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                null
            }

            if (originalBitmap != null) {
                // Apply Image Compression Config
                val compressedBitmap = compressBitmap(originalBitmap, pageData.compressionRatio)

                // Render image with configured margins
                val margin = pageData.marginDp.toFloat() // In points
                val renderWidth = pageWidth - (margin * 2)
                val renderHeight = pageHeight - (margin * 2)

                if (renderWidth > 0 && renderHeight > 0) {
                    val srcRect = android.graphics.Rect(0, 0, compressedBitmap.width, compressedBitmap.height)
                    
                    // Fit image into parent bounded box centers
                    val scaleX = renderWidth / compressedBitmap.width
                    val scaleY = renderHeight / compressedBitmap.height
                    val scale = minOf(scaleX, scaleY)

                    val finalW = compressedBitmap.width * scale
                    val finalH = compressedBitmap.height * scale

                    val left = margin + (renderWidth - finalW) / 2
                    val top = margin + (renderHeight - finalH) / 2
                    val destRect = RectF(left, top, left + finalW, top + finalH)

                    canvas.drawBitmap(compressedBitmap, srcRect, destRect, paint)

                    // Draw Interactive Overlays (Texts, Shapes, Signatures)
                    // Everything coordinates scale based on destRect (image canvas area) or parent page canvas!
                    // Let's overlay onto the image content canvas destRect area for maximum relative alignment!
                    
                    // Draw Texts
                    drawOverlayTexts(canvas, destRect, pageData.overlayTexts)

                    // Draw Shapes
                    drawOverlayShapes(canvas, destRect, pageData.overlayShapes)

                    // Draw Signatures
                    drawOverlaySignatures(canvas, destRect, pageData.overlaySignatures)
                }
                
                if (compressedBitmap != originalBitmap) {
                    compressedBitmap.recycle()
                }
                originalBitmap.recycle()
            }

            pdfDocument.finishPage(page)
        }

        // Save completed PDF out to cache directory
        val outputDir = File(context.cacheDir, "compiled_pdfs").apply { mkdirs() }
        val filename = if (pdfTitle.endsWith(".pdf", ignoreCase = true)) pdfTitle else "$pdfTitle.pdf"
        val pdfFile = File(outputDir, filename)
        
        FileOutputStream(pdfFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()

        pdfFile
    }

    private fun compressBitmap(bitmap: Bitmap, quality: Int): Bitmap {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val byteArray = stream.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    private fun drawOverlayTexts(canvas: Canvas, area: RectF, texts: List<OverlayText>) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        for (textObj in texts) {
            textPaint.textSize = textObj.fontSize * 1.2f // scale up slightly for high PPI print
            textPaint.color = textObj.color
            
            // Map text relative coordinates to base rect bounds
            val drawX = area.left + (textObj.x * area.width())
            val drawY = area.top + (textObj.y * area.height())

            // Support multi-line draws in native canvas
            val lines = textObj.text.split("\n")
            var currentY = drawY
            val fontHeight = textPaint.fontSpacing
            for (line in lines) {
                canvas.drawText(line, drawX, currentY, textPaint)
                currentY += fontHeight
            }
        }
    }

    private fun drawOverlayShapes(canvas: Canvas, area: RectF, shapes: List<OverlayShape>) {
        val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        for (shape in shapes) {
            shapePaint.color = shape.color
            
            val left = area.left + (shape.x * area.width())
            val top = area.top + (shape.y * area.height())
            val width = shape.width * area.width()
            val height = shape.height * area.height()
            val right = left + width
            val bottom = top + height

            when (shape.type) {
                ShapeType.RECTANGLE -> {
                    canvas.drawRect(left, top, right, bottom, shapePaint)
                }
                ShapeType.CIRCLE -> {
                    canvas.drawOval(RectF(left, top, right, bottom), shapePaint)
                }
                ShapeType.ARROW -> {
                    // Draw a neat directional arrow
                    canvas.drawLine(left, top, right, bottom, shapePaint)
                    
                    // Simple arrow cap math
                    val dx = right - left
                    val dy = bottom - top
                    val angle = Math.atan2(dy.toDouble(), dx.toDouble())
                    val arrowLength = 15.0
                    val arrowAngle = Math.PI / 6.0 // 30 degrees

                    val path = Path()
                    path.moveTo(right, bottom)
                    path.lineTo(
                        (right - arrowLength * Math.cos(angle - arrowAngle)).toFloat(),
                        (bottom - arrowLength * Math.sin(angle - arrowAngle)).toFloat()
                    )
                    path.moveTo(right, bottom)
                    path.lineTo(
                        (right - arrowLength * Math.cos(angle + arrowAngle)).toFloat(),
                        (bottom - arrowLength * Math.sin(angle + arrowAngle)).toFloat()
                    )
                    canvas.drawPath(path, shapePaint)
                }
            }
        }
    }

    private fun drawOverlaySignatures(canvas: Canvas, area: RectF, signatures: List<OverlaySignature>) {
        val sigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (sig in signatures) {
            sigPaint.color = sig.color
            
            val left = area.left + (sig.x * area.width())
            val top = area.top + (sig.y * area.height())
            val width = sig.width * area.width()
            val height = sig.height * area.height()

            if (sig.points.size > 1) {
                val path = Path()
                // Format relative offset to dynamic area width
                val firstPt = sig.points.first()
                path.moveTo(left + firstPt.x * width, top + firstPt.y * height)

                for (i in 1 until sig.points.size) {
                    val pt = sig.points[i]
                    path.lineTo(left + pt.x * width, top + pt.y * height)
                }
                canvas.drawPath(path, sigPaint)
            }
        }
    }
}
