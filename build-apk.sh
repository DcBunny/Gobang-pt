#!/usr/bin/env bash
set -euo pipefail

BUILD_TYPE="${1:-debug}"
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

case "$BUILD_TYPE" in
  debug)
    GRADLE_TASK=":app:assembleDebug"
    SOURCE_APK="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
    ;;
  release)
    GRADLE_TASK=":app:assembleRelease"
    SOURCE_APK="$PROJECT_DIR/app/build/outputs/apk/release/app-release-unsigned.apk"
    ;;
  *)
    echo "用法: $0 [debug|release]" >&2
    exit 1
    ;;
esac

cd "$PROJECT_DIR"
./gradlew "$GRADLE_TASK"

if [[ ! -f "$SOURCE_APK" ]]; then
  echo "构建完成，但未找到 APK: $SOURCE_APK" >&2
  exit 1
fi

OUTPUT_DIR="$PROJECT_DIR/dist"
OUTPUT_APK="$OUTPUT_DIR/gobang-pt-$BUILD_TYPE.apk"
mkdir -p "$OUTPUT_DIR"
cp "$SOURCE_APK" "$OUTPUT_APK"

echo "APK 已生成: $OUTPUT_APK"
if [[ "$BUILD_TYPE" == "release" ]]; then
  echo "提示: 当前项目未配置正式签名，Release APK 需签名后才能安装。"
fi
