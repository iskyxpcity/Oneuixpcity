# Aloha OneUI 维护总记录 — 2026-10-26

> 规则：每个自然日只维护一份总记录，文件名只使用年月日（YYYYMMDD），不含时分秒。同一天的所有续写都追加到本文件；次日续写另建次日日期文件。后续不再创建带时间戳的碎片记录。

# Aloha OneUI 维护续写（2026-10-26 HST）

> 本文件是本轮追加记录索引；完整历史记录续写稿为 `AlohaOneUI_维护总记录_2610262025.md`（含全部既有历史和最新 K 节）。由于资料库上传接口当前返回 throttled，完整合并版与 ZIP 已在本地生成并校验，但尚未能重新入库；不得把入库说成成功。

## 当前工作基线
- 仓库：`iskyxpcity/Oneuixpcity`
- 分支：`test/voyage-theme-1to1-20261026`
- 不修改/合并 `main`
- 包名：`com.alohaoneui.app`
- 任务顺序：1) Voyage 八种 Switch；2) 四种 Slider；3) 50 个真实图标形状预设；4) Smooth G²。

## 本轮已记录源码提交
- Switch：`52b55164e46321e5e003545dfbaf219620e3ed24`
- Slider：`e7db96a58bcf5db770d50d770463b0c7c764dfcd`
- Slider API 修正：`580744956d0118f99b88e7f9cc87ea8f5601f5c0`
- 最新 Slider API 修正：`ba0a0e1c3a031cdea03bcde42ed4317ee781aef2`

## 最新构建状态
- Build #1604 / Run `40460369222`
- HEAD `ba0a0e1c3a031cdea03bcde42ed4317ee781aef2`
- GitHub Actions 已查询为 `completed / success`。
- Run：https://github.com/iskyxpcity/Oneuixpcity/actions/runs/40460369222
- CI 通过不代表已完成 Voyage 1:1 视觉验收，也不代表最终 APK 交付；仍须核实 jobs、Owner 签名、包身份、APK Artifact、APK SHA-256、V2 签名及 ZIP 完整性。

## 原版源码对照索引
- Switch：`gfj0.java`、`h7w0.m99521O(...)`、八个 marker classes。
- Slider：`kdj0.java`、`h7w0.m99521O(...)`、`gaf0/jaf0/iaf0/haf0.java` 与 `y8f0` 的两条绘制路径。
- Icon shapes：`k9t0.m108318Io`、`vqp.java`、`eig0.java`、`d8t0.m88393lO(...)`、`shq.java`、`ewm.m92946Oo()`、`qha0.java`；50 个实际预设，Random 为 sentinel，None index 为 -1。
- Smooth G²：`ban.java`、`dan.java`、`fqw0.java`、`opw0.java`、`uwl.java`、`wcf.java`。

## 未完成与交付纪律
1. 按 Voyage 反编译实现逐一核对八种 Switch 的真实外观与交互。
2. 按四条原始绘制路径核对 Slider。
3. 真实移植 50 个矢量形状、预览、选中边框、None/Random 和持久化。
4. 最后实现 Smooth G² 原始 profile 与 path 算法。
5. 继续维护完整总记录；资料库上传被限流 9 小时，按约束停止重试，本轮不声称资料库同步成功。
6. 交付必须匹配最终 HEAD，并提供 APK 直链、run/build、文件名、大小、SHA-256、包名/版本、Owner 证书 SHA-256、V2 签名与完整性校验。未完成的验证如实标注。


## 2026-10-26 续查：Build #1605 与 APK 产物核验

