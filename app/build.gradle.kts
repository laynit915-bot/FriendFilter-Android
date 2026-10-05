plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "com.local.friendfilter"
    compileSdk = 35
    defaultConfig { applicationId = "com.local.friendfilter"; minSdk = 26; targetSdk = 35; versionCode = 3; versionName = "0.3.0" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
