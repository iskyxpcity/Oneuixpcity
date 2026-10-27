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


## Build #1663：Switch 全局描边宽度修复

- 产品源码提交：`e507579e98f2a8e772bf5e6d348eaf2259c4f0f4`。
- 在 `ExpressiveSwitch.kt` 中修正全局轮廓宽度行为：此前部分样式在宽度为 0 时仍会画出至少 1dp 的边线，而另一些样式不响应全局描边宽度。现改为仅当 `LocalMaterialOutlineWidth > 0` 时绘制统一的全局描边，并直接使用设定宽度。
- Build #1663：Actions Run `40493483230`，结论 `success`，head SHA `e507579e98f2a8e772bf5e6d348eaf2259c4f0f4`。
- Artifact ID：`12729931602`；Artifact ZIP SHA-256：`3e5c5c36dfc5b48b075d4ad1f62b60fc1f90a56237dbf0b9b534c4fac6c10618`。
- APK：`AlohaOneUI_1.0.261025_BUILD_1663.apk`，大小 `28,748,245` bytes；SHA-256：`b5e2a1f078fdd8a53e4747401f2753185e21dec7febe76d55104d091d9d0450e`。
- 验收：Artifact ZIP 和 APK 的 `unzip -t` 通过；`SHA256SUMS` 校验通过；包名 `com.alohaoneui.app`、versionName `1.0.261025`、versionCode `16`；V2 签名通过，签名证书 SHA-256 `93bf61a6493c6b336383832eb41301bd8fe061f43566e7e384e3fdd9fc1c26a1`。
- Build #1663 包含 #1661 的“所有开关样式遵循全局阴影”修复与本次“全局描边宽度统一”修复。以上均为 Aloha 组件设置一致性修复，不能替代 Voyage 原版绘制路径的 1:1 移植。


## 2026-10-27 本轮接续核查（未生成新产品构建）

- 已重新读取本维护总记录，并从 GitHub 提交记录核验 `735e3cee7b45cde58a227763401bae843e08ad2b` 为 Build #1663 维护记录提交；对应产品源码提交仍为 `e507579e98f2a8e772bf5e6d348eaf2259c4f0f4`。测试分支仍为 `test/voyage-theme-1to1-20261026`，未对 `main` 执行写入。
- 重新读取 `Theme.kt`，确认 `AppSettings`、`ThemeController.getAppSettings()` 与 `CompositionLocalProvider` 已传递 `materialShadowFloatingButtons` 到 `LocalMaterialShadowFloatingButtons`。这只能证明设置值被传递，不能证明存在实际消费该值的浮动按钮组件；尚未找到该组件的真实调用路径，因此本轮不将它套用到 `FloatingBottomBar` 或其他未经证实的控件。
- 复核 `MaterialTheme.kt`：当前 Material 主题根仍使用 `Shapes()` 与 `MotionScheme.expressive()`；该文件本身没有提供 Voyage 原版八种 Switch、四种 Slider、50 条图标路径或 Smooth G² 绘制参数的证据，因此未据此伪造原版实现。
- 再次查询 Build #1663 对应的 Actions artifact：Run `40493483230`，分支 `test/voyage-theme-1to1-20261026`，head SHA `e507579e98f2a8e772bf5e6d348eaf2259c4f0f4`；Artifact ID `12729931602`，ZIP SHA-256 `3e5c5c36dfc5b48b075d4ad1f62b60fc1f90a56237dbf0b9b534c4fac6c10618`。这是已知阶段性产物，不是本轮新构建，也不代表 1:1 完成。
- 当前 GitHub 文件读取接口对本轮尝试的四个猜测组件路径返回 404，且仓库文件搜索接口没有返回分支源码结果；因此本轮无法可靠确认这些组件的实际仓库路径及其最新实现，不能在错误路径上创建同名文件或提交猜测性代码。此限制不代表项目文件不存在。
- 本轮没有产品源码修改，也没有新的 APK；不把维护记录提交当成产品修复。后续必须先取得测试分支实际 HEAD/树与真实组件路径，再继续有源码依据的批量修复。仍未完成：Voyage Switch/Slider 原始绘制、真实图标路径、Smooth G²、浮动按钮阴影消费链、主题页面 1:1 核对及最终 APK 验收。


