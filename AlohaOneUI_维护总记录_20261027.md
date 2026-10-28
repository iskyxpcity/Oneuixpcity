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


## 2026-10-27 后续推进：下拉详情菜单框体批次构建成功

- 产品源码提交：`b200090463dd9c7c078bb646ae2a75ab28622283`（`fix: import menu BorderStroke and animate choice chip shadows`），测试分支 `test/voyage-theme-1to1-20261026`，未修改 `main`。
- Build #1681：Actions Run `40503555329`，head SHA `b200090463dd9c7c078bb646ae2a75ab28622283`，结论 `success`；build job `122187029797` 成功且没有失败步骤。签名 APK artifact ID `12733598689`，名称 `AlohaOneUI-Signed-Test-APK`，大小 27,771,488 bytes，Artifact ZIP SHA-256 `e427917d1f28dcf56325f575eddcaa7ce334d526d700ea3fd6e6263160f73c9e`。这不是最终交付 APK，仍需对最终 HEAD 重新构建并验证 APK 本体。
- 该批次修复 `BorderStroke` 导入错误，并让 `SegmentedChoiceOptionsItem` 的阴影随按压状态动态过渡；下拉菜单组的全局描边/阴影消费已进入可成功构建的 HEAD。

## 2026-10-27 后续推进：主题详情页导航返回按钮动效统一

- 继续核查 `ColorPaletteScreenMaterial`（主题/配色设置页）发现其导航返回按钮仍直接使用无交互源的原生 `IconButton`，没有消费全局按钮阴影/描边设置，也没有与其他主题选项一致的按压缩放。
- 已在返回按钮中接入独立 `MutableInteractionSource`、spring 按压缩放、按压时动态阴影，并按 `LocalMaterialShadowButtons` 控制阴影显示、按 `LocalMaterialOutlineWidth` 控制圆形描边；保留原有返回回调、图标和触摸语义。
- 仅修改测试分支，不改 `main`。该修改用于补齐主题子页面导航控件与其他选项按钮的一致性；继续检查其余页面的直接按钮调用，避免只修复封装组件而漏掉页面内的原生按钮。


## 2026-10-27 后续推进：主题导航按钮构建错误修复与选项组外框统一

- Build #1682 / Run `40504157522` 编译失败；诊断 artifact `12734236742` 指出 `ColorPaletteScreenMaterial.kt:19:48` 与 `:21:48` 重复导入 `MutableInteractionSource`，以及同一组 `collectIsPressedAsState` 重复导入。根因是本次新增导航返回按钮时重复插入了此前已存在的 imports。本批次去除重复导入，保留新增返回按钮交互代码。
- 继续定位主题设置页选项框体不一致：`SegmentedChoiceOptionsItem` 和 `IconShapePresetPicker` 都是直接绘制内部标题/芯片的 Column，而不是标准 `SegmentedListItem`，所以在 `SegmentedColumn` 分组中缺少与相邻行一致的外框、容器阴影和全局描边消费。
- 新增共用 `SegmentedOptionFrame`：读取当前分组的 `LocalListItemShapes`，继承同一分段形状；背景沿用 `surfaceColorAtElevation(1.dp)`；容器阴影受 `LocalMaterialShadowContainers` 控制；描边受 `LocalMaterialOutlineWidth` 控制。将主题多选一选项组和图标形状选项条目统一包入该框体，内部 chips/图标 tile 的独立点击、选中态与按压动效保持不变。
- 本次仅修改测试分支，不改 `main`。目标是让原先缺少行容器框体的复杂选项与普通列表/详情行共用一致的框架规则；构建后还需按实际页面效果核对有无重复嵌套框体，并以 Voyage 原版层级为准调整。
- 当前 Build #1682 未通过，签名和 APK 产物步骤跳过；此提交包含导入修复与选项组框体统一，必须监测新 HEAD 的 Actions。当前没有本批次新 APK。


## 2026-10-27 后续推进：主题选项结构框体可见性与色彩选项本地化

- 用户再次明确优先项：色彩风格、色彩标准、页面切换手势、形状样式、开关样式、滑条样式、图标形状必须具有清晰可见的框体，并继承父级分段列表的形状层级；同时所有页面子页面按钮的点击动效/阴影必须继续按 Voyage 原实现逐项对照，不能把通用动画冒充完成了源码移植。
- 核查后确认上述选项均使用 `SegmentedOptionFrame` 或 `SegmentedChoiceOptionsItem`，但旧框体背景使用 `surfaceColorAtElevation(1.dp)`，在当前系统主题与默认 0dp 轮廓设置下对比度过低，造成“没有框体”的实际观感。本批次将框体背景改为跟随 Material 主题的 `surfaceContainerHigh`，继承 `LocalListItemShapes` 的父级分段形状；当用户将可选描边宽度设为 0 时，仍保留低对比度 0.5dp 结构边线以明确显示容器边界；配置了正描边宽度时则严格使用该值和主题 `outlineVariant`。容器阴影继续受全局容器阴影设置控制。
- `ColorPaletteScreenMaterial.kt` 原先把调色板风格和色彩标准直接显示成枚举名（如 `TonalSpot`、`SPEC_2025`），因此系统语言切换时不会显示本地化文字。本批次将它们改为资源映射，并新增英文默认资源与简体中文资源；其他语言沿用 Android 默认英文回退，之后可继续补齐各语言翻译。标题、说明和选项色彩继续使用 Material 动态色彩角色，随系统明暗模式/当前色彩方案更新。
- 本次只修改测试分支，不改 `main`。这批处理结构框体可见性和色彩选项文字本地化，不代表已完成全部 Voyage 源码对照；按钮点击曲线、各控件原始形状/阴影数值仍要逐项核验。
- 构建状态以本批提交后的最新 Actions 为准；此前 Build #1683（Run `40504818929`，head `2b58baf71db27e7961aa2a8da17d0069d90450e4`）已成功，签名 APK artifact ID `12734020169` 只对应旧 HEAD，不能当成本批次交付包。


## 2026-10-27 后续推进：主题设置选择控件按 Voyage 原版改为框架行 + 弹出选项

- 已恢复并重新核对 Voyage APK 静态分析 artifact `voyage-apk-theme-inspection`（Run `40160923915`，Artifact ID `12586213779`，SHA-256 `e0fdc91a777164b2b18a49684e41f442bc3e4869e5c70c6ae3160518b294a545`）。此次读取实际反编译文件 `decompiled/sources/p000/k9t0.java`、`vfw0.java`、`vhw0.java`、`p7w0.java`、`zee0.java`，不是仅凭组件名称推断。
- 来源核对结果：
  - `k9t0.m108317Il` 通过 `bhu0.m6094l` 渲染设置行；`k9t0.m108319O` 中“开关样式/滑条样式”调用 `vfw0.m137946o`；`vfw0.m137946o` 将标题、选项集合、当前选中项、回调、父级形状和可用状态传入选择器。选择器再调用 `vhw0.m138123O` 或 `p7w0.m122564lI` 展示选项表面。
  - 因此原版不是常驻展示一排 `FilterChip`。之前的 `SegmentedChoiceOptionsItem` 用 `FlowRow + FilterChip` 展示所有选项，与原版控件层级不一致。
  - 本批次将 `SegmentedChoiceOptionsItem` 改为委托 `SegmentedDropdownItem`：外层为继承 `LocalListItemShapes` 的设置行框体，展示标题/说明/当前值与展开箭头；点击后以分组弹出选项列表呈现选择项、选中标记、触觉反馈和关闭行为。弹出菜单继续消费容器阴影/全局描边设置，菜单行使用 Material 3 的原生交互反馈，避免再造无源码依据的按压曲线。
  - 形状圆角依据继续为 `zee0.m147825O`：外角 16dp、内角 4dp，spring 阻尼比 0.4、刚度 200；选项组父子形状由 `SegmentedColumn` 提供的 `LocalListItemShapes` 传递。
- 该变更优先覆盖“色彩风格、色彩标准、页面切换手势、形状样式、开关样式、滑条样式”这些共用选择控件的框体和详情选项交互。图标形状仍需进一步按 `k9t0.m108318Io` 的原版子页面入口/详情层级处理，不能把当前预览条当作已完成的一比一移植。
- 本批次只改测试分支，不改 `main`。Build #1684 / Run `40506081632` 已成功，HEAD `342392f1e1da4548e10080a826da4a523960ece6`；它验证的是上批框体可见性和色彩标签本地化，不包含本批选择控件结构变更。新提交必须重新构建并检查最新 Actions。


## 2026-10-27 后续推进：图标形状入口/详情层级按 Voyage 反编译结构重整

- 源码依据来自本轮恢复的 Voyage 检查 artifact `12586213779`：`k9t0.m108318Io` 将图标形状作为一个设置行入口交给 `d8t0.m88399ol`；详情页由 `eig0` 的 case 22 遍历 `ewm.m92946Oo()`，逐项调用 `d8t0.m88393lO`。该行的原始预览尺寸为 52dp，选中背景为主色 alpha 0.7，未选中背景为主色 alpha 0.1；使用 ripple 点击，不是当前旧实现的 44dp 横向缩略图、0.94 缩放与 1–3dp 动态阴影。
- 已把 `IconShapePresetPicker` 改成父级分段框体内的单行入口；点击后进入独立图标形状详情页，详情行采用 52dp 高度、主色透明度状态和 ripple/haptic，不再在主主题页横向铺开全部形状。
- 同步修正图标形状索引模型以对应原版数据：None 使用 `-1`，50 个基础形状使用 `0..49`，Random sentinel 使用 `50`；设置读取、保存、导入快照、ViewModel、AppSettings 和 CompositionLocal 的范围/默认值已同步改为 `-1..50`。详情列表顺序为 50 个形状、Random、None。
- 仍有明确未完成项：`iconShapeForStyle` 的几何生成目前仍是算法近似，尚未逐项移植 `ewm.m92955ol()` 的 50 个真实路径；本次只校正父子页面层级、索引语义和可见交互，不标记图标形状 1:1 完成。
- 本批次仅修改测试分支，不改 `main`。前一提交 `5cf517b2bdecc110b237410b65eba54bafdc3c3b` 对应 Build #1685 / Run `40506732543` 已成功，签名 APK artifact ID `12735422060`（27,771,186 bytes；ZIP SHA-256 `15b644bcd36f1979190089d2ac7ef5c4ace857b0f0ff91a024538c48a9cad296`）仅对应前一 HEAD，不是本批次产物。本次提交后必须继续检查新 Actions。


## 2026-10-27 构建失败定位与修复：图标形状详情页导入缺失

- Build #1686 / Run `40507254852` 对提交 `a0d0d9c5e558d3da7771ff3659e51decc8c6042f` 结论为 `failure`，失败在 Kotlin 编译阶段，owner 签名与 APK 产物步骤未运行。
- 日志定位到三类导入问题：
  - `IconShapePresetPicker.kt:16,144`：`RectangleShape` 实际应导入 `androidx.compose.ui.graphics.RectangleShape`，不是 `androidx.compose.foundation.shape.RectangleShape`。
  - `ColorPaletteScreenMaterial.kt:135`：新增子页面状态缺少 `androidx.compose.runtime.mutableStateOf` 导入。
  - `ColorPaletteScreenMaterial.kt:138`：调用新详情页但未导入 `IconShapePresetDetailScreen`。
- 已按日志修正以上导入，未撤销本轮图标形状入口/索引移植。修复提交后继续检查最新 Actions，失败继续按日志修复；不得交付旧 APK。


## 2026-10-27 后续推进：按 Voyage 交互源码撤除无依据的整行缩放曲线

- Build #1687 / Run `40507612809` 对修复提交 `e5880c020c12802462496e5f6b4fb405fb7b5188` 结论为 `success`，build job `122200846389` 无失败步骤。签名 APK artifact ID `12734959611`，大小 27,774,681 bytes，ZIP SHA-256 `4551c300a800a7fd2cbd91cf22ffeddcff22033a4e6a6d5b570713c0751ee581`。该包只用于确认图标形状详情页导入修复，不是最终交付包。
- 对照 `bhu0.m6094l` → `vhw0.m138133o` → `vhw0.m138132lo` 以及 `d8t0.m88393lO` 的真实交互实现后，确认原版使用 ripple/clickable、启用状态 alpha、父级分段形状的 spring morph；未发现之前自加的整行 0.985/0.96 按压缩放曲线。因此本批次撤除 `SegmentedListItem`、顶部返回按钮、Material 3 ToggleButton、调色板返回按钮和颜色选项的通用按压缩放/按压阴影动画，保留原生交互反馈、形状 morph、选中状态动画及全局阴影开关；按钮/容器阴影改为稳定静态层级，不再伪称为 Voyage 原始按压曲线。
- `alohaOneUISegmentedColors` 的行容器颜色改为 `surfaceContainerHigh`，确保父子分段行在系统浅色/深色方案下有可辨识的框体底色；行框体仍继承 `LocalListItemShapes`，全局描边宽度与容器阴影开关保持有效。
- 图标形状详情页选择某一形状后保持停留在详情页，避免每次点击选项都被提前关闭。索引升级增加一次性迁移标记：旧版 0→None(-1)、1→Random(50)、2..51→基础形状 0..49；设置写入和备份恢复会标记新索引格式，避免升级后旧值错位。
- 本次改动仍仅限测试分支，不修改 `main`。本次提交后继续检查最新 Actions，编译/签名失败则按日志修复。


## 2026-10-27 后续推进：开关/滑条交互去除无源码依据的按压缩放

