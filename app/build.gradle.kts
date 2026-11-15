plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
}
// Build and local UX remain available without a Firebase project;
// FCM is activated only when a real google-services.json is provisioned.
if (file("google-services.json").exists()) {
 apply(plugin = "com.google.gms.google-services")
}
android {
 namespace = "de.einsatzpulse.alarm"
 compileSdk = 35
 defaultConfig {
  applicationId = "de.einsatzpulse.alarm"
  minSdk = 26
  targetSdk = 35
  versionCode = 1
  versionName = "0.1.0-beta.1"
 }
 buildTypes {
  release { isMinifyEnabled = false }
 }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
 kotlinOptions { jvmTarget = "17" }
 buildFeatures { compose = true; buildConfig = true }
}
dependencies {
 implementation("androidx.core:core-ktx:1.16.0")
 implementation("androidx.activity:activity-compose:1.10.1")
 implementation(platform("androidx.compose:compose-bom:2025.04.01"))
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-extended")
 implementation("androidx.compose.foundation:foundation")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
 implementation("com.squareup.okhttp3:okhttp:4.12.0")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
 implementation("com.google.firebase:firebase-messaging:24.1.1")
 testImplementation("junit:junit:4.13.2")
 testImplementation("org.json:json:20240303")
}
