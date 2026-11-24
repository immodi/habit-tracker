#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

IMAGE="ghcr.io/cirruslabs/android-sdk:35"
GRADLE_CACHE="${HOME}/.gradle-container"
mkdir -p "${GRADLE_CACHE}"

docker run --rm \
  -u "$(id -u):$(id -g)" \
  -e HOME=/tmp \
  -e GRADLE_USER_HOME=/gradle-home \
  -e ANDROID_HOME=/opt/android-sdk-linux \
  -e ANDROID_SDK_ROOT=/opt/android-sdk-linux \
  -v "${PWD}/android:/workspace" \
  -v "${GRADLE_CACHE}:/gradle-home" \
  -w /workspace \
  "${IMAGE}" \
  bash -lc './gradlew :app:assembleDebug :app:testDebugUnitTest --no-daemon --console=plain'

echo
echo "APK: android/app/build/outputs/apk/debug/app-debug.apk"
