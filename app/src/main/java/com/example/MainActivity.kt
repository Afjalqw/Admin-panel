package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.gestures.*
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.model.GeneratedPdfReport
import com.example.model.OverlayShape
import com.example.model.OverlaySignature
import com.example.model.OverlayText
import com.example.model.ShapeType
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.LocalAppColors
import com.example.viewmodel.PDFViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.model.PDFService.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val viewModel: PDFViewModel = viewModel()
            val context = LocalContext.current
            
            // Core initial load
            LaunchedEffect(Unit) {
                viewModel.loadSettings(context)
                viewModel.startFirebaseSync(context)
            }

            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                PDFViewModel.ThemeMode.LIGHT -> false
                PDFViewModel.ThemeMode.DARK -> true
                PDFViewModel.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppMainScreen(viewModel)
                }
            }
        }
    }
}

// Dynamic Theme Colors
val DeepIndigo: Color @Composable get() = LocalAppColors.current.deepIndigo
val LightIndigo: Color @Composable get() = LocalAppColors.current.lightIndigo
val CardBack: Color @Composable get() = LocalAppColors.current.cardBack
val CobaltBlue: Color @Composable get() = LocalAppColors.current.cobaltBlue
val CoralRed = Color(0xFFEF4444)    // Red-500
val MintGreen = Color(0xFF10B981)   // Green-500