- Build #1688 / Run `40508054207` 对提交 `a8f2c31f8005d7ea5d3533a79d5a878d63df8a31` 结论为 `success`，build job `122202380940` 成功且无失败步骤。签名 APK artifact ID `12735063980`，大小 27,769,473 bytes，ZIP SHA-256 `3ab24c05595a91c41f8acbec8b8ede2231aabbc27cec98bd62a22ef034f9f8f9`。该包用于验证本批之前的源交互修正，不是最终交付包。
- 进一步对照 Voyage 的 `j7w0.java`（八种开关样式的实际渲染分支）与 `h7w0.m99521O`（滑条渲染）后，未发现 Aloha 当前额外添加的开关整体 0.985 缩放、thumb 0.96 缩放或滑条 thumb 0.94 缩放。已移除这些人为按压曲线，保留开关 checked 状态的 thumb travel 动画、滑条原生拖动行为、Material ripple/触觉反馈，以及全局阴影开关；开关/滑条阴影改为稳定的 2dp，不再随按压额外跳变。
- 补齐色彩风格与色彩标准标签的繁体中文资源（香港、台湾），让系统使用简体/繁体中文时都能切换到对应文案；其他语言继续使用 Android 默认英文回退。标题、详情文字和选项状态颜色均使用 MaterialTheme 色彩角色。
- 仍需继续逐项核对开关 8 种样式与滑条 4 种样式的几何、颜色和动画参数；当前移除无源码依据的缩放曲线不等于全部样式已完成 1:1 验收。本次只写入测试分支，不改 `main`；提交后继续监控最新 Actions。


## 2026-10-27 后续推进：标签页按钮反馈统一为源码交互

- Build #1689 / Run `40508687812` 对提交 `90c37141d47d5b10747ab05e758908acc98f6d30` 结论为 `success`，build job `122204551282` 成功且无失败步骤。签名 APK artifact ID `12736527067`，大小 27,768,067 bytes，ZIP SHA-256 `cffd0c5b1f5bc2ed31b31ac55a2cdc5a9ca3da84e816f95a2ee88aec4bdb9f53`。该 APK 仅对应上一 HEAD，不能当作当前后续改动的产物。
- 继续审计 Material 组件目录后发现 `ExpressiveTabRow.kt` 仍有先前通用化添加的 0.96 按压缩放和动态按压阴影。按原版页面控件的 ripple/选择状态逻辑移除这组无来源依据的缩放曲线，保留 Material 3 Tab 的选择指示器、ripple、主题动态文字颜色与全局按钮阴影开关；选中/未选中阴影按静态层级显示。
- 目前本批已覆盖：主题设置共用选择行/弹出选项、图标形状详情页、设置行、顶部返回按钮、ToggleButton、颜色选项、开关、滑条、标签页的框体/交互。仍继续逐项核对 Voyage 原始几何/样式参数；图标形状 50 条真实路径尚未完成移植。
- 本次只修改测试分支，不改 `main`。提交后继续监控 Actions。


## 2026-10-27 后续推进：详情子页面滚动与返回按钮接入共享框体

- Build #1690 / Run `40509173337` 对提交 `d9206c0c76620df87f0d8228ea0920d88edbadd7` 结论为 `success`，build job `122206215760` 成功且无失败步骤。签名 APK artifact ID `12736277758`，大小 27,767,503 bytes，ZIP SHA-256 `2c9e9c3b8e5473c5c20f398bb4e4c8f1ad8a1bd3aa9dec243a38b44f6e6e0fed`。该 APK 只对应标签页交互修正之前的 HEAD，不是当前产物。
- 图标形状详情子页面的滚动容器现已接入 `TopAppBarDefaults.exitUntilCollapsedScrollBehavior` 的 nested scroll connection，避免标题栏声明了滚动行为却不随列表滚动；返回按钮改用共享 `TopBarBackButton`，因此遵循全局按钮阴影/轮廓设置，不再出现详情页返回按钮与其他设置页风格分离。
- 本次仍只修改测试分支，不改 `main`；继续检查新 Actions。


## 2026-10-27 后续推进：修正全局滑条样式接线与波浪形状缓存

- 核查确认通用 `AlohaOneUISlider` 的 Material 分支此前仍使用默认 Material Slider，因而普通设置页与详情子页面并不会随 `LocalMaterialSliderStyle` 变化；设置阴影的 modifier 也覆盖整条 Slider 触摸区域，容易形成灰色前景投影且压低轨道底色。
- 提交 `7b005d338c68b04db9f9f1d84137d686cbd026dd` 将四种样式共享绘制器接入通用 `AlohaOneUISlider` 的 Material 分支；提交 `8be42e1e9647d735085601ae8d5e1965200fd844` 将阴影从整条 Slider 移至实际滑块，并将两个组件共享的轨道/滑块绘制器开放为 internal。普通 Material 设置页与子页面凡使用通用滑条，都读取 `LocalMaterialSliderStyle` 绘制活动/未活动轨道、滑块、离散刻度与全局轮廓描边。
- 滑块阴影保持稳定 2dp，并受 `LocalMaterialShadowSliders` 控制；不添加未在 Voyage 源码中确认的按压缩放或按压阴影动画。滑块内层颜色改用 Material `surface` 角色，避免固定白色不随系统明暗模式变化。
- 提交 `fc6e6cccaed7d2495cb36d986dbfc726ac8e766b` 为 `VoyagePathShape` 增加 24 项 LRU Outline 缓存，缓存键包含尺寸、density、布局方向和四角半径；`voyageCornerShape` 使用 `remember(style, scaledRadius)` 复用 Shape 实例，减少 Wavy 切换/滚动时反复采样整圈波形、分配采样点和生成二次曲线造成的卡顿。
- 完成度边界：本批修复通用 Material 滑条的样式接线、轨道底色/阴影位置与 Wavy 重复计算；仍须依据 Voyage 的 `kdj0.java`、`h7w0.m99521O`、`gaf0/jaf0/iaf0/haf0`、`y8f0.m144819Il/m144822lo` 核验四种滑条的真实几何、颜色、thumb 造型和交互状态。当前绘制器仍是 Aloha 共用适配，不应标记为 Voyage 1:1 已验收。
- 本次只修改测试分支 `test/voyage-theme-1to1-20261026`，未修改 `main`。必须监测最新 Actions；构建失败时继续按真实日志修复。


## 2026-10-27 构建故障记录与修复：滑条自定义绘制 API

- 第一次接入通用滑条的提交 HEAD `edb376f4b4bd1bec1d3d10140a47cee995ac50b8` 对应 Build #1696 / Run `40510263954` 失败。真实日志指出 `SegmentedSliderItem.kt:177,185` 在 Canvas 绘制 lambda 内读取了 Composable MaterialTheme；`AlohaOneUISlider.kt:137` 使用了不匹配的 Slider 重载，导致自定义 thumb/track lambda 的类型推断和 Composable 上下文连带失败。
- 修复提交 `fe87bddcb633a454a9c2b892d31e28dfbbd1dbe4` 将 surface 色彩角色提前在 Composable 上下文读取并传入绘制作用域；提交 `d9776ddb8b34af02a91555de59f5244352a9bda2` 改用 `rememberSliderState` 对应的自定义 track/thumb 重载，并同步 controlled value。
- Build #1698 / Run `40510728838` 对 HEAD `d9776ddb8b34af02a91555de59f5244352a9bda2` 最终结论 `completed / success`。以下步骤均成功：源码校验、`:app:compileDebugKotlin`、加载 owner 签名密钥、构建签名 APK、包名/稳定证书校验、Actions Artifact 上传。
- 成功产物 artifact ID `12737106641`，名称 `AlohaOneUI-Signed-Test-APK`，大小 `27,772,892` bytes，Artifact ZIP SHA-256 `4e0f515f38cb66e61db6c5e2af8c7ff9208601568ac72d74bef6c74f313eed4c`。这证明该 HEAD 构建成功，不代表四种滑条的源码级视觉 1:1 已完成；本阶段仍需继续做原始几何与交互对照。


## 2026-10-27 后续修正：撤掉未经源码证实的滑块外形

- 用户指出滑条滑块仍未按反编译源码移植；复核后确认前一版 `VoyageSliderThumb` 中同心圆环、内嵌圆点、圆角方块以及 20/24/16dp 的混合尺寸都是自行推导的近似绘制，不应作为 Voyage 原版交付。
- 已直接从本地提供的 `voyage-apk-theme-inspection.zip` 读取真实反编译文件：
  - `decompiled/sources/p000/kdj0.java`：4 个样式枚举；
  - `h7w0.java:m99521O(...)`：实际分派到四条组件实现；
  - `y8f0.java:m144825l/m144829o/m144826lI`：其中一条绘制路径；
  - `vhf.java:m138079I/m138080O/m138077ll` 与 `rhf.java`：另两条路径的绘制实现，默认绘制容器尺寸 32dp；
  - `ncw0.java`、`y3w0.java`、`w3w0.java`、`v2t0.java`：样式分流后的组件入口。
- 已提交 `53bfaa36aca6236ead6e0553023dce0aecbc300a`：删除未经源码证实的多层圆环、中心点和方块造型，按反编译调用链所见的 16dp / 32dp 绘制槽尺寸调整 thumb 尺寸分组。已提交 `d2cfbf799342ae79d65c14fdefbbf571ab677107`：同步修正通用 `AlohaOneUISlider`，避免普通页面与详情页继续使用旧的猜测尺寸。
- **重要验收边界：**本次先撤销明显错误的手工造型并依据源码调用尺寸校正；四条路径的最终 Canvas 几何、颜色状态与动态行为仍需继续逐段移植，不能标记为滑块 1:1 完成。源码中 `vhf.m138077ll` 还使用 2dp 绘制宽度、enabled/active 多组颜色以及 tick positions；需要把这些行为和对应槽位准确区分，不能把轨道/tick 绘制误当成 thumb 本体。
- 只改 `test/voyage-theme-1to1-20261026`，未修改 `main`。下一步检查本次最新 Actions，编译错误按日志修复；同时继续追踪每条样式的 thumb slot 与 track slot 对应关系，再做真正源码移植。


## 2026-10-27 滑块继续移植：纠正 thumb 尺寸与 track 尺寸混淆

- 继续逐段读取 `voyage-apk-theme-inspection.zip` 中四种 Slider 分支。反编译的 `h7w0.m99521O(...)` 确认四条实现入口分别为 `ncw0.m116420I`、`y3w0.m144516l`、`w3w0.m139521l`、`v2t0.m136842l`。
- 进一步核对 `j5v0.m105238O` 第 144 行与 `z4v0.m147159l` 第 695 行：两处都对实际 thumb 交互/命中槽应用 `Modifier.size(20.dp, 20.dp)`。此前把 `y8f0` 的 16dp 轨道高度与 `vhf` 的 32dp 轨道容器误当成 thumb 尺寸，这个判断已纠正。
- 代码提交 `d482f9a8e0cc20c850c9ae7f29cb6f4de72f3e23` 与 `153944624a2a2d788868ca8afb62ba3b5012c6b5`：设置页专用 Slider 与通用 `AlohaOneUISlider` 的 thumb 槽统一改为源码可确认的 20dp × 20dp；轨道高度仍单独配置，不再从轨道尺寸推导滑块尺寸。
- 当前只确认并修正 thumb 尺寸/命中槽；thumb 本体的填充形状、描边、颜色状态、按压/拖动动画仍需继续追踪各实现的实际 slot 和 Canvas/Modifier 链。现有 `VoyageSliderThumb` 的单圆形只是临时占位，不能视为源码级完成；本条记录不把它标记为 1:1 验收。
- 本次仅在测试分支 `test/voyage-theme-1to1-20261026` 修改，未触碰 `main`。接下来监测当前 HEAD 的 Actions，并继续追踪 thumb 的真实绘制入口。


## 2026-10-27 用户反馈：滑块缺少背景与阴影，恢复源码可确认的交互阴影

- 用户实际反馈当前滑块视觉变化不明显，背景和阴影缺失。复查当前 HEAD 后确认：VoyageSliderThumb 的 shadowEnabled 参数此前没有用于任何 Modifier，因而全局滑块阴影开关虽然传入组件，实际 thumb 仍完全没有阴影；通用 AlohaOneUISlider 也未把共享 MutableInteractionSource 传入 thumb。
- 再次核对 Voyage 反编译 vhf.m138081l(...)：该方法将实际 20dp thumb 交互槽与阴影 modifier 连接；交互列表为空时采用 1dp 阴影，交互进行时采用 6dp 阴影，并受启用状态控制。当前修复提交 3ad1aae5f8c0ef0d04bc532af5c40fa63bc9ad5d 将按下状态接入 thumb 的阴影层，并确保阴影只作用于滑块本体，不覆盖整条 Slider 触摸区域；72089c469446068eeba5f1926a737836af00ecaa 将通用 Material Slider 的 interaction source 接线到共享 thumb。
- 未完成项必须明确：当前只修复已查实的阴影缺失。四种 Voyage 样式各自的 thumb 本体背景/填充、描边与 enabled/active 颜色映射尚未全部从 qhf、y8f0 及四条组件路径逐项还原；轨道背景色与刻度绘制也仍需对照原始参数验收。因此本次不能声称视觉 1:1 完成，后续需继续源码追踪并在构建日志通过后验证 APK。
- 仅更新测试分支 test/voyage-theme-1to1-20261026，未修改 main。本次源代码改动后必须检查最新 Actions；若编译失败，按真实错误修复。


## 2026-10-27 继续移植：复核滑块阴影作用域与样式差异（未冒进修改参数）

