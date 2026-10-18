# OneUI XP

面向 Samsung One UI 的 LSPosed 应用，使用 KernelSU v3.2.5 双主题 UI 框架（Miuix / Material），但**不是 KernelSU Root 管理工具**。

## 当前状态

- 应用 ID：`com.oneuixp.app`
- 版本号支持三段格式 `1.0.YYMMDD` 与四段补丁格式 `1.0.YYMMDD.PATCH`；当前基线 `1.0.261019`
- 现代 Xposed 模块：`app/src/main/resources/META-INF/xposed/`（API 101）
- 兼容旧模块入口：`app/src/main/assets/xposed_init`
- 唯一预设作用域：`com.android.systemui`
- 已由手机实测：LSPosed 服务可即时获取管理员配置的 SystemUI 作用域激活状态。
- SystemUI heartbeat / 只读类诊断只证明实际进程注入；它与「已勾选作用域」是不同状态。
- 状态栏实际视觉 Hook 尚不应视为已完成，不能把只读探针当作效果验证。

## 主要目录

- `app/src/main/java/com/oneuixp/app/statusbar/`：LSPosed/SystemUI 状态及只读诊断。
- `app/src/main/java/com/oneuixp/app/ui/`：两套 UI 和四个底部页面。
- `app/src/main/resources/META-INF/xposed/`：现代模块必需的启动/作用域元数据。
- `app/src/main/assets/xposed_init` 与 `xposed_scope`：旧版 Xposed 兼容入口。
- `.github/workflows/build.yml`：GitHub Actions 检查、编译、签名与验证。

## 构建与签名

正式分发 APK **只**使用 OneUI XP 专用的永久签名密钥。仓库 Actions Secrets 中应配置：
`KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`。
签名证书的公开 SHA-256 指纹在构建工作流中固定校验；签名私钥和密码不得提交到 Git。

更新检查使用公开发行仓库的 [Releases](https://github.com/iSkyJIE/OneUIXP-Releases/releases)。
构建产物见 [GitHub Actions](https://github.com/iskyxpcity/Oneuixpcity/actions)。

## 开发与历史记录

历史阶段报告已从代码仓库根目录清出，以保持主线简洁；原始逐字内容仍可在清理前的 Git 快照
[`7d35d7d`](https://github.com/iSkyJIE/OneuiXP/tree/7d35d7d1e1fc8e092b9d094b95b1fc4809df285d)
中查阅。持续接力记录另存于个人资料库 `/OneuiXP/OneuiXP_接力记录_2026-09-22.md`。
第二轮已在独立分支移除旧 Root、模块安装、刷写、WebUI、特权执行、protobuf 与原生构建实现；保留两套主题布局、状态栏诊断、现代与旧版 LSPosed 入口、更新检查及固定签名。代码与 APK 须经过 CI、真机验证后才能合并。

## UI 来源与许可证

OneUI XP 是独立的 LSPosed/SystemUI 应用，不连接或调用 KernelSU 的 Root/模块功能。部分 Miuix / Material UI 框架及视觉组件改编自 [KernelSU v3.2.5](https://github.com/tiann/KernelSU/tree/v3.2.5)，保留原始作者署名与 GPL-3.0 许可证文本（见 `LICENSE`）。功能独立不等于抹除保留代码的来源或许可证义务。
