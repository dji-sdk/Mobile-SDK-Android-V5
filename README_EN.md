# DJI Mobile SDK for Android V5 Latest Version V5.20.1

[中文版](README.md)

## What is DJI Mobile SDK V5?

DJI Mobile SDK V5 has a series of APIs to control the software and hardware interfaces of an aircraft. We provide an open source production sample and a tutorial for developers to develop a more competitive drone solution on mobile device. This improves the experience and efficiency of MSDK App development.

Supported Product: [DJI Matrice 5T]() · [DJI Mavic 3TA]() · [Matrice 400]() · [Matrice 4D Enterprise Series]() · [DJI Mini4 PRO](https://www.dji.com/cn/mini-4-pro?from=store-product-page) · [Matrice 4 Enterprise Series](https://enterprise.dji.com/cn/matrice-4-series) · [H30 Series](https://enterprise.dji.com/cn/zenmuse-h30-series) · [DJI Mini3 Pro](https://www.dji.com/cn/mini-3-pro?site=brandsite&from=landing_page) · [DJI Mini3](https://www.dji.com/cn/mini-3?site=brandsite&from=landing_page) · [Mavic 3 Enterprise Series](https://www.dji.com/cn/mavic-3-enterprise) · [M30 Series](https://www.dji.com/matrice-30?site=brandsite&from=nav) · [M300 RTK](https://www.dji.com/matrice-300?site=brandsite&from=nav) · [Matrice 350 RTK](https://enterprise.dji.com/cn/matrice-350-rtk) · [DJI Mavic 3TA]()

## Sample Code

| Sample | Description |
|---|---|
| [open-pilot-plugin-sample](SampleCode-V5/open-pilot-plugin-sample) | Pilot plugin interface demo: custom UI, map / AR, video stream drawing, waypoint, file and data pipeline |
| [open-pilot-flighttask-sample](SampleCode-V5/open-pilot-flighttask-sample) | Flight task management: task dispatch from Web, task receiving on RC, KMZ waypoint execution, resource upload |
| [open-pilot-gasdetection-sample](SampleCode-V5/open-pilot-gasdetection-sample) | Gas detection: task dispatch, PSDK payload access, real-time monitoring and alarm, ship identification and following |
| [open-pilot-patrol-sample](SampleCode-V5/open-pilot-patrol-sample) | Intelligent patrol: waypoint patrol, target recognition and capture, map / AR marking, resource upload |
| [android-sdk-v5-uxsdk](SampleCode-V5/android-sdk-v5-uxsdk) | Scenographic Example (UX SDK). Currently only aircraft are supported |
| [android-sdk-v5-sample](SampleCode-V5/android-sdk-v5-sample) | Basic MSDK Sample App, depends on uxsdk |

## Open Pilot Wizard (OPW)

OPW is an AI toolkit for developing DJI Pilot plugins with OpenCode, Claude Code, or Codex. Describe your requirements in natural language to get help creating plugin projects, adding features, troubleshooting issues, and migrating existing MSDK apps.

Download the [OPW 1.0.0 installer](OpenPilotWizard/open-pilot-wizard-installer-1.0.0.zip) using the raw-file download option on the file page. Give your AI tool the local ZIP path and ask it to follow `INSTALL_AGENT.md` inside the archive. If you have cloned this repository, use the archive in `OpenPilotWizard` directly.

For installation and usage, see the [OPW README](OpenPilotWizard/README.md).

## Integration

For further detail on how to integrate the DJI Android SDK into your Android Studio project, please check the tutorial:
- [Notice of Run MSDK](https://developer.dji.com/doc/mobile-sdk-tutorial/en/quick-start/user-project-caution.html)

## AAR Explanation

> **Notice:** sdkVersion = 5.20.1

| SDK package | Explanation | How to use|
| :---------------: | :-----------------:  | :---------------: |
|     dji-sdk-v5-aircraft      | Aircraft main package, which provides support for MSDK to control the aircraft. | implementation 'com.dji:dji-sdk-v5-aircraft:{sdkVersion}' |
| dji-sdk-v5-aircraft-provided | Aircraft compilation package, which provides interfaces related to the aircraft package. | compileOnly 'com.dji:dji-sdk-v5-aircraft-provided:{sdkVersion}' |
| dji-sdk-v5-networkImp | Network library package, which provides network connection ability for MSDK. Without this dependency, all network functions of MSDK will not work, but the interfaces of hardware control can be used normally. | runtimeOnly 'com.dji:dji-sdk-v5-networkImp:{sdkVersion}' |

- If only the aircraft product is in need to support, please use:

  ```groovy
  implementation 'com.dji:dji-sdk-v5-aircraft:{sdkVersion}'
  compileOnly 'com.dji:dji-sdk-v5-aircraft-provided:{sdkVersion}'
  ```
  
- If the MSDK have to use network(required by default), please use:

  ```groovy
  runtimeOnly 'com.dji:dji-sdk-v5-networkImp:{sdkVersion}'
  ```

### Software License

The DJI Android SDK is dynamically linked with unmodified libraries of <a href=http://ffmpeg.org>FFmpeg</a> licensed under the <a href=https://www.gnu.org/licenses/lgpl-2.1.html.en>LGPLv2.1</a>. The source code of these FFmpeg libraries, the compilation instructions, and the LGPL v2.1 license are provided in [Github](https://github.com/dji-sdk/FFmpeg). The DJI Sample Code V5 in this repo is offered under MIT License.

## Support

You can get support from DJI with the following method:

- Post questions in DJI Developer Forums: [**DEVELOPER SUPPORT**](https://djisdksupport.zendesk.com/hc/en-us/community/topics)
