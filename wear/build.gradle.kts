import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  kotlin("android")
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

  // Wearable Data Layer
  implementation(libs.play.services.wearable)

  // Testing dependencies
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.mockito:mockito-core:5.8.0")
  testImplementation("org.mockito:mockito-kotlin:5.2.1")
  testImplementation("org.robolectric:robolectric:4.11.1")
  testImplementation("androidx.test:core:1.5.0")
  testImplementation("androidx.compose.ui:ui-test-junit4:1.5.8")
  androidTestImplementation("androidx.test.ext:junit:1.1.5")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
  androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.8")
  debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.8")
}

kotlin {
  jvmToolchain(17)
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
  }
}
