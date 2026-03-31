
# DJI Mobile SDK for Android V5 Latest Version V5.17.0
[English Version](README.md)

## What is DJI Mobile SDK V5?

DJI Mobile SDK V5 provides a simpler and more user-friendly drone hardware control interface and software service interface, open-sourced production-level sample code, and rich tutorials. It offers developers a competitive mobile drone solution, greatly improving development experience and efficiency.

Currently supported models:
* [DJI Mavic 3TA]()
* [Matrice 400]()
* [Matrice 4D Industry Series]()
* [DJI Mini4 PRO](https://www.dji.com/cn/mini-4-pro?from=store-product-page)
* [Matrice 4 Industry Series](https://enterprise.dji.com/cn/matrice-4-series)
* [Zenmuse H30 Series](https://enterprise.dji.com/cn/zenmuse-h30-series)
* [DJI Mini3 Pro](https://www.dji.com/cn/mini-3-pro?site=brandsite&from=landing_page)
* [DJI Mini3](https://www.dji.com/cn/mini-3?site=brandsite&from=landing_page)
* [Mavic 3 Enterprise Series](https://www.dji.com/cn/mavic-3-enterprise)
* [Matrice 30 Series](https://www.dji.com/cn/matrice-30?site=brandsite&from=nav)
* [Matrice 300 RTK](https://www.dji.com/cn/matrice-300?site=brandsite&from=nav)
* [Matrice 350 RTK](https://enterprise.dji.com/cn/matrice-350-rtk)
* [DJI Mavic 3TA]()

## Project Directory Overview

```
├── Docs
│   └── Android_API
├── LICENSE.txt
├── README.md
├── README_CN.md
└── SampleCode-V5
  ├── android-sdk-v5-as
  ├── android-sdk-v5-sample
  └── android-sdk-v5-uxsdk
```


### Software License

DJI Android SDK is dynamically linked with the <a href=https://www.gnu.org/licenses/lgpl-2.1.html.en>LGPLv2.1</a>-licensed <a href=http://ffmpeg.org>FFmpeg</a> library. The source code, build instructions, and LGPL v2.1 license for FFmpeg are available on [Github](https://github.com/dji-sdk/FFmpeg). The sample code implementation of Mobile SDK V5 is under the MIT license.

### Sample Description

The sample is divided into 3 parts:

- Scenario-based examples: Provide scenario support for aircraft.
- Sample module: Provides an aircraft sample app.

For detailed configuration, please refer to [settings.gradle](SampleCode-V5/android-sdk-v5-as/settings.gradle).

Scenario-based example:

- uxsdk: Scenario-based example, currently only supports aircraft.


Sample module:

- sample: Compiles the aircraft sample app, depends on uxsdk.

## Integration

If you need to integrate DJI Mobile SDK into your Android Studio project, please refer to: [MSDK Usage Notes](https://developer.dji.com/doc/mobile-sdk-tutorial/cn/quick-start/user-project-caution.html)


## AAR Description

> **Note:** sdkVersion = 5.17.0

| SDK Package | Description | Usage |
| :---------------: | :-----------------:  | :---------------: |
| dji-sdk-v5-aircraft | Main aircraft package, provides MSDK support for aircraft control. | implementation 'com.dji:dji-sdk-v5-aircraft:{sdkVersion}' |
| dji-sdk-v5-aircraft-provided | Aircraft compile package, provides related interfaces for the aircraft package. | compileOnly 'com.dji:dji-sdk-v5-aircraft-provided:{sdkVersion}' |
| dji-sdk-v5-networkImp | Network library package, provides network capabilities for MSDK (if this dependency is not added, all network functions of MSDK will be disabled, but hardware control interfaces can still be used normally). | runtimeOnly 'com.dji:dji-sdk-v5-networkImp:{sdkVersion}' |

- If you only need to support aircraft products, use:

  ```groovy
  implementation 'com.dji:dji-sdk-v5-aircraft:{sdkVersion}'
  compileOnly 'com.dji:dji-sdk-v5-aircraft-provided:{sdkVersion}'
  ```
- If you need MSDK to use the network (usually required), use:
  ```groovy
  runtimeOnly 'com.dji:dji-sdk-v5-networkImp:{sdkVersion}'
  ```

## Support

You can [fill out the form](https://djisdksupport.zendesk.com/hc/zh-cn/community/topics) to get technical support from DJI.

