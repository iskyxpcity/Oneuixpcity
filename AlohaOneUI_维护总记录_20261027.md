# Aloha OneUI 维护总记录 — 2026-10-27

## 本轮继续推进（HST）

- 仓库：`iskyxpcity/Oneuixpcity`
- 工作分支：`test/voyage-theme-1to1-20261026`
- 约束：不修改/合并 `main`；不以自创近似外观冒充 Voyage 1:1；视觉参数必须追溯到原版反编译方法、资源或实测对照。
- 已重新读取 `AlohaOneUI_维护总记录_20261026.md`，并复核测试分支的 `ExpressiveSwitch.kt`、`SegmentedSliderItem.kt`、`SegmentedList.kt` 与 `Theme.kt`。
- Switch 当前源码仍是项目内硬编码的 8 个样式分支。已有的 SwitchColors 映射、禁用颜色处理和 thumbContent 动画定位属于 Aloha 内部行为修复，不证明与 Voyage 原版 1:1。
- Slider 仍需按 Voyage 原始 `y8f0` 绘制方法及 `kdj0` 枚举构造参数逐项对照，不能以 4 个分支或 CI 通过标记完成。
- `SegmentedList.kt` 有多个 `LocalMaterialShadowContainers` 阴影应用点；继续沿实际设置回调、状态仓库和 CompositionLocal 传递链核查用户反馈的阴影详情开关问题。
- Voyage APK 的目标 DEX 类已定位，但当前环境没有可用的 JADX/baksmali 等工具；现有自制 DEX 指令解码仍不足以可靠恢复完整方法体。本轮不根据不完整反汇编猜改视觉参数。
- 本轮源码复核未发现可安全依据原版方法体完成的 Switch/Slider 视觉参数修改；不以猜测制造新的差异。阶段性 APK Build #1649 不是最终 1:1 交付。
- 待办顺序：①八种 Switch 原版对照；②四种 Slider 原始绘制；③50 个真实图标形状及 None/Random 状态；④Smooth G² 几何算法。继续核查阴影详情开关、主题详情/列表框体、导航/语言/显示与文字/动效与触感页面、父标题矢量图标/底纹及全局组件接入。
- 最终交付仍须匹配最终 HEAD，并核实包名/版本、APK 大小与 SHA-256、Owner 证书 SHA-256、V2 签名及 ZIP 完整性。当前没有新的最终 APK 可宣称完成。


## 本轮实际提交：撤销未经原版证据支持的间距调整

- 维护记录提交：`1c6f2c4eb54387057c7dce2d2fa520e098d0a516`。
- 产品源码修正提交：`57cd523b415a22564fedc45f325159b32fc73218`，将 `SegmentedList.kt` 中先前未经原版对照验证的 `Arrangement.spacedBy(4.dp)` 恢复为变更前的 `2.dp`。
- 产品源码修正提交：`a3397439dbc3cdb7c8c045451defaf8171f6fb7e`，将 `ColorPaletteScreenMaterial.kt` 中 7 处先前未经原版对照验证的顶部间距 `8.dp` 恢复为变更前的 `4.dp`。
- 这两项是撤销缺乏原版依据的推测性间距修改，并非声称 2dp/4dp 已通过 Voyage 原版像素验收。后续应以原版页面布局/截图/方法参数为准。
- 两项源码修改已提交至测试分支，`main` 未修改。当前尚未核验这些最新提交对应的 Actions 终态，也没有新的 APK 产物可交付；必须继续查询最新构建并修复任何失败。
- 阴影详情设置页目前六个开关分别连接到独立的 `uiState` 字段和 `actions.onSet...` 回调，`Theme.kt` 也把对应字段提供为 CompositionLocal。仍需继续沿设置仓库的读写链与具体阴影应用点核查“切换后不生效/状态不保持”的问题，不能只凭设置页连线认定已修复。


## 本轮实际修复：50 种图标形状设置被持久化层截断

- 源码提交：`9747ebc31ae85083b481fc8ca1f054813a9ae35e`。
- 在 `SettingsRepositoryImpl.kt` 中发现明确的索引范围不一致：读取 getter 允许 `0..51`，但 setter 和设置快照恢复时都把 `material_icon_shape_style` 截断到 `0..1`。这会导致选择 50 个预设中的项目后，保存/恢复时被改回前两项，直接破坏选择器持久化。
- 已把 setter 与快照恢复两处范围统一修正为 `0..51`，与当前选择器的 50 个预设及 None/Random 项范围保持一致。该修复依据当前代码中的明确范围矛盾，不代表 50 个图形路径已经与 Voyage 原版逐项还原。
- 当前最新产品源码提交为 `9747ebc31ae85083b481fc8ca1f054813a9ae35e`。尚未核验该提交对应的 GitHub Actions 构建与 APK；后续必须先确认编译结果，再继续批量移植。不得把前一阶段 APK 当作包含此修复的新包。


## 最新构建验收：Build #1655

- GitHub Actions：`40487579007`，Run #1655，head SHA 为维护记录提交 `668a95738f37935215619e71244a3a0f29d5c653`；该提交包含前序产品修复 `9747ebc31ae85083b481fc8ca1f054813a9ae35e`。构建结论 `success`，build job 与签名验证步骤均成功。
- Artifact ID：`12726210569`，名称 `AlohaOneUI-Signed-Test-APK`；Artifact ZIP SHA-256：`c1862e83dc8c9ea0f944d5523d2cb071e8f14a076c078e186c80af55478cb357`。
- APK：`AlohaOneUI_1.0.261025_BUILD_1655.apk`，大小 `28,747,517` bytes；SHA-256：`1a98ce182c89c5f4d67358caddc67d1aa4226f3e4b35b44e2f9213da9a8fe91e`。
- 验收：外层 Artifact ZIP 与 APK 的 `unzip -t` 均通过；APK 包名 `com.alohaoneui.app`，versionName `1.0.261025`，versionCode `16`；V2 签名验证通过，证书 SHA-256 为 `93bf61a6493c6b336383832eb41301bd8fe061f43566e7e384e3fdd9fc1c26a1`。
- 该包确实包含图标形状索引持久化范围修复，并撤销了上述两项无原版证据的间距猜测；但仍只是阶段性测试包。50 个图形仍为算法生成轮廓，Switch/Slider 与 Smooth G² 尚未完成 Voyage 原始绘制路径逐项移植，不得标记为 1:1 最终版。
- 下一步继续定位并对照 Voyage 的实际渲染实现，优先 Switch/Slider 与形状路径；原始方法体无法可靠读取前，不提交猜测性的视觉参数。后续产品改动必须在同一测试分支完成构建与 APK 验收，`main` 保持不动。
