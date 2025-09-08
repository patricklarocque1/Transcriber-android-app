#!/usr/bin/env bash
# Android Environment Diagnostic Script for CI/CD
# Prints comprehensive Android SDK/NDK/CMake environment information

set -euo pipefail

echo "=============================================="
echo "Android Environment Diagnostic Report"
echo "=============================================="

# Basic environment variables
echo ""
echo "=== Environment Variables ==="
echo "ANDROID_HOME: ${ANDROID_HOME:-'Not set'}"
echo "ANDROID_SDK_ROOT: ${ANDROID_SDK_ROOT:-'Not set'}"
echo "ANDROID_NDK_HOME: ${ANDROID_NDK_HOME:-'Not set'}"
echo "JAVA_HOME: ${JAVA_HOME:-'Not set'}"
echo "PATH: ${PATH}"

# Java version and configuration
echo ""
echo "=== Java Configuration ==="
if command -v java &> /dev/null; then
    java -version
    echo "Java executable: $(which java)"
else
    echo "❌ Java not found in PATH"
fi

if command -v javac &> /dev/null; then
    javac -version
    echo "Javac executable: $(which javac)"
else
    echo "❌ Javac not found in PATH"
fi

# Gradle version and configuration
echo ""
echo "=== Gradle Configuration ==="
if [ -f "./gradlew" ]; then
    echo "Gradle wrapper found, checking version..."
    ./gradlew --version || echo "❌ Failed to get Gradle version"
else
    echo "❌ Gradle wrapper not found"
fi

# Android SDK information
echo ""
echo "=== Android SDK Information ==="
if [ -n "${ANDROID_HOME:-}" ] && [ -d "$ANDROID_HOME" ]; then
    echo "✅ Android SDK found at: $ANDROID_HOME"
    echo "SDK directory contents:"
    ls -la "$ANDROID_HOME" 2>/dev/null || echo "❌ Cannot list SDK directory"
    
    # Platform tools
    if [ -d "$ANDROID_HOME/platform-tools" ]; then
        echo "✅ Platform tools found"
        echo "ADB version:"
        "$ANDROID_HOME/platform-tools/adb" version 2>/dev/null || echo "❌ ADB not working"
    else
        echo "❌ Platform tools not found"
    fi
    
    # Build tools
    if [ -d "$ANDROID_HOME/build-tools" ]; then
        echo "✅ Build tools found"
        echo "Available build tools versions:"
        ls -1 "$ANDROID_HOME/build-tools" 2>/dev/null || echo "❌ Cannot list build tools"
    else
        echo "❌ Build tools not found"
    fi
    
    # Platforms
    if [ -d "$ANDROID_HOME/platforms" ]; then
        echo "✅ Platforms found"
        echo "Available platforms:"
        ls -1 "$ANDROID_HOME/platforms" 2>/dev/null || echo "❌ Cannot list platforms"
    else
        echo "❌ Platforms not found"
    fi
    
else
    echo "❌ Android SDK not found or ANDROID_HOME not set"
fi

# Android NDK information
echo ""
echo "=== Android NDK Information ==="
if [ -n "${ANDROID_NDK_HOME:-}" ] && [ -d "$ANDROID_NDK_HOME" ]; then
    echo "✅ Android NDK found at: $ANDROID_NDK_HOME"
    
    # NDK version
    if [ -f "$ANDROID_NDK_HOME/source.properties" ]; then
        echo "NDK version info:"
        cat "$ANDROID_NDK_HOME/source.properties" | grep "Pkg.Revision" || echo "❌ Cannot read NDK version"
    fi
    
    # NDK toolchains
    if [ -d "$ANDROID_NDK_HOME/toolchains" ]; then
        echo "Available NDK toolchains:"
        ls -1 "$ANDROID_NDK_HOME/toolchains" 2>/dev/null | head -10 || echo "❌ Cannot list toolchains"
    fi
    
    # NDK build tools
    if [ -f "$ANDROID_NDK_HOME/ndk-build" ]; then
        echo "✅ ndk-build found"
        "$ANDROID_NDK_HOME/ndk-build" -version 2>/dev/null || echo "❌ ndk-build not working"
    else
        echo "❌ ndk-build not found"
    fi
    
