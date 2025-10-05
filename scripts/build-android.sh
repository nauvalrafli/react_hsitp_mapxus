#!/bin/bash

# Build Android AAR for npm publishing
# This script builds the Android library and copies the AAR to the appropriate location

set -e

echo "🤖 Building Android AAR..."

# Navigate to the example android directory where gradlew is located
cd "$(dirname "$0")/../example/android"

# Build the AAR
echo "📦 Building release AAR..."
./gradlew :react-native-mapxus-hsitp:assembleRelease

# Get the project root directory
PROJECT_ROOT="$(dirname "$(dirname "$(pwd)")")"
AAR_FILE="$PROJECT_ROOT/android/build/outputs/aar/react-native-mapxus-hsitp-release.aar"
LIBS_DIR="$PROJECT_ROOT/android/libs"

# Create libs directory if it doesn't exist
mkdir -p "$LIBS_DIR"

# Copy AAR to libs directory
echo "📋 Copying AAR to libs directory..."
cp "$AAR_FILE" "$LIBS_DIR/"

echo "✅ Android AAR built successfully!"
echo "📍 AAR location: $LIBS_DIR/react-native-mapxus-hsitp-release.aar"
echo "📏 File size: $(du -h "$LIBS_DIR/react-native-mapxus-hsitp-release.aar" | cut -f1)"
