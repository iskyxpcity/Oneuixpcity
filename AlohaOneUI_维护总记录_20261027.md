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