## 2026-10-27 后续推进：修正图标形状 None 预览的裁切

- 产品源码提交：`3450efee6eef33de152d39e74be62b64cc2f0549`（`fix: render None icon shape preview without clipping`）。仅提交到 `test/voyage-theme-1to1-20261026`，未修改 `main`。
- 修改文件：`app/src/main/java/com/alohaoneui/app/ui/component/material/IconShapePresetPicker.kt`。
- 源码依据：该文件将 `ICON_SHAPE_NONE = 0` 定义为“未遮罩”，`iconShapeForStyle(0)` 返回 `null`；但预览列表把空形状回退成 `RoundedCornerShape(12.dp)`，随后仍对预览 tile 调用 `clip(shape)`，使 None 项视觉上继续被圆角裁切，与本文件的 None 语义矛盾。
- 修复：以 `RectangleShape` 替换 None 预览的圆角回退形状，并移除不再使用的 `RoundedCornerShape` 导入。没有更改 50 个几何轮廓，也没有声称它们已与 Voyage 原版路径匹配。
- 交叉核对真实源码路径后，确认本项目组件实际位于 `ui/component/material/`（单数 component），此前猜测的 `ui/components/` 路径错误；这只是定位纠正，不代表源码不存在。
- 本次源码提交后立即查询到的 combined status 尚未返回状态项，不能据此判定 CI 成功或失败；仍需继续核对该提交触发的 Actions 运行、构建日志和产物。当前没有新 APK 可交付，不得复用 Build #1663 冒充本次构建。
- 本轮重新确认：`SegmentedSliderItem.kt` 四种样式目前仍使用手写颜色、轨道高度、拇指尺寸和路径；`ExpressiveSwitch.kt` 八种样式仍是 Aloha 手写分支；`IconShapePresetPicker.kt` 的 50 个轮廓仍来自 `geometricIconShape` 算法。这些实现继续标记为待 Voyage 原始实现逐项对照，不视为 1:1 完成。
- 仍未完成：恢复 Voyage Switch/Slider 的原始方法体与参数、50 个原版图标路径/顺序/None(-1)/Random 映射、Smooth G² 的 cubic→arc→cubic 分支、`LocalMaterialShadowFloatingButtons` 的真实组件消费路径、设置页面视觉与层级对照，以及最终 HEAD 的构建和 APK 元数据/签名/SHA-256 验收。


## 2026-10-27 后续推进：主题选项按钮动效与阴影接入

- 产品源码提交：`72c8577ce387d2b6f543e4246e70fba562c14274`（`fix: animate theme option chips and honor button shadow setting`），只写入测试分支 `test/voyage-theme-1to1-20261026`，未修改 `main`。
- 重点文件：`app/src/main/java/com/alohaoneui/app/ui/component/material/SegmentedList.kt`，组件 `SegmentedChoiceOptionsItem`。
- 发现的问题：主题设置中的多选一选项按钮使用 `FilterChip`，原有实现没有把 `LocalMaterialShadowButtons` 接入这些按钮，也没有提供明确的按压缩放反馈；这会造成“按钮阴影”全局设置在此类选项上不生效，触摸反馈与其他可交互控件不一致。
- 已做的修改：
  1. 将每个选项按钮的 `MutableInteractionSource` 接入按压状态；
  2. 使用 Compose spring 动画在按压时缩放至 0.96，松开恢复 1.0；
  3. 选项按钮按当前全局 `LocalMaterialShapeStyle` 通过 `voyageCornerShape(8.dp)` 取得框体形状；
  4. 按 `LocalMaterialShadowButtons` 控制阴影：关闭时不添加阴影，开启时未选中为 1dp、选中为 2dp；
  5. 保留原有选中状态、勾选图标、触觉反馈及选择回调。
