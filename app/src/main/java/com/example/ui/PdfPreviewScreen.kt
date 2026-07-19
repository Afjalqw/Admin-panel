package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.viewmodel.PDFViewModel
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PdfPreviewScreen(
    viewModel: PDFViewModel,
    fileUri: Uri,
    initialDisplayName: String,
    onNavigateBack: () -> Unit,
    onOpenTool: (com.example.ui.ToolType, Uri, String) -> Unit = { _, _, _ -> },
    onOpenVisualEditor: (Uri, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var currentFile by remember { mutableStateOf<File?>(null) }
    var displayName by remember { mutableStateOf(initialDisplayName) }
    var totalPages by remember { mutableStateOf(0) }
    
    // PDF Editor options state
    var showEditorSheet by remember { mutableStateOf(false) }
    
    // UI states
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Int>>(emptyList()) } // Matched page indexes
    var currentSearchMatchIndex by remember { mutableStateOf(0) }
    
    // Zoom/Pinch States
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    // File manipulation
    var showRenameDialog by remember { mutableStateOf(false) }
    var newNameInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Lazy list controller for continuous scroll and visible page detection
    val listState = rememberLazyListState()
    val visiblePageIndex = remember { derivedStateOf { listState.firstVisibleItemIndex } }
    
    // Background list of extracted text page-by-page
    val pageTexts = remember { mutableStateListOf<String>() }

    // Resolve file from Uri
    LaunchedEffect(fileUri) {
        withContext(Dispatchers.IO) {
            try {
                // If it's a file uri, use it directly, otherwise copy to cache to read
                val resolvedFile = if (fileUri.scheme == "file") {
                    File(fileUri.path ?: "")
                } else {
                    val cacheFile = File(context.cacheDir, "temp_preview.pdf")
                    context.contentResolver.openInputStream(fileUri)?.use { input ->
                        FileOutputStream(cacheFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    cacheFile
                }
                
                if (resolvedFile.exists()) {
                    context.contentResolver.openFileDescriptor(Uri.fromFile(resolvedFile), "r")?.use { pfd ->
                        android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                            totalPages = renderer.pageCount
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        currentFile = resolvedFile
                    }
                    
                    // Pre-extract text in the background for search functionality
                    val doc = PDDocument.load(resolvedFile)
                    val stripper = PDFTextStripper()
                    for (i in 1..doc.numberOfPages) {
                        stripper.startPage = i
                        stripper.endPage = i
                        val text = try { stripper.getText(doc) } catch(e: Exception) { "" }
                        pageTexts.add(text)
                    }
                    doc.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorMessage = "Failed to load PDF preview: ${e.localizedMessage}"
                }
            }
        }
    }

    // Double tap toggle helper
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset += offsetChange
    }

    Scaffold(
        topBar = {
            if (isSearching) {
                // Search Header Mode
                Surface(
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            isSearching = false
                            searchQuery = ""
                            searchResults = emptyList()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Search", tint = Color.White)
                        }
                        
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { query ->
                                searchQuery = query
                                if (query.trim().length >= 2) {
                                    // Search text in extracted pages
                                    val matches = mutableListOf<Int>()
                                    for (i in pageTexts.indices) {
                                        if (pageTexts[i].contains(query, ignoreCase = true)) {
                                            matches.add(i)
                                        }
                                    }
                                    searchResults = matches
                                    currentSearchMatchIndex = 0
                                    
                                    // Scroll to first match
                                    if (matches.isNotEmpty()) {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(matches[0])
                                        }
                                    }
                                } else {
                                    searchResults = emptyList()
                                }
                            },
                            placeholder = { Text("Search text...", color = Color.Gray, fontSize = 14.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pdf_search_input")
                        )

                        if (searchResults.isNotEmpty()) {
                            Text(
                                text = "${currentSearchMatchIndex + 1}/${searchResults.size}",
                                color = Color(0xFF3B82F6),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            
                            IconButton(onClick = {
                                if (currentSearchMatchIndex > 0) {
                                    currentSearchMatchIndex--
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(searchResults[currentSearchMatchIndex])
                                    }
                                }
                            }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Prev Match", tint = Color.White)
                            }

                            IconButton(onClick = {
                                if (currentSearchMatchIndex < searchResults.size - 1) {
                                    currentSearchMatchIndex++
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(searchResults[currentSearchMatchIndex])
                                    }
                                }
                            }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next Match", tint = Color.White)
                            }
                        }
                    }
                }
            } else {
                // Standard Header Mode
                TopAppBar(
                    title = { 
                        Column {
                            Text(displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                            if (totalPages > 0) {
                                Text("Page ${visiblePageIndex.value + 1} of $totalPages", color = Color.LightGray, fontSize = 11.sp)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                        }
                        IconButton(onClick = {
                            val file = currentFile
                            if (file != null) {
                                printPdfDocument(context, file)
                            }
                        }) {
                            Icon(Icons.Default.Print, contentDescription = "Print", tint = Color.White)
                        }
                        IconButton(onClick = {
                            val file = currentFile
                            if (file != null) {
                                sharePdfDocument(context, file)
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                        IconButton(onClick = {
                            newNameInput = displayName.substringBeforeLast(".pdf")
                            showRenameDialog = true
                        }) {
                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Rename", tint = Color.White)
                        }
                        IconButton(onClick = { showEditorSheet = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "PDF Editor", tint = Color(0xFF3B82F6))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
                )
            }
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (currentFile == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF3B82F6))
                }
            } else {
                // Continuous scrollable, lazy loaded, gesture-zoomable container
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    ) {
                        items(totalPages) { pageIndex ->
                            PdfPageItem(
                                context = context,
                                file = currentFile!!,
                                pageIndex = pageIndex,
                                highlight = isSearching && searchResults.contains(pageIndex)
                            )
                        }
                    }
                }

                // Floating indicator badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Page ${visiblePageIndex.value + 1} of $totalPages",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Rename PDF File", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newNameInput,
                    onValueChange = { newNameInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("preview_rename_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = currentFile
                        if (file != null && newNameInput.trim().isNotEmpty()) {
                            val cleanNewName = newNameInput.trim()
                            val finalNewName = if (cleanNewName.endsWith(".pdf", ignoreCase = true)) cleanNewName else "$cleanNewName.pdf"
                            val parent = file.parentFile
                            val targetFile = File(parent, finalNewName)
                            
                            if (file.renameTo(targetFile)) {
                                displayName = finalNewName
                                currentFile = targetFile
                                showRenameDialog = false
                                viewModel.loadHistory(context)
                            } else {
                                errorMessage = "Failed to rename file. Check storage/permissions."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }

    // Global Error dialog
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("Error", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = { Text(errorMessage!!, color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = {
                    errorMessage = null
                    onNavigateBack()
                }) {
                    Text("Go Back", color = Color(0xFF3B82F6))
                }
            }
        )
    }

    // PDF Editor & Tools Bottom Sheet
    if (showEditorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEditorSheet = false },
            containerColor = Color(0xFF1E293B),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "PDF Editor & Tools",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "Select an editing tool to modify \"$displayName\"",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Advanced Visual Editor
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3B82F6).copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val file = currentFile
                                if (file != null) {
                                    showEditorSheet = false
                                    onOpenVisualEditor(Uri.fromFile(file), displayName)
                                }
                            }
                            .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Advanced Visual Editor", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Draw, add text blocks & highlight directly on pages", color = Color.LightGray, fontSize = 12.sp)
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val tools = listOf(
                        Triple("Rotate Pages", "Rotate page orientations by 90/180/270°", Icons.Default.RotateRight to com.example.ui.ToolType.ROTATE),
                        Triple("Add Watermark", "Apply custom text stamp to pages", Icons.Default.WaterDrop to com.example.ui.ToolType.WATERMARK),
                        Triple("Compress PDF", "Reduce file size with optimized compression", Icons.Default.Compress to com.example.ui.ToolType.COMPRESS),
                        Triple("Split PDF", "Extract specific pages or ranges to a new file", Icons.Default.CallSplit to com.example.ui.ToolType.SPLIT),
                        Triple("Extract Text (OCR)", "Recognize and extract text from images/scans", Icons.Default.TextSnippet to com.example.ui.ToolType.OCR),
                        Triple("Protect PDF", "Encrypt document with password security", Icons.Default.Lock to com.example.ui.ToolType.PASSWORD_PROTECT),
                        Triple("Unlock PDF", "Remove password protection from document", Icons.Default.LockOpen to com.example.ui.ToolType.REMOVE_PASSWORD)
                    )
                    
                    tools.forEach { (title, desc, iconAndType) ->
                        val (icon, type) = iconAndType
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val file = currentFile
                                    if (file != null) {
                                        showEditorSheet = false
                                        onOpenTool(type, Uri.fromFile(file), displayName)
                                    }
                                },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Text(desc, color = Color.Gray, fontSize = 12.sp)
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Memory-optimized rendering item for LazyColumn
@Composable
fun PdfPageItem(
    context: Context,
    file: File,
    pageIndex: Int,
    highlight: Boolean
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pageHeightByWidthRatio by remember { mutableStateOf(1.41f) } // default A4 ratio

    LaunchedEffect(file, pageIndex) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openFileDescriptor(Uri.fromFile(file), "r")?.use { pfd ->
                    android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                        if (pageIndex < renderer.pageCount) {
                            renderer.openPage(pageIndex).use { page ->
                                // Maintain high-fidelity display without crashing memory (e.g., set width to 800 and keep aspect ratio)
                                val w = 800
                                val ratio = page.height.toFloat() / page.width.toFloat()
                                pageHeightByWidthRatio = ratio
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f / pageHeightByWidthRatio)
            .border(
                width = if (highlight) 3.dp else 1.dp,
                color = if (highlight) Color(0xFFFFB300) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(4.dp)
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF3B82F6), modifier = Modifier.size(24.dp))
                }
            }
            
            // Subtle page indicator number on top-left of the page
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${pageIndex + 1}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Native share using FileProvider
private fun sharePdfDocument(context: Context, file: File) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF Document"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Native printer integration
private fun printPdfDocument(context: Context, file: File) {
    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "${file.name.substringBeforeLast(".pdf")} Document"
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(file.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: android.os.ParcelFileDescriptor,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback
            ) {
                var input: FileInputStream? = null
                var output: FileOutputStream? = null
                try {
                    input = FileInputStream(file)
                    output = FileOutputStream(destination.fileDescriptor)
                    val buf = ByteArray(1024)
                    var bytesRead: Int
                    while (input.read(buf).also { bytesRead = it } > 0) {
                        output.write(buf, 0, bytesRead)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    input?.close()
                    output?.close()
                }
            }
        }
        printManager.print(jobName, adapter, null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
