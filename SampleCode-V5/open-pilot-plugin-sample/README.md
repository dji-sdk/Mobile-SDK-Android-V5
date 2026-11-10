# Pilot 开放插件 Sample

[English Version](README_EN.md)

基于 DJI Mobile SDK V5 与 DJI Pilot 开放能力的接口演示插件，展示自定义 UI、地图 / AR、图传绘制、航线、文件及数据管道等能力的接入和调用方式。

项目以单项能力示例为主，适合作为 Pilot 插件初始化、接口调用、远端界面交互与状态订阅的开发参考，无需启动业务后端。

示例通过 MSDK Aircraft SDK 使用 DJI Pilot 提供的开放能力。SDK 版本与依赖配置以对应发布工程为准。

## 功能

当前主页提供以下 11 个示例入口：

| 示例 | 内容 | 源码目录 |
|---|---|---|
| 自定义 UI 组件 | 在 Pilot 界面叠加布局、文本、按钮、输入框、图片、列表等组件，演示属性更新与交互 | `sampleitem/remoteviewpage/` |
| AR 投射 | 点、线、面、圆与雷达效果，演示元素上传、更新、删除及点击回调 | `sampleitem/armanager/` |
| 地图控件 | 标记及几何控件、批量控件、图层管理、地图元素与状态订阅 | `sampleitem/mapmanager/` |
| 对话框 | 提示、确认、输入、单选与多选对话框，以及用户操作回调 | `sampleitem/dialogmanager/` |
| 图传绘制 | 在图传画面叠加矩形框、多边形和文字 | `sampleitem/cameradrawing/` |
| 页面跳转 | 跳转到 Pilot 指定页面及插件页面 | `sampleitem/navigatemanager/` |
| 航线任务 | 本地 KMZ 文件接入、任务控制、断点续飞与状态订阅 | `sampleitem/waypoint/` |
| Key Manager | Key 值读取、设置、监听及动作调用 | `sampleitem/keymanager/` |
| 文件传输 | 向插件私有目录或 Pilot 日志目录上传文件；删除按钮暂时置灰 | `sampleitem/filetransfer/` |
| MOP 数据管道 | 管道连接、字节读写与分块大数据传输 | `sampleitem/pipeline/` |
| 媒体文件任务 | 浏览相机媒体、预览、原文件下载、XMP 读取与热成像照片解析 | `sampleitem/mediatask/` |

表中源码目录相对于 `src/main/java/com/dji/open/pilot/plugin/`。主页同时展示 SDK 注册、Pilot 服务连接及已获取的国家码状态；具体能力是否可用取决于飞行器、载荷和 Pilot 版本。

当前地图示例屏蔽文字标注、比例尺缩放和设置中心点入口，左侧菜单及悬浮窗只提供地图控件、批量控件、图层管理和地图元素。

当前 5.20.1 的 base/client 不提供富文本属性和按码流控制 AR 投射的接口。自定义 UI 中的 HTML 文案在客户端解析后以纯文本显示，仍支持语言刷新；AR 投射开关页的显示 / 隐藏按钮置灰。地图状态头仅回显当前协议提供的就绪状态和缩放值。

地图元素示例记录成功创建的 UUID，重进后通过查询恢复对应对象；删除回执后会延迟查询核验，未确认消失的目标保留供重试。AR 显隐回执仅表示请求已提交，实际注记刷新由 Pilot 实现。未记录 UUID 的历史元素无法自动认领。

## 项目信息

| 项目 | 值 |
|---|---|
| 中文显示名 | Pilot 开放插件 Sample |
| 英文显示名 | Open Pilot Plugin Sample |
| 开发者（契约 XML） | DJI Inc. |
| APK 包名 / 插件 ID | `com.dji.open.pilot.plugin` |
| 源码命名空间 | `com.dji.open.pilot.plugin` |
| 插件入口 | `com.dji.open.pilot.plugin.main.MainActivity` |
| Application | `com.dji.open.pilot.plugin.main.PluginApplication` |
| SDK 连接模式 | `REMOTE_PILOT` |

