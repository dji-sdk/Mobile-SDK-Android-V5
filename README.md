# DJI Mobile SDK for Android V5 Latest Alpha Version 5.7.0-a1

[中文版](README_CN.md)

##  Alpha Version Announcement

1. To improve the problem-solving efficiency of developers' feedback, we will fix the serious problems first. And we will release the alpha version immediately after the regression test.
2. For letting developers experience and test the MSDK functions that have been developed but not officially released, we will also release the alpha version immediately after the product acceptance test and functional test. 
3. The alpha version is not strictly tested before the release. There might exist some unstable problems. Please judge and choose whether to use this version according to the release note. If you have other problems, please contact us immediately.
4. All changes in the alpha version will be merged into the official version and will be strictly tested before the release.
5. It is not suggested that developers directly merge the MSDK alpha version and released it as an official version.

## Release Date

2023.8.24

## Release Notes

- Add Sample Widget:
- Flight Controller
	- `FC Home Point Widget` supports home point setting.
	- `FC Flight Mode Widget` supports flight mode setting.
	- `FC Return Home Mode Widget` supports return home mode setting.
	- `FC Distance Height Limit Widget` supports height and distance limit setting.
	- `FC IMU Status Widget` supports IMU status display and IMU calibration function.
	- `FC Compass Status Widget` supports compass status display and compass calibration function.
	- `FC Lost Action Widget` supports signal lost behavior setting.

- Perception
	- `Perception Avoidance Type Widget` supports obstacle sensing behavior type setting.
	- `Perception Vision Widget` supports vision positioning setting.

- Remote controller
	- `RC Pairing Widget` supports remote controller pairing setting.
	- `RC Calibration Widget` supports remote controller calibration function.

- Image transmission
	- `HD Frequency Mode Widget` supports video transmission working frequency mode setting.
	- `HD SDR Info Widget` supports video transmission signal status display.
	- `HD SDR Channel Mode Widget` supports video transmission channel mode setting.
	- `HD SDR Frequency Widget` supports video transmission channel setting.
	- `HD SDR Band Width Select Widget` supports image transmission downlink bandwidth setting.
	- `HD SDR Band Width Widget` supports image transmission downlink bandwidth display.
	- `HD SDR Video Rate Widget` supports image transmission code rate display.

- Battery
	- `Battery Info Widget` supports battery information display.
	- `Battery Alert Widget` supports battery warning setting.

- Gimbal
	- `Gimbal Fine Tune Widget` supports gimbal adjust setting.
	- `Gimbal Setting Widget` supports reset gimbal parameters and gimbal calibration function.

- Common
	- `Common Device Name Widget` supports device name display and renaming function.
	- `Common About Widget` supports firmware version information display.


### Bug Fixes
- **Call `PerceptionInformationListener` to report null pointer error:** Fixed
- **The `subItemsList` obtained by `payloadManager` is empty:** Fixed
- **Call `setLTEEnhancedTransmissionType` to set `OCU_ SYNC_ LTE`, prompt opening LTE failed:** Fixed
- **`RTKCenter` reported `ConcurrentModificationException` error:** Fixed
- **`KeyCameraStorageInfos` failed to obtain the storage information of the PSDK payload:** Fixed
- **The video stream cannot be opened after the PSDK load camera has backed out from media playback:** Fixed
- **Mavic 3E performs Return-To-Home after completing the waypoint mission, and the `WaypointMissionExecuteState` changes to `Unknown`:** Fixed


## Offline Documentation

- /Docs/Android_API/en/index.html

## AAR Explanation

> **Notice:** sdkVersion = 5.7.0-a1

| SDK package  <div style="width: 150pt">  | Explanation  <div style="width: 200pt">   | How to use <div style="width: 300pt">|
| :---------------: | :-----------------:  | :---------------: |
|     dji-sdk-v5-aircraft-alpha      | Aircraft main package, which provides support for MSDK to control the aircraft. | implementation 'com.dji:dji-sdk-v5-aircraft-alpha:{sdkVersion}' |
| dji-sdk-v5-aircraft-provided-alpha | Aircraft compilation package, which provides interfaces related to the aircraft package. | compileOnly 'com.dji:dji-sdk-v5-aircraft-provided-alpha:{sdkVersion}' |
| dji-sdk-v5-handheld-alpha<br/>(Not official released version) | Handheld main package, which provides support for MSDK to control handheld product. | implementation 'com.dji:dji-sdk-v5-handheld-alpha:{sdkVersion}' |
| dji-sdk-v5-handheld-provided-alpha<br/>(Not official released version) |     Handheld compilation package, which provides interfaces related to handheld package.             | compileOnly 'com.dji:dji-sdk-v5-handheld-provided-alpha:{sdkVersion}' |
| dji-sdk-v5-all-alpha<br/>(Not official released version) | Main package of all product, which provides support for MSDK to control aircraft and handheld product. | implementation 'com.dji:dji-sdk-v5-all-alpha:{sdkVersion}' |
| dji-sdk-v5-all-provided-alpha<br/>(Not official released version) |    Compilation package of all product, which provides interfaces related to all product.          | compileOnly 'com.dji:dji-sdk-v5-all-provided-alpha:{sdkVersion}' |
| dji-sdk-v5-networkImp-alpha | Network library package, which provides network connection ability for MSDK. Without this dependency, all network functions of MSDK will not work, but the interfaces of hardware control can be used normally. | runtimeOnly 'com.dji:dji-sdk-v5-networkImp-alpha:{sdkVersion}' |

- If only the aircraft product is in need to support, please use:
  ```groovy
  implementation 'com.dji:dji-sdk-v5-aircraft-alpha:{sdkVersion}'
  compileOnly 'com.dji:dji-sdk-v5-aircraft-provided-alpha:{sdkVersion}'
  ```

- If only the handheld product is in need to support, please use:
  ```groovy
  implementation 'com.dji:dji-sdk-v5-handheld-alpha:{sdkVersion}'
  compileOnly 'com.dji:dji-sdk-v5-handheld-provided-alpha:{sdkVersion}'
  ```
  
- If the aircraft and handheld product are in need to support, please use:
  ```groovy
  implementation 'com.dji:dji-sdk-v5-all-alpha:{sdkVersion}'
  compileOnly 'com.dji:dji-sdk-v5-all-provided-alpha:{sdkVersion}'
  ```
  
- If the MSDK have to use network(required by default), please use:
  ```groovy
  runtimeOnly 'com.dji:dji-sdk-v5-networkImp-alpha:{sdkVersion}'
  ```