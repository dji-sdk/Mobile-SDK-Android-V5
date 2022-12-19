# DJI Mobile SDK for Android V5 最新版本 V5.2.0

[English Version](README.md)

## DJI Mobile SDK V5 是什么?

DJI Mobile SDK V5拥有更加简洁易用的无人机硬件控制接口和软件服务接口，开放全开源的生产代码级 Sample 和丰富的教程，为开发者提供了具有竞争力的无人机移动端解决方案，极大的提升开发体验和效率。


当前版本支持机型：
* [DJI Mavic 3 多光谱版](https://ag.dji.com/cn/mavic-3-m?site=brandsite&from=nav)
* [DJI Mavic 3 行业系列](https://www.dji.com/cn/mavic-3-enterprise)
* [经纬 M30 系列](https://www.dji.com/cn/matrice-30?site=brandsite&from=nav)
* [经纬 M300 RTK](https://www.dji.com/cn/matrice-300?site=brandsite&from=nav)

## 工程目录介绍

```
├── Docs
│   ├── API-Diff
│   └── Android_API
├── LICENSE.txt
├── README.md
├── README_CN.md
└── SampleCode-V5
    ├── android-sdk-v5-as
    ├── android-sdk-v5-sample
    │   ├── app-aircraft
    │   ├── app-all
    │   ├── app-handheld
    │   ├── module-aircraft
    │   ├── module-common
    │   └── module-handheld
    └── android-sdk-v5-uxsdk
```

### API 差异
- [5.1.0_5.2.0_android_diff.html](https://dji-sdk.github.io/Mobile-SDK-Android-V5/Docs/API-Diff/5.1.0_5.2.0_android_diff.html)
- [5.0.0_5.1.0_android_diff.html](https://dji-sdk.github.io/Mobile-SDK-Android-V5/Docs/API-Diff/5.0.0_5.1.0_android_diff.html)
- [5.0.0_beta3_5.0.0_android_diff](https://dji-sdk.github.io/Mobile-SDK-Android-V5/Docs/API-Diff/5.0.0_beta3_5.0.0_android_diff.html)
- [5.0.0_beta2_5.0.0_beta3_android_diff](https://dji-sdk.github.io/Mobile-SDK-Android-V5/Docs/API-Diff/5.0.0_beta2_5.0.0_beta3_android_diff.html)

### 离线文档
- [API](https://dji-sdk.github.io/Mobile-SDK-Android-V5/Docs/Android_API/cn/index.html)

### 软件证书

DJI Android SDK 与基于<a href=https://www.gnu.org/licenses/lgpl-3.0.html.en>LGPLv3.0</a>协议的<a href=http://ffmpeg.org>FFmpeg</a>库是动态连接的。[Github](https://github.com/dji-sdk/FFmpeg)中提供了FFmpeg 库的源码、编译的指导与 LGPL v3.0的证书。而Mobile SDK V5的样例代码的实现是基于MIT协议。

### Sample说明

Sample分为3部分：

- 基础模块：提供各产品包的基础操作。
- 场景化Sample：提供对飞机的场景化Sample支持。
- App模块：用于配置3个App，分别是飞机、手持、全产品。

详细配置请参考[settings.gradle](SampleCode-V5/android-sdk-v5-as/settings.gradle)。

基础模块：

- sample-module-common：飞机App、手持App的通用代码。
- sample-module-aircraft：飞机App特有代码，依赖sample-module-common。
- sample-module-handheld：手持App特有代码，依赖sample-module-common。

场景化Sample：

- uxsdk：场景化Sample，当前仅支持飞机(dji-sdk-v5-aircraft 或 dji-sdk-v5-all)。


App模块：

- sample-app-aircraft：编译飞机App，依赖sample-module-aircraft、uxsdk。
- sample-app-handheld：编译手持App，依赖sample-module-handheld。
- sample-app-all：编译全产品App，依赖sample-module-aircraft、sample-module-handheld、uxsdk。



## 整合

若您需要整合DJI Mobile SDK到您的 Android Studio项目中，请参考：[运行MSDK注意事项](https://developer.dji.com/doc/mobile-sdk-tutorial/cn/quick-start/user-project-caution.html)


## AAR说明

> **注意：** sdkVersion = 5.2.0

| SDK包  <div style="width: 150pt">  | 说明  <div style="width: 200pt">   | 使用方式 <div style="width: 300pt">|
| :---------------: | :-----------------:  | :---------------: |
|     dji-sdk-v5-aircraft      | 飞机主包，提供MSDK对飞机控制的支持。 | implementation 'com.dji:dji-sdk-v5-aircraft:{sdkVersion}' |
| dji-sdk-v5-aircraft-provided | 飞机编译包，提供飞机包相关接口。 | compileOnly 'com.dji:dji-sdk-v5-aircraft-provided:{sdkVersion}' |
| dji-sdk-v5-handheld<br/>（非正式发布版本） | 手持主包，提供MSDK对手持产品控制的支持。 | implementation 'com.dji:dji-sdk-v5-handheld:{sdkVersion}' |
| dji-sdk-v5-handheld-provided<br/>（非正式发布版本） |            手持编译包，提供手持包相关接口。            | compileOnly 'com.dji:dji-sdk-v5-handheld-provided:{sdkVersion}' |
| dji-sdk-v5-all<br/>（非正式发布版本） | 全产品主包，提供MSDK对飞机产品、手持产品控制的支持。 | implementation 'com.dji:dji-sdk-v5-all:{sdkVersion}' |
| dji-sdk-v5-all-provided<br/>（非正式发布版本） |          全产品编译包，提供全产品包相关接口。          | compileOnly 'com.dji:dji-sdk-v5-all-provided:{sdkVersion}' |
| dji-sdk-v5-networkImp | 网络库包，为MSDK提供联网能力（如果不加此依赖，MSDK所有联网功能都会停用，但控制硬件的相关接口还可以正常使用）。 | runtimeOnly 'com.dji:dji-sdk-v5-networkImp:{sdkVersion}' |

- 如果仅需支持飞机产品，使用：

  ```groovy
  implementation 'com.dji:dji-sdk-v5-aircraft:{sdkVersion}'
  compileOnly 'com.dji:dji-sdk-v5-aircraft-provided:{sdkVersion}'
  ```

- 如果仅需支持手持产品，使用：

  ```groovy
  implementation 'com.dji:dji-sdk-v5-handheld:{sdkVersion}'
  compileOnly 'com.dji:dji-sdk-v5-handheld-provided:{sdkVersion}'
  ```
- 如果需支持飞机产品和手持产品，使用：
  ```groovy
  implementation 'com.dji:dji-sdk-v5-all:{sdkVersion}'
  compileOnly 'com.dji:dji-sdk-v5-all-provided:{sdkVersion}'
  ```
- 如果需要MSDK使用网络（默认都需要），使用：
  ```groovy
  runtimeOnly 'com.dji:dji-sdk-v5-networkImp:{sdkVersion}'
  ```

## 支持

您可以 [填写表单](https://djisdksupport.zendesk.com/hc/zh-cn/community/topics) 以获得DJI的技术支持。


