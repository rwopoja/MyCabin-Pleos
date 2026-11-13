#!/usr/bin/env bash
set -euo pipefail
# Actual AVD/device recording. Requires an already running, authorized Pleos AVD.
# Run this, perform docs/DEMO_GUIDE.md, then wait up to 120 seconds.
adb get-state
adb shell am start -n kr.mycabin.app/.MainActivity
adb shell screenrecord --time-limit 120 --bit-rate 6000000 /sdcard/MyCabin_AVD.mp4
mkdir -p media
adb pull /sdcard/MyCabin_AVD.mp4 media/MyCabin_AVD_Actual.mp4
printf '%s\n' 'Saved actual AVD screen recording: media/MyCabin_AVD_Actual.mp4'
