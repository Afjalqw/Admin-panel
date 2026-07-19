package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GeneratedPdfReport
import com.example.viewmodel.PDFViewModel
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class SelectedPdfItem(
    val id: String,
    val uri: Uri,
    val name: String,
    val pageCount: Int,
    val fileSizeFormatted: String,
    val thumbnail: Bitmap? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergePdfScreen(
    viewModel: PDFViewModel,
    onNavigateBack: () -> Unit,
    onPreviewPdf: (Uri, String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedPdfs by remember { mutableStateOf<List<SelectedPdfItem>>(emptyList()) }
    var outputFileName by remember { mutableStateOf("Merged_Document") }
    
    // Merge states
    var isMerging by remember { mutableStateOf(false) }
    var mergeProgress by remember { mutableStateOf(0f) } // 0.0f to 1.0f
    var mergeJob by remember { mutableStateOf<Job?>(null) }
    
    // Error handling dialogs
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf<File?>(null) }

    // Launcher for PDF picking
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                val newItems = uris.mapNotNull { uri ->
                    try {
                        val name = getFileNameFromUri(context, uri)
                        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                        var pageCount = 0
                        var sizeStr = "0 B"
                        var thumbnail: Bitmap? = null
                        
                        if (pfd != null) {
                            val renderer = android.graphics.pdf.PdfRenderer(pfd)
                            pageCount = renderer.pageCount
                            sizeStr = formatBytes(pfd.statSize)
                            
                            if (pageCount > 0) {
                                val page = renderer.openPage(0)
                                val bmp = Bitmap.createBitmap(120, 160, Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bmp)
                                canvas.drawColor(android.graphics.Color.WHITE)
                                page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                thumbnail = bmp
                                page.close()
                            }
                            renderer.close()
                            pfd.close()
                        }
                        
                        SelectedPdfItem(
                            id = UUID.randomUUID().toString(),
                            uri = uri,
                            name = name,
                            pageCount = pageCount,
                            fileSizeFormatted = sizeStr,
                            thumbnail = thumbnail
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                withContext(Dispatchers.Main) {
                    selectedPdfs = selectedPdfs + newItems
                }
            }
        }
    }

    // Trigger initial pick if list is empty when launching screen
    LaunchedEffect(Unit) {
        if (selectedPdfs.isEmpty()) {
            pdfPickerLauncher.launch("application/pdf")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Merge PDF Documents", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("merge_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Filename setting
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Output PDF Filename", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = outputFileName,
                            onValueChange = { outputFileName = it },
                            placeholder = { Text("Enter filename", color = Color.Gray) },
                            singleLine = true,
                            trailingIcon = { Text(".pdf", color = Color.Gray, modifier = Modifier.padding(end = 12.dp)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF475569)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("merge_filename_input")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // PDF List Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Files to Merge (${selectedPdfs.size})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    
                    Row {
                        IconButton(onClick = { pdfPickerLauncher.launch("application/pdf") }, modifier = Modifier.testTag("merge_add_more_btn")) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Add More", tint = Color(0xFF3B82F6))
                        }
                        IconButton(onClick = { selectedPdfs = emptyList() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All", tint = Color.Red.copy(alpha = 0.8f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedPdfs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No PDF Files Selected", color = Color.Gray, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { pdfPickerLauncher.launch("application/pdf") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                            ) {
                                Text("Select PDFs", color = Color.White)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(selectedPdfs, key = { _, item -> item.id }) { index, pdf ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Thumbnail Container
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (pdf.thumbnail != null) {
                                            Image(
                                                bitmap = pdf.thumbnail.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // File Info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            pdf.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "${pdf.pageCount} Pages • ${pdf.fileSizeFormatted}",
                                            color = Color.LightGray,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Actions Layout
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        // Preview
                                        IconButton(onClick = { onPreviewPdf(pdf.uri, pdf.name) }) {
                                            Icon(Icons.Default.Visibility, contentDescription = "Preview", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                        }
                                        
                                        // Move Up
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    val mutable = selectedPdfs.toMutableList()
                                                    val temp = mutable[index]
                                                    mutable[index] = mutable[index - 1]
                                                    mutable[index - 1] = temp
                                                    selectedPdfs = mutable
                                                }
                                            },
                                            enabled = index > 0
                                        ) {
                                            Icon(
                                                Icons.Default.ArrowUpward,
                                                contentDescription = "Move Up",
                                                tint = if (index > 0) Color.LightGray else Color.Gray.copy(alpha = 0.4f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Move Down
                                        IconButton(
                                            onClick = {
                                                if (index < selectedPdfs.size - 1) {
                                                    val mutable = selectedPdfs.toMutableList()
                                                    val temp = mutable[index]
                                                    mutable[index] = mutable[index + 1]
                                                    mutable[index + 1] = temp
                                                    selectedPdfs = mutable
                                                }
                                            },
                                            enabled = index < selectedPdfs.size - 1
                                        ) {
                                            Icon(
                                                Icons.Default.ArrowDownward,
                                                contentDescription = "Move Down",
                                                tint = if (index < selectedPdfs.size - 1) Color.LightGray else Color.Gray.copy(alpha = 0.4f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Remove
                                        IconButton(onClick = {
                                            selectedPdfs = selectedPdfs.filter { it.id != pdf.id }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Compilation Action Button
                    Button(
                        onClick = {
                            if (selectedPdfs.isEmpty()) {
                                errorDialogMessage = "Please select at least one PDF file to merge."
                                return@Button
                            }
                            if (outputFileName.trim().isEmpty()) {
                                errorDialogMessage = "Please enter a valid filename."
                                return@Button
                            }
                            isMerging = true
                            mergeProgress = 0f
                            
                            // Background merge operation
                            mergeJob = coroutineScope.launch {
                                performMergeProcess(
                                    context = context,
                                    selectedItems = selectedPdfs,
                                    outputName = outputFileName.trim(),
                                    viewModel = viewModel,
                                    onProgress = { progress -> mergeProgress = progress },
                                    onSuccess = { file ->
                                        isMerging = false
                                        showSuccessDialog = file
                                    },
                                    onFailure = { errorMsg ->
                                        isMerging = false
                                        errorDialogMessage = errorMsg
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("merge_pdf_compile_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.MergeType, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Merge PDF Files", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Processing overlay dialog with cancellation and percentage
            if (isMerging) {
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
                                progress = { mergeProgress },
                                color = Color(0xFF3B82F6),
                                strokeWidth = 5.dp,
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                "Merging PDF Documents...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "${(mergeProgress * 100).toInt()}% Completed",
                                color = Color(0xFF3B82F6),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Combining pages in background. Please wait...",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedButton(
                                onClick = {
                                    mergeJob?.cancel()
                                    isMerging = false
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

            // Material 3 Error Dialog
            if (errorDialogMessage != null) {
                AlertDialog(
                    onDismissRequest = { errorDialogMessage = null },
                    containerColor = Color(0xFF1E293B),
                    title = { Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Operation Error", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }},
                    text = { Text(errorDialogMessage!!, color = Color.LightGray) },
                    confirmButton = {
                        TextButton(onClick = { errorDialogMessage = null }) {
                            Text("OK", color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Success Prompt Dialog
            if (showSuccessDialog != null) {
                AlertDialog(
                    onDismissRequest = { showSuccessDialog = null },
                    containerColor = Color(0xFF1E293B),
                    title = { Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PDF Merged Successfully!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }},
                    text = { 
                        Column {
                            Text("Saved to:", color = Color.Gray, fontSize = 11.sp)
                            Text(showSuccessDialog!!.absolutePath, color = Color.White, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("The new PDF was added to your local history tab and is ready for use.", color = Color.LightGray, fontSize = 13.sp)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val file = showSuccessDialog!!
                                showSuccessDialog = null
                                onNavigateBack()
                                onPreviewPdf(Uri.fromFile(file), file.name)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                        ) {
                            Text("Open PDF Now", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showSuccessDialog = null
                            onNavigateBack()
                        }) {
                            Text("Done", color = Color.LightGray)
                        }
                    }
                )
            }
        }
    }
}

// Perform the merge operation in a background thread
private suspend fun performMergeProcess(
    context: Context,
    selectedItems: List<SelectedPdfItem>,
    outputName: String,
    viewModel: PDFViewModel,
    onProgress: (Float) -> Unit,
    onSuccess: (File) -> Unit,
    onFailure: (String) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val totalFiles = selectedItems.size
        val mergedDocument = PDDocument()
        
        for (i in 0 until totalFiles) {
            val item = selectedItems[i]
            onProgress(i.toFloat() / totalFiles.toFloat() * 0.9f)
            
            // Open stream to load PDF Document
            try {
                context.contentResolver.openFileDescriptor(item.uri, "r")?.use { pfd ->
                    val sourceDoc = PDDocument.load(FileInputStream(pfd.fileDescriptor))
                    
                    if (sourceDoc.isEncrypted) {
                        withContext(Dispatchers.Main) {
                            onFailure("Cannot merge '${item.name}': It is password protected. Please unlock it first.")
                        }
                        mergedDocument.close()
                        return@withContext
                    }
                    
                    for (page in sourceDoc.pages) {
                        mergedDocument.addPage(page)
                    }
                    sourceDoc.close()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onFailure("Error reading file '${item.name}'. It may be corrupted or invalid.")
                }
                mergedDocument.close()
                return@withContext
            }
        }

        // Output destination setup
        val finalFilename = if (outputName.endsWith(".pdf", ignoreCase = true)) outputName else "$outputName.pdf"
        
        // 1. Save to cache directory (scanned by viewModel.loadHistory)
        val cacheDir = File(context.cacheDir, "compiled_pdfs")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val cacheOutputFile = File(cacheDir, finalFilename)
        
        mergedDocument.save(cacheOutputFile)
        mergedDocument.close()
        
        // 2. Save to public Documents directory
        try {
            val publicDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "AI PDF Studio"
            )
            if (!publicDir.exists()) publicDir.mkdirs()
            val publicOutputFile = File(publicDir, finalFilename)
            
            // Copy from cache output to public output
            FileInputStream(cacheOutputFile).use { input ->
                FileOutputStream(publicOutputFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace() // Storage error can be skipped or shown, fallback is cache which is safe
        }

        onProgress(1.0f)
        
        // Load into history database/lists
        withContext(Dispatchers.Main) {
            viewModel.loadHistory(context)
            onSuccess(cacheOutputFile)
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            onFailure("Merge operation failed: ${e.localizedMessage ?: "Unknown storage error"}")
        }
    }
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
