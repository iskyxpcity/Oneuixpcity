# OneUI XP 项目结构与独立化约定

## 项目标识

- Android application ID：`com.oneuixp.app`
- 应用命名空间：`com.oneuixp.app`
- 当前版本由 `gradle.properties` 中的 `oneuixpVersionName` 与 `oneuixpVersionCode` 管理。
- Xposed 主入口：`com.oneuixp.hook.Main`。
- 所有可分发 APK 使用同一套 OneUI XP 永久签名密钥。

## 自有命名空间

| 模块 / 源码 | 命名空间或路径 |
|---|---|
| 应用代码 | `com.oneuixp.app` |
| 迁移后的功能 UI | `com.oneuixp.mod.ui` |
| `common` 模块 | `com.oneuixp.common` |
| `hook` 模块 | `com.oneuixp.hook` |
| `stub` 模块 | `com.oneuixp.stub` |
| 应用功能目录 | `com.oneuixp.app.oneuixp` |

源码目录、Kotlin / Java 的 `package` 声明、`import`、Gradle `namespace`、Xposed 入口清单和 CI 路径检查必须保持一致。更改目录或类名时，应一并更新所有引用。

## 构建与发布约定

- `.github/workflows/build.yml` 执行源码预检查、编译、签名及 APK 元数据验证。
- Actions Secrets 只用于 APK 签名：`KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`。
- 自有仓库同时是源码、正式发行与测试预发布的唯一位置；不维护跨仓库镜像。
- 不配置跨仓库发布 Token，也不保留旧项目的同步或自动合并流程。
- 变更应先在独立分支完成构建验证，再考虑合并到 `main`。

## 第三方许可证

第三方许可证文件存放于 `LICENSE` 与 `third_party/THIRD_PARTY_MOD_LICENSE.txt`。本次仅规范文件路径，不改写许可证正文、版权归属或必要的来源声明。
