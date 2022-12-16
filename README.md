# DJI Mobile SDK for Android V5 Latest Version 5.2.0

[中文版](README_CN.md)

## What is DJI Mobile SDK V5?

DJI Mobile SDK V5 has a series of APIs to control the software and hardware interfaces of an aircraft. We provide an open source production sample and a tutorial for developers to develop a more competitive drone solution on mobile device. This improves the experience and efficiency of MSDK App development.

Supported Product:
* [DJI Mavic 3M](https://ag.dji.com/cn/mavic-3-m?site=brandsite&from=nav)
* [DJI Mavic 3 Enterprise Series](https://www.dji.com/cn/mavic-3-enterprise)
* [M30 Series](https://www.dji.com/matrice-30?site=brandsite&from=nav)
* [M300 RTK](https://www.dji.com/matrice-300?site=brandsite&from=nav)


## Project Directory Introduction

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

### API Difference
- [5.1.0_5.2.0_android_diff.html](https://dji-sdk.github.io/Mobile-SDK-Android-V5/Docs/API-Diff/5.1.0_5.2.0_android_diff.html)
- [5.0.0_5.1.0_android_diff.html](https://dji-sdk.github.io/Mobile-SDK-Android-V5/Docs/API-Diff/5.0.0_5.1.0_android_diff.html)
- [5.0.0_beta3_5.0.0_android_diff](https://dji-sdk.github.io/Mobile-SDK-Android-V5/Docs/API-Diff/5.0.0_beta3_5.0.0_android_diff.html)
- [5.0.0_beta2_5.0.0_beta3_android_diff](https://dji-sdk.github.io/Mobile-SDK-Android-V5/Docs/API-Diff/5.0.0_beta2_5.0.0_beta3_android_diff.html)

### Offline Documentation
- [API](https://dji-sdk.github.io/Mobile-SDK-Android-V5/Docs/Android_API/en/index.html)

### Software License

The DJI Android SDK is dynamically linked with unmodified libraries of <a href=http://ffmpeg.org>FFmpeg</a> licensed under the <a href=https://www.gnu.org/licenses/lgpl-3.0.html.en>LGPLv3.0</a>. The source code of these FFmpeg libraries, the compilation instructions, and the LGPL v3.0 license are provided in [Github](https://github.com/dji-sdk/FFmpeg). The DJI Sample Code V5 in this repo is offered under MIT License.


### Sample Explanation

Sample can be divided into three parts:

- Basic module: Provides basic operation of every poduct package.
- Scenographic Sample: Provides scenographic sample support of aircraft.
- App Module: Used to configure three Apps, which are aircraft, handheld product and all products.

For detailed configuration, please refer to [settings.gradle](SampleCode-V5/android-sdk-v5-as/settings.gradle).

Basic module:

- sample-module-common: Common code of aircraft App and handheld App.
- sample-module-aircraft: Unique code of aircraft App, which depends on sample-module-common.
- sample-module-handheld：Unique code of handheld App, which depends on sample-module-common.

Scenographic Sample：

- uxsdk: Scenographic Sample. Currently only aircraft(`dji-sdk-v5-aircraft` or `dji-sdk-v5-all`) are supported.

App module:

- sample-app-aircraft：Compile aircraft App, which depends on sample-module-aircraft, uxsdk.
- sample-app-handheld：Compile handheld App, which depends on sample-module-handheld.
- sample-app-all：Compile all product App, which depends on sample-module-aircraft, sample-module-handheld, uxsdk.



## Integration

For further detail on how to integrate the DJI Android SDK into your Android Studio project, please check the tutorial:
- [Notice of Run MSDK](https://developer.dji.com/doc/mobile-sdk-tutorial/en/quick-start/user-project-caution.html)

## AAR Explanation

> **Notice:** sdkVersion = 5.2.0

| SDK package  <div style="width: 150pt">  | Explanation  <div style="width: 200pt">   | How to use <div style="width: 300pt">|
| :---------------: | :-----------------:  | :---------------: |
|     dji-sdk-v5-aircraft      | Aircraft main package, which provides support for MSDK to control the aircraft. | implementation 'com.dji:dji-sdk-v5-aircraft:{sdkVersion}' |
| dji-sdk-v5-aircraft-provided | Aircraft compilation package, which provides interfaces related to the aircraft package. | compileOnly 'com.dji:dji-sdk-v5-aircraft-provided:{sdkVersion}' |
| dji-sdk-v5-handheld<br/>(Not official released version) | Handheld main package, which provides support for MSDK to control handheld product. | implementation 'com.dji:dji-sdk-v5-handheld:{sdkVersion}' |
| dji-sdk-v5-handheld-provided<br/>(Not official released version) |     Handheld compilation package, which provides interfaces related to handheld package.             | compileOnly 'com.dji:dji-sdk-v5-handheld-provided:{sdkVersion}' |
| dji-sdk-v5-all<br/>(Not official released version) | Main package of all product, which provides support for MSDK to control aircraft and handheld product. | implementation 'com.dji:dji-sdk-v5-all:{sdkVersion}' |
| dji-sdk-v5-all-provided<br/>(Not official released version) |    Compilation package of all product, which provides interfaces related to all product.          | compileOnly 'com.dji:dji-sdk-v5-all-provided:{sdkVersion}' |
| dji-sdk-v5-networkImp | Network library package, which provides network connection ability for MSDK. Without this dependency, all network functions of MSDK will not work, but the interfaces of hardware control can be used normally. | runtimeOnly 'com.dji:dji-sdk-v5-networkImp:{sdkVersion}' |

- If only the aircraft product is in need to support, please use:

  ```groovy
  implementation 'com.dji:dji-sdk-v5-aircraft:{sdkVersion}'
  compileOnly 'com.dji:dji-sdk-v5-aircraft-provided:{sdkVersion}'
  ```

- If only the handheld product is in need to support, please use:

  ```groovy
  implementation 'com.dji:dji-sdk-v5-handheld:{sdkVersion}'
  compileOnly 'com.dji:dji-sdk-v5-handheld-provided:{sdkVersion}'
  ```
- If the aircraft and handheld product are in need to support, please use:
  ```groovy
  implementation 'com.dji:dji-sdk-v5-all:{sdkVersion}'
  compileOnly 'com.dji:dji-sdk-v5-all-provided:{sdkVersion}'
  ```
- If the MSDK have to use network(required by default), please use:
  ```groovy
  runtimeOnly 'com.dji:dji-sdk-v5-networkImp:{sdkVersion}'
  ```



## Support

You can get support from DJI with the following method:

- Post questions in DJI Developer Forums: [**DEVELOPER SUPPORT**](https://djisdksupport.zendesk.com/hc/en-us/community/topics)