- 实际查询 Build #1605 / Run ID `40461181776`：job `build` 的终态为 `completed / success`。源码校验、Kotlin/Android 编译、Owner 签名密钥加载、Owner 签名 APK 构建、包名/固定证书检查、Artifact 保存步骤均成功；Release 发布步骤按测试分支工作流被跳过。
- 对应提交为维护记录提交 `7b36a6abfca09609a7e56c0cd9b7a6b75a87cb97`，分支 `test/voyage-theme-1to1-20261026`。该提交仅新增维护续写文档，不包含 Switch/Slider/形状/G² 源码修复；因此不能把 Build #1605 描述成新的主题移植代码已验证。
- Artifact：`AlohaOneUI-Signed-Test-APK`，Artifact ID `12714711691`，ZIP 大小 `27,737,864 bytes`，ZIP SHA-256 `9a62a24831cb34c3165ab132417227eb2915795aa76731e4a44a60ad8047f798`。已下载并通过外层 ZIP CRC 检查；APK 内部 ZIP CRC 检查也通过。
- APK：`AlohaOneUI_1.0.261025_BUILD_1605.apk`，`28,728,945 bytes`，SHA-256 `5fa582953b49667094de3fad9a3447931f9b71b24b97bd70c0aa8cf973d840ec`；包名 `com.alohaoneui.app`，`versionName=1.0.261025`，`versionCode=16`。Owner 证书 SHA-256 为 `93bf61a6493c6b336383832eb41301bd8fe061f43566e7e384e3fdd9fc1c26a1`，APK Signature Scheme V2 验证为 true，单签名者。
- 源码审计发现：当前 `ExpressiveSwitch.kt` 将八个分支与颜色、半径、thumb 比例等参数直接硬编码，但文件本身没有可核验的逐样式来源映射；`SegmentedSliderItem.kt` 仍以 Material3 `SliderDefaults.Thumb/Track` 通过尺寸、颜色等参数分支实现。两者目前均不能证明与 Voyage 原版绘制路径 1:1 一致，继续列为未完成，不接受以当前实现/构建成功替代原版源码核对。
- 已从资料库取回 `Voyage主题框架.zip`（399,486 bytes），内容为资源框架 XML 等 50 个条目，不包含目标 Java 反编译类。Voyage 原始 APK（151,234,453 bytes）可读取，但本轮未能获得可用的 Java 反编译输出；故本轮没有修改控件实现，避免根据类名或外观猜测继续制造差异。下一批先恢复/生成目标类的反编译文本，再严格按用户指定顺序修改 Switch、Slider、50 个图标形状，最后才处理 Smooth G²。
- 本轮没有修改/合并 `main`，没有改变版本、包名、更新源或签名配置。资料库新配对上传状态仍未确认；不得声称已重新同步。


## 2026-10-27 续查：定位原始 DEX 反编译输入

- 检查用户指定的 Voyage 源码资料仓库 `iskyxpcity/AppData` 的完整 Git tree（递归树 6,067 项，未截断）。`voyage/` 下存在 14 个原始 DEX（合计 196,366,552 bytes），但整个仓库中没有任何 `.java` 反编译源码文件；`gfj0.java`、`kdj0.java`、`ban.java` 等文件名只存在于用户给出的待核对索引，不存在于仓库可取回的 Java 源文件。
- 本轮执行环境有 Java runtime，但没有安装 jadx、apktool、baksmali 或 androguard，且运行环境不能联网下载反编译工具；因此不能在此轮诚实地宣称已完成这些 DEX 的 Java 反编译或逐行对照。已确认输入确实是 DEX，而不是“源码已在 GitHub 可读”的状态。
- 为避免再次凭想象改写样式，本轮未修改 `ExpressiveSwitch.kt`、`SegmentedSliderItem.kt` 或其它产品源码，也未触发新的 APK 构建。该项是阻塞性工具/源码恢复问题，不代表任务完成。
- 下一步必须先得到可用的反编译结果：对 `voyage/classes*.dex` 执行 JADX（或等价的 DEX→Java 反编译），至少导出用户指定的 Switch、Slider、图标形状和 Smooth G² 类；之后按既定顺序以方法体/路径为依据进行逐项移植。禁止用 DEX 文件名、资源 XML 或现有 Aloha 参数猜测替代方法体核对。
- 本轮仍只在 `test/voyage-theme-1to1-20261026` 记录维护状态；没有触碰 `main`。


## 2026-10-27 继续推进：原始 DEX 类与方法表提取

