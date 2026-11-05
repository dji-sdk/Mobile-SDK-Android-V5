# 智能巡检作业

基于 DJI Mobile SDK V5 与 DJI Pilot 开放能力的智能巡检作业示例，演示任务下发、航线巡检、目标识别与拍照、地图 / AR 标记以及资源回传。

项目包含 Android 插件与 Node.js 演示后端，适合作为智能巡检业务编排与 Pilot 集成的开发参考。

示例通过 MSDK Aircraft SDK 使用 DJI Pilot 提供的开放能力。SDK 版本与依赖配置以对应发布工程为准。

## 功能

- 任务管理：工单接收、状态回传、航线加载与起飞确认。
- 航线作业：航线执行、暂停、恢复、停止及返航请求。
- 智能巡检：目标识别、拍照编排与拍照点记录。
- Pilot 展示：悬浮窗操作、地图 / AR 标记与目的地 FlyTo。
- 资源回传：媒体下载、上传队列与传输进度展示。
- Web 管理台：任务创建与下发、遥控器登记、航线及媒体文件查看。

## 项目信息

| 项目 | 值 |
|---|---|
| 中文显示名 | 智能巡检作业 |
| 英文显示名 | SmartPatrol |
| 开发者 | DJI Inc. |
| APK 包名 / 插件 ID | `dji.sample.patrol` |
| 源码命名空间 | `dji.sample.patrol` |
| 插件入口 | `dji.sample.patrol.PluginActivity` |
| 默认后端地址 | `http://127.0.0.1:18081` |

Pilot 使用 APK 包名作为插件 ID，优先读取 `dji_open_pilot_plugin_*` 字符串资源展示插件信息，契约 XML 中的名称、开发者和描述为兜底值。

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
PATROL_SAMPLE_API_KEY=<你的 DJI AppKey>
PATROL_SERVER_URL=http://127.0.0.1:18081
```

模块通过 Gradle 项目属性读取 AppKey，再用 `manifestPlaceholders` 注入 Manifest 的 `${DJI_API_KEY}`；不读取模块级 `local.properties` 中的 AppKey。AppKey 未配置时注入空值，SDK 注册无法完成。后端地址在编译时写入 `BuildConfig`，修改后需重新构建并安装。

`PATROL_SERVER_URL` 未配置时回退到 `http://127.0.0.1:18081`，不使用飞行任务模块的 `FLIGHT_TASK_SERVER_URL`。

debug 与 release 均使用本模块的 `msdkkeystore.jks`，别名为 `msdkkeystore`。申请 AppKey 时使用 APK 包名 `dji.sample.patrol` 和实际签名指纹。

### 3. 启动后端

```bash
cd open-pilot-patrol-sample/server
npm start
```

服务默认监听 `127.0.0.1:18081`，浏览器打开 <http://127.0.0.1:18081/>。无需安装第三方 Node.js 依赖或构建前端。

在另一个终端检查服务：

```bash
curl http://127.0.0.1:18081/health
```

返回 `{"status":"ok"}` 表示服务正常。Docker 部署、数据保存及 API 说明见 [`server/README.md`](server/README.md)。

### 4. 构建并安装插件

```bash
./gradlew :open-pilot-patrol-sample:assembleDebug
```

APK 位于本模块的 `build/outputs/apk/debug/open-pilot-patrol-sample-debug.apk`。将下面的路径替换为实际模块位置后安装：

```bash
adb install -r /path/to/open-pilot-patrol-sample/build/outputs/apk/debug/open-pilot-patrol-sample-debug.apk
```

### 5. 连接遥控器并下发任务

启用遥控器 USB 调试，连接电脑后执行：

```bash
adb devices
adb reverse tcp:18081 tcp:18081
adb reverse --list
```

1. 在遥控器上打开“智能巡检作业”，等待 SDK 注册及 Pilot 连接成功。
2. 插件取得遥控器 SN 后访问后端，管理台即可选择该遥控器。
3. 在管理台创建任务、上传 `.kmz` 航线，并保存下发。
4. 在插件中接收任务、加载航线，确认起飞后执行巡检。
5. 在插件的任务、资源、传输页面及 Pilot 悬浮窗查看作业状态，在管理台查看回传文件。

USB 重连或遥控器重启后，如无法连接后端，请重新执行 `adb reverse`。连接多台设备时，为 ADB 命令添加 `-s <遥控器序列号>`。

## 目录结构

```text
open-pilot-patrol-sample/
├── build.gradle          # Android 应用构建与配置注入
├── src/main/
│   ├── java/dji/sample/patrol/
│   │   ├── data/         # 工单、目标、拍照点与资源模型
│   │   ├── manager/      # 航线、识别、拍照、地图 / AR 与上传管理
│   │   ├── models/       # 连接与业务 ViewModel
│   │   ├── pages/        # 任务、资源、传输页面
│   │   └── pilot/        # SDK 初始化与 Pilot 接入
│   └── res/              # 页面资源、多语言文案与插件契约
└── server/               # Node.js 后端与 Web 管理台
```

## 开发与验证

后端测试：

```bash
cd open-pilot-patrol-sample/server
npm test
```

Android 构建使用前述 `assembleDebug` 命令。目标识别、FlyTo、地图 / AR 和航线能力需结合实际支持的飞行器、遥控器与 Pilot 版本联调。

扩展作业流程可从 `RouteController`、`RecognitionManager`、`CaptureController` 和 `PatrolAssetReporter` 入手；SDK 生命周期由 `SdkBootstrapper` 统一管理。

## 常见问题

- **SDK 注册失败**：检查 `PATROL_SAMPLE_API_KEY`、APK 包名、签名指纹及遥控器网络。
- **后端不可达或连接到了其他 Demo**：检查 `PATROL_SERVER_URL`，巡检服务使用独立的 `18081` 端口。
- **任务列表为空**：确认任务已下发到当前遥控器 SN；草稿不会出现在插件任务列表。
- **智能能力不可用**：检查对应飞行器与 Pilot 是否支持该能力，并查看插件连接状态和操作反馈。
