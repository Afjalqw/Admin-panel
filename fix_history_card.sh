sed -i 's/fun HistoryFileCard(report: GeneratedPdfReport, viewModel: PDFViewModel, onDismiss: () -> Unit) {/fun HistoryFileCard(report: GeneratedPdfReport, viewModel: PDFViewModel, onDismiss: () -> Unit) {\n    val context = androidx.compose.ui.platform.LocalContext.current/g' app/src/main/java/com/example/ui/HistoryManagerScreen.kt

sed -i 's/file), "application\/pdf")/report.file), "application\/pdf")/g' app/src/main/java/com/example/ui/HistoryManagerScreen.kt
