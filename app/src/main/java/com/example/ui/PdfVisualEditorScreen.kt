package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.viewmodel.PDFViewModel
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Enum for selected editor tool
enum class EditorTool {
    PEN, HIGHLIGHT, TEXT, ERASER
}

// Data class representing a hand-drawn stroke in relative coordinates (0f to 1f)
data class RelativeStroke(
    val points: List<Offset>, // Normalized points (X/Width, Y/Height)
    val color: Color,
    val strokeWidth: Float,
    val isHighlight: Boolean = false
)

// Data class representing a text overlay
data class TextAnnotation(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val xPercent: Float, // Normalized horizontal position (0f to 1f)
    val yPercent: Float, // Normalized vertical position (0f to 1f)
    val color: Color,
    val fontSize: Float = 16f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfVisualEditorScreen(
    viewModel: PDFViewModel,
    fileUri: Uri,
    initialDisplayName: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Base files & states
    var resolvedFile by remember { mutableStateOf<File?>(null) }
    var totalPages by remember { mutableStateOf(0) }
    var currentPageIndex by remember { mutableStateOf(0) }
    var pageHeightByWidthRatio by remember { mutableStateOf(1.41f) } // default A4
    var isRenderingPage by remember { mutableStateOf(true) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Editor Tools states
    var selectedTool by remember { mutableStateOf(EditorTool.PEN) }
    var activeColor by remember { mutableStateOf(Color.Red) }
    var strokeWidth by remember { mutableStateOf(5f) }
    var isAnnotationsVisible by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Multi-page annotations map
    val pageStrokesMap = remember { mutableStateMapOf<Int, List<RelativeStroke>>() }
    val pageTextsMap = remember { mutableStateMapOf<Int, List<TextAnnotation>>() }
    
    // Undo / Redo stacks per page
    val pageUndoStrokesMap = remember { mutableStateMapOf<Int, List<List<RelativeStroke>>>() }
    val pageRedoStrokesMap = remember { mutableStateMapOf<Int, List<List<RelativeStroke>>>() }
    val pageUndoTextsMap = remember { mutableStateMapOf<Int, List<List<TextAnnotation>>>() }
    val pageRedoTextsMap = remember { mutableStateMapOf<Int, List<List<TextAnnotation>>>() }

    // Interactive Text edit/addition dialog controls
    var showTextDialog by remember { mutableStateOf(false) }
    var textDialogInput by remember { mutableStateOf("") }
    var textDialogSize by remember { mutableStateOf(16f) }
    var textDialogColor by remember { mutableStateOf(Color.Red) }
    var textDialogPlacementOffset by remember { mutableStateOf(Offset(0.5f, 0.5f)) } // relative
    var textToEditId by remember { mutableStateOf<String?>(null) }

    // Color options for drawing/text
    val colorsList = listOf(
        Color.Red,
        Color(0xFF3B82F6), // Cobalt Blue
        Color(0xFF10B981), // Emerald Green
        Color(0xFFF59E0B), // Amber Yellow
        Color.Black,
        Color.White
    )

    // Resolve file from Uri on load
    LaunchedEffect(fileUri) {
        withContext(Dispatchers.IO) {
            try {
                val file = if (fileUri.scheme == "file") {
                    File(fileUri.path ?: "")
                } else {
                    val cacheFile = File(context.cacheDir, "temp_editor.pdf")
                    context.contentResolver.openInputStream(fileUri)?.use { input ->
                        FileOutputStream(cacheFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    cacheFile
                }
                
                if (file.exists()) {
                    context.contentResolver.openFileDescriptor(Uri.fromFile(file), "r")?.use { pfd ->
                        android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                            totalPages = renderer.pageCount
                        }
                    }
                    withContext(Dispatchers.Main) {
                        resolvedFile = file
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_LONG).show()
                    onNavigateBack()
                }
            }
        }
    }

    // Load active page bitmap when current page or resolved file changes
    LaunchedEffect(resolvedFile, currentPageIndex) {
        val file = resolvedFile ?: return@LaunchedEffect
        isRenderingPage = true
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openFileDescriptor(Uri.fromFile(file), "r")?.use { pfd ->
                    android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                        if (currentPageIndex < renderer.pageCount) {
                            renderer.openPage(currentPageIndex).use { page ->
                                val targetWidth = 1000 // high-res visual canvas
                                val ratio = page.height.toFloat() / page.width.toFloat()
                                withContext(Dispatchers.Main) {
                                    pageHeightByWidthRatio = ratio
                                }
                                val targetHeight = (targetWidth * ratio).toInt()
                                val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bmp)
                                canvas.drawColor(android.graphics.Color.WHITE)
                                page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                
                                withContext(Dispatchers.Main) {
                                    pageBitmap = bmp
                                    isRenderingPage = false
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isRenderingPage = false
                    Toast.makeText(context, "Failed to render page: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Intercept back actions to confirm exit
    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PDF Visual Editor", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(initialDisplayName, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val original = resolvedFile
                            if (original != null && !isSaving) {
                                isSaving = true
                                coroutineScope.launch {
                                    try {
                                        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                        val outputName = "Edited_${initialDisplayName.substringBeforeLast(".pdf")}_${sdf.format(Date())}.pdf"
                                        val dir = File(context.cacheDir, "compiled_pdfs")
                                        if (!dir.exists()) dir.mkdirs()
                                        val destination = File(dir, outputName)

                                        withContext(Dispatchers.IO) {
                                            val doc = PDDocument.load(original)
                                            for (i in 0 until doc.numberOfPages) {
                                                val page = doc.getPage(i)
                                                val strokes = pageStrokesMap[i] ?: emptyList()
                                                val texts = pageTextsMap[i] ?: emptyList()

                                                if (strokes.isNotEmpty() || texts.isNotEmpty()) {
                                                    // Render high quality page, draw, and save back
                                                    context.contentResolver.openFileDescriptor(Uri.fromFile(original), "r")?.use { pfd ->
                                                        android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                                                            renderer.openPage(i).use { rPage ->
                                                                val scaleFactor = 3f // High quality save
                                                                val w = (rPage.width * scaleFactor).toInt()
                                                                val h = (rPage.height * scaleFactor).toInt()
                                                                
                                                                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                                                val canvas = android.graphics.Canvas(bmp)
                                                                canvas.drawColor(android.graphics.Color.WHITE)
                                                                rPage.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                                                // Draw hand-drawn lines
                                                                val paint = android.graphics.Paint().apply {
                                                                    isAntiAlias = true
                                                                    style = android.graphics.Paint.Style.STROKE
                                                                    strokeJoin = android.graphics.Paint.Join.ROUND
                                                                    strokeCap = android.graphics.Paint.Cap.ROUND
                                                                }

                                                                strokes.forEach { stroke ->
                                                                    if (stroke.points.size > 1) {
                                                                        paint.color = stroke.color.toArgb()
                                                                        paint.strokeWidth = stroke.strokeWidth * scaleFactor
                                                                        paint.alpha = if (stroke.isHighlight) (0.4f * 255).toInt() else 255
                                                                        
                                                                        val path = android.graphics.Path()
                                                                        val first = stroke.points.first()
                                                                        path.moveTo(first.x * w, first.y * h)
                                                                        for (idx in 1 until stroke.points.size) {
                                                                            val pt = stroke.points[idx]
                                                                            path.lineTo(pt.x * w, pt.y * h)
                                                                        }
                                                                        canvas.drawPath(path, paint)
                                                                    }
                                                                }

                                                                // Draw texts
                                                                val textPaint = android.graphics.Paint().apply {
                                                                    isAntiAlias = true
                                                                    style = android.graphics.Paint.Style.FILL
                                                                }

                                                                texts.forEach { textAnn ->
                                                                    textPaint.color = textAnn.color.toArgb()
                                                                    textPaint.textSize = textAnn.fontSize * scaleFactor
                                                                    textPaint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                                                                    
                                                                    val tx = textAnn.xPercent * w
                                                                    // Align text baseline
                                                                    val ty = textAnn.yPercent * h + (textAnn.fontSize * scaleFactor * 0.8f)
                                                                    canvas.drawText(textAnn.text, tx, ty, textPaint)
                                                                }

                                                                // Overwrite PDF page
                                                                val pdImage = JPEGFactory.createFromImage(doc, bmp)
                                                                val contentStream = PDPageContentStream(doc, page, PDPageContentStream.AppendMode.OVERWRITE, true, true)
                                                                contentStream.drawImage(pdImage, 0f, 0f, page.mediaBox.width, page.mediaBox.height)
                                                                contentStream.close()
                                                                bmp.recycle()
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            doc.save(destination)
                                            doc.close()
                                        }

                                        viewModel.loadHistory(context)
                                        Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                                        isSaving = false
                                        onNavigateBack()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                                        isSaving = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Save copy", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 90.dp), // space for bottom toolbar
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page selector header
                if (totalPages > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            enabled = currentPageIndex > 0,
                            onClick = { currentPageIndex-- }
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Page", tint = if (currentPageIndex > 0) Color.White else Color.DarkGray)
                        }
                        
                        Text(
                            text = "Page ${currentPageIndex + 1} of $totalPages",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        IconButton(
                            enabled = currentPageIndex < totalPages - 1,
                            onClick = { currentPageIndex++ }
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Page", tint = if (currentPageIndex < totalPages - 1) Color.White else Color.DarkGray)
                        }
                    }
                }

                // Interactive Annotation Canvas Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRenderingPage) {
                        CircularProgressIndicator(color = Color(0xFF3B82F6))
                    } else if (pageBitmap != null) {
                        var canvasWidth by remember { mutableStateOf(1f) }
                        var canvasHeight by remember { mutableStateOf(1f) }

                        // Outer card frame holding drawing and text content
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f / pageHeightByWidthRatio)
                                .onGloballyPositioned { coordinates ->
                                    canvasWidth = coordinates.size.width.toFloat()
                                    canvasHeight = coordinates.size.height.toFloat()
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    // Pointer Gestures: captures drawing, highlighting, erasing, or tapping
                                    .pointerInput(selectedTool, activeColor, strokeWidth, currentPageIndex, canvasWidth, canvasHeight) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                if (!isAnnotationsVisible) return@detectDragGestures
                                                
                                                val startNormalized = Offset(offset.x / canvasWidth, offset.y / canvasHeight)
                                                if (selectedTool == EditorTool.PEN || selectedTool == EditorTool.HIGHLIGHT) {
                                                    // Undo & Redo state management
                                                    val currentStrokes = pageStrokesMap[currentPageIndex] ?: emptyList()
                                                    val undoStack = pageUndoStrokesMap[currentPageIndex] ?: emptyList()
                                                    pageUndoStrokesMap[currentPageIndex] = undoStack + listOf(currentStrokes)
                                                    pageRedoStrokesMap[currentPageIndex] = emptyList() // Clear redo on new action
                                                    
                                                    val newStroke = RelativeStroke(
                                                        points = listOf(startNormalized),
                                                        color = activeColor,
                                                        strokeWidth = strokeWidth,
                                                        isHighlight = selectedTool == EditorTool.HIGHLIGHT
                                                    )
                                                    pageStrokesMap[currentPageIndex] = currentStrokes + newStroke
                                                } else if (selectedTool == EditorTool.ERASER) {
                                                    // Erase hand drawn strokes close to pointer
                                                    val currentStrokes = pageStrokesMap[currentPageIndex] ?: emptyList()
                                                    val hitRadius = 0.04f // relative percentage threshold
                                                    val keptStrokes = currentStrokes.filter { stroke ->
                                                        stroke.points.none { pt ->
                                                            (pt - startNormalized).getDistance() < hitRadius
                                                        }
                                                    }
                                                    if (keptStrokes.size != currentStrokes.size) {
                                                        val undoStack = pageUndoStrokesMap[currentPageIndex] ?: emptyList()
                                                        pageUndoStrokesMap[currentPageIndex] = undoStack + listOf(currentStrokes)
                                                        pageStrokesMap[currentPageIndex] = keptStrokes
                                                    }
                                                }
                                            },
                                            onDrag = { change, _ ->
                                                if (!isAnnotationsVisible) return@detectDragGestures
                                                change.consume()
                                                val currentPosNormalized = Offset(change.position.x / canvasWidth, change.position.y / canvasHeight)
                                                
                                                if (selectedTool == EditorTool.PEN || selectedTool == EditorTool.HIGHLIGHT) {
                                                    val currentStrokes = (pageStrokesMap[currentPageIndex] ?: emptyList()).toMutableList()
                                                    if (currentStrokes.isNotEmpty()) {
                                                        val lastStroke = currentStrokes.last()
                                                        val updatedPoints = lastStroke.points + currentPosNormalized
                                                        currentStrokes[currentStrokes.size - 1] = lastStroke.copy(points = updatedPoints)
                                                        pageStrokesMap[currentPageIndex] = currentStrokes
                                                    }
                                                } else if (selectedTool == EditorTool.ERASER) {
                                                    val currentStrokes = pageStrokesMap[currentPageIndex] ?: emptyList()
                                                    val hitRadius = 0.04f
                                                    val keptStrokes = currentStrokes.filter { stroke ->
                                                        stroke.points.none { pt ->
                                                            (pt - currentPosNormalized).getDistance() < hitRadius
                                                        }
                                                    }
                                                    if (keptStrokes.size != currentStrokes.size) {
                                                        val undoStack = pageUndoStrokesMap[currentPageIndex] ?: emptyList()
                                                        pageUndoStrokesMap[currentPageIndex] = undoStack + listOf(currentStrokes)
                                                        pageStrokesMap[currentPageIndex] = keptStrokes
                                                    }
                                                }
                                            }
                                        )
                                    }
                                    .pointerInput(selectedTool, currentPageIndex, canvasWidth, canvasHeight) {
                                        detectTapGestures { offset ->
                                            if (!isAnnotationsVisible) return@detectTapGestures
                                            val normalizedOffset = Offset(offset.x / canvasWidth, offset.y / canvasHeight)
                                            
                                            if (selectedTool == EditorTool.TEXT) {
                                                // Create a brand new text block at tapped coordinates
                                                textDialogInput = ""
                                                textDialogColor = activeColor
                                                textDialogSize = 16f
                                                textDialogPlacementOffset = normalizedOffset
                                                textToEditId = null
                                                showTextDialog = true
                                            }
                                        }
                                    }
                            ) {
                                // 1. Render Base PDF Page Image
                                androidx.compose.foundation.Image(
                                    bitmap = pageBitmap!!.asImageBitmap(),
                                    contentDescription = "PDF Page View",
                                    modifier = Modifier.fillMaxSize()
                                )

                                // 2. Draw Hand-drawn strokes & highlighters
                                if (isAnnotationsVisible) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val strokes = pageStrokesMap[currentPageIndex] ?: emptyList()
                                        strokes.forEach { stroke ->
                                            if (stroke.points.size > 1) {
                                                val path = Path()
                                                val firstPoint = stroke.points.first()
                                                path.moveTo(firstPoint.x * size.width, firstPoint.y * size.height)
                                                for (i in 1 until stroke.points.size) {
                                                    val pt = stroke.points[i]
                                                    path.lineTo(pt.x * size.width, pt.y * size.height)
                                                }
                                                drawPath(
                                                    path = path,
                                                    color = stroke.color,
                                                    alpha = if (stroke.isHighlight) 0.4f else 1f,
                                                    style = Stroke(
                                                        width = stroke.strokeWidth,
                                                        cap = StrokeCap.Round,
                                                        join = StrokeJoin.Round
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // 3. Render Interactive & Draggable Text Overlays
                                    val textAnnotations = pageTextsMap[currentPageIndex] ?: emptyList()
                                    textAnnotations.forEach { textAnn ->
                                        var relativeX by remember(textAnn.id) { mutableStateOf(textAnn.xPercent) }
                                        var relativeY by remember(textAnn.id) { mutableStateOf(textAnn.yPercent) }

                                        Box(
                                            modifier = Modifier
                                                .offset {
                                                    IntOffset(
                                                        (relativeX * canvasWidth).roundToInt(),
                                                        (relativeY * canvasHeight).roundToInt()
                                                    )
                                                }
                                                .wrapContentSize()
                                                .border(
                                                    width = if (selectedTool == EditorTool.ERASER) 1.dp else 0.dp,
                                                    color = if (selectedTool == EditorTool.ERASER) Color.Red.copy(alpha = 0.5f) else Color.Transparent,
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                // Handle text block drag movement
                                                .pointerInput(textAnn.id, canvasWidth, canvasHeight) {
                                                    detectDragGestures(
                                                        onDragEnd = {
                                                            // Save new position back into page list
                                                            val currentList = pageTextsMap[currentPageIndex] ?: emptyList()
                                                            val updatedList = currentList.map {
                                                                if (it.id == textAnn.id) {
                                                                    it.copy(xPercent = relativeX, yPercent = relativeY)
                                                                } else it
                                                            }
                                                            pageTextsMap[currentPageIndex] = updatedList
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            if (selectedTool == EditorTool.PEN || selectedTool == EditorTool.HIGHLIGHT) return@detectDragGestures
                                                            
                                                            relativeX = (relativeX + dragAmount.x / canvasWidth).coerceIn(0f, 0.9f)
                                                            relativeY = (relativeY + dragAmount.y / canvasHeight).coerceIn(0f, 0.9f)
                                                        }
                                                    )
                                                }
                                                .clickable {
                                                    if (selectedTool == EditorTool.ERASER) {
                                                        // Eraser tool deletes text blocks immediately
                                                        val currentList = pageTextsMap[currentPageIndex] ?: emptyList()
                                                        val undoStack = pageUndoTextsMap[currentPageIndex] ?: emptyList()
                                                        pageUndoTextsMap[currentPageIndex] = undoStack + listOf(currentList)
                                                        pageTextsMap[currentPageIndex] = currentList.filter { it.id != textAnn.id }
                                                        Toast.makeText(context, "Text removed", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        // Other tools tap open the text editing panel
                                                        textToEditId = textAnn.id
                                                        textDialogInput = textAnn.text
                                                        textDialogSize = textAnn.fontSize
                                                        textDialogColor = textAnn.color
                                                        showTextDialog = true
                                                    }
                                                }
                                                .padding(4.dp)
                                        ) {
                                            Text(
                                                text = textAnn.text,
                                                color = textAnn.color,
                                                fontSize = textAnn.fontSize.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // FLOATING PILL TOOLBAR (Pen, Highlight, Text, Eraser, Undo, Redo, Hide/Show Annotations)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(0.95f),
                shape = RoundedCornerShape(30.dp),
                color = Color(0xFF1E293B),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tool Configuration Drawer (Displays color selection or stroke width sliders)
                    if (selectedTool == EditorTool.PEN || selectedTool == EditorTool.HIGHLIGHT || selectedTool == EditorTool.TEXT) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Color circle selectors
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                colorsList.forEach { color ->
                                    val isSelected = activeColor == color
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF3B82F6) else Color.DarkGray,
                                                shape = CircleShape
                                            )
                                            .clickable { activeColor = color }
                                    )
                                }
                            }

                            // Size adjustment slider
                            if (selectedTool != EditorTool.TEXT) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text("Size:", color = Color.Gray, fontSize = 11.sp)
                                    Slider(
                                        value = strokeWidth,
                                        onValueChange = { strokeWidth = it },
                                        valueRange = 2f..30f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF3B82F6),
                                            activeTrackColor = Color(0xFF3B82F6),
                                            inactiveTrackColor = Color.DarkGray
                                        ),
                                        modifier = Modifier.width(100.dp)
                                    )
                                    Text("${strokeWidth.toInt()}px", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                    }

                    // Main Tool Selection row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Pen Tool Button
                        ToolbarIcon(
                            icon = Icons.Default.Edit,
                            description = "Draw/Pen",
                            isSelected = selectedTool == EditorTool.PEN,
                            onClick = { selectedTool = EditorTool.PEN }
                        )

                        // 2. Highlight Tool Button
                        ToolbarIcon(
                            icon = Icons.Default.BorderColor,
                            description = "Highlight",
                            isSelected = selectedTool == EditorTool.HIGHLIGHT,
                            onClick = { selectedTool = EditorTool.HIGHLIGHT }
                        )

                        // 3. Text Overlay Button
                        ToolbarIcon(
                            icon = Icons.Default.TextFields,
                            description = "Text Box",
                            isSelected = selectedTool == EditorTool.TEXT,
                            onClick = { selectedTool = EditorTool.TEXT }
                        )

                        // 4. Eraser Button
                        ToolbarIcon(
                            icon = Icons.Default.LayersClear,
                            description = "Eraser",
                            isSelected = selectedTool == EditorTool.ERASER,
                            onClick = { selectedTool = EditorTool.ERASER }
                        )

                        // Vertical Separator
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.DarkGray))

                        // 5. Undo Button
                        val canUndoStrokes = (pageUndoStrokesMap[currentPageIndex]?.isNotEmpty() == true)
                        val canUndoTexts = (pageUndoTextsMap[currentPageIndex]?.isNotEmpty() == true)
                        IconButton(
                            enabled = canUndoStrokes || canUndoTexts,
                            onClick = {
                                if (canUndoStrokes) {
                                    val currentStrokes = pageStrokesMap[currentPageIndex] ?: emptyList()
                                    val undoStack = pageUndoStrokesMap[currentPageIndex] ?: emptyList()
                                    val redoStack = pageRedoStrokesMap[currentPageIndex] ?: emptyList()
                                    
                                    val previousState = undoStack.last()
                                    pageUndoStrokesMap[currentPageIndex] = undoStack.dropLast(1)
                                    pageRedoStrokesMap[currentPageIndex] = redoStack + listOf(currentStrokes)
                                    pageStrokesMap[currentPageIndex] = previousState
                                }
                                if (canUndoTexts) {
                                    val currentTexts = pageTextsMap[currentPageIndex] ?: emptyList()
                                    val undoStack = pageUndoTextsMap[currentPageIndex] ?: emptyList()
                                    val redoStack = pageRedoTextsMap[currentPageIndex] ?: emptyList()
                                    
                                    val previousState = undoStack.last()
                                    pageUndoTextsMap[currentPageIndex] = undoStack.dropLast(1)
                                    pageRedoTextsMap[currentPageIndex] = redoStack + listOf(currentTexts)
                                    pageTextsMap[currentPageIndex] = previousState
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Undo",
                                tint = if (canUndoStrokes || canUndoTexts) Color.White else Color.DarkGray
                            )
                        }

                        // 6. Redo Button
                        val canRedoStrokes = (pageRedoStrokesMap[currentPageIndex]?.isNotEmpty() == true)
                        val canRedoTexts = (pageRedoTextsMap[currentPageIndex]?.isNotEmpty() == true)
                        IconButton(
                            enabled = canRedoStrokes || canRedoTexts,
                            onClick = {
                                if (canRedoStrokes) {
                                    val currentStrokes = pageStrokesMap[currentPageIndex] ?: emptyList()
                                    val undoStack = pageUndoStrokesMap[currentPageIndex] ?: emptyList()
                                    val redoStack = pageRedoStrokesMap[currentPageIndex] ?: emptyList()
                                    
                                    val nextState = redoStack.last()
                                    pageRedoStrokesMap[currentPageIndex] = redoStack.dropLast(1)
                                    pageUndoStrokesMap[currentPageIndex] = undoStack + listOf(currentStrokes)
                                    pageStrokesMap[currentPageIndex] = nextState
                                }
                                if (canRedoTexts) {
                                    val currentTexts = pageTextsMap[currentPageIndex] ?: emptyList()
                                    val undoStack = pageUndoTextsMap[currentPageIndex] ?: emptyList()
                                    val redoStack = pageRedoTextsMap[currentPageIndex] ?: emptyList()
                                    
                                    val nextState = redoStack.last()
                                    pageRedoTextsMap[currentPageIndex] = redoStack.dropLast(1)
                                    pageUndoTextsMap[currentPageIndex] = undoStack + listOf(currentTexts)
                                    pageTextsMap[currentPageIndex] = nextState
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Redo,
                                contentDescription = "Redo",
                                tint = if (canRedoStrokes || canRedoTexts) Color.White else Color.DarkGray
                            )
                        }

                        // 7. Hide/Show Annotations eye icon
                        IconButton(onClick = { isAnnotationsVisible = !isAnnotationsVisible }) {
                            Icon(
                                imageVector = if (isAnnotationsVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Visibility",
                                tint = if (isAnnotationsVisible) Color(0xFF3B82F6) else Color.LightGray
                            )
                        }
                    }
                }
            }

            // Saving progress modal overlay
            if (isSaving) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator(color = Color(0xFF3B82F6))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Burning annotations to PDF copy...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("This makes changes 100% permanent & high-res", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // INTERACTIVE TEXT DIALOG (Insert & Edit values, size, and colors)
    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            containerColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = if (textToEditId == null) "Add Text Block" else "Edit Text Block",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = textDialogInput,
                        onValueChange = { textDialogInput = it },
                        placeholder = { Text("Enter text here...", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Text Color choice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Text Color:", color = Color.LightGray, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            colorsList.forEach { col ->
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .border(
                                            width = if (textDialogColor == col) 2.dp else 1.dp,
                                            color = if (textDialogColor == col) Color.White else Color.DarkGray,
                                            shape = CircleShape
                                        )
                                        .clickable { textDialogColor = col }
                                )
                            }
                        }
                    }

                    // Font Size slider choice
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Font Size:", color = Color.LightGray, fontSize = 13.sp)
                            Text("${textDialogSize.toInt()} sp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Slider(
                            value = textDialogSize,
                            onValueChange = { textDialogSize = it },
                            valueRange = 10f..40f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF3B82F6),
                                activeTrackColor = Color(0xFF3B82F6),
                                inactiveTrackColor = Color.DarkGray
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textDialogInput.trim().isNotEmpty()) {
                            val currentList = pageTextsMap[currentPageIndex] ?: emptyList()
                            val undoStack = pageUndoTextsMap[currentPageIndex] ?: emptyList()
                            pageUndoTextsMap[currentPageIndex] = undoStack + listOf(currentList)
                            pageRedoTextsMap[currentPageIndex] = emptyList() // clear redo

                            val editId = textToEditId
                            if (editId != null) {
                                // Edit mode: replace text
                                val updated = currentList.map {
                                    if (it.id == editId) {
                                        it.copy(
                                            text = textDialogInput.trim(),
                                            color = textDialogColor,
                                            fontSize = textDialogSize
                                        )
                                    } else it
                                }
                                pageTextsMap[currentPageIndex] = updated
                            } else {
                                // Create mode: append text
                                val newAnn = TextAnnotation(
                                    text = textDialogInput.trim(),
                                    xPercent = textDialogPlacementOffset.x,
                                    yPercent = textDialogPlacementOffset.y,
                                    color = textDialogColor,
                                    fontSize = textDialogSize
                                )
                                pageTextsMap[currentPageIndex] = currentList + newAnn
                            }
                        }
                        showTextDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("OK", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }
}

// Custom styled Icon wrapper for the bottom bar buttons
@Composable
fun ToolbarIcon(
    icon: ImageVector,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.2f) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (isSelected) Color(0xFF3B82F6) else Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}
