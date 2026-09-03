<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="112" alt="Geyser Launcher icon">
</p>

<h1 align="center">Geyser Launcher</h1>

<p align="center">
  在 Android 设备上运行和管理 Geyser Standalone 的轻量启动器。
</p>

## 功能

- 一键启动、停止 Geyser，并根据实际日志区分启动中、运行中和启动失败状态
- 从 `Geyser.jar` 的 Manifest 或 `git.properties` 动态读取已安装版本
- 首次启动时通过 GeyserMC 官方 API 下载最新 Standalone JAR
- 检查和安装 Geyser 更新，下载完成后校验官方 SHA-256
- 图形化编辑 Geyser 配置，同时支持直接编辑 YAML 和语法高亮
- 浏览 Geyser 工作目录，支持导入、导出、重命名和删除文件
- 将目录导出为 ZIP 文件
- 彩色日志显示、复制日志，以及向 Geyser 控制台发送指令
- 自定义 JVM 启动参数
- 前台服务、CPU 唤醒锁和高性能 Wi-Fi 锁，改善后台运行稳定性

## Geyser 下载

本项目不在 APK 中分发 Geyser。首次启动 Geyser 时，应用会访问以下官方接口：

- 构建信息：<https://download.geysermc.org/v2/projects/geyser/versions/latest/builds>
- Standalone 下载：<https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/standalone>

应用选择接口返回的最新 build，并使用该 build 对应的 SHA-256 校验下载文件。下载或校验失败时不会安装该文件；手动更新时，当前可用 JAR 会在替换完成前保留。

## 系统要求

- Android 8.0（API 26）或更高版本
- ARM64 设备（`arm64-v8a`）
- 首次启动 Geyser 和检查更新时需要连接互联网
- Geyser 运行期间不能替换或更新 JAR

## 构建

需要 JDK 17 和 Android SDK 34。项目使用 Gradle Wrapper，无需单独安装 Gradle。

仓库中的内置 ARM64 JRE 以分片保存；Gradle 会在构建前自动重组并校验 SHA-256，无需手动处理。

Windows PowerShell：

```powershell
$env:GRADLE_USER_HOME = (Resolve-Path '.gradle-user').Path
.\gradlew.bat :app:assembleDebug --no-daemon
```

Linux 或 macOS：

```bash
GRADLE_USER_HOME="$PWD/.gradle-user" ./gradlew :app:assembleDebug --no-daemon
```

Debug APK 输出到：

```text
app/build/outputs/apk/debug/app-debug.apk
```

运行单元测试和 Lint：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug --no-daemon
```

## 项目结构

```text
app/src/main/java/dev/lisfox/geyserlauncher/
├── data/          # 配置、文件和 Geyser 分发管理
├── runtime/       # 运行状态、日志和 JVM 参数
├── ui/            # Jetpack Compose 页面与组件
├── viewmodel/     # 页面状态和操作入口
└── GeyserService.java
```

## 签名

Release 构建默认生成未签名 APK。请使用自己的 Android 签名证书完成发布签名，并妥善备份私钥和密码。项目的 `.gitignore` 会排除本地 `.signing` 目录、JKS 文件和构建产物。

## 第三方项目

- [Geyser](https://github.com/GeyserMC/Geyser) 由 GeyserMC 开发，本应用仅通过其官方服务下载并启动 Standalone 版本。
- [Zalith Launcher](https://docs.zalithlauncher.cn/) 使用了由 Zalith Launcher 编译的 JRE25，并内置在APP中。

本项目不是 GeyserMC 官方项目，也不受 GeyserMC 认可或维护。
