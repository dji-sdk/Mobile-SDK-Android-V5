# 气体检测

基于 DJI Mobile SDK V5、DJI Pilot 开放能力与 PSDK 载荷数据通道的气体检测示例，演示检测任务下发、载荷测量接入、实时监测与告警、船舶识别 / 跟随以及检测记录回传。

本次发布 Android 插件及可复用的 Android 侧 PSDK 通信库，并保留 Node.js 演示后端用于任务联调。载荷侧 PSDK 应用由设备开发者自行实现，不随本项目发布。

示例通过 MSDK Aircraft SDK 使用 DJI Pilot 提供的开放能力。SDK 版本与依赖配置以对应发布工程为准。

## 功能

- 任务管理：工单接收与拒绝、作业预检、任务状态同步及结束记录生成。
- 气体监测：通道目录配置、载荷在线状态、测量数据、趋势与告警展示。
- 作业控制：自动 FlyTo、船舶识别、跟随与手动作业切换。
- 检测记录：任务级数据聚合、本地上传队列与离线补传。
- Pilot 集成：`REMOTE_PILOT` 接入、悬浮窗状态与操作。
- 负载调试：独立二级页面，支持 `psdk-bridge` 协议请求与原始 Hex / UTF-8 字节收发。
- Web 管理台：检测任务创建与下发、遥控器登记及检测记录查看。

## 项目信息

| 项目 | 值 |
|---|---|
| 中文显示名 | 气体检测 |
| 英文显示名 | Gas Detection |
| 开发者 | DJI Inc. |
| APK 包名 / 插件 ID | `com.dji.sample.gasdetection` |
| 源码命名空间 | `com.dji.sample.gasdetection` |
| 插件入口 | `com.dji.sample.gasdetection.MainActivity` |
| 默认后端地址 | `http://127.0.0.1:18082` |

Pilot 使用 APK 包名作为插件 ID，契约 XML 不声明 ID。插件名称、开发者和描述优先从 `dji_open_pilot_plugin_*` 字符串资源按语言读取，契约 XML 为兜底值。

## 环境要求

- Android Studio、JDK 17、Android SDK Platform 35 与 Build Tools 34.0.0。
- 使用 Android 工程提供的 Gradle Wrapper，并安装工程 `gradle.properties` 中 `NDK_VERSION` 指定的 NDK。
- 支持 Pilot 开放能力的 MSDK Aircraft SDK 二进制发布包及配套依赖，版本应与目标 Pilot 匹配。
- 安装兼容 DJI Pilot 的遥控器；APK 仅包含 `arm64-v8a`，最低 Android API 为 24。
- 后端建议使用 Node.js 24；USB 联调需要 ADB。
- 测量链路需要支持 PSDK 数据通道、能够按本文测量协议上报数据的载荷。Android 工程构建不依赖载荷侧 PSDK 工程或工具链。

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
GASDETECTION_SAMPLE_API_KEY=<你的 DJI AppKey>
GASDETECTION_SERVER_URL=http://127.0.0.1:18082
```

模块通过 Gradle 项目属性读取 AppKey，再用 `manifestPlaceholders` 注入 Manifest 的 `${DJI_API_KEY}`；不读取模块级 `local.properties` 中的 AppKey。AppKey 未配置时注入空值，SDK 注册无法完成。后端地址在编译时写入 `BuildConfig`，修改后需重新构建并安装；未配置时回退到 `http://127.0.0.1:18082`。

debug 与 release 均使用 Android 工程根目录的 `msdkkeystore.jks`，别名为 `msdkkeystore`。申请 AppKey 时使用 APK 包名 `com.dji.sample.gasdetection` 和实际签名指纹。

### 3. 启动后端

```bash
cd open-pilot-gasdetection-sample/server
npm start
```

服务默认监听 `127.0.0.1:18082`，浏览器打开 <http://127.0.0.1:18082/>。无需安装第三方 Node.js 依赖或构建前端。

在另一个终端检查服务：

```bash
curl http://127.0.0.1:18082/health
```

返回 `{"status":"ok"}` 表示服务正常。任务字段、状态流转及检测记录 API 见 [`server/README.md`](server/README.md)。

### 4. 构建并安装插件

```bash
./gradlew :open-pilot-gasdetection-sample:assembleDebug
```

APK 位于本模块的 `build/outputs/apk/debug/open-pilot-gasdetection-sample-debug.apk`。将下面的路径替换为实际模块位置后安装：

```bash
adb install -r /path/to/open-pilot-gasdetection-sample/build/outputs/apk/debug/open-pilot-gasdetection-sample-debug.apk
```

### 5. 连接遥控器并开始联调

启用遥控器 USB 调试，连接电脑后执行：

```bash
adb devices
adb reverse tcp:18082 tcp:18082
adb reverse --list
```

