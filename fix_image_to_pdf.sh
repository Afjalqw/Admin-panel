cat << 'INNER_EOF' >> app/src/main/java/com/example/viewmodel/PDFViewModel.kt

    fun generateImageToPdf(context: android.content.Context, uris: List<android.net.Uri>) {
        androidx.lifecycle.viewModelScope.launch {
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
                    numPages = com.example.model.PDFService.getPdfPageCountApprox(file),
                    fileSizeFormatted = com.example.model.PDFService.formatFileSize(file.length()),
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
INNER_EOF
