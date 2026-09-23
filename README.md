# OneuiXP

面向 Samsung One UI 的 LSPosed 应用，使用 KernelSU v3.2.5 双主题 UI 框架（Miuix / Material），但**不是 KernelSU Root 管理工具**。

## 当前状态

- 应用 ID：`com.oneuixp.app`
- 版本号格式：`1.0.YYMMDD`，当前基线 `1.0.260923`
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

正式分发 APK **只**使用 OneuiXP 专用的永久签名密钥。仓库 Actions Secrets 中应配置：
`KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`。
签名证书的公开 SHA-256 指纹在构建工作流中固定校验；签名私钥和密码不得提交到 Git。

更新检查使用本仓库的 [Releases](https://github.com/iSkyJIE/OneuiXP/releases)。
构建产物见 [GitHub Actions](https://github.com/iSkyJIE/OneuiXP/actions)。

## 开发与历史记录

历史阶段报告已从代码仓库根目录清出，以保持主线简洁；原始逐字内容仍可在清理前的 Git 快照
[`7d35d7d`](https://github.com/iSkyJIE/OneuiXP/tree/7d35d7d1e1fc8e092b9d094b95b1fc4809df285d)
中查阅。持续接力记录另存于个人资料库 `/OneuiXP/OneuiXP_接力记录_2026-09-22.md`。
此轮仅清理无运行/构建依赖的历史文档和误入库缓存；旧 KernelSU 兼容源码应在独立分支逐项验证后再移除。
