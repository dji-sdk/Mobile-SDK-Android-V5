# DJI Mobile SDK for Android V5 最新Alpha版本 5.8.0-a2

[English Version](README.md)

## Alpha版本声明

1. 为了提高开发者反馈问题的解决效率，我们会优先修复一些严重的问题，在进行问题回归测试后，会第一时间对外发布我们的Alpha版本。
2. 为了能够让开发者尽快体验和测试到我们开发完成但暂未正式发布的功能，在进行产品验收和功能测试后，我们也会第一时间对外发布我们的Alpha版本。
3. Alpha版本未经过严格发布测试，可能存在一些不稳定问题。请开发者根据版本发布记录，自行判断和选择使用Alpha版本。如果有其他问题请第一时间反馈给我们。
4. Alpha版本的所有改动都会同步到正式版本，进行严格的发布测试后对外发布。
5. 不建议开发者直接集成MSDK Alpha版本作为正式版本进行发布。

## 发布日期

2023.11.03

## 发布记录

- 支持相机码流管理类：ICameraStreamManager。

> **注意：**
> IVideoStreamManager将从MSDK 5.8.0版本开始废弃。请使用ICameraStreamManager实现码流管理相关功能。
> 该Alpha版本混淆规则发生变更，请务必同步更新sample.pro文件到自己的工程中。
> 该Alpha版本需要手动添加依赖：com.iqiyi.xcrash:xcrash-android-lib:3.1.0

## 离线文档

- /Docs/Android_API/cn/index.html

## AAR说明

> **注意：** sdkVersion = 5.8.0-a2

| SDK包  <div style="width: 150pt">  | 说明  <div style="width: 200pt">   | 使用方式 <div style="width: 300pt">|
| :---------------: | :-----------------:  | :---------------: |
|     dji-sdk-v5-aircraft-alpha     | 飞机主包，提供MSDK对飞机控制的支持。 | implementation 'com.dji:dji-sdk-v5-aircraft-alpha:{sdkVersion}' |
| dji-sdk-v5-aircraft-provided-alpha | 飞机编译包，提供飞机包相关接口。 | compileOnly 'com.dji:dji-sdk-v5-aircraft-provided-alpha:{sdkVersion}' |
| dji-sdk-v5-networkImp-alpha | 网络库包，为MSDK提供联网能力（如果不加此依赖，MSDK所有联网功能都会停用，但控制硬件的相关接口还可以正常使用）。 | runtimeOnly 'com.dji:dji-sdk-v5-networkImp-alpha:{sdkVersion}' |

- 如果仅需支持飞机产品，使用：
  ```groovy
  implementation 'com.dji:dji-sdk-v5-aircraft-alpha:{sdkVersion}'
  compileOnly 'com.dji:dji-sdk-v5-aircraft-provided-alpha:{sdkVersion}'
  ```


- 如果需要MSDK使用网络（默认都需要），使用：
  ```groovy
  runtimeOnly 'com.dji:dji-sdk-v5-networkImp-alpha:{sdkVersion}'
  ```