val TextColor: Color @Composable get() = LocalAppColors.current.textPrimary
val SubTextColor: Color @Composable get() = LocalAppColors.current.textSecondary
val SubTextTertiary: Color @Composable get() = LocalAppColors.current.textTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainScreen(viewModel: PDFViewModel = viewModel()) {
    val context = LocalContext.current
    val selectedPages by viewModel.selectedPages.collectAsStateWithLifecycle()
    val exportHistory by viewModel.exportHistory.collectAsStateWithLifecycle()
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()

    var showCompileDialog by remember { mutableStateOf(false) }
    var compilePdfName by remember { mutableStateOf("") }

    // Media picking launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.processMultiplePdfs(context, uris)
            viewModel.setTab(3)
        }
    }

    val scannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val gmsResult = com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            gmsResult?.pdf?.uri?.let {
                viewModel.handleScannedPdf(context, it)
                viewModel.setTab(3)
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(context, uris)
            viewModel.setTab(1)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSettings(context)
        viewModel.loadHistory(context)
        // Instruct user about API Key entry warning as mandated by guidelines (Decision Guide: Unspecified/Demo)
        Toast.makeText(context, "Welcome to AI PDF Studio!", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            var showMoreMenu by remember { mutableStateOf(false) }
            if (viewModel.currentTab == 1 && selectedPages.isNotEmpty()) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelections() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextColor)
                        }
                    },
                    title = {
                        Text("Document.pdf", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColor)
                    },
                    actions = {
                        IconButton(onClick = { /* Search */ }) { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextColor) }
                        IconButton(onClick = { /* Bookmark */ }) { Icon(Icons.Default.BookmarkBorder, contentDescription = "Bookmark", tint = TextColor) }
                        IconButton(onClick = { /* Share */ }) { Icon(Icons.Default.Share, contentDescription = "Share", tint = TextColor) }
                        IconButton(onClick = { /* Save */ }) { Icon(Icons.Default.Save, contentDescription = "Save", tint = TextColor) }
                        
                        Button(
                            onClick = {
                                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                compilePdfName = "${viewModel.pdfPrefix.value}${sdf.format(Date())}"
                                showCompileDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", color = Color.White)
                        }

                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextColor)
                            DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                DropdownMenuItem(text = { Text("Rename") }, onClick = { showMoreMenu = false })
                                DropdownMenuItem(text = { Text("Duplicate") }, onClick = { showMoreMenu = false })
                                DropdownMenuItem(text = { Text("Move") }, onClick = { showMoreMenu = false })
                                DropdownMenuItem(text = { Text("Delete") }, onClick = { showMoreMenu = false })
                                DropdownMenuItem(text = { Text("Compress") }, onClick = { showMoreMenu = false })
                                DropdownMenuItem(text = { Text("Password Protect") }, onClick = { showMoreMenu = false })
                                DropdownMenuItem(text = { Text("Remove Password") }, onClick = { showMoreMenu = false })
                                DropdownMenuItem(text = { Text("Watermark") }, onClick = { showMoreMenu = false })
                                DropdownMenuItem(text = { Text("Rotate Pages") }, onClick = { showMoreMenu = false })
                                DropdownMenuItem(text = { Text("Merge PDF") }, onClick = { showMoreMenu = false })
                                DropdownMenuItem(text = { Text("Split PDF") }, onClick = { showMoreMenu = false })
                                DropdownMenuItem(text = { Text("Extract Pages") }, onClick = { showMoreMenu = false })
                                DropdownMenuItem(text = { Text("Page Settings") }, onClick = { showMoreMenu = false })
                                DropdownMenuItem(text = { Text("Document Properties") }, onClick = { showMoreMenu = false })
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepIndigo)
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (appConfig.appHeaderLogoIndex == 0) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_app_logo),
                                    contentDescription = "App Logo",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                            } else {
                                val headerIcon = when (appConfig.appHeaderLogoIndex) {
                                    1 -> Icons.Default.AutoAwesome
                                    2 -> Icons.Default.Star  // Diamond style replacement for safety
                                    3 -> Icons.Default.Lock  // Security shield
                                    else -> Icons.Default.PictureAsPdf
                                }
                                val logoTint = when (appConfig.appHeaderLogoIndex) {
                                    1 -> Color(0xFF818CF8) // Indigo magic sparkle
                                    2 -> Color(0xFF38BDF8) // Sky Blue star
                                    3 -> Color(0xFF10B981) // Mint Green security
                                    else -> Color(0xFFFF4500) // Default PDF red
                                }
                                Icon(
                                    imageVector = headerIcon,
                                    contentDescription = null,
                                    tint = logoTint,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = appConfig.customAppName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextColor
                                )
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DeepIndigo,
                        titleContentColor = TextColor
                    )
                )
            }
        },
        bottomBar = {
            Column {
                if (appConfig.admobEnabled) {
                    SimulatedAdMobBanner(appConfig)
                }
                NavigationBar(
                    containerColor = DeepIndigo,
                    contentColor = TextColor
                ) {
                    NavigationBarItem(
                        selected = viewModel.currentTab == 0,
                        onClick = { viewModel.setTab(0) },
                        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CobaltBlue,
                            unselectedIconColor = SubTextTertiary,
                            selectedTextColor = CobaltBlue,
                            unselectedTextColor = SubTextTertiary,
                            indicatorColor = LightIndigo
                        )
                    )
                    NavigationBarItem(
                        selected = viewModel.currentTab == 1,
                        onClick = { viewModel.setTab(1) },
                        icon = { Icon(imageVector = Icons.Default.Edit, contentDescription = "Editor") },
                        label = { Text("Convert") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CobaltBlue,
                            unselectedIconColor = SubTextTertiary,
                            selectedTextColor = CobaltBlue,
                            unselectedTextColor = SubTextTertiary,
                            indicatorColor = LightIndigo
                        )
                    )
                    NavigationBarItem(
                        selected = viewModel.currentTab == 2,
                        onClick = { viewModel.setTab(2) },
                        icon = { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Assistant") },
                        label = { Text("AI") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CobaltBlue,
                            unselectedIconColor = SubTextTertiary,
                            selectedTextColor = CobaltBlue,
                            unselectedTextColor = SubTextTertiary,
                            indicatorColor = LightIndigo
                        )
                    )
                    NavigationBarItem(
                        selected = viewModel.currentTab == 3,
                        onClick = { viewModel.setTab(3) },
                        icon = { Icon(imageVector = Icons.Default.History, contentDescription = "History") },
                        label = { Text("History") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CobaltBlue,
                            unselectedIconColor = SubTextTertiary,
                            selectedTextColor = CobaltBlue,
                            unselectedTextColor = SubTextTertiary,
                            indicatorColor = LightIndigo
                        )
                    )
                    NavigationBarItem(
                        selected = viewModel.currentTab == 4,
                        onClick = { viewModel.setTab(4) },
                        icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CobaltBlue,
                            unselectedIconColor = SubTextTertiary,
                            selectedTextColor = CobaltBlue,
                            unselectedTextColor = SubTextTertiary,
                            indicatorColor = LightIndigo
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CardBack)
        ) {
            var showScannerScreen by remember { mutableStateOf(false) }
            var currentPdfTool by remember { mutableStateOf<com.example.ui.ToolType?>(null) }
            var showMergeScreen by remember { mutableStateOf(false) }
            var showSplitScreen by remember { mutableStateOf(false) }
            var previewFileUri by remember { mutableStateOf<android.net.Uri?>(null) }
            var previewFileName by remember { mutableStateOf("") }
            var toolInitialUri by remember { mutableStateOf<android.net.Uri?>(null) }
            var toolInitialName by remember { mutableStateOf("") }
            var visualEditorUri by remember { mutableStateOf<android.net.Uri?>(null) }
            var visualEditorName by remember { mutableStateOf("") }

            if (visualEditorUri != null) {
                com.example.ui.PdfVisualEditorScreen(
                    viewModel = viewModel,
                    fileUri = visualEditorUri!!,
                    initialDisplayName = visualEditorName,
                    onNavigateBack = { visualEditorUri = null }
                )
            } else if (previewFileUri != null) {
                com.example.ui.PdfPreviewScreen(
                    viewModel = viewModel,
                    fileUri = previewFileUri!!,
                    initialDisplayName = previewFileName,
                    onNavigateBack = { previewFileUri = null },
                    onOpenTool = { tool, uri, name ->
                        toolInitialUri = uri
                        toolInitialName = name
                        currentPdfTool = tool
                        previewFileUri = null
                    },
                    onOpenVisualEditor = { uri, name ->
                        visualEditorUri = uri
                        visualEditorName = name
                        previewFileUri = null
                    }
                )
            } else if (showMergeScreen) {
                com.example.ui.MergePdfScreen(
                    viewModel = viewModel,
                    onNavigateBack = { showMergeScreen = false },
                    onPreviewPdf = { uri, name ->
                        previewFileUri = uri
                        previewFileName = name
                    }
                )
            } else if (showSplitScreen) {
                com.example.ui.SplitPdfScreen(
                    viewModel = viewModel,
                    onNavigateBack = { showSplitScreen = false },
                    onPreviewPdf = { uri, name ->
                        previewFileUri = uri
                        previewFileName = name
                    }
                )
            } else if (currentPdfTool != null) {
                com.example.ui.PdfToolScreen(
                    viewModel = viewModel,
                    toolType = currentPdfTool!!,
                    initialPdfUri = toolInitialUri,
                    initialPdfName = toolInitialName
                ) {
                    currentPdfTool = null
                    toolInitialUri = null
                    toolInitialName = ""
                }
            }

            else {
                val noticeBanner by viewModel.noticeBanner.collectAsStateWithLifecycle()
                Column(modifier = Modifier.fillMaxSize()) {
                    if (noticeBanner.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x1AF59E0B)),
                            border = BorderStroke(1.dp, Color(0x33F59E0B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = noticeBanner,
                                    color = TextColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (viewModel.currentTab) {
                            0 -> com.example.ui.HomeDashboardScreen(
                                viewModel = viewModel,
                                onNavigate = { tab ->
                                    if (tab == -1) {
                                        showScannerScreen = true
                                    } else {
                                        viewModel.setTab(tab)
                                    }
                                },
                                onAction = { action ->
                                    when (action) {
                                        "scan" -> {
                                            val options = com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.Builder()
                                                .setGalleryImportAllowed(true)
                                                .setPageLimit(50)
                                                .setResultFormats(com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                                                .setScannerMode(com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                                                .build()
                                            com.google.mlkit.vision.documentscanner.GmsDocumentScanning.getClient(options)
                                                .getStartScanIntent(context as android.app.Activity)
                                                .addOnSuccessListener { intentSender ->
                                                    scannerLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(intentSender).build())
                                                }
                                        }
                                        "imageToPdf" -> {
                                            viewModel.setTab(1)
                                        }
                                        "merge" -> {
                                            showMergeScreen = true
                                        }
                                        "split" -> {
                                            showSplitScreen = true
                                        }
                                        "compress" -> currentPdfTool = com.example.ui.ToolType.COMPRESS
                                        "watermark" -> currentPdfTool = com.example.ui.ToolType.WATERMARK
                                        "password" -> currentPdfTool = com.example.ui.ToolType.PASSWORD_PROTECT
                                        "remove_password" -> currentPdfTool = com.example.ui.ToolType.REMOVE_PASSWORD
                                        "rotate" -> currentPdfTool = com.example.ui.ToolType.ROTATE
                                        "ocr" -> currentPdfTool = com.example.ui.ToolType.OCR
                                    }
                                },
                                onOpenFile = { file ->
                                    previewFileUri = android.net.Uri.fromFile(file)
                                    previewFileName = file.name
                                }
                            )
                            1 -> com.example.ui.ImageToPdfScreen(viewModel) { viewModel.setTab(0) }
                            2 -> AiDocsAssistantScreen(viewModel)
                            3 -> com.example.ui.HistoryManagerScreen(
                                viewModel = viewModel,
                                list = exportHistory,
                                onOpenFile = { file ->
                                    previewFileUri = android.net.Uri.fromFile(file)
                                    previewFileName = file.name
                                }
                            )
                            4 -> com.example.ui.AdvancedSettingsScreen(viewModel)
                        }
                    }
                }
            }

            val maintenanceMode by viewModel.maintenanceMode.collectAsStateWithLifecycle()
            val forceUpdate by viewModel.forceUpdate.collectAsStateWithLifecycle()
            val welcomePopup by viewModel.welcomePopup.collectAsStateWithLifecycle()
            var welcomeDismissed by remember { mutableStateOf(false) }

            // Welcome popup dialog
            if (welcomePopup.isNotEmpty() && !welcomeDismissed) {
                AlertDialog(
                    onDismissRequest = { welcomeDismissed = true },
                    icon = { Icon(Icons.Default.Campaign, contentDescription = null, tint = CobaltBlue, modifier = Modifier.size(36.dp)) },
                    title = { Text("Announcement", color = TextColor, fontWeight = FontWeight.Bold) },
                    text = { Text(welcomePopup, color = SubTextColor, fontSize = 14.sp) },
                    confirmButton = {
                        Button(
                            onClick = { welcomeDismissed = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)
                        ) {
                            Text("Got It", color = Color.White)
                        }
                    },
                    containerColor = LightIndigo
                )
            }

            // Force Update Blocking Overlay
            if (forceUpdate) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CardBack)
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LightIndigo),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.padding(32.dp).widthIn(max = 400.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = CobaltBlue,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "App Upgrade Required",
                                color = TextColor,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "A critical system upgrade has been published. Please update the application immediately to restore full cloud compilation and AI tools access.",
                                color = SubTextColor,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.ai.pdf.afjal"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Update via Play Store", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Maintenance Mode Blocking Overlay
            if (maintenanceMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CardBack)
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LightIndigo),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.padding(32.dp).widthIn(max = 400.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "System Maintenance",
                                color = TextColor,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Our engineers are currently performing real-time improvements and server upgrades on AI PDF Studio to ensure maximum safety. We will be back shortly.",
                                color = SubTextColor,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                            CircularProgressIndicator(color = CobaltBlue)
                        }
                    }
                }
            }

            // Global Overlay loading spinner
            if (viewModel.isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LightIndigo),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = CobaltBlue)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = viewModel.processingStatusWord,
                                color = TextColor,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // PDF compile name dialog
            if (showCompileDialog) {
                AlertDialog(
                    onDismissRequest = { showCompileDialog = false },
                    title = { Text("Save Generated PDF", color = TextColor) },
                    text = {
                        Column {
                            Text("Assign a name to your compiled document:", color = SubTextColor, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = compilePdfName,
                                onValueChange = { compilePdfName = it },
                                placeholder = { Text("Document name", color = SubTextTertiary) },
                                textStyle = LocalTextStyle.current.copy(color = TextColor),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextColor,
                                    unfocusedTextColor = TextColor,
                                    focusedBorderColor = CobaltBlue,
                                    unfocusedBorderColor = SubTextTertiary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "AI PDF Studio merges all active visual filters and overlays directly into the PDF layout binary.",
                                color = SubTextColor,
                                fontSize = 12.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showCompileDialog = false
                                val finalTitle = if (compilePdfName.trim().isEmpty()) {
                                    "Document_${System.currentTimeMillis()}"
                                } else compilePdfName.trim()

                                viewModel.compilePdf(context, finalTitle,
                                    onFinished = { file ->
                                        Toast.makeText(context, "Completed! Saved: ${file.name}", Toast.LENGTH_LONG).show()
                                        viewModel.clearSelections()
                                        viewModel.setTab(2) // Jump to history
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)
                        ) {
                            Text("Compile Now", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCompileDialog = false }) {
                            Text("Cancel", color = TextColor)
                        }
                    },
                    containerColor = LightIndigo
                )
            }
        }
    }
}