elif [ -n "${ANDROID_HOME:-}" ] && [ -d "$ANDROID_HOME/ndk" ]; then
    echo "NDK found in SDK directory:"
    ls -1 "$ANDROID_HOME/ndk" 2>/dev/null || echo "❌ Cannot list NDK versions"
else
    echo "❌ Android NDK not found"
fi

# CMake information
echo ""
echo "=== CMake Information ==="
if command -v cmake &> /dev/null; then
    echo "✅ CMake found"
    cmake --version
    echo "CMake executable: $(which cmake)"
else
    echo "❌ CMake not found in PATH"
    
    # Check if CMake is in Android SDK
    if [ -n "${ANDROID_HOME:-}" ] && [ -d "$ANDROID_HOME/cmake" ]; then
        echo "CMake versions in Android SDK:"
        ls -1 "$ANDROID_HOME/cmake" 2>/dev/null || echo "❌ Cannot list CMake versions"
        
        # Try to use SDK CMake
        CMAKE_VERSIONS=($(ls -1 "$ANDROID_HOME/cmake" 2>/dev/null | sort -V))
        if [ ${#CMAKE_VERSIONS[@]} -gt 0 ]; then
            LATEST_CMAKE="$ANDROID_HOME/cmake/${CMAKE_VERSIONS[-1]}/bin/cmake"
            if [ -f "$LATEST_CMAKE" ]; then
                echo "Using SDK CMake: $LATEST_CMAKE"
                "$LATEST_CMAKE" --version || echo "❌ SDK CMake not working"
            fi
        fi
    fi
fi

# SDK Manager information
echo ""
echo "=== SDK Manager Information ==="
if [ -n "${ANDROID_HOME:-}" ] && [ -f "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
    echo "✅ SDK Manager found"
    SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
elif [ -n "${ANDROID_HOME:-}" ] && [ -f "$ANDROID_HOME/tools/bin/sdkmanager" ]; then
    echo "✅ SDK Manager found (legacy location)"
    SDKMANAGER="$ANDROID_HOME/tools/bin/sdkmanager"
else
    echo "❌ SDK Manager not found"
    SDKMANAGER=""
fi

if [ -n "$SDKMANAGER" ]; then
    echo "Installed packages (first 20):"
    "$SDKMANAGER" --list_installed 2>/dev/null | head -20 || echo "❌ Cannot list installed packages"
fi

# Gradle project information
echo ""
echo "=== Gradle Project Information ==="
if [ -f "./gradlew" ]; then
    echo "Gradle projects:"
    ./gradlew projects 2>/dev/null || echo "❌ Cannot list Gradle projects"
    
    echo ""
    echo "Key Gradle tasks (first 30):"
    ./gradlew tasks 2>/dev/null | head -30 || echo "❌ Cannot list Gradle tasks"
else
    echo "❌ Gradle wrapper not found"
fi

# System information
echo ""
echo "=== System Information ==="
echo "Operating System: $(uname -a)"
echo "Available disk space:"
df -h . 2>/dev/null || echo "❌ Cannot check disk space"
echo "Available memory:"
free -h 2>/dev/null || echo "❌ Cannot check memory"

# File permissions check
echo ""
echo "=== File Permissions Check ==="
echo "Gradle wrapper permissions:"
ls -la ./gradlew 2>/dev/null || echo "❌ gradlew not found"

if [ -n "${ANDROID_HOME:-}" ]; then
    echo "Android SDK permissions:"
    ls -la "$ANDROID_HOME" 2>/dev/null | head -5 || echo "❌ Cannot check SDK permissions"
fi

echo ""
echo "=============================================="
echo "Android Environment Diagnostic Complete"
echo "=============================================="