# DJI Mobile SDK for Android V5 最新版本 V5.17.0
[English Version](README.md)

## DJI Mobile SDK V5 是什么?

DJI Mobile SDK V5拥有更加简洁易用的无人机硬件控制接口和软件服务接口，开放全开源的生产代码级 Sample 和丰富的教程，为开发者提供了具有竞争力的无人机移动端解决方案，极大的提升开发体验和效率。


当前版本支持机型：
* [DJI Mavic 3TA]()
* [Matrice 400]()
* [Matrice 4D 行业系列]()
* [DJI Mini4 PRO](https://www.dji.com/cn/mini-4-pro?from=store-product-page)
* [Matrice 4 行业系列](https://enterprise.dji.com/cn/matrice-4-series)
* [禅思 H30系列](https://enterprise.dji.com/cn/zenmuse-h30-series)
* [DJI Mini3 Pro](https://www.dji.com/cn/mini-3-pro?site=brandsite&from=landing_page)
* [DJI Mini3](https://www.dji.com/cn/mini-3?site=brandsite&from=landing_page)
* [Mavic 3 行业系列](https://www.dji.com/cn/mavic-3-enterprise)
* [经纬 M30 系列](https://www.dji.com/cn/matrice-30?site=brandsite&from=nav)
* [经纬 M300 RTK](https://www.dji.com/cn/matrice-300?site=brandsite&from=nav)
* [Matrice 350 RTK](https://enterprise.dji.com/cn/matrice-350-rtk)
* [DJI Mavic 3TA]()

## 工程目录介绍

```
├── Docs
│   └── Android_API
├── LICENSE.txt
├── README.md
├── README_CN.md
└── SampleCode-V5
    ├── android-sdk-v5-as
    ├── android-sdk-v5-sample
    └── android-sdk-v5-uxsdk
```



### 软件证书

DJI Android SDK 与基于<a href=https://www.gnu.org/licenses/lgpl-2.1.html.en>LGPLv2.1</a>协议的<a href=http://ffmpeg.org>FFmpeg</a>库是动态连接的。[Github](https://github.com/dji-sdk/FFmpeg)中提供了FFmpeg 库的源码、编译的指导与 LGPL v2.1的证书。而Mobile SDK V5的样例代码的实现是基于MIT协议。

### Sample说明

Sample分为3部分：

- 场景化示例：提供对飞机的场景化支持。
- Sample模块：提供一个飞机Sample App。

详细配置请参考[settings.gradle](SampleCode-V5/android-sdk-v5-as/settings.gradle)。

场景化示例：

- uxsdk：场景化示例，当前仅支持飞机。


Sample模块：

- sample：编译飞机sample App，依赖uxsdk。

## 整合

若您需要整合DJI Mobile SDK到您的 Android Studio项目中，请参考：[运行MSDK注意事项](https://developer.dji.com/doc/mobile-sdk-tutorial/cn/quick-start/user-project-caution.html)


## AAR说明

> **注意：** sdkVersion = 5.17.0

| SDK包| 说明| 使用方式|
| :---------------: | :-----------------:  | :---------------: |
|     dji-sdk-v5-aircraft      | 飞机主包，提供MSDK对飞机控制的支持。 | implementation 'com.dji:dji-sdk-v5-aircraft:{sdkVersion}' |
| dji-sdk-v5-aircraft-provided | 飞机编译包，提供飞机包相关接口。 | compileOnly 'com.dji:dji-sdk-v5-aircraft-provided:{sdkVersion}' |
| dji-sdk-v5-networkImp | 网络库包，为MSDK提供联网能力（如果不加此依赖，MSDK所有联网功能都会停用，但控制硬件的相关接口还可以正常使用）。 | runtimeOnly 'com.dji:dji-sdk-v5-networkImp:{sdkVersion}' |

- 如果仅需支持飞机产品，使用：

  ```groovy
  implementation 'com.dji:dji-sdk-v5-aircraft:{sdkVersion}'
  compileOnly 'com.dji:dji-sdk-v5-aircraft-provided:{sdkVersion}'
  ```
- 如果需要MSDK使用网络（默认都需要），使用：
  ```groovy
  runtimeOnly 'com.dji:dji-sdk-v5-networkImp:{sdkVersion}'
  ```

## 支持

您可以 [填写表单](https://djisdksupport.zendesk.com/hc/zh-cn/community/topics) 以获得DJI的技术支持。