- 直接从用户提供的原始 APK 解出 14 个 DEX 到本地临时目录，并对 DEX 头、字符串表、类型表、原型表、方法表和 class_data 进行结构化解析；本次不是仅按文件名猜测。
- 确认目标类在 APK 中的实际分布包括：gfj0、kdj0 在 classes5.dex；h7w0、d8t0、eig0、k9t0 在 classes7.dex；y8f0 在 classes2.dex；gaf0、haf0、iaf0、jaf0、fqw0 在 classes6.dex；ban、dan、shq、qha0、vqp、ewm 等类在其它主 DEX 分布中。部分类在多个 DEX 中重复出现，需结合 class descriptor 与方法定义进一步去重。
- 已提取相关 class_data 中的真实方法签名、参数类型、访问标志和 code_off。关键初步结果：gfj0、kdj0 为枚举类（可见 <clinit> / valueOf / values）；h7w0 含多条静态 UI 构建/设置方法；y8f0 含多条绘制路径方法；d8t0 含多条静态构建与位图/形状相关方法；ban、dan、wcf 暴露几何数据结构与参数方法；fqw0、opw0 含相关几何运算方法。
- 重要限制：上述结果是 DEX 结构解析出的真实方法表与代码偏移，不是 Java 反编译源码，尚未还原方法体控制流/绘制细节，因此不能据此宣称 1:1 移植完成。尝试用 GitHub connector 在测试分支创建自动反编译工作流时被安全检查拦截，未创建文件，也未触发 Actions。
- 当前可用推进成果：目标类与方法入口已缩小到具体 DEX 和 code_off，避免后续反编译时对整包盲查。下一步仍需可执行的 DEX 反编译器或 smali 反汇编器，恢复目标方法体后才能进行源代码级修改。
- 本轮没有修改任何产品源文件，没有构建新 APK，没有触碰 main。

## 2026-10-27 继续推进：目标方法表结构化提取

- 从原始 APK 解出 classes.dex 至 classes14.dex，使用本地脚本解析 DEX header、string_ids、type_ids、proto_ids、method_ids、class_defs 和 encoded class_data，提取目标类的方法签名、访问标志及 code_off，并记录方法指令区长度。
- 实际解析到 321 条目标类方法记录。确认 `gfj0` 与 `kdj0` 位于 classes5.dex，二者均有 `<clinit> / valueOf / values`；`y8f0` 位于 classes2.dex，含多条较长绘制路径方法（其中 `IO` 1220 code units）；`ban` 位于 classes.dex，`I(JFFFF)Lxl30;` 方法为 1433 code units、`l(J)Lxl30;` 为 949 code units；`fqw0` 位于 classes6.dex；`opw0` 位于 classes6.dex，含几何/曲线运算方法；`d8t0` 位于 classes7.dex。
- 这一步将目标方法从类名线索推进到 DEX 方法表和代码区位置。当前只完成结构化提取，尚未完成可靠指令解码或 Java/smali 还原；不能把 code_off 或指令长度当作算法已理解，也未据此改动产品实现。
- 未修改产品源码，未构建新 APK，未触碰 `main`。下一步继续恢复目标方法的可读指令与调用关系，再按既定顺序移植。

## 2026-10-27 继续推进：DEX 指令层验证

- 对目标方法代码区进行本地指令解码实验，进一步定位到 `gfj0.<clinit>`（149 code units）、`kdj0.<clinit>`（81 code units）、`h7w0` 多个 UI 构建方法，以及 `ban.I(JFFFF)`（1433 code units）、`ban.l(J)`（949 code units）、`y8f0.IO(...)`（1220 code units）等目标实现。
- 解码器当前仅为临时分析脚本，部分 Dalvik 指令格式/混淆引用尚未完整覆盖，输出不能作为可信的源码还原结果；未据此推断具体参数、几何公式或 8 种 Switch/4 种 Slider 的视觉定义。
- 已确认本地环境没有 jadx、baksmali、apktool、androguard，且无法连接 Python 包源；因此本轮没有可靠恢复方法体控制流，也没有进行产品源码修改或新 APK 构建。`main` 未修改。
- 下一步继续从可用的 Voyage 资源与 DEX 引用中交叉验证组件映射；只有在方法逻辑和参数映射可靠后才调整产品实现，避免用猜测覆盖现有实现。


## 2026-10-27 继续推进：Voyage 主题资源审计

- 对用户提供的 `Voyage主题框架.zip` 重新进行资源清点：共 50 个条目、32 个 XML；`res/values/styles.xml` 含 413 个 style 声明，`values-night/styles.xml` 含 16 个 style 声明。确认其中包含 Material 3 组件与 ShapeAppearance 定义，但未找到可直接对应目标 8 种 Switch、4 种 Slider 或 50 种图标预设的完整 Java/Kotlin 源实现。
- 资源审计定位到 `ShapeAppearance.M3.Sys.Shape.Corner.Full`、`ShapeAppearance.M3.Sys.Shape.Corner.None`、`ShapeAppearance.MaterialComponents`、`Widget.Material3.SideSheet` 等主题框架定义；`dimens.xml` 中通用 AppCompat 控件角半径 `abc_control_corner_material=2dp`，不可误用为 Voyage 自定义 Smooth G² 角半径。当前 ZIP 的 style/attrs/dimens 资源可以用于主题基线，不足以证明控件绘制算法或各风格参数。
- 本地环境再次确认没有 jadx、baksmali、apktool、androguard 或 Android SDK `android.jar` 可用；因此未能在本轮完成可信 Java/smali 还原。未修改产品源码、未构建新 APK，`main` 未修改。
- 后续继续以原始 DEX 方法引用与资源 ID/样式定义交叉分析，优先建立可验证的控件—资源映射，再落地实现，避免凭名称猜测参数。


