#!/usr/bin/env bash
set -e

# ==========================================
# CONFIGURATION & VARIANTS (PURE JAVA BUILD)
# ==========================================
BUILD_TYPE="${1:-debug}" # opsi: debug | release

# Auto-detect Android SDK directory
SDK_DIR="${ANDROID_SDK_ROOT:-/opt/android/sdk}"
BUILD_TOOLS_DIR="$SDK_DIR/build-tools/36.0.0"
if [ ! -d "$BUILD_TOOLS_DIR" ]; then
    BUILD_TOOLS_DIR=$(ls -d $SDK_DIR/build-tools/* 2>/dev/null | tail -n 1)
fi

ANDROID_JAR_PATH="$SDK_DIR/platforms/android-34/android.jar"
if [ ! -f "$ANDROID_JAR_PATH" ]; then
    ANDROID_JAR_PATH=$(ls $SDK_DIR/platforms/android-*/android.jar 2>/dev/null | tail -n 1)
fi

AAPT2="$BUILD_TOOLS_DIR/aapt2"
D8_BIN="$BUILD_TOOLS_DIR/d8"
APKSIGNER="$BUILD_TOOLS_DIR/apksigner"
APKSIGNER_JAR="$BUILD_TOOLS_DIR/lib/apksigner.jar"

PROJ_DIR="$(pwd)"
BUILD_DIR="$PROJ_DIR/build_manual"
OUT_APK="$PROJ_DIR/KeyboardHemat-${BUILD_TYPE}-manual.apk"

echo "=========================================="
echo " Building Pure Java APK [Variant: ${BUILD_TYPE^^}] "
echo "=========================================="
echo "SDK Directory : $SDK_DIR"
echo "Build Tools   : $BUILD_TOOLS_DIR"
echo "Android JAR   : $ANDROID_JAR_PATH"
echo "=========================================="

# 0. Setup Folder Structure
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/compiled" "$BUILD_DIR/gen" "$BUILD_DIR/obj" "$BUILD_DIR/dex"

# 1. Resource Compilation (AAPT2)
echo "[1/5] Compiling and linking resources with AAPT2..."
$AAPT2 compile --dir "$PROJ_DIR/app/src/main/res" -o "$BUILD_DIR/compiled/"

$AAPT2 link -I "$ANDROID_JAR_PATH" \
    --manifest "$PROJ_DIR/app/src/main/AndroidManifest.xml" \
    --min-sdk-version 30 \
    --target-sdk-version 34 \
    --version-code 1 \
    --version-name 1.0 \
    -o "$BUILD_DIR/app-res.apk" \
    --java "$BUILD_DIR/gen" \
    "$BUILD_DIR/compiled/"*.flat

# 2. Java Compilation (javac)
echo "[2/5] Compiling Pure Java code..."
javac -d "$BUILD_DIR/obj" \
    -classpath "$ANDROID_JAR_PATH" \
    $(find "$PROJ_DIR/app/src/main/java" "$BUILD_DIR/gen" -name "*.java")

# 3. Dexing
echo "[3/5] Processing bytecode with D8..."
D8_RELEASE_FLAG="--debug"
if [ "$BUILD_TYPE" = "release" ]; then
    D8_RELEASE_FLAG="--release"
fi

"$D8_BIN" $D8_RELEASE_FLAG \
    --lib "$ANDROID_JAR_PATH" \
    --output "$BUILD_DIR/dex" \
    $(find "$BUILD_DIR/obj" -name "*.class")

# 4. Packaging
echo "[4/5] Packaging APK..."
cp "$BUILD_DIR/app-res.apk" "$BUILD_DIR/app-unsigned.apk"

# Add dex files to unsigned APK using python zipfile module
python3 -c "
import zipfile, os
apk_path = '$BUILD_DIR/app-unsigned.apk'
dex_dir = '$BUILD_DIR/dex'
with zipfile.ZipFile(apk_path, 'a', zipfile.ZIP_DEFLATED) as z:
    for f in os.listdir(dex_dir):
        if f.endswith('.dex'):
            z.write(os.path.join(dex_dir, f), f)
"

# 5. Signing (Debug / Release Key)
echo "[5/5] Signing APK..."
KEYSTORE="$PROJ_DIR/debug.keystore"
PASS="pass:android"
ALIAS="androiddebugkey"

if [ -x "$APKSIGNER" ]; then
    $APKSIGNER sign \
        --ks "$KEYSTORE" \
        --ks-pass "$PASS" \
        --ks-key-alias "$ALIAS" \
        --out "$OUT_APK" \
        "$BUILD_DIR/app-unsigned.apk"
else
    java -cp "$APKSIGNER_JAR" com.android.apksigner.ApkSignerTool sign \
        --ks "$KEYSTORE" \
        --ks-pass "$PASS" \
        --ks-key-alias "$ALIAS" \
        --out "$OUT_APK" \
        "$BUILD_DIR/app-unsigned.apk"
fi

echo ""
echo "=========================================="
echo " SUCCESS! Output APK: $OUT_APK"
echo "=========================================="
