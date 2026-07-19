package com.example.viewmodel

import androidx.lifecycle.viewModelScope
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.example.api.GeminiService
import com.example.data.AppDatabase
import com.example.data.UserLogin
import com.example.data.AppConfig
import com.example.model.GeneratedPdfReport
import com.example.model.OverlayShape
import com.example.model.OverlaySignature
import com.example.model.OverlayText
import com.example.model.SelectedImagePage
import com.example.model.ShapeType
import com.example.util.PDFCompiler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PDFViewModel : ViewModel() {

    private val geminiService = GeminiService()

    // --- GMAIL SIGNUP/LOGIN STATE ---
    var isUserLoggedIn by mutableStateOf(true)
    var userEmail by mutableStateOf("user@ai.studio")
    var userName by mutableStateOf("AI Studio User")

    // --- ADMIN PANEL STATE ---
    var isAdminLoggedIn by mutableStateOf(false)

    // --- LIVE FIREBASE STATEFLOWS ---
    private val _aiEnabled = MutableStateFlow(true)
    val aiEnabled: StateFlow<Boolean> = _aiEnabled.asStateFlow()

    private val _ocrEnabled = MutableStateFlow(true)
    val ocrEnabled: StateFlow<Boolean> = _ocrEnabled.asStateFlow()

    private val _chatPdfEnabled = MutableStateFlow(true)
    val chatPdfEnabled: StateFlow<Boolean> = _chatPdfEnabled.asStateFlow()

    private val _translationEnabled = MutableStateFlow(true)
    val translationEnabled: StateFlow<Boolean> = _translationEnabled.asStateFlow()

    private val _rewriteEnabled = MutableStateFlow(true)
    val rewriteEnabled: StateFlow<Boolean> = _rewriteEnabled.asStateFlow()

    private val _summarizeEnabled = MutableStateFlow(true)
    val summarizeEnabled: StateFlow<Boolean> = _summarizeEnabled.asStateFlow()

    private val _grammarEnabled = MutableStateFlow(true)
    val grammarEnabled: StateFlow<Boolean> = _grammarEnabled.asStateFlow()

    private val _notesEnabled = MutableStateFlow(true)
    val notesEnabled: StateFlow<Boolean> = _notesEnabled.asStateFlow()

    private val _quizEnabled = MutableStateFlow(true)
    val quizEnabled: StateFlow<Boolean> = _quizEnabled.asStateFlow()

    private val _flashcardsEnabled = MutableStateFlow(true)
    val flashcardsEnabled: StateFlow<Boolean> = _flashcardsEnabled.asStateFlow()

    private val _geminiApiKey = MutableStateFlow("")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _maintenanceMode = MutableStateFlow(false)
    val maintenanceMode: StateFlow<Boolean> = _maintenanceMode.asStateFlow()

    private val _forceUpdate = MutableStateFlow(false)
    val forceUpdate: StateFlow<Boolean> = _forceUpdate.asStateFlow()

    private val _latestVersion = MutableStateFlow("1.0.0")
    val latestVersion: StateFlow<String> = _latestVersion.asStateFlow()

    private val _minimumVersion = MutableStateFlow("1.0.0")
    val minimumVersion: StateFlow<String> = _minimumVersion.asStateFlow()

    private val _welcomePopup = MutableStateFlow("")
    val welcomePopup: StateFlow<String> = _welcomePopup.asStateFlow()

    private val _noticeBanner = MutableStateFlow("")
    val noticeBanner: StateFlow<String> = _noticeBanner.asStateFlow()

    private val _firebaseTheme = MutableStateFlow(ThemeMode.SYSTEM)
    val firebaseTheme: StateFlow<ThemeMode> = _firebaseTheme.asStateFlow()

    private val _enableScan = MutableStateFlow(true)
    val enableScan: StateFlow<Boolean> = _enableScan.asStateFlow()

    private val _enableMerge = MutableStateFlow(true)
    val enableMerge: StateFlow<Boolean> = _enableMerge.asStateFlow()

    private val _enableSplit = MutableStateFlow(true)
    val enableSplit: StateFlow<Boolean> = _enableSplit.asStateFlow()

    private val _enableCompress = MutableStateFlow(true)
    val enableCompress: StateFlow<Boolean> = _enableCompress.asStateFlow()

    private val _enableOCR = MutableStateFlow(true)
    val enableOCR: StateFlow<Boolean> = _enableOCR.asStateFlow()

    private val _enableAI = MutableStateFlow(true)
    val enableAI: StateFlow<Boolean> = _enableAI.asStateFlow()

    private val _enablePdfEditor = MutableStateFlow(true)
    val enablePdfEditor: StateFlow<Boolean> = _enablePdfEditor.asStateFlow()

    private val _enableHistory = MutableStateFlow(true)
    val enableHistory: StateFlow<Boolean> = _enableHistory.asStateFlow()

    private val _enableSettings = MutableStateFlow(true)
    val enableSettings: StateFlow<Boolean> = _enableSettings.asStateFlow()

    private val _enablePremium = MutableStateFlow(true)
    val enablePremium: StateFlow<Boolean> = _enablePremium.asStateFlow()

    fun startFirebaseSync(context: Context) {
        viewModelScope.launch {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                // Real-time listener for app_settings
                db.collection("app_settings").document("current")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            error.printStackTrace()
                            return@addSnapshotListener
                        }
                        if (snapshot != null && snapshot.exists()) {
                            val customAppName = snapshot.getString("customAppName") ?: snapshot.getString("latestVersion")?.let { "AI PDF Studio" } ?: "AI PDF Studio"
                            val admobEnabled = snapshot.getBoolean("admobEnabled") ?: false
                            _maintenanceMode.value = snapshot.getBoolean("maintenanceMode") ?: false
                            _forceUpdate.value = snapshot.getBoolean("forceUpdate") ?: false
                            _latestVersion.value = snapshot.getString("latestVersion") ?: "1.0.0"
                            _minimumVersion.value = snapshot.getString("minimumVersion") ?: "1.0.0"
                            _welcomePopup.value = snapshot.getString("welcomePopup") ?: ""
                            _noticeBanner.value = snapshot.getString("noticeBanner") ?: ""
                            
                            val themeStr = snapshot.getString("theme") ?: "system"
                            _firebaseTheme.value = when (themeStr.lowercase()) {
                                "light" -> ThemeMode.LIGHT
                                "dark" -> ThemeMode.DARK
                                else -> ThemeMode.SYSTEM
                            }

                            _enableScan.value = snapshot.getBoolean("enableScan") ?: true
                            _enableMerge.value = snapshot.getBoolean("enableMerge") ?: true
                            _enableSplit.value = snapshot.getBoolean("enableSplit") ?: true
                            _enableCompress.value = snapshot.getBoolean("enableCompress") ?: true
                            _enableOCR.value = snapshot.getBoolean("enableOCR") ?: true
                            _enableAI.value = snapshot.getBoolean("enableAI") ?: true
                            _enablePdfEditor.value = snapshot.getBoolean("enablePdfEditor") ?: true
                            _enableHistory.value = snapshot.getBoolean("enableHistory") ?: true
                            _enableSettings.value = snapshot.getBoolean("enableSettings") ?: true
                            _enablePremium.value = snapshot.getBoolean("enablePremium") ?: true

                            // Sync into local AppConfig too
                            viewModelScope.launch {
                                val current = _appConfig.value
                                val updated = current.copy(
                                    customAppName = customAppName,
                                    admobEnabled = admobEnabled
                                )
                                _appConfig.value = updated
                                com.example.data.AppDatabase.getDatabase(context).appConfigDao().saveConfig(updated)
                            }
                        }
                    }

                // Real-time listener for admob_settings
                db.collection("admob_settings").document("current")
                    .addSnapshotListener { snapshot, error ->
                        if (snapshot != null && snapshot.exists()) {
                            viewModelScope.launch {
                                val current = _appConfig.value
                                val updated = current.copy(
                                    admobEnabled = snapshot.getBoolean("admobEnabled") ?: snapshot.getBoolean("enabled") ?: false,
                                    admobBannerUnitId = snapshot.getString("admobBannerUnitId") ?: snapshot.getString("bannerAdUnitId") ?: current.admobBannerUnitId,
                                    admobInterstitialUnitId = snapshot.getString("admobInterstitialUnitId") ?: snapshot.getString("interstitialAdUnitId") ?: current.admobInterstitialUnitId,
                                    admobRewardedUnitId = snapshot.getString("admobRewardedUnitId") ?: snapshot.getString("rewardedAdUnitId") ?: current.admobRewardedUnitId,
                                    admobNativeUnitId = snapshot.getString("admobNativeUnitId") ?: snapshot.getString("nativeAdUnitId") ?: current.admobNativeUnitId,
                                    admobAppOpenUnitId = snapshot.getString("admobAppOpenUnitId") ?: snapshot.getString("appOpenAdUnitId") ?: current.admobAppOpenUnitId,
                                    testAdMode = snapshot.getBoolean("testAdMode") ?: snapshot.getBoolean("testMode") ?: true
                                )
                                _appConfig.value = updated
                                com.example.data.AppDatabase.getDatabase(context).appConfigDao().saveConfig(updated)
                            }
                        }
                    }

                // Real-time listener for ai_settings
                db.collection("ai_settings").document("current")
                    .addSnapshotListener { snapshot, error ->
                        if (snapshot != null && snapshot.exists()) {
                            _aiEnabled.value = snapshot.getBoolean("aiEnabled") ?: snapshot.getBoolean("enabled") ?: true
                            _ocrEnabled.value = snapshot.getBoolean("ocrEnabled") ?: true
                            _chatPdfEnabled.value = snapshot.getBoolean("chatPdf") ?: true
                            _translationEnabled.value = snapshot.getBoolean("translation") ?: true
                            _rewriteEnabled.value = snapshot.getBoolean("rewrite") ?: true
                            _summarizeEnabled.value = snapshot.getBoolean("summarize") ?: true
                            _grammarEnabled.value = snapshot.getBoolean("grammar") ?: true
                            _notesEnabled.value = snapshot.getBoolean("notes") ?: true
                            _quizEnabled.value = snapshot.getBoolean("quiz") ?: true
                            _flashcardsEnabled.value = snapshot.getBoolean("flashcards") ?: true
                            _geminiApiKey.value = snapshot.getString("geminiApiKey") ?: ""
                        }
                    }

                // Track and report dynamic session statistics to Firestore analytics
                val userId = userEmail.replace(".", "_")
                val statRef = db.collection("analytics").document("overview")
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(statRef)
                    val totalUsers = (snapshot.getLong("totalUsers") ?: 0L) + 1L
                    transaction.set(statRef, mapOf("totalUsers" to totalUsers), com.google.firebase.firestore.SetOptions.merge())
                }

                // Log user entry
                val userLogRef = db.collection("users").document(userId)
                userLogRef.set(mapOf(
                    "email" to userEmail,
                    "displayName" to userName,
                    "lastLogin" to com.google.firebase.Timestamp.now(),
                    "device" to "Android Phone",
                    "status" to "Active",
                    "premium" to true
                ), com.google.firebase.firestore.SetOptions.merge())

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- ROOM PERSISTED admin configuration state ---
    private val _appConfig = MutableStateFlow<AppConfig>(AppConfig())
    val appConfig: StateFlow<AppConfig> = _appConfig.asStateFlow()

    // --- ROOM PERSISTED user login information list ---
    private val _userLogins = MutableStateFlow<List<UserLogin>>(emptyList())
    val userLogins: StateFlow<List<UserLogin>> = _userLogins.asStateFlow()

    // --- PERSISTENT USER SETTINGS ---
    enum class ThemeMode {
        SYSTEM, LIGHT, DARK
    }

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _pdfPrefix = MutableStateFlow("PDF_")
    val pdfPrefix: StateFlow<String> = _pdfPrefix.asStateFlow()

    private val _defaultLanguage = MutableStateFlow("English")
    val defaultLanguage: StateFlow<String> = _defaultLanguage.asStateFlow()

    private val _defaultTone = MutableStateFlow("Professional")
    val defaultTone: StateFlow<String> = _defaultTone.asStateFlow()

    private val _defaultHasMargin = MutableStateFlow(true)
    val defaultHasMargin: StateFlow<Boolean> = _defaultHasMargin.asStateFlow()

    private val _compressionLevel = MutableStateFlow("Medium") // High, Medium, Low
    val compressionLevel: StateFlow<String> = _compressionLevel.asStateFlow()

    private val _dynamicColors = MutableStateFlow(true)
    val dynamicColors: StateFlow<Boolean> = _dynamicColors.asStateFlow()

    private val _defaultQualityDpi = MutableStateFlow("High (300 DPI)")
    val defaultQualityDpi: StateFlow<String> = _defaultQualityDpi.asStateFlow()

    private val _defaultPageSize = MutableStateFlow("A4")
    val defaultPageSize: StateFlow<String> = _defaultPageSize.asStateFlow()

    private val _aiModel = MutableStateFlow("Gemini 1.5 Pro")
    val aiModel: StateFlow<String> = _aiModel.asStateFlow()

    private val _aiCreativity = MutableStateFlow("Balanced")
    val aiCreativity: StateFlow<String> = _aiCreativity.asStateFlow()

    private val _fingerprintLock = MutableStateFlow(false)
    val fingerprintLock: StateFlow<Boolean> = _fingerprintLock.asStateFlow()

    private val _pinLock = MutableStateFlow(false)
    val pinLock: StateFlow<Boolean> = _pinLock.asStateFlow()

    private val _encryptLocalStorage = MutableStateFlow(true)
    val encryptLocalStorage: StateFlow<Boolean> = _encryptLocalStorage.asStateFlow()

    private val _cloudBackup = MutableStateFlow("Google Drive")
    val cloudBackup: StateFlow<String> = _cloudBackup.asStateFlow()

    fun loadSettings(context: Context) {
        val prefs = context.getSharedPreferences("ai_pdf_studio_settings", Context.MODE_PRIVATE)
        
        isUserLoggedIn = true
        userEmail = prefs.getString("user_email", "user@ai.studio") ?: "user@ai.studio"
        if (userEmail.isEmpty()) { userEmail = "user@ai.studio" }
        userName = prefs.getString("user_name", "AI Studio User") ?: "AI Studio User"
        if (userName.isEmpty()) { userName = "AI Studio User" }

        val savedTheme = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val parsedTheme = try { ThemeMode.valueOf(savedTheme) } catch(e: Exception) { ThemeMode.SYSTEM }
        _themeMode.value = parsedTheme
        _pdfPrefix.value = prefs.getString("pdf_prefix", "PDF_") ?: "PDF_"
        _defaultLanguage.value = prefs.getString("default_language", "English") ?: "English"
        _defaultTone.value = prefs.getString("default_tone", "Professional") ?: "Professional"
        _defaultHasMargin.value = prefs.getBoolean("default_has_margin", true)
        _compressionLevel.value = prefs.getString("compression_level", "Medium") ?: "Medium"
        _dynamicColors.value = prefs.getBoolean("dynamic_colors", true)
        _defaultQualityDpi.value = prefs.getString("default_quality_dpi", "High (300 DPI)") ?: "High (300 DPI)"
        _defaultPageSize.value = prefs.getString("default_page_size", "A4") ?: "A4"
        _aiModel.value = prefs.getString("ai_model", "Gemini 1.5 Pro") ?: "Gemini 1.5 Pro"
        _aiCreativity.value = prefs.getString("ai_creativity", "Balanced") ?: "Balanced"
        _fingerprintLock.value = prefs.getBoolean("fingerprint_lock", false)
        _pinLock.value = prefs.getBoolean("pin_lock", false)
        _encryptLocalStorage.value = prefs.getBoolean("encrypt_local_storage", true)
        _cloudBackup.value = prefs.getString("cloud_backup", "Google Drive") ?: "Google Drive"

        // Load configurations and logins from Room Database asynchronously
        viewModelScope.launch {
            try {
                var config = AppDatabase.getDatabase(context).appConfigDao().getConfig()
                if (config == null) {
                    config = AppConfig()
                    AppDatabase.getDatabase(context).appConfigDao().saveConfig(config)
                }
                _appConfig.value = config

                // Observe configurations updates
                AppDatabase.getDatabase(context).appConfigDao().getConfigFlow().collect { updated ->
                    if (updated != null) {
                        _appConfig.value = updated
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        viewModelScope.launch {
            try {
                AppDatabase.getDatabase(context).userLoginDao().getAllLogins().collect { loginsList ->
                    _userLogins.value = loginsList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Gmail Login Session Save
    fun saveLoginSession(context: Context, name: String, email: String) {
        isUserLoggedIn = true
        userEmail = email
        userName = name
        
        val prefs = context.getSharedPreferences("ai_pdf_studio_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("user_logged_in", true)
            .putString("user_email", email)
            .putString("user_name", name)
            .apply()

        // Persistent Room registration
        viewModelScope.launch {
            try {
                AppDatabase.getDatabase(context).userLoginDao().insertLogin(UserLogin(displayName = name, email = email))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Gmail Log out
    fun logoutUser(context: Context) {
        // Sign out is disabled in single-user mode to bypass login screen
        isAdminLoggedIn = false
    }

    // Admin Config Persistence Save
    fun saveAdminConfig(context: Context, newConfig: AppConfig) {
        viewModelScope.launch {
            try {
                AppDatabase.getDatabase(context).appConfigDao().saveConfig(newConfig)
                _appConfig.value = newConfig
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Clear saved login audits in local Room DB
    fun clearLoginLogs(context: Context) {
        viewModelScope.launch {
            try {
                AppDatabase.getDatabase(context).userLoginDao().clearLogins()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateThemeMode(context: Context, mode: ThemeMode) {
        _themeMode.value = mode
        val prefs = context.getSharedPreferences("ai_pdf_studio_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun updatePdfPrefix(context: Context, prefix: String) {
        _pdfPrefix.value = prefix
        val prefs = context.getSharedPreferences("ai_pdf_studio_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("pdf_prefix", prefix).apply()
    }

    fun updateDefaultLanguage(context: Context, language: String) {
        _defaultLanguage.value = language
        val prefs = context.getSharedPreferences("ai_pdf_studio_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("default_language", language).apply()
    }

    fun updateDefaultTone(context: Context, tone: String) {
        _defaultTone.value = tone
        val prefs = context.getSharedPreferences("ai_pdf_studio_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("default_tone", tone).apply()
    }

    fun updateDefaultHasMargin(context: Context, hasMargin: Boolean) {
        _defaultHasMargin.value = hasMargin
        val prefs = context.getSharedPreferences("ai_pdf_studio_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("default_has_margin", hasMargin).apply()
    }

    fun updateCompressionLevel(context: Context, level: String) {
        _compressionLevel.value = level
        val prefs = context.getSharedPreferences("ai_pdf_studio_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("compression_level", level).apply()
    }

    fun updateDynamicColors(context: Context, enabled: Boolean) {
        _dynamicColors.value = enabled
        val prefs = context.getSharedPreferences("ai_pdf_studio_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("dynamic_colors", enabled).apply()
    }

    fun updateDefaultQualityDpi(context: Context, quality: String) {
        _defaultQualityDpi.value = quality
        val prefs = context.getSharedPreferences("ai_pdf_studio_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("default_quality_dpi", quality).apply()
    }

    fun updateDefaultPageSize(context: Context, size: String) {
        _defaultPageSize.value = size
        val prefs = context.getSharedPreferences("ai_pdf_studio_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("default_page_size", size).apply()
    }

    fun updateAiModel(context: Context, model: String) {
        _aiModel.value = model
        val prefs = context.getSharedPreferences("ai_pdf_studio_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("ai_model", model).apply()
    }

    fun updateAiCreativity(context: Context, level: String) {
        _aiCreativity.value = level
        val prefs = context.getSharedPreferences("ai_pdf_studio_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("ai_creativity", level).apply()
    }

    fun updateFingerprintLock(context: Context, enabled: Boolean) {
        _fingerprintLock.value = enabled
        val prefs = context.getSharedPreferences("ai_pdf_studio_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("fingerprint_lock", enabled).apply()
    }

    fun updatePinLock(context: Context, enabled: Boolean) {
        _pinLock.value = enabled
        val prefs = context.getSharedPreferences("ai_pdf_studio_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("pin_lock", enabled).apply()
    }

    fun updateEncryptLocalStorage(context: Context, enabled: Boolean) {
        _encryptLocalStorage.value = enabled
        val prefs = context.getSharedPreferences("ai_pdf_studio_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("encrypt_local_storage", enabled).apply()
    }

    fun updateCloudBackup(context: Context, backup: String) {
        _cloudBackup.value = backup
        val prefs = context.getSharedPreferences("ai_pdf_studio_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("cloud_backup", backup).apply()
    }

    fun clearPdfHistory(context: Context) {
        viewModelScope.launch {
            isProcessing = true
            processingStatusWord = "Deleting compiled PDF cache store..."
            val outputDir = File(context.cacheDir, "compiled_pdfs")
            if (outputDir.exists()) {
                outputDir.listFiles()?.forEach { file ->
                    file.delete()
                }
            }
            loadHistory(context)
            isProcessing = false
        }
    }

    // Screen navigation layout / Tabs index (0 = Convert/Editor, 1 = History, 2 = AI Docs Assistant)
    var currentTab by mutableStateOf(0)
        private set

    // Selected PDF Pages cache
    private val _selectedPages = MutableStateFlow<List<SelectedImagePage>>(emptyList())
    val selectedPages: StateFlow<List<SelectedImagePage>> = _selectedPages.asStateFlow()

    // Selected Page index inside Canvas Canvas editor
    var activePageIndex by mutableStateOf(-1)

    // Export histories list
    private val _exportHistory = MutableStateFlow<List<GeneratedPdfReport>>(emptyList())
    val exportHistory: StateFlow<List<GeneratedPdfReport>> = _exportHistory.asStateFlow()

    // Loading / processing indicators
    var isProcessing by mutableStateOf(false)
        private set

    var processingStatusWord by mutableStateOf("")
        private set

    // Gemini API Action results
    var ocrResultText by mutableStateOf("")
    var aiAssistantResultText by mutableStateOf("")

    // API Input / Workspace Text Container for Docs Assistant Tab
    var docsAssistantInputText by mutableStateOf("")

    fun setTab(index: Int) {
        currentTab = index
    }

    // Refresh history
    fun loadHistory(context: Context) {
        viewModelScope.launch {
            val outputDir = File(context.cacheDir, "compiled_pdfs")
            if (!outputDir.exists()) outputDir.mkdirs()

            var pdfFiles = outputDir.listFiles { file -> file.extension.lowercase(Locale.ROOT) == "pdf" } ?: emptyArray()

            // If empty, generate a pre-compiled sample demo PDF file!
            if (pdfFiles.isEmpty()) {
                val demoFile = File(outputDir, "Demo_AI_PDF_Studio.pdf")
                if (!demoFile.exists()) {
                    generateSampleCompiledPdf(context, demoFile)
                }
                pdfFiles = outputDir.listFiles { file -> file.extension.lowercase(Locale.ROOT) == "pdf" } ?: emptyArray()
            }

            val reports = pdfFiles.map { file ->
                val sizeBytes = file.length()
                val formattedSize = formatFileSize(sizeBytes)
                val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                val formattedDate = sdf.format(Date(file.lastModified()))
                GeneratedPdfReport(
                    file = file,
                    displayName = file.name,
                    numPages = getPdfPageCountApprox(file),
                    fileSizeFormatted = formattedSize,
                    dateString = formattedDate
                )
            }.sortedByDescending { it.file.lastModified() }

            _exportHistory.value = reports
        }
    }

    private fun generateSampleCompiledPdf(context: Context, file: File) {
        try {
            val pdfDocument = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = android.graphics.Paint()
            
            // Draw gradient or neat blocks
            paint.color = 0xFF1E1B4B.toInt() // rich dark indigo background
            paint.style = android.graphics.Paint.Style.FILL
            canvas.drawRect(0f, 0f, 595f, 842f, paint)
            
            // Title
            paint.color = 0xFF818CF8.toInt() // Indigo custom
            paint.textSize = 34f
            paint.isFakeBoldText = true
            canvas.drawText("AI PDF STUDIO DEMO", 60f, 180f, paint)
            
            // Subtitle
            paint.color = 0xFFFFFFFF.toInt()
            paint.textSize = 18f
            paint.isFakeBoldText = false
            canvas.drawText("An elegant offline-first document platform.", 60f, 220f, paint)
            
            // Content paragraph
            paint.color = 0xFF94A3B8.toInt() // slate 400
            paint.textSize = 15f
            var currentY = 320f
            val lines = arrayOf(
                "This pre-compiled PDF demonstrates the robust graphics rendering",
                "and compilation framework built within AI PDF Studio. You can",
                "create your own layouts from physical image scans or camera snaps.",
                "",
                "Key App Capabilities Activated:",
                "  • Multi-Image Stitching & Re-ordering",
                "  • Live Canvas Editing & Signature Overlays",
                "  • Intelligent Gemini Pro AI-OCR Content Cleaner",
                "  • Smart Content Assistance (Summarize, Rewrite, Translate)",
                "",
                "Use the 'Convert & Editor' tab to build custom compilations."
            )
            for (line in lines) {
                canvas.drawText(line, 60f, currentY, paint)
                currentY += 32f
            }
            
            pdfDocument.finishPage(page)
            java.io.FileOutputStream(file).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Programmatically generate beautiful mock templates for instantaneous testing

    fun deletePdf(context: Context, report: GeneratedPdfReport) {
        viewModelScope.launch {
            if (report.file.exists()) {
                report.file.delete()
            }
            loadHistory(context)
        }
    }

    // Load selected image uris
    fun addImages(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            val list = _selectedPages.value.toMutableList()
            val ratio = when (_compressionLevel.value) {
                "High" -> 100
                "Medium" -> 85
                "Low" -> 60
                else -> 85
            }
            val defaultMargin = if (_defaultHasMargin.value) 16 else 0

            for (uri in uris) {
                val name = getFileName(context, uri) ?: "Image-${list.size + 1}"
                list.add(
                    SelectedImagePage(
                        uri = uri,
                        name = name,
                        compressionRatio = ratio,
                        marginDp = defaultMargin
                    )
                )
            }
            _selectedPages.value = list
            if (activePageIndex == -1 && list.isNotEmpty()) {
                activePageIndex = 0
            }
        }
    }

    fun removePage(index: Int) {
        val list = _selectedPages.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _selectedPages.value = list
            
            // Re-bound active index
            if (list.isEmpty()) {
                activePageIndex = -1
            } else if (activePageIndex >= list.size) {
                activePageIndex = list.size - 1
            }
        }
    }

    fun clearSelections() {
        _selectedPages.value = emptyList()
        activePageIndex = -1
        ocrResultText = ""
    }

    // Move page up or down
    fun movePage(index: Int, direction: Int) {
        val list = _selectedPages.value.toMutableList()
        val targetIndex = index + direction
        if (index in list.indices && targetIndex in list.indices) {
            val temp = list[index]
            list[index] = list[targetIndex]
            list[targetIndex] = temp
            _selectedPages.value = list
            if (activePageIndex == index) {
                activePageIndex = targetIndex
            } else if (activePageIndex == targetIndex) {
                activePageIndex = index
            }
        }
    }

    // Canvas Parameter modifiers
    fun updatePageMargin(index: Int, marginDp: Int) {
        val list = _selectedPages.value.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(marginDp = marginDp)
            _selectedPages.value = list
        }
    }

    fun updatePageOrientation(index: Int, isLandscape: Boolean) {
        val list = _selectedPages.value.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(isLandscape = isLandscape)
            _selectedPages.value = list
        }
    }

    fun updatePageQuality(index: Int, quality: Int) {
        val list = _selectedPages.value.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(compressionRatio = quality.coerceIn(10, 100))
            _selectedPages.value = list
        }
    }

    // Overlay Operations
    fun addTextToActivePage(text: String, x: Float = 0.3f, y: Float = 0.4f, fontSize: Float = 16f, color: Int = 0xFF000000.toInt()) {
        val idx = activePageIndex
        val list = _selectedPages.value.toMutableList()
        if (idx in list.indices) {
            val page = list[idx]
            val newTexts = page.overlayTexts + OverlayText(text = text, x = x, y = y, fontSize = fontSize, color = color)
            list[idx] = page.copy(overlayTexts = newTexts)
            _selectedPages.value = list
        }
    }

    fun updateTextInActivePage(textId: String, newText: String, x: Float, y: Float, fontSize: Float, color: Int) {
        val idx = activePageIndex
        val list = _selectedPages.value.toMutableList()
        if (idx in list.indices) {
            val page = list[idx]
            val newTexts = page.overlayTexts.map {
                if (it.id == textId) {
                    it.copy(text = newText, x = x, y = y, fontSize = fontSize, color = color)
                } else it
            }
            list[idx] = page.copy(overlayTexts = newTexts)
            _selectedPages.value = list
        }
    }

    fun removeTextFromActivePage(textId: String) {
        val idx = activePageIndex
        val list = _selectedPages.value.toMutableList()
        if (idx in list.indices) {
            val page = list[idx]
            val newTexts = page.overlayTexts.filter { it.id != textId }
            list[idx] = page.copy(overlayTexts = newTexts)
            _selectedPages.value = list
        }
    }

    fun addShapeToActivePage(type: ShapeType, x: Float = 0.4f, y: Float = 0.4f, color: Int = 0xFFE53935.toInt()) {
        val idx = activePageIndex
        val list = _selectedPages.value.toMutableList()
        if (idx in list.indices) {
            val page = list[idx]
            val newShapes = page.overlayShapes + OverlayShape(type = type, x = x, y = y, color = color)
            list[idx] = page.copy(overlayShapes = newShapes)
            _selectedPages.value = list
        }
    }

    fun updateShapeInActivePage(shapeId: String, x: Float, y: Float, width: Float, height: Float) {
        val idx = activePageIndex
        val list = _selectedPages.value.toMutableList()
        if (idx in list.indices) {
            val page = list[idx]
            val newShapes = page.overlayShapes.map {
                if (it.id == shapeId) {
                    it.copy(x = x, y = y, width = width, height = height)
                } else it
            }
            list[idx] = page.copy(overlayShapes = newShapes)
            _selectedPages.value = list
        }
    }

    fun removeShapeFromActivePage(shapeId: String) {
        val idx = activePageIndex
        val list = _selectedPages.value.toMutableList()
        if (idx in list.indices) {
            val page = list[idx]
            val newShapes = page.overlayShapes.filter { it.id != shapeId }
            list[idx] = page.copy(overlayShapes = newShapes)
            _selectedPages.value = list
        }
    }

    fun addSignatureToActivePage(points: List<Offset>, x: Float = 0.35f, y: Float = 0.7f, color: Int = 0xFF1E88E5.toInt()) {
        val idx = activePageIndex
        val list = _selectedPages.value.toMutableList()
        if (idx in list.indices) {
            val page = list[idx]
            val newSigs = page.overlaySignatures + OverlaySignature(points = points, x = x, y = y, color = color)
            list[idx] = page.copy(overlaySignatures = newSigs)
            _selectedPages.value = list
        }
    }

    fun updateSignatureInActivePage(sigId: String, x: Float, y: Float) {
        val idx = activePageIndex
        val list = _selectedPages.value.toMutableList()
        if (idx in list.indices) {
            val page = list[idx]
            val newSigs = page.overlaySignatures.map {
                if (it.id == sigId) {
                    it.copy(x = x, y = y)
                } else it
            }
            list[idx] = page.copy(overlaySignatures = newSigs)
            _selectedPages.value = list
        }
    }

    fun removeSignatureFromActivePage(sigId: String) {
        val idx = activePageIndex
        val list = _selectedPages.value.toMutableList()
        if (idx in list.indices) {
            val page = list[idx]
            val newSigs = page.overlaySignatures.filter { it.id != sigId }
            list[idx] = page.copy(overlaySignatures = newSigs)
            _selectedPages.value = list
        }
    }

    // Gemini Smart OCR Cleanup Call
    fun runOcrOnActivePage(context: Context, onSuccess: () -> Unit) {
        val idx = activePageIndex
        val list = _selectedPages.value
        if (idx !in list.indices) return

        viewModelScope.launch {
            isProcessing = true
            processingStatusWord = "Scanning Image & Formatting Text via Gemini..."
            ocrResultText = "Analyzing..."

            val page = list[idx]
            val bitmap = try {
                context.contentResolver.openInputStream(page.uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) {
                null
            }

            if (bitmap == null) {
                ocrResultText = "Unable to load page bitmap data."
                isProcessing = false
                return@launch
            }

            // Downscale to 1024 max dimensions to keep network fast, high performance
            val maxDim = 1024
            val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else {
                bitmap
            }

            val cleanedText = geminiService.executeOcrCleanup(scaledBitmap)
            ocrResultText = cleanedText
            
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            bitmap.recycle()

            isProcessing = false
            onSuccess()
        }
    }

    // Docs AI Functions (Summarize, Rewrite, Translate)
    fun runSummarize(text: String) {
        viewModelScope.launch {
            isProcessing = true
            processingStatusWord = "AI Summarizing..."
            val result = geminiService.executeSummary(text)
            aiAssistantResultText = result
            isProcessing = false
        }
    }

    fun runTranslate(text: String, lang: String) {
        viewModelScope.launch {
            isProcessing = true
            processingStatusWord = "AI Translating..."
            val result = geminiService.executeTranslate(text, lang)
            aiAssistantResultText = result
            isProcessing = false
        }
    }

    fun runRewrite(text: String, style: String) {
        viewModelScope.launch {
            isProcessing = true
            processingStatusWord = "AI Rewriting..."
            val result = geminiService.executeRewrite(text, style)
            aiAssistantResultText = result
            isProcessing = false
        }
    }

    // Compile into final PDF
    fun compilePdf(context: Context, pdfTitle: String, onFinished: (File) -> Unit, onError: (String) -> Unit) {
        val list = _selectedPages.value
        if (list.isEmpty()) {
            onError("Cannot compile: No pages selected.")
            return
        }

        viewModelScope.launch {
            isProcessing = true
            processingStatusWord = "Compiling PDF Pages, rendering canvas overlays..."
            try {
                val file = PDFCompiler.compile(context, list, pdfTitle)
                loadHistory(context)
                isProcessing = false
                onFinished(file)
            } catch (e: Exception) {
                isProcessing = false
                onError(e.localizedMessage ?: e.message ?: "Failed compiling PDF")
            }
        }
    }

    // Helper formatting structures
    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.US, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    private fun getPdfPageCountApprox(file: File): Int {
        // Since we can't fully parse a pdf binary structure easily without huge libraries,
        // and we compile standard PDFs with A4 headers:
        // We can use a simple trick of scanning for occurrences of "/Type /Page" or "/Page" blocks,
        // or since we are the ones compiling the documents, we search our document cache lists!
        // To be safe and clean, scanning standard "/Type /Page" is excellent and works flawlessly for most local files!
        return try {
            var count = 0
            file.useLines { lines ->
                for (line in lines) {
                    if (line.contains("/Type /Page") || line.contains("/Type/Page")) {
                        count++
                    }
                }
            }
            if (count == 0) 1 else count
        } catch (e: Exception) {
            1
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) {
                        result = cursor.getString(idx)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    fun handleScannedPdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            isProcessing = true
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                    val filename = "Scanned_${sdf.format(java.util.Date())}.pdf"
                    val dir = java.io.File(context.cacheDir, "compiled_pdfs")
                    if (!dir.exists()) dir.mkdirs()
                    val file = java.io.File(dir, filename)
                    file.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                    inputStream.close()
                    
                    val report = GeneratedPdfReport(
                        file = file,
                        displayName = filename,
                        numPages = getPdfPageCountApprox(file),
                        fileSizeFormatted = formatFileSize(file.length()),
                        dateString = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                    )
                    
                    loadHistory(context)
                    
                    android.widget.Toast.makeText(context, "Scanned PDF Saved!", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Error saving scanned PDF: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                isProcessing = false
            }
        }
    }

    fun processMultiplePdfs(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            isProcessing = true
            try {
                val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                val filename = "Merged_${sdf.format(java.util.Date())}.pdf"
                val dir = java.io.File(context.cacheDir, "compiled_pdfs")
                if (!dir.exists()) dir.mkdirs()
                val file = java.io.File(dir, filename)
                
                com.example.model.PDFService.mergePdfs(context, uris, file)
                
                val report = GeneratedPdfReport(
                    file = file,
                    displayName = filename,
                    numPages = getPdfPageCountApprox(file),
                    fileSizeFormatted = formatFileSize(file.length()),
                    dateString = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                )
                
                loadHistory(context)
                
                android.widget.Toast.makeText(context, "PDFs merged successfully!", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Error merging PDFs: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                isProcessing = false
            }
        }
    }

    fun generateImageToPdfWithRotation(context: android.content.Context, images: List<com.example.ui.ImagePage>) {
        viewModelScope.launch {
            isProcessing = true
            try {
                val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                val filename = "ImageToPDF_${sdf.format(java.util.Date())}.pdf"
                val dir = java.io.File(context.cacheDir, "compiled_pdfs")
                if (!dir.exists()) dir.mkdirs()
                val file = java.io.File(dir, filename)
                com.example.model.PDFService.imagesToPdfRotated(context, images, file)
                val report = com.example.model.GeneratedPdfReport(file = file, displayName = filename, numPages = getPdfPageCountApprox(file), fileSizeFormatted = formatFileSize(file.length()), dateString = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
                loadHistory(context)
                android.widget.Toast.makeText(context, "PDF generated successfully!", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                isProcessing = false
            }
        }
    }

    fun generateImageToPdf(context: android.content.Context, uris: List<android.net.Uri>) {
        viewModelScope.launch {
            isProcessing = true
            try {
                val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                val filename = "ImageToPDF_${sdf.format(java.util.Date())}.pdf"
                val dir = java.io.File(context.cacheDir, "compiled_pdfs")
                if (!dir.exists()) dir.mkdirs()
                val file = java.io.File(dir, filename)
                
                com.example.model.PDFService.imagesToPdf(context, uris, file)
                
                val report = com.example.model.GeneratedPdfReport(
                    file = file,
                    displayName = filename,
                    numPages = getPdfPageCountApprox(file),
                    fileSizeFormatted = formatFileSize(file.length()),
                    dateString = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                )
                
                loadHistory(context)
                android.widget.Toast.makeText(context, "Images converted to PDF successfully!", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Error converting images to PDF: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                isProcessing = false
            }
        }
    }
}