1. 在遥控器上打开“气体检测”，等待 SDK 注册及 Pilot 连接成功。
2. 插件取得遥控器 SN 后访问后端，在管理台为该遥控器创建并下发检测任务。
3. 在插件“任务”页接收任务并查看预检结果；“设备”页查看载荷及通道测量；“传输”页查看检测记录与诊断。
4. 接入已实现下述气体测量协议的载荷，在设备页核对端口在线状态与通道测量。
5. 结束任务后，在插件和 Web 管理台查看生成及回传的检测记录。

USB 重连或遥控器重启后，如无法连接后端，请重新执行 `adb reverse`。连接多台设备时，为 ADB 命令添加 `-s <遥控器序列号>`。

## 载荷通信与调试

### 气体测量上行

Android 插件通过 `IPayloadManager` 数据回调接收原始字节，由 `GasDemoFrameAdapter` 和 `JsonMeasurementCodec` 解码 v1 紧凑 JSON 测量帧；通道名称、单位和量程由 `src/main/assets/gas-inspection-config.json` 补全。

设备端需按 Android 解码契约上报 UTF-8 JSON，例如：

```json
{"v":1,"s":"gas-session-001","n":0,"t":1700000000001,"p":[null,null,null,null,0],"g":[[1,657,1],[2,462,1],[3,499,1]],"f":4,"a":0}
```

- `v`：协议版本；`s`：会话标识；`n`：序列号；`t`：设备采样时间戳（Unix 毫秒）。
- `p`：位置数组；示例中的四个 `null` 和有效标志 `0` 表示位置未知。
- `g`：通道数组，每项为 `[wireId, 数值, 质量码]`，`wireId` 与 Android 通道目录对应。
- `f`：作业阶段码；`a`：自动模式标志。示例中的 `f=4`、`a=0` 表示手动作业。

示例用于说明格式，实际联调时应使用当前采样时间和设备测量值。该链路直接接收原始 JSON，不附加 `psdk-bridge` 帧头。

完整字段校验和编码约束以 Android 侧的 [`JsonMeasurementCodec.kt`](src/main/java/com/dji/sample/gasdetection/gas/wire/JsonMeasurementCodec.kt)、[`GasWireLimits.kt`](src/main/java/com/dji/sample/gasdetection/gas/wire/GasWireLimits.kt) 与[通道配置](src/main/assets/gas-inspection-config.json)为准。

### 协议与原始字节调试

从插件主页进入负载调试页，选择实际在线端口与包长，再点击“应用端口 / 开始接收”：

- 默认协议模式使用 Android 侧 `psdk-bridge`，发送 `READ_DEVICE_INFO`（命令编号 `2`）并显示协议状态及请求结果；设备端需实现匹配的请求 / 响应协议。
- 取消协议模式后使用原始字节收发；Hex 输入 `00 41 FF` 实际发送 3 个字节。
- 协议模式与原始模式在调试页中互斥切换，离开页面时移除页面监听。

可复用通信库的接入、请求 / 响应、事件和生命周期约定见 [`psdk-bridge/README.md`](psdk-bridge/README.md)。

## 目录结构

```text
open-pilot-gasdetection-sample/
├── build.gradle          # Android 应用构建与配置注入
├── src/main/
│   ├── java/com/dji/sample/gasdetection/
│   │   ├── gas/          # 任务、测量、告警、识别、跟随与检测记录
│   │   └── ui/           # 页面状态映射与负载调试页
│   ├── assets/           # 气体通道与作业配置
│   └── res/              # 页面资源、多语言文案与插件契约
├── server/               # Node.js 后端与 Web 管理台
└── psdk-bridge/          # 可复用 Android 侧 PSDK 通信库
```

## 开发与验证

后端测试：

```bash
cd open-pilot-gasdetection-sample/server
npm test
```

Android 构建使用前述 `assembleDebug` 命令。测量通道、自动飞行、识别与跟随能力需要结合实际支持的载荷、飞行器、遥控器与 Pilot 版本联调。

扩展气体类型时更新通道配置和测量适配器；扩展设备命令时参考 `psdk-bridge`。界面文案使用 `values/strings.xml` 和 `values-zh/strings.xml`，保持双语资源 key 一致。

## 常见问题

- **SDK 注册失败**：检查 `GASDETECTION_SAMPLE_API_KEY`、APK 包名、签名指纹及遥控器网络。
- **后端不可达**：检查 `GASDETECTION_SERVER_URL`、服务监听端口与 `adb reverse`。
- **没有测量数据**：检查载荷端口是否在线、帧格式是否匹配、通道配置是否加载成功，并查看端口诊断。
- **协议请求未就绪**：检查 SDK 注册、Pilot、飞行器、载荷连接及功能开放状态，具体条件见通信库说明。
- **检测记录未回传**：检查传输页的上传状态、当前遥控器 SN 和后端连接，恢复连接后查看补传结果。
