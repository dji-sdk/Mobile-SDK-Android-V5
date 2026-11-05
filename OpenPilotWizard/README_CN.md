# Open Pilot Wizard（OPW）

[English](README.md)

## OPW 是什么？

Open Pilot Wizard（简称 OPW）是一套帮助你用 AI 开发 DJI Pilot 开放插件的工具包。它为 AI 编程工具提供插件开发知识、工程模板和开发流程，让你通过自然语言描述需求，由 AI 协助创建工程、编写代码和验证结果。

OPW 不是独立的 AI 编程工具，需要配合 **OpenCode、Claude Code 或 Codex** 使用。

## 能解决什么问题？

Pilot 开放插件开发不仅涉及业务代码，还需要处理插件入口、App Key 绑定、SDK 版本匹配和遥控器调试。通用 AI 容易遗漏这些细节，导致代码无法编译，或安装后 Pilot 无法识别插件。

OPW 帮助 AI 先查开发资料，再按流程实施，主要适用于：

- **新建插件**：从空目录生成基础工程、页面和 Pilot 插件入口。
- **开发与维护**：增加功能、调整 UI、排查故障和重构代码。
- **迁移应用**：评估并将已有 MSDK Android 应用迁移为 Pilot 开放插件。
- **能力咨询**：查询 API、错误码和组件用法，不修改工程代码。

开发任务会先给出方案，等你批准后再实现；进度保存在工程中，会话中断后可以继续。

## 怎么安装？

### 1. 准备环境

- macOS（Apple Silicon / Intel），或 Windows 10 22H2 / 11 x64。
- 已安装并能正常使用的 OpenCode、Claude Code 或 Codex，以及可用的模型服务。
- 安装过程需要联网；不必提前安装 Node.js，安装脚本会检查并按需安装。

### 2. 下载安装包

下载 [OPW 1.0.0 安装包](open-pilot-wizard-installer-1.0.0.zip)：打开文件页面后，选择下载原始文件，将 ZIP 保存到本机。如果已克隆本仓库，直接使用本目录中的 `open-pilot-wizard-installer-1.0.0.zip` 即可，无需重复下载。

### 3. 让 AI 安装 OPW

在 AI 工具中新建会话，输入：

```text
我已下载 Open Pilot Wizard 压缩包，路径是 <ZIP 文件路径>。
请按照压缩包中的 INSTALL_AGENT.md 帮我安装 Open Pilot Wizard；需要权限时请请求批准。
```

把 `<ZIP 文件路径>` 替换为已下载或仓库内 ZIP 文件的本机绝对路径。AI 会解压安装包、说明操作并在获得授权后完成安装。登录、密钥配置和许可证确认仍需你自己完成。

> AI 工具需要具备本地文件访问和终端执行能力；仅将 ZIP 上传到聊天窗口不能完成本机安装。

OPW 只需安装一次，多个项目共用。安装后重启终端和 AI 工具，可执行 `opw --version` 查看版本。

## 怎么使用？

### 1. 打开工程并选择入口

- **新建插件**：在 AI 工具中打开一个空目录。
- **已有插件或待迁移的 MSDK 工程**：在工程根目录执行 `opw setup`；OPW 生成的工程无需重复执行。

| AI 工具 | 使用入口 |
| --- | --- |
| OpenCode | 按 Tab 切换到 `pilot-plugin-dev`，或在消息中使用 `@pilot-plugin-dev` |
| Claude Code | 执行 `/output-style pilot-plugin-dev` |
| Codex | 直接调用 `opw` Skill，无需切换智能体 |

### 2. 用自然语言描述需求

**创建插件：**

```text
请使用 OPW 创建一个名为「航线助手」的 Pilot 开放插件，使用默认图标。
先完成基础页面和 Pilot 插件入口，给出方案，等我确认后再实现。
```

**开发功能：**

```text
请为当前插件增加飞行器连接状态显示。
先给出实现方案和验证步骤，等我确认后再改代码。
```

**迁移应用：**

```text
请评估把当前 MSDK 应用迁移为 Pilot 开放插件。
先说明哪些功能可以保留、哪些需要调整，我确认后再开始。
```

AI 会确认必要信息、提出方案，获批后实现并报告构建结果。将 APK 安装到遥控器后，在 Pilot 的插件管理页面启用插件并验证功能。

> 调用 MSDK 能力前，需要使用最终包名申请并绑定 DJI App Key。构建通过不等于运行正常，涉及飞行控制的功能仍需审查代码并进行实机验证。

### 常用命令

| 命令 | 作用 |
| --- | --- |
| `opw --version` | 查看已安装版本 |
| `opw setup` | 为当前工程初始化 OPW 配置 |
| `opw doctor` | 检查工程和 AI 开发能力是否就绪 |
| `opw update` | 将已安装版本的 OPW 配置同步到当前工程，不下载新版本 |

更多说明：[OPW 开发者文档](https://developer.dji.com/doc/mobile-sdk-tutorial/cn/Open-Pilot/ai-coding/overview.html)。
