# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BMS (Battery Management System) is an Android application for employee time tracking and attendance management using biometric authentication (fingerprint and facial recognition). The app operates in online/offline modes with cloud synchronization capabilities.

**Key Technologies:**
- Android SDK (minSdk 30, targetSdk 33, compileSdk 35)
- Kotlin + Java
- Jetpack Compose + traditional View system
- SQLite with custom DatabaseHelper
- GreenDAO (for FacePass integration)
- Spring Security (BCrypt password hashing)
- Third-party biometric SDKs: HFT Eco Fingerprint SDK, FacePass AAR

## Build Commands

### Building the Application
```bash
# Clean and build debug APK
./gradlew clean assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run instrumented tests on connected device
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests "com.example.bms.YourTestClass"
```

### Lint and Code Quality
```bash
# Run lint checks
./gradlew lint

# Generate lint report
./gradlew lintDebug
```

### Dependencies
```bash
# View dependency tree
./gradlew :app:dependencies
```

## Architecture Overview

### Three-Tier Architecture

1. **UI Layer**: Activities and Fragments
2. **Business Logic Layer**: Repository pattern for data management
3. **Data Layer**: SQLite (DatabaseHelper) + GreenDAO (for face recognition)

### Application Flow

```
EndpointRegistration (Launcher)
  → LoginActivity
  → SplashScreen (validates session)
  → DeviceRegistration (first run)
  → TimeEntryRegister (main screen) / GroupActivity (superadmin)
```

### Core Packages

- **Root (`com.example.bms`)**: Core activities, App class, Configuration, repositories
- **`ui.login`**: Authentication (LoginActivity, LoginViewModel, LoginRepository)
- **`enrollment`**: User management (EnrollmentActivity, EnrollmentList, EnrollmentEdit, EnrollmentShow)
- **`time_entry`**: Time tracking (TimeEntryRegister, TimeRepository)
- **`data.model`**: Data models (User, LoggedInUser)

### Database Structure (bms.db)

**Key Tables:**
- `users`: Employee records with roles (superadmin/groupadmin/employee), personal info, hashed passwords
- `biometrics`: Biometric templates (fingerprint/face/rfid) with type field
- `fingerprints`: Actual fingerprint template data (Base64-encoded)
- `time_entries`: Check-in/out records with type (check_in, check_out, break_in, break_out, overtime), GPS coordinates, snapshots
- `devices`: Registered devices with serial numbers, groups, online status, configuration flags
- `groups`: User groups for organizing employees
- `announcements`: System announcements with expiration

**Critical Fields:**
- `is_synced`: Tracks cloud sync status (0 = pending, 1 = synced)
- `deleted_at`, `deleted_by`: Soft delete mechanism
- `source`: Origin of record (web/device)

### Biometric Integration

**Fingerprint (HFT Eco SDK)**
- Library: `libs/fingerprintv3.aar`
- Main class: `com.hfteco.finger.FingerSDK`
- Used in: `FingerPrintScanActivity`
- Flow: Capture → Encode to Base64 → Store in `biometrics` and `fingerprints` tables

**Face Recognition (FacePass)**
- Library: `libs/facepass.aar`
- Package: `facex.facepass.*`
- Used in: `FaceScanner`, `ScanFaceActivity`, `TimeEntryRegister`
- Database: Separate GreenDAO database (`facex_db`) with User/UserDao
- Helper: `InitFacePassHandler` manages group-based face enrollment
- Flow: Initialize FacePass → Capture face → addFace() → bindGroup()

### Synchronization Strategy

**Background Sync (App.java):**
- Runs every 60 seconds (configurable via `Configuration.PREFS_NAME`)
- Syncs: devices, users, time entries, announcements, settings
- Uses `ExecutorService` for threading
- Implements `SyncCallback` interface for async results

**Manual Sync:**
- Triggered on logout or via Configuration settings
- Uses `push_status`/`pull_status` flags

**Upload Strategy:**
- Time entries uploaded in batches with multipart form data
- Includes photo snapshots (stored in external storage `/snapshots`)
- Auto-cleanup of old snapshots based on retention period

### Authentication & Security

**Encryption:**
- `EncryptionUtil`: Uses AndroidKeyStore with AES/GCM
- Encrypts user session data in SharedPreferences
- Format: "DisplayName,Email,Password,GroupID,Role,Token"

**Password Hashing:**
- BCrypt via Spring Security (`org.springframework.security.crypto.bcrypt.BCrypt`)

**Authorization:**
- JWT tokens (Bearer authentication)
- Token refresh endpoint: `/api/refresh-token`

**Roles:**
- `superadmin`: Full access, group selection (group_id=0)
- `groupadmin`: Manage users within assigned group
- `employee`: Time tracking only

### Key API Endpoints

Base URL configured in `EndpointRegistration`

