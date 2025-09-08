import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.ksp)
  id("jacoco")
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

  packaging.resources.excludes += 
    listOf("META-INF/AL2.0", "META-INF/LGPL2.1")

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  externalNativeBuild {
    cmake {
      path = file("src/main/cpp/CMakeLists.txt")
    }
  }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.google.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)

  androidTestImplementation(platform(libs.compose.bom))
  androidTestImplementation(libs.compose.ui.test.junit4)
  debugImplementation(libs.compose.ui.tooling)
  debugImplementation(libs.compose.ui.test.manifest)
  
  // Coroutines for simple in-app event bus and service work
  implementation(libs.kotlinx.coroutines.android)

  // Wearable Data Layer
  implementation(libs.play.services.wearable)

  // Room database (KSP)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)
  kspTest(libs.androidx.room.compiler)
  kspAndroidTest(libs.androidx.room.compiler)

  // Preferences DataStore
  implementation(libs.androidx.datastore.preferences)

  // ML Kit: on-device translation
  implementation(libs.mlkit.translate)
  // ML Kit: language identification
  implementation(libs.mlkit.language.id)

  // OkHttp for model downloads
  implementation(libs.okhttp)

  // WorkManager for background tasks
  implementation(libs.androidx.work.runtime.ktx)

  // Testing dependencies
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.mockito:mockito-core:5.8.0")
  testImplementation("org.mockito:mockito-kotlin:5.2.1")
  testImplementation("org.robolectric:robolectric:4.11.1")
  testImplementation("androidx.test:core:1.5.0")
  testImplementation("androidx.room:room-testing:2.6.1")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
  testImplementation("androidx.arch.core:core-testing:2.2.0")
  testImplementation("io.mockk:mockk:1.13.12")
  testImplementation("androidx.compose.ui:ui-test-junit4:1.7.4")
}

kotlin {
  jvmToolchain(17)
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
  }
}

// Jacoco configuration
tasks.withType<Test>().configureEach {
  useJUnit()
  finalizedBy(tasks.named("jacocoTestReport"))
}

jacoco {
  toolVersion = "0.8.10"
}

tasks.register<JacocoReport>("jacocoTestReport") {
  dependsOn(tasks.named("testDebugUnitTest"))
  reports {
    xml.required.set(true)
    html.required.set(true)
  }
  val debugTree = fileTree("${project.buildDir}/intermediates/javac/debug") {
    exclude("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*")
  }
  val kotlinDebugTree = fileTree("${project.buildDir}/tmp/kotlin-classes/debug") {
    exclude("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*")
  }
  classDirectories.setFrom(files(debugTree, kotlinDebugTree))
  sourceDirectories.setFrom(files("src/main/java"))
  executionData.setFrom(fileTree(buildDir) {
    include(
      "jacoco/testDebugUnitTest.exec",
      "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
    )
  })
}

tasks.register("jacocoTestCoverageVerification", JacocoCoverageVerification::class) {
  dependsOn(tasks.named("jacocoTestReport"))
  violationRules {
    rule {
      limit {
        minimum = "0.90".toBigDecimal()
      }
    }
  }
  executionData.setFrom(fileTree(buildDir) {
    include(
      "jacoco/testDebugUnitTest.exec",
      "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
    )
  })
  classDirectories.setFrom(files(
    fileTree("${project.buildDir}/intermediates/javac/debug"),
    fileTree("${project.buildDir}/tmp/kotlin-classes/debug")
  ))
}
