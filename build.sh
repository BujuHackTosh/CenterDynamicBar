#!/data/data/com.termux/files/usr/bin/bash
set -e
cd "$(dirname "$0")"
for t in javac d8 aapt2 apksigner zip keytool; do
  command -v $t >/dev/null || { echo "MISSING TOOL: $t"; exit 1; }
done
JAR=$(find "$PREFIX" -name android.jar 2>/dev/null | head -1)
[ -z "$JAR" ] && { echo "android.jar NOT FOUND (try: pkg install aapt)"; exit 1; }
echo "Using $JAR"
rm -rf build && mkdir -p build/stubcls build/cls build/dex
echo "[1/6] stubs";   javac -d build/stubcls $(find termux/stubs -name '*.java')
echo "[2/6] compile"; javac -source 17 -target 17 -cp "$JAR:build/stubcls" -d build/cls app/src/main/java/dev/centerbar/Hook.java
echo "[3/6] dex";     d8 --min-api 33 --lib "$JAR" --output build/dex $(find build/cls -name '*.class')
echo "[4/6] package"
aapt2 compile --dir app/src/main/res -o build/res.zip
aapt2 link -o build/base.apk -I "$JAR" --manifest termux/AndroidManifest.xml \
  --min-sdk-version 33 --target-sdk-version 29 -A app/src/main/assets build/res.zip
(cd build/dex && zip -q ../base.apk classes.dex)
echo "[5/6] key"
[ -f termux/key.jks ] || keytool -genkeypair -keystore termux/key.jks -storepass android -keypass android \
  -alias k -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=centerbar"
echo "[6/6] sign"
apksigner sign --ks termux/key.jks --ks-pass pass:android --out CenterDynamicBar.apk build/base.apk
[ -d /sdcard/Download ] && cp CenterDynamicBar.apk /sdcard/Download/ && echo "Copied to /sdcard/Download/CenterDynamicBar.apk"
echo "DONE"
