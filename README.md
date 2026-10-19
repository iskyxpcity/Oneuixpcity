# OneUI XP

OneUI XP 是面向 Samsung One UI 的独立 Android / LSPosed 项目，提供系统界面定制与双主题设置界面（Miuix / Material）。它不是 Root 管理工具。

## 当前版本

- 应用 ID：`com.oneuixp.app`
- 当前版本：`1.0.261019.1`（versionCode：`9`）
- Xposed API：`101`
- 模块入口：`com.oneuixp.hook.Main`
- 作用域由 `app/src/main/resources/META-INF/xposed/scope.list` 管理。

## 项目结构

- `app/src/main/java/com/oneuixp/app/`：应用入口、设置界面、状态诊断与更新器。
- `app/src/main/java/com/oneuixp/mod/ui/`：已迁移到独立命名空间的功能界面代码。
- `common/`：共享配置模型和通用逻辑，命名空间为 `com.oneuixp.common`。
- `hook/`：LSPosed 功能入口和 Hook 实现，命名空间为 `com.oneuixp.hook`。
- `stub/`：编译期 Android API 存根，命名空间为 `com.oneuixp.stub`。
- `.github/workflows/build.yml`：源码检查、编译、签名与 APK 验证。

## 构建与发布

正式 APK 使用 OneUI XP 专用的永久签名密钥。仓库 Actions Secrets 仅需配置：

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

签名证书指纹由构建工作流固定校验；私钥和密码不得提交到 Git。

私有源码仓库负责构建、签名及正式 Release。公开更新仓库的 APK 与发行说明由维护者手动上传，构建流程不依赖跨仓库发布 Token。

- [GitHub Actions 构建记录](https://github.com/iskyxpcity/Oneuixpcity/actions)
- [公开版本下载](https://github.com/iSkyJIE/OneUIXP-Releases/releases)

## 独立性与许可证

项目使用自身的应用 ID、源码命名空间、构建配置和发布流程，不配置旧项目的代码同步或自动合并任务。独立化不会改变应用 ID、签名或版本管理。

项目中保留的第三方组件仍遵守其原有许可证与必要署名。相关许可证正文保存在 `LICENSE` 和 `third_party/THIRD_PARTY_MOD_LICENSE.txt`；许可证文件内容保持原样。
