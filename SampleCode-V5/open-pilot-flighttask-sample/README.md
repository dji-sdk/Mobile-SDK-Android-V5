# 飞行任务管理

基于 DJI Mobile SDK V5 与 DJI Pilot 开放能力的飞行任务管理示例，演示从 Web 下发任务、遥控器接收工单、执行 KMZ 航线到回传资源的完整流程。

项目包含 Android 插件与 Node.js 演示后端，适合作为任务管理、航线执行和数据同步功能的开发参考。

示例通过 MSDK Aircraft SDK 使用 DJI Pilot 提供的开放能力。SDK 版本与依赖配置以对应发布工程为准。

## 功能

- 任务管理：任务列表、详情、接收、取消及状态回传。
- 航线执行：KMZ 航线下载、加载、执行及暂停、恢复、停止。
- 资源管理：新照片检测、媒体下载、本地资源查看与上传队列管理。
- Pilot 集成：`REMOTE_PILOT` 接入、悬浮窗操作与连接状态展示。
- Web 管理台：任务创建与下发、遥控器登记、航线及回传文件查看。

## 项目信息

| 项目 | 值 |
|---|---|
| 中文显示名 | 飞行任务管理 |
| 英文显示名 | Flight Task Management |
| 开发者 | DJI Inc. |
| APK 包名 / 插件 ID | `com.dji.sample.flighttask` |
| 源码命名空间 | `dji.sample.flighttask` |
| 插件入口 | `dji.sample.flighttask.PluginActivity` |
| 默认后端地址 | `http://127.0.0.1:8080` |

Pilot 使用 APK 包名作为插件 ID，优先读取 `dji_open_pilot_plugin_*` 字符串资源展示插件信息，契约 XML 中的名称、开发者和描述为兜底值。源码命名空间与 APK 包名可以不同。

## 环境要求

- Android Studio、JDK 17、Android SDK Platform 35 与 Build Tools 34.0.0。
- 使用 Android 工程提供的 Gradle Wrapper，并安装工程 `gradle.properties` 中 `NDK_VERSION` 指定的 NDK。
- 支持 Pilot 开放能力的 MSDK Aircraft SDK 二进制发布包及配套依赖，版本应与目标 Pilot 匹配。
- 安装兼容 DJI Pilot 的遥控器；APK 仅包含 `arm64-v8a`，最低 Android API 为 24。
- 后端建议使用 Node.js 24；Docker Compose 为可选部署方式。USB 联调需要 ADB。

## 快速开始

后端命令从示例仓库根目录开始执行；Android 构建命令在包含 `settings.gradle` 和 Gradle Wrapper 的 Android 工程根目录执行。启动后端后保持该终端运行，构建与安装另开终端。

### 1. 准备工程

在 Android Studio 中打开发布包中的 Android 工程，使用该版本提供的 SDK 二进制依赖配置，并在工程根目录的 `local.properties` 中设置 Android SDK 路径：

```properties
sdk.dir=/your/Android/sdk
```

### 2. 配置 AppKey 与后端地址

在 Android 工程根目录的 `gradle.properties` 中配置：

```properties
FLIGHTTASK_SAMPLE_API_KEY=<你的 DJI AppKey>
FLIGHT_TASK_SERVER_URL=http://127.0.0.1:8080
```

模块通过 Gradle 项目属性读取 AppKey，再用 `manifestPlaceholders` 注入 Manifest 的 `${API_KEY}`；不读取模块级 `local.properties` 中的 AppKey。AppKey 未配置时注入空值，SDK 注册无法完成。后端地址在编译时写入 `BuildConfig`，修改后需重新构建并安装。

debug 与 release 均使用本模块的 `msdkkeystore.jks`，别名为 `msdkkeystore`。申请 AppKey 时使用 APK 包名 `com.dji.sample.flighttask` 和实际签名指纹。

### 3. 启动后端

```bash
cd open-pilot-flighttask-sample/server
npm start
```

服务默认监听 `127.0.0.1:8080`，浏览器打开 <http://127.0.0.1:8080/>。无需安装第三方 Node.js 依赖或构建前端。

在另一个终端检查服务：

```bash
curl http://127.0.0.1:8080/health
```

返回 `{"status":"ok"}` 表示服务正常。Docker 部署、数据保存及 API 说明见 [`server/README.md`](server/README.md)。

### 4. 构建并安装插件

```bash
./gradlew :open-pilot-flighttask-sample:assembleDebug
```

APK 位于本模块的 `build/outputs/apk/debug/open-pilot-flighttask-sample-debug.apk`。将下面的路径替换为实际模块位置后安装：

```bash
adb install -r /path/to/open-pilot-flighttask-sample/build/outputs/apk/debug/open-pilot-flighttask-sample-debug.apk
```

### 5. 连接遥控器并下发任务

启用遥控器 USB 调试，连接电脑后执行：

```bash
adb devices
adb reverse tcp:8080 tcp:8080
adb reverse --list
```

1. 在遥控器上打开“飞行任务管理”，等待 SDK 注册及 Pilot 连接成功。
2. 插件取得遥控器 SN 后访问后端，管理台即可选择该遥控器。
3. 在管理台创建任务、上传 `.kmz` 航线，并保存下发。
4. 在插件中接收任务、执行航线，并在任务、资源、传输页面查看状态。
5. 在管理台查看回传的航线和媒体文件。

USB 重连或遥控器重启后，如无法连接后端，请重新执行 `adb reverse`。连接多台设备时，为 ADB 命令添加 `-s <遥控器序列号>`。

## 目录结构

```text
open-pilot-flighttask-sample/
├── build.gradle          # Android 应用构建与配置注入
├── src/main/
│   ├── java/dji/sample/flighttask/
│   │   ├── data/         # 工单、资源和云端配置模型
│   │   ├── manager/      # 任务、航线、媒体与上传管理
│   │   ├── models/       # 连接与业务 ViewModel
│   │   └── pages/        # 任务、资源、传输页面
│   └── res/              # 页面资源、多语言文案与插件契约
├── server/               # Node.js 后端与 Web 管理台
└── spec/                 # 模块设计与需求说明
```

## 开发与验证

后端测试：

```bash
cd open-pilot-flighttask-sample/server
npm test
```

Android 构建使用前述 `assembleDebug` 命令。航线执行和媒体链路需要结合实际支持的飞行器、遥控器与 Pilot 版本联调。

扩展业务时可从 `TaskManager`、`WaylineManager`、`MediaManager` 和 `AssetUploadManager` 入手；架构与模块职责见 [`spec/00-overview.md`](spec/00-overview.md)。

## 常见问题

- **SDK 注册失败**：检查 `FLIGHTTASK_SAMPLE_API_KEY`、APK 包名、签名指纹及遥控器网络。
- **插件未出现或 Pilot 不兼容**：检查 Pilot 版本、插件契约与连接状态。
- **后端不可达**：检查服务是否启动、`FLIGHT_TASK_SERVER_URL` 与 `adb reverse` 端口是否一致。
- **任务列表为空**：确认任务已下发到当前遥控器 SN；草稿不会出现在插件任务列表。
