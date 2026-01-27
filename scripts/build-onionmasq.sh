#!/bin/bash
#
# Build OnionMasq from source for Android
#
# This script clones OnionMasq from the Tor Project GitLab,
# builds it for all Android architectures, and copies the
# resulting .so files to the jniLibs directory.
#
# FOSS Compliance: This ensures we build from source rather
# than using pre-built binaries from Maven.
#
# Usage: ./scripts/build-onionmasq.sh
#

set -e

# Configuration
ONIONMASQ_VERSION="${ONIONMASQ_VERSION:-v1.4.0Beta}"  # Pin to specific release
ONIONMASQ_REPO="https://gitlab.torproject.org/tpo/core/onionmasq.git"
BUILD_DIR=".onionmasq-build"
OUTPUT_DIR="app/src/main/jniLibs"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Building OnionMasq $ONIONMASQ_VERSION from source ===${NC}"

# Check for required tools
check_requirements() {
    echo "Checking requirements..."

    if ! command -v rustc &> /dev/null; then
        echo -e "${RED}Error: Rust is not installed. Please install Rust first:${NC}"
        echo "  curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh"
        exit 1
    fi

    if ! command -v cargo &> /dev/null; then
        echo -e "${RED}Error: Cargo is not installed.${NC}"
        exit 1
    fi

    if ! command -v cargo-ndk &> /dev/null; then
        echo -e "${YELLOW}Installing cargo-ndk...${NC}"
        cargo install cargo-ndk
    fi

    # Check for Android NDK
    if [ -z "$ANDROID_NDK_HOME" ] && [ -z "$NDK_HOME" ]; then
        # Try common locations
        if [ -d "$HOME/Android/Sdk/ndk" ]; then
            export ANDROID_NDK_HOME=$(ls -d "$HOME/Android/Sdk/ndk"/* 2>/dev/null | sort -V | tail -1)
        elif [ -d "$HOME/Library/Android/sdk/ndk" ]; then
            export ANDROID_NDK_HOME=$(ls -d "$HOME/Library/Android/sdk/ndk"/* 2>/dev/null | sort -V | tail -1)
        fi
    fi

    if [ -z "$ANDROID_NDK_HOME" ] && [ -z "$NDK_HOME" ]; then
        echo -e "${RED}Error: Android NDK not found. Please set ANDROID_NDK_HOME or NDK_HOME.${NC}"
        exit 1
    fi

    echo -e "${GREEN}Requirements satisfied.${NC}"
}

# Add Android targets to Rust
setup_rust_targets() {
    echo "Setting up Rust targets for Android..."
    rustup target add aarch64-linux-android
    rustup target add armv7-linux-androideabi
    rustup target add i686-linux-android
    rustup target add x86_64-linux-android
    echo -e "${GREEN}Rust targets configured.${NC}"
}

# Clone OnionMasq
clone_onionmasq() {
    echo "Cloning OnionMasq $ONIONMASQ_VERSION..."

    # Clean previous build
    rm -rf "$BUILD_DIR"

    # Clone specific version (shallow, fast)
    if [[ "$ONIONMASQ_VERSION" == "main" ]]; then
        git clone --depth 1 "$ONIONMASQ_REPO" "$BUILD_DIR"
    else
        git clone --depth 1 --branch "$ONIONMASQ_VERSION" "$ONIONMASQ_REPO" "$BUILD_DIR" 2>/dev/null || \
        git clone --depth 1 "$ONIONMASQ_REPO" "$BUILD_DIR"
    fi

    echo -e "${GREEN}OnionMasq cloned successfully.${NC}"
}

# Build OnionMasq for Android
build_onionmasq() {
    echo "Building OnionMasq for Android architectures..."

    cd "$BUILD_DIR"

    # Create output directories
    mkdir -p "../$OUTPUT_DIR/arm64-v8a"
    mkdir -p "../$OUTPUT_DIR/armeabi-v7a"
    mkdir -p "../$OUTPUT_DIR/x86"
    mkdir -p "../$OUTPUT_DIR/x86_64"

    # Build for all Android architectures
    # Note: cargo-ndk handles the cross-compilation
    cargo ndk \
        -t arm64-v8a \
        -t armeabi-v7a \
        -t x86 \
        -t x86_64 \
        -o "../$OUTPUT_DIR" \
        build --release

    cd ..

    echo -e "${GREEN}OnionMasq built successfully.${NC}"
}

# Verify build outputs
verify_outputs() {
    echo "Verifying build outputs..."

    local missing=0

    for arch in arm64-v8a armeabi-v7a x86 x86_64; do
        if [ -f "$OUTPUT_DIR/$arch/libonionmasq.so" ]; then
            local size=$(ls -lh "$OUTPUT_DIR/$arch/libonionmasq.so" | awk '{print $5}')
            echo -e "  ${GREEN}$arch: libonionmasq.so ($size)${NC}"
        else
            echo -e "  ${RED}$arch: MISSING${NC}"
            missing=1
        fi
    done

    if [ $missing -eq 1 ]; then
        echo -e "${RED}Error: Some architectures failed to build.${NC}"
        exit 1
    fi

    echo -e "${GREEN}All architectures built successfully.${NC}"
}

# Cleanup build directory
cleanup() {
    echo "Cleaning up build directory..."
    rm -rf "$BUILD_DIR"
    echo -e "${GREEN}Cleanup complete.${NC}"
}

# Main execution
main() {
    # Navigate to project root
    cd "$(dirname "$0")/.."

    check_requirements
    setup_rust_targets
    clone_onionmasq
    build_onionmasq
    verify_outputs
    cleanup

    echo ""
    echo -e "${GREEN}=== OnionMasq build complete ===${NC}"
    echo "Built libraries are in: $OUTPUT_DIR/"
    echo ""
    echo "To update OnionMasq version, set ONIONMASQ_VERSION environment variable:"
    echo "  ONIONMASQ_VERSION=v1.5.0 ./scripts/build-onionmasq.sh"
}

main "$@"
