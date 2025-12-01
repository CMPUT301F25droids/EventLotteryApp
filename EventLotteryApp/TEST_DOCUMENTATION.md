# Event Lottery App - Test Documentation

This document provides a comprehensive overview of all unit tests and UI tests created for the Event Lottery System Application.

## Test Structure

Tests are organized into two main categories:
1. **Unit Tests** (`app/src/test/java/`) - Test individual components, models, controllers, and helpers
2. **UI Tests** (`app/src/androidTest/java/`) - Test activities, fragments, and user interactions

---

## Unit Tests

### Data Models

#### `EventTest.java`
**Location:** `app/src/test/java/com/example/eventlotteryapp/data/EventTest.java`

**Tests:**
- Default constructor
- Getter methods for all fields (title, description, location, price, dates, etc.)
- Image getter and setter
- Event cancellation status

**Related User Stories:**
- US 02.01.01: Create event and generate QR code
- US 02.01.04: Set registration period

---

#### `UserTest.java`
**Location:** `app/src/test/java/com/example/eventlotteryapp/models/UserTest.java`

**Tests:**
- Default constructor
- Parameterized constructor
- All getters and setters (UID, name, email, phone)
- Serializable implementation
- Profile update functionality

**Related User Stories:**
- US 01.02.01: Provide personal information
- US 01.02.02: Update profile information
- US 01.07.01: Device identification

---

#### `EntrantTest.java`
**Location:** `app/src/test/java/com/example/eventlotteryapp/models/EntrantTest.java`

**Tests:**
- Default constructor
- Parameterized constructor
- Getter and setter methods
- Entrant equality checks

**Related User Stories:**
- US 01.01.01: Join waiting list
- US 01.05.02: Accept invitation
- US 01.05.03: Decline invitation

---

### Helper Classes

#### `DateTimeFormatTest.java`
**Location:** `app/src/test/java/com/example/eventlotteryapp/Helpers/DateTimeFormatTest.java`

**Tests:**
- Date to Firebase format conversion
- Firebase format to Date parsing
- Date consistency
- Invalid format error handling

---

#### `RelativeTimeTest.java`
**Location:** `app/src/test/java/com/example/eventlotteryapp/Helpers/RelativeTimeTest.java`

**Tests:**
- "Just now" time calculation
- Minutes ago formatting
- Hours ago formatting
- Days ago formatting
- Weeks ago formatting
- Months ago formatting
- Relative time ordering

---

### Controllers

#### `LotteryControllerTest.java`
**Location:** `app/src/test/java/com/example/eventlotteryapp/Controllers/LotteryControllerTest.java`

**Tests:**
- Controller initialization
- Accept invitation callback interface
- Decline invitation callback interface
- Null callback handling

**Related User Stories:**
- US 01.05.02: Accept invitation
- US 01.05.03: Decline invitation

**Note:** Full implementation would require Firebase Firestore mocking.

---

#### `CsvExportControllerTest.java`
**Location:** `app/src/test/java/com/example/eventlotteryapp/CsvExportControllerTest.java`

**Tests:**
- Controller initialization
- File name generation
- File name generation with special characters
- Empty entrants list handling
- Entrants list creation

**Related User Stories:**
- US 02.06.05: Export final list as CSV

---

## UI Tests

### Entrant Features

#### `EntrantHomePageActivityTest.java`
**Location:** `app/src/androidTest/java/com/example/eventlotteryapp/EntrantView/EntrantHomePageActivityTest.java`

**Tests:**
- Activity launches successfully
- Tab navigation between fragments

**Related User Stories:**
- US 01.01.03: See list of events
- US 01.02.03: View event history

---

#### `EventDetailsActivityTest.java`
**Location:** `app/src/androidTest/java/com/example/eventlotteryapp/EntrantView/EventDetailsActivityTest.java`

**Tests:**
- Activity launches with event ID
- Event details display
- Join waiting list button
- Leave waiting list button

**Related User Stories:**
- US 01.01.01: Join waiting list
- US 01.01.02: Leave waiting list
- US 01.06.01: View event details via QR code
- US 01.06.02: Sign up from QR code
- US 01.05.04: See waiting list count

---

#### `InvitationResponseActivityTest.java`
**Location:** `app/src/androidTest/java/com/example/eventlotteryapp/EntrantView/InvitationResponseActivityTest.java`

**Tests:**
- Activity launches with invitation data
- Accept invitation functionality
- Decline invitation functionality

**Related User Stories:**
- US 01.05.02: Accept invitation
- US 01.05.03: Decline invitation
- US 01.04.01: Receive notification when chosen

---

