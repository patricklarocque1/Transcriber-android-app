import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

// Optional release signing driven by keystore.properties (not committed)
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
  if (keystorePropertiesFile.exists()) {
    load(keystorePropertiesFile.inputStream())
  }
}
val hasKeystore = keystorePropertiesFile.exists()

android {
  namespace = "com.example.wristlingo.wear"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.example.wristlingo.wear"
    minSdk = 30
    targetSdk = 35
    versionCode = 1
    versionName = "0.1.0"
  }

  signingConfigs {
    if (hasKeystore) {
      create("release") {
        storeFile = file(keystoreProperties["storeFile"] as String)
        storePassword = keystoreProperties["storePassword"] as String
        keyAlias = keystoreProperties["keyAlias"] as String
        keyPassword = keystoreProperties["keyPassword"] as String
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        file("proguard-rules.pro")
      )
      if (hasKeystore) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
  }

  flavorDimensions += listOf("mode")
  productFlavors {
    create("offline") { applicationIdSuffix = ".offline" }
    create("hybrid") { applicationIdSuffix = ".hybrid" }
    create("cloudstt") { applicationIdSuffix = ".cloudstt" }
  }

  buildFeatures { compose = true }
  packaging.resources.excludes += listOf("META-INF/AL2.0", "META-INF/LGPL2.1")

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

dependencies {
  implementation(platform(libs.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.compose.ui)
  implementation(libs.compose.ui.tooling.preview)
  implementation(libs.wear.compose.material)
  implementation(libs.wear.compose.foundation)
  debugImplementation(libs.compose.ui.tooling)
}

kotlin {
  jvmToolchain(17)
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
  }
}
