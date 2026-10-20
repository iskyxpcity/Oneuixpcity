# KernelSU 上游主题同步规则

OneUI Aloha 的 **Material 3** 与 **Miuix** 两套界面风格都应持续对照 [KernelSU 上游](https://github.com/tiann/KernelSU)，不能把某个依赖版本或某次移植当成永久终点。

## 当前核对基线

- 上游仓库：`tiann/KernelSU`
- 最近核对的上游提交：[`9f5966b4d534869fd72a79b3c6ea602e5eae3022`](https://github.com/tiann/KernelSU/commit/9f5966b4d534869fd72a79b3c6ea602e5eae3022)
- Material 3：`1.5.0-alpha28`
- Miuix：`0.9.4`
- 受跟踪的源文件、Blob SHA 与版本基线见 `docs/upstream/kernelsu-ui-sync-baseline.json`。这里的文件路径和 SHA 指向第三方 `tiann/KernelSU` 仓库，应按上游原样保留。

## 持续跟进机制

`.github/workflows/kernelsu-upstream-watch.yml` 每周检查一次，也可手动触发。若上游相关导航栏、主题预览、主题文件或版本目录发生变化，会创建或更新 **KernelSU upstream UI/theme sync review** 议题，列出上游提交、差异文件和版本变化，提醒维护时核对。

监测器不会盲目覆盖 OneUI Aloha 的代码：四个主导航目的地、应用自有图标、`com.oneuialoha.app` 包名、本项目设置和行为必须保留。收到提醒后，分别对照 Material 3 与 Miuix 上游，移植适配变更，运行源码校验、编译、签名及 APK 完整性检查，并由真机验收后才考虑合并主线。上游 API 变化需要同步修正依赖与相关组件，不能因为一次编译问题永久冻结在旧版本。

这个监测器只识别需要人工 review 的上游差异，不会把第三方源码自动同步进 OneUI Aloha，也不会自动合并或更新本项目 `main`。
