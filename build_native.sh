#!/bin/bash

set -e

echo "Building native overlay for Standoff 2..."

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NATIVE_SOURCE="$PROJECT_DIR/so2-external-main"
ASSETS_DIR="$PROJECT_DIR/app/src/main/assets"

if [ ! -d "$NATIVE_SOURCE" ]; then
    echo "Extracting sources..."
    cd "$PROJECT_DIR"
    unzip -q so2-external-main.zip
fi

if [ -z "$ANDROID_NDK_HOME" ] && [ -z "$NDK_ROOT" ]; then
    echo "Error: NDK not found. Set ANDROID_NDK_HOME or NDK_ROOT"
    echo "You can still build the APK, native will be built on device if NDK available"
    exit 0
fi

NDK_BUILD="${ANDROID_NDK_HOME:-$NDK_ROOT}/ndk-build"

if [ ! -f "$NDK_BUILD" ]; then
    echo "Warning: ndk-build not found at $NDK_BUILD"
    echo "Continuing without prebuilt binary..."
    exit 0
fi

echo "Using NDK: ${ANDROID_NDK_HOME:-$NDK_ROOT}"

cd "$NATIVE_SOURCE"

echo "Cleaning previous builds..."
$NDK_BUILD clean || true

echo "Building for arm64-v8a..."
$NDK_BUILD NDK_PROJECT_PATH=. NDK_APPLICATION_MK=Application.mk APP_ABI=arm64-v8a

if [ -f "libs/arm64-v8a/standof.sh" ]; then
    echo "Build successful!"
    
    mkdir -p "$ASSETS_DIR"
    cp libs/arm64-v8a/standof.sh "$ASSETS_DIR/"
    
    echo "Prebuilt binary copied to assets"
    ls -lh "$ASSETS_DIR/standof.sh"
else
    echo "Build failed - binary not found"
    exit 1
fi

echo "Native build complete!"
