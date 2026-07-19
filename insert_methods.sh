sed -i '$ d' app/src/main/java/com/example/viewmodel/PDFViewModel.kt
cat << 'VM_EOF' >> app/src/main/java/com/example/viewmodel/PDFViewModel.kt

    fun handleScannedPdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            isProcessing = true
            loadingMessage = "Saving scanned PDF..."
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                    val filename = "Scanned_${sdf.format(java.util.Date())}.pdf"
                    val file = File(context.filesDir, filename)
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
                    
                    val db = AppDatabase.getDatabase(context)
                    db.pdfHistoryDao().insertHistory(report)
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
            loadingMessage = "Merging PDFs..."
            try {
                val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                val filename = "Merged_${sdf.format(java.util.Date())}.pdf"
                val file = File(context.filesDir, filename)
                
                com.example.model.PDFService.mergePdfs(context, uris, file)
                
                val report = GeneratedPdfReport(
                    file = file,
                    displayName = filename,
                    numPages = getPdfPageCountApprox(file),
                    fileSizeFormatted = formatFileSize(file.length()),
                    dateString = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                )
                
                val db = AppDatabase.getDatabase(context)
                db.pdfHistoryDao().insertHistory(report)
                loadHistory(context)
                
                android.widget.Toast.makeText(context, "PDFs merged successfully!", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Error merging PDFs: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                isProcessing = false
            }
        }
    }
}
VM_EOF