## 2026-10-27 继续整批任务：控件实现核查与反汇编推进

- 已通过 GitHub 提交差异核对当前测试分支真实源文件路径：`app/src/main/java/com/alohaoneui/app/ui/component/material/ExpressiveSwitch.kt` 与 `SegmentedSliderItem.kt`。当前 Switch 使用手写 Canvas 与人为定义的圆角/半径常量；Slider 使用 Material3 `SliderDefaults.Thumb/Track` 并以 16/20/24dp 拇指、4/6/8dp 轨道等常量区分风格。这些实现当前没有足够的 Voyage 原始方法证据支撑 1:1 等价，不能当作已完成移植。
- 对本地 14 个 DEX 的目标类指令区进行了进一步反汇编尝试，确认 `gfj0.<clinit>` 构造 8 个枚举实例、`kdj0.<clinit>` 构造 4 个枚举实例；`ban.I(JFFFF)`、`ban.l(J)`、`dan.l()` 和 `y8f0.IO(...)` 存在较长几何/绘制方法。现有自写反汇编器对部分 opcode/指令格式的覆盖不完整，字段与调用解析有误读风险，不能直接据其生成产品算法。
- 代码核查发现 `ExpressiveSwitch` 的 `colors: SwitchColors` 参数在绘制分支中未被使用，且 `thumbContent` 当前覆盖整个开关容器；这些是当前实现的可疑点，但因还未完成原版语义对照，本轮暂不凭猜测改变其行为。
- 本轮完成的是源文件定位与反汇编交叉核查；尚未提交产品源码变更，尚未构建新 APK。`main` 未修改。下一步优先修正/补齐 DEX opcode 解码与枚举字段/字符串关联，取得可验证的源参数后再批量改写 Switch、Slider、50 个图标预设和 Smooth G²。


## 2026-10-27 继续整批任务：原始 DEX 字符串与 Smooth G² 线索

- 对 14 个 DEX 的 string_ids 做了针对性扫描，确认原始 APK 中存在 `G2Continuity`、`G2ContinuityProfile(extendedFraction=...)`、`AbsoluteRoundedCornerShape(...)`、`CutCornerShape(...)` 等真实符号/日志字符串。这进一步证明 Smooth G² 任务应围绕连续角轮廓参数与 Shape 构造路径追踪，而不是简单替换 `RoundedCornerShape` 的固定半径。
- 同时发现 DEX 字符串表混有大量第三方库与其他业务功能字符串；仅靠全局字符串搜索会产生大量误命中，不能把 `Switch`、`Slider` 或 `shape` 等普通词直接认定为目标实现。下一步需从目标类的 field/method 引用表和代码区逐级跟踪到 `G2ContinuityProfile`、路径构造器及资源 ID。
- 本轮未改产品源码、未构建 APK；维护记录继续追加到测试分支，`main` 保持不变。


## 2026-10-27 继续整批移植：Smooth G² 类型与引用定位

- 完成对 14 个 DEX 的定向字符串引用扫描，不只搜索字符串表，而是扫描方法代码中的 `const-string` 指令并映射回类/方法。已确认 `dan.toString()` 与另一 DEX 中 `can.toString()` 实际引用 `G2ContinuityProfile(extendedFraction=...)`；`ban` 持有两个 `dan` 类型实例字段，表明它与 G² profile 对象存在直接结构关联。
- 进一步解析类字段/方法表：`dan` 具有四个 Double 字段和一个 `wcf` 类型字段；`wcf` 持有四个 `k580` 类型字段；`ban` 的方法包含 `I`、`O`、`l`、`equals`、`hashCode`、`toString`。`y8f0` 位于 classes2.dex，含多条静态绘制方法；`gfj0`/`kdj0` 分别为 8/4 个枚举值的 Kotlin enum 类。以上是可复核的类结构证据，尚未代表完整算法反编译。
- 该方法定位把 G² profile 与其几何消费者关联起来；后续将继续解码 `ban`/`dan`/`wcf` 的方法调用及参数变换，再用同一流程解析 Switch/Slider 的枚举构造参数和图标形状工厂。
- 本轮没有改产品源码或构建新 APK；测试分支维护记录已更新，`main` 未修改。


