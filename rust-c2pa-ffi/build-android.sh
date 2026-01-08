#!/bin/bash

# Build script for C2PA Rust FFI for Android
# This script compiles the Rust library for all Android architectures
# and copies the .so files to the correct jniLibs directory

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Building C2PA Rust FFI for Android${NC}"
echo ""

# Check if Rust is installed
if ! command -v cargo &> /dev/null; then
    echo -e "${RED}Error: Rust/Cargo not found${NC}"
    echo "Install Rust from: https://rustup.rs/"
    exit 1
fi

# Check if Android NDK is configured
if [ ! -f ".cargo/config.toml" ]; then
    echo -e "${YELLOW}Warning: .cargo/config.toml not found${NC}"
    echo "Make sure you've configured the Android NDK paths"
fi

# Get the script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( dirname "$SCRIPT_DIR" )"
JNILIBS_DIR="$PROJECT_ROOT/app/src/main/jniLibs"

echo "Project root: $PROJECT_ROOT"
echo "JNI libs dir: $JNILIBS_DIR"
echo ""

# Android target architectures (compatible with Bash 3.2+)
# Using simple arrays instead of associative arrays for macOS compatibility
RUST_TARGETS=("aarch64-linux-android" "armv7-linux-androideabi" "i686-linux-android" "x86_64-linux-android")
ANDROID_ABIS=("arm64-v8a" "armeabi-v7a" "x86" "x86_64")

# Environment variables for each target
export CC_aarch64_linux_android="$HOME/Library/Android/sdk/ndk/27.1.12297006/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android30-clang"
export AR_aarch64_linux_android="$HOME/Library/Android/sdk/ndk/27.1.12297006/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-ar"

export CC_armv7_linux_androideabi="$HOME/Library/Android/sdk/ndk/27.1.12297006/toolchains/llvm/prebuilt/darwin-x86_64/bin/armv7a-linux-androideabi30-clang"
export AR_armv7_linux_androideabi="$HOME/Library/Android/sdk/ndk/27.1.12297006/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-ar"

export CC_i686_linux_android="$HOME/Library/Android/sdk/ndk/27.1.12297006/toolchains/llvm/prebuilt/darwin-x86_64/bin/i686-linux-android30-clang"
export AR_i686_linux_android="$HOME/Library/Android/sdk/ndk/27.1.12297006/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-ar"

export CC_x86_64_linux_android="$HOME/Library/Android/sdk/ndk/27.1.12297006/toolchains/llvm/prebuilt/darwin-x86_64/bin/x86_64-linux-android30-clang"
export AR_x86_64_linux_android="$HOME/Library/Android/sdk/ndk/27.1.12297006/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-ar"

# Build for each target
for i in "${!RUST_TARGETS[@]}"; do
    TARGET="${RUST_TARGETS[$i]}"
    ABI="${ANDROID_ABIS[$i]}"
    echo -e "${YELLOW}Building for $TARGET ($ABI)...${NC}"

    cd "$SCRIPT_DIR"
    cargo build --target "$TARGET" --release

    # Create jniLibs directory structure
    mkdir -p "$JNILIBS_DIR/$ABI"

    # Copy the .so file
    cp "target/$TARGET/release/libc2pa_ffi.so" "$JNILIBS_DIR/$ABI/"

    # Get file size
    SIZE=$(du -h "$JNILIBS_DIR/$ABI/libc2pa_ffi.so" | cut -f1)
    echo -e "${GREEN}✓ Built $ABI ($SIZE)${NC}"
    echo ""
done

echo -e "${GREEN}Build complete!${NC}"
echo ""
echo "Shared libraries copied to:"
echo "  $JNILIBS_DIR"
echo ""
echo "You can now build your Android app with:"
echo "  ./gradlew assembleFossDevDebug"
