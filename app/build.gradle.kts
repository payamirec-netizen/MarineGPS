plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose"); id("com.google.devtools.ksp") }
android {
 namespace = "com.marinegps.app"
 compileSdk = 35
 defaultConfig { applicationId = "com.marinegps.app"; minSdk = 26; targetSdk = 35; versionCode = 17; versionName = "0.8.0" }
 signingConfigs {
  getByName("debug") {
   storeFile = file(System.getenv("MARINE_KEYSTORE_PATH") ?: "${System.getProperty("user.home")}/.android/debug.keystore")
   storePassword = "android"
   keyAlias = "androiddebugkey"
   keyPassword = "android"
  }
 }
 buildTypes { getByName("debug") { signingConfig = signingConfigs.getByName("debug") } }
 buildFeatures { compose = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
kotlin { jvmToolchain(17) }
dependencies {
 testImplementation("org.json:json:20240303")
 implementation("com.google.zxing:core:3.5.3")
 implementation("androidx.core:core:1.15.0")
 implementation("androidx.room:room-runtime:2.6.1")
 implementation("androidx.room:room-ktx:2.6.1")
 ksp("androidx.room:room-compiler:2.6.1")
 testImplementation("junit:junit:4.13.2")
 implementation(platform("androidx.compose:compose-bom:2024.12.01"))
 implementation("androidx.activity:activity-compose:1.10.0")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.foundation:foundation")
 implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
}
