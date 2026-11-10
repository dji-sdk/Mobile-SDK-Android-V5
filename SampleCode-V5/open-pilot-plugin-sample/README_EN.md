# Open Pilot Plugin Sample

An interface demo plugin built on DJI Mobile SDK V5 and DJI Pilot open capabilities. It shows how to integrate and invoke custom UI, map / AR, video-stream drawing, waypoint, file and data pipeline capabilities.

The project focuses on single-capability examples. It is intended as a development reference for Pilot plugin initialization, API invocation, remote UI interaction and state subscription, and does not require a business backend.

The samples use the open capabilities provided by DJI Pilot through MSDK Aircraft SDK. The SDK version and dependency configuration follow the corresponding release project.

## Features

The home page provides the following 11 sample entries:

| Sample | Description | Source directory |
|---|---|---|
| Custom UI components | Overlay layouts, text, buttons, input fields, images and lists on the Pilot UI, demonstrating property updates and interaction | `sampleitem/remoteviewpage/` |
| AR projection | Point, line, polygon, circle and radar effects, demonstrating element upload, update, deletion and click callbacks | `sampleitem/armanager/` |
| Map widgets | Marker and geometry widgets, batch widgets, layer management, map elements and state subscription | `sampleitem/mapmanager/` |
| Dialogs | Alert, confirm, prompt, single-choice and multi-choice dialogs, with user action callbacks | `sampleitem/dialogmanager/` |
| Video-stream drawing | Overlay rectangles, polygons and text on the live video stream | `sampleitem/cameradrawing/` |
| Page navigation | Jump to designated Pilot pages and plugin pages | `sampleitem/navigatemanager/` |
| Waypoint task | Local KMZ file access, task control, resume-from-break and state subscription | `sampleitem/waypoint/` |
| Key Manager | Read, set, listen to and invoke actions on Key values | `sampleitem/keymanager/` |
| File transfer | Upload files to the plugin private directory or the Pilot log directory; the delete button is currently disabled | `sampleitem/filetransfer/` |
| MOP data pipeline | Pipeline connection, byte read/write and chunked large-data transfer | `sampleitem/pipeline/` |
| Media file task | Browse camera media, preview, download original files, read XMP and parse thermal images | `sampleitem/mediatask/` |

The source directories in the table are relative to `src/main/java/com/dji/open/pilot/plugin/`. The home page also displays SDK registration, Pilot service connection and the acquired country code. Whether a specific capability is available depends on the aircraft, payload and Pilot version.

The map samples currently hide the text annotation, scale-zoom and set-center entries; the left menu and floating window only provide map widget, batch widget, layer management and map element entries.

DJI Pilot 5.20.1 base/client does not provide rich-text properties or APIs to control AR projection per video stream. HTML text in custom UI is parsed on the client and displayed as plain text; language refresh is still supported. The show/hide buttons on the AR projection switch page are disabled. The map status header only echoes the ready state and zoom value provided by the current protocol.

The map element sample records the UUIDs of successfully created elements and restores them by query after re-entering. After a deletion receipt it re-queries with a delay to verify; targets not confirmed gone are kept for retry. An AR visibility receipt only means the request has been submitted; actual annotation refresh is implemented by Pilot. Historical elements whose UUIDs were not recorded cannot be claimed automatically.

## Project Information

| Item | Value |
|---|---|
| Chinese display name | 开放插件 Sample |
| English display name | Open Pilot Plugin Sample |
| Developer (contract XML) | DJI Inc. |
| APK package name / plugin ID | `com.dji.open.pilot.plugin` |
| Source namespace | `com.dji.open.pilot.plugin` |
| Plugin entry | `com.dji.open.pilot.plugin.main.MainActivity` |
| Application | `com.dji.open.pilot.plugin.main.PluginApplication` |
| SDK connection mode | `REMOTE_PILOT` |

Pilot discovers the plugin through the `dji.open.pilot.client.OPEN_PILOT_PERMISSION_META_DATA` marker in the manifest and uses the APK package name as the plugin ID. The contract for this sample is at [`src/main/res/xml/dji_open_pilot_plugin.xml`](src/main/res/xml/dji_open_pilot_plugin.xml). The plugin name is provided in Chinese and English through the `dji_open_pilot_plugin_name` string resource.

## Requirements

- Android Studio, JDK 17, Android SDK Platform 35 and Build Tools 34.0.0.
- Use the Gradle Wrapper provided by the Android project. Source projects require the NDK specified by `NDK_VERSION` in the project `gradle.properties`.
- The MSDK Aircraft SDK binary release that supports Pilot open capabilities, plus its dependencies. The version must match the target Pilot.
- A remote controller compatible with DJI Pilot. The APK contains only `arm64-v8a`; the minimum Android API is 24.
- ADB for USB installation and debugging. The first SDK registration requires the remote controller to be online.
- Waypoint, camera media and MOP data pipeline flows require a compatible aircraft or payload.

## Quick Start

Run Android build commands from the Android project root that contains `settings.gradle` and the Gradle Wrapper. In this repository that directory is `android-sdk-v5-as/`, and the sample module is at the sibling `open-pilot-plugin-sample/`. In release packages, follow the actual project layout.

### 1. Prepare the project

Open the Android project in Android Studio, use the SDK dependency configuration provided with the corresponding version, and set the Android SDK path in `local.properties` at the Android project root:

```properties
sdk.dir=/your/Android/sdk
```

Source projects use the module's `build.gradle`; binary release projects integrate the Aircraft SDK using the build configuration provided in the release package. `aarbuild.gradle` is used for release builds; `utils`, `open-pilot-base` and `open-pilot-client` are already included in the corresponding Aircraft AAR, so dependency configuration should follow the accompanying project.

### 2. Configure the AppKey

