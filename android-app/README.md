# SmartBMS Utility - Android

Android version of SmartBMS Utility - A Battery Management System monitoring application.

## Overview

This is a native Android application ported from the iOS SmartBMS Utility. It provides real-time monitoring, configuration, and analytics for compatible BMS hardware (JBD, Daly) via Bluetooth Low Energy connectivity.

## Technology Stack

- **Language**: Kotlin
- **Min SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)
- **Architecture**: MVVM (Model-View-ViewModel)
- **UI**: Material Design 3 with Navigation Component
- **Bluetooth**: Android Bluetooth LE API

## Project Structure

```
android-app/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/smartbms/utility/
│   │       │   ├── bluetooth/          # Bluetooth LE interface
│   │       │   │   └── BluetoothInterface.kt
│   │       │   ├── data/               # Data models and BMS protocol
│   │       │   │   ├── BMSStructs.kt
│   │       │   │   └── BMSDevice.kt
│   │       │   ├── ui/                 # UI components
│   │       │   │   ├── MainActivity.kt
│   │       │   │   ├── devices/        # Device discovery & connection
│   │       │   │   ├── overview/       # Battery dashboard
│   │       │   │   ├── configuration/  # BMS settings
│   │       │   │   ├── logging/        # Data logging
│   │       │   │   └── settings/       # App settings
│   │       │   ├── gps/                # GPS tracking
│   │       │   ├── logging/            # File management
│   │       │   └── utils/              # Utilities
│   │       ├── res/
│   │       │   ├── layout/             # XML layouts
│   │       │   ├── navigation/         # Navigation graph
│   │       │   ├── values/             # Strings, colors, themes
│   │       │   └── menu/               # Menu definitions
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Features

### Implemented Core Features

1. **BMS Protocol Support**
   - Request/Response packet structure
   - Checksum validation
   - Multi-part packet reassembly
   - Basic information (0x03)
   - Cell voltages (0x04)
   - Configuration parameters

2. **Bluetooth LE Interface**
   - Device scanning with service UUID filtering
   - Connection management
   - GATT service discovery
   - Characteristic read/write/notify
   - Multi-part packet handling
   - LiveData-based state management

3. **Data Models**
   - BMSDevice
   - BasicInformation (voltage, current, capacity, protection status)
   - CellVoltages
   - Configuration
   - ProtectionStatus

4. **Android Permissions**
   - Bluetooth (BLUETOOTH_SCAN, BLUETOOTH_CONNECT)
   - Location (required for BLE scanning)
   - Storage (for data logging)

### Features To Be Implemented

The following UI components and features need to be completed:

1. **Devices Screen** - Device discovery, connection list
2. **Overview Screen** - Real-time battery metrics dashboard
3. **Configuration Screen** - BMS settings editor
4. **Logging Screen** - Data logging and CSV export
5. **GPS Integration** - Location tracking and efficiency metrics
6. **Settings Screen** - App preferences
7. **BMS Data Parser** - Protocol parsing logic
8. **File Management** - Log file handling
9. **Demo Mode** - Testing without hardware

## Setup Instructions

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or later
- Android SDK with API 34
- Physical Android device with Bluetooth LE (emulator doesn't support BLE well)

### Building the Project

1. **Open in Android Studio**:
   ```bash
   cd android-app
   ```
   Then open the `android-app` directory in Android Studio.

2. **Sync Gradle**:
   - Android Studio should automatically sync Gradle
   - If not, click "File > Sync Project with Gradle Files"

3. **Build**:
   ```bash
   ./gradlew build
   ```

4. **Run on Device**:
   - Connect Android device via USB
   - Enable USB debugging in Developer Options
   - Click Run button or use:
   ```bash
   ./gradlew installDebug
   ```

### Required Permissions

The app requires the following permissions:

- `BLUETOOTH` - Bluetooth operations
- `BLUETOOTH_ADMIN` - Bluetooth management
- `BLUETOOTH_SCAN` - Scan for BLE devices (Android 12+)
- `BLUETOOTH_CONNECT` - Connect to BLE devices (Android 12+)
- `ACCESS_FINE_LOCATION` - Required for BLE scanning
- `ACCESS_COARSE_LOCATION` - Location services
- `WRITE_EXTERNAL_STORAGE` - Save log files (Android 12 and below)

## BMS Protocol

The application communicates with JBD/Daly BMS devices using a custom binary protocol:

### Packet Structure

```
[StartByte] [Status] [Command] [Length] [Data...] [Checksum] [StopByte]
   0xDD       0xA5      0x03      0x00              0xFFFF       0x77
```

### Supported Commands

- `0x03` - Basic Information (voltage, current, SOC, protection status)
- `0x04` - Cell Voltages
- `0x05` - Hardware Version
- `0x10-0xA2` - Configuration Parameters

### Service UUIDs

- Service: `0000FF00-0000-1000-8000-00805F9B34FB`
- RX (Notify): `0000FF01-0000-1000-8000-00805F9B34FB`
- TX (Write): `0000FF02-0000-1000-8000-00805F9B34FB`

## Dependencies

```kotlin
// AndroidX Core
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.appcompat:appcompat:1.6.1")
implementation("com.google.android.material:material:1.11.0")

// Lifecycle & ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

// Navigation
implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
implementation("androidx.navigation:navigation-ui-ktx:2.7.6")

// Bluetooth
implementation("androidx.bluetooth:bluetooth:1.0.0-alpha02")

// Location
implementation("com.google.android.gms:play-services-location:21.1.0")

// Charts
implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

// CSV
implementation("com.opencsv:opencsv:5.9")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

## Next Steps for Development

1. **Complete UI Fragments**:
   - DevicesFragment + ViewModel
   - OverviewFragment + ViewModel
   - ConfigurationFragment + ViewModel
   - LoggingFragment + ViewModel
   - SettingsFragment

2. **Implement Data Parser**:
   - Port BMSData.swift parsing logic
   - Handle all command responses
   - Implement data validation

3. **Add Data Persistence**:
   - SharedPreferences for settings
   - Room database for historical data (optional)
   - File-based CSV logging

4. **GPS Integration**:
   - Location tracking service
   - Distance calculation
   - Energy efficiency metrics

5. **Testing**:
   - Unit tests for protocol parsing
   - UI tests for fragments
   - Bluetooth integration tests (on device)

## Known Limitations

- Bluetooth LE cannot be tested on Android emulator (requires physical device)
- Location permission is required for BLE scanning on Android (platform requirement)
- Multi-part packet handling needs thorough testing with real hardware

## License

Same as the original SmartBMS Utility project (AGPL-3.0)

## Contributing

This is a port of the iOS version. Core protocol and features should match the iOS implementation for consistency.
