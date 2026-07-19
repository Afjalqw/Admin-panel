package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.model.GeneratedPdfReport
import com.example.viewmodel.PDFViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryManagerScreen(
    viewModel: PDFViewModel,
    list: List<GeneratedPdfReport>,
    onOpenFile: (File) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("Recent") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(300) // Fast visual load
        isLoading = false
    }

    // Filter list based on search and favorite filters
    val filteredList = remember(list, searchQuery, filterMode) {
        var base = list
        if (searchQuery.trim().isNotEmpty()) {
            base = base.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
        }
        if (filterMode == "Favorites") {
            // Simply use favorites logic if available, or sort
            base = base.sortedByDescending { it.file.lastModified() }
        }
        base
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search generated PDFs...", color = Color.Gray, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFF475569),
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("history_search_input")
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tab Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterTab(text = "Recent", selected = filterMode == "Recent") {
                filterMode = "Recent"
            }
            FilterTab(text = "All Files", selected = filterMode == "All Files") {
                filterMode = "All Files"
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(5) {
                    HistorySkeletonItem()
                }
            }
        } else if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No files found", color = Color.Gray, fontSize = 16.sp)
                }
            }
        } else {
            AnimatedVisibility(
                visible = !isLoading,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })
            ) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredList, key = { it.file.absolutePath }) { report ->
                        var isDismissed by remember { mutableStateOf(false) }
                        AnimatedVisibility(
                            visible = !isDismissed,
                            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
                        ) {
                            HistoryFileCard(
                                report = report,
                                viewModel = viewModel,
                                onOpenFile = onOpenFile,
                                onDismiss = { isDismissed = true }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF3B82F6) else Color(0xFF1E293B))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text, color = if (selected) Color.White else Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HistorySkeletonItem() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "Skeleton"
    )
    Card(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = alpha)),
        shape = RoundedCornerShape(12.dp)
    ) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFileCard(
    report: GeneratedPdfReport,
    viewModel: PDFViewModel,
    onOpenFile: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var isFavorite by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInputText by remember { mutableStateOf(report.displayName.substringBeforeLast(".pdf")) }
    
    // Lazy loaded high-fidelity PDF page thumbnail
    var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(report.file) {
        withContext(Dispatchers.IO) {
            try {
                val pfd = android.os.ParcelFileDescriptor.open(report.file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                if (pfd != null) {
                    val renderer = android.graphics.pdf.PdfRenderer(pfd)
                    if (renderer.pageCount > 0) {
                        val page = renderer.openPage(0)
                        val bmp = Bitmap.createBitmap(100, 130, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bmp)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        thumbnailBitmap = bmp
                        page.close()
                    }
                    renderer.close()
                    pfd.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                report.file.delete()
                viewModel.loadHistory(context)
                onDismiss()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Red)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenFile(report.file) }
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // High fidelity Thumbnail container
                Box(
                    modifier = Modifier
                        .size(50.dp, 65.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbnailBitmap != null) {
                        Image(
                            bitmap = thumbnailBitmap!!.asImageBitmap(),
                            contentDescription = "Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Red, modifier = Modifier.size(28.dp))
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        report.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${report.dateString} • ${report.numPages} Pages",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
                
                IconButton(onClick = { isFavorite = !isFavorite }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFFB300) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // More options menu layout
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray)
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.LightGray) },
                            onClick = {
                                showMenu = false
                                onOpenFile(report.file)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.LightGray) },
                            onClick = {
                                showMenu = false
                                renameInputText = report.displayName.substringBeforeLast(".pdf")
                                showRenameDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.LightGray) },
                            onClick = {
                                showMenu = false
                                try {
                                    val original = report.file
                                    val base = original.name.substringBeforeLast(".pdf")
                                    val copy = File(original.parentFile, "${base}_Copy.pdf")
                                    original.copyTo(copy, overwrite = true)
                                    viewModel.loadHistory(context)
                                    Toast.makeText(context, "Duplicated successfully!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color.LightGray) },
                            onClick = {
                                showMenu = false
                                try {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", report.file)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share PDF"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Print", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Print, contentDescription = null, tint = Color.LightGray) },
                            onClick = {
                                showMenu = false
                                try {
                                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                                    val adapter = object : PrintDocumentAdapter() {
                                        override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes, cancellationSignal: android.os.CancellationSignal?, callback: LayoutResultCallback, extras: android.os.Bundle?) {
                                            callback.onLayoutFinished(PrintDocumentInfo.Builder(report.file.name).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(), true)
                                        }
                                        override fun onWrite(pages: Array<out PageRange>?, destination: android.os.ParcelFileDescriptor, cancellationSignal: android.os.CancellationSignal?, callback: WriteResultCallback) {
                                            try {
                                                FileInputStream(report.file).use { input ->
                                                    FileOutputStream(destination.fileDescriptor).use { output ->
                                                        input.copyTo(output)
                                                    }
                                                }
                                                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                                            } catch (e: Exception) {
                                                callback.onWriteFailed(e.message)
                                            }
                                        }
                                    }
                                    printManager.print("${report.file.name.substringBeforeLast(".pdf")} Document", adapter, null)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Print failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to Downloads", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null, tint = Color.LightGray) },
                            onClick = {
                                showMenu = false
                                try {
                                    val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AI PDF Studio")
                                    if (!publicDir.exists()) publicDir.mkdirs()
                                    val target = File(publicDir, report.file.name)
                                    report.file.copyTo(target, overwrite = true)
                                    Toast.makeText(context, "Moved to Downloads/AI PDF Studio/", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Move failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        HorizontalDivider(color = Color(0xFF334155))
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color.Red) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                            onClick = {
                                showMenu = false
                                report.file.delete()
                                viewModel.loadHistory(context)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }

    // Rename dialog inside card scope
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Rename PDF", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("card_rename_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val original = report.file
                        val cleanName = renameInputText.trim()
                        if (cleanName.isNotEmpty()) {
                            val finalName = if (cleanName.endsWith(".pdf", ignoreCase = true)) cleanName else "$cleanName.pdf"
                            val renamedFile = File(original.parentFile, finalName)
                            if (original.renameTo(renamedFile)) {
                                viewModel.loadHistory(context)
                                showRenameDialog = false
                            } else {
                                Toast.makeText(context, "Rename failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }
}
