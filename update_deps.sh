sed -i 's/play-services-document-scanner/play-services-mlkit-document-scanner/g' gradle/libs.versions.toml
sed -i 's/playServicesDocumentScanner = "16.0.0-beta1"/playServicesDocumentScanner = "16.0.0"/g' gradle/libs.versions.toml
