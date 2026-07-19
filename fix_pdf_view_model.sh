# Remove the stray closing bracket at line 981 (or wherever the class ended before the append)
sed -i '/^}$/d' app/src/main/java/com/example/viewmodel/PDFViewModel.kt
# Add a single closing bracket at the end
echo "}" >> app/src/main/java/com/example/viewmodel/PDFViewModel.kt
