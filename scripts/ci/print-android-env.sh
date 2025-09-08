#!/usr/bin/env bash
set -euo pipefail

echo "Android SDK: ${ANDROID_HOME:-not set}"
if [ -d "${ANDROID_HOME:-}" ]; then
  echo "Build-tools: $(ls "${ANDROID_HOME}/build-tools" 2>/dev/null | tr '\n' ' ' || echo none)"
  if [ -d "${ANDROID_HOME}/ndk" ]; then
    echo "NDK: $(ls "${ANDROID_HOME}/ndk" 2>/dev/null | tr '\n' ' ')"
  else
    echo "NDK: none"
  fi
  if [ -d "${ANDROID_HOME}/cmake" ]; then
    echo "CMake: $(ls "${ANDROID_HOME}/cmake" 2>/dev/null | tr '\n' ' ')"
  else
    echo "CMake: none"
  fi
else
  echo "SDK directory not found"
fi

