sed -i 's/try {/try {\n                val db = AppDatabase.getDatabase(context)/' app/src/main/java/com/example/viewmodel/PDFViewModel.kt