- 这是对当前选项按钮交互/全局阴影设置消费链的实际修复，不声称 0.96 缩放、1/2dp 阴影或当前 Chip 框体参数已经通过 Voyage 原版像素级对照。若后续原版源码/实测确认了不同参数，仍应以 Voyage 为准。
- 提交后重新读取源码确认新增的 `interactionSource`、按压动画、`voyageCornerShape` 与 `LocalMaterialShadowButtons` 已写入分支。当前可用的 commit workflow 查询仅返回空列表，combined status 也没有状态项；不能据此判定构建成功或失败。当前尚无与此 HEAD 对应的已验证 APK，不得复用 Build #1663 作为本次产物。
- 主题设置页的列表/详情框体仍需逐页核查：尤其页面分组外框、行项目首尾圆角、详情选择器框体、选中/未选中颜色、框体边线与阴影是否在所有重载中一致。此提交只处理 `SegmentedChoiceOptionsItem` 的选项按钮，不代表整个设置页框体问题已完成。
- 后续继续：定位实际主题设置页面源码并逐页核对列表/详情框架；检查列表行、下拉选项、Radio/Checkbox、页内操作按钮是否全部消费对应全局阴影和动效设置；再进行最新 HEAD 的构建、编译错误修复和 APK 元数据/签名/SHA-256 验收。


## 同一轮追加修复：主题选项按钮框体描边统一

- 产品源码提交：`c4d838a438f03d31f19fba13844e4ffe4a67c0a5`（`fix: apply global outline width to theme option chips`），测试分支独立提交，`main` 未修改。
- 在上一项选项按钮阴影/动效修复基础上，继续发现 `SegmentedChoiceOptionsItem` 的 `FilterChip` 仍未消费 `LocalMaterialOutlineWidth`。因此，即使全局轮廓宽度已启用，主题选项按钮仍可能缺少与列表行一致的框体描边。
- 已为每个选项按钮接入全局描边宽度：宽度大于 0 时，按当前选项的 `voyageCornerShape(8.dp)` 绘制 `outlineVariant` 描边；宽度为 0 或负值时不绘制描边。保留上一个提交的全局按钮阴影与按压缩放动画。
- 目前已实际覆盖该选项控件的三项全局视觉设置：按钮阴影、轮廓宽度、主题形状风格；这只证明 Aloha 当前通用选项控件的设置消费链已接入，不证明这些 dp 参数已通过 Voyage 原版像素对照。
- 本次产品源码最新提交为 `c4d838a438f03d31f19fba13844e4ffe4a67c0a5`。当前可用的 workflow 查询接口未返回该提交对应的构建运行记录，combined status 也无状态项；构建状态未知，尚无可核验的新 APK。必须继续验证编译与产物，不能把旧 APK 当成包含这两项修改的交付包。


## 2026-10-27 构建失败定位与修复：RectangleShape 导入包错误

