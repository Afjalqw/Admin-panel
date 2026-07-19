package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.viewmodel.PDFViewModel
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class SplitMode {
    EVERY_PAGE, RANGE, EXTRACT, CUSTOM_INTERVAL
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SplitPdfScreen(
    viewModel: PDFViewModel,
    onNavigateBack: () -> Unit,
    onPreviewPdf: (Uri, String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfName by remember { mutableStateOf("") }
    var pdfPageCount by remember { mutableStateOf(0) }
    var pdfSizeFormatted by remember { mutableStateOf("") }

    // Split Configurations
    var selectedSplitMode by remember { mutableStateOf(SplitMode.EVERY_PAGE) }
    var rangeInput by remember { mutableStateOf("1-2, 3-4") }
    var intervalInput by remember { mutableStateOf("2") } // Split every N pages
    val selectedPagesList = remember { mutableStateListOf<Int>() } // selected indices (0-indexed)

    // Zoom Preview Dialog
    var zoomedPageIndex by remember { mutableStateOf<Int?>(null) }

    // Processing States
    var isSplitting by remember { mutableStateOf(false) }
    var splitProgress by remember { mutableStateOf(0f) }
    var splitJob by remember { mutableStateOf<Job?>(null) }
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }
    var splitResultsDialog by remember { mutableStateOf<List<File>?>(null) }

    // PDF Pick Launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedPdfUri = uri
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val name = getFileNameFromUri(context, uri)
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        val count = android.graphics.pdf.PdfRenderer(pfd).use { it.pageCount }
                        val sizeStr = formatBytes(pfd.statSize)
                        
                        withContext(Dispatchers.Main) {
                            pdfName = name
                            pdfPageCount = count
                            pdfSizeFormatted = sizeStr
                            
                            // Pre-select all pages by default
                            selectedPagesList.clear()
                            for (i in 0 until count) {
                                selectedPagesList.add(i)
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        errorDialogMessage = "Failed to load PDF. It may be corrupted or encrypted."
                    }
                }
            }
        }
    }

    // Auto-launch picker
    LaunchedEffect(Unit) {
        if (selectedPdfUri == null) {
            pdfPickerLauncher.launch("application/pdf")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Split PDF Document", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("split_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (selectedPdfUri == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No PDF File Selected", color = Color.Gray, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { pdfPickerLauncher.launch("application/pdf") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            modifier = Modifier.testTag("split_select_btn")
                        ) {
                            Text("Select PDF File", color = Color.White)
                        }
                    }
                }
            } else {
                // PDF Selected Card Info
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Red, modifier = Modifier.size(28.dp))
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                pdfName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "$pdfPageCount Pages • $pdfSizeFormatted",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }

                        IconButton(onClick = { selectedPdfUri = null }) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Change PDF", tint = Color(0xFF3B82F6))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Split Modes Selector Row
                Text("Select Splitting Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeChip(text = "Split Every Page", selected = selectedSplitMode == SplitMode.EVERY_PAGE) {
                        selectedSplitMode = SplitMode.EVERY_PAGE
                    }
                    ModeChip(text = "Page Range", selected = selectedSplitMode == SplitMode.RANGE) {
                        selectedSplitMode = SplitMode.RANGE
                    }
                    ModeChip(text = "Extract Selected", selected = selectedSplitMode == SplitMode.EXTRACT) {
                        selectedSplitMode = SplitMode.EXTRACT
                    }
                    ModeChip(text = "Custom Interval", selected = selectedSplitMode == SplitMode.CUSTOM_INTERVAL) {
                        selectedSplitMode = SplitMode.CUSTOM_INTERVAL
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Configure view matching chosen Split Mode
                when (selectedSplitMode) {
                    SplitMode.EVERY_PAGE -> {
                        Text(
                            "This will split the document into $pdfPageCount individual PDF files, one for each page.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                    SplitMode.RANGE -> {
                        Column {
                            Text("Enter Page Ranges", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Separate multiple ranges with commas (e.g. 1-3, 4-7, 8-10)", color = Color.LightGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = rangeInput,
                                onValueChange = { rangeInput = it },
                                placeholder = { Text("e.g. 1-5, 6-10", color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    unfocusedBorderColor = Color(0xFF475569)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("split_range_input")
                            )
                        }
                    }
                    SplitMode.EXTRACT -> {
                        Text(
                            "Select the desired pages from the preview grid below. We will extract only those pages into a new single PDF.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                    SplitMode.CUSTOM_INTERVAL -> {
                        Column {
                            Text("Split Every N Pages", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Create new documents containing chunks of N pages each", color = Color.LightGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = intervalInput,
                                onValueChange = { intervalInput = it },
                                placeholder = { Text("e.g. 2", color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    unfocusedBorderColor = Color(0xFF475569)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("split_interval_input")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Page Preview Grid Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Pages Preview (${selectedPagesList.size} / $pdfPageCount Selected)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    Row {
                        TextButton(onClick = {
                            selectedPagesList.clear()
                            for (i in 0 until pdfPageCount) selectedPagesList.add(i)
                        }) {
                            Text("Select All", color = Color(0xFF3B82F6), fontSize = 12.sp)
                        }
                        TextButton(onClick = {
                            selectedPagesList.clear()
                        }) {
                            Text("Deselect All", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Page Preview Grid (LAZY LOADED & MEMORY OPTIMIZED)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed((0 until pdfPageCount).toList()) { _, pageIndex ->
                        val isSelected = selectedPagesList.contains(pageIndex)
                        PagePreviewCard(
                            context = context,
                            pdfUri = selectedPdfUri!!,
                            pageIndex = pageIndex,
                            isSelected = isSelected,
                            onSelectionChanged = { checked ->
                                if (checked) {
                                    if (!selectedPagesList.contains(pageIndex)) {
                                        selectedPagesList.add(pageIndex)
                                    }
                                } else {
                                    selectedPagesList.remove(pageIndex)
                                }
                            },
                            onZoomClick = {
                                zoomedPageIndex = pageIndex
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Compile Button
                Button(
                    onClick = {
                        if (selectedSplitMode == SplitMode.EXTRACT && selectedPagesList.isEmpty()) {
                            errorDialogMessage = "Please select at least one page to extract."
                            return@Button
                        }
                        isSplitting = true
                        splitProgress = 0f
                        
                        splitJob = coroutineScope.launch {
                            performSplitProcess(
                                context = context,
                                pdfUri = selectedPdfUri!!,
                                pdfName = pdfName,
                                mode = selectedSplitMode,
                                rangeStr = rangeInput,
                                intervalStr = intervalInput,
                                selectedPages = selectedPagesList.toList(),
                                viewModel = viewModel,
                                onProgress = { progress -> splitProgress = progress },
                                onSuccess = { files ->
                                    isSplitting = false
                                    splitResultsDialog = files
                                },
                                onFailure = { errorMsg ->
                                    isSplitting = false
                                    errorDialogMessage = errorMsg
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("split_pdf_compile_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CallSplit, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Split PDF Now", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Split Processing Dialog
        if (isSplitting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = { splitProgress },
                            color = Color(0xFF3B82F6),
                            strokeWidth = 5.dp,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "Splitting PDF Document...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${(splitProgress * 100).toInt()}% Completed",
                            color = Color(0xFF3B82F6),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Processing ranges and exporting new files. Please wait...",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(
                            onClick = {
                                splitJob?.cancel()
                                isSplitting = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel Operation")
                        }
                    }
                }
            }
        }

        // ZOOM / FULL VIEW PINCH-ZOOM DIALOG
        if (zoomedPageIndex != null) {
            ZoomPreviewDialog(
                context = context,
                pdfUri = selectedPdfUri!!,
                pageIndex = zoomedPageIndex!!,
                onDismiss = { zoomedPageIndex = null }
            )
        }

        // SPLIT SUCCESS DIALOG
        if (splitResultsDialog != null) {
            AlertDialog(
                onDismissRequest = { splitResultsDialog = null },
                containerColor = Color(0xFF1E293B),
                title = { Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PDF Split Successfully!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }},
                text = {
                    Column {
                        Text("Exported ${splitResultsDialog!!.size} PDF files to public Documents directory:", color = Color.LightGray, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp)
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                splitResultsDialog!!.forEach { file ->
                                    Text(
                                        "• ${file.name}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val firstFile = splitResultsDialog!!.firstOrNull()
                            splitResultsDialog = null
                            onNavigateBack()
                            if (firstFile != null) {
                                onPreviewPdf(Uri.fromFile(firstFile), firstFile.name)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text("Open First File", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        splitResultsDialog = null
                        onNavigateBack()
                    }) {
                        Text("Done", color = Color.LightGray)
                    }
                }
            )
        }

        // ERROR DIALOG
        if (errorDialogMessage != null) {
            AlertDialog(
                onDismissRequest = { errorDialogMessage = null },
                containerColor = Color(0xFF1E293B),
                title = { Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Split Error", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }},
                text = { Text(errorDialogMessage!!, color = Color.LightGray) },
                confirmButton = {
                    TextButton(onClick = { errorDialogMessage = null }) {
                        Text("OK", color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
fun ModeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFF3B82F6) else Color(0xFF1E293B))
            .clickable { onClick() }
            .border(1.dp, if (selected) Color(0xFF3B82F6) else Color(0xFF334155), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text,
            color = if (selected) Color.White else Color.LightGray,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

// Memory-optimized, lazy-loaded page thumbnail viewer
@Composable
fun PagePreviewCard(
    context: Context,
    pdfUri: Uri,
    pageIndex: Int,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
    onZoomClick: () -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pdfUri, pageIndex) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openFileDescriptor(pdfUri, "r")?.use { pfd ->
                    android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                        if (pageIndex < renderer.pageCount) {
                            renderer.openPage(pageIndex).use { page ->
                                // Determine responsive size keeping aspect ratio
                                val w = 150
                                val ratio = page.height.toFloat() / page.width.toFloat()
                                val h = (w * ratio).toInt()
                                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                
                                val canvas = android.graphics.Canvas(bmp)
                                canvas.drawColor(android.graphics.Color.WHITE)
                                page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bitmap = bmp
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A).copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF334155),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onSelectionChanged(!isSelected) }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Page ${pageIndex + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                }

                // Checkbox Overlay (top-left)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onSelectionChanged(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF3B82F6),
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }

                // Magnifier overlay (bottom-right)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(26.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .clickable { onZoomClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom Page", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "Page ${pageIndex + 1}",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }
    }
}

// Pinch-to-zoom full view page dialog
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ZoomPreviewDialog(
    context: Context,
    pdfUri: Uri,
    pageIndex: Int,
    onDismiss: () -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Pinch state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(pdfUri, pageIndex) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openFileDescriptor(pdfUri, "r")?.use { pfd ->
                    android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                        if (pageIndex < renderer.pageCount) {
                            renderer.openPage(pageIndex).use { page ->
                                // Higher quality rendering for zoom
                                val w = 600
                                val ratio = page.height.toFloat() / page.width.toFloat()
                                val h = (w * ratio).toInt()
                                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                
                                val canvas = android.graphics.Canvas(bmp)
                                canvas.drawColor(android.graphics.Color.WHITE)
                                page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bitmap = bmp
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Page ${pageIndex + 1} Preview", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val state = rememberTransformableState { zoomChange, offsetChange, _ ->
                    scale = (scale * zoomChange).coerceIn(1f, 5f)
                    offset += offsetChange
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.72f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .transformable(state = state)
                        .combinedClickable(
                            onClick = {},
                            onDoubleClick = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                ),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        CircularProgressIndicator(color = Color(0xFF3B82F6))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Pinch to zoom • Double tap to toggle",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Background splitting processor using native coroutine
private suspend fun performSplitProcess(
    context: Context,
    pdfUri: Uri,
    pdfName: String,
    mode: SplitMode,
    rangeStr: String,
    intervalStr: String,
    selectedPages: List<Int>,
    viewModel: PDFViewModel,
    onProgress: (Float) -> Unit,
    onSuccess: (List<File>) -> Unit,
    onFailure: (String) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val baseName = pdfName.substringBeforeLast(".pdf")
        val exportedFiles = mutableListOf<File>()

        context.contentResolver.openFileDescriptor(pdfUri, "r")?.use { pfd ->
            val srcDoc = PDDocument.load(FileInputStream(pfd.fileDescriptor))
            val totalPages = srcDoc.numberOfPages

            if (srcDoc.isEncrypted) {
                withContext(Dispatchers.Main) {
                    onFailure("This document is password protected. Please unlock it before splitting.")
                }
                srcDoc.close()
                return@withContext
            }

            when (mode) {
                SplitMode.EVERY_PAGE -> {
                    for (i in 0 until totalPages) {
                        onProgress(i.toFloat() / totalPages.toFloat())
                        
                        val singlePageDoc = PDDocument()
                        singlePageDoc.addPage(srcDoc.getPage(i))
                        
                        val outFile = saveSplitFile(context, singlePageDoc, "${baseName}_Page_${i + 1}.pdf")
                        exportedFiles.add(outFile)
                        singlePageDoc.close()
                    }
                }
                
                SplitMode.RANGE -> {
                    // Parse ranges (e.g., "1-3, 4-7")
                    val ranges = parseRangeString(rangeStr, totalPages)
                    if (ranges.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            onFailure("Invalid range input. Please enter valid formats (e.g. 1-5, 6-10).")
                        }
                        srcDoc.close()
                        return@withContext
                    }

                    for (rIdx in ranges.indices) {
                        onProgress(rIdx.toFloat() / ranges.size.toFloat())
                        val range = ranges[rIdx]
                        
                        val rangeDoc = PDDocument()
                        for (p in range) {
                            if (p in 0 until totalPages) {
                                rangeDoc.addPage(srcDoc.getPage(p))
                            }
                        }
                        
                        if (rangeDoc.numberOfPages > 0) {
                            val rangeName = "${baseName}_Range_${range.first() + 1}_to_${range.last() + 1}.pdf"
                            val outFile = saveSplitFile(context, rangeDoc, rangeName)
                            exportedFiles.add(outFile)
                        }
                        rangeDoc.close()
                    }
                }

                SplitMode.EXTRACT -> {
                    onProgress(0.3f)
                    if (selectedPages.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            onFailure("No pages selected for extraction.")
                        }
                        srcDoc.close()
                        return@withContext
                    }

                    val extractDoc = PDDocument()
                    for (p in selectedPages) {
                        if (p in 0 until totalPages) {
                            extractDoc.addPage(srcDoc.getPage(p))
                        }
                    }
                    
                    if (extractDoc.numberOfPages > 0) {
                        val outFile = saveSplitFile(context, extractDoc, "${baseName}_Extracted.pdf")
                        exportedFiles.add(outFile)
                    }
                    extractDoc.close()
                    onProgress(0.8f)
                }

                SplitMode.CUSTOM_INTERVAL -> {
                    val step = intervalStr.toIntOrNull() ?: 2
                    if (step <= 0) {
                        withContext(Dispatchers.Main) {
                            onFailure("Invalid interval value. Please enter a number greater than 0.")
                        }
                        srcDoc.close()
                        return@withContext
                    }

                    var chunkIndex = 1
                    var start = 0
                    while (start < totalPages) {
                        onProgress(start.toFloat() / totalPages.toFloat())
                        val end = minOf(start + step, totalPages)
                        
                        val intervalDoc = PDDocument()
                        for (p in start until end) {
                            intervalDoc.addPage(srcDoc.getPage(p))
                        }
                        
                        if (intervalDoc.numberOfPages > 0) {
                            val outFile = saveSplitFile(context, intervalDoc, "${baseName}_Chunk_${chunkIndex}.pdf")
                            exportedFiles.add(outFile)
                        }
                        intervalDoc.close()
                        
                        start += step
                        chunkIndex++
                    }
                }
            }

            srcDoc.close()
        }

        onProgress(1.0f)

        withContext(Dispatchers.Main) {
            viewModel.loadHistory(context)
            onSuccess(exportedFiles)
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            onFailure("Failed to split PDF: ${e.localizedMessage ?: "Unknown storage error"}")
        }
    }
}

// Save split documents both to cache (history) and public Documents
private fun saveSplitFile(context: Context, doc: PDDocument, filename: String): File {
    // 1. Cache Save
    val cacheDir = File(context.cacheDir, "compiled_pdfs")
    if (!cacheDir.exists()) cacheDir.mkdirs()
    val cacheFile = File(cacheDir, filename)
    doc.save(cacheFile)

    // 2. Public Documents Save
    try {
        val publicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "AI PDF Studio"
        )
        if (!publicDir.exists()) publicDir.mkdirs()
        val publicFile = File(publicDir, filename)
        
        FileInputStream(cacheFile).use { input ->
            FileOutputStream(publicFile).use { output ->
                input.copyTo(output)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return cacheFile
}

// Range string parser ("1-5, 6-10" to indices [[0,1,2,3,4], [5,6,7,8,9]])
private fun parseRangeString(input: String, totalPages: Int): List<List<Int>> {
    val result = mutableListOf<List<Int>>()
    try {
        val tokens = input.split(",")
        for (token in tokens) {
            val rangeStr = token.trim()
            if (rangeStr.contains("-")) {
                val parts = rangeStr.split("-")
                if (parts.size == 2) {
                    val start = parts[0].trim().toIntOrNull() ?: continue
                    val end = parts[1].trim().toIntOrNull() ?: continue
                    
                    // Convert to 0-indexed
                    val startIdx = (start - 1).coerceIn(0, totalPages - 1)
                    val endIdx = (end - 1).coerceIn(0, totalPages - 1)
                    
                    val list = mutableListOf<Int>()
                    if (startIdx <= endIdx) {
                        for (i in startIdx..endIdx) list.add(i)
                    } else {
                        for (i in startIdx downTo endIdx) list.add(i)
                    }
                    if (list.isNotEmpty()) result.add(list)
                }
            } else {
                val singlePage = rangeStr.toIntOrNull()
                if (singlePage != null) {
                    val idx = (singlePage - 1).coerceIn(0, totalPages - 1)
                    result.add(listOf(idx))
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return result
}

private fun getFileNameFromUri(context: Context, uri: Uri): String {
    var name = ""
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index != -1) {
                name = it.getString(index)
            }
        }
    }
    if (name.isEmpty()) {
        name = uri.lastPathSegment ?: "document.pdf"
    }
    return name
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

// Extension to scale modifiers
private fun Modifier.scale(scale: Float): Modifier = this.graphicsLayer(scaleX = scale, scaleY = scale)
