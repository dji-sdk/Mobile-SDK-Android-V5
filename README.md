# DJI Mobile SDK for Android V5 最新版本 V5.20.1
[English Version](README_EN.md)

## DJI Mobile SDK V5 是什么?

DJI Mobile SDK V5拥有更加简洁易用的无人机硬件控制接口和软件服务接口，开放全开源的生产代码级 Sample 和丰富的教程，为开发者提供了具有竞争力的无人机移动端解决方案，极大的提升开发体验和效率。


当前版本支持机型：[DJI Matrice 5T]() · [DJI Mavic 3TA]() · [Matrice 400]() · [Matrice 4D 行业系列]() · [DJI Mini4 PRO](https://www.dji.com/cn/mini-4-pro?from=store-product-page) · [Matrice 4 行业系列](https://enterprise.dji.com/cn/matrice-4-series) · [禅思 H30系列](https://enterprise.dji.com/cn/zenmuse-h30-series) · [DJI Mini3 Pro](https://www.dji.com/cn/mini-3-pro?site=brandsite&from=landing_page) · [DJI Mini3](https://www.dji.com/cn/mini-3?site=brandsite&from=landing_page) · [Mavic 3 行业系列](https://www.dji.com/cn/mavic-3-enterprise) · [经纬 M30 系列](https://www.dji.com/cn/matrice-30?site=brandsite&from=nav) · [经纬 M300 RTK](https://www.dji.com/cn/matrice-300?site=brandsite&from=nav) · [Matrice 350 RTK](https://enterprise.dji.com/cn/matrice-350-rtk) · [DJI Mavic 3TA]()

## Sample 代码

| Sample | 说明 |
|---|---|
| [open-pilot-plugin-sample](SampleCode-V5/open-pilot-plugin-sample) | Pilot 插件接口演示：自定义 UI、地图 / AR、图传绘制、航线、文件与数据管道 |
| [open-pilot-flighttask-sample](SampleCode-V5/open-pilot-flighttask-sample) | 飞行任务管理：Web 下发任务、遥控器接单、KMZ 航线执行、资源回传 |
| [open-pilot-gasdetection-sample](SampleCode-V5/open-pilot-gasdetection-sample) | 气体检测：任务下发、PSDK 载荷接入、实时监测告警、船舶识别跟随 |
| [open-pilot-patrol-sample](SampleCode-V5/open-pilot-patrol-sample) | 智能巡检作业：航线巡检、目标识别拍照、地图 / AR 标记、资源回传 |
| [android-sdk-v5-uxsdk](SampleCode-V5/android-sdk-v5-uxsdk) | 场景化示例（UX SDK），当前仅支持飞机 |
| [android-sdk-v5-sample](SampleCode-V5/android-sdk-v5-sample) | MSDK 基础 Sample App，依赖 uxsdk |

## Open Pilot Wizard（OPW）

OPW 是面向 DJI Pilot 插件开发的 AI 工具包，可配合 OpenCode、Claude Code 或 Codex，通过自然语言协助创建插件工程、开发功能、排查问题及迁移已有 MSDK 应用。

下载 [OPW 1.0.0 安装包](OpenPilotWizard/open-pilot-wizard-installer-1.0.0.zip)（在文件页面选择下载原始文件），然后把本机 ZIP 路径告诉 AI，让它按照包内的 `INSTALL_AGENT.md` 完成安装。已克隆本仓库时，可直接使用 `OpenPilotWizard` 目录中的安装包。

安装与使用说明请参考 [OPW 中文 README](OpenPilotWizard/README_CN.md)。

## 整合

若您需要整合DJI Mobile SDK到您的 Android Studio项目中，请参考：[运行MSDK注意事项](https://developer.dji.com/doc/mobile-sdk-tutorial/cn/quick-start/user-project-caution.html)


## AAR说明

> **注意：** sdkVersion = 5.20.1

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
  
### 软件证书

DJI Android SDK 与基于<a href=https://www.gnu.org/licenses/lgpl-2.1.html.en>LGPLv2.1</a>协议的<a href=http://ffmpeg.org>FFmpeg</a>库是动态连接的。[Github](https://github.com/dji-sdk/FFmpeg)中提供了FFmpeg 库的源码、编译的指导与 LGPL v2.1的证书。而Mobile SDK V5的样例代码的实现是基于MIT协议。

## 支持

您可以 [填写表单](https://djisdksupport.zendesk.com/hc/zh-cn/community/topics) 以获得DJI的技术支持。

