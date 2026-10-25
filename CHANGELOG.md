# Changelog

All notable changes to this project will be documented in this file.

## [v1.0.0] - 2026-10-26

### 核心特性
- **Glance 艺术字排版引擎**：基于原生 Canvas + TextPaint 离屏位图光栅化，加载开源艺术字体 `SpaceGrotesk-Bold.ttf`，实现货币符号（¥/$）、整数核心与小数共享统一基线排版，紧凑字距（-0.03f），生成约 180KB 高清局部 Bitmap，彻底避免 Binder 1.5MB 溢出。
- **极简主义设计语言**：80% 视觉重心归于数字主体，20% 归于顶栏状态与轻交互；支持 Obsidian 暗色基底（`#E6121316`）与 Alabaster 亮色，提供 DeepSeek 电光蓝（`#4D6BFE`）状态指示灯与低余额预警红灯（`#FF5252`）。
- **零明文凭据安全保障（Zero-Plaintext Policy）**：全面采用 Android KeyStore 硬件根 AES256-GCM 加密存储 API Key（`EncryptedSharedPreferences`），API Key 绝不落盘至明文 DataStore。
- **单向数据流与防抖刷新**：基于 `PreferencesGlanceStateDefinition` 的不可变状态机（UNCONFIGURED / LOADING / SUCCESS / ERROR）；响应点击的 `RefreshBalanceAction` 内置 3 秒硬件级事件防抖，点击瞬间即时展示 `Syncing...` 状态，异步重绘。
- **多币种智能识别与直连**：OkHttp + Coroutines 直连 `https://api.deepseek.com/user/balance`，自动解析总余额、赠送金及充值金，优先匹配可用余额大于 0 的币种（CNY / USD）。
- **WorkManager 后台静默同步**：注册 60 分钟联网周期同步任务（支持 15m/30m/1h/2h 动态调整），解锁手机即见准实时余额；低于警戒阈值时支持系统通知强提醒。
- **配置与实时渲染管理 App**：提供 Material 3 边到边主界面，支持 API Key 密文配置、连通性实时测试、小组件 2×1 所见即所得双色模式预览与一键 Pin 添加到桌面。
