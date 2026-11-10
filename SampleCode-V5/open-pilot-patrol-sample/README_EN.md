# Smart Patrol

An intelligent patrol sample built on DJI Mobile SDK V5 and DJI Pilot open capabilities. It demonstrates task dispatch, waypoint patrol, target recognition and capture, map / AR marking and resource reporting.

The project contains an Android plugin and a Node.js demo backend. It is intended as a development reference for smart patrol orchestration and Pilot integration.

The samples use the open capabilities provided by DJI Pilot through MSDK Aircraft SDK. The SDK version and dependency configuration follow the corresponding release project.

## Features

- Task management: work order receiving, status reporting, route loading and takeoff confirmation.
- Waypoint operations: route execution, pause, resume, stop and return-to-home requests.
- Smart patrol: target recognition, capture orchestration and capture point recording.
- Pilot display: floating window operations, map / AR marking and destination FlyTo.
- Resource reporting: media download, upload queue and transfer progress display.
- Web console: task creation and dispatch, remote controller registration, route and media file viewing.

## Project Information

| Item | Value |
|---|---|
| Chinese display name | 智能巡检作业 |
| English display name | SmartPatrol |
| Developer | DJI Inc. |
| APK package name / plugin ID | `dji.sample.patrol` |
| Source namespace | `dji.sample.patrol` |
| Plugin entry | `dji.sample.patrol.PluginActivity` |
| Default backend URL | `http://127.0.0.1:18081` |

Pilot uses the APK package name as the plugin ID. It preferentially reads `dji_open_pilot_plugin_*` string resources to display plugin information; the name, developer and description in the contract XML are fallback values.

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
PATROL_SAMPLE_API_KEY=<your DJI AppKey>
PATROL_SERVER_URL=http://127.0.0.1:18081
```

The module reads the AppKey from Gradle project properties and injects it into the manifest's `${DJI_API_KEY}` through `manifestPlaceholders`; it does not read an AppKey from module-level `local.properties`. When the AppKey is not configured, an empty value is injected and SDK registration cannot complete. The backend URL is written into `BuildConfig` at compile time; rebuild and reinstall after changing it.

When `PATROL_SERVER_URL` is not configured, it falls back to `http://127.0.0.1:18081`; the flight task module's `FLIGHT_TASK_SERVER_URL` is not used.

Both debug and release use this module's `msdkkeystore.jks` with the alias `msdkkeystore`. When applying for an AppKey, use the APK package name `dji.sample.patrol` and the actual signing fingerprint.

### 3. Start the backend

```bash
cd open-pilot-patrol-sample/server
npm start
```

The service listens on `127.0.0.1:18081` by default. Open <http://127.0.0.1:18081/> in a browser. No third-party Node.js dependencies need to be installed and no frontend build is required.

Check the service in another terminal:

```bash
curl http://127.0.0.1:18081/health
```

A `{"status":"ok"}` response means the service is healthy. See [`server/README.md`](server/README.md) for Docker deployment, data storage and API documentation.

### 4. Build and install the plugin

```bash
./gradlew :open-pilot-patrol-sample:assembleDebug
```

The APK is at `build/outputs/apk/debug/open-pilot-patrol-sample-debug.apk` in this module. Replace the path below with the actual module location before installing:

```bash
adb install -r /path/to/open-pilot-patrol-sample/build/outputs/apk/debug/open-pilot-patrol-sample-debug.apk
```

### 5. Connect the remote controller and dispatch a task

Enable USB debugging on the remote controller, connect it to the computer, and run:

```bash
adb devices
adb reverse tcp:18081 tcp:18081
adb reverse --list
```

1. Open "智能巡检作业" (SmartPatrol) on the remote controller and wait for SDK registration and Pilot connection to succeed.
2. Once the plugin has obtained the remote controller SN it accesses the backend, and the console can then select that remote controller.
3. Create a task in the console, upload a `.kmz` route, and save and dispatch it.
4. Receive the task in the plugin, load the route, confirm takeoff and execute the patrol.
5. Check job status on the plugin task, resource and transfer pages and in the Pilot floating window, and view uploaded files in the console.

After a USB reconnect or remote controller restart, run `adb reverse` again if the backend cannot be reached. When multiple devices are connected, add `-s <RC serial number>` to ADB commands.

## Directory Structure

```text
open-pilot-patrol-sample/
├── build.gradle          # Android app build and configuration injection
├── src/main/
│   ├── java/dji/sample/patrol/
│   │   ├── data/         # Work order, target, capture point and resource models
│   │   ├── manager/      # Route, recognition, capture, map / AR and upload management
│   │   ├── models/       # Connection and business ViewModels
│   │   ├── pages/        # Task, resource and transfer pages
│   │   └── pilot/        # SDK initialization and Pilot integration
│   └── res/              # Page resources, multilingual strings and plugin contract
└── server/               # Node.js backend and Web console
```

## Development and Verification

Backend tests:

```bash
cd open-pilot-patrol-sample/server
npm test
```

Use the `assembleDebug` command above to build for Android. Target recognition, FlyTo, map / AR and waypoint capabilities require joint debugging with the actual supported aircraft, remote controller and Pilot version.

To extend the job flow, start from `RouteController`, `RecognitionManager`, `CaptureController` and `PatrolAssetReporter`; the SDK lifecycle is managed uniformly by `SdkBootstrapper`.

## Troubleshooting

- **SDK registration fails**: check `PATROL_SAMPLE_API_KEY`, APK package name, signing fingerprint and remote controller network.
- **Backend unreachable, or connected to another demo**: check `PATROL_SERVER_URL`; the patrol service uses the dedicated port `18081`.
- **Task list is empty**: confirm the task has been dispatched to the current remote controller SN; drafts do not appear in the plugin task list.
- **Smart capabilities unavailable**: check whether the corresponding aircraft and Pilot support the capability, and check the plugin connection state and operation feedback.
