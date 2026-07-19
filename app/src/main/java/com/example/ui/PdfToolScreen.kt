package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.PDFViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class ToolType {
    SPLIT, COMPRESS, WATERMARK, PASSWORD_PROTECT, REMOVE_PASSWORD, ROTATE, OCR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToolScreen(
    viewModel: PDFViewModel,
    toolType: ToolType,
    initialPdfUri: Uri? = null,
    initialPdfName: String = "",
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedPdfUri by remember { mutableStateOf(initialPdfUri) }
    var selectedPdfName by remember { mutableStateOf(initialPdfName.ifEmpty { initialPdfUri?.lastPathSegment ?: "document.pdf" }) }
    
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedPdfUri = uri
            selectedPdfName = "document.pdf"
        }
    }
    
    val coroutineScope = rememberCoroutineScope()

    // Tool specific states
    var splitStart by remember { mutableStateOf("1") }
    var splitEnd by remember { mutableStateOf("1") }
    
    var compressQuality by remember { mutableStateOf("Medium") }
    
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    
    var passwordText by remember { mutableStateOf("") }
    
    var rotationAngle by remember { mutableStateOf("90") }
    
    var showResult by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))
    ) {
        TopAppBar(
            title = { 
                Text(
                    when (toolType) {
                        ToolType.SPLIT -> "Split PDF"
                        ToolType.COMPRESS -> "Compress PDF"
                        ToolType.WATERMARK -> "Add Watermark"
                        ToolType.PASSWORD_PROTECT -> "Protect PDF"
                        ToolType.REMOVE_PASSWORD -> "Unlock PDF"
                        ToolType.ROTATE -> "Rotate Pages"
                        ToolType.OCR -> "Extract Text (OCR)"
                    }, color = Color.White
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
        
        Column(modifier = Modifier.padding(16.dp)) {
            if (selectedPdfUri == null) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(150.dp).clickable { pdfLauncher.launch("application/pdf") },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Select PDF File", color = Color.White)
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Red, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(selectedPdfName, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("PDF Document Selected", color = Color.LightGray, fontSize = 12.sp)
                        }
                        IconButton(onClick = { selectedPdfUri = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                when (toolType) {
                    ToolType.SPLIT -> {
                        Text("Page Range", color = Color.White, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = splitStart,
                                onValueChange = { splitStart = it },
                                label = { Text("Start Page", color = Color.LightGray) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            OutlinedTextField(
                                value = splitEnd,
                                onValueChange = { splitEnd = it },
                                label = { Text("End Page", color = Color.LightGray) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }
                    }
                    ToolType.COMPRESS -> {
                        Text("Compression Level", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("Low", "Medium", "High").forEach {
                                ChoiceChip(text = it, selected = compressQuality == it) { compressQuality = it }
                            }
                        }
                    }
                    ToolType.WATERMARK -> {
                        OutlinedTextField(
                            value = watermarkText,
                            onValueChange = { watermarkText = it },
                            label = { Text("Watermark Text", color = Color.LightGray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    ToolType.PASSWORD_PROTECT, ToolType.REMOVE_PASSWORD -> {
                        OutlinedTextField(
                            value = passwordText,
                            onValueChange = { passwordText = it },
                            label = { Text("Password", color = Color.LightGray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    ToolType.ROTATE -> {
                        Text("Rotation Angle", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("90", "180", "270").forEach {
                                ChoiceChip(text = it, selected = rotationAngle == it) { rotationAngle = it }
                            }
                        }
                    }
                    else -> {}
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val uri = selectedPdfUri!!
                                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                val filename = "${toolType.name}_${sdf.format(Date())}.pdf"
                                val dir = File(context.cacheDir, "compiled_pdfs")
                                if (!dir.exists()) dir.mkdirs()
                                val file = File(dir, filename)
                                
                                when (toolType) {
                                    ToolType.SPLIT -> com.example.model.PDFService.splitPdf(context, uri, splitStart.toIntOrNull() ?: 1, splitEnd.toIntOrNull() ?: 1, file)
                                    ToolType.COMPRESS -> com.example.model.PDFService.compressPdf(context, uri, if(compressQuality == "High") 100 else if(compressQuality == "Medium") 50 else 10, file)
                                    ToolType.WATERMARK -> com.example.model.PDFService.addWatermark(context, uri, watermarkText, file)
                                    ToolType.PASSWORD_PROTECT -> com.example.model.PDFService.protectPdf(context, uri, passwordText, file)
                                    ToolType.REMOVE_PASSWORD -> com.example.model.PDFService.removePassword(context, uri, passwordText, file)
                                    ToolType.ROTATE -> com.example.model.PDFService.rotatePages(context, uri, rotationAngle.toIntOrNull() ?: 90, file)
                                    ToolType.OCR -> {
                                        val text = com.example.model.PDFService.extractText(context, uri)
                                        viewModel.ocrResultText = text
                                        onNavigateBack()
                                        return@launch
                                    }
                                }
                                viewModel.loadHistory(context)
                                android.widget.Toast.makeText(context, "Success!", android.widget.Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Apply Tool", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .background(if (selected) androidx.compose.ui.graphics.Color(0xFF3B82F6) else androidx.compose.ui.graphics.Color(0xFF334155))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        androidx.compose.material3.Text(text, color = if (selected) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.LightGray, fontSize = 13.sp)
    }
}
