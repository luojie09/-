#!/usr/bin/env bash
set -euo pipefail

package=com.secretbase.app
report=app/build/reports/paired-startup
mkdir -p "$report"
adb install -r app/build/outputs/apk/debug/app-debug.apk

capture() {
  local name=$1
  adb logcat -d -v threadtime > "$report/$name-logcat.txt"
  adb exec-out screencap -p > "$report/$name.png" || true
  adb shell uiautomator dump /sdcard/startup-window.xml >/dev/null 2>&1 || true
  adb pull /sdcard/startup-window.xml "$report/$name-window.xml" >/dev/null 2>&1 || true
}

seed_identity() {
  local role=$1
  # This is an unsigned, unusable test token, not a user session. No data mutations
  # are performed. Keeping the remote modules enabled exercises their real startup.
  local token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIwMDAwMDAwMC0wMDAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwMDEiLCJyb2xlIjoiYXV0aGVudGljYXRlZCIsImV4cCI6NDEwMjQ0NDgwMH0.smoke-test-invalid-signature
  printf '%s\n' '<?xml version="1.0" encoding="utf-8" standalone="yes"?>' \
    '<map>' \
    "<string name=\"current_user_id\">$role</string>" \
    '<string name="couple_id">00000000-0000-0000-0000-000000000002</string>' \
    "<string name=\"access_token\">$token</string>" \
    '<string name="refresh_token">invalid-smoke-test-refresh-token</string>' \
    '<string name="auth_user_id">00000000-0000-0000-0000-000000000001</string>' \
    '<long name="expires_at_epoch_seconds" value="4102444800" />' \
    '</map>' > "$report/test-identity.xml"
  adb push "$report/test-identity.xml" /data/local/tmp/test-identity.xml >/dev/null
  adb shell run-as "$package" mkdir -p shared_prefs
  adb shell "run-as $package sh -c 'cat /data/local/tmp/test-identity.xml > shared_prefs/secret_base_identity.xml'"
}

start_and_check() {
  local name=$1
  # Some API 26 images refuse to clear the main buffer during initial boot.
  adb logcat -b crash -c || true
  adb shell am start -W -n "$package/.MainActivity"
  local initial_pid
  initial_pid=$(adb shell pidof "$package" | tr -d '\r') || true
  for attempt in $(seq 1 12); do
    sleep 5
    local current_pid
    current_pid=$(adb shell pidof "$package" | tr -d '\r') || true
    if [[ -z "$initial_pid" || "$current_pid" != "$initial_pid" ]]; then
      capture "$name"
      echo "::error::The paired app process exited or restarted ($name)."
      grep ' E AndroidRuntime' "$report/$name-logcat.txt" || true
      tail -n 250 "$report/$name-logcat.txt"
      return 1
    fi
  done
  capture "$name"
  # The hero's accessibility description exists only after HomePayload renders.
  if ! grep -q '小羊和小耶的日常插画' "$report/$name-window.xml"; then
    echo "::error::The paired app did not render its home screen ($name)."
    cat "$report/$name-window.xml" || true
    tail -n 250 "$report/$name-logcat.txt"
    return 1
  fi
  echo "Paired home screen stayed alive for 60 seconds: $name"
}

for role in chick sheep; do
  adb shell am force-stop "$package"
  seed_identity "$role"
  start_and_check "$role-online"
done

# Reopening an already paired app without connectivity must also preserve login.
adb shell am force-stop "$package"
adb shell svc wifi disable
adb shell svc data disable
start_and_check sheep-offline