## 2026-10-27 继续推进：修复 Switch 自定义拇指内容定位

- 对测试分支 `ExpressiveSwitch.kt` 做了实际源码修改：此前传入 `thumbContent` 时，它被放在整个 52×32dp 开关容器中心，既没有裁进拇指，也没有随 checked 动画移动；现在将内容限制在当前样式对应的拇指直径内，并依据拇指行程与 `travel` 动画偏移，使其随拇指一起移动。
- 将样式相关的拇指内缩比例提取为共享参数，Canvas 绘制和 Compose 内容定位使用同一比例，避免两套位置公式漂移。随后修正 `Modifier.offset` 的 Compose import。
- 源码提交：`83f6600961e5e19e8e1ccff4dc65cbc9b06a4b83`。此项是实际产品源码修复，但它只修复自定义 thumbContent 的定位/动画行为，不证明八种 Switch 已与 Voyage 原版 1:1 一致。
- 本次尚未读取到该新提交对应的 CI 终态，也未核验新 APK；后续必须检查编译结果，若失败立即修复。未修改/合并 `main`。


## 2026-10-27 继续推进：复核 Switch 拇指定位公式

- 重新读取测试分支当前 `ExpressiveSwitch.kt`，确认 `thumbContent` 现已使用与 Canvas 同源的 `thumbRadiusFraction` / `thumbInsetFraction` 计算尺寸与总行程，并通过 `Modifier.offset` 随 `travel` 动画移动；`Modifier.offset` 已改用 `androidx.compose.foundation.layout.offset` 正确导入。
- 复核定位公式：控件基准宽高为 52×32dp，拇指中心从左右内缩位置移动；`thumbTravel = 52dp - 2 × 32dp × (thumbInsetFraction + thumbRadiusFraction)`，偏移范围为 `[-thumbTravel/2, +thumbTravel/2]`。该公式与当前 Canvas 轨道/拇指几何一致，避免之前整容器居中覆盖的问题。
- 当前可用 GitHub workflow 查询接口只返回 PR 触发的关联运行；对该提交查询没有返回运行记录，因此尚不能据此判断普通 push workflow 的构建结果。未把它误报为构建成功。
- 下一步继续推进原版参数恢复与图标形状工厂；在取得可靠的 Voyage 方法体证据之前，不猜测 8 种 Switch 的原始颜色、半径和 4 种 Slider 的绘制参数。`main` 未修改。


## 2026-10-27 继续推进：核对原版 Smooth G² 关键方法签名

- 直接复核从 Voyage APK 提取的 DEX 方法表，进一步确认 `ban` 的关键几何入口：`I(J,F,F,F,F)` 返回 `xl30`，`l(J)` 返回 `xl30`，`O(dan)` 返回 `wcf`；`dan` 构造器接收四个 Double，`dan.l()` 返回 `wcf`；`wcf` 构造器接收四个 `k580`，并提供 `ll(D)` 变换方法。这些签名证实 G² 算法链由四个几何参数到四段/四类几何值组合组成，不能用单一 `RoundedCornerShape` 近似替代。
- `y8f0.IO(...)` 是一条长绘制方法（1220 code units），包含浮点数组、多个颜色/绘制参数及回调；后续应恢复其曲线路径，而不是继续手写猜测路径。
- 当前仍未获得可信的完整指令反编译输出，因此本轮不伪造原版参数，也不把近似 UI 宣称为 1:1。已将本轮 DEX 方法签名证据记入维护记录；产品源码本轮未新增修改，`main` 未动。


## 2026-10-27 继续推进：区分 Android 资源主题与 Voyage 自定义组件

- 重新检查 `Voyage主题框架.zip` 的 `res/values/styles.xml`：413 个 style 声明，`ShapeAppearance` 仅 17 处，属于常规 Android/Material 资源主题；其中没有 `G2Continuity`，也没有 Slider 的自定义绘制实现。因此不能把这份资源包误当作 50 种图标轮廓或 Smooth G² 算法的完整来源。
- 继续核对当前 `SegmentedSliderItem.kt`：现有四套样式仍共享 Material3 `SliderDefaults.Thumb/Track`，只是更换尺寸、颜色和轨道参数；这还不能证明符合 Voyage 的 `y8f0.IO(...)` 原始绘制路径。暂不再猜写曲线，避免将近似效果冒充原版。
- 当前分支仍是 `test/voyage-theme-1to1-20261026`；`main` 未修改。本轮没有新增产品代码或新 APK，下一步继续从原始 DEX 的方法指令/引用表恢复可验证的几何与控件参数。


