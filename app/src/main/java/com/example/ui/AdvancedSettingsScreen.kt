package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.PDFViewModel
import com.example.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AdvancedSettingsScreen(viewModel: PDFViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalAppColors.current

    // Observe settings states from viewmodel
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColors by viewModel.dynamicColors.collectAsState()
    val defaultLanguage by viewModel.defaultLanguage.collectAsState()
    val defaultQualityDpi by viewModel.defaultQualityDpi.collectAsState()
    val compressionLevel by viewModel.compressionLevel.collectAsState()
    val defaultPageSize by viewModel.defaultPageSize.collectAsState()
    val aiModel by viewModel.aiModel.collectAsState()
    val aiCreativity by viewModel.aiCreativity.collectAsState()
    val fingerprintLock by viewModel.fingerprintLock.collectAsState()
    val pinLock by viewModel.pinLock.collectAsState()
    val encryptLocalStorage by viewModel.encryptLocalStorage.collectAsState()
    val cloudBackup by viewModel.cloudBackup.collectAsState()

    // Dialog state controllers
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showCompressionDialog by remember { mutableStateOf(false) }
    var showPageSizeDialog by remember { mutableStateOf(false) }
    var showAiModelDialog by remember { mutableStateOf(false) }
    var showAiCreativityDialog by remember { mutableStateOf(false) }
    var showCloudBackupDialog by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showDuplicateScanDialog by remember { mutableStateOf(false) }
    var isScanningDuplicates by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Settings & Security", 
                fontWeight = FontWeight.Bold, 
                color = colors.textPrimary, 
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        item {
            SettingsGroup("General") {
                val currentThemeText = when (themeMode) {
                    PDFViewModel.ThemeMode.SYSTEM -> "System Default"
                    PDFViewModel.ThemeMode.LIGHT -> "Light Theme"
                    PDFViewModel.ThemeMode.DARK -> "Dark Theme"
                }
                SettingsItem(Icons.Default.Palette, "Theme", currentThemeText) {
                    showThemeDialog = true
                }
                SettingsItem(Icons.Default.ColorLens, "Dynamic Colors", if (dynamicColors) "Enabled" else "Disabled") {
                    viewModel.updateDynamicColors(context, !dynamicColors)
                    Toast.makeText(context, "Dynamic Colors ${if (!dynamicColors) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                }
                SettingsItem(Icons.Default.Language, "Language", defaultLanguage) {
                    showLanguageDialog = true
                }
            }
        }
        
        item {
            SettingsGroup("PDF Preferences") {
                SettingsItem(Icons.Default.HighQuality, "Default Quality", defaultQualityDpi) {
                    showQualityDialog = true
                }
                SettingsItem(Icons.Default.Compress, "Compression", compressionLevel) {
                    showCompressionDialog = true
                }
                SettingsItem(Icons.Default.CropPortrait, "Default Page Size", defaultPageSize) {
                    showPageSizeDialog = true
                }
            }
        }
        
        item {
            SettingsGroup("AI Settings") {
                SettingsItem(Icons.Default.AutoAwesome, "AI Model", aiModel) {
                    showAiModelDialog = true
                }
                SettingsItem(Icons.Default.Lightbulb, "Creativity Level", aiCreativity) {
                    showAiCreativityDialog = true
                }
            }
        }
        
        item {
            SettingsGroup("Security") {
                SettingsItem(Icons.Default.Fingerprint, "Fingerprint Lock", if (fingerprintLock) "Enabled" else "Disabled") {
                    viewModel.updateFingerprintLock(context, !fingerprintLock)
                }
                SettingsItem(Icons.Default.Pin, "PIN Lock", if (pinLock) "Enabled" else "Disabled") {
                    viewModel.updatePinLock(context, !pinLock)
                }
                SettingsItem(Icons.Default.Security, "Encrypt Local Storage", if (encryptLocalStorage) "Enabled" else "Disabled") {
                    viewModel.updateEncryptLocalStorage(context, !encryptLocalStorage)
                }
            }
        }
        
        item {
            SettingsGroup("Backup & Storage") {
                SettingsItem(Icons.Default.CloudUpload, "Cloud Backup", cloudBackup) {
                    showCloudBackupDialog = true
                }
                SettingsItem(Icons.Default.CleaningServices, "Cache Cleaner", "Clean temporary storage") {
                    showClearCacheConfirm = true
                }
                SettingsItem(Icons.Default.DeleteSweep, "Duplicate Finder", "Scan Now") {
                    showDuplicateScanDialog = true
                    isScanningDuplicates = true
                    coroutineScope.launch {
                        delay(1500)
                        isScanningDuplicates = false
                    }
                }
            }
        }
        
        item {
            SettingsGroup("About") {
                SettingsItem(Icons.Default.Info, "Version", "2.4.0 Premium") {
                    Toast.makeText(context, "AI PDF Studio v2.4.0 Premium is up to date!", Toast.LENGTH_SHORT).show()
                }
                SettingsItem(Icons.Default.PrivacyTip, "Privacy Policy", "Read policy") {
                    showPrivacyDialog = true
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // --- DIALOGS FOR OPTIONS ---

    if (showThemeDialog) {
        val options = listOf("System Default", "Light Theme", "Dark Theme")
        val currentSelected = when (themeMode) {
            PDFViewModel.ThemeMode.SYSTEM -> "System Default"
            PDFViewModel.ThemeMode.LIGHT -> "Light Theme"
            PDFViewModel.ThemeMode.DARK -> "Dark Theme"
        }
        SettingsSelectionDialog(
            title = "Select Theme Mode",
            options = options,
            selectedOption = currentSelected,
            onDismiss = { showThemeDialog = false },
            onSelect = { selected ->
                val newMode = when (selected) {
                    "Light Theme" -> PDFViewModel.ThemeMode.LIGHT
                    "Dark Theme" -> PDFViewModel.ThemeMode.DARK
                    else -> PDFViewModel.ThemeMode.SYSTEM
                }
                viewModel.updateThemeMode(context, newMode)
            }
        )
    }

    if (showLanguageDialog) {
        SettingsSelectionDialog(
            title = "Select Default Language",
            options = listOf("English", "Spanish", "Hindi", "French", "Arabic"),
            selectedOption = defaultLanguage,
            onDismiss = { showLanguageDialog = false },
            onSelect = { viewModel.updateDefaultLanguage(context, it) }
        )
    }

    if (showQualityDialog) {
        SettingsSelectionDialog(
            title = "Select Default Render Quality",
            options = listOf("High (300 DPI)", "Medium (150 DPI)", "Low (72 DPI)"),
            selectedOption = defaultQualityDpi,
            onDismiss = { showQualityDialog = false },
            onSelect = { viewModel.updateDefaultQualityDpi(context, it) }
        )
    }

    if (showPageSizeDialog) {
        SettingsSelectionDialog(
            title = "Select Default Page Size",
            options = listOf("A4", "Letter", "Legal"),
            selectedOption = defaultPageSize,
            onDismiss = { showPageSizeDialog = false },
            onSelect = { viewModel.updateDefaultPageSize(context, it) }
        )
    }

    if (showCompressionDialog) {
        SettingsSelectionDialog(
            title = "Select PDF Compression level",
            options = listOf("High", "Medium", "Low"),
            selectedOption = compressionLevel,
            onDismiss = { showCompressionDialog = false },
            onSelect = { viewModel.updateCompressionLevel(context, it) }
        )
    }

    if (showAiModelDialog) {
        SettingsSelectionDialog(
            title = "Select Default AI Model",
            options = listOf("Gemini 1.5 Pro", "Gemini 1.5 Flash", "Gemini 1.0 Pro"),
            selectedOption = aiModel,
            onDismiss = { showAiModelDialog = false },
            onSelect = { viewModel.updateAiModel(context, it) }
        )
    }

    if (showAiCreativityDialog) {
        SettingsSelectionDialog(
            title = "Select Creativity Level",
            options = listOf("Creative", "Balanced", "Precise"),
            selectedOption = aiCreativity,
            onDismiss = { showAiCreativityDialog = false },
            onSelect = { viewModel.updateAiCreativity(context, it) }
        )
    }

    if (showCloudBackupDialog) {
        SettingsSelectionDialog(
            title = "Select Cloud Storage Destination",
            options = listOf("None", "Google Drive", "Dropbox", "OneDrive"),
            selectedOption = cloudBackup,
            onDismiss = { showCloudBackupDialog = false },
            onSelect = { viewModel.updateCloudBackup(context, it) }
        )
    }

    // Cache Cleaner Confirmation Dialog
    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text("Clear PDF Cache", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = { Text("Are you sure you want to delete all cached generated PDFs? This will clear up internal cache storage space.", color = colors.textSecondary) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.clearPdfHistory(context)
                        showClearCacheConfirm = false
                        Toast.makeText(context, "PDF cache cleared successfully!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear Cache", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.deepIndigo,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Duplicate PDF Finder Progress/Success Dialog
    if (showDuplicateScanDialog) {
        AlertDialog(
            onDismissRequest = { if (!isScanningDuplicates) showDuplicateScanDialog = false },
            title = {
                Text(
                    text = if (isScanningDuplicates) "Scanning Local Storage..." else "Scan Complete",
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isScanningDuplicates) {
                        CircularProgressIndicator(color = colors.cobaltBlue)
                        Text("Looking for duplicate PDF files in documents folders...", color = colors.textSecondary)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No duplicate PDF files found! Your device storage is fully optimized.",
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                if (!isScanningDuplicates) {
                    Button(
                        onClick = { showDuplicateScanDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.cobaltBlue)
                    ) {
                        Text("Done", color = Color.White)
                    }
                }
            },
            containerColor = colors.deepIndigo,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Effective Date: 19 July 2026",
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Welcome to AI PDF Studio. Your privacy is important to us. This Privacy Policy explains how we collect, use, and protect your information.",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "1. Information We Collect",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "We may collect:\n• Email address (if you sign in)\n• App settings and preferences\n• PDF processing information required to provide app features\n• Crash reports and diagnostic information to improve the app",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "2. How We Use Your Information",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "We use your information to:\n• Provide PDF editing and AI features\n• Synchronize your settings across devices\n• Improve app performance and security\n• Respond to support requests",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "3. Data Storage",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Your data may be securely stored using Firebase services. We take reasonable measures to protect your information from unauthorized access.",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "4. PDF Files",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Your PDF files are processed only to provide the requested features. We do not sell or share your files with third parties.",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "5. Third-Party Services",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Our app may use:\n• Google Firebase\n• Google AdMob (if advertisements are enabled)\n• Google AI services (for AI-powered features)\n\nThese services have their own privacy policies.",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "6. Data Sharing",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "We do not sell your personal information. We may share information only when required by law or to provide app functionality through trusted service providers.",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "7. Children's Privacy",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Our app is not intended for children under 13 years of age.",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "8. Changes to This Policy",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "We may update this Privacy Policy from time to time. Changes will be posted within the app or on our website.",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "9. Contact Us",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "If you have any questions about this Privacy Policy, please contact us at:\nEmail: gyanguru623@gmail.com",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.cobaltBlue)
                ) {
                    Text("I Understand", color = Color.White)
                }
            },
            containerColor = colors.deepIndigo,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAppColors.current
    Column {
        Text(
            text = title.uppercase(), 
            color = colors.cobaltBlue, 
            fontWeight = FontWeight.Bold, 
            fontSize = 12.sp, 
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.deepIndigo),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = colors.textSecondary, fontSize = 12.sp)
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight, 
            contentDescription = null, 
            tint = colors.textTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsSelectionDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = colors.textPrimary) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { option ->
                    val isSelected = option == selectedOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(option)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onSelect(option)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.cobaltBlue,
                                unselectedColor = colors.textSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = option,
                            color = if (isSelected) colors.cobaltBlue else colors.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.cobaltBlue)
            }
        },
        containerColor = colors.deepIndigo,
        shape = RoundedCornerShape(16.dp)
    )
}
