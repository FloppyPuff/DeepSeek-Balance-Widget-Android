# DeepSeek Balance

> 专注、安全、极简的 DeepSeek API 余额监控 Android 原生应用与桌面小组件。  
> 直连官方 API，无中间服务器，保障 API 密钥与账户隐私。

---

## 📱 核心功能

- **全网格尺寸桌面小组件**：
  - 全面适配主流 Android 桌面启动器网格（`2×1`、`3×1`、`4×1`、`2×2`、`3×2`、`2×4`、`3×4`、`4×4` 等）。
  - **单行防裁切**：在 2×1、3×1、4×1 等低高度网格下动态缩放字号并居中排版，彻底避免数字被腰斩截断。
  - **高窗纵向重排**：在 2×4 等纵向网格下自动重构为结构化纵向信息流，展示余额及明细。
- **多币种智能识别**：
  - 自动解析人民币（`¥`）与美元（`$`），优先展示有可用余额的币种。
- **账户余额明细拆解**：
  - 完整展示总可用余额、赠送金额度（Granted）以及充值余额（Top-up）。
- **自定义低余额预警**：
  - 支持快捷选择（¥2、¥5、¥10、¥20、¥50）或自由输入任意自定义警戒金额（如 ¥2.5、¥8、¥99 等）。
  - 余额低于警戒线时，小组件状态灯由电光蓝即刻转为预警红灯。
- **后台自动同步与手动即时刷新**：
  - **后台静默同步**：基于 Android WorkManager 周期任务，支持 15m / 30m / 1h / 2h / 6h 自由设置。
  - **手动即时刷新**：桌面小组件右上角与主应用内均可一键触发刷新，内置 3 秒防抖保护。
- **系统日间 / 暗夜模式自动适配**：
  - 原生跟随系统切换浅色（Alabaster）与深色（Obsidian）主题，由系统渲染引擎直接自动重绘。
- **零明文安全保障（Zero-Plaintext Policy）**：
  - API Key 仅保存在本机 Android KeyStore 硬件根 AES-256-GCM 加密存储中，绝不上传任何第三方服务器，界面默认脱敏显示（`sk-••••••••abcd`）。
- **在线检测更新**：
  - 设置页面内一键检测 GitHub Releases 最新版本，弹窗查看更新日志并直接调起浏览器下载最新 APK。

---

## 🏗️ 技术栈

- **开发语言**：Kotlin 2.0
- **UI 框架**：Jetpack Compose + Material 3
- **小组件框架**：Jetpack Glance 1.1+ (AppWidget)
- **后台调度**：Android Jetpack WorkManager
- **安全存储**：AndroidX Security Crypto (EncryptedSharedPreferences / AES256-GCM)
- **网络直连**：OkHttp 4 + Kotlin Coroutines

---

## 📦 本地编译与构建

```bash
# 运行单元测试
./gradlew testDebugUnitTest

# 构建 Release APK
./gradlew assembleRelease
```

- 编译生成的安装包位于：`app/build/outputs/apk/release/app-release.apk`
- 签名配置：在项目根目录创建 `signing.properties` 配置专属 Release Keystore，或通过环境变量注入。

---

## 👨‍💻 开源与作者

- **GitHub 仓库**：[FloppyPuff/DeepSeek-Balance-Widget-Android](https://github.com/FloppyPuff/DeepSeek-Balance-Widget-Android)
- **开发者**：[FloppyPuff](https://github.com/FloppyPuff)
- **开源协议**：[Apache License 2.0](LICENSE)
