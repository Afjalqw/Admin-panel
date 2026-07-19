sed -i '88c\
        val parsedTheme = try { ThemeMode.valueOf(savedTheme) } catch(e: Exception) { ThemeMode.SYSTEM }\
        _themeMode.value = parsedTheme' app/src/main/java/com/example/viewmodel/PDFViewModel.kt
