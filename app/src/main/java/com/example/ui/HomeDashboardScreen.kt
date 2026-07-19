package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.viewmodel.PDFViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeDashboardScreen(
    viewModel: PDFViewModel,
    onNavigate: (Int) -> Unit,
    onAction: (String) -> Unit = {},
    onOpenFile: (java.io.File) -> Unit = {}
) {
    val history by viewModel.exportHistory.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(modifier = Modifier.weight(1f), "Total PDFs", history.size.toString(), Icons.Default.PictureAsPdf, Color(0xFFEF4444))
            StatCard(modifier = Modifier.weight(1f), "Total Pages", history.sumOf { it.numPages }.toString(), Icons.Default.Scanner, Color(0xFF10B981))
            StatCard(modifier = Modifier.weight(1f), "Favorites", "0", Icons.Default.AutoAwesome, Color(0xFF3B82F6))
        }

        // Quick Actions
        Column {
            Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QuickActionItem("Scan", Icons.Default.DocumentScanner, Color(0xFFF59E0B)) { onAction("scan") }
                QuickActionItem("Image to PDF", Icons.Default.Image, Color(0xFF3B82F6)) { onAction("imageToPdf") }
                QuickActionItem("Merge", Icons.Default.MergeType, Color(0xFF8B5CF6)) { onAction("merge") }
                QuickActionItem("Split", Icons.Default.CallSplit, Color(0xFFEC4899)) { onAction("split") }
                QuickActionItem("Compress", Icons.Default.Compress, Color(0xFF14B8A6)) { onAction("compress") }
                QuickActionItem("OCR", Icons.Default.TextSnippet, Color(0xFFF97316)) { onAction("ocr") }
                QuickActionItem("Watermark", Icons.Default.WaterDrop, Color(0xFF06B6D4)) { onAction("watermark") }
                QuickActionItem("Protect", Icons.Default.Lock, Color(0xFFEF4444)) { onAction("password") }
                QuickActionItem("Unlock", Icons.Default.LockOpen, Color(0xFF22C55E)) { onAction("remove_password") }
                QuickActionItem("Rotate", Icons.Default.RotateRight, Color(0xFF8B5CF6)) { onAction("rotate") }
                QuickActionItem("AI Assistant", Icons.Default.AutoAwesome, Color(0xFFEAB308)) { onNavigate(2) }
            }
        }

        // Recent Files
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Files", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                Text("See All", fontSize = 12.sp, color = Color(0xFF3B82F6), modifier = Modifier.clickable { onNavigate(3) })
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            if (history.isEmpty()) {
                Text("No recent files", color = Color.Gray, modifier = Modifier.padding(16.dp))
            } else {
                history.take(3).forEach { pdf ->
                    RecentFileItem(
                        pdf = pdf,
                        onOpenFile = onOpenFile,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, title: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(title, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun QuickActionItem(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, fontSize = 11.sp, color = Color.LightGray)
    }
}
@Composable
fun RecentFileItem(
    pdf: com.example.model.GeneratedPdfReport,
    onOpenFile: (java.io.File) -> Unit,
    viewModel: PDFViewModel
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    // Lazy loaded high-fidelity PDF page thumbnail
    var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(pdf.file) {
        withContext(Dispatchers.IO) {
            try {
                val pfd = android.os.ParcelFileDescriptor.open(pdf.file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onOpenFile(pdf.file) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp, 65.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = "PDF Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(pdf.displayName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("${pdf.dateString} • ${pdf.fileSizeFormatted}", color = Color.Gray, fontSize = 12.sp)
            }
            
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
                            onOpenFile(pdf.file)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color.LightGray) },
                        onClick = {
                            showMenu = false
                            try {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdf.file)
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
                    HorizontalDivider(color = Color(0xFF334155))
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color.Red) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                        onClick = {
                            showMenu = false
                            pdf.file.delete()
                            viewModel.loadHistory(context)
                            Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
