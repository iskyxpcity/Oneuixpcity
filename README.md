# Aloha OneUI

Aloha OneUI 是面向 Samsung One UI 的独立 Android / LSPosed 模块，提供系统界面定制与双主题设置界面（Miuix / Material）。它不是 Root 管理工具。

## 应用身份与版本

- 用户可见名称：`Aloha OneUI`
- Android application ID / app namespace：`com.alohaoneui.app`
- Hook 入口：`com.alohaoneui.hook.Main`
- Xposed API：`101`
- 当前正式发行版本：`1.0.261023`（versionCode：`12`）。
- Xposed self-scope：`app/src/main/resources/META-INF/xposed/scope.list`

变更后的 application ID 使 Aloha OneUI 成为独立安装的 Android 应用，而不是 OneUI XP 的原地升级。Android 不会因安装 Aloha 自动迁移旧 XP 的应用数据或 DataStore 设置；安装前请保留旧应用及需要的配置备份。

## 项目结构

- `app/src/main/java/com/alohaoneui/app/`：应用入口、设置界面、状态诊断与更新器。
- `app/src/main/java/com/alohaoneui/mod/ui/`：功能设置界面。
- `common/`：共享配置模型与通用逻辑，命名空间为 `com.alohaoneui.common`。
- `hook/`：LSPosed 功能入口与 Hook 实现，命名空间为 `com.alohaoneui.hook`。
- `stub/`：编译期 Android API 存根，命名空间为 `com.alohaoneui.stub`。
- `.github/workflows/build.yml`：源码检查、编译、签名与 APK 验证。
- `verify_migration.py`、`verify_no_jni.py`、`verify_alohaoneui_identity.py`：源码迁移、JNI 禁用与 Aloha 身份/版本/入口/资源校验。

## 更新源职责

- Aloha OneUI 自身更新及旧应用迁移目标：[`iSkyJIE/AlohaOneUI`](https://github.com/iSkyJIE/AlohaOneUI/releases)
- 已安装 OneUI XP 的旧版本常规更新与无 Aloha 可用迁移资产时的回退源：[`iSkyJIE/OneuiXP-Releases`](https://github.com/iSkyJIE/OneuiXP-Releases/releases)

旧 XP feed 与旧包名只应出现在明确的迁移/兼容逻辑中，不是 Aloha 当前应用身份的一部分。迁移检查必须继续限制为 Aloha 官方 HTTPS Release 资产，且不得将不同 application ID 的 Aloha APK 当作旧 XP 包的原地覆盖升级。

## 构建、签名与发布

构建使用 GitHub Actions Secrets 提供既有 Owner 签名凭据：

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

公开 Aloha Release 仅在 `main` 上由明确的 `release:*` 提交触发；该正式发布步骤另需单独配置 `ALOHA_RELEASE_TOKEN`，用于向 `iSkyJIE/AlohaOneUI` 发布。该 Token 与 APK 签名凭据分离，不用于迁移分支 CI；正式发布同时附带 APK SHA-256、Owner 签名信息与 APK 包信息，测试预发布包不会作为正式版交付。

固定 Owner 证书 SHA-256 基线为 `93bf61a6493c6b336383832eb41301bd8fe061f43566e7e384e3fdd9fc1c26a1`。请勿更换密钥、生成替代签名或将 keystore / 密码写入仓库。Secret 名称保持既有设置；本项目 Gradle 中使用的环境变量以 `ALOHAONEUI_*` 命名。

- 私有源码仓库：[`iskyxpcity/Oneuixpcity`](https://github.com/iskyxpcity/Oneuixpcity)
- 私有源码仓库 Actions：[`Oneuixpcity/actions`](https://github.com/iskyxpcity/Oneuixpcity/actions)
- Aloha 公开 Release 与 Aloha 更新源：[`AlohaOneUI`](https://github.com/iSkyJIE/AlohaOneUI/releases)
- 旧 XP 更新源：[`OneuiXP-Releases`](https://github.com/iSkyJIE/OneuiXP-Releases/releases)

源码预检、CI 成功、APK 已签名与三星真机验收是不同的验证阶段。正式交付前须核对最终 HEAD 对应的 Actions run、实际 APK 直链及资产、包名/应用标签/versionName/versionCode、APK SHA-256、Owner 证书指纹、APK V2 签名与 ZIP 完整性。CI 通过本身不代表已在三星真机完成验收。

## 独立性与许可证

Aloha OneUI 使用独立的 application ID、源码命名空间、构建配置与发布流程。项目保留的第三方组件仍遵守原有许可证与必要署名；相关许可证正文保存在 `LICENSE` 和 `third_party/THIRD_PARTY_MOD_LICENSE.txt`，不得为追求品牌清理而删除或篡改许可证内容。
