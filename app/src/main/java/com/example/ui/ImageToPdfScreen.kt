package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.viewmodel.PDFViewModel

data class ImagePage(
    val id: String,
    val uri: Uri,
    var rotation: Float = 0f,
    val cropRatio: Float? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(viewModel: PDFViewModel, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var selectedImages by remember { mutableStateOf<List<ImagePage>>(emptyList()) }
    var hasLaunchedPicker by remember { mutableStateOf(false) }
    
    // Dialog state for visual crop selection
    var showCropDialogForIndex by remember { mutableStateOf<Int?>(null) }
    
    // Collect ViewModel configuration states
    val vmPages by viewModel.selectedPages.collectAsState()
    val pdfPrefix by viewModel.pdfPrefix.collectAsState()
    val defaultHasMargin by viewModel.defaultHasMargin.collectAsState()
    
    // Local copy of prefix input to keep typed inputs smooth
    var prefixInput by remember { mutableStateOf(pdfPrefix) }
    
    // Synchronize pages loaded from MainActivity/ViewModel on first enter
    LaunchedEffect(vmPages) {
        if (vmPages.isNotEmpty() && selectedImages.isEmpty()) {
            selectedImages = vmPages.map { ImagePage(id = it.id, uri = it.uri, rotation = 0f) }
        }
    }

    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(50)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newPages = uris.map { ImagePage(id = java.util.UUID.randomUUID().toString(), uri = it) }
            selectedImages = selectedImages + newPages
        }
    }

    // Auto launch if empty and not launched yet
    LaunchedEffect(Unit) {
        if (!hasLaunchedPicker && selectedImages.isEmpty() && vmPages.isEmpty()) {
            hasLaunchedPicker = true
            multiplePhotoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image to PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearSelections()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("app_bar_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A),
        bottomBar = {
            if (selectedImages.isNotEmpty()) {
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = {
                                viewModel.generateImageToPdfWithRotation(context, selectedImages)
                                viewModel.clearSelections()
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("create_pdf_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Create PDF (${selectedImages.size} Pages)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            if (selectedImages.isEmpty()) {
                // Polished Material 3 Empty State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(Color(0xFF3B82F6).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Text(
                                "No Images Selected",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                "Select high quality snapshots or documents to compile a clean PDF.",
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(
                                onClick = {
                                    multiplePhotoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("select_images_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select Images", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Layout Header Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Selected Pages (${selectedImages.size})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                multiplePhotoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.testTag("add_more_button")
                        ) {
                            Text("Add More", color = Color(0xFF3B82F6), fontWeight = FontWeight.SemiBold)
                        }
                        
                        TextButton(
                            onClick = {
                                selectedImages = emptyList()
                                viewModel.clearSelections()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(alpha = 0.8f))
                        ) {
                            Text("Clear All")
                        }
                    }
                }

                // Chunked list into 2 columns for a clean nested-scroll layout
                val chunkedImages = selectedImages.chunked(2)
                chunkedImages.forEachIndexed { rowIndex, rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowItems.forEachIndexed { itemIndex, item ->
                            val actualIndex = rowIndex * 2 + itemIndex
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                    .testTag("image_card_$actualIndex"),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    // Image preview container
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.75f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0F172A))
                                    ) {
                                        AsyncImage(
                                            model = item.uri,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .rotate(item.rotation),
                                            contentScale = ContentScale.Crop
                                        )
                                        
                                        // Page indicator badge (bottom left)
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(6.dp)
                                                .size(22.dp)
                                                .background(Color(0xFF3B82F6), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "${actualIndex + 1}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        
                                        // Visual Crop visual tag if applied
                                        if (item.cropRatio != null) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = when (item.cropRatio) {
                                                        1f -> "1:1"
                                                        1.777f -> "16:9"
                                                        1.333f -> "4:3"
                                                        else -> "Cropped"
                                                    },
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Utility action icons under the preview
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Rotate
                                        IconButton(
                                            onClick = {
                                                val mList = selectedImages.toMutableList()
                                                mList[actualIndex] = item.copy(rotation = (item.rotation + 90f) % 360f)
                                                selectedImages = mList
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RotateRight,
                                                contentDescription = "Rotate",
                                                tint = Color.LightGray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Crop
                                        IconButton(
                                            onClick = {
                                                showCropDialogForIndex = actualIndex
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Crop,
                                                contentDescription = "Crop",
                                                tint = Color.LightGray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Move Left/Up
                                        IconButton(
                                            onClick = {
                                                if (actualIndex > 0) {
                                                    val mList = selectedImages.toMutableList()
                                                    val prev = mList[actualIndex - 1]
                                                    mList[actualIndex - 1] = item
                                                    mList[actualIndex] = prev
                                                    selectedImages = mList
                                                }
                                            },
                                            enabled = actualIndex > 0,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Move Left",
                                                tint = if (actualIndex > 0) Color.LightGray else Color.Gray.copy(alpha = 0.4f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Move Right/Down
                                        IconButton(
                                            onClick = {
                                                if (actualIndex < selectedImages.size - 1) {
                                                    val mList = selectedImages.toMutableList()
                                                    val next = mList[actualIndex + 1]
                                                    mList[actualIndex + 1] = item
                                                    mList[actualIndex] = next
                                                    selectedImages = mList
                                                }
                                            },
                                            enabled = actualIndex < selectedImages.size - 1,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = "Move Right",
                                                tint = if (actualIndex < selectedImages.size - 1) Color.LightGray else Color.Gray.copy(alpha = 0.4f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Delete
                                        IconButton(
                                            onClick = {
                                                val mList = selectedImages.toMutableList()
                                                mList.removeAt(actualIndex)
                                                selectedImages = mList
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Empty placeholder spacer to keep grid alignment symmetrical if odd number of items
                        if (rowItems.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // PDF Settings Card Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PDF Compilation Settings", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // Filename Prefix Input
                        Text("Filename Prefix", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = prefixInput,
                            onValueChange = {
                                prefixInput = it
                                viewModel.updatePdfPrefix(context, it)
                            },
                            placeholder = { Text("e.g. Doc_Scan_", color = Color.Gray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF475569)
                            ),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Margin Toggle Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Add Page Margins", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Include standard 16dp spacing surrounding images", color = Color.LightGray, fontSize = 11.sp)
                            }
                            Switch(
                                checked = defaultHasMargin,
                                onCheckedChange = { viewModel.updateDefaultHasMargin(context, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF3B82F6)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
    
    // Interactive Crop Selector Dialog
    if (showCropDialogForIndex != null) {
        val targetIndex = showCropDialogForIndex!!
        val currentItem = selectedImages[targetIndex]
        
        Dialog(onDismissRequest = { showCropDialogForIndex = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Crop Grid Preview Aspect Ratio",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        "Configure the visual boundaries of this image page in the workspace preview grid.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Crop Options List
                    val options = listOf(
                        "Original Aspect" to null,
                        "Square (1:1)" to 1f,
                        "Cinema (16:9)" to 1.777f,
                        "Document (4:3)" to 1.333f
                    )
                    
                    options.forEach { (label, ratio) ->
                        val isSelected = currentItem.cropRatio == ratio
                        Button(
                            onClick = {
                                val mList = selectedImages.toMutableList()
                                mList[targetIndex] = currentItem.copy(cropRatio = ratio)
                                selectedImages = mList
                                showCropDialogForIndex = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF3B82F6) else Color(0xFF0F172A),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    TextButton(
                        onClick = { showCropDialogForIndex = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.LightGray)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
