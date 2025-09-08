// Root build, keeping it minimal. Module-specific configs live in each module.
plugins {
    id("java-library")
}

// Add test configuration for root module
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

// Test task for root module
tasks.test {
    useJUnit()
    testLogging {
        events("passed", "skipped", "failed")
    }
}