- 本轮重新读取当前测试分支 `SegmentedSliderItem.kt`、`AlohaOneUISlider.kt`、`ExpressiveSwitch.kt` 以及最新 Actions。当前 Build #1708 / Run `40518891979` 对 HEAD `be424b0da70fe532cf490c27000d37be3e978a67` 结论为 success；这是编译结果，不是原版视觉验收。
- 当前 `VoyageSliderThumb` 已把 shadow modifier 放在 20dp thumb Box 上，而不是整条 Slider modifier；通用 `AlohaOneUISlider` 也已传入同一 `MutableInteractionSource`。但用户反馈的实际画面仍是滑块阴影不明显/轨道呈现异常，说明仅凭 Modifier 接线不能确认原版阴影效果已复现。下一步应继续沿 Voyage `vhf.m138081l`、四条 Slider 入口与父级布局/裁切链逐项核实，尤其区分 thumb hit slot、thumb 绘制内容、track 绘制层及其父级 clipping/elevation；未确认前不继续叠加新的阴影或形状猜测。
- 复核 `SegmentedSliderItem.kt` 与 `AlohaOneUISlider.kt` 后，确认两处仍共享手写的样式索引颜色映射和轨道高度映射（4/8/4/6dp）；`VoyageSliderTrack` 仍以单一 Canvas 的圆角轨道与刻度绘制概括多种样式。此实现目前不是四条 Voyage 原始路径的完整移植。接下来需把原版 `kdj0` 四个枚举到 `ncw0/y3w0/w3w0/v2t0` 的实际调用链逐条映射，再按各自绘制参数分开实现，而不是继续共用一套推断参数。
- `ExpressiveSwitch.kt` 仍包含按样式索引选择的硬编码颜色、corner fraction、thumb radius/inset 与统一 Canvas 轨道/圆形 thumb。用户录屏展示原版八种开关有明显不同的几何形状；当前实现不能据此认定 8 种已还原。后续以 Voyage `j7w0.java` 八条分支的真实绘制方法和参数为依据替换，不以调节现有 fraction 代替移植。
- 本轮没有把未经证实的值写入产品源码；维护记录仅记录当前 HEAD 的真实差距与下一步取证路径。测试分支保持 `test/voyage-theme-1to1-20261026`，未修改 `main`。Build #1708 仍不得作为最终交付 APK；所有未完成样式完成后，需对最终 HEAD 构建、签名、包名/版本、SHA-256 与 APK 完整性重新验收。


## 2026-10-27 继续移植：滑条轨道阴影联动第一步

- 分支仍为 `test/voyage-theme-1to1-20261026`，未修改 `main`。
- 修改 `app/src/main/java/com/alohaoneui/app/ui/component/material/SegmentedSliderItem.kt`：为可见轨道层加入独立阴影 modifier；阴影形状随现有轨道形状参数分支变化；轨道阴影读取 `LocalMaterialShadowSliders`，与滑块共用同一开关，不再只控制滑块。
- 修改 `app/src/main/java/com/alohaoneui/mod/ui/component/AlohaOneUISlider.kt`：通用 Material 滑条入口也把相同阴影开关传入轨道绘制层，避免设置页和通用控件入口状态不一致。
- 相关提交：`18299643ebf30218886dd350503713eb6d90a601`（设置页轨道阴影）；`ef477acb608c80267d5575f95fb5b51ab4fb6901`（通用滑条入口同步）。
- 这是一项针对用户指出的“轨道背景之后还有阴影，阴影滑条开关控制开关”的第一步代码修改，不代表已完成 Voyage 1:1。当前轨道/滑块几何映射仍有硬编码，后续需继续从 Voyage 原始实现与录屏逐项校准；尤其滑块形状、圆角、尺寸及颜色的全局联动尚未完成。
- 验收状态：代码已提交到测试分支；尚未确认新提交对应的 Actions 构建结果，也未完成 APK 下载、签名/包名/版本/SHA-256 验收。不得将旧构建当成本轮交付。


## 2026-10-27 后续推进：滑块与轨道接入全局主题形状联动

- 本批产品代码只写入 `test/voyage-theme-1to1-20261026`，未修改或合并 `main`。
- 在复核 `SegmentedSliderItem.kt` 与 `AlohaOneUISlider.kt` 时，确认滑块本体仍固定用 `drawCircle` 绘制，阴影固定使用 `CircleShape`；轨道外形也通过 slider style 索引固定映射为 50%、22%、36% 圆角。这样全局 Material 形状样式/圆角比例变化时，滑块与轨道阴影外形不跟随共享主题形状工厂，造成组件联动断开。
- 修改 `app/src/main/java/com/alohaoneui/app/ui/component/material/SegmentedSliderItem.kt`：滑块本体改为使用共享 `voyageCornerShape(size.minDimension / 2f)`，将同一个 Shape 同时用于滑块阴影、裁切和填充；轨道/轨道阴影使用 `voyageCornerShape(height / 2f)`，并在可见轨道层应用相同 Shape 的 clip。全局形状样式和圆角比例因此会传递到设置页滑块本体及轨道容器，不再强制画成圆形/胶囊形。
- 修改 `app/src/main/java/com/alohaoneui/mod/ui/component/AlohaOneUISlider.kt`：通用 Material Slider 继续复用 `VoyageSliderThumb` / `VoyageSliderTrack`，因此普通设置页和详情子页面使用相同的主题形状绘制入口，避免两处各自维护不同的滑块几何。
- 相关提交（均在目标测试分支）：
  - `9b59c7201aa919f11891fec66e0f4f0977398e60` — `fix: link slider thumb and track to shared theme shape`
  - `2980fd16b23d63ec77c94fc060e45aa503034806` — `fix: link generic slider to shared theme shape factory`
  - `f6a9f00571bc68aa35c7f120c9b1ba8b79c31d00` — 清理不再使用的固定形状导入
  - `40edb3973e077cffc690596c5434a6e184d5a32c` — 清理通用滑条文件中的未使用导入
- 验证边界：该批修复的是“全局形状样式/圆角设置必须联动到滑块与轨道容器”的明确断链，不代表四种 Voyage Slider 原版绘制路径已完整移植。活动/非活动轨道颜色、四种样式的实际轨道尺寸与内部绘制参数、thumb 的原版填充/描边颜色仍需按 `kdj0`、`h7w0`、`y8f0` 及四条实现路径逐项核对，不能将当前遗留的颜色/轨道参数映射宣称为原版数值。
- 构建状态：提交 `40edb3973e077cffc690596c5434a6e184d5a32c` 的 combined status 返回空状态列表，commit workflow 查询也未返回关联运行；因此当前**不能**认定 CI 已通过，也没有新的 APK 可交付。下一步必须继续查找该分支对应的 Actions run/构建产物；如果没有触发构建，应在确认 workflow 配置后触发真实构建，再按日志修复并核验 APK。
- 下一步：继续恢复并对照四种 Slider 的原版颜色/几何/轨道阴影绘制；完成后继续处理八种 Switch、50 条原版图标路径和 Smooth G²，并持续检查主题页面状态与组件共享联动。最终 APK 仍须匹配最终 HEAD，核验包名/版本、固定 Owner 签名、V2 签名、APK/ZIP 完整性及 SHA-256。


## 2026-10-27 后续推进：统一两处滑条的样式参数来源

- 产品源码提交：`2fd8ffa45e6933747fa2ea15624ed38ce16d321e` 与 `3ba9ff26b58d551c33702b11d61fededd03ddc44`，均只写入测试分支 `test/voyage-theme-1to1-20261026`。
- 新增共享的 `VoyageSliderAppearance` / `voyageSliderAppearance()`，把 Material 滑条当前的样式索引归一化、主题活动/非活动颜色、20dp thumb slot 和轨道高度集中到 `SegmentedSliderItem.kt` 一个入口；设置页滑条与 `AlohaOneUISlider.kt` 的通用 Material 分支都改为消费该共享配置，不再各自复制同一套 `when(style)` 参数表。
- 这次统一的是配置入口与状态联动，不把现有颜色/高度映射说成已确认的 Voyage 原版数值。共享函数注释已明确：在四条原始 draw path 逐项核实之前，当前映射只是兼容现有项目状态；下一步要继续依据反编译方法和资源校正数据本身，而不是让两处重复保持同一份未验证硬编码。
- 当前最新产品源码提交为 `3ba9ff26b58d551c33702b11d61fededd03ddc44`。截至查询时，GitHub combined status 返回空状态列表，commit workflow 查询也未返回运行记录；没有可验证的当前 HEAD 构建结果或新 APK。不得把之前 Build #1708 或更早 Artifact 当作包含本批修改的产物。
- 后续继续：检查当前分支 Actions 触发方式与最新运行；修复任何实际编译问题；随后从 `kdj0` / `h7w0` / `y8f0` 和四条实现类逐项恢复 Slider 的真实颜色、轨道、thumb、描边、阴影及启用状态行为。完成后继续其他主题组件，不停在配置抽取这一步。


## 2026-10-27 滑条共享绘制入口的编译前自检

- 最新产品代码修正提交：`fd0aca9f1f4f8cdf4178709b97b55bf129d0eb3e`。复核 `VoyageSliderThumb` 时补正 Compose Box 的空内容 lambda，并用 `minOf(size.width, size.height)` 计算圆形基准半径，避免依赖不必要的 DpSize 扩展属性。
- 当前滑块的阴影、裁切和填充共用 `voyageCornerShape` 返回的主题 Shape；轨道的阴影与可见层也共用同一主题 Shape。设置页与通用 Material Slider 共用 `voyageSliderAppearance()` 配置入口。
- 当前最新维护记录提交之前，针对产品提交 `fd0aca9f1f4f8cdf4178709b97b55bf129d0eb3e` 查询到的 combined status 为空，commit workflow 查询没有关联运行。**尚未进行真实编译验收**；这不是成功构建记录。必须继续找到可查询的最新 Actions run 或可用的构建触发路径，构建失败时按真实日志修复。


## 2026-10-27 本批滑条改动的源码收尾与 CI 查询边界

- 最后清理提交：`66f6089f568c7cba7fe7274f99558d49d32709c0`（移除滑条文件不再使用的 `fillMaxSize` 导入）、`62a770e2a2906362fea911bae7c128ed3c85b640`（移除通用滑条文件不再使用的 `DpSize` 导入）。修改均在 `test/voyage-theme-1to1-20261026`。
- 已读取仓库的 `.github/workflows/build.yml`，确认 workflow 配置了对 `main`、`test/**`、`migration/**` 分支 push 的触发条件，并有 `workflow_dispatch`。当前可用 GitHub 查询接口未提供“按分支列出所有 push-triggered runs”的列表操作；对产品提交查询 combined status 为空，commit workflow 查询仅覆盖 PR-triggered runs，因此无法从这两项空结果推断 push 构建没有运行，也不能据此宣称构建成功。
- 当前尚无本批 HEAD 对应的、可核验的真实构建终态和 APK 产物。下一步必须通过 Actions 实际 run/job/artifact 记录核验构建结果；若失败，读取诊断日志继续修复。最终 APK 需与最终 HEAD 一致，不能拿 Build #1708 或更早的阶段包代替。

## 2026-10-27 八种 Switch 源尺寸与交互 thumb 几何校正