Configure the following in `gradle.properties` at the Android project root:

```properties
OPEN_PLUGIN_SAMPLE_API_KEY=<your DJI AppKey>
```

The module reads the AppKey from Gradle project properties and injects it into the manifest's `${API_KEY}` through `manifestPlaceholders`; the corresponding metadata is `com.dji.sdk.API_KEY`. The module does not read an AppKey from module-level `local.properties`. When it is not configured, an empty value is injected and SDK registration cannot complete. Rebuild and reinstall after changing it.

Both debug and release use this module's `msdkkeystore.jks` with the alias `msdkkeystore`. When applying for an AppKey, use the APK package name `com.dji.open.pilot.plugin` and the actual signing fingerprint.

### 3. Build and install the plugin

From the Android project root:

```bash
./gradlew :open-pilot-plugin-sample:assembleDebug
```

The APK is at `build/outputs/apk/debug/open-pilot-plugin-sample-debug.apk` in this module. Enable USB debugging on the remote controller, connect it to the computer, and run (for this repository layout):

```bash
adb devices
adb install -r ../open-pilot-plugin-sample/build/outputs/apk/debug/open-pilot-plugin-sample-debug.apk
```

If the module location differs in a release package, replace the install path with the actual APK path. When multiple devices are connected, add `-s <RC serial number>` to ADB commands.

### 4. Launch and explore the samples

1. Open a compatible version of DJI Pilot on the remote controller, then open "开放插件 Sample" (Open Pilot Plugin Sample).
2. Wait until the home page status shows "Registered" and "Connected". The home page checks these two states before entering a sample.
3. Choose the module you want to verify. For UI, map / AR and dialog samples, select a case on the left and tap "View effect", then operate and observe results on the Pilot page and in the floating window.
4. Use "Back to plugin app" in the floating window to return to the plugin page and switch samples.
5. For waypoint, media and data pipeline samples, configure the files, camera or pipeline parameters on their pages and observe callbacks and states.

When the waypoint sample reads a local KMZ file, Android 11 and above require granting the app access to all files; follow the on-page guidance to authorize in system settings.

## Directory Structure

```text
open-pilot-plugin-sample/
├── README.md
├── build.gradle              # Source project app build and AppKey injection
├── aarbuild.gradle           # Binary release build configuration
├── AR_CLICK_CASES.md         # AR click and selection-state test cases
└── src/main/
    ├── AndroidManifest.xml  # Plugin discovery marker, AppKey and activity declarations
    ├── java/com/dji/open/pilot/plugin/
    │   ├── main/            # Application, SDK initialization and home module entries
    │   ├── common/          # Shared ViewModel, data listeners, drawing and navigation wrappers
    │   └── sampleitem/      # 11 samples grouped by open capability
    ├── assets/
    │   ├── plugin/          # Remote icons and demo images
    │   ├── mop/             # Data pipeline transfer samples
    │   └── rvcase_*.html    # RemoteView component preview pages
    └── res/                 # Layouts, Chinese/English strings, preview images and plugin contract
```

## Development and Verification

### Development entry points

- SDK integration: [`PluginApplication.kt`](src/main/java/com/dji/open/pilot/plugin/main/PluginApplication.kt) performs initialization at startup; [`MainViewModel.kt`](src/main/java/com/dji/open/pilot/plugin/main/MainViewModel.kt) configures `REMOTE_PILOT`, registers the SDK, handles Pilot connection state, and initializes shared listeners and the home floating window.
- Sample entries: maintain module cards in `MainViewModel.buildModuleItems()`; pages are opened by [`MainActivity.kt`](src/main/java/com/dji/open/pilot/plugin/main/MainActivity.kt). Adding an entry requires declaring the activity and strings; cards marked `TODO` are filtered out from the home page.
- Remote drawing: reuse the shared wrappers in [`common/drawing/`](src/main/java/com/dji/open/pilot/plugin/common/drawing/); refer to the corresponding module's ViewModel and Drawer.
- State and navigation: shared data listeners are in [`common/data/`](src/main/java/com/dji/open/pilot/plugin/common/data/); navigation wrappers are in [`NavigationHelper.kt`](src/main/java/com/dji/open/pilot/plugin/common/NavigationHelper.kt).
- Localization: strings live in `res/values/strings.xml` and `res/values-zh-rCN/strings.xml`; keep resource keys consistent when adding strings.

### Verification flow

Use the `assembleDebug` command above to build. After installation, first check SDK registration and Pilot connection state, then enter the target module to verify API callbacks, remote rendering and state updates.

Map / AR, video-stream drawing, waypoint, media download and MOP communication require joint debugging with the actual supported aircraft, payload, remote controller and Pilot version. See [`AR_CLICK_CASES.md`](AR_CLICK_CASES.md) for the specific steps to observe AR element clicks and selection state.

## Troubleshooting

- **SDK registration fails**: check `OPEN_PLUGIN_SAMPLE_API_KEY`, APK package name, signing fingerprint and remote controller network; rebuild and reinstall after changing the AppKey, and check the registration failure logs in `MainViewModel`.
- **Home page shows not connected, or samples cannot be entered**: confirm a compatible Pilot is running, and check the plugin discovery marker, SDK / Pilot version compatibility and `onRemoteServerCompatibleError` logs. Sample entries require successful SDK registration and a connected Pilot service.
- **Local KMZ not found or unreadable**: check the file path and grant all-files access in system settings on Android 11 and above.
- **Remote icons or drawing effects do not appear**: check Pilot connection, the resource upload result, the current Pilot page and the corresponding projection switch; use the operation callbacks to confirm the target Pilot supports the capability.
- **Waypoint, media or MOP operations fail**: check aircraft / payload connection, camera or pipeline parameters, capability support, and the error message returned by the page.
