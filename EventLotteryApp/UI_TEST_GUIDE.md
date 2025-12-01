# UI Test Guide - How to Run Instrumented Tests

This guide explains how to check and run UI (instrumented) tests for the Event Lottery App.

## Prerequisites

Before running UI tests, you need:
1. **Android device connected via USB** OR
2. **Android emulator running**

## Method 1: Using Android Studio (Recommended)

### Step 1: Connect Device or Start Emulator

**Option A: Physical Device**
1. Enable Developer Options on your Android device
2. Enable USB Debugging
3. Connect device via USB
4. Verify connection in Android Studio → Device Manager

**Option B: Emulator**
1. Open Android Studio
2. Go to Tools → Device Manager
3. Start an emulator (or create one if needed)

### Step 2: Run UI Tests

**Run All UI Tests:**
1. Open the project in Android Studio
2. Right-click on `app/src/androidTest` folder
3. Select **"Run 'Tests in androidTest'"**
4. Tests will run on your connected device/emulator

**Run Specific Test Class:**
1. Open a test file (e.g., `EntrantHomePageActivityTest.java`)
2. Right-click on the test class name or method
3. Select **"Run 'EntrantHomePageActivityTest'"**

**Run Single Test Method:**
1. Click the green arrow next to a test method
2. Select **"Run 'testMethodName()'"**

### Step 3: View Results

After tests complete:
- Results appear in the **Run** tool window at the bottom
- Green checkmarks = passed tests
- Red X marks = failed tests
- Click on failed tests to see error details

---

## Method 2: Using Gradle Command Line

### Step 1: Check Connected Devices

```bash
# Navigate to project root
cd /Users/nazmaakter_1/AndroidStudioProjects/EventLotteryApp/EventLotteryApp

# Check connected devices (ADB should be in Android SDK path)
$ANDROID_HOME/platform-tools/adb devices
```

**Expected output:**
```
List of devices attached
emulator-5554    device
```

If no devices are listed, connect a device or start an emulator.

### Step 2: Run All UI Tests

```bash
./gradlew connectedAndroidTest
```

This will:
- Build the debug APK
- Install it on connected device(s)
- Run all UI tests
- Generate reports in `app/build/reports/androidTests/connected/`

### Step 3: Run Specific Test Class

```bash
./gradlew connectedAndroidTest --tests "com.example.eventlotteryapp.EntrantView.EntrantHomePageActivityTest"
```

### Step 4: View Test Reports

After tests complete, open the HTML report:

```bash
# Open test report in browser
open app/build/reports/androidTests/connected/index.html

# Or view in Android Studio:
# Build → View → Reports → Tests → androidTests
```

---

## Method 3: Using ADB Directly

If you have Android SDK platform-tools in your PATH:

### Check Devices
```bash
adb devices
```

### Install Test APK
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
```

### Run Tests
```bash
adb shell am instrument -w -r \
  -e debug false \
  -e class com.example.eventlotteryapp.EntrantView.EntrantHomePageActivityTest \
  com.example.eventlotteryapp.test/androidx.test.runner.AndroidJUnitRunner
```

---

## Available UI Test Files

### Entrant Features
- `EntrantView/EntrantHomePageActivityTest.java`
- `EntrantView/EventDetailsActivityTest.java`
- `EntrantView/InvitationResponseActivityTest.java`
- `EntrantView/ScanQrCodeActivityTest.java`
- `ui/profile/ProfileFragmentTest.java`

### Organizer Features
- `organizer/CreateEventActivityTest.java`
- `organizer/OrganizerEventDetailsActivityTest.java`
- `organizer/RunLotteryActivityTest.java`
- `organizer/NotifyEntrantsActivityTest.java`

### Admin Features
- `Admin/AdminHomeActivityTest.java`
- `Admin/AdminBrowseEventsActivityTest.java`
- `Admin/AdminBrowseProfilesActivityTest.java`
- `Admin/AdminBrowseImagesActivityTest.java`

### Authorization
- `AuthActivityTest.java` (already exists)

---

## Troubleshooting

### "No devices found"
- Check USB connection
- Enable USB debugging on device
- Restart ADB: `adb kill-server && adb start-server`
- Check device appears in Android Studio Device Manager

### "Test installation failed"
- Uninstall previous test APKs: `adb uninstall com.example.eventlotteryapp.test`
- Clean and rebuild: `./gradlew clean connectedAndroidTest`

### "Firebase authentication required"
- UI tests that require Firebase need proper setup
- Consider using Firebase Test Lab for cloud testing
- Or mock Firebase services in tests

### Tests timeout or hang
- Increase timeout in test configuration
- Check device/emulator is responsive
- Review logcat for errors: `adb logcat`

---

## Test Reports Location

After running tests, reports are generated at:

```
app/build/reports/androidTests/connected/
├── index.html          # Main test report
├── classes/            # Per-class reports
└── packages/           # Per-package reports
```

---

## Quick Commands Reference

```bash
# Check connected devices
adb devices

# Run all UI tests
./gradlew connectedAndroidTest

# Run specific test class
./gradlew connectedAndroidTest --tests "com.example.eventlotteryapp.EntrantView.EventDetailsActivityTest"

# Clean and run tests
./gradlew clean connectedAndroidTest

# View test report
open app/build/reports/androidTests/connected/index.html

# Check ADB version
adb version
```

---

## Next Steps

1. **Set up device/emulator** - Connect a device or start emulator
2. **Run a simple test** - Start with `AuthActivityTest.java`
3. **Review results** - Check test reports for any failures
4. **Fix issues** - Address any Firebase/auth setup needed
5. **Expand coverage** - Add more detailed test assertions

---

**Note**: Some UI tests may require:
- Firebase authentication setup
- Test data in Firestore
- Proper permissions granted
- Network connectivity

Consider mocking these dependencies for more reliable tests.

