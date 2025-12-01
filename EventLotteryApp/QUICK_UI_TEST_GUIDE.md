# Quick Guide: How to Check & Run UI Tests

## ✅ Good News!
You have an **emulator connected** (`emulator-5554`), so you can run UI tests right now!

---

## 🚀 Method 1: Using the Script (Easiest)

I've created a helper script for you:

```bash
# Run ALL UI tests
./run_ui_tests.sh

# Run a specific test class
./run_ui_tests.sh "com.example.eventlotteryapp.EntrantView.EventDetailsActivityTest"
```

---

## 🚀 Method 2: Using Gradle Directly

### Check connected devices:
```bash
$HOME/Library/Android/sdk/platform-tools/adb devices
```

You should see:
```
List of devices attached
emulator-5554    device
```

### Run all UI tests:
```bash
./gradlew connectedAndroidTest
```

### Run a specific test class:
```bash
./gradlew connectedAndroidTest --tests "com.example.eventlotteryapp.AuthActivityTest"
```

---

## 🚀 Method 3: Using Android Studio (Visual)

1. **Open Android Studio**
2. **Connect device** (you already have emulator running ✅)
3. **Right-click** on `app/src/androidTest` folder
4. **Select** "Run 'Tests in androidTest'"
5. **Watch tests run** in the Run tool window

---

## 📋 Available UI Test Classes

### Entrant Tests:
- `EntrantView.EntrantHomePageActivityTest`
- `EntrantView.EventDetailsActivityTest`
- `EntrantView.InvitationResponseActivityTest`
- `EntrantView.ScanQrCodeActivityTest`
- `ui.profile.ProfileFragmentTest`

### Organizer Tests:
- `organizer.CreateEventActivityTest`
- `organizer.OrganizerEventDetailsActivityTest`
- `organizer.RunLotteryActivityTest`
- `organizer.NotifyEntrantsActivityTest`

### Admin Tests:
- `Admin.AdminHomeActivityTest`
- `Admin.AdminBrowseEventsActivityTest`
- `Admin.AdminBrowseProfilesActivityTest`
- `Admin.AdminBrowseImagesActivityTest`

### Auth:
- `AuthActivityTest` (existing test)

---

## 📊 View Test Results

After tests complete, view the report:

```bash
open app/build/reports/androidTests/connected/index.html
```

Or in Android Studio:
- **Build** → **View** → **Reports** → **Tests** → **androidTests**

---

## ⚠️ Important Notes

1. **Firebase Setup**: Some UI tests may require Firebase authentication. You may need to:
   - Set up a test Firebase project, OR
   - Mock Firebase services in tests

2. **Test Data**: Some tests may need test data in Firestore

3. **Permissions**: Tests might need permissions granted on the device

4. **Network**: Some tests may require network connectivity

---

## 🔍 Quick Commands Reference

```bash
# Check devices
$HOME/Library/Android/sdk/platform-tools/adb devices

# Run all UI tests
./gradlew connectedAndroidTest

# Run specific test
./gradlew connectedAndroidTest --tests "com.example.eventlotteryapp.EntrantView.EventDetailsActivityTest"

# View report
open app/build/reports/androidTests/connected/index.html

# Clean and run
./gradlew clean connectedAndroidTest
```

---

## 🎯 Try It Now!

You can run a simple test right away:

```bash
# Run the existing AuthActivityTest (already implemented)
./gradlew connectedAndroidTest --tests "com.example.eventlotteryapp.AuthActivityTest"
```

Or use the script:
```bash
./run_ui_tests.sh "com.example.eventlotteryapp.AuthActivityTest"
```

---

**Need more details?** See `UI_TEST_GUIDE.md` for comprehensive documentation.

