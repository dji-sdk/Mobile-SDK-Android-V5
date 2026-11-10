# Flight Task Management

A flight task management sample built on DJI Mobile SDK V5 and DJI Pilot open capabilities. It demonstrates the complete flow from dispatching a task on the Web, receiving the work order on the remote controller, executing a KMZ waypoint route, to uploading resources back.

The project contains an Android plugin and a Node.js demo backend. It is intended as a development reference for task management, waypoint execution and data synchronization.

The samples use the open capabilities provided by DJI Pilot through MSDK Aircraft SDK. The SDK version and dependency configuration follow the corresponding release project.

## Features

- Task management: task list, details, receiving, cancellation and status reporting.
- Waypoint execution: KMZ route download, loading, execution, pause, resume and stop.
- Resource management: new-photo detection, media download, local resource viewing and upload queue management.
- Pilot integration: `REMOTE_PILOT` access, floating window operations and connection state display.
- Web console: task creation and dispatch, remote controller registration, route and uploaded file viewing.

## Project Information

| Item | Value |
|---|---|
| Chinese display name | 飞行任务管理 |
| English display name | Flight Task Management |
| Developer | DJI Inc. |
| APK package name / plugin ID | `com.dji.sample.flighttask` |
| Source namespace | `dji.sample.flighttask` |
| Plugin entry | `dji.sample.flighttask.PluginActivity` |
| Default backend URL | `http://127.0.0.1:8080` |

Pilot uses the APK package name as the plugin ID. It preferentially reads `dji_open_pilot_plugin_*` string resources to display plugin information; the name, developer and description in the contract XML are fallback values. The source namespace may differ from the APK package name.

## Requirements

- Android Studio, JDK 17, Android SDK Platform 35 and Build Tools 34.0.0.
- Use the Gradle Wrapper provided by the Android project, and install the NDK specified by `NDK_VERSION` in the project `gradle.properties`.
- The MSDK Aircraft SDK binary release that supports Pilot open capabilities, plus its dependencies. The version must match the target Pilot.
- A remote controller compatible with DJI Pilot. The APK contains only `arm64-v8a`; the minimum Android API is 24.
- Node.js 24 is recommended for the backend; Docker Compose is an optional deployment method. USB debugging requires ADB.

## Quick Start

Run backend commands from the sample repository root; run Android build commands from the Android project root that contains `settings.gradle` and the Gradle Wrapper. Keep the backend terminal running, and use another terminal for building and installation.

### 1. Prepare the project

Open the Android project from the release package in Android Studio, use the SDK binary dependency configuration provided with that version, and set the Android SDK path in `local.properties` at the project root:

```properties
sdk.dir=/your/Android/sdk
```

### 2. Configure the AppKey and backend URL

Configure the following in `gradle.properties` at the Android project root:

```properties
FLIGHTTASK_SAMPLE_API_KEY=<your DJI AppKey>
FLIGHT_TASK_SERVER_URL=http://127.0.0.1:8080
```

The module reads the AppKey from Gradle project properties and injects it into the manifest's `${API_KEY}` through `manifestPlaceholders`; it does not read an AppKey from module-level `local.properties`. When the AppKey is not configured, an empty value is injected and SDK registration cannot complete. The backend URL is written into `BuildConfig` at compile time; rebuild and reinstall after changing it.

Both debug and release use this module's `msdkkeystore.jks` with the alias `msdkkeystore`. When applying for an AppKey, use the APK package name `com.dji.sample.flighttask` and the actual signing fingerprint.

### 3. Start the backend

```bash
cd open-pilot-flighttask-sample/server
npm start
```

The service listens on `127.0.0.1:8080` by default. Open <http://127.0.0.1:8080/> in a browser. No third-party Node.js dependencies need to be installed and no frontend build is required.

Check the service in another terminal:

```bash
curl http://127.0.0.1:8080/health
```

A `{"status":"ok"}` response means the service is healthy. See [`server/README.md`](server/README.md) for Docker deployment, data storage and API documentation.

### 4. Build and install the plugin

```bash
./gradlew :open-pilot-flighttask-sample:assembleDebug
```

The APK is at `build/outputs/apk/debug/open-pilot-flighttask-sample-debug.apk` in this module. Replace the path below with the actual module location before installing:

```bash
adb install -r /path/to/open-pilot-flighttask-sample/build/outputs/apk/debug/open-pilot-flighttask-sample-debug.apk
```

### 5. Connect the remote controller and dispatch a task

Enable USB debugging on the remote controller, connect it to the computer, and run:

```bash
adb devices
adb reverse tcp:8080 tcp:8080
adb reverse --list
```

1. Open "飞行任务管理" (Flight Task Management) on the remote controller and wait for SDK registration and Pilot connection to succeed.
2. Once the plugin has obtained the remote controller SN it accesses the backend, and the console can then select that remote controller.
3. Create a task in the console, upload a `.kmz` route, and save and dispatch it.
4. Receive the task in the plugin, execute the route, and check the status on the task, resource and transfer pages.
5. View the uploaded route and media files in the console.

After a USB reconnect or remote controller restart, run `adb reverse` again if the backend cannot be reached. When multiple devices are connected, add `-s <RC serial number>` to ADB commands.

## Directory Structure

```text
open-pilot-flighttask-sample/
├── build.gradle          # Android app build and configuration injection
├── src/main/
│   ├── java/dji/sample/flighttask/
│   │   ├── data/         # Work order, resource and cloud configuration models
│   │   ├── manager/      # Task, route, media and upload management
│   │   ├── models/       # Connection and business ViewModels
│   │   └── pages/        # Task, resource and transfer pages
│   └── res/              # Page resources, multilingual strings and plugin contract
├── server/               # Node.js backend and Web console
└── spec/                 # Module design and requirements
```

## Development and Verification

Backend tests:

```bash
cd open-pilot-flighttask-sample/server
npm test
```

Use the `assembleDebug` command above to build for Android. Route execution and media flows require joint debugging with the actual supported aircraft, remote controller and Pilot version.

To extend business logic, start from `TaskManager`, `WaylineManager`, `MediaManager` and `AssetUploadManager`; see [`spec/00-overview.md`](spec/00-overview.md) for architecture and module responsibilities.

## Troubleshooting

- **SDK registration fails**: check `FLIGHTTASK_SAMPLE_API_KEY`, APK package name, signing fingerprint and remote controller network.
- **Plugin does not appear, or Pilot is incompatible**: check the Pilot version, the plugin contract and the connection state.
- **Backend unreachable**: check whether the service is running, and whether `FLIGHT_TASK_SERVER_URL` and the `adb reverse` port match.
- **Task list is empty**: confirm the task has been dispatched to the current remote controller SN; drafts do not appear in the plugin task list.