## 2026-10-27 继续推进：检查 APK 资源中是否存在可直接复用的形状预设

- 扫描原始 Voyage APK 的资源目录，共发现 2,300 个 XML 资源文件；没有独立命名为 shape/icon/preset 的 XML 文件可直接作为 50 个图标轮廓预设导入。主题框架 ZIP 中的 `ShapeAppearance` 主要是 Material 3 通用圆角样式，不能替代目标预设库。
- 因此 50 个形状的后续实现必须继续追踪 `k9t0` / `vqp` / `eig0` / `d8t0` 等代码生成的实际几何轮廓与选择状态，不能用 50 个不同圆角数值拼凑来冒充原版。
- 本轮完成原始 APK 资源排查并记录结果；未修改产品源码、未生成新 APK，`main` 未修改。


## 2026-10-27 继续推进：复核当前 Switch 交互几何与原版证据边界

- 复核 `ExpressiveSwitch.kt` 的 thumb 内容动画定位公式：未选中/选中两端的中心位置与 Canvas 自绘 thumb 使用同一组 inset、radius 参数，现有公式在几何上闭合；暂不再改动这一段，避免没有原版证据的参数漂移。
- 当前代码仍有明显的来源验证缺口：8 种样式的颜色、半径、边框和弹簧参数是本项目中的实现值，现有证据不能证明它们来自 Voyage 原始 `gfj0` / `h7w0`。因此不能把这部分标记为“已完成 1:1”。
- 继续搜索公开的 AppData 源码索引未找到 `m108318Io` 的可检索反编译结果；接下来需要依赖本地 APK 的 DEX 指令解析继续恢复真实参数。维护记录更新完成；本轮没有新增产品代码或 APK，`main` 未修改。


## 2026-10-27 继续推进：补齐目标 DEX 方法索引（本轮）

- 重新解析已提取的 14 个 Voyage DEX，生成目标类方法清单，定位到：
  - Switch 枚举类 `gfj0`、Slider 枚举类 `kdj0`（均位于 `classes5.dex`，含静态初始化器）。
  - Switch UI 构造方法位于 `h7w0` 所在 DEX；Slider 绘制核心 `y8f0.IO(...)` 位于 `classes2.dex`，代码长度 1220 code units。
  - G² 连续角核心 `ban.I(J,F,F,F,F)` 与 `ban.l(J)` 分别位于 `classes.dex`，代码长度分别为 1433、949 code units；`dan` 四个 Double 参数映射到 `wcf` 四个 `k580` 几何参数。
  - 形状相关 `d8t0`、`eig0`、`k9t0` 与 `qha0`、`shq`、`vqp`、`ewm` 均已确认存在于 DEX 方法索引，后续优先还原实际路径生成与选择持久化链。
- 本轮未把不完整的自制 DEX 指令解码误报为已恢复源码；尚未据此改写 UI，也未产出新 APK。原因是当前运行环境没有 jadx/baksmali/apktool/Android SDK，必须继续恢复可验证的原始指令与常量，避免再凭外观猜测实现。
- 产品代码提交：无；APK：无；`main`：未修改。
- 下一批执行顺序仍为 Switch → Slider → 50 种形状 → G² Smooth，完成后再进行正式构建、签名和 APK 校验。


## 2026-10-27 继续推进：形状选择页调用链定位

- 深入检查 `k9t0` 的 DEX 方法表：`<clinit>` 118 code units；页面构建入口 `I(...)` 819、`II(...)` 454、`IO(...)` 1157、`lO(...)` 811；另有图像转换/缓存方法 `OI`、`OO`、`oO`、`oo`。这些是形状/图标设置 UI 及预览链的下一批反汇编目标。
- `d8t0` 包含 `lII(...): byte[]`、`oo(...): Bitmap`、`lIl/llO/llo(...): float` 等方法，表明它还承担图像/几何辅助处理；不能把它误当成单纯的 50 项常量表。
- 本轮只完成方法调用链定位，没有把混淆后的常量擅自命名成 50 个预设，也没有提交猜测性的产品代码。下一步将继续还原静态初始化、路径数据来源和选择状态写入点，再据证据落地实现。
- 本轮产品代码与 APK 状态不变；`main` 未修改。


