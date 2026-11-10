# Gas Detection

A gas detection sample built on DJI Mobile SDK V5, DJI Pilot open capabilities and the PSDK payload data channel. It demonstrates detection task dispatch, payload measurement access, real-time monitoring and alerts, vessel identification / following, and detection record reporting.

This release provides the Android plugin and a reusable Android-side PSDK communication library, and keeps a Node.js demo backend for task debugging. The payload-side PSDK application is implemented by the device developer and is not shipped with this project.

The samples use the open capabilities provided by DJI Pilot through MSDK Aircraft SDK. The SDK version and dependency configuration follow the corresponding release project.

## Features

- Task management: work order receiving and rejection, job preflight, task status synchronization and completion record generation.
- Gas monitoring: channel catalog configuration, payload online state, measurement data, trends and alert display.
- Job control: automatic FlyTo, vessel identification, following and switching to manual operation.
- Detection records: task-level data aggregation, local upload queue and offline retransmission.
- Pilot integration: `REMOTE_PILOT` access, floating window state and operations.
- Payload debugging: a separate second-level page supporting `psdk-bridge` protocol requests and raw Hex / UTF-8 byte send and receive.
- Web console: detection task creation and dispatch, remote controller registration and detection record viewing.

## Project Information

| Item | Value |
|---|---|
| Chinese display name | 气体检测 |
| English display name | Gas Detection |
| Developer | DJI Inc. |
| APK package name / plugin ID | `com.dji.sample.gasdetection` |
| Source namespace | `com.dji.sample.gasdetection` |
| Plugin entry | `com.dji.sample.gasdetection.MainActivity` |
| Default backend URL | `http://127.0.0.1:18082` |

Pilot uses the APK package name as the plugin ID; the contract XML does not declare an ID. Plugin name, developer and description are read from `dji_open_pilot_plugin_*` string resources according to language, with the contract XML as a fallback.

## Requirements

- Android Studio, JDK 17, Android SDK Platform 35 and Build Tools 34.0.0.
- Use the Gradle Wrapper provided by the Android project, and install the NDK specified by `NDK_VERSION` in the project `gradle.properties`.
- The MSDK Aircraft SDK binary release that supports Pilot open capabilities, plus its dependencies. The version must match the target Pilot.
- A remote controller compatible with DJI Pilot. The APK contains only `arm64-v8a`; the minimum Android API is 24.
- Node.js 24 is recommended for the backend; USB debugging requires ADB.
- The measurement flow requires a payload that supports the PSDK data channel and can report data according to the measurement protocol in this document. The Android build does not depend on the payload-side PSDK project or toolchain.

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
GASDETECTION_SAMPLE_API_KEY=<your DJI AppKey>
GASDETECTION_SERVER_URL=http://127.0.0.1:18082
```

The module reads the AppKey from Gradle project properties and injects it into the manifest's `${DJI_API_KEY}` through `manifestPlaceholders`; it does not read an AppKey from module-level `local.properties`. When the AppKey is not configured, an empty value is injected and SDK registration cannot complete. The backend URL is written into `BuildConfig` at compile time; rebuild and reinstall after changing it. When not configured, it falls back to `http://127.0.0.1:18082`.

Both debug and release use `msdkkeystore.jks` at the Android project root with the alias `msdkkeystore`. When applying for an AppKey, use the APK package name `com.dji.sample.gasdetection` and the actual signing fingerprint.

### 3. Start the backend

```bash
cd open-pilot-gasdetection-sample/server
npm start
```

The service listens on `127.0.0.1:18082` by default. Open <http://127.0.0.1:18082/> in a browser. No third-party Node.js dependencies need to be installed and no frontend build is required.

Check the service in another terminal:

```bash
curl http://127.0.0.1:18082/health
```

A `{"status":"ok"}` response means the service is healthy. See [`server/README.md`](server/README.md) for task fields, state transitions and the detection record API.

### 4. Build and install the plugin

```bash
./gradlew :open-pilot-gasdetection-sample:assembleDebug
```

The APK is at `build/outputs/apk/debug/open-pilot-gasdetection-sample-debug.apk` in this module. Replace the path below with the actual module location before installing:

```bash
adb install -r /path/to/open-pilot-gasdetection-sample/build/outputs/apk/debug/open-pilot-gasdetection-sample-debug.apk
```

### 5. Connect the remote controller and start debugging

Enable USB debugging on the remote controller, connect it to the computer, and run:

```bash
adb devices
adb reverse tcp:18082 tcp:18082
adb reverse --list
```

