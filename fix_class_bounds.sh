# Remove the stray closing bracket at the end
sed -i '/^}$/d' app/src/main/java/com/example/viewmodel/PDFViewModel.kt
# Find where processMultiplePdfs ends and remove its closing bracket if it was the class one
# Wait, it's easier to just remove the `}` above `fun generateImageToPdf`
sed -i '/}    fun generateImageToPdf/c\    fun generateImageToPdf' app/src/main/java/com/example/viewmodel/PDFViewModel.kt
# Alternatively, I can just rewrite the end of the file.
