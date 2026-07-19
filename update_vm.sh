cat << 'VM_EOF' >> app/src/main/java/com/example/viewmodel/PDFViewModel.kt

    fun handleScannedPdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            isProcessing = true
            loadingMessage = "Saving scanned PDF..."
            try {
                // For simplicity, we just copy the scanned PDF to our history
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    val filename = "Scanned_${sdf.format(Date())}.pdf"
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
                        dateString = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())
                    )
                    
                    val db = AppDatabase.getDatabase(context)
                    db.pdfHistoryDao().insertHistory(report)
                    loadHistory(context)
                    
                    Toast.makeText(context, "Scanned PDF Saved!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving scanned PDF: ${e.message}", Toast.LENGTH_SHORT).show()
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
                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val filename = "Merged_${sdf.format(Date())}.pdf"
                val file = File(context.filesDir, filename)
                
                com.example.model.PDFService.mergePdfs(context, uris, file)
                
                val report = GeneratedPdfReport(
                    file = file,
                    displayName = filename,
                    numPages = getPdfPageCountApprox(file),
                    fileSizeFormatted = formatFileSize(file.length()),
                    dateString = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())
                )
                
                val db = AppDatabase.getDatabase(context)
                db.pdfHistoryDao().insertHistory(report)
                loadHistory(context)
                
                Toast.makeText(context, "PDFs merged successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error merging PDFs: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isProcessing = false
            }
        }
    }
VM_EOF
bash update_vm.sh