Pilot 通过 Manifest 中的 `dji.open.pilot.client.OPEN_PILOT_PERMISSION_META_DATA` 标记发现插件，使用 APK 包名作为插件 ID。本示例的契约位于 [`src/main/res/xml/dji_open_pilot_plugin.xml`](src/main/res/xml/dji_open_pilot_plugin.xml)，插件名称通过 `dji_open_pilot_plugin_name` 字符串资源提供中英文显示。

## 环境要求

- Android Studio、JDK 17、Android SDK Platform 35 与 Build Tools 34.0.0。
- 使用 Android 工程提供的 Gradle Wrapper；源码工程需安装工程 `gradle.properties` 中 `NDK_VERSION` 指定的 NDK。
- 支持 Pilot 开放能力的 MSDK Aircraft SDK 二进制发布包及配套依赖，版本应与目标 Pilot 匹配。
- 安装兼容 DJI Pilot 的遥控器；APK 仅包含 `arm64-v8a`，最低 Android API 为 24。
- USB 安装与联调需要 ADB；首次 SDK 注册需要遥控器联网。
- 航线、相机媒体及 MOP 数据管道等链路需要对应能力的飞行器或载荷配合。

## 快速开始

Android 构建命令在包含 `settings.gradle` 和 Gradle Wrapper 的 Android 工程根目录执行。当前仓库的该目录为 `android-sdk-v5-as/`，示例模块位于其同级目录 `open-pilot-plugin-sample/`；发布包以实际工程布局为准。

### 1. 准备工程

在 Android Studio 中打开 Android 工程，使用对应版本提供的 SDK 依赖配置，并在 Android 工程根目录的 `local.properties` 中设置 Android SDK 路径：

```properties
sdk.dir=/your/Android/sdk
```

源码工程使用模块的 `build.gradle`；二进制发布工程按发布包提供的构建配置接入 Aircraft SDK。`aarbuild.gradle` 用于发布构建，其中 `utils`、`open-pilot-base` 与 `open-pilot-client` 已包含在相应 Aircraft AAR 中，依赖配置应遵循配套工程。

### 2. 配置 AppKey

在 Android 工程根目录的 `gradle.properties` 中配置：

```properties
OPEN_PLUGIN_SAMPLE_API_KEY=<你的 DJI AppKey>
```

模块通过 Gradle 项目属性读取 AppKey，再用 `manifestPlaceholders` 注入 Manifest 的 `${API_KEY}`，对应元数据为 `com.dji.sdk.API_KEY`；不读取模块级 `local.properties` 中的 AppKey。未配置时注入空值，SDK 注册无法完成。修改后需重新构建并安装。

debug 与 release 均使用本模块的 `msdkkeystore.jks`，别名为 `msdkkeystore`。申请 AppKey 时使用 APK 包名 `com.dji.open.pilot.plugin` 和实际签名指纹。

### 3. 构建并安装插件

在 Android 工程根目录执行：

```bash
./gradlew :open-pilot-plugin-sample:assembleDebug
```

APK 位于本模块的 `build/outputs/apk/debug/open-pilot-plugin-sample-debug.apk`。启用遥控器 USB 调试并连接电脑，当前仓库布局下可执行：

```bash
adb devices
adb install -r ../open-pilot-plugin-sample/build/outputs/apk/debug/open-pilot-plugin-sample-debug.apk
```

发布包的模块位置如有不同，请将安装路径替换为实际 APK 路径。连接多台设备时，为 ADB 命令添加 `-s <遥控器序列号>`。

### 4. 启动并查看示例

1. 在遥控器上打开兼容版本的 DJI Pilot，再打开“Pilot 开放插件 Sample”。
2. 等待主页状态显示“已注册”和“已连接”。主页进入示例前会检查这两个状态。
3. 选择需要验证的模块。UI、地图 / AR、对话框等示例可选择左侧用例并点击“查看效果”，在 Pilot 页面和悬浮窗中执行操作、查看结果。
4. 通过悬浮窗中的“返回插件 APP”回到当前插件页面，继续切换示例。
5. 航线、媒体和数据管道示例按各自页面配置文件、相机或管道参数，并查看操作回调与状态。

