# DJI Mobile SDK for Android V5 最新Alpha版本 5.8.0-a6

[English Version](README.md)

## Alpha版本声明

1. 为了提高开发者反馈问题的解决效率，我们会优先修复一些严重的问题，在进行问题回归测试后，会第一时间对外发布我们的Alpha版本。
2. 为了能够让开发者尽快体验和测试到我们开发完成但暂未正式发布的功能，在进行产品验收和功能测试后，我们也会第一时间对外发布我们的Alpha版本。
3. Alpha版本未经过严格发布测试，可能存在一些不稳定问题。请开发者根据版本发布记录，自行判断和选择使用Alpha版本。如果有其他问题请第一时间反馈给我们。
4. Alpha版本的所有改动都会同步到正式版本，进行严格的发布测试后对外发布。
5. 不建议开发者直接集成MSDK Alpha版本作为正式版本进行发布。

## 5.8.0-a6发布记录（2023.12.21）

- 一些Bug修复

## 5.8.0-a5发布记录（2023.12.19）

- 优化图传的图像质量

- 一些Bug修复

## 5.8.0-a4发布记录（2023.12.11）

- 删除API接口：ICameraStreamManager.setPriorityEnsureFrameRate(bool)。
> 优先保证帧率优先容易导致花屏的问题，所以不开放该接口。

- 提高解码和编码能力的兼容性。

- 优化图传的稳定性和图像质量

- 不需要需要手动添加此依赖：runtimeOnly "com.dji:dji-sdk-v5-networkImp-alpha:{sdkVersion}"

- 升级Sample工程的APG版本，支持JAVA 17

- 不再需要手动为sdk添加混淆规则，混淆规则会内置在SDK中

- 增加相关功能，以满足当地法规要求

- 一些Bug修复

## 5.8.0-a3发布记录（2023.11.15）

- 新增API接口：ICameraStreamManager.setKeepAliveDecoding(boolean)。
> 如果该接口设置为 false，同时当内部没有任何Surface、ReceiveStreamListener、CameraFrameListener时，解码器会暂停工作以降低后台性能/电量的消耗，但这也会增加首次推送相机数据的延迟。如果设置为true，那么解码器会持续的在工作，这会增加后台性能/电量的消耗，但是会降低首次推送相机数据的延迟。默认值为false。

- 新增API接口：ICameraStreamManager.setPriorityEnsureFrameRate(bool)。
> 如果该接口设置为true，则优先保证帧率，在码流信号较差或者干扰比较强的时候，优先保证图传的帧率而不是质量，好处就是帧率相对平稳，但是可能出现花屏的现象。 如果设置为false，则优先保证帧质量，会自动丢弃那些质量不好的帧。默认值是false。

- 优化多负载下的解码性能。

- 优化图传加载的速度。

- 不需要需要手动添加依赖："com.iqiyi.xcrash:xcrash-android-lib:3.1.0"

## 5.8.0-a2发布记录（2023.11.03）

- 支持相机码流管理类：ICameraStreamManager。
> **注意：**
> IVideoStreamManager将从MSDK 5.8.0版本开始废弃。请使用ICameraStreamManager实现码流管理相关功能。
> 该Alpha版本混淆规则发生变更，请务必同步更新sample.pro文件到自己的工程中。
> 该Alpha版本需要手动添加依赖：com.iqiyi.xcrash:xcrash-android-lib:3.1.0

## 离线文档

- /Docs/Android_API/cn/index.html

## AAR说明

> **注意：** sdkVersion = 5.8.0-a6

| SDK包  <div style="width: 150pt">  | 说明  <div style="width: 200pt">   | 使用方式 <div style="width: 300pt">|
| :---------------: | :-----------------:  | :---------------: |
|     dji-sdk-v5-aircraft-alpha     | 飞机主包，提供MSDK对飞机控制的支持。 | implementation 'com.dji:dji-sdk-v5-aircraft-alpha:{sdkVersion}' |
| dji-sdk-v5-aircraft-provided-alpha | 飞机编译包，提供飞机包相关接口。 | compileOnly 'com.dji:dji-sdk-v5-aircraft-provided-alpha:{sdkVersion}' |
| dji-sdk-v5-networkImp-alpha | 网络库包，为MSDK提供联网能力（如果不加此依赖，MSDK所有联网功能都会停用，但控制硬件的相关接口还可以正常使用）。 | runtimeOnly 'com.dji:dji-sdk-v5-networkImp-alpha:{sdkVersion}' |

- 如果仅需支持飞机产品，使用：
  ```groovy
  implementation "com.dji:dji-sdk-v5-aircraft-alpha:{sdkVersion}"
  compileOnly "com.dji:dji-sdk-v5-aircraft-provided-alpha:{sdkVersion}"
  ```


- 如果需要MSDK使用网络（默认都需要），使用：
  ```groovy
  runtimeOnly "com.dji:dji-sdk-v5-networkImp-alpha:{sdkVersion}"
  ```

