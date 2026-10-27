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


## Build #1656 验收与最新状态

- Actions Run：`40488342757`（Build #1656），状态 `success`；对应 HEAD `f05c80d37e2ae5709db6e4681fbaa8bce20941be`，这是维护记录更新提交，未包含新的产品源码改动。
- Artifact ID：`12727255451`，Artifact ZIP SHA-256：`181e80cfa70bae49b49e592e43fea31e69b95abe389fe86f4c6e9829118765b5`。
- APK：`AlohaOneUI_1.0.261025_BUILD_1656.apk`，大小 `28,747,517` bytes；SHA-256 与 Build #1655 完全相同：`1a98ce182c89c5f4d67358caddc67d1aa4226f3e4b35b44e2f9213da9a8fe91e`。这说明本次仅维护记录变化，不能声称生成了新的产品二进制。
- 验收：Artifact ZIP 与 APK 的 `unzip -t` 均通过；包名 `com.alohaoneui.app`、versionName `1.0.261025`、versionCode `16`；V2 签名通过，证书 SHA-256 仍为 `93bf61a6493c6b336383832eb41301bd8fe061f43566e7e384e3fdd9fc1c26a1`。
- 重新核查 `OneUiFeatureIcon.kt`：图标形状设置目前确实用于该共享功能图标的裁切；形状生成仍为项目内 `geometricIconShape` 算法，不是 Voyage 原版 50 条路径。不能把索引持久化修复说成轮廓移植完成。
- 当前没有新的产品源码提交；下一步仍须获得/恢复可可靠读取 Voyage 原始绘制方法的路径，再进行有证据的批量移植。不得为了产生新提交而随意更改视觉参数。


## 本轮原始 DEX 继续取证（未据此猜改 UI）

- 已直接读取本地 Voyage APK 的 DEX（classes5.dex、classes6.dex、classes7.dex 等），编写最小只读解析器读取类定义、方法签名、code_item 与 invoke 引用；这是对原始 APK 二进制的检查，不是根据现有 Aloha 代码反推 Voyage 外观。
- 直接确认原版 DEX 中存在 Switch 样式枚举类 `Lgfj0;`（8 个枚举构造调用）及 Slider 样式枚举类 `Lkdj0;`（4 个枚举构造调用）。枚举名称经过字符串混淆，当前尚未完成解密和语义映射。
- 定位到原始 Slider 绘制相关类 `Ly8f0;`：其中 `IO` 方法签名包含 DrawScope、浮点参数、多个颜色/长整型值与绘制 lambda，方法体长度为 1220 个 16-bit 指令单元；`Io` 是其被重复调用的绘制辅助方法。已保存本地反汇编草稿用于继续分析。
- 原始 G² 相关类 `Lban;`、`Ldan;`、`Lwcf;`、`Lfqw0;`、`Lopw0;` 也已定位到具体方法及 code_item 偏移。当前自制解码器只可靠恢复了方法边界与部分调用引用，尚不足以重建完整控制流、绘制参数或 G² 路径；不能把这份草稿当作可直接移植的源码。
- 因缺少可用 JADX/baksmali 工具，且现有解码尚未达到逐项核验绘制路径的可靠程度，本轮没有改写 Switch/Slider/G² 的视觉常量，也没有提交伪称 1:1 的近似实现。
- 下一步继续从已定位的原始方法体恢复分支、浮点常量、颜色/路径构造与枚举到绘制分支的映射；只有在调用链和参数含义能被验证后，才批量移植到测试分支。当前产品最新有效二进制仍与 Build #1655 相同。


## 本轮实际产品修复：滑块全局阴影/描边设置未接入通用控件

- 源码提交：`d3937c8db61b6491452a8901717f436292bffd22`。
- 对照 `ColorPaletteScreenMaterial.kt` 中页面缩放滑块的既有实现，确认该预览会依据 `LocalMaterialShadowSliders` 添加 `Modifier.shadow(2.dp, voyageCornerShape(50.dp)))，并依据 `LocalMaterialOutlineWidth` 添加轮廓描边；但通用 `SegmentedSliderItem.kt` 的实际 Slider 组件之前完全没有读取这两个 CompositionLocal，导致用户关闭/开启“滑块阴影”或调整全局描边时，通用滑块没有同步应用设置。
- 已在 `SegmentedSliderItem.kt` 的实际 Slider modifier 中接入同一阴影条件与同一轮廓宽度设置，沿用现有项目预览的形状与参数，未引入新的猜测性数值。
- Build #1659：Actions Run `40490743739`，head SHA `d3937c8db61b6491452a8901717f436292bffd22`，结论 `success`。
- Artifact ID `12727872553`；Artifact ZIP SHA-256：`ca411229929b25089a7692fd5a905037342e3a9d0707022c67a3059e584501a3`。
- APK `AlohaOneUI_1.0.261025_BUILD_1659.apk`，大小 `28,747,517` bytes；SHA-256：`87b7780faee1ece9a139e5a5fae341e6cd7c9e0c2f65d2b0feca706aaafaecff`。
- 验收：Artifact ZIP 与 APK `unzip -t` 通过；包名 `com.alohaoneui.app`，versionName `1.0.261025`，versionCode `16`；V2 签名通过，证书 SHA-256 `93bf61a6493c6b336383832eb41301bd8fe061f43566e7e384e3fdd9fc1c26a1`。
- 该修复是 Aloha 通用控件的设置接入修复，不代表 Voyage 原版 Slider 绘制已经 1:1 完成。后续仍须以已定位的原始 `Ly8f0;` 绘制调用链恢复四种样式的实际几何与颜色参数。


## Build #1661：Switch 全局阴影设置接入修复

- 产品源码提交：`b1da7a3488c28f4f90138edb33faad7b8154bdc1`。
- 在 `ExpressiveSwitch.kt` 中确认开关阴影设置 `LocalMaterialShadowSwitches` 原先仅在样式索引 2、5、6 生效，导致用户启用“开关阴影”后，其他样式不会遵循这个全局设置。已去除不一致的样式白名单，改为所有开关样式统一受该设置控制；阴影开关关闭时所有样式都不应用该阴影修饰符。
- 此修复解决的是 Aloha 当前全局设置与实际控件之间的不一致；它不是对 Voyage 原版八种 Switch 绘制外观的 1:1 还原证明。
- GitHub Actions Run `40492886129`（Build #1661）结论 `success`，head SHA 为 `b1da7a3488c28f4f90138edb33faad7b8154bdc1`。
- Artifact ID `12730190238`；Artifact ZIP SHA-256：`d31fead1065495ccc67feaea4b0d87ec618ce82314bd0f41ad0cfacb44a2be2d`。
- APK：`AlohaOneUI_1.0.261025_BUILD_1661.apk`，大小 `28,748,245` bytes；SHA-256：`55fcd9907c8cc57d8bbcf80db57d38bfeebf35385dd5aede7f906122becc8436`。
- 验收：Artifact ZIP 与 APK `unzip -t` 均通过；`SHA256SUMS` 校验通过；包名 `com.alohaoneui.app`，versionName `1.0.261025`，versionCode `16`；V2 签名验证通过，证书 SHA-256 为 `93bf61a6493c6b336383832eb41301bd8fe061f43566e7e384e3fdd9fc1c26a1`。
- 当前最新产品 APK 为 Build #1661。后续继续处理 Voyage 原始 Switch/Slider 绘制路径、真实图标形状与 Smooth G²；在还原原始方法体并验证参数前，不将手写近似样式标记为原版移植完成。