## 2026-10-27 交付规则执行：修复 Switch 编译错误并触发完整构建

- 实际产品代码已修改（测试分支）：
  1. `ExpressiveSwitch` 的默认样式现在使用调用方传入的 `SwitchColors` 中 checked/unchecked track、thumb、icon 颜色，修复参数此前未生效的问题。
  2. 修复 thumbContent 横向位移计算中 `Float * Dp` 的 Kotlin 类型错误，改为 `Dp * Float`，对应之前 Actions 构建失败的编译点。
- 代码提交：`17199ac3b527d8311fef33a05e04fb98068f8c3a`（默认颜色参数）；`3bea499a4d6b082de1058edb036953b3d165503c`（Dp 运算编译修复）。
- 已触发 GitHub Actions 构建；必须以最新提交的构建结果为准。之前构建 `40466474706` 失败原因已从日志确认是 Switch 的 Dp 运算类型不匹配，尚不能作为交付 APK。
- `main` 未修改；当前改动仍在 `test/voyage-theme-1to1-20261026`。本轮尚未确认新 APK、签名或哈希，未宣称交付完成。


# Aloha OneUI 维护续写 — 2026-10-27 07:28 UTC

## 本轮构建核验
- 仓库：`iskyxpcity/Oneuixpcity`
- 分支：`test/voyage-theme-1to1-20261026`（未修改 `main`）
- 最新提交：`c61b040eb1cf52c650623c409eca26b6c14fd81f`
- GitHub Actions：Run `40466814918`，状态 `completed`，结论 `success`；`build` job 成功。
- 构建产物：`AlohaOneUI-Signed-Test-APK`，artifact ID `12716743118`，归档大小 `27,738,979` bytes；Artifact ZIP SHA-256：`fc01bbba33db3b771b964ba065e7a2c848b90bacae60bfc87316ad72ec05eeff`。
- 此结果确认当前分支可完成 CI 构建，不代表 Voyage 的全部控件已完成 1:1 移植。尚需从 APK/签名包中核验 APK 本体的版本、包名、签名和 SHA-256，不能把 ZIP 摘要当成 APK 摘要。

## 当前实际进度与未完成项
- 已修复 `ExpressiveSwitch.kt` 默认样式忽略调用方 `SwitchColors` 的问题，以及 thumb 位移 `Float * Dp` 类型错误；代码提交分别为 `17199ac3b527d8311fef33a05e04fb98068f8c3a`、`3bea499a4d6b082de1058edb036953b3d165503c`。
- 8 种 Switch 的 Voyage 原始颜色/几何/弹簧参数仍未全部通过可信 DEX 指令证据验证；不可标记为 1:1 完成。
- Slider 四套样式目前仍共享 Material3 `SliderDefaults.Thumb/Track`，尚未恢复 `y8f0.IO(...)` 的原始绘制实现。
- 50 种形状预设仍需还原实际路径、预览与选择状态；Smooth G² 仍需恢复 `ban`/`dan`/`wcf` 的真实几何算法。
- 按既定顺序继续：Switch 原版参数验证 → Slider 原版绘制 → 50 种形状 → Smooth G²；完成后再做最终 APK、签名、包名/版本与哈希验收。

## 交付纪律
- 只在测试分支工作，不改 `main`。
- 不以 CI 成功代替 1:1 视觉/行为验收；不把近似实现说成原版。
- 最终交付需提供可下载 APK、文件大小、APK 本体 SHA-256、包名/版本及签名校验结果。


## 2026-10-27 继续推进记录（Switch disabled colors）

- 分支：`test/voyage-theme-1to1-20261026`；未修改 `main`。
- 新提交：`237fd17bdab4e627e6e669c8bde859df81b43ef3`。
- 修改：`ExpressiveSwitch.kt` 的默认样式（style 0）在禁用状态下，改为读取传入的 `SwitchColors` 中 `disabledCheckedTrackColor`、`disabledUncheckedTrackColor`、`disabledCheckedThumbColor`、`disabledUncheckedThumbColor`，不再一律套用本地 alpha，保证调用方配置能实际生效。其他七种自定义样式的禁用视觉行为保持不变。
- 验证状态：代码已提交；等待 GitHub Actions 编译验证。未宣称 Voyage 1:1 已完成。
- 后续顺序仍为：对照 Voyage 原始行为校准 8 种 Switch → 4 种 Slider → 50 个图形路径与预览 → Smooth G²；只提交有依据的实现，构建失败立即修复，最终需验证 APK 包名、版本、签名、哈希。


