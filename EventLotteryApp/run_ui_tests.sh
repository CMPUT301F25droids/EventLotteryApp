#!/bin/bash

# Script to run UI tests for Event Lottery App
# Usage: ./run_ui_tests.sh [test_class_name]

PROJECT_DIR="/Users/nazmaakter_1/AndroidStudioProjects/EventLotteryApp/EventLotteryApp"
ADB_PATH="$HOME/Library/Android/sdk/platform-tools/adb"

cd "$PROJECT_DIR"

echo "=== Event Lottery App - UI Test Runner ==="
echo ""

# Check if ADB exists
if [ ! -f "$ADB_PATH" ]; then
    echo "❌ Error: ADB not found at $ADB_PATH"
    echo "Please ensure Android SDK is installed"
    exit 1
fi

# Check connected devices
echo "Checking connected devices..."
DEVICES=$("$ADB_PATH" devices | grep -v "List" | grep "device" | wc -l | tr -d ' ')

if [ "$DEVICES" -eq 0 ]; then
    echo ""
    echo "⚠️  No devices found!"
    echo ""
    echo "Please:"
    echo "  1. Connect an Android device via USB, OR"
    echo "  2. Start an Android emulator"
    echo ""
    echo "To check devices manually, run:"
    echo "  $ADB_PATH devices"
    echo ""
    exit 1
fi

echo "✅ Found $DEVICES device(s)"
echo ""

# List connected devices
echo "Connected devices:"
"$ADB_PATH" devices | grep -v "List"
echo ""

# Check if specific test class was provided
if [ -z "$1" ]; then
    echo "Running ALL UI tests..."
    echo ""
    ./gradlew connectedAndroidTest
else
    echo "Running specific test class: $1"
    echo ""
    ./gradlew connectedAndroidTest --tests "$1"
fi

TEST_RESULT=$?

echo ""
if [ $TEST_RESULT -eq 0 ]; then
    echo "✅ Tests completed successfully!"
    echo ""
    echo "View test report:"
    echo "  open app/build/reports/androidTests/connected/index.html"
else
    echo "❌ Some tests failed. Check the report above for details."
    echo ""
    echo "View detailed report:"
    echo "  open app/build/reports/androidTests/connected/index.html"
fi

exit $TEST_RESULT

