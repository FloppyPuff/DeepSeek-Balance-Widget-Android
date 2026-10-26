# DeepSeek Balance (DeepSeek-Balance-Widget-Android)

> 极简主义、先锋社论排版、零冗余交互的 DeepSeek API 余额监控 Android 原生应用与桌面小组件。
> 基于 **Kotlin 2.0 + Jetpack Compose + Jetpack Glance 1.1+ + Coroutines + WorkManager + Material 3** 构建。

![Banner](app/src/main/res/drawable/ic_launcher_foreground.xml)

---

## ✨ 核心特性

- **多网格响应式自适应排版（2×1、3×1、4×1、2×2、3×2、2×4、3×4、4×4）**：
  - **单行矮高度（2×1、3×1、4×1，高度 < 100dp）**：消除拥挤底栏，数字主体垂直精准居中；宽屏（4×1）将 Granted / Top-up 摘要优雅收纳至顶栏右侧，中屏（3×1）保留顶栏时间与刷新；引入动态自适应降级循环（Auto-Downscale Loop），根据实际像素预算动态调节字号，彻底杜绝数字被腰斩截断。
  - **超高细长卡片（2×4，宽度 < 180dp 且高度 >= 200dp）**：针对 2 列窄窗高屏空间，采用结构化纵向信息流架构（AVAILABLE BALANCE 微标 + 大字号余额 + 细线分割 + 纵向堆叠明细），告别元素悬浮与大面积空白。
  - **大仪表板尺寸（3×4、4×4）**：升级为杂志式大仪表板排版，启用 50sp 高对比艺术字标，配合底部横向宽幅双列明细与常驻同步状态，空间层次充实平衡。
- **离屏 Canvas 艺术字体渲染引擎**：
  - 突破 Android RemoteViews 无法直接从 assets 加载外部 TTF 的限制。通过原生 Canvas + TextPaint 离屏位图光栅化渲染开源艺术字体 `SpaceGrotesk-Bold.ttf`，实现货币符号（¥/$）、超大整数核心与小数位严格同基线对齐（Baseline Alignment），紧凑字间距（-0.03f），生成紧凑的高分辨率位图，彻底规避 Binder 1.5MB 事务溢出。
- **系统日间 / 暗夜模式无缝自适应（Day / Night Mode Auto-Switching）**：
  - 纯资源语义绑定（`values/colors.xml` 与 `values-night/colors.xml`）：底层接入系统级 `ColorProvider(R.color.*)`，配合高动态 Alpha 遮罩位图染色，在系统切换日间（Alabaster 深墨字）与暗夜（Obsidian 亮白字）模式时由 Android RemoteViews 引擎直接自动重绘，零延迟、零能耗、无需手动刷新。
- **Zero-Plaintext Policy（零明文凭据安全保障）**：
  - API Key 仅保存在 Android KeyStore 硬件根 AES256-GCM 加密存储中（`EncryptedSharedPreferences`），API Key 绝不落盘至明文 DataStore 或配置文件中；界面展示时默认采用脱敏掩码（`sk-••••••••abcd`）。
- **自定义低余额警戒线**：
  - 支持预设金额（¥2、¥5、¥10、¥20、¥50）或任意金额自由输入（如 ¥2.5、¥8、¥99 等）。警戒线保存后即刻重新计算全量桌面组件红蓝指示灯，余额告警即时生效。
- **3 秒硬件级防抖与即刻反馈**：
  - 小组件内刷新按键内置 3000ms 时间戳事件防抖，点击瞬间即进入 `Syncing...` 状态，异步完成网络拉取与写回重绘。
- **WorkManager 后台静默周期同步**：
  - 注册带 `NetworkType.CONNECTED` 约束的周期性后台同步（支持 15m/30m/1h/2h/6h），手机解锁即见准实时余额；低于警戒阈值时支持系统通知强提醒。
- **主应用先锋社论美学 UI**：
  - 沉浸式渐变画布背景与电光蓝径向氛围光晕，搭配浮空光影预览卡片（支持 2×1 与 2×2 所见即所得切换）；集成液态弹性微动效设置抽屉，一屏聚焦核心主操作（预览 + 密钥管理 + 立即同步/添加到桌面），界面零冗余、极度克制优雅。

---

## 🎨 视觉色彩规范

| 语义角色 | Token 名称 | 暗色模式（Obsidian） | 亮色模式（Alabaster） |
|---|---|---|---|
| 背景底板 | SurfaceBg | `#E6121316` (90% 不透明) | `#F2F7F8FA` (95% 不透明) |
| 主数字文本 | PrimaryInk | `#FFFFFF` | `#111318` |
| 次要文本/小数 | SecondaryInk | `#8E95A5` | `#636B78` |
| 弱化标签/时间 | MutedInk | `#636B78` | `#9EA6B4` |
| 品牌/正常状态 | AccentDeepSeek | `#4D6BFE` | `#3B5BFF` |
| 低余额/异常预警 | StatusWarning | `#FF5252` | `#D32F2F` |

---

## 🏗️ 架构设计

```
+-----------------------------------------------------------------------------------+
|                              UI Layer (Glance)                                    |
|   [DeepSeekBalanceWidget] <--- reads --- [PreferencesGlanceStateDefinition]      |
|          |                                              ^                         |
|    Clicks [↻] Button                                    | Updates State           |
|          v                                              |                         |
|   [RefreshBalanceAction] -------------------------------+                         |
+---------------------------------------------------------|-------------------------+
                                                          |
+---------------------------------------------------------|-------------------------+
|                              Domain / Sync Layer        |                         |
|   [WorkManager (Periodic)] ----> [WidgetSyncRepository]                           |
|   [MainActivity / User Action] --+ (syncAllWidgets)                               |
+---------------------------------------------------------|-------------------------+
                                                          |
+---------------------------------------------------------|-------------------------+
|                              Data Layer                 v                         |
|   [SecureKeyStore (AES-256-GCM)]          [DeepSeekApiClient (OkHttp/Coroutines)] |
|   [BalanceCache (Local Snapshot)]                                                 |
+-----------------------------------------------------------------------------------+
```

---

## 📦 构建与安装

```bash
# 运行单元测试
./gradlew testDebugUnitTest

# 构建 Release APK
./gradlew assembleRelease
```

Release 产物位于：`app/build/outputs/apk/release/app-release.apk`
SHA-256 校验文件位于同级目录 `app-release.apk.sha256`。

---

## 📄 开源许可证

本项目基于 [Apache License 2.0](LICENSE) 开源。