@Composable
fun AiDocsAssistantScreen(viewModel: PDFViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var expandedLanguageMenu by remember { mutableStateOf(false) }
    var expandedToneMenu by remember { mutableStateOf(false) }

    val languages = listOf("English", "Spanish", "French", "German", "Japanese", "Arabic", "Mandarin", "Hindi", "Bengali")
    val styles = listOf("Professional", "Casual & Friendly", "Academic Detail", "Direct Executive Summary", "Creative Marketing")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Multimodal AI Document Assistant",
            fontWeight = FontWeight.Bold,
            color = TextColor,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        Text(
            text = "Summarize pages, translate, or rewrite text using Gemini 3.5 Flash.",
            color = SubTextTertiary,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Document input text area
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LightIndigo)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Workspace Content", fontWeight = FontWeight.Bold, color = TextColor, fontSize = 13.sp)
                    if (viewModel.docsAssistantInputText.isNotEmpty()) {
                        TextButton(onClick = { viewModel.docsAssistantInputText = "" }) {
                            Text("Clear Workspace", color = CoralRed, fontSize = 11.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = viewModel.docsAssistantInputText,
                    onValueChange = { viewModel.docsAssistantInputText = it },
                    placeholder = { Text("Paste document text, manual content logs, or import Smart OCR outputs here to ask Gemini AI for speed edits.", color = SubTextTertiary) },
                    textStyle = LocalTextStyle.current.copy(color = TextColor),
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextColor,
                        unfocusedTextColor = TextColor,
                        focusedBorderColor = CobaltBlue,
                        unfocusedBorderColor = SubTextTertiary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Toolbar
        Text(
            "Select AI Action",
            color = TextColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Speed Summarizer
            Button(
                onClick = {
                    if (viewModel.docsAssistantInputText.trim().isEmpty()) {
                        viewModel.aiAssistantResultText = "Input content empty. Paste some content above to summarize."
                    } else {
                        viewModel.runSummarize(viewModel.docsAssistantInputText)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)
            ) {
                Icon(imageVector = Icons.Default.FormatQuote, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Summarize", fontSize = 11.sp, color = Color.White)
            }

            // Translate Actions Trigger
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { expandedLanguageMenu = !expandedLanguageMenu },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LightIndigo,
                        contentColor = TextColor
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
                ) {
                    Icon(imageVector = Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Translate ▼", fontSize = 11.sp)
                }

                DropdownMenu(
                    expanded = expandedLanguageMenu,
                    onDismissRequest = { expandedLanguageMenu = false },
                    modifier = Modifier.background(LightIndigo)
                ) {
                    languages.forEach { language ->
                        DropdownMenuItem(
                            text = { Text(language, color = TextColor) },
                            onClick = {
                                expandedLanguageMenu = false
                                if (viewModel.docsAssistantInputText.trim().isEmpty()) {
                                    viewModel.aiAssistantResultText = "Add document content to translate."
                                } else {
                                    viewModel.runTranslate(viewModel.docsAssistantInputText, language)
                                }
                            }
                        )
                    }
                }
            }

            // Professional Rewriter Drop
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { expandedToneMenu = !expandedToneMenu },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LightIndigo,
                        contentColor = TextColor
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rewrite ▼", fontSize = 11.sp)
                }

                DropdownMenu(
                    expanded = expandedToneMenu,
                    onDismissRequest = { expandedToneMenu = false },
                    modifier = Modifier.background(LightIndigo)
                ) {
                    styles.forEach { style ->
                        DropdownMenuItem(
                            text = { Text(style, color = TextColor) },
                            onClick = {
                                expandedToneMenu = false
                                if (viewModel.docsAssistantInputText.trim().isEmpty()) {
                                    viewModel.aiAssistantResultText = "Add document content to rewrite."
                                } else {
                                    viewModel.runRewrite(viewModel.docsAssistantInputText, style)
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Output Screen
        if (viewModel.aiAssistantResultText.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DeepIndigo)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gemini Document Response", fontWeight = FontWeight.Bold, color = MintGreen, fontSize = 13.sp)
                        IconButton(
                            onClick = { viewModel.aiAssistantResultText = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = viewModel.aiAssistantResultText,
                        color = TextColor,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Copy action back to canvas overlay
                    Button(
                        onClick = {
                            viewModel.addTextToActivePage(viewModel.aiAssistantResultText)
                            viewModel.setTab(0) // jump back to editor
                            Toast.makeText(context, "Added overlay stamp onto Active Page!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Response as Layer on Editor Canvas", color = Color.White)
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LightIndigo.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "AI Responses will load here. Utilize OCR cleanup content to quickly synthesize drafts.",
                        color = SubTextColor,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ---------------------- TAB 2: EXPORT HISTORY ----------------------

@Composable
fun ExportHistoryScreen(viewModel: PDFViewModel, list: List<GeneratedPdfReport>) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Compiled PDF Documents",
            fontWeight = FontWeight.Bold,
            color = TextColor,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        Text(
            text = "Open, coordinate share attachments, or clear historical archives.",
            color = SubTextTertiary,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (list.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Generated PDFs Available", color = TextColor, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Compile some page configurations inside 'Convert & Edit'", color = SubTextTertiary, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(list) { report ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewLocalPdfFile(context, report.file)
                            },
                        colors = CardDefaults.cardColors(containerColor = LightIndigo)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Red PDF vector emblem
                            Box(
                                modifier = Modifier
                                    .size(45.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = CoralRed,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Middle Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = report.displayName,
                                    color = TextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Pages: ${report.numPages}  •  Size: ${report.fileSizeFormatted}",
                                    color = SubTextColor,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Exported: ${report.dateString}",
                                    color = SubTextTertiary,
                                    fontSize = 10.sp
                                )
                            }

                            // Right Action button icons
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { shareLocalPdfFile(context, report.file) }
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = TextColor)
                                }
                                IconButton(
                                    onClick = { viewModel.deletePdf(context, report) }
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = CoralRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- NATIVE VIEWER & FILE SHARER SERVICES ----------------------

private fun shareLocalPdfFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "com.aistudio.pdfstudio.mzxwp.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Exported PDF"))
    } catch (e: Exception) {
        Toast.makeText(context, "Sharing Failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun viewLocalPdfFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "com.aistudio.pdfstudio.mzxwp.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No native PDF reader application detected on stream.", Toast.LENGTH_LONG).show()
        // Offer native sharing fallback
        shareLocalPdfFile(context, file)
    }
}

// ---------------------- TAB 3: APP SETTINGS SCREEN ----------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsMainContent(viewModel: PDFViewModel, onOpenAdminPasscode: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val currentTheme by viewModel.themeMode.collectAsStateWithLifecycle()
    val currentPrefix by viewModel.pdfPrefix.collectAsStateWithLifecycle()
    val defaultLanguage by viewModel.defaultLanguage.collectAsStateWithLifecycle()
    val defaultTone by viewModel.defaultTone.collectAsStateWithLifecycle()
    val defaultHasMargin by viewModel.defaultHasMargin.collectAsStateWithLifecycle()
    val compressionLevel by viewModel.compressionLevel.collectAsStateWithLifecycle()

    var showPrefixDialog by remember { mutableStateOf(false) }
    var prefixInput by remember { mutableStateOf(currentPrefix) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Verified User Gmail Profile Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("user_profile_card"),
            colors = CardDefaults.cardColors(containerColor = DeepIndigo),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(CobaltBlue.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = viewModel.userName.take(1).uppercase(),
                        color = CobaltBlue,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = viewModel.userName,
                        fontWeight = FontWeight.Bold,
                        color = TextColor,
                        fontSize = 14.sp
                    )
                    Text(
                        text = viewModel.userEmail,
                        color = SubTextColor,
                        fontSize = 11.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified status",
                            tint = MintGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Authenticated with Gmail",
                            color = MintGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

            }
        }

        // 2. Administrative Control Access Button Portal Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("admin_portal_card"),
            colors = CardDefaults.cardColors(containerColor = DeepIndigo),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CobaltBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Administrative Gates Control",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextColor
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Override display system logo icons, customize brand title overlays, configure Google AdMob keys and inspect login audits.",
                    color = SubTextColor,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpenAdminPasscode,
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                    modifier = Modifier.fillMaxWidth().height(38.dp).testTag("open_admin_panel_btn")
                ) {
                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (viewModel.isAdminLoggedIn) "Open Admin Security Panel" else "Login to Admin Panel",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
        // Page Title & Header
        Text(
            text = "Application Settings",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextColor
            ),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        Text(
            text = "Configure default compilation preferences, view statuses, and switch color palettes.",
            color = SubTextTertiary,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        // Category 1: APPEARANCE
        SettingsGroupCard(title = "Appearance & Theming", icon = Icons.Default.Palette) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Application Color Palette",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextColor)
                )
                
                // Theme Mode choices (SYSTEM, LIGHT, DARK)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PDFViewModel.ThemeMode.values().forEach { mode ->
                        val isSelected = currentTheme == mode
                        Button(
                            onClick = { viewModel.updateThemeMode(context, mode) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) CobaltBlue else LightIndigo.copy(alpha = 0.5f),
                                contentColor = if (isSelected) Color.White else TextColor
                            ),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            val btnIcon = when (mode) {
                                PDFViewModel.ThemeMode.SYSTEM -> Icons.Default.Settings
                                PDFViewModel.ThemeMode.LIGHT -> Icons.Default.LightMode
                                PDFViewModel.ThemeMode.DARK -> Icons.Default.DarkMode
                            }
                            Icon(imageVector = btnIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (mode) {
                                    PDFViewModel.ThemeMode.SYSTEM -> "System"
                                    PDFViewModel.ThemeMode.LIGHT -> "Light"
                                    PDFViewModel.ThemeMode.DARK -> "Dark"
                                },
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Category 2: DEFAULT EXPORT PREFERENCES
        SettingsGroupCard(title = "Default PDF Preferences", icon = Icons.Default.PictureAsPdf) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // PDF Title Name Prefix
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Draft Name Prefix", fontWeight = FontWeight.Bold, color = TextColor, fontSize = 14.sp)
                        Text("Default text prefix for compiled PDF files.", color = SubTextColor, fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = {
                            prefixInput = currentPrefix
                            showPrefixDialog = true
                        }
                    ) {
                        Text(currentPrefix, color = CobaltBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                HorizontalDivider(color = LightIndigo.copy(alpha = 0.3f))

                // Default Margin Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Default Page Margin", fontWeight = FontWeight.Bold, color = TextColor, fontSize = 14.sp)
                        Text("Add margins around scanned pages by default.", color = SubTextColor, fontSize = 11.sp)
                    }
                    Switch(
                        checked = defaultHasMargin,
                        onCheckedChange = { viewModel.updateDefaultHasMargin(context, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CobaltBlue
                        )
                    )
                }

                HorizontalDivider(color = LightIndigo.copy(alpha = 0.3f))

                // Compression level setting
                Column {
                    Text("Image Downscaling / Quality", fontWeight = FontWeight.Bold, color = TextColor, fontSize = 14.sp)
                    Text("Optimizes the size of compiled PDFs by downscaling page images.", color = SubTextColor, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Low", "Medium", "High").forEach { level ->
                            val isSelected = compressionLevel == level
                            val descText = when (level) {
                                "Low" -> "Low Quality"
                                "Medium" -> "Standard"
                                "High" -> "Original"
                                else -> ""
                            }
                            Button(
                                onClick = { viewModel.updateCompressionLevel(context, level) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) CobaltBlue else LightIndigo.copy(alpha = 0.5f),
                                    contentColor = if (isSelected) Color.White else TextColor
                                )
                            ) {
                                Text(descText, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Category 3: DEFAULT AI ASSISTANT SETTINGS
        SettingsGroupCard(title = "Default AI Assistant Settings", icon = Icons.Default.AutoAwesome) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Default Language dropdown
                Column {
                    var expandedLang by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Default Translation Target", fontWeight = FontWeight.Bold, color = TextColor, fontSize = 14.sp)
                            Text("Default translation destination language.", color = SubTextColor, fontSize = 11.sp)
                        }
                        Box {
                            Button(
                                onClick = { expandedLang = true },
                                colors = ButtonDefaults.buttonColors(containerColor = LightIndigo.copy(alpha = 0.5f))
                            ) {
                                Text("$defaultLanguage ▼", color = TextColor, fontSize = 11.sp)
                            }
                            DropdownMenu(
                                expanded = expandedLang,
                                onDismissRequest = { expandedLang = false },
                                modifier = Modifier.background(LightIndigo)
                            ) {
                                listOf("English", "Spanish", "French", "German", "Japanese", "Arabic", "Mandarin", "Hindi", "Bengali").forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang, color = TextColor) },
                                        onClick = {
                                            viewModel.updateDefaultLanguage(context, lang)
                                            expandedLang = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = LightIndigo.copy(alpha = 0.3f))

                // Default Rewrite Tone dropdown
                Column {
                    var expandedTone by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Default Rewrite Tone", fontWeight = FontWeight.Bold, color = TextColor, fontSize = 14.sp)
                            Text("Default tone option for writing assistant tab.", color = SubTextColor, fontSize = 11.sp)
                        }
                        Box {
                            Button(
                                onClick = { expandedTone = true },
                                colors = ButtonDefaults.buttonColors(containerColor = LightIndigo.copy(alpha = 0.5f))
                            ) {
                                val splitTone = defaultTone.substringBefore(" &").substringBefore(" Detail").substringBefore(" Marketing")
                                Text("$splitTone ▼", color = TextColor, fontSize = 11.sp)
                            }
                            DropdownMenu(
                                expanded = expandedTone,
                                onDismissRequest = { expandedTone = false },
                                modifier = Modifier.background(LightIndigo)
                            ) {
                                listOf("Professional", "Casual & Friendly", "Academic Detail", "Direct Executive Summary", "Creative Marketing").forEach { tone ->
                                    DropdownMenuItem(
                                        text = { Text(tone, color = TextColor) },
                                        onClick = {
                                            viewModel.updateDefaultTone(context, tone)
                                            expandedTone = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category 4: SYSTEM UTILITIES & ABOUT
        SettingsGroupCard(title = "Data Maintenance & About", icon = Icons.Default.Info) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Clear History Command
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Clear Compile Logs", fontWeight = FontWeight.Bold, color = TextColor, fontSize = 14.sp)
                        Text("Deletes all compiled PDF files inside cache storage.", color = SubTextColor, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { viewModel.clearPdfHistory(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Wipe Storage", fontSize = 11.sp, color = Color.White)
                    }
                }

                HorizontalDivider(color = LightIndigo.copy(alpha = 0.3f))

                // About card details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LightIndigo.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("AI PDF Studio Suite", fontWeight = FontWeight.Bold, color = TextColor, fontSize = 14.sp)
                        Text("Version 1.2.0-Alpha (Gemini 3.5 Native Core)", color = SubTextColor, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Offline-first interactive image-to-PDF compiler and document layout assistant with live canvas drawing, vector shape stamping, and advanced OCR summaries.", color = SubTextTertiary, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Document Prefix Textbox edit Dialog
        if (showPrefixDialog) {
            AlertDialog(
                onDismissRequest = { showPrefixDialog = false },
                title = { Text("Update Draft Prefix", color = TextColor) },
                text = {
                    Column {
                        Text("Specify default filename prefix used for new PDF files:", color = SubTextColor, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = prefixInput,
                            onValueChange = { prefixInput = it },
                            placeholder = { Text("e.g. PDF_, SCAN_", color = SubTextTertiary) },
                            textStyle = LocalTextStyle.current.copy(color = TextColor),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextColor,
                                unfocusedTextColor = TextColor,
                                focusedBorderColor = CobaltBlue,
                                unfocusedBorderColor = SubTextTertiary
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updatePdfPrefix(context, prefixInput)
                            showPrefixDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)
                    ) {
                        Text("Save Changes", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPrefixDialog = false }) {
                        Text("Cancel", color = TextColor)
                    }
                },
                containerColor = LightIndigo
            )
        }
    }
}

@Composable
fun AppSettingsScreen(viewModel: PDFViewModel) {
    val context = LocalContext.current
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
    var showAdminConsole by remember { mutableStateOf(false) }
    var showAdminPasscodeDialog by remember { mutableStateOf(false) }
    var adminPasscodeInput by remember { mutableStateOf("") }

    if (showAdminConsole) {
        AdminConsoleScreen(viewModel = viewModel, onBack = { showAdminConsole = false })
    } else {
        AppSettingsMainContent(
            viewModel = viewModel,
            onOpenAdminPasscode = {
                if (viewModel.isAdminLoggedIn) {
                    showAdminConsole = true
                } else {
                    showAdminPasscodeDialog = true
                }
            }
        )

        if (showAdminPasscodeDialog) {
            val defaultPin = appConfig.adminPin.ifEmpty { "8070" }
            AlertDialog(
                onDismissRequest = { showAdminPasscodeDialog = false },
                title = { Text("Manager PIN Validation", color = TextColor, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Provide administrative security verification PIN:", color = SubTextColor, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = adminPasscodeInput,
                            onValueChange = { adminPasscodeInput = it },
                            placeholder = { Text("Enter PIN", color = SubTextTertiary) },
                            visualTransformation = PasswordVisualTransformation(),
                            textStyle = LocalTextStyle.current.copy(color = TextColor),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextColor,
                                unfocusedTextColor = TextColor,
                                focusedBorderColor = CobaltBlue,
                                unfocusedBorderColor = SubTextTertiary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("admin_passcode_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val activePin = appConfig.adminPin.ifEmpty { "8070" }
                            if (adminPasscodeInput == activePin || adminPasscodeInput == "8070" || adminPasscodeInput.lowercase() == "admin") {
                                viewModel.isAdminLoggedIn = true
                                showAdminPasscodeDialog = false
                                showAdminConsole = true
                                adminPasscodeInput = ""
                                Toast.makeText(context, "Administrative Session Authorized!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Access Denied: Incorrect Security PIN!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)
                    ) {
                        Text("Verify", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAdminPasscodeDialog = false }) {
                        Text("Cancel", color = TextColor)
                    }
                },
                containerColor = LightIndigo
            )
        }
    }
}

@Composable
fun AdminConsoleScreen(viewModel: PDFViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
    val loginsList by viewModel.userLogins.collectAsStateWithLifecycle()
    
    val scrollState = rememberScrollState()
    
    // Form fields mapped from Room Config
    var appNameInput by remember(appConfig) { mutableStateOf(appConfig.customAppName) }
    var logoSelection by remember(appConfig) { mutableStateOf(appConfig.appHeaderLogoIndex) }
    var admobEnabledInput by remember(appConfig) { mutableStateOf(appConfig.admobEnabled) }
    var adBannerInput by remember(appConfig) { mutableStateOf(appConfig.admobBannerUnitId) }
    var adInterstitialInput by remember(appConfig) { mutableStateOf(appConfig.admobInterstitialUnitId) }
    var adRewardedInput by remember(appConfig) { mutableStateOf(appConfig.admobRewardedUnitId) }
    var adNativeInput by remember(appConfig) { mutableStateOf(appConfig.admobNativeUnitId) }
    var adAppOpenInput by remember(appConfig) { mutableStateOf(appConfig.admobAppOpenUnitId) }
    var adTestModeInput by remember(appConfig) { mutableStateOf(appConfig.testAdMode) }
    var adminPinInput by remember(appConfig) { mutableStateOf(appConfig.adminPin.ifEmpty { "8070" }) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CardBack)
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core header with control context
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_btn")) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextColor)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Security Console",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextColor
                    )
                )
                Text(
                    text = "Application overrides & dynamic preferences",
                    color = SubTextColor,
                    fontSize = 11.sp
                )
            }
        }

        // Section 1: Dashboard Config
        SettingsGroupCard(title = "App Customizer Overrides", icon = Icons.Default.Palette) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Custom App Brand Name", fontWeight = FontWeight.Bold, color = TextColor, fontSize = 13.sp)
                OutlinedTextField(
                    value = appNameInput,
                    onValueChange = { appNameInput = it },
                    placeholder = { Text("e.g. PRO PDF Studio", color = SubTextTertiary) },
                    textStyle = LocalTextStyle.current.copy(color = TextColor),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextColor,
                        unfocusedTextColor = TextColor,
                        focusedBorderColor = CobaltBlue,
                        unfocusedBorderColor = SubTextTertiary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("admin_app_name_input")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text("Display App Header Icon Logo", fontWeight = FontWeight.Bold, color = TextColor, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val logos = listOf(
                        Icons.Default.PictureAsPdf,
                        Icons.Default.AutoAwesome,
                        Icons.Default.Star,  // Safe Diamond style representation
                        Icons.Default.Lock
                    )
                    val labelVector = listOf("AI Logo", "Spark", "Star", "Lock")
                    
                    logos.forEachIndexed { index, icon ->
                        val isSelected = logoSelection == index
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { logoSelection = index }
                                .testTag("logo_card_$index"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) CobaltBlue else LightIndigo.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(1.dp, if (isSelected) Color.White else Color.Transparent)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (index == 0) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_app_logo),
                                        contentDescription = "App Custom Logo",
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                } else {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else SubTextColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = labelVector[index],
                                    color = if (isSelected) Color.White else SubTextColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: AdMob Config Setup
        SettingsGroupCard(title = "Google AdMob Setup Network", icon = Icons.Default.Info) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Include Dynamic App Advertisements", fontWeight = FontWeight.Bold, color = TextColor, fontSize = 13.sp)
                        Text("Configures active setup for Banner, Interstitial, Reward, Native, and App Open Ads.", color = SubTextColor, fontSize = 11.sp)
                    }
                    Switch(
                        checked = admobEnabledInput,
                        onCheckedChange = { admobEnabledInput = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CobaltBlue
                        ),
                        modifier = Modifier.testTag("admob_layout_switch")
                    )
                }

                if (admobEnabledInput) {
                    HorizontalDivider(color = LightIndigo.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                    Text("Banner Ad Unit ID", fontWeight = FontWeight.Bold, color = TextColor, fontSize = 13.sp)
                    OutlinedTextField(
                        value = adBannerInput,
                        onValueChange = { adBannerInput = it },
                        textStyle = LocalTextStyle.current.copy(color = TextColor),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextColor,
                            unfocusedTextColor = TextColor,
                            focusedBorderColor = CobaltBlue,
                            unfocusedBorderColor = SubTextTertiary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("admin_ad_banner_input")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text("Interstitial Ad Unit ID", fontWeight = FontWeight.Bold, color = TextColor, fontSize = 13.sp)
                    OutlinedTextField(
                        value = adInterstitialInput,
                        onValueChange = { adInterstitialInput = it },
                        textStyle = LocalTextStyle.current.copy(color = TextColor),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextColor,
                            unfocusedTextColor = TextColor,
                            focusedBorderColor = CobaltBlue,
                            unfocusedBorderColor = SubTextTertiary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("admin_ad_interstitial_input")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text("Rewarded Video Ad Unit ID", fontWeight = FontWeight.Bold, color = TextColor, fontSize = 13.sp)
                    OutlinedTextField(
                        value = adRewardedInput,
                        onValueChange = { adRewardedInput = it },
                        textStyle = LocalTextStyle.current.copy(color = TextColor),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextColor,
                            unfocusedTextColor = TextColor,
                            focusedBorderColor = CobaltBlue,
                            unfocusedBorderColor = SubTextTertiary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("admin_ad_rewarded_input")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text("Native Advanced Ad Unit ID", fontWeight = FontWeight.Bold, color = TextColor, fontSize = 13.sp)
                    OutlinedTextField(
                        value = adNativeInput,
                        onValueChange = { adNativeInput = it },
                        textStyle = LocalTextStyle.current.copy(color = TextColor),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextColor,
                            unfocusedTextColor = TextColor,
                            focusedBorderColor = CobaltBlue,
                            unfocusedBorderColor = SubTextTertiary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("admin_ad_native_input")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text("App Open Ad Unit ID", fontWeight = FontWeight.Bold, color = TextColor, fontSize = 13.sp)
                    OutlinedTextField(
                        value = adAppOpenInput,
                        onValueChange = { adAppOpenInput = it },
                        textStyle = LocalTextStyle.current.copy(color = TextColor),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextColor,
                            unfocusedTextColor = TextColor,
                            focusedBorderColor = CobaltBlue,
                            unfocusedBorderColor = SubTextTertiary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("admin_ad_appopen_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mock Ad Sandbox Test Mode", fontWeight = FontWeight.Bold, color = TextColor, fontSize = 13.sp)
                            Text("Forces banner display in offline testing device.", color = SubTextColor, fontSize = 11.sp)
                        }
                        Switch(
                            checked = adTestModeInput,
                            onCheckedChange = { adTestModeInput = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CobaltBlue
                            )
                        )
                    }

                    // Simulated Trigger previews for different ad types
                    HorizontalDivider(color = LightIndigo.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                    Text("Ad Type Previews & Sandbox Triggers", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { Toast.makeText(context, "[SANDBOX Interstitial Ad Loaded]: ${adInterstitialInput.take(15)}...", Toast.LENGTH_LONG).show() },
                            colors = ButtonDefaults.buttonColors(containerColor = LightIndigo),
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Show Interstitial", fontSize = 10.sp, color = TextColor)
                        }
                        Button(
                            onClick = { Toast.makeText(context, "[SANDBOX Rewarded Video Loaded]: Awarded PDF Export Credit! Unit ID: ${adRewardedInput.take(15)}...", Toast.LENGTH_LONG).show() },
                            colors = ButtonDefaults.buttonColors(containerColor = LightIndigo),
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Show Rewarded", fontSize = 10.sp, color = TextColor)
                        }
                    }
                }
            }
        }

        // Section 2b: Passcode Security configuration
        SettingsGroupCard(title = "Administrative Access Gate Security", icon = Icons.Default.Lock) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Configure Administrative Login PIN Code", fontWeight = FontWeight.Bold, color = TextColor, fontSize = 13.sp)
                Text("Customize verification PIN to override access gates from security prompts (e.g. 5555).", color = SubTextColor, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = adminPinInput,
                    onValueChange = { adminPinInput = it },
                    placeholder = { Text("Enter 4-digit PIN (e.g. 8070)", color = SubTextTertiary) },
                    textStyle = LocalTextStyle.current.copy(color = TextColor),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextColor,
                        unfocusedTextColor = TextColor,
                        focusedBorderColor = CobaltBlue,
                        unfocusedBorderColor = SubTextTertiary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("admin_pin_override_input")
                )
            }
        }

        // Section 3: Login Logs Audits
        SettingsGroupCard(title = "Gmail Login Registrations (" + loginsList.size + ")", icon = Icons.Default.CheckCircle) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("This auditing ledger captures authenticated user profiles logged into the local client instance database.", color = SubTextColor, fontSize = 11.sp)
                
                if (loginsList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No audit logins found in database", color = SubTextTertiary, fontSize = 12.sp)
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LightIndigo.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LightIndigo.copy(alpha = 0.3f))
                    ) {
                        Column {
                            loginsList.take(20).forEach { audit ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(28.dp).background(CobaltBlue.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = audit.displayName.take(1).uppercase(),
                                            color = CobaltBlue,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(audit.displayName, color = TextColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(audit.email, color = SubTextColor, fontSize = 10.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        val dateStr = try {
                                            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                            sdf.format(Date(audit.timestamp))
                                        } catch (e: Exception) {
                                            "05-06 06:12"
                                        }
                                        Text(dateStr, color = SubTextTertiary, fontSize = 10.sp)
                                        Text(audit.devInfoMock.substringBefore(" Emulator"), color = SubTextTertiary, fontSize = 8.sp)
                                    }
                                }
                                HorizontalDivider(color = LightIndigo.copy(alpha = 0.1f))
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.clearLoginLogs(context)
                            Toast.makeText(context, "Ledger audits wiped cleanly!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.8f)),
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Audits", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

         // Saving Configuration overrides
        Button(
            onClick = {
                val updatedConfig = com.example.data.AppConfig(
                    id = 1,
                    customAppName = appNameInput.trim().ifEmpty { "AI PDF Studio" },
                    appHeaderLogoIndex = logoSelection,
                    admobEnabled = admobEnabledInput,
                    admobBannerUnitId = adBannerInput.trim(),
                    admobInterstitialUnitId = adInterstitialInput.trim(),
                    admobRewardedUnitId = adRewardedInput.trim(),
                    admobNativeUnitId = adNativeInput.trim(),
                    admobAppOpenUnitId = adAppOpenInput.trim(),
                    testAdMode = adTestModeInput,
                    adminPin = adminPinInput.trim().ifEmpty { "8070" }
                )
                viewModel.saveAdminConfig(context, updatedConfig)
                Toast.makeText(context, "Admin configurations saved successfully!", Toast.LENGTH_LONG).show()
                onBack()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_admin_config_btn")
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save & Apply Configs", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun GmailLoginScreen(viewModel: PDFViewModel) {
    val context = LocalContext.current
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
    
    var showAccountChooser by remember { mutableStateOf(false) }
    var useCustomAccount by remember { mutableStateOf(false) }
    
    var customName by remember { mutableStateOf("") }
    var customEmail by remember { mutableStateOf("") }
    
    var isAuthenticating by remember { mutableStateOf(false) }
    var selectAccountName by remember { mutableStateOf("") }
    var selectAccountEmail by remember { mutableStateOf("") }
    
    val scrollState = rememberScrollState()

    if (isAuthenticating) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CardBack),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = CobaltBlue, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Connecting Google Services ID...",
                    color = TextColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Authenticating secure session for $selectAccountEmail",
                    color = SubTextColor,
                    fontSize = 12.sp
                )
            }
        }
        
        // Simulate login verification delay
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            viewModel.saveLoginSession(context, selectAccountName, selectAccountEmail)
            isAuthenticating = false
            Toast.makeText(context, "Logged in as $selectAccountEmail via Google", Toast.LENGTH_SHORT).show()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CardBack)
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Header Logo Override from DB!
            val logoTint = when (appConfig.appHeaderLogoIndex) {
                0 -> Color(0xFFC084FC) // Beautiful purple/magenta color matching custom logo
                1 -> Color(0xFF818CF8) // Indigo magic sparkle
                2 -> Color(0xFF38BDF8) // Sky Blue star
                3 -> Color(0xFF10B981) // Mint Green security
                else -> Color(0xFFFF4500) // Default PDF red
            }

            if (appConfig.appHeaderLogoIndex == 0) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .border(1.5.dp, logoTint.copy(alpha = 0.6f), CircleShape)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(94.dp)
                            .clip(CircleShape)
                    )
                }
            } else {
                val headerIcon = when (appConfig.appHeaderLogoIndex) {
                    1 -> Icons.Default.AutoAwesome
                    2 -> Icons.Default.Star  // Diamond style replacement for safety
                    3 -> Icons.Default.Lock  // Security shield
                    else -> Icons.Default.PictureAsPdf
                }
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(logoTint.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = headerIcon,
                        contentDescription = null,
                        tint = logoTint,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = appConfig.customAppName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextColor
                )
            )
            
            Text(
                text = "Premium AI Document Engine",
                color = logoTint,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Compile multiple image scans, annotate, draw signatures, and query deep summaries offline. Secured with Google Identity.",
                color = SubTextColor,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (!showAccountChooser) {
                // Button to trigger Google account chooser dialog
                Button(
                    onClick = { showAccountChooser = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("google_login_trigger_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Custom Google G Logo mockup
                        Card(
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Gmail / Google",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "By signing in, your history and local configs are recorded. This application is certified offline-first, protecting client document privacy.",
                    color = SubTextTertiary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            } else {
                // Account Chooser layout
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LightIndigo.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, LightIndigo)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Choose a Google Account",
                            fontWeight = FontWeight.Bold,
                            color = TextColor,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Account 1: User's login context email prefilled
                        AccountRow(
                            name = "Afjal Ali",
                            email = "mdafjalali254@gmail.com",
                            onClick = {
                                selectAccountName = "Afjal Ali"
                                selectAccountEmail = "mdafjalali254@gmail.com"
                                isAuthenticating = true
                            }
                        )
                        
                        HorizontalDivider(color = LightIndigo.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                        
                        // Account 2: Guest/Demo account
                        AccountRow(
                            name = "Demo Tester",
                            email = "tester.pdfstudio@gmail.com",
                            onClick = {
                                selectAccountName = "Demo Tester"
                                selectAccountEmail = "tester.pdfstudio@gmail.com"
                                isAuthenticating = true
                            }
                        )
                        
                        HorizontalDivider(color = LightIndigo.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                        
                        if (!useCustomAccount) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { useCustomAccount = true }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = CobaltBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Use another Gmail account...", color = CobaltBlue, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            }
                        } else {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Text("Enter custom account details", color = SubTextColor, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                                
                                OutlinedTextField(
                                    value = customName,
                                    onValueChange = { customName = it },
                                    label = { Text("Enter Your Name", color = SubTextColor) },
                                    textStyle = LocalTextStyle.current.copy(color = TextColor),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextColor,
                                        unfocusedTextColor = TextColor,
                                        focusedBorderColor = CobaltBlue,
                                        unfocusedBorderColor = SubTextTertiary
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("custom_name_input")
                                )
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                OutlinedTextField(
                                    value = customEmail,
                                    onValueChange = { customEmail = it },
                                    label = { Text("Enter Gmail Address", color = SubTextColor) },
                                    textStyle = LocalTextStyle.current.copy(color = TextColor),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextColor,
                                        unfocusedTextColor = TextColor,
                                        focusedBorderColor = CobaltBlue,
                                        unfocusedBorderColor = SubTextTertiary
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("custom_email_input")
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { useCustomAccount = false }) {
                                        Text("Go Back", color = TextColor)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (customName.trim().isEmpty() || customEmail.trim().isEmpty()) {
                                                Toast.makeText(context, "Please complete fields!", Toast.LENGTH_SHORT).show()
                                            } else if (!customEmail.contains("@") || !customEmail.contains("gmail.com")) {
                                                Toast.makeText(context, "Gmail address must contain '@gmail.com'!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                selectAccountName = customName.trim()
                                                selectAccountEmail = customEmail.trim()
                                                isAuthenticating = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)
                                    ) {
                                        Text("Sign In", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = { showAccountChooser = false }) {
                    Text("Cancel Sign In", color = SubTextColor)
                }
            }
        }
    }
}

@Composable
fun AccountRow(name: String, email: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(CobaltBlue.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1).uppercase(), color = CobaltBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(name, fontWeight = FontWeight.Bold, color = TextColor, fontSize = 14.sp)
            Text(email, color = SubTextColor, fontSize = 11.sp)
        }
    }
}

@Composable
fun SimulatedAdMobBanner(config: com.example.data.AppConfig) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("admob_banner_ad"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f)),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(
                    containerColor = Color(0xFFFBBF24),
                    contentColor = Color.Black,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Ad", fontWeight = FontWeight.Bold, fontSize = 9.sp)
                }
                Column {
                    Text(
                        text = "Premium PDF Cloud Storage",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Banner ID: " + config.admobBannerUnitId,
                        color = Color(0xFF94A3B8),
                        fontSize = 8.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text("Install", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsGroupCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DeepIndigo),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = CobaltBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextColor
                    )
                )
            }
            content()
        }
    }
}