```
POST   /api/sync/devices           - Register/sync device
POST   /api/sync/users             - Sync user data
GET    /api/sync/users/login       - Fetch users for login device
POST   /api/sync/time_entries      - Upload time entries (multipart with snapshots)
GET    /api/sync/time-entries/{id} - Fetch time entries
GET    /api/sync/announcements     - Fetch announcements
GET    /api/sync/devices/status    - Get device push/pull status
POST   /api/sync/devices/status    - Update device status
GET    /api/sync/settings          - Get device configuration
POST   /api/refresh-token          - Refresh authentication token
GET    /all/devices                - Get similar devices by model
```

### Repository Pattern

All database operations go through repository classes:
- `UserRepository`
- `BiometricRepository`
- `FingerprintRepository`
- `DeviceRepository`
- `TimeRepository`
- `GroupRepository`
- `AnnouncementRepository`

Each repository wraps `DatabaseHelper` operations and provides a clean API for business logic.

### Location Tracking

- **Google Play Services**: `FusedLocationProviderClient`
- High-accuracy location updates via `LocationCallback`
- Location stored with each time entry for attendance verification
- Initialized in `App.onCreate()` → `initLocation()`

### Device Admin Features

- `MyAdminReceiver`: BroadcastReceiver for device admin events
- `DevicePolicyManager`: Remote lock screen functionality
- Requires user to grant device admin permissions

## Important Development Notes

### Working with Biometrics

When adding/modifying biometric features:
1. Fingerprint data is stored as Base64-encoded strings in `biometrics.key` and `fingerprints.key`
2. Face data uses separate GreenDAO database (`facex_db`) - don't mix with main database operations
3. Always check device capabilities before initializing biometric SDKs
4. FacePass requires group binding - use `InitFacePassHandler.bindGroup()`

### Time Entry Types

Valid types for `time_entries.type` column:
- `check_in`
- `check_out`
- `break_in`
- `break_out`
- `overtime_in`
- `overtime_out`

Device configuration flags in `devices` table control which types are enabled.

### Database Migrations

- Main database version: `DATABASE_VERSION = 32` (in `DatabaseHelper.java`)
- GreenDAO schema version: `schemaVersion = 1` (in `app/build.gradle.kts`)
- Use `MigrationHelper` for GreenDAO upgrades (see `OnepassOpenHelper`)
- Always test migrations with existing data before deploying

### Configuration Management

Settings stored in SharedPreferences under `Configuration.PREFS_NAME`:
- `endpoint`: API base URL
- `device_serial`: Unique device identifier
- `online_status`: Online/offline mode
- `sync_interval`: Background sync frequency
- `group_id`: Selected group (for superadmin)
- `manual_time_entry`, `check_in`, `check_out`, `break_in`, `break_out`, `overtime_in`, `overtime_out`: Feature flags

### Testing Considerations

- Device requires physical fingerprint scanner and camera for full functionality
- Test with Android API 30+ devices (minSdk 30)
- Location permissions required for GPS tracking
- Camera and storage permissions required for snapshots
- NFC hardware required if using NFC features

### File Storage

**Snapshots:**
- Location: External storage `/snapshots` directory
- Naming: `{timestamp}_{userId}.jpg`
- Cleanup: Old files deleted after retention period (configurable)

**Logos:**
- Location: Application cache directory
- Loaded from remote URL specified in device settings

## Common Modifications

### Adding a New Time Entry Type

1. Update `devices` table schema with new boolean column
2. Add configuration field in `Configuration` activity
3. Update `TimeEntryRegister` to show/hide new button based on config
4. Add case in time entry creation logic
5. Sync new field with server via `/api/sync/settings`

### Adding a New Biometric Method

1. Add new AAR library to `libs/` folder
2. Update `app/build.gradle.kts` dependencies
3. Create new Activity extending biometric scanning pattern
4. Add new `type` value to `biometrics` table schema
5. Update repository methods to handle new type
6. Add initialization logic in `App.onCreate()`

### Modifying Sync Behavior

- Sync interval: Modify handler delay in `App.java`
- Sync endpoints: Update method calls in `App.syncData()`
- Retry logic: Implement in repository callback handlers
- Batch size: Adjust pagination in `syncTimeEntries()`

### Role-Based Access Control

When adding features requiring authorization:
1. Check `LoggedInUser.getRole()` value
2. Use `getGroupId()` for group-level filtering
3. Hide/show UI elements based on role
4. Validate on server side (client-side checks are not secure)

## Troubleshooting

**Common Issues:**

1. **Fingerprint SDK not initializing**: Check USB permissions and device compatibility
2. **FacePass crashes**: Ensure GreenDAO database is initialized before FacePass
3. **Sync failing**: Verify `is_synced` flags and check network connectivity
4. **Location not updating**: Confirm location permissions granted and FusedLocationProviderClient initialized
5. **Device not registering**: Check serial number uniqueness and group model compatibility