#### `ScanQrCodeActivityTest.java`
**Location:** `app/src/androidTest/java/com/example/eventlotteryapp/EntrantView/ScanQrCodeActivityTest.java`

**Tests:**
- Activity launches camera for QR scanning
- QR code scanning functionality
- Navigation to event details after scanning

**Related User Stories:**
- US 01.06.01: View event details via QR code
- US 01.06.02: Sign up from QR code

---

#### `ProfileFragmentTest.java`
**Location:** `app/src/androidTest/java/com/example/eventlotteryapp/ui/profile/ProfileFragmentTest.java`

**Tests:**
- Profile fragment display
- Update profile name
- Update profile email
- Update profile phone
- Notification preference toggle
- Delete profile functionality

**Related User Stories:**
- US 01.02.01: Provide personal information
- US 01.02.02: Update profile information
- US 01.02.04: Delete profile
- US 01.04.03: Opt out of notifications

---

### Organizer Features

#### `CreateEventActivityTest.java`
**Location:** `app/src/androidTest/java/com/example/eventlotteryapp/organizer/CreateEventActivityTest.java`

**Tests:**
- Activity launches
- Step 1: Basic event info (title, description, location)
- Step 2: Pricing information
- Step 3: Event dates
- Step 4: Registration period
- Step 5: Participant limits
- Step 6: Poster upload
- Event publishing and QR code generation

**Related User Stories:**
- US 02.01.01: Create event and generate QR code
- US 02.01.04: Set registration period
- US 02.03.01: Limit waiting list
- US 02.04.01: Upload event poster
- US 02.04.02: Update event poster

---

#### `OrganizerEventDetailsActivityTest.java`
**Location:** `app/src/androidTest/java/com/example/eventlotteryapp/organizer/OrganizerEventDetailsActivityTest.java`

**Tests:**
- Activity launches with event ID
- View waiting list
- View entrant locations on map
- Toggle geolocation requirement
- View chosen entrants
- View cancelled entrants
- View final enrolled list
- Cancel entrants
- Export CSV

**Related User Stories:**
- US 02.02.01: View waiting list
- US 02.02.02: See map of entrant locations
- US 02.02.03: Enable/disable geolocation requirement
- US 02.06.01: View chosen entrants
- US 02.06.02: View cancelled entrants
- US 02.06.03: View final enrolled list
- US 02.06.04: Cancel entrants
- US 02.06.05: Export CSV

---

#### `RunLotteryActivityTest.java`
**Location:** `app/src/androidTest/java/com/example/eventlotteryapp/organizer/RunLotteryActivityTest.java`

**Tests:**
- Activity launches with event ID
- Set number of participants to draw
- Run lottery draw
- Notifications sent after draw
- Draw replacement applicant

**Related User Stories:**
- US 02.05.01: Send notification to chosen entrants
- US 02.05.02: Set number of attendees to sample
- US 02.05.03: Draw replacement applicant

---

#### `NotifyEntrantsActivityTest.java`
**Location:** `app/src/androidTest/java/com/example/eventlotteryapp/organizer/NotifyEntrantsActivityTest.java`

**Tests:**
- Activity launches with event ID
- Send notifications to waiting list
- Send notifications to selected entrants
- Send notifications to cancelled entrants

**Related User Stories:**
- US 02.07.01: Send notifications to all waiting list entrants
- US 02.07.02: Send notifications to all selected entrants
- US 02.07.03: Send notifications to all cancelled entrants

---

### Admin Features

#### `AdminHomeActivityTest.java`
**Location:** `app/src/androidTest/java/com/example/eventlotteryapp/Admin/AdminHomeActivityTest.java`

**Tests:**
- Activity launches
- Navigate to browse events
- Navigate to browse profiles
- Navigate to browse images
- Navigate to notification logs

**Related User Stories:**
- US 03.04.01: Browse events
- US 03.05.01: Browse profiles
- US 03.06.01: Browse images
- US 03.08.01: Review notification logs

---

#### `AdminBrowseEventsActivityTest.java`
**Location:** `app/src/androidTest/java/com/example/eventlotteryapp/Admin/AdminBrowseEventsActivityTest.java`

**Tests:**
- Activity launches
- Browse events list
- Remove event

**Related User Stories:**
- US 03.04.01: Browse events
- US 03.01.01: Remove events

---

#### `AdminBrowseProfilesActivityTest.java`
**Location:** `app/src/androidTest/java/com/example/eventlotteryapp/Admin/AdminBrowseProfilesActivityTest.java`

**Tests:**
- Activity launches
- Browse profiles list
- Remove profile
- Remove organizer

