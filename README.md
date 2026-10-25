# DeepSeek-Balance-Widget-Android

> 极简主义、高对比度社论排版、零冗余交互的 DeepSeek API 余额监控 Android 桌面小组件。
> 基于 **Kotlin 2.0 + Jetpack Glance 1.1+ + Coroutines + WorkManager + Material 3** 构建。

![Banner](app/src/main/res/drawable/ic_launcher_foreground.xml)

---

## ✨ 核心特性

- **80/20 极简排版法则**：80% 视觉重心归于数字主体，20% 归于状态指示与极简交互。
- **离屏 Canvas 艺术字体渲染引擎**：解决 Android RemoteViews 无法直接加载自定义字体的问题。通过离屏 Canvas 与 TextPaint 光栅化加载 `SpaceGrotesk-Bold.ttf`，实现货币符号、整数主体与小数位严格同基线对齐（Baseline Alignment），生成紧凑的高分辨率位图，杜绝 Binder 1.5MB 事务溢出。
- **Zero-Plaintext Policy（零明文凭据安全）**：API Key 仅通过 Android KeyStore 硬件根 AES256-GCM 加密存储于 `EncryptedSharedPreferences`，严禁写入明文 DataStore。
- **3秒硬件级防抖与即刻反馈**：小组件内刷新按钮内置 3000ms 时间戳防抖，点击瞬间即进入 `Syncing...` 状态，异步网络写回后重绘。
- **WorkManager 后台静默同步**：带 `NetworkType.CONNECTED` 约束的周期性后台同步（支持 15m/30m/1h/2h），手机解锁即见准实时余额。
- **低余额警戒预警**：当账户余额低于阈值（默认 ¥5.00）时，组件状态灯自动由电光蓝变为预警红，并触发系统通知。
- **伴生配置与实时预览 App**：Material 3 现代化界面，支持 API 连通性测试、明细（赠送金/充值金）查询、2×1 小组件双色模式动态预览与一键添加至桌面。

---

## 🎨 视觉色彩规范

| 语义角色 | Token 名称 | 暗色模式（Obsidian） | 亮色模式（Alabaster） |
|---|---|---|---|
| 背景底板 | SurfaceBg | `#E6121316` (90% 不透明) | `#F2F7F8FA` (95% 不透明) |
| 主数字文本 | PrimaryInk | `#FFFFFF` | `#111318` |
| 次要文本/小数 | SecondaryInk | `#8E95A5` | `#636B78` |
| 品牌/正常状态 | AccentDeepSeek | `#4D6BFE` | `#3B5BFF` |
| 低余额/异常预警 | StatusWarning | `#FF5252` | `#D32F2F` |

---

## 🏗️ 架构设计

```
+-----------------------------------------------------------------------+
|                           UI Layer (Glance)                           |
|  [DeepSeekBalanceWidget] <--- reads --- [PreferencesGlanceStateDef]   |
|         |                                        ^                    |
|   Clicks [↻] Button                              | Updates State      |
|         v                                        |                    |
|  [RefreshBalanceAction] -------------------------+                    |
+--------------------------------------------------|--------------------+
                                                   |
+--------------------------------------------------|--------------------+
|                       Domain / Background Layer  |                    |
|  [WorkManager (Periodic 60m)] ---> [WidgetSyncRepository]             |
+--------------------------------------------------|--------------------+
                                                   |
+--------------------------------------------------|--------------------+
|                            Data Layer            v                    |
|  [EncryptedSharedPreferences (AES256)]   [OkHttp / DeepSeek API]      |
+-----------------------------------------------------------------------+
```

---

## 📦 构建与安装

```bash
# 运行单元测试
./gradlew testDebugUnitTest

# 构建 Release APK
./gradlew assembleRelease
```

產物位于：`app/build/outputs/apk/release/app-release.apk`