- 失败运行：GitHub Actions Run [40497133882](https://github.com/iskyxpcity/Oneuixpcity/actions/runs/40497133882)，build job ID `122165966812`，结论为 `failure`。
- 失败步骤：`Compile Aloha OneUI before the owner-signing gate`（步骤 10）；源码身份校验等前置步骤成功，APK 签名和产物发布因编译失败而跳过。
- 日志中的确切错误：`IconShapePresetPicker.kt:13:42` 与 `IconShapePresetPicker.kt:55:28` 均报 `Unresolved reference 'RectangleShape'`。根因是导入写成 `androidx.compose.foundation.shape.RectangleShape`，但当前 Compose API 的 `RectangleShape` 应从 `androidx.compose.ui.graphics.RectangleShape` 导入。
- 已修复产品源码并提交到测试分支：`67769c6fe925511b9a6b6208f52aa5c2baf3372b`（`fix: import Compose RectangleShape from graphics package`）。修改仅纠正 import，不改变 None 预览使用矩形、不裁切的原意；未修改 `main`。
- 当前状态：修复提交已写入分支，新的 Actions 运行结果仍待核验。此刻不宣称构建通过，也没有可交付的新 APK。必须继续读取后续运行日志；若再失败，按新日志继续修复，直到成功构建并验证 APK 包名、版本、签名与 SHA-256。


## 2026-10-27 后续推进：主题设置列表行按压动效与动态阴影统一

- 产品源码提交：`0c8d789fd9efac1d68d077891e07e344221ea4a7`（`fix: animate Voyage settings list rows and container shadows`），仅写入 `test/voyage-theme-1to1-20261026`，未修改 `main`。
- 重点文件：`app/src/main/java/com/alohaoneui/app/ui/component/material/SegmentedList.kt`，覆盖普通列表行、复选状态行、选中/单选状态行三个公开重载。
- 本次修复：
  1. 为三个重载统一解析并复用 `MutableInteractionSource`，使列表行本身能够读取按压状态，不再依赖调用方恰好传入 interaction source。
  2. 接入 spring 按压缩放，按压时缩至 `0.985f`、释放后回到 `1.0f`；三个重载使用独立动画 label，避免同一页面多行之间状态混淆。
  3. 在 `LocalMaterialShadowContainers` 开启时，将容器阴影从固定 1dp 改为随按压状态由 1dp 过渡到 2dp；关闭时不绘制容器阴影。
  4. 保留各行基于 `voyageSegmentedShape` 的框体、`LocalMaterialOutlineWidth` 的全局描边和原有选中/点击回调；没有改变行项目文本、数据或设置行为。
- 本次针对用户指出的“主题设置页列表/详情选项框架与各按钮动效、阴影”优先修复列表行交互层。它并不表示页面分组框体、详情面板布局、所有控件阴影或 Voyage 原版像素级参数已全部完成；后续仍须逐项对照真实 Voyage 组件与设置页结构。
- 构建监测：此前 RectangleShape 修复所在的 Run `40498179981`（Build #1673）在查询时为 `in_progress`，编译步骤正在运行；本次源码 push 可能因分支 workflow 的 `cancel-in-progress` 策略取消该旧 run 并触发新 run。必须以本次提交或包含它的最新 HEAD 对应 run 为准，继续监测编译、签名、APK 验证与 artifact，不可将旧 APK 当作本次产物。
- 当前阶段结论：源码已提交，构建尚未验收；无新 APK 可交付。若 CI 失败，按最新失败日志修复并再次构建；只有确认成功并核验包名、版本、签名证书与 SHA-256 后才可标记完成。


## 2026-10-27 后续推进：滑块描边类型修复与开关/标签按钮按压动效统一

- 最新源码基线：此前提交 `8459efba6cfe14478a3ff8953af9798a8bbe7d24`（`fix: animate slider thumb and keep outline on visible track`）。该提交的 Build #1676 / Run `40499299889` 编译失败；已下载并检查该次 Actions 诊断产物，明确错误为 `SegmentedSliderItem.kt:158:44 Argument type mismatch: actual type is 'Float', but 'Dp' was expected`。
- 根因：`LocalMaterialOutlineWidth.current` 的类型是 Float，而 `VoyageSliderTrack` 接收 `Dp`。已在调用处转换为 `LocalMaterialOutlineWidth.current.dp`，保持实际数值不变，修复编译类型不匹配。
- 同一提交批次继续补齐常用交互组件的动效和全局视觉设置消费：
  1. `ExpressiveSwitch.kt`：开关按压时增加 spring 缩放，拇指按压缩放由即时条件切换改为动画；开关阴影在按压时由 2dp 过渡到 3dp，仍由 `LocalMaterialShadowSwitches` 控制是否显示。
  2. `ExpressiveTabRow.kt`：标签按钮增加 spring 按压缩放和动态阴影；阴影继续受 `LocalMaterialShadowButtons` 控制，并将 `LocalMaterialOutlineWidth` 应用于标签胶囊框体，避免全局描边设置只覆盖列表项而漏掉标签按钮。
  3. `SegmentedSliderItem.kt`：修复上次滑轨描边接线的 Dp 类型错误；滑轨轮廓仍绘制在可见轨道范围，不再包围整行触摸区域。
- 本批次仅修改 `test/voyage-theme-1to1-20261026`，不修改 `main`。这些是通用控件层面的统一处理，仍需继续逐页检查详情面板/分组框体、下拉菜单和对话框按钮，不能据此声称所有页面已经完成 Voyage 像素级还原。
- 构建监测：Build #1676 / Run `40499299889` 已结束且结论为 `failure`；签名、APK 验证和产物步骤均因编译失败跳过。本记录与上述代码修复将合并在单次提交中，随后必须监测新 HEAD 的 Actions；若仍失败，读取诊断 artifact 并继续修复。当前无可交付的新 APK。


## 2026-10-27 后续推进：修复标签动效编译错误并补齐主题页选择按钮框体/阴影

- 上一批次提交 `ad4f70ce516bb7e2d1dc29f7012d07b1032d2557` 的 Build #1677 / Run `40500736929` 编译失败。诊断 artifact `12732691769` 已下载检查，明确错误为 `ExpressiveTabRow.kt` 的三个 delegated state 缺少 `androidx.compose.runtime.getValue` 导入；本批次补上该导入。
- 继续覆盖主题设置页里之前没有统一消费按钮动效/阴影的选择器：
  1. `ExpressiveToggleButton.kt`：使用同一个 `MutableInteractionSource` 让按压缩放与 Material 3 ToggleButton 的交互状态同步；全局按钮阴影关闭时传入 `elevation = null`，开启时使用组件自身的交互式 elevation；描边改为组件原生 `border` 参数，以便形状/描边随按钮交互统一。
  2. `IconShapePresetPicker.kt`：图标形状选项增加 spring 按压缩放、动态阴影、ripple；选中和未选中选项均消费全局描边宽度，选中态使用 primary 描边、未选中态使用 outlineVariant，去除原来硬编码的 2dp 边线。None 项仍保持 RectangleShape、不裁切。
  3. `ColorPaletteScreenMaterial.kt`：主题颜色选项卡增加按压缩放和动态阴影，并复用同一 interaction source；原有选色回调、触觉反馈、选中指示动画和全局描边设置保持不变。
- 本次改动仍只写入测试分支，不改 `main`。这次针对“主题设置页各级选项按钮框体、按压动效与阴影”扩大到开关模式按钮、图标形状条目与颜色选项卡；Voyage 原版几何和视觉参数仍需以原始实现逐项核对，不能把通用动效接入等同于 1:1 验收。
- 构建监测：Run `40500736929` / Build #1677 结论为 `failure`，失败发生在 Kotlin 编译阶段，owner 签名和 APK 产物步骤被跳过。下一次提交将包含本次修复与记录更新，并触发新构建；继续检查新 HEAD 对应 Actions 和诊断日志，不交付旧 APK。


## 2026-10-27 后续推进：主题页选择器动效批次构建成功

- 产品源码提交：`56cbd4db4a5e96ec4e33af0ed6365ffd669a339e`（`fix: animate theme selector controls and unify outline handling`），测试分支 `test/voyage-theme-1to1-20261026`，未修改 `main`。
- 该批次修复了 Build #1677 的缺失 `getValue` 导入，并为 ToggleButton、图标形状选项、主题颜色选项接入统一按压动效、动态阴影和全局描边消费。
- Build #1678：Actions Run `40501789348`，head SHA `56cbd4db4a5e96ec4e33af0ed6365ffd669a339e`，结论 `success`；build job `122181009263` 完成且无失败步骤。签名 APK artifact ID `12733067342`，名称 `AlohaOneUI-Signed-Test-APK`，大小 27,768,341 bytes，Artifact ZIP SHA-256 `ec7313956bb3681a77acd8c0c94dd1645eaedfa4bd5506877b6f72df2775e635`。
- 本记录只确认该 HEAD 构建成功且产生签名 APK artifact；尚未把它标记为最终交付包，因为当前继续开发会产生新的 HEAD，且该 APK 的包名、版本、证书与 APK 本体 SHA-256 仍需在最终 HEAD 上再次核验。

## 2026-10-27 后续推进：返回按钮、可点击卡片和对话框容器阴影统一

- `TopBarBackButton.kt`：增加可访问的交互源、spring 按压缩放、动态按钮阴影；阴影仍受 `LocalMaterialShadowButtons` 控制，描边继续消费 `LocalMaterialOutlineWidth`。
- `TonalCard.kt`：可点击卡片的 pressedElevation 与容器阴影开关联动，按压时由 2dp 动态过渡到 3dp；关闭全局容器阴影时默认/按压 elevation 都为 0dp。非点击展示卡片维持原有静态容器阴影逻辑。
- `ExpressiveDialog.kt`：标准与自定义内容对话框的 tonal elevation 均消费 `LocalMaterialShadowContainers`；关闭容器阴影时不再保留默认 tonal elevation，描边设置仍独立生效。
- 本批次继续只修改测试分支，不改 `main`。这些修改覆盖返回按钮、可点击详情卡片和弹窗容器，不等同于所有 Voyage 页面已完成逐像素复刻。提交后继续监测新 Actions，发现编译问题即按日志修复。


## 2026-10-27 后续推进：返回按钮/卡片/对话框阴影批次构建成功

- 产品源码提交：`ebc848d70e7c1f2b6d0d1f2b7ae222a5ca669c89`（`fix: animate back button and unify container shadow settings`），测试分支 `test/voyage-theme-1to1-20261026`，未修改 `main`。
- Build #1679：Actions Run `40502441349`，head SHA `ebc848d70e7c1f2b6d0d1f2b7ae222a5ca669c89`，结论 `success`；build job `122183204091` 成功且没有失败步骤。签名 APK artifact ID `12734020169`，名称 `AlohaOneUI-Signed-Test-APK`，大小 27,770,526 bytes，Artifact ZIP SHA-256 `bbd9f5a3c8a7faa8bb8c448c63790fca71a94d884ccad132acddd5a0b2af3e46`。该 APK 尚不是最终交付物，需等最终 HEAD 并完成 APK 本体验证。
- 上批次修复：返回按钮按压缩放/动态阴影；可点击卡片按压时提升 elevation；标准与自定义内容对话框遵循全局容器阴影开关。全局描边设置保持独立。

## 2026-10-27 后续推进：下拉详情菜单框体统一

- 继续核查主题设置与其他子页面使用的下拉选项：原先 `SegmentedDropdownItem` 和 `OffsetAnchoredExpressiveMenu` 的 `DropdownMenuGroup` 都沿用默认 popup 阴影和默认边框，导致全局“容器阴影/轮廓宽度”在列表行可见，却在展开的详情选项菜单里不生效。
- 已为这两处菜单组统一接入 `LocalMaterialShadowContainers` 与 `LocalMaterialOutlineWidth`：开启容器阴影时使用 Material 3 菜单组的默认 shadow elevation，关闭时 elevation 归零；全局轮廓宽度大于 0 时使用 `outlineVariant` 绘制组框边线，宽度为 0 时不添加边线。菜单项目原有的 selected 状态、形状组与回调不变。
- 修改仅写入测试分支，不改 `main`。这次针对的是弹出的详情选项菜单框体；仍需继续检查设置页面的其他容器/按钮，并以 Voyage 原版对照结果校准视觉参数。


## 2026-10-27 后续推进：修复下拉菜单框体构建错误并补齐选项 Chip 动态阴影

- Build #1680 / Run `40503070353` 编译失败。已下载并检查诊断 artifact `12734015948`，唯一 Kotlin 错误为 `SegmentedList.kt:1045:29 Unresolved reference 'BorderStroke'`。
- 根因：在 `SegmentedList.kt` 中新增下拉菜单组描边时，上一批次的导入插入规则没有匹配到该文件真实 import 区，因此调用已写入但 `androidx.compose.foundation.BorderStroke` 未导入。本批次直接在文件包声明后补入正确 import，保留菜单组的全局描边与阴影行为。
- 同时补齐 `SegmentedChoiceOptionsItem` 的按钮阴影过渡：按压且可用时 elevation 动态升至 3dp，选中态 2dp，普通态 1dp；动画使用 spring，并继续受 `LocalMaterialShadowButtons` 控制。此前只缩放按钮，阴影在按压过程中是固定值。
- 修改仅写入测试分支，不改 `main`。提交后继续监测新 HEAD 构建；如果出现新错误，继续按真实日志修复。当前无本批次新 APK。
