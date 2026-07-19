package com.example.model

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import java.io.File

data class OverlayText(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val x: Float, // relative ratio 0f..1f of canvas width
    val y: Float, // relative ratio 0f..1f of canvas height
    val fontSize: Float = 16f,
    val color: Int = Color.Black().hashCode()
)

enum class ShapeType {
    RECTANGLE, CIRCLE, ARROW
}

data class OverlayShape(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: ShapeType,
    val x: Float, // relative 0..1
    val y: Float, // relative 0..1
    val width: Float = 0.15f,  // relative width
    val height: Float = 0.1f, // relative height
    val color: Int = Color.Red().hashCode()
)

data class OverlaySignature(
    val id: String = java.util.UUID.randomUUID().toString(),
    val points: List<Offset>, // normalized offsets inside normalized boundary
    val x: Float, // relative 0..1 location
    val y: Float, // relative 0..1 location
    val width: Float = 0.3f,
    val height: Float = 0.15f,
    val color: Int = Color.Blue().hashCode()
)

data class SelectedImagePage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String = "Page",
    val compressionRatio: Int = 85, // 0..100
    val marginDp: Int = 16,        // 0, 16, 32
    val isLandscape: Boolean = false,
    val overlayTexts: List<OverlayText> = emptyList(),
    val overlayShapes: List<OverlayShape> = emptyList(),
    val overlaySignatures: List<OverlaySignature> = emptyList()
)

data class GeneratedPdfReport(
    val file: File,
    val displayName: String,
    val numPages: Int,
    val fileSizeFormatted: String,
    val dateString: String
)

private fun Color.Companion.Black(): Color = Color(0xFF000000)
private fun Color.Companion.Red(): Color = Color(0xFFE53935)
private fun Color.Companion.Blue(): Color = Color(0xFF1E88E5)