**Related User Stories:**
- US 03.05.01: Browse profiles
- US 03.02.01: Remove profiles
- US 03.07.01: Remove organizers

---

#### `AdminBrowseImagesActivityTest.java`
**Location:** `app/src/androidTest/java/com/example/eventlotteryapp/Admin/AdminBrowseImagesActivityTest.java`

**Tests:**
- Activity launches
- Browse images grid/list
- View image details
- Remove image

**Related User Stories:**
- US 03.06.01: Browse images
- US 03.03.01: Remove images

---

## Test Coverage Summary

### User Story Coverage

#### Entrant User Stories (US 01.x.x)
- ✅ US 01.01.01: Join waiting list
- ✅ US 01.01.02: Leave waiting list
- ✅ US 01.01.03: See list of events
- ⚠️ US 01.01.04: Filter events (needs implementation)
- ✅ US 01.02.01: Provide personal information
- ✅ US 01.02.02: Update profile information
- ✅ US 01.02.03: View event history
- ✅ US 01.02.04: Delete profile
- ✅ US 01.04.01: Receive notification when chosen
- ✅ US 01.04.02: Receive notification when not chosen (covered by notification tests)
- ✅ US 01.04.03: Opt out of notifications
- ✅ US 01.05.01: Another chance if someone declines (covered by lottery tests)
- ✅ US 01.05.02: Accept invitation
- ✅ US 01.05.03: Decline invitation
- ✅ US 01.05.04: See waiting list count
- ⚠️ US 01.05.05: Know lottery criteria (needs implementation)
- ✅ US 01.06.01: View event details via QR code
- ✅ US 01.06.02: Sign up from QR code
- ✅ US 01.07.01: Device identification

#### Organizer User Stories (US 02.x.x)
- ✅ US 02.01.01: Create event and generate QR code
- ✅ US 02.01.04: Set registration period
- ✅ US 02.02.01: View waiting list
- ✅ US 02.02.02: See map of entrant locations
- ✅ US 02.02.03: Enable/disable geolocation requirement
- ✅ US 02.03.01: Limit waiting list
- ✅ US 02.04.01: Upload event poster
- ✅ US 02.04.02: Update event poster
- ✅ US 02.05.01: Send notification to chosen entrants
- ✅ US 02.05.02: Set number of attendees to sample
- ✅ US 02.05.03: Draw replacement applicant
- ✅ US 02.06.01: View chosen entrants
- ✅ US 02.06.02: View cancelled entrants
- ✅ US 02.06.03: View final enrolled list
- ✅ US 02.06.04: Cancel entrants
- ✅ US 02.06.05: Export CSV
- ✅ US 02.07.01: Send notifications to all waiting list entrants
- ✅ US 02.07.02: Send notifications to all selected entrants
- ✅ US 02.07.03: Send notifications to all cancelled entrants

#### Admin User Stories (US 03.x.x)
- ✅ US 03.01.01: Remove events
- ✅ US 03.02.01: Remove profiles
- ✅ US 03.03.01: Remove images
- ✅ US 03.04.01: Browse events
- ✅ US 03.05.01: Browse profiles
- ✅ US 03.06.01: Browse images
- ✅ US 03.07.01: Remove organizers
- ✅ US 03.08.01: Review notification logs

## Running Tests

### Unit Tests
```bash
./gradlew test
```

### UI Tests
```bash
./gradlew connectedAndroidTest
```

### Specific Test Class
```bash
./gradlew test --tests "com.example.eventlotteryapp.data.EventTest"
```

### Specific Test Method
```bash
./gradlew test --tests "com.example.eventlotteryapp.data.EventTest.testDefaultConstructor"
```

## Notes

1. **Firebase Dependencies**: Many UI tests require Firebase authentication and Firestore mocking. Consider using Firebase Test Lab or mocking Firebase services for more reliable tests.

2. **Async Operations**: Some tests use Thread.sleep() which is not ideal. Consider implementing Espresso Idling Resources for better async handling.

3. **Test Data**: Tests may require setup data in Firebase. Consider using a test Firebase project or mocking Firebase entirely.

4. **View IDs**: Some UI tests reference view IDs that need to be verified against actual layout files. Update view IDs as needed.

5. **Camera Permissions**: QR code scanning tests require camera permissions. Ensure permissions are granted in test setup.

6. **Geolocation**: Tests involving maps and geolocation require location permissions and may need mocking on emulators.

## Future Improvements

1. Add Espresso Idling Resources for better async test handling
2. Implement Firebase mocking for unit tests
3. Add more comprehensive edge case testing
4. Implement test data builders for easier test setup
5. Add performance tests
6. Add accessibility tests
7. Implement screenshot testing for UI regression detection
