plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.example.wristlingo"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.example.wristlingo"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "0.1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        file("proguard-rules.pro")
      )
    }
  }

  flavorDimensions += listOf("mode")
  productFlavors {
    create("offline") { applicationIdSuffix = ".offline" }
    create("hybrid") { applicationIdSuffix = ".hybrid" }
    create("cloudstt") { applicationIdSuffix = ".cloudstt" }
  }

  buildFeatures { compose = true }

  packaging.resources.excludes += 
    listOf("META-INF/AL2.0", "META-INF/LGPL2.1")
}

dependencies {
  implementation(platform(libs.compose.bom))
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.compose.ui)
  implementation(libs.compose.ui.tooling.preview)
  implementation(libs.compose.material3)

  androidTestImplementation(platform(libs.compose.bom))
  androidTestImplementation(libs.compose.ui.test.junit4)
  debugImplementation(libs.compose.ui.tooling)
  debugImplementation(libs.compose.ui.test.manifest)
}