1. Open "气体检测" (Gas Detection) on the remote controller and wait for SDK registration and Pilot connection to succeed.
2. Once the plugin has obtained the remote controller SN it accesses the backend; create and dispatch a detection task for that remote controller in the console.
3. Receive the task on the plugin "Task" page and check the preflight result; check the payload and channel measurements on the "Device" page; check detection records and diagnostics on the "Transfer" page.
4. Connect a payload that implements the gas measurement protocol below, and verify port online state and channel measurements on the device page.
5. After finishing the task, view the generated and reported detection records in the plugin and the Web console.

After a USB reconnect or remote controller restart, run `adb reverse` again if the backend cannot be reached. When multiple devices are connected, add `-s <RC serial number>` to ADB commands.

## Payload Communication and Debugging

### Gas measurement uplink

The Android plugin receives raw bytes through the `IPayloadManager` data callback, and `GasDemoFrameAdapter` and `JsonMeasurementCodec` decode the v1 compact JSON measurement frame; channel names, units and ranges are completed from `src/main/assets/gas-inspection-config.json`.

The device side must report UTF-8 JSON according to the Android decoding contract, for example:

```json
{"v":1,"s":"gas-session-001","n":0,"t":1700000000001,"p":[null,null,null,null,0],"g":[[1,657,1],[2,462,1],[3,499,1]],"f":4,"a":0}
```

- `v`: protocol version; `s`: session identifier; `n`: sequence number; `t`: device sampling timestamp (Unix milliseconds).
- `p`: position array; the four `null` values and the validity flag `0` in the example mean the position is unknown.
- `g`: channel array, each item being `[wireId, value, quality code]`; `wireId` corresponds to the Android channel catalog.
- `f`: job phase code; `a`: automatic mode flag. `f=4` and `a=0` in the example mean manual operation.

The example illustrates the format; use the current sampling time and actual device measurements during debugging. This flow receives raw JSON directly and does not prepend a `psdk-bridge` frame header.

Complete field validation and encoding constraints are defined by [`JsonMeasurementCodec.kt`](src/main/java/com/dji/sample/gasdetection/gas/wire/JsonMeasurementCodec.kt), [`GasWireLimits.kt`](src/main/java/com/dji/sample/gasdetection/gas/wire/GasWireLimits.kt) and the [channel configuration](src/main/assets/gas-inspection-config.json) on the Android side.

### Protocol and raw byte debugging

From the plugin home page, open the payload debug page, select the actually online port and packet length, then tap "Apply port / Start receiving":

- The default protocol mode uses Android-side `psdk-bridge`, sends `READ_DEVICE_INFO` (command number `2`) and displays protocol state and request results; the device side must implement the matching request / response protocol.
- After disabling protocol mode, raw byte send and receive is used; the Hex input `00 41 FF` actually sends 3 bytes.
- Protocol mode and raw mode are mutually exclusive on the debug page; page listeners are removed when leaving the page.

See [`psdk-bridge/README.md`](psdk-bridge/README.md) for integrating the reusable communication library, and for its request / response, event and lifecycle conventions.

## Directory Structure

```text
open-pilot-gasdetection-sample/
├── build.gradle          # Android app build and configuration injection
├── src/main/
│   ├── java/com/dji/sample/gasdetection/
│   │   ├── gas/          # Task, measurement, alert, identification, following and detection records
│   │   └── ui/           # Page state mapping and payload debug page
│   ├── assets/           # Gas channel and job configuration
│   └── res/              # Page resources, multilingual strings and plugin contract
├── server/               # Node.js backend and Web console
└── psdk-bridge/          # Reusable Android-side PSDK communication library
```

## Development and Verification

Backend tests:

```bash
cd open-pilot-gasdetection-sample/server
npm test
```

Use the `assembleDebug` command above to build for Android. Measurement channels, automatic flight, identification and following capabilities require joint debugging with the actual supported payload, aircraft, remote controller and Pilot version.

Update the channel configuration and measurement adapter when adding gas types; refer to `psdk-bridge` when adding device commands. UI strings use `values/strings.xml` and `values-zh/strings.xml`; keep bilingual resource keys consistent.

## Troubleshooting

- **SDK registration fails**: check `GASDETECTION_SAMPLE_API_KEY`, APK package name, signing fingerprint and remote controller network.
- **Backend unreachable**: check `GASDETECTION_SERVER_URL`, the service listening port and `adb reverse`.
- **No measurement data**: check whether the payload port is online, whether the frame format matches, whether the channel configuration loaded successfully, and check port diagnostics.
- **Protocol requests not ready**: check SDK registration, Pilot, aircraft and payload connections, and capability availability; see the communication library documentation for the specific conditions.
- **Detection records not reported**: check the upload state on the transfer page, the current remote controller SN and the backend connection; check retransmission results after the connection recovers.
