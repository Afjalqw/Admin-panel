sed -i 's/\/\/ implementation(libs.androidx.datastore.preferences)/implementation(libs.androidx.datastore.preferences)/g' app/build.gradle.kts
sed -i 's/dependencies {/dependencies {\n  implementation(libs.pdfbox.android)\n  implementation(libs.play.services.document.scanner)\n  implementation(libs.play.services.mlkit.text.recognition)/g' app/build.gradle.kts
