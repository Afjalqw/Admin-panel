sed -i 's/Icons.AutoMirrored.Filled.MergeType/Icons.Default.MergeType/g' app/src/main/java/com/example/ui/HomeDashboardScreen.kt
sed -i 's/Icons.AutoMirrored.Filled.CallSplit/Icons.Default.CallSplit/g' app/src/main/java/com/example/ui/HomeDashboardScreen.kt
sed -i 's/Icons.AutoMirrored.Filled.TextSnippet/Icons.Default.TextSnippet/g' app/src/main/java/com/example/ui/HomeDashboardScreen.kt

sed -i '/return/d' app/src/main/java/com/example/MainActivity.kt