## 2026-10-26 追加：Switch 禁用颜色配置修正

- 分支：`test/voyage-theme-1to1-20261026`；`main` 未修改。
- 代码提交：`237fd17bdab4e627e6e669c8bde859df81b43ef3`。
- 修改 `ExpressiveSwitch.kt`：默认样式（style 0）禁用时使用调用方提供的 `disabledCheckedTrackColor`、`disabledUncheckedTrackColor`、`disabledCheckedThumbColor`、`disabledUncheckedThumbColor`，不再统一使用局部 alpha 覆盖配置。
- 维护记录提交：`df5f38391620958700fe46a93c5048e0a27c5e4d`（记录曾追加在旧时间戳文件中，现已合并至本日总记录）。
- 构建：Build #1630（Run `40468155462`）提交 `237fd17b` 正在构建；截至记录时尚未确认结果。Build #1631 是随后维护文档提交触发的运行，不应替代对产品代码提交的构建核验。
- 当前仍未完成 Voyage 1:1：8 种 Switch 的原始参数验证、4 种 Slider 原始绘制、50 种真实形状路径/预览/选择状态、Smooth G² 原始几何算法均待继续。执行顺序保持不变；只在测试分支工作，编译失败即修复，最终验收真实 APK 包名、版本、签名与 SHA-256。


## 2026-10-26 本轮整批推进（继续）

### Slider 第一轮实现
- 在 `SegmentedSliderItem.kt` 开始把四种 Slider 从仅调用 Material 默认 Thumb/Track 的方式改为项目内自绘 Track/Thumb，保留 Material SliderState 的拖动、离散步进和范围语义。
- 四种样式分别定义轨道厚度、圆角、缩略图尺寸、轨道颜色和刻度表现；补正 SliderState 初始化参数为当前依赖使用的 `trackRange`，并修正 Thumb 的测量尺寸与 Canvas 绘制尺寸。
- 代码提交：
  - `53172b779717ae6a8dff9ff77c69b8c7c9d79bdc` — 自绘 Slider 轨道与 Thumb 初版。
  - `4b0d005576cb870a445533917ed67854b39099d2` — 修正 SliderState API 和 Thumb 尺寸。
- 当前仍属于主题组件实现阶段，四种视觉尚未与 Voyage 原始 APK 完成逐像素验收；不得将其标注为已完成 1:1。
- 最新提交已触发 GitHub Actions，需等待该分支最新代码的构建结果；若失败，读取日志并修复后再继续批量实现。
- 下一批仍按既定顺序推进 50 种图标形状预设，再处理 Smooth G²；不触碰 `main`。

## 2026-10-26 HST 续查：Slider 构建确认与 50 种图标形状预审

- 检查最新工作流 Run #1637（ID `40469136995`），当前 HEAD `47ceff6d7eaf3a4e96ef7a64160ed44cb3400c58` 的 build job 为 `completed / success`。较早的 Slider 首版提交曾在 Kotlin 编译阶段失败，错误涉及 `rememberSliderState` 参数名和 Canvas 绘制作用域中的 Composable 调用；后续 API 修正已包含在当前 HEAD 并通过编译。该结果只确认当前代码可编译，不等于 Voyage 原版视觉 1:1 验收。
- 开始下一顺序任务前，对图标形状设置链路做了预审：`ColorPaletteScreenMaterial.kt` 目前只提供 `None` 与 `Random` 两项；`SettingsRepositoryImpl.kt` 将 `materialIconShapeStyle` 限制在 `0..1`；`LocalMaterialIconShapeStyle` 已由主题提供，但 50 个原版形状预设、逐项预览和对应应用行为尚未移植完成。
- Voyage 原 APK 可供本地检查，但当前取得的拆分材料主要是 DEX 与资源框架 XML，目标逻辑使用混淆类名。后续必须继续核对原版形状数据与 `None=-1`、`Random` sentinel 的实际索引/保存行为；不以临时生成的近似形状冒充原版。
- 本轮继续严格按既定顺序：先完成四种 Slider 的原版对照，再推进 50 种图标形状，Smooth G² 仍最后处理。分支约束不变：只改 `test/voyage-theme-1to1-20261026`，不改 `main`。
