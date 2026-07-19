sed -i 's/val file = File(context.filesDir, filename)/val dir = java.io.File(context.cacheDir, "compiled_pdfs")\n                    if (!dir.exists()) dir.mkdirs()\n                    val file = java.io.File(dir, filename)/g' app/src/main/java/com/example/viewmodel/PDFViewModel.kt
sed -i '/val db = AppDatabase.getDatabase(context)/d' app/src/main/java/com/example/viewmodel/PDFViewModel.kt
sed -i '/db.pdfHistoryDao().insertHistory(report)/d' app/src/main/java/com/example/viewmodel/PDFViewModel.kt
sed -i '/loadingMessage = /d' app/src/main/java/com/example/viewmodel/PDFViewModel.kt

sed -i 's/getStartScanIntent(this@MainActivity)/getStartScanIntent(context as android.app.Activity)/g' app/src/main/java/com/example/MainActivity.kt
