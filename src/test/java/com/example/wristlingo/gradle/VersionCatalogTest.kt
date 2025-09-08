package com.example.wristlingo.gradle

import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.util.Properties

/**
 * Test to validate the Gradle version catalog and project configuration.
 * These tests ensure that the build configuration is consistent and valid.
 */
class VersionCatalogTest {

    @Test
    fun `version catalog file exists`() {
        val catalogFile = File("gradle/libs.versions.toml")
        assertTrue("Version catalog file should exist", catalogFile.exists())
    }

    @Test
    fun `version catalog has required versions`() {
        val catalogFile = File("gradle/libs.versions.toml")
        val content = catalogFile.readText()
        
        // Check for essential version declarations
        assertTrue("Should declare AGP version", content.contains("agp ="))
        assertTrue("Should declare Kotlin version", content.contains("kotlin ="))
        assertTrue("Should declare Compose BOM version", content.contains("compose-bom ="))
        assertTrue("Should declare Room version", content.contains("room ="))
        assertTrue("Should declare Wear Compose version", content.contains("wear-compose ="))
    }

    @Test
    fun `version catalog has required libraries`() {
        val catalogFile = File("gradle/libs.versions.toml")
        val content = catalogFile.readText()
        
        // Check for essential library declarations
        assertTrue("Should declare Compose UI", content.contains("compose-ui ="))
        assertTrue("Should declare Room runtime", content.contains("androidx-room-runtime ="))
        assertTrue("Should declare Room KTX", content.contains("androidx-room-ktx ="))
        assertTrue("Should declare Room compiler", content.contains("androidx-room-compiler ="))
        assertTrue("Should declare Wear services", content.contains("play-services-wearable ="))
        assertTrue("Should declare ML Kit translate", content.contains("mlkit-translate ="))
    }

    @Test
    fun `version catalog has required plugins`() {
        val catalogFile = File("gradle/libs.versions.toml")
        val content = catalogFile.readText()
        
        // Check for essential plugin declarations
        assertTrue("Should declare Android application plugin", content.contains("android-application ="))
        assertTrue("Should declare Kotlin Android plugin", content.contains("kotlin-android ="))
        assertTrue("Should declare Kotlin Compose plugin", content.contains("kotlin-compose ="))
        assertTrue("Should declare KSP plugin", content.contains("ksp ="))
    }

    @Test
    fun `gradle properties are valid`() {
        val gradlePropsFile = File("gradle.properties")
        if (gradlePropsFile.exists()) {
            val props = Properties()
            props.load(gradlePropsFile.inputStream())
            
            // Validate common Gradle properties if they exist
            val kotlinCodeStyle = props.getProperty("kotlin.code.style")
            if (kotlinCodeStyle != null) {
                assertTrue("Kotlin code style should be 'official'", 
                    kotlinCodeStyle == "official")
            }
            
            val androidUseAndroidX = props.getProperty("android.useAndroidX")
            if (androidUseAndroidX != null) {
                assertTrue("Should use AndroidX", androidUseAndroidX.toBoolean())
            }
        }
    }

    @Test
    fun `settings gradle file exists and is valid`() {
        val settingsFile = File("settings.gradle.kts")
        assertTrue("Settings file should exist", settingsFile.exists())
        
        val content = settingsFile.readText()
        assertTrue("Should include app module", content.contains(":app"))
        assertTrue("Should include wear module", content.contains(":wear"))
    }

    @Test
    fun `build gradle files exist for all modules`() {
        val rootBuildFile = File("build.gradle.kts")
        val appBuildFile = File("app/build.gradle.kts")
        val wearBuildFile = File("wear/build.gradle.kts")
        
        assertTrue("Root build file should exist", rootBuildFile.exists())
        assertTrue("App build file should exist", appBuildFile.exists())
        assertTrue("Wear build file should exist", wearBuildFile.exists())
    }

    @Test
    fun `product flavors are consistently defined`() {
        val appBuildFile = File("app/build.gradle.kts")
        val wearBuildFile = File("wear/build.gradle.kts")
        
        val appContent = appBuildFile.readText()
        val wearContent = wearBuildFile.readText()
        
        // Check that both modules define the same flavors
        val flavors = listOf("offline", "hybrid", "cloudstt")
        
        for (flavor in flavors) {
            assertTrue("App module should have $flavor flavor", 
                appContent.contains("create(\"$flavor\")"))
            assertTrue("Wear module should have $flavor flavor", 
                wearContent.contains("create(\"$flavor\")"))
        }
    }

    @Test
    fun `compile and target sdk versions are consistent`() {
        val appBuildFile = File("app/build.gradle.kts")
        val wearBuildFile = File("wear/build.gradle.kts")
        
        val appContent = appBuildFile.readText()
        val wearContent = wearBuildFile.readText()
        
        // Both should use SDK 35
        assertTrue("App should use compileSdk 35", appContent.contains("compileSdk = 35"))
        assertTrue("Wear should use compileSdk 35", wearContent.contains("compileSdk = 35"))
        assertTrue("App should target SDK 35", appContent.contains("targetSdk = 35"))
        assertTrue("Wear should target SDK 35", wearContent.contains("targetSdk = 35"))
    }
}