航线示例读取本地 KMZ 文件时，Android 11 及以上需要允许本应用访问所有文件；按页面引导进入系统设置授权。

## 目录结构

```text
open-pilot-plugin-sample/
├── README.md
├── build.gradle              # 源码工程应用构建与 AppKey 注入
├── aarbuild.gradle           # 二进制发布构建配置
├── AR_CLICK_CASES.md         # AR 点击与选择态联调用例
└── src/main/
    ├── AndroidManifest.xml  # 插件发现标记、AppKey 与页面声明
    ├── java/com/dji/open/pilot/plugin/
    │   ├── main/            # Application、SDK 初始化与主页模块入口
    │   ├── common/          # 共享 ViewModel、数据监听、绘制与导航封装
    │   └── sampleitem/      # 按开放能力划分的 11 个示例
    ├── assets/
    │   ├── plugin/          # 远端图标与演示图片
    │   ├── mop/             # 数据管道传输样本
    │   └── rvcase_*.html    # RemoteView 组件预览页面
    └── res/                 # 布局、中英文文案、预览图与插件契约
```

## 开发与验证

### 开发入口

- SDK 接入：[`PluginApplication.kt`](src/main/java/com/dji/open/pilot/plugin/main/PluginApplication.kt) 启动初始化；[`MainViewModel.kt`](src/main/java/com/dji/open/pilot/plugin/main/MainViewModel.kt) 配置 `REMOTE_PILOT`、注册 SDK、处理 Pilot 连接状态，并初始化共享监听和主页悬浮窗。
- 示例入口：在 `MainViewModel.buildModuleItems()` 中维护模块卡片，页面由 [`MainActivity.kt`](src/main/java/com/dji/open/pilot/plugin/main/MainActivity.kt) 打开。新增入口需同步声明 Activity 和文案；标记为 `TODO` 的卡片会被主页过滤。
- 远端绘制：复用 [`common/drawing/`](src/main/java/com/dji/open/pilot/plugin/common/drawing/) 中的公共封装，参考对应模块的 ViewModel 与 Drawer。
- 状态与导航：共享数据监听位于 [`common/data/`](src/main/java/com/dji/open/pilot/plugin/common/data/)，页面跳转封装见 [`NavigationHelper.kt`](src/main/java/com/dji/open/pilot/plugin/common/NavigationHelper.kt)。
- 多语言：文案位于 `res/values/strings.xml` 与 `res/values-zh-rCN/strings.xml`，新增文案时保持资源 key 一致。

### 验证流程

Android 构建使用前述 `assembleDebug` 命令。安装后先核对 SDK 注册与 Pilot 连接状态，再进入目标模块验证接口回调、远端显示及状态更新。

地图 / AR、图传绘制、航线、媒体下载与 MOP 通信需结合实际支持的飞行器、载荷、遥控器和 Pilot 版本联调。AR 元素点击和选择态的具体观察步骤见 [`AR_CLICK_CASES.md`](AR_CLICK_CASES.md)。

## 常见问题

- **SDK 注册失败**：检查 `OPEN_PLUGIN_SAMPLE_API_KEY`、APK 包名、签名指纹及遥控器网络；修改 AppKey 后重新构建安装，并查看 `MainViewModel` 的注册失败日志。
- **主页显示未连接或无法进入示例**：确认兼容版本的 Pilot 已启动，检查插件发现标记、SDK / Pilot 版本配套关系与 `onRemoteServerCompatibleError` 日志。示例入口要求 SDK 注册成功且 Pilot 服务已连接。
- **找不到本地 KMZ 或无法读取**：检查文件路径，并在 Android 11 及以上系统设置中授予本应用所有文件访问权限。
- **远端图标或绘制效果未出现**：检查 Pilot 连接、资源上传结果、当前 Pilot 页面及对应投射开关；结合操作回调确认目标 Pilot 支持该能力。
- **航线、媒体或 MOP 操作失败**：检查飞行器 / 载荷连接、相机或管道参数、功能支持情况，并查看页面返回的错误信息。
