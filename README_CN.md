# DJI Mobile SDK for Android V5 最新Alpha版本 5.7.0-a1

[English Version](README.md)

## Alpha版本声明

1. 为了提高开发者反馈问题的解决效率，我们会优先修复一些严重的问题，在进行问题回归测试后，会第一时间对外发布我们的Alpha版本。
2. 为了能够让开发者尽快体验和测试到我们开发完成但暂未正式发布的功能，在进行产品验收和功能测试后，我们也会第一时间对外发布我们的Alpha版本。
3. Alpha版本未经过严格发布测试，可能存在一些不稳定问题。请开发者根据版本发布记录，自行判断和选择使用Alpha版本。如果有其他问题请第一时间反馈给我们。
4. Alpha版本的所有改动都会同步到正式版本，进行严格的发布测试后对外发布。
5. 不建议开发者直接集成MSDK Alpha版本作为正式版本进行发布。

## 发布日期

2023.8.24

## 发布记录

- 新增 Sample 控件：
- 飞控
	- FC Home Point Widget 支持返航点设置。
	- FC Flight Mode Widget 支持飞行模式设置。
	- FC Return Home Mode Widget 支持返航模式设置。
	- FC Distance Height Limit Widget 支持限高限远设置。
	- FC IMU Status Widget 支持IMU状态显示和IMU校准功能。
	- FC Compass Status Widget 支持指南针状态显示和指南针校准功能。
	- FC Lost Action Widget 支持失控行为设置。

- 感知
	- Perception Avoidance Type Widget 支持避障行为类型设置。
	- Perception Vision Widget 支持视觉定位设置。

- 遥控器
	- RC Pairing Widget 支持遥控器对频设置。
	- RC Calibration Widget 支持遥控器校准功能。

- 图传
	- HD Frequency Mode Widget 支持图传工作频段模式设置。
	- HD SDR Info Widget 支持图传信号状态显示。
	- HD SDR Channel Mode Widget 支持图传信道模式设置。
	- HD SDR Frequency Widget 支持图传信道设置。
	- HD SDR Band Width Select Widget 支持图传下行带宽设置。
	- HD SDR Band Width Widget 支持图传下行带宽显示。
	- HD SDR Video Rate Widget 支持图传码率显示。

- 电池
	- Battery Info Widget 支持电池信息显示。
	- Battery Alert Widget 支持电池报警设置。

- 云台
	- Gimbal Fine Tune Widget 支持云台微调设置。
	- Gimbal Setting Widget 支持重置云台参数和云台自动校准功能。

- 通用
	- Common Device Name Widget 支持设备名称显示和重命名功能。
	- Common About Widget 支持固件版本信息显示。

### Bug修复
- **调用`PerceptionInformationListener`报空指针错误。:** 已修复
- **`payloadManager`获取的`subItemsList`为空:** 已修复
- **调用`setLTEEnhancedTransmissionType`设置`OCU_SYNC_LTE`，提示开启LTE失败:** 已修复
- **`RTKCenter`报`ConcurrentModificationException`错误:** 已修复
- **`KeyCameraStorageInfos`无法获取到PSDK负载的存储信息:** 已修复
- **PSDK负载相机从媒体回放退出来以后视频流无法打开:** 已修复
- **Mavic 3E执行完航线任务后执行返航，`WaypointMissionExecuteState`变成`Unknown`:** 已修复

## 离线文档

- /Docs/Android_API/cn/index.html

## AAR说明

> **注意：** sdkVersion = 5.7.0-a1

| SDK包  <div style="width: 150pt">  | 说明  <div style="width: 200pt">   | 使用方式 <div style="width: 300pt">|
| :---------------: | :-----------------:  | :---------------: |
|     dji-sdk-v5-aircraft-alpha     | 飞机主包，提供MSDK对飞机控制的支持。 | implementation 'com.dji:dji-sdk-v5-aircraft-alpha:{sdkVersion}' |
| dji-sdk-v5-aircraft-provided-alpha | 飞机编译包，提供飞机包相关接口。 | compileOnly 'com.dji:dji-sdk-v5-aircraft-provided-alpha:{sdkVersion}' |
| dji-sdk-v5-handheld-alpha<br/>（非正式发布版本） | 手持主包，提供MSDK对手持产品控制的支持。 | implementation 'com.dji:dji-sdk-v5-handheld-alpha:{sdkVersion}' |
| dji-sdk-v5-handheld-provided-alpha<br/>（非正式发布版本） |            手持编译包，提供手持包相关接口。            | compileOnly 'com.dji:dji-sdk-v5-handheld-provided-alpha:{sdkVersion}' |
| dji-sdk-v5-all-alpha<br/>（非正式发布版本） | 全产品主包，提供MSDK对飞机产品、手持产品控制的支持。 | implementation 'com.dji:dji-sdk-v5-all-alpha:{sdkVersion}' |
| dji-sdk-v5-all-provided-alpha<br/>（非正式发布版本） |          全产品编译包，提供全产品包相关接口。          | compileOnly 'com.dji:dji-sdk-v5-all-provided-alpha:{sdkVersion}' |
| dji-sdk-v5-networkImp-alpha | 网络库包，为MSDK提供联网能力（如果不加此依赖，MSDK所有联网功能都会停用，但控制硬件的相关接口还可以正常使用）。 | runtimeOnly 'com.dji:dji-sdk-v5-networkImp-alpha:{sdkVersion}' |

- 如果仅需支持飞机产品，使用：
  ```groovy
  implementation 'com.dji:dji-sdk-v5-aircraft-alpha:{sdkVersion}'
  compileOnly 'com.dji:dji-sdk-v5-aircraft-provided-alpha:{sdkVersion}'
  ```

- 如果仅需支持手持产品，使用：
  ```groovy
  implementation 'com.dji:dji-sdk-v5-handheld-alpha:{sdkVersion}'
  compileOnly 'com.dji:dji-sdk-v5-handheld-provided-alpha:{sdkVersion}'
  ```
  
- 如果需支持飞机产品和手持产品，使用：
  ```groovy
  implementation 'com.dji:dji-sdk-v5-all-alpha:{sdkVersion}'
  compileOnly 'com.dji:dji-sdk-v5-all-provided-alpha:{sdkVersion}'
  ```
  
- 如果需要MSDK使用网络（默认都需要），使用：
  ```groovy
  runtimeOnly 'com.dji:dji-sdk-v5-networkImp-alpha:{sdkVersion}'
  ```