- 已恢复并校验 Voyage 原始检查归档：Actions artifact `12586213779` 下载为 `voyage-inspection-report.zip`，ZIP 完整性检查通过；本批直接对照其中的 `p000/j7w0.java` 分派和 `z4v0.java`、`frt0.java`、`it70.java`、`ckw0.java`、`rdf.java`、`y2t0.java`、`l140.java` 原始实现，没有用猜测代替尺寸来源。
- 本批代码提交：`908b228305dc700358d5185f1ae67ff8bf31c739`，仅位于 `test/voyage-theme-1to1-20261026`，未修改/合并 `main`。修正了第 6 种 Liquid Glass 开关：原始 `rdf.m127933O → m127932I` 的轨道是 **64×28dp**，此前错误地与第 5 种 Cupertino 一起使用 51×31dp；同时把该轨道圆角半径修正为 14dp。
- 修正了源代码可核实的 thumb slot 尺寸与交互扩展：第 0 种 `z4v0.m147162ll` 为固定 20dp；第 1 种 `frt0.m95463l` 为静止时 16/24dp、交互时扩展至 28dp；第 2 种 `it70.m103876l` 为 20dp；第 3 种 `ckw0.m9293l` 为静止 12dp、交互时 17dp；第 5 种 `rdf` 为 20dp；第 6 种 `y2t0.m144445l` 为静止 20dp、交互时 23dp；第 7 种 `l140.m110104l` 为 22dp。交互扩展现在观察共享 `MutableInteractionSource` 的 pressed 状态，不再错误地仅随 checked 状态改变。
- **真实构建验证通过**：GitHub Actions [Build #1728](https://github.com/iskyxpcity/Oneuixpcity/actions/runs/40565117750)，run ID `40565117750`，对应产品代码提交 `908b228305dc700358d5185f1ae67ff8bf31c739`。Compile、owner signing key load、signed APK build、package/stable-certificate verification、artifact retention 均成功。
- 该次测试 APK 已从 artifact 下载并校验：`AlohaOneUI_1.0.261025_BUILD_1728.apk`，包名 `com.alohaoneui.app`，versionCode `16`，versionName `1.0.261025`，大小 **28,773,833 bytes**，SHA-256 `9f90a6beefd602f09129ee30dcb2c4f141b337f794387e34cb4dfca775fb4919`；签名证书 SHA-256 `93bf61a6493c6b336383832eb41301bd8fe061f43566e7e384e3fdd9fc1c26a1`。artifact 名称 `AlohaOneUI-Signed-Test-APK`，ID `12763026483`。这是一份已验证的**测试分支构建产物**，并非 Voyage 主题迁移最终版；本次运行没有发布新的 GitHub Release 资产。
- 当前仍未完成的事项必须继续按原版逐项核实：`IconShapePresetPicker.kt` 的 51 个形状目前仍使用临时几何生成器，尚未 1:1 复刻 `vqp.java` / `shq.java` / `ewm.java` / `qha0.java` 的原始形状目录；四种 Slider 的轨道、thumb、颜色和描边仍未逐条对照 `kdj0.java` / `h7w0.java` / `y8f0.java` 与四条实际实现；Wavy、Smooth G² 与其他形状也仍需完成整套截图/数值核对。不得把本次 Switch 几何修正或测试 APK 宣称为整体迁移完成。

## 2026-10-27 后续推进：滑条真实形状轮廓与交互阴影联动

- 原始依据：已重新下载并校验 Voyage 检查归档 Actions artifact `12586213779`（`voyage-inspection-report.zip`，约 204 MB）。直接核对 `decompiled/sources/p000/vhf.java`：`f288572I` 是 20×20dp 槽位，`f288573O=1dp` 为静止阴影，`f288576o=6dp` 为交互阴影，`f288575ll=2dp` 是刻度绘制直径；`m138081l` 根据非空 interaction list 在两种阴影层级间切换，`m138077ll` 将轨道线段与刻度分开绘制。
- 本批只修改 `SegmentedSliderItem.kt` 的共享滑条实现：滑块阴影从只观察 Press 改为同时观察 Press、Drag、Hover、Focus，避免实际拖动时仍停留在 1dp；设置页和通用 Material Slider 共用此入口，因此两处同步生效。
- 轨道不再只靠 `clip(trackShape)` 裁切一条另行硬编码圆角的矩形；现在按 `voyageCornerShape` 生成的真实 Outline 绘制未活动轨道、活动轨道和轮廓。这样全局 Material Shape Style / Corner Scale 改变时，Wavy、Smooth G²、切角及其他共享形状会直接改变轨道外形。离散刻度半径统一为源码可确认的 1dp。
- **未完成边界：**这一步修复的是共享形状轮廓和交互阴影的断链，不代表四种 Voyage Slider 的完整 1:1。四条分支的轨道线宽、具体颜色角色、thumb 本体填充/描边以及状态动画仍需按 `kdj0.java`、`h7w0.java`、`ncw0.java`、`y3w0.java`、`w3w0.java`、`v2t0.java`、`y8f0.java`、`vhf.java` 和对应 slot 继续核对。
- 当前修改只计划写入 `test/voyage-theme-1to1-20261026`，未修改/合并 `main`。本批 Actions 构建结果待实际运行后追加；旧 Build #1731 不能证明本批代码已通过编译。

## 2026-10-27 构建故障定位：当前 Compose 版本不暴露 drawOutline 扩展

- Build #1732 / Run `40569099232` 对提交 `c294b364df1dd969104c3f3ab5c84b5235652d26` 的真实结果为 `failure`，失败步骤为 `:app:compileDebugKotlin`。诊断日志只有一类编译错误：`SegmentedSliderItem.kt` 中 `drawOutline` 在导入处及 3 个调用点均为 unresolved reference；不是签名密钥、包名或 APK 打包故障。
- 修复方案：改为使用当前项目 Compose 版本确实提供的 `DrawScope.drawRect/drawPath`，增加本地 `drawVoyageOutline` 适配器，分别处理 `Outline.Rectangle`、`Outline.Rounded`（保留四角路径）和 `Outline.Generic`（直接绘制 Voyage 自定义路径），继续保留轨道形状联动意图，不回退到固定圆角矩形。
- 本次修复只写测试分支；修复提交后的 Actions 尚待运行。不得把 Build #1732 标为成功，也不得把旧 APK 当作本批产物。


## 2026-10-27：Actions Run 40569099232 构建失败诊断

- 已直接读取 [Actions run 40569099232](https://github.com/iskyxpcity/Oneuixpcity/actions/runs/40569099232) 的 build job 日志及 Artifact 元数据。
- 运行对应分支：`test/voyage-theme-1to1-20261026`；运行 HEAD：`c294b364df1dd969104c3f3ab5c84b5235652d26`。
- 真实失败原因：`SegmentedSliderItem.kt` 第 37、270、273、281 行调用了当前项目 Compose 依赖中无法解析的 `drawOutline`，编译器报 `Unresolved reference 'drawOutline'`。失败发生在 `Compile Aloha OneUI before the owner-signing gate`，因此签名 APK、release 验证和最终 Artifact 均被跳过；本次运行**没有产出可交付 APK**。
- 已核对当前测试分支上的 `SegmentedSliderItem.kt`（当前文件 blob SHA：`715e0356c4b09360aea201c1ee1107a0ffa91abd`）：文件已经不再导入或调用不可解析的 `drawOutline`，改为本地 `DrawScope.drawVoyageOutline()`，分别处理 `Outline.Rectangle`、`Outline.Rounded` 和 `Outline.Generic`。这说明失败点在当前文件中已被改写，但仍须由新的真实构建确认没有后续编译问题，不能仅凭源码推断成功。
- 本次运行只生成诊断 Artifact `AlohaOneUI-Build-Diagnostics`（Artifact ID `12763389345`，ZIP SHA-256：`54bcdd3054484558b7a83a2e82bcef75a9bfc56dec9762b2ed92431592474b6e`）；它不是 APK。
- 后续操作：以当前测试分支最新代码重新触发/检查 Actions，若出现新的编译错误继续按日志修复。必须等新的构建成功并取得该次 run 的 APK Artifact 后，再验证包名、版本、签名和 SHA-256。不得修改或合并 `main`。

## 2026-10-27 构建故障定位与第三轮修复：drawscope 类型包与 Switch 形状联动

- Build #1733 / Run `40569571885` 失败；Build #1734 / Run `40569677922` 也失败。两次都在 `:app:compileDebugKotlin` 结束，精确错误为 `DrawStyle` 与 `Fill` 的导入包不正确（它们属于 `androidx.compose.ui.graphics.drawscope`）。本提交已把导入修正到该包；先前两个失败 run 均没有产出 APK。
- 本批把 `drawVoyageOutline` 提升为同包可复用的 `internal` 绘制扩展，并用于八种 Switch 的轨道填充、轨道轮廓、thumb 与 Liquid Glass halo；thumbContent 也使用同一个 `thumbShape` 裁切。每个风格原有的源尺寸、角半径基值、交互尺寸、颜色分支和图标分支保留，只替换原先绕过 Shape 系统的圆角矩形/圆形绘制。
- 这让全局 Shape Style / Corner Scale 真正影响 Switch 与 Slider 的轨道和 thumb。该改动仍是联动修复，不代表八种开关及四种滑条的所有源动画、颜色角色和图标路径已完成 1:1。当前提交后的 Actions 结果待验证。

## 2026-10-27：Build #1735 签名测试 APK 已验证；图标形状随机预览修正

- GitHub Actions：[#1735 / Run 40570224070](https://github.com/iskyxpcity/Oneuixpcity/actions/runs/40570224070)，对应产品代码提交 `b70124ae89f5181301ad42177e43a52e7f56323f`，结果 `success`。
- 真实构建步骤均通过：源代码校验、Kotlin 编译、加载现有 owner signing key、构建 owner-signed APK、包名/稳定证书校验、保存签名 APK Artifact。
- APK：`AlohaOneUI_1.0.261025_BUILD_1735.apk`，28,777,069 bytes；SHA-256 `9d9bad0abe53f1fe559047ba112de319a4fbf1bb287af6d0b628e759ac84796c`；签名证书 SHA-256 `93bf61a6493c6b336383832eb41301bd8fe061f43566e7e384e3fdd9fc1c26a1`。Manifest 验证：包名 `com.alohaoneui.app`、versionCode `16`、versionName `1.0.261025`、minSdk 31、targetSdk 36、compileSdk 37。
- Artifact：`AlohaOneUI-Signed-Test-APK`，ID `12764733250`，链接为 [Actions artifact](https://github.com/iskyxpcity/Oneuixpcity/actions/runs/40570224070/artifacts/12764733250)。该 run 是测试构建，**没有发布新的 GitHub Release**，也不代表 Voyage 1:1 迁移完成。
- 源码依据：`shq.m130523I(p7r)` 在 random sentinel 被解析时从非 random shape 列表随机返回一个实际 shape。因此图标形状网格中的 Random 预览不应显示独立 Shuffle 图标；本批改为显示一个稳定的代表性 shape 轮廓，None 仍显示 Block 图标。真实 51 个预设的几何替换仍未完成：`iconShapeForStyle` 当前仍调用临时 `geometricIconShape`，后续必须按 `vqp.java` 的 26 个基础 shape 与 25 个 `qha0` RoundedPolygon 数据逐项移植。
- 当前所有修改仅在 `test/voyage-theme-1to1-20261026`，未修改/合并 `main`。


## 2026-10-27 用户反馈跟进：滑条未填充轨道底色/阴影与 Switch 滑块尺寸

- 本批产品代码提交，均只在 `test/voyage-theme-1to1-20261026`：
  - `bdba534d7b3e60288d88aa44364ce643c64feec8` — `SegmentedSliderItem.kt`：未填充轨道颜色改为主题 `surfaceContainerHighest`，与 Switch 关闭轨道采用相同的中性容器底色来源；将 `LocalMaterialShadowSliders` 开关传入共享 `VoyageSliderTrack`，在轨道自身几何与共享形状上添加 1dp 阴影，阴影开关关闭时不绘制该阴影。轨道形状继续使用 `voyageCornerShape()`，因此仍由共享主题形状工厂驱动。
  - `08351676be79465ec95f8180ea89c4844b88bafc` — `ExpressiveSwitch.kt`：把八种 Switch 风格的滑块半径从约 0.30–0.36 倍轨道高度统一增至 0.40 倍，使开/关滑块明显加大；删除 Canvas 内绘制对勾/叉号的图形分支，状态改由滑块位置和颜色表达。外部传入的 `thumbContent` 插槽仍保留，以免破坏调用方自定义内容。
- 说明：这批是根据用户最新录屏反馈做的定向视觉修正。Switch 滑块的 0.40 比例属于本轮调整值，仍需对照 Voyage 原始录屏/DEX 再确认，不将其宣称为已经验证的原版精确值。
- 验证状态：当前还没有本批最新 HEAD 对应的真实构建终态和 APK；必须继续获取 Actions 的真实 job 日志/构建产物并修复可能的编译问题。不能把本轮提交描述成已构建通过。
- 下一步：检查滑条背景底色是否与 Voyage 的 Switch 关闭轨道实际同源；核实轨道阴影在主题开关开启/关闭时的表现、thumb/track 形状联动与描边；继续检查其它主题组件联动，并最终构建、签名与校验 APK。


## 2026-10-27 用户反馈：增大列表选项卡上下间距

- 产品源码提交：`94ce989ac9aa55f69596bf966bc0320363fdb7fa`（`ui: increase vertical spacing between list option cards`）。
- 修改文件：`app/src/main/java/com/alohaoneui/app/ui/component/material/SegmentedList.kt`。
- 将共享列表容器 `SegmentedColumn` 中相邻选项卡的垂直间距从 `2.dp` 增至 `6.dp`，让卡片之间更有呼吸感，同时不改动单个卡片内部排版、形状或点击行为。该共享容器覆盖使用 `SegmentedColumn` 的选项列表页面。
- 本次修改已提交到 `test/voyage-theme-1to1-20261026` 测试分支；`main` 未修改。
- 构建状态：本次产品提交之后尚未核验对应 GitHub Actions 运行与 APK 产物，因此不能宣称已编译通过或已有新 APK。后续需检查最新 HEAD 的构建结果，并在交付前验收 APK。


## 2026-10-27 用户反馈：选择 Wavy（波浪）形状时界面卡顿

- 产品代码提交：`302f439ee306cf5868cc081fab9db839a18a3883`（`perf: share cached outlines for animated wavy shapes`）。
- 定位到 `SegmentedList.kt` 的 `VoyagePathShape`：Wavy 形状会沿整个周边采样、计算正弦位移并构建大量二次曲线；原先 Outline LRU 缓存属于单个 Shape 实例，但列表行在圆角弹簧动画过程中会反复创建 Shape 实例，导致多个列表项难以共享缓存，增加主线程路径生成负担。
- 修复：Wavy Outline 使用跨 Shape 实例共享、容量上限 160 的 LRU 缓存；缓存键包含样式、控件尺寸、密度、布局方向和四角半径。Wavy 动画半径按 0.5dp 分桶以便同帧多个同尺寸列表项复用已生成的轮廓；其他形状仍走原有实例缓存路径，波浪路径的采样/位移算法没有删减。
- 修改只提交到 `test/voyage-theme-1to1-20261026`，没有修改或合并 `main`。
- 验证状态：目前已完成源码级性能优化，但尚未获得本次提交对应的真实 Actions 构建结果，也没有设备帧时间/录屏实测数据；不能先行宣称卡顿已完全消除。后续必须先检查编译和 APK 构建，再在设备上验证 Wavy 切换、动画过程及滚动性能。若仍卡顿，继续优化采样/动画路径并对照 Voyage 外观，不能直接移除波浪形状。


## 2026-10-27 用户反馈：形状样式为 Smooth 时开关不可见

- 修复提交：`7e419dc6faf42f948f3110f5f571ebb6916035ed`（`fix: keep switches visible with smooth shape style`）。
- 修改文件：`app/src/main/java/com/alohaoneui/app/ui/component/material/ExpressiveSwitch.kt`。
- 开关轨道、滑块和样式 5 的 halo 在全局形状样式为 Smooth（索引 3）时，改用稳定的胶囊形 `RoundedCornerShape`；其他形状样式继续走 `voyageCornerShape`。开关的开/关颜色、尺寸、滑动动画、点击语义、描边及阴影逻辑保持不变。
- 这是针对开关控件的安全回退，避免 Smooth 的自定义 G² 路径在开关这类胶囊尺寸下发生退化而导致开关无法绘制；不改变全局 Smooth 样式在其他组件上的使用。
- 本次修改只提交到 `test/voyage-theme-1to1-20261026`，没有修改或合并 `main`。
- 验证状态：已提交源码修复，仍需以真实构建和设备运行确认开关重新显示且交互正常；当前不能声称 APK 已构建通过。


## 2026-10-27 用户反馈：开关选择 Liquid Glass 后缺少液态玻璃视觉效果

- 修复提交：`0b69412b917c55497068994d4e612bbf2a51debb`（`feat: add liquid glass rendering to switch style`）。
- 修改文件：`app/src/main/java/com/alohaoneui/app/ui/component/material/ExpressiveSwitch.kt`。
- 为开关样式 5 增加独立玻璃绘制：轨道和滑块使用半透明纵向渐变、白色高光边缘；滑块增加窄高光反射线，减少此前看起来像普通纯色开关的问题。
- 采用轻量 Canvas 渐变与描边，不引入实时背景模糊或额外 shader，避免给设置列表滚动增加明显 GPU 成本。此实现是液态玻璃的轻量视觉模拟，并非系统级真实背景折射。
- 其他开关样式维持原绘制逻辑；改动只提交到 `test/voyage-theme-1to1-20261026`，未修改或合并 `main`。
- 验证状态：源码已提交，尚待最新 CI/真实 APK 构建与设备视觉验证；需要确认开/关两态、浅色/深色主题下玻璃透明度与高光均可见。


## 2026-10-27 用户反馈：语言跟随系统文案、语言分组框体与切换黑屏闪烁

- 本轮仅提交到 `test/voyage-theme-1to1-20261026`，没有修改或合并 `main`。
- 修正“跟随系统”文案来源：Material 与 Miuix 语言选项不再硬编码英文 `Follow system`，改为使用已有本地化资源 `R.string.settings_theme_mode_system`。该资源在简体中文、繁体中文、日文和韩文资源中已有对应翻译。
- 修正语言父标题固定显示英文的问题：补齐 `settings_group_language` 的简体中文“语言”、繁体中文“語言”、日文“言語”和韩文“언어”资源；英文默认资源保留“Language”。这样标题会随应用实际解析到的当前语言资源显示，而不是一直回退到英文。
- Material 设置页的语言父标题已从框体外的独立小标题改为语言分组框体内的首行，加入语言图标和展开/收起箭头；点击父标题即可展开或收起语言选项，选择项与父标题共用同一个 `SegmentedColumn` 分组框体。
- Miuix 设置页的语言标题同样移入语言卡片内部，改为可点击的整行标题，并显示与展开状态同步的上/下箭头；语言选项随该标题展开或收起。
- 修正切换语言时的黑屏闪一下：`SettingsScreen.kt` 不再在 Android 13（API 33）及以上同时调用 `LocaleManager.applicationLocales` 与显式 `Activity.recreate()`。Android 13+ 的应用语言配置由系统处理，避免重复触发 Activity 重建；API 32 及以下仍保留显式重建路径，以便旧系统重新应用包装后的语言 Context。
- 主要源码提交：
  - `3a65af2a135c390ea07517ff56384af327b97b0a` — Material 语言分组本地化与可折叠框体。
  - `84aed1a21d76703671522ab737a62e15d1709588` — Miuix 语言分组可折叠。
  - `a96ef5c597add25b5e207ab36a0a1fefe45e5f06` — 避免 Android 13+ 重复 Activity 重建。
  - `b46c5faeb3d2cae31279c31b0b45c02fbf3f8335`、`e89dc4d347668e2ccd9f0667672737f04448e87f`、`22a86ce81c96af7e5e642c6e9a3bd011035f0f41`、`e16021192cacf01df188b1ed576ae5bd79e18f97`、`67f36a36b11474d1d3a23caa729fbf39b29381f0` — 补齐简体中文、繁体中文、日文与韩文的语言父标题翻译。
  - `b489ef70062deb59f8150282c4e2b5db5cc77c3f`、`f9ff16b2991f194c13dd61895f99a2eed8d73518` — Miuix 父标题加入明确的展开/收起箭头，并改用项目已有的标题文本样式。
- 当前验证边界：以上是已提交的源码与资源修改；仍须检查最新测试分支 HEAD 对应的真实 GitHub Actions 编译结果，并在设备上确认系统简体中文、手动切换各语言、展开/收起，以及语言切换时的屏幕闪烁。尚未据此宣称已通过构建或设备验证，也没有新的 APK 可交付；不得使用旧 APK 冒充本轮产物。

- 后续源码收尾：`dbaca03a551710fbbccf48f52fee098177522474` 删除了 Miuix 设置页新增的重复 `fillMaxWidth` 导入，避免 Kotlin 编译歧义。由于该提交晚于前述维护记录提交，最终构建必须以更新后的分支 HEAD 为准。


## 2026-10-27 用户反馈：Switch 内部滑块加大饱满，继续校正 Slider 轨道外观

- 所有本轮改动仅提交至 `test/voyage-theme-1to1-20261026`；没有修改或合并 `main`。
- Switch 修改提交：`f17fa6e21024f29a11ac71f23da706c2924747ce`（`ui: make switch thumb fuller and 65 percent of track width`）。
  - 用户明确要求内部按钮更饱满、更长，宽度约占整个开关轨道的 65%。将可见 thumb 改为横向胶囊形：宽度按轨道宽度的 65% 计算，高度约为轨道高度的 72%–82%，并继续通过共享 Voyage Shape 工厂应用全局形状样式。
  - 开关状态位置的行程改为按新 thumb 宽度计算，保证关闭/开启两端均在轨道内；Liquid Glass 样式的 halo、渐变、高光与 thumbContent 插槽也同步改用新的横向尺寸。
  - 原始八种样式的交互状态判定保留为尺寸下限/交互依据，但可见胶囊的 65% 宽度是按用户本轮视觉要求所作的定向调整，不能声称这是 Voyage 原版所有八种开关的原始几何值；需要结合录屏逐个核对。
- Slider 轨道修改提交：`1ad794f840293ac7860995bb958587d6b4a84b4d`（`ui: preserve rounded Voyage shape on filled slider track`）。
  - 修正原先用矩形 `clipRect` 截断整条轨道造成活动轨道前端被切平的问题。活动长度足够时，改为使用同一个 Voyage shape 工厂按当前活动长度单独生成并绘制轮廓；长度很小时保留按真实比例裁切，避免初始值附近突然出现整颗胶囊。
  - 这让活动/未活动轨道的端部形状更协调，并继续跟随全局 Shape Style / Corner Scale；没有改变已核实的 20dp Slider thumb 交互槽，也没有伪造四种 Slider 的原始轨道高度参数。
- 原始依据与未完成边界：维护记录中已核实 Voyage `vhf.m138081l` 的 20×20dp thumb 槽、静止 1dp/交互 6dp 阴影，以及 `vhf.m138077ll` 的轨道段与刻度绘制路径。但四种 Slider 分支（`ncw0/y3w0/w3w0/v2t0`）的轨道宽度、thumb 填充/描边、颜色角色与动画仍需继续逐条比对用户已提供的录屏及反编译分支。不能把本次轨道端部修正记作 1:1 完成。
- 构建/设备验证：本轮两个源码提交之后的最新 HEAD 仍须检查对应 GitHub Actions 最终结果，并在设备上比较开关八种样式与四种滑条的外观/拖动效果。没有新的终态证据前，不声称构建通过，也不把旧 APK 当成本轮产物。


## 2026-10-27 用户反馈跟进：Wavy 仍导致全界面卡顿，界面缩放滑条统一 Material 控件

- 本轮性能修复提交：`3280779067184f6662cb5361e86a249bee1bbe9b`（`perf: avoid serializing Wavy outline generation`），仅修改 `SegmentedList.kt`，仍在 `test/voyage-theme-1to1-20261026`，未改动 `main`。
- 复查发现先前共享 Wavy Outline LRU 虽然能命中缓存，但缓存未命中时仍在共享缓存锁内部同步生成波浪路径。Wavy 路径每次需要沿周边采样并生成大量路径段；形状切换动画期间，多种尺寸的组件会频繁触发未命中，导致昂贵路径生成被全局锁串行化。
- 本轮修复将路径生成移出共享缓存锁：先在锁内查询，未命中后在锁外计算，再回到锁内二次检查并写入，避免不同组件的 Wavy 路径生成互相阻塞；共享 Wavy 缓存容量从 160 增至 768，降低一屏多个尺寸控件及动画过程中的缓存抖动。路径采样/波浪轮廓算法未被删除或改成普通圆角。
- **验证边界：**这是针对缓存锁争用与容量不足的进一步性能修复，尚无本次提交对应的设备帧时间/录屏验证，不能宣称全界面卡顿已彻底解决；必须检查新的 Actions 构建，并在设备上继续验证选择 Wavy、形状过渡动画和页面滚动。
- 界面缩放滑条：已检查当前分支的 `SegmentedSliderItem.kt`，它是统一 Material Slider/Thumb/Track 组件；但现有 `ScaleDialog.kt` 是 Miuix 数字输入对话框，不能据此把它误认成用户反馈的界面缩放滑条。当前还需定位实际承载 `pageScale` 修改入口的控件，确认其当前实现后再接入共享 Material Slider，避免在错误页面新增或替换控件。该项尚未完成，不记录为已修复。
- 下一步：定位实际调用 `SettingsViewModel.setPageScale` 的 UI 控件，把界面缩放轨道和滑块改为共用 `SegmentedSliderItem` / `VoyageSliderThumb` / `VoyageSliderTrack` 组件；随后检查本轮 Actions 的真实构建结果。不得使用旧 APK 作为最新产物，也不得合并 `main`。


## 2026-10-27 用户进一步明确 Switch 内滑块间距与 Slider 外层底色结构

- 所有修改仅提交到 `test/voyage-theme-1to1-20261026`，没有修改或合并 `main`。
- Switch 提交 `93c8693967620fa1762eddb0b138389163a8ca1c`（`ui: inset switch thumb and fill 90 percent of track height`）：
  - 内部 thumb 的可见高度改为轨道高度的 90%，而不是此前的 72%–82%，使滑块上下更饱满。
  - 开关两端各增加 2dp 水平内缩，关闭/开启位置都不再让滑块胶囊边缘贴死外轨道边缘；thumbContent 插槽同步使用新的行程。
  - 保持横向约 65% 轨道宽度的设计，仍由共享 Voyage Shape 样式控制形状。
- Slider 提交 `c4a17d2996933822c7ef07da8203444372a2fcc5`（`ui: wrap slider rail around inset thumb like switch`），并由 `4eed5f96e1d312e70a390af11e32fcac06315347` 修正内层 Shape 的创建时机：
  - 原先滑条底色是 4–8dp 的细轨道，滑块叠在细线之上；这与用户描述的“底色包裹整个滑轨、滑块在底色里面，像开关”不符。
  - 现在外层中性底色是 24dp 高的完整胶囊轨道，使用 `surfaceContainerHighest` 并跟随全局 Voyage 形状；滑块保留 20dp Material 测量槽，但可见填充缩至 16dp，在外层底色中留出均匀间距。
  - 活动进度绘制为外轨道内部向内缩 4dp 的内层轨道，跟随主题活动色与共享 Shape 样式；刻度和描边也移入内层轨道。阴影设置仍可控制外轨道和 thumb 的阴影。
  - 额外修正：内层 Voyage Shape 在 Canvas 绘制开始前预先创建，避免在非 Composable 绘制回调内调用 Composable Shape 工厂。
- 验证边界：源码改动已推送，尚未确认本批最新 HEAD 的 Actions 编译终态，也没有设备录屏确认。接下来必须检查构建日志；如有编译错误继续修复，并在设备上确认开关两端留白、90% 高度及 Slider 外底色包裹效果。不可宣称设备验收完成，也不可用旧 APK 作为本批最新交付物。

## 2026-10-27 用户反馈：列表选项卡间距收紧，凹弧与缺口形状默认深度减半

- 源码提交：`5e1e7bd1c202cd40a57bbb66b7f62a83e7ccfb29`（`ui: tighten list card spacing and soften concave/notch shapes`）。
- 修改文件：`app/src/main/java/com/alohaoneui/app/ui/component/material/SegmentedList.kt`。
- 列表选项卡/分组条目之间的默认间距由 `6.dp` 调整为 `4.dp`，减少条目间的空隙，其他内部内容间距不变。
- Voyage 形状样式 5（凹弧）与样式 6（缺口）的有效几何深度减半：在绘制轮廓时将四角相关尺寸乘以 `0.5f`。因此凹弧凹入/外凸幅度和缺口的切入深度都会相应缩小；全局 Corner Scale 仍会继续作用于新尺寸，其他形状样式不受影响。
- 本次改动只提交至 `test/voyage-theme-1to1-20261026`，没有修改或合并 `main`。
- 验证边界：源码已提交，需以本次提交后的最新 HEAD 检查 GitHub Actions 构建结果，并在设备上确认列表间距、凹弧与缺口形状在不同 Corner Scale 下的实际观感；未完成这些检查前不宣称设备验收通过，也不使用旧 APK 作为本轮交付物。

## 2026-10-27 用户反馈：四种滑条需分别呈现对应样式，包裹轨道底色加宽并使用次色/第三色容器色

- 源码提交：`e69a25f80c88e869f4ddbba29a8906462f0b6789`（`ui: distinguish four slider families and widen colored rail`）。
- 修改文件：`app/src/main/java/com/alohaoneui/app/ui/component/material/SegmentedSliderItem.kt`。
- 纠正之前所有样式共用同一种轨道外观的问题：四个可选样式（Material You、Fancy、Material、Hyper OS）现在分别配置可见滑块尺寸、内层轨道留白及配色；Material You 使用标准 20dp 滑块，Fancy 使用 22dp 滑块，Material 使用 18dp 紧凑滑块，Hyper OS 使用 20×24dp 竖向滑块。各样式对应的轨道内缩分别为 4dp、3dp、5dp、4dp。
- 包裹滑轨的外层轨道高度从 24dp 增至 32dp，让底色上下更宽、更明显。外层底色不再使用中性色 `surfaceContainerHighest`，改为随主题动态变化的 `secondaryContainer` 或 `tertiaryContainer`；滑块/已填充进度仍使用对应的 primary、secondary 或 tertiary 色，避免外层底色与滑块同色。
- 继续使用共享 Voyage 形状工厂生成轨道与滑块形状，使全局形状样式及圆角缩放设置仍可联动影响组件。滑条交互、范围与步进仍交给 Material `SliderState` 处理。
- 参考边界：本轮检索未找到可直接查看的那段历史录屏附件，因此没有声称已逐帧对照视频；本次依据用户在当前消息中明确指出的四种样式应各自不同、轨道底色用次色/第三色且上下更宽来修正。仍需核对 Voyage 原版四种样式各自的完整绘制路径，并在设备上对照录屏检查视觉细节。
- 只修改测试分支 `test/voyage-theme-1to1-20261026`，未修改或合并 `main`。本提交的 CI 构建与设备验收待确认；不得把旧 APK 当作本轮交付版本。


## 2026-10-27 — 四类 Slider 独立轨道绘制分支

- 产品代码提交：`63d04d464856b79a245a6eb918b47cc75d1cc069`，文件 `SegmentedSliderItem.kt`。
- 不再让 Material You、Fancy、Material 2、HyperOS 四种滑条只共用同一种轨道几何。现在每个样式拥有独立绘制分支：
  - Material You：32dp 外层容器内的完整高度圆角轨道；
  - Fancy：独立的居中较窄通道；
  - Material 2：经典 4dp 细轨道，保留外层次色/第三色容器；
  - HyperOS：较厚的独立胶囊通道。
- 四种样式的外层轨道仍由次色/第三色容器色承担，拇指与进度填充继续使用强调色，避免外层底色与滑块同色。外层维持 32dp 高度，使底色上下留白不再过窄；全局 Voyage Shape/Corner Scale 继续参与轨道外轮廓和各通道轮廓。
- 为不丢失离散步进反馈，非 Material 2 分支增加了按各自通道位置绘制的刻度；Material 2 刻度仍沿经典细轨道绘制。代码提交：`bb31cdb04a747df010c1a5b930c1359479e9b154`。
- 构建检查发现此前共享组件增加 `innerInset` 后，另一个调用点 `AlohaOneUISlider.kt` 未传入该参数，导致 Run `40585120152` 编译失败（`No value passed for parameter 'innerInset'`）。已补上传入 `materialAppearance.innerInset`，修复提交：`48cffbb5e11011af08b2c831a48339a2a75cdda7`。新代码提交后仍需重新确认 CI 编译结果。
- 样式来源核对参考：Android 官方 Compose Material 3 Slider 组件文档与 Material Components Slider anatomy 文档，分别说明轨道、拇指、刻度和 active/inactive track 的组成；这些通用资料仅用于理解控件结构，不能替代 Voyage APK 原始方法与用户录屏的最终对照。
- **准确性边界：**四种轨道现已在代码结构上真正分开，但当前几何比例仍属于可独立校正的实现阶段；尚未声称与 Voyage 原版像素级 1:1，也未完成真机视觉验收。下一步继续从 Voyage DEX/调用链追踪四种轨道的原始绘制分支，检查颜色映射与全局形状/圆角联动，再以最新成功构建验证。
- 分支边界不变：只更新 `test/voyage-theme-1to1-20261026`，未修改或合并 `main`。


## 2026-10-27 — 统一界面缩放滑块入口

- 产品提交：`f8f6b9eafec93b717ee7beb69e3d3a6c5dd8e704`。
- 将 `colorpalette/ColorPaletteScreenMaterial.kt` 中仍直接调用 Material3 默认 `Slider` 的“界面缩放”控件改为 `AlohaOneUISlider`，让它与其他 Material 设置滑条共用 Voyage 四种轨道样式选择、共享轨道/拇指组件及当前数值保存回调。
- 该入口原先直接调用系统默认 Slider，主题中选择 Material You/Fancy/Material 2/HyperOS 时不会跟随自定义轨道。现在已接入共享入口；尚需确认编译、拖动/保存行为和真机视觉表现。
- 当前工作仍只在 `test/voyage-theme-1to1-20261026`。CI 应以本节产品提交及后续维护记录提交对应的最新 HEAD 为准；旧的失败构建不能作为新代码验收。


## 2026-10-27 — 按上游四类 Slider 源码校正轨道几何

- 重要源代码定位：通过公开的 `T8RIN/ImageToolbox` 上游仓库找到与 Voyage 四个滑条名称一致的实现文件：
  - `M3Slider.kt`（Material You）：使用 Material 3 标准 Slider，并在外部增加容器与水平/垂直 padding；
  - `FancySlider.kt`：`FancyTrack` 高度为 `38.dp`，拇指 `26.dp`，随进度旋转 `1080f * fraction`；
  - `M2Slider.kt`：自定义轨道高度为 `4.dp`，容器水平 padding 为 `12.dp`；
  - `HyperOSSlider.kt`：轨道高度为 `30.dp`，`StrokeCap.Butt`，thumb composable 为空（不显示拇指）。
- 依据上述真实实现，修正先前根据视觉反馈临时设定的几何参数。产品提交 `e3a5b4d20029c865f3e2eeeee563a64432e53e04` 更新了各类型轨道高度、拇指尺寸及独立容器颜色；提交 `a0a25dbb17b0187415aed54ee0231468be2c9992` 按四类各自的轨道绘制方式重新实现；提交 `906d018e184cfc288503efc71c20d84856306a73` 将这些容器和拇指逻辑接入另一个共用入口 `AlohaOneUISlider.kt`。
- 当前映射：Material You 4dp 标准圆端轨道、外层容器与 12dp/6dp 内边距；Fancy 38dp 胶囊轨道、26dp 拇指与旋转动画、外层 6dp 水平内边距；Material 2 4dp 圆端细轨道、外层 12dp 水平内边距；HyperOS 30dp 粗轨道、方端笔帽、不显示拇指。四种轨道不再共用同一套轨道轮廓。
- 外层容器色与 active track/thumb 色分离，容器从 `secondaryContainer` / `tertiaryContainer` 色系取值，进度和拇指使用主题强调色。全局 Voyage 形状工厂继续用于轨道/容器轮廓，以保留用户要求的形状和圆角联动。
- 上游文件可从 `https://github.com/T8RIN/ImageToolbox/tree/master/core/ui/src/main/kotlin/com/t8rin/imagetoolbox/core/ui/widget/sliders` 核查。它提供了四类同名 Slider 的具体尺寸/绘制逻辑；最终仍需对照 Voyage 当前版本 DEX/录屏确认是否存在版本分叉，不能仅因名称相同就声称像素级验收完成。
- 最新源码已提交到 `test/voyage-theme-1to1-20261026`，没有触碰 `main`。需检查后续最新 HEAD 的 Actions 编译结果；如有失败继续修复，不以旧 APK 交付。


## 2026-10-27 — 轨道容器描边联动与编译前检查

- 产品提交 `601d006aa47dfd4015b4e412f4332ef6b4242816`：将 `voyageSliderContainerModifier` 标记为 `@Composable`，因为它需要调用读取全局形状/圆角 CompositionLocal 的 `voyageCornerShape`。
- 产品提交 `7be4fab4218ccb3e71b43375e5b7fc7352345753` 与 `2fadf1b08ea95d489baf92bf81b6099f81e8890c`：把全局描边宽度与 outlineVariant 颜色应用到 Material You/Fancy/Material 2 的外层容器；HyperOS 则在 30dp 轨道容器上应用描边，避免描边错误地只画在内部进度线上。
- 已将界面缩放滑块入口接入共享滑条组件，因此 Material 主题中的滑条样式选择会覆盖该入口；Miuix 现有分支保留独立绘制行为。
- 构建状态：之前 Run `40585120152` 已明确因 `AlohaOneUISlider.kt` 漏传 `innerInset` 编译失败，后续已补齐后又按上游源代码移除该临时参数。当前最新源码提交之后的 CI 仍需等待并核实，不能用较早 HEAD 的构建结果代替。


## 2026-10-27 — 开关比例、波浪形状性能与语言选择修复

- 分支：`test/voyage-theme-1to1-20261026`；基于已通过构建的 `a716a11cdeeec59a0756f2cb9d7b9040c7d98c4b` 继续修改，未改动 `main`。
- 开关尺寸：各样式整体宽度略增、高度略减；滑块高度调整为轨道高度的约 85%。关闭状态轨道使用较暗的 `onSurface` 半透明色，滑块在关闭状态仍保留主题主色。
- 波浪形状性能：保留 Voyage 波浪路径轮廓与共享 LRU 缓存；将波浪半径缓存量化由 0.5dp 改为 1dp，降低形状动画时缓存键变化与路径重建；将总路径采样预算从 360 降至 180，并提高最小采样间距，以减少所有组件同时使用波浪形状时的 CPU/分配压力。需要在设备上确认视觉与流畅度。
- 语言选择：Material 与 Miuix 语言列表此前使用 `zh-Hans/zh-Hant`，而 ViewModel 持久化白名单使用 `zh-CN/zh-TW`，导致选择态与保存值不匹配；统一列表值为 `zh-CN/zh-TW`，由 `AppLocaleContext` 映射到 Android LocaleManager 使用的规范语言标签。
- 黑屏闪烁：保留 Android 13+ 由 LocaleManager 触发的系统语言配置切换路径，并避免额外手动 recreate；本次同时修复 locale tag 不一致。仍需真实设备复测语言切换过程，不能仅凭编译宣称闪屏已完全消失。
- 验证：等待本轮新提交的 GitHub Actions 编译与 APK artifact；不得以旧构建代替本轮产物。


## 2026-10-27 — 开关外形、波浪形状性能、语言反馈与滑条细节第二轮修复

- 本轮继续只提交到 `test/voyage-theme-1to1-20261026`，没有修改或合并 `main`。基线为已成功构建的 `a716a11cdeeec59a0756f2cb9d7b9040c7d98c4b`。
- Switch：`ExpressiveSwitch.kt` 将各样式轨道高度略微收窄、宽度略微增加；内部胶囊高度固定为轨道高度的 85%，横向占比由 65% 增至 70%，两端内缩改为 1.5dp。关闭状态轨道使用深色 `#45464A`，thumb 继续使用主题主色。提交：`760245d81172a4e10209f1e83c40e03190de34b5` 与 `08f8082aeba3ccad99e3a57e5e4f511133673512`。
- Slider 不再受全局组件描边影响：移除外层容器与 HyperOS 轨道上的描边绘制/调用；描边宽度设置仍可用于其他组件。底色阴影改由既有 `LocalMaterialShadowSliders` 控制：Material You、Fancy、Material 2 的外层容器使用 2dp 阴影，HyperOS 的粗轨道也使用相同偏好控制的 2dp 阴影。相关提交：`65dcac621e35652eee46d7320cb23427ea8e3248`、`6300d7eb21850a93bf3f591dfb95c3af60bab14e`。
- Material You 滑块：共享 `VoyageSliderThumb` 在 style 0 分支改为调用 Compose Material 3 的 `SliderDefaults.Thumb`，使用官方滑块实现；其他三种样式仍保留各自的自定义绘制。共享入口和设置页入口分别按样式索引使用 `key(style)` / `key(materialSliderStyle)` 包裹 Slider 子树，以便切换样式时立即重建对应轨道与 thumb，而不是等待用户拖动才看见变化。相关提交：`6300d7eb21850a93bf3f591dfb95c3af60bab14e`、`5e8987736d556763361e0b4256b8e0320900ffb3`、`e92579f741cf3eda9c693ea0164ee84995640b7c`。
- 波浪形状仍存在全局卡顿反馈，因此进一步降低波浪路径的采样/分配开销：每波弧段采样预算从 8 降至 6，总路径采样预算从 180 降至 96，最小采样间距从 1.5px 提高到 2.5px；共享 LRU 缓存仍保留。提交：`0d024e293cf152527e8795d5fe4bbd5f399155e2`。这项优化需要真机确认流畅度和边缘波纹是否仍符合原版观感。
- 语言选择：Material 设置语言列表每个当前选项现在显示主题色勾选图标，补强选中态。提交：`79f2d42034d6752bf804e059a81cb0277bb590f4`。
- 语言黑屏：设置语言动作现在先比较当前保存值与目标值；重复选择当前语言时不再重新写入 LocaleManager 或触发旧版 Activity recreate，避免无意义的重建闪屏。真正切换语言仍由 Android 13+ LocaleManager 管理配置切换；低版本只执行一次必要的 Activity recreate。提交：`2a06b73f548a3ac9c992104fb03b50ef1fb1eb94`。该修复不能代替真机确认，若切换到不同语言仍黑屏，需要继续检查 Activity 窗口背景与系统语言配置重建过程。
- 验证边界：本节所有改动均为已推送源码，当前仍待最新 HEAD 的 Actions 编译终态、APK artifact 和设备视觉/交互复测；未宣称本轮已通过新构建或已完成真机验收。不得用旧 APK 作为本轮交付物。


## 2026-10-27 — Run #1803 构建验证结果

- GitHub Actions Run #1803：`40591312502`，结果 `success`，对应产品源码提交 `4157133de953ef60498b1afc693c9d9f02ea7ae3`。Compile、签名 APK 构建、包名/签名验证及 Actions Artifact 保存步骤均成功。
- Artifact：`AlohaOneUI-Signed-Test-APK`，Artifact ID `12773174792`；下载 ZIP 内的 APK 文件为 `AlohaOneUI_1.0.261025_BUILD_1803.apk`，大小 28,788,313 bytes（约 27.45 MiB）。
- APK package：`com.alohaoneui.app`；versionName `1.0.261025`，versionCode `16`；SDK 31–36，编译 SDK 37。
- APK SHA-256：`b61956d574b8bb1f9a669129fcb8d87c9320b3196e6ae59725f96101da8cb900`。
- 签名验证：APK Signature Scheme v2 验证通过；证书 SHA-256 `93bf61a6493c6b336383832eb41301bd8fe061f43566e7e384e3fdd9fc1c26a1`，与该构建的签名检查文件一致。
- 本次是测试分支的 signed test APK；workflow 的 optimized release validation 与 GitHub Release publish 步骤被跳过，因此交付以 Actions Artifact / APK 文件为准，不宣称已发布正式 Release。
- 设备验收仍未完成：构建成功只能证明源码编译、APK 签名与包信息验证通过，不能代替真机检查波浪形状流畅度、语言切换闪屏、开关/滑条观感及切换即时生效。


## 2026-10-27 — 语言切换黑屏的窗口背景补强

- 继续针对不同语言切换时的黑屏闪帧排查：发现主 Activity 的 `Theme.AlohaOneUI` 在日间/夜间主题里都没有显式设置 `android:windowBackground`，系统在 LocaleManager 导致 Activity 配置重建时可能短暂显示平台默认黑色窗口背景。
- 在 `app/src/main/res/values/themes.xml` 与 `app/src/main/res/values-night/themes.xml` 的 `Theme.AlohaOneUI` 中增加 `android:windowBackground=?android:attr/colorBackground`，让 Activity 重建期间的窗口底色跟随平台日间/夜间主题背景，避免默认黑色空窗。提交：`375f4361b512ff60e366c5ee763e012b18a93e5e`、`7b64d39b742ce7b7a1131141fae31080eac943d1`。
- 该设置不改 Startup Activity 的透明启动窗口，也不替代系统 LocaleManager 的配置切换流程。必须以最新 HEAD 构建成功后在真机上切换不同语言验证；源码推断不能代替设备确认。
- 上一节 Run #1803 的 APK 证明此前源码版本可编译、签名和包信息验证成功；但该 APK 不包含本节窗口背景改动，因此不能作为最新修复版本交付。等待最新 HEAD 的 Actions 构建与 Artifact。


## 2026-10-27 — Run #1807：包含窗口背景修复的最新构建

- 最新源码 HEAD：`452b12a43bae86e7da125a0f77d71e81aec1a69c`（本维护记录更新前的 HEAD）；GitHub Actions Run #1807 / ID `40592035710` 成功。
- 本次构建包含日间与夜间 `Theme.AlohaOneUI` 的 `android:windowBackground` 修复，以及本节之前记录的 Switch、Slider、波浪路径采样、语言选择与语言重建优化。Compile、owner-signed APK 构建、包信息/签名验证和 Artifact 保存均成功。
- 最新 Artifact ID：`12775365629`，名称 `AlohaOneUI-Signed-Test-APK`。APK：`AlohaOneUI_1.0.261025_BUILD_1807.apk`，大小 28,788,337 bytes。
- APK SHA-256：`3ebe5feda548e711fa7c1946cb93f8444a76088e40b4f5e91662b625c1f50292`。
- 包名 `com.alohaoneui.app`；versionName `1.0.261025`；versionCode `16`；签名证书 SHA-256 `93bf61a6493c6b336383832eb41301bd8fe061f43566e7e384e3fdd9fc1c26a1`。
- 直接 APK 文件已从 Artifact ZIP 中提取，并在工作环境重新计算 SHA-256，与 Artifact 内的 `SHA256SUMS` 完全一致。该构建仍是测试分支 signed test APK，workflow 未发布 GitHub Release。
- 真机验收仍待完成。尤其要在 Android 13+ 设备上切换到不同语言观察是否仍出现闪帧，并实际检查波浪样式流畅度与 Slider 样式即时切换；构建成功不等于这些视觉/交互问题已在设备上彻底解决。


## 2026-10-27 — Slider 阴影层级修正与官方控件对照继续

- 修复提交：`a1c9550eb7e7111d1c5b388d656ad658b9c7bb4e`，仅修改 `SegmentedSliderItem.kt`，提交到 `test/voyage-theme-1to1-20261026`，未修改 `main`。
- 修正 HyperOS 粗轨道的 Modifier 绘制顺序：阴影 Modifier 现在位于背景色 Modifier 外层，先作为底层投影绘制，再由轨道容器底色绘制覆盖；此前顺序可能令阴影与背景色的前后关系不符合预期。
- 已参考 Google 官方 Material 3 Slider API 文档，确认官方 Slider 的轨道与拇指分别由 `SliderDefaults.Track` / `SliderDefaults.Thumb` 提供，并使用主题色角色配置。后续将继续把 Material You 轨道实现与官方 Compose Material 3 行为逐项对齐，同时单独核对 Material 2 的轨道、拇指、刻度与状态色，避免两者只在名称或尺寸上有区别。
- 开关后续按每个样式的名称与来源实现独立几何、状态与动画，颜色仍取 Aloha OneUI 自身主题色。波浪形状则继续以 Voyage 原始路径与录屏观感为准，在不破坏波形的前提下优化性能。
- 验证状态：当前只确认阴影层级修改已提交；该提交之后的 CI 构建及真机显示尚未验证。不得将本次记录当作整个开关/滑条/波浪校正已完成，也不得以旧 APK 冒充最新产物。


## 2026-10-27 — Material You 开关滑块几何与 Slider 前景阴影第二轮

- 开关提交：`44dd104f4aaac6eec65876a8b0f74745c71135d9`，修改 `ExpressiveSwitch.kt`。
  - Material You 样式（style 0）现在使用 52×32dp 的外轨道，并按状态将圆形 thumb 设置为未选中 16dp、选中 24dp、按压 28dp；不再错误地把该样式的滑块也拉成长胶囊。
  - 其余样式按各自分支使用不同的可见 thumb 宽度比例，继续保留原有样式差异和 Aloha OneUI 主题色。该比例是本轮代码校正，不等于已证实的所有 Voyage 原版像素值，仍需逐个样式对照录屏/原始分支。
- Slider 阴影提交：`20358bc866dd4a97c5833c44571f2b7fa1173f36`，修改 `SegmentedSliderItem.kt`。
  - 移除拇指自身的第二层 elevation shadow，避免 thumb 的阴影作为前景叠在轨道容器底色上；阴影由轨道/外层容器负责，目标是让投影处于底色表面之后。
  - HyperOS 轨道另由 `a1c9550eb7e7111d1c5b388d656ad658b9c7bb4e` 调整 Modifier 顺序，使阴影在容器底色前方的错误层级得到修正。
- 官方依据：Google Material 3 Slider API 公开了官方 `SliderDefaults.Thumb` 与 `SliderDefaults.Track`，Material 2 的 Compose Slider API 亦提供其自身默认轨道/拇指实现。现有 Material You thumb 已使用官方 M3 thumb；Material You track 仍需继续校正，不能将本轮称为完整官方 1:1。尝试直接接入官方 Track 的文件更新未成功提交，因此没有把该项记录为完成。
- 验证状态：上述提交已进入测试分支，但尚未确认这些新提交的 Actions 构建和真机表现。重点验收 Material You 开关的圆形 thumb 状态尺寸、其余开关样式差异，以及 Slider 阴影是否完全位于背景底色下方；没有真机证据前不宣称验收完成。


## 2026-10-27 — Slider 重复小背景层移除

- 按用户明确反馈，修正提交 `19aa08fbfc20ccc153c9fec5f7d6c69d2a95d5ff`，仅修改 `SegmentedSliderItem.kt`，推送到 `test/voyage-theme-1to1-20261026`；未修改或合并 `main`。
- Material You、Fancy、Material 2 与 HyperOS 滑条不再绘制未选中部分的第二层小轨道底色；保留外层大背景表面，并让进度色直接显示在该表面上。HyperOS 仍保留其 30dp 宽轨道自身作为大背景容器，不再叠加额外的 inactiveColor 轨道。
- 阴影层级维持在大背景容器之后/之下：外层容器 Modifier 先绘制阴影，再绘制主题背景；thumb 不再附加独立前景阴影。此项改动目标是去掉用户指出的小背景色，并让阴影由大背景承接。
- 验证边界：源码已提交，仍需等待最新 HEAD 的 GitHub Actions 编译和 APK artifact，并在设备上检查各样式。因未进行真机复测，暂不宣称视觉验收完成。


## 2026-10-27 — 统一缩小全局波浪形状（Slider 与 Switch 共用）

- 按用户反馈，形状样式中的波浪轮廓当前过大，且开关似乎仍显示旧波浪。本轮在共享形状工厂 VoyagePathShape（SegmentedList.kt）中修正波浪分支参数，而不是只改滑条或开关某一处。
- 波浪目标长度从 12dp × density × scale 调整为 6dp × density × scale；波幅从 0.9dp × density × scale 调整为 0.45dp × density × scale。目标是让波峰/波谷更细小，同时保持全局形状样式与圆角缩放的联动逻辑。
- ExpressiveSwitch.kt 的轨道、thumb 与 SegmentedSliderItem.kt 的轨道/拇指都经 voyageCornerShape(...) 使用共享形状工厂，因此本次修改应同时作用于开关与滑条，避免两处保留不同版本的波浪参数。源码检查显示 Switch 并没有另一套独立的波浪绘制算法；若真机仍显示旧波形，需继续排查形状选择索引、组件重组与实际安装包版本。
- 产品源码提交：29dc161ccf8f40e7b05f12599c0d5a69bd35212d，只提交至 test/voyage-theme-1to1-20261026，未修改或合并 main。
- 验证边界：参数修改已推送；当前尚未完成最新 HEAD 的 CI 构建与真机视觉验收。需确认新 APK 中波浪确实变细、Switch 与 Slider 同步更新且其他形状样式未受影响；不得把旧 APK 当成本轮产物。


## 2026-10-27 — Material You / Fancy 滑条轨道与配色校正

- 按用户反馈，在共享滑条组件和通用滑条入口同步调整：Material You 轨道高度从 4dp 增至 16dp（4 倍），进度轨道使用主题色系统的 secondary；thumb 保持 primary。通用 `AlohaOneUISlider` 和设置列表 `SegmentedSliderItem` 都调用同一共享外观参数。
- Fancy 滑块旋转动画方向由 `1080f * valueFraction` 改为 `-1080f * valueFraction`，反转滚动方向。
- Fancy 轨道恢复完整胶囊底轨，使用固定浅灰 `#D9D9D9` 并位于滑块后方，厚度仍为 Fancy 的 38dp，使轨道视觉上包裹滑块。已加数值的进度段改用主题 secondary；Fancy thumb 和刻度点改用主题 primary，不再让 thumb 与进度段同色。
- `voyageSliderAppearance` 增加独立 `thumbColor`，避免轨道进度与滑块被同一个颜色参数绑在一起；其他样式的既有色彩映射保持原逻辑（HyperOS 进度使用 tertiary，Material 2 使用 primary）。
- 相关源码提交：`89e7f3c60e8727c6a46fc7437937b4fa2b5df80f`、`76f2970824d675b626ad9e0f7d8731b014a97a0d`、`90e964db331083f94b80a9d1c9850a0c50118066`、`ca7940e22b501206732ea88a9765f2f37abf6e70`、`cb54b8abbe87445e2d532788038e509128c5eb71`、`a7645a2decdb7e2ea5d6a926eff2dd8ed86116f6`。所有更改仅在 `test/voyage-theme-1to1-20261026`，没有修改或合并 `main`。
- 验证边界：上述为源码层修改；仍需检查最新 HEAD 的 CI 编译和 APK artifact，并在设备上确认 Material You 轨道实际显示 16dp、Fancy 旋转方向正常、浅灰底轨完整包裹滑块、主色/次色区分正确。构建与真机验收未完成前，不宣称视觉验收通过。


## 2026-10-27 — 修复“开关样式 / 滑条样式”选择后不即时生效

- 用户反馈：在主题设置中更改“开关样式”或“滑条样式”后，预览控件不会立即切换，必须再手动点一次开关或滑条才显示新样式。
- 根因已在 MainActivityViewModel.kt 中确认：SettingsRepositoryImpl 保存到 SharedPreferences 的键名分别是 material_switch_style 和 material_slider_style，ThemeController.getAppSettings() 也会读取这两个值；但 MainActivityViewModel.observedKeys 没有包含这两个键。因此设置写入后，SharedPreferences.OnSharedPreferenceChangeListener 不会刷新 MainActivityUiState，根主题传递给 LocalMaterialSwitchStyle / LocalMaterialSliderStyle 的值仍是旧值。随后手动点击控件触发内部重组，才意外显示新样式。
- 已修复：将 material_switch_style 与 material_slider_style 加入 observedKeys。设置保存后，监听器会立即重新读取 AppSettings 并更新 StateFlow，触发主题 CompositionLocal 与控件重组；无需再手动点控件。未改动样式绘制参数或设置保存格式。
- 产品源码提交：993c7e85073aa09b714152669ec64f21b64edd4c，文件：app/src/main/java/com/alohaoneui/app/ui/viewmodel/MainActivityViewModel.kt。
- 所有修改仅在 test/voyage-theme-1to1-20261026；没有修改或合并 main。
- 验证边界：已确认存储键名、状态监听键列表和主题状态读取链相互对应，并已推送修复。仍需检查此 HEAD 的 GitHub Actions 编译结果；构建通过后还要在设备上分别切换开关样式与滑条样式，确认预览即时变化且不会影响当前控件值。此时尚不能宣称 CI 或真机验收已通过。


## 2026-10-27 — 语言切换黑屏闪帧与选中项整行底色二次修复

- 用户复测反馈：语言切换仍会短暂黑屏，且当前语言选中态的颜色没有覆盖完整选项行。上一轮虽已给日间/夜间 Theme.AlohaOneUI 设置 windowBackground，但真机反馈说明仍不足以消除黑色预览帧，因此继续处理，不将旧修复视为已验收。
- 语言选中态：SettingsMaterial.kt 的语言选项显式传入 alohaOneUISegmentedColors().copy(selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer)，使选中底色明确使用主题 secondaryContainer，并由整行 SegmentedListItem 的选中容器绘制，而不只依赖勾选图标体现选中状态。修改仅作用于语言选项，避免改变其他设置列表的选中配色。
- 黑屏闪帧：在 values/themes.xml 与 values-night/themes.xml 的主 Theme.AlohaOneUI 中，在现有 windowBackground 跟随系统日/夜背景的基础上增加 android:windowDisablePreview=true，尝试阻止 LocaleManager 触发 Activity 配置重建时显示旧窗口预览黑帧；Startup Activity 的透明主题不变。语言切换仍由 Android 13+ LocaleManager 负责，不额外调用 Activity.recreate()。
- 提交：语言选中整行底色 37fa9b0f5446a8d146ee45ba3c212c830dfc14d3；日间窗口主题 940582993711299b25da9732e16f50d12569fcd1；夜间窗口主题 f4a4a2a777a80d75a57cb826fd40d4312792105a。
- 所有修改仅在 test/voyage-theme-1to1-20261026，没有修改或合并 main。
- 验证边界：源码更新已推送，尚未完成最新 HEAD 的 CI 编译与真机复测。需确认 Kotlin 的 ListItemColors.copy(selectedContainerColor) 可编译，并在设备上切换不同语言确认黑屏闪帧消失、选中底色从行首覆盖到行尾且圆角符合当前形状设置。若黑帧仍出现，应继续基于设备录像定位系统 LocaleManager 重建时序，不应仅凭主题属性推断问题已解决。


## 2026-10-27 — Slider 阴影位置与 Material 3 轨道二次修正

- 用户复测指出：滑条阴影仍未落在轨道下方、进度色不符合次色/第三色要求，Material You 轨道仍不像 Material 3。已查看用户上传的 `Screen_Recording_20261027_171519_Voyage Toolbox.mp4`；录屏中 Material You 分支仍表现为粗大的彩色胶囊式进度段，并带主题背景胶囊，不是 M3 的细轨道结构。
- 产品源码提交 `283c8f272f7e194a20cce3418a69fd348a4ea04f`（`SegmentedSliderItem.kt`）：
  1. Material You 轨道由 16dp 改回 Material 3 标准 4dp 轨道，取消该分支额外的主题色胶囊背景与内边距。
  2. 轨道按 M3 几何绘制完整 inactive rail 与 active rail；轨道两端按 10dp thumb 半径内缩，进度段使用主题 tertiary，未选中轨道使用 `surfaceContainerHighest`。
  3. Material You 保留官方 `SliderDefaults.Thumb`，避免用自绘大胶囊取代 M3 圆形拇指。
  4. 将可选阴影移到细轨道绘制层后方，不再把 Material You 的投影只放在外层大背景胶囊上；其他样式保持原有几何分支。
- 通用滑条入口产品源码提交 `b518180a2a4b535c35d88f0b2a3538b4adf5dd50`（`AlohaOneUISlider.kt`）：此前虽然调用共享轨道组件，但没有把 `LocalMaterialShadowSliders.current` 传给 `VoyageSliderTrack`，导致该入口的轨道阴影开关实际上没有生效；现已补上传递。
- 两项产品源码改动均只提交到 `test/voyage-theme-1to1-20261026`，未修改或合并 `main`。
- 验证边界：录屏确认了用户指出的外观差异；源码修改已提交。尚未确认这两个最新提交对应的最终 HEAD 构建结果，也未在真机安装新 APK 复测。接下来需核查 CI 编译与签名产物，并验证 M3 轨道厚度、进度第三色、圆形 thumb 及阴影是否确实落在轨道下方。不能将源码提交等同于视觉验收完成。


### Material 3 离散刻度对比色补正

- 产品源码提交 `33bb43a92b39544e443e77c7a6a71660e2138e76`：Material You 的已通过刻度点使用 `onTertiary`，确保刻度点与第三色进度轨道有正确对比；未通过刻度继续使用低透明度的 `onSurfaceVariant`。
- 此提交只在测试分支；CI 需以最新 HEAD 重新构建。真机验收仍待完成。


## 2026-10-27 — 按 Voyage Toolbox 录屏逐帧重做四种滑条

- 用户明确说明 `Screen_Recording_20261027_171519_Voyage Toolbox.mp4` 是 Voyage 正确效果的参考录像，不是问题演示。本轮按录像逐帧查看四种滑条的切换与拖动过程，依据画面分别重做 Material You、灵动/Fancy、Material 2、HyperOS 四个分支；不再把 Material You 直接等同于系统默认的 4dp Material 3 细线滑条。
- Material You（style 0）：录屏中是带圆角的较厚胶囊轨道，已选进度与未选轨道颜色不同，轨道内有刻度点，当前值由一条窄的竖向标记指示；本轮轨道高度调整为 16dp，进度使用主题 secondary，未选部分使用 tertiaryContainer，标记使用 primary，已选/未选刻度分别反白与主题 primary。轨道与外层容器的阴影都由背景层先绘制，之后才绘制轨道/进度，避免阴影浮在进度色上。
- 灵动/Fancy（style 1）：录屏显示固定浅灰色完整底轨、柔和薄荷色进度、主色不规则形状滑块及外侧薄荷色光环；轨道高度调整为 28dp，进度使用 secondaryContainer，滑块核心和已选刻度使用 primary，未选刻度为浅色。保留滑块随进度旋转的方向修正。
- Material 2（style 2）：录屏显示独立的细轨道、主色圆形滑块与主色刻度，外层保留圆角容器；轨道高度调整为 6dp，进度使用 tertiary，滑块使用 primary。
- HyperOS（style 3）：录屏显示较厚的圆角整轨、主题色进度和刻度，不单独显示滑块；轨道高度调整为 24dp，进度使用 secondary，未选轨道使用 tertiaryContainer。
- 四种样式共用的 `voyageSliderAppearance`、`VoyageSliderThumb`、`VoyageSliderTrack` 和容器 modifier 已同步修改，因此设置列表预览与通用 `AlohaOneUISlider` 入口会使用同一套四分支绘制。通用入口原有 `LocalMaterialShadowSliders` 传递保留。
- 产品源码提交：`5b279fd6ff1acc10bdc43fe07643037fcd5279c5`，只提交至 `test/voyage-theme-1to1-20261026`，没有修改或合并 `main`。
- 验证边界：本轮已按录屏重做绘制分支并推送源码，但仍需确认最新 HEAD 的 CI 编译与 APK artifact；编译成功后还需要在设备上对照四种样式、拖动位置、主题色切换和阴影层级。未完成这些验证前不宣称已达到 1:1。

- 逐帧复核后又补正 Material You 轨道：进度段和未选段现在在当前值标记两侧分别绘制，并留出 4dp 间隔，不再把进度色直接涂满整条底轨；提交 `f104d341a46981a8163185ca8d70d86707875296`。此补正仍只在测试分支，等待最终 HEAD 构建验证。


## 2026-10-27 — 滑条颜色联动修正（撤销固定色）

- 用户明确指出 Voyage 录像只作为外观/交互参考，不应把颜色或组件参数写死；Aloha OneUI 的所有控件颜色必须跟随当前动态主题联动。本轮据此修正四种滑条绘制：Fancy 底轨从固定 `Color(0xFFD9D9D9)` 改为 `MaterialTheme.colorScheme.surfaceContainerHighest`；进度改用主题 `secondary`；外侧光环使用 `secondaryContainer`；刻度对比色改用 `onSecondary` / `onSurface` 等主题角色，移除白色常量。其它样式继续从 `primary`、`secondary`、`tertiary`、`tertiaryContainer` 和 surface 角色取色。
- 代码提交：`be8fa8ea2b1669f6eff462ca6367d0529eea38a7`，只在 `test/voyage-theme-1to1-20261026`。
- 后续对照原则：视频用于理解形状、层级、轨道/滑块关系、动画和相对比例；颜色必须来自 AlohaOneUI 当前主题角色，尺寸/圆角必须继续检查是否应连接现有全局控件参数，不能因参考画面而把主题联动切断。
- 验证状态：等待此最新提交的 CI；尚未完成设备端主题切换联动测试。


## 2026-10-27 — 滑块滚动与小波浪轮廓修正

- 用户明确要求 Fancy 与 Material 2 滑块有滚动效果，圆形滑块应改为小波浪圆形，并继续修正全局波浪形状。
- `SegmentedSliderItem.kt`：Material 2 的滑块改为使用 `voyageCornerShape` 提供的当前全局形状轮廓（不再强制圆角裁切），并依据当前滑动值增加旋转，让小波浪轮廓在拖动时表现为滚动；滑块颜色使用传入的主题联动颜色。Fancy 继续沿进度旋转，形状与光环保持分层。
- `SegmentedList.kt`：针对全局 Wavy 形状，将目标波长从 6dp 调整为 8dp、振幅从 0.45dp 调整为 0.7dp，并保留尺寸缩放因子。目的是减少过密细波纹，让轮廓更平滑、起伏更可见；此改动会影响所有使用全局 Wavy 样式的组件，需要实际设备对照检查，若与参考效果不符继续校准。
- 参考 Google 官方 Compose Shapes 文档中的自定义 Shape/Path 与平滑轮廓实践；官方 Material 3 Slider API 支持自定义 thumb 和 track，但没有提供内置的“波浪滑块”样式，因此波浪轮廓仍由 Aloha OneUI 的共享 Shape 实现，颜色继续来自当前主题。
- 本轮只修改测试分支 `test/voyage-theme-1to1-20261026`，不改 `main`。构建与真机滚动/波浪轮廓验证仍待完成，不将源码提交视为视觉验收。


## 2026-10-27：开始按官方 Material 3 阴影/高度语义修正

- 官方依据：Google Android Developers 的 Compose 阴影文档明确区分基于高度的投影阴影（`Modifier.shadow()` / Material 3 `shadowElevation`）与可自定义投影；Material 3 主题文档及 M2→M3 迁移文档明确区分 `shadowElevation`（投射阴影）和 `tonalElevation`（表面色调叠加），两者不能互相替代。
- 本轮开始在测试分支 `test/voyage-theme-1to1-20261026` 修正这一语义混用，`main` 未修改。
- 产品提交 `c6cb6d87b8c5e1041599a759d20b0aa4914421a1`：在 `ExpressiveDialog.kt` 中，先把对话框的 `tonalElevation` 从“容器阴影”开关中分离；但提交中换行格式有误，随后立即由提交 `15aa28a6450cfa3eb49576410f1e10e3bb9c59f7` 修正源码格式。最终文件中标准 `AlertDialog` 保留调用方指定的 tonal elevation；自定义内容对话框的 Material 3 `Surface` 保留默认 tonal elevation，并由容器阴影开关控制 `shadowElevation`（6dp/0dp）。
- 这只是第一处已确认的语义错误修复，不代表全项目阴影审计完成。标准 `AlertDialog` API 当前封装未暴露独立的 `shadowElevation` 参数，因此其投影阴影仍需进一步改为可单独控制的 Material 3 Surface/对话框实现；不能把 tonal elevation 关闭当作关闭投影阴影。
- 接下来继续逐一审计页面容器、顶部栏、普通按钮、浮动按钮、Switch、Slider、菜单、卡片和所有子页面：检查每类开关是否控制正确组件，投影高度是否通过 Material 3 elevation API 表达，tonal elevation 是否保持独立，关闭阴影后是否仅移除投影而不错误改变表面颜色。还需核对 Miuix 分支与 Material 分支的适用范围。
- Google 官方参考：
  - https://developer.android.com/develop/ui/compose/graphics/draw/shadows
  - https://developer.android.com/develop/ui/compose/designsystems/material3
  - https://developer.android.com/develop/ui/compose/designsystems/material2-material3
- 当前提交尚未完成 Actions/编译验证；不可据此宣称构建成功或已有新 APK。必须继续检查最新 HEAD 的 CI，修复编译错误并在构建通过后验收 APK。


## 2026-10-27：Fancy 滑条轨道与滑块细节修正

- 在测试分支 `test/voyage-theme-1to1-20261026` 修改 `SegmentedSliderItem.kt`，没有触碰 `main`。
- Fancy 样式（style 1）取消外围容器背景、容器圆角和容器阴影，避免滑条外面再套一层框体。
- Fancy 轨道高度由 28dp 调整为 40dp（总高度增加 12dp），并通过左右延展的轨道绘制让轨道包住滑块光环；进度端点比滑块中心略向左 4dp，滑块核心也向左偏移 4dp，避免滑块贴住进度色右边缘。
- Fancy 进度色继续使用主题第二颜色（Material 3 `colorScheme.secondary`）；滑块外侧光环改为圆形，并使用第二颜色 24% alpha 半透明填充，内核继续按全局 Voyage 形状样式裁切与旋转。
- Fancy 轨道的未完成段与进度段都通过全局 `voyageCornerShape` 生成轮廓绘制，因此会跟随全局形状样式，而不是固定使用圆角矩形。
- 已提交：`0062c879597a71d9d9298d2dcf7e9b0f64c4058f`（`ui: refine Fancy slider rail and thumb geometry`）。
- 尚待 CI 编译验证和设备视觉复核；尤其需检查轨道端部延展、拖动命中区域与滑块视觉偏移是否符合录屏参考。未通过编译前不视为完成，也不发布 APK。


## 2026-10-27 — Fancy 滑条光环改为仅交互时显示

- 用户补充明确要求：Fancy 滑条的圆形半透明光环只在按压或拖动滑块时出现，静止状态不显示光环。
- 在 `SegmentedSliderItem.kt` 中通过同一个 `MutableInteractionSource` 读取 `collectIsPressedAsState()` 与 `collectIsDraggedAsState()`，并把 `isPressed || isDragged` 传给 Fancy thumb；光环仅在该状态为真时绘制，半透明第二主题色、圆形外形和尺寸保持不变。
- 产品源码提交：`6f23bf264b5b228cbbbb82e49317b28a9428689f`，仅写入 `test/voyage-theme-1to1-20261026`，没有修改或合并 `main`。
- 验证状态：源码提交完成，尚需核验该提交对应的 Actions 编译结果，并在设备上确认静止时光环消失、按压/拖动时出现、松开后消失。
