# OneUI Aloha 项目结构与独立化约定

## 当前应用身份

- 用户可见应用名：`OneUI Aloha`
- Android application ID / app namespace：`com.oneuialoha.app`
- 当前迁移候选版本属性：`oneuiAlohaVersionName=1.0.261021.1`、`oneuiAlohaVersionCode=11`（当前迁移测试候选值；正式发布前另行确认并同步更新验证规则）。
- Xposed 主入口：`com.oneuialoha.hook.Main`。
- 模块命名空间：`com.oneuialoha.common`、`com.oneuialoha.hook`、`com.oneuialoha.stub`。
- 可分发 APK 继续使用固定 Owner 签名证书基线：`93bf61a6493c6b336383832eb41301bd8fe061f43566e7e384e3fdd9fc1c26a1`；不得重生成或替换签名密钥。

## 项目结构与命名空间

| 模块 / 源码 | 命名空间或路径 |
|---|---|
| 应用代码 | `app/src/main/java/com/oneuialoha/app/` |
| 功能设置 UI | `app/src/main/java/com/oneuialoha/mod/ui/` |
| `common` 模块 | `com.oneuialoha.common` |
| `hook` 模块 | `com.oneuialoha.hook` |
| `stub` 模块 | `com.oneuialoha.stub` |
| Aloha 特定功能模型与桥接 | `com.oneuialoha.app.oneuialoha` |

源码目录、Kotlin / Java 的 `package` 声明、`import`、Gradle `namespace`、Manifest 应用类和主题、Xposed 入口/作用域清单与 CI 路径检查必须保持一致。任何包或文件名变更均须同时调整引用并通过 `verify_oneuialoha_identity.py`。

## 更新源与迁移兼容

- OneUI Aloha 迁移目标及自身正常更新源：[`iSkyJIE/OneUIAloha-Releases`](https://github.com/iSkyJIE/OneUIAloha-Releases/releases)。
- 旧 OneUI XP 安装的常规更新及在没有可用 Aloha 迁移资产时的回退源：[`iSkyJIE/OneuiXP-Releases`](https://github.com/iSkyJIE/OneuiXP-Releases/releases)。
- 旧安装识别常量 `com.oneuixp.app` 和 XP 更新源只允许保留在明确的迁移兼容逻辑中；不得把它们用作 Aloha 的 namespace、Xposed self-scope 或当前 APK package。
- Aloha 的 `applicationId` 不同于 XP，因此它作为独立应用安装，不是原地覆盖升级。用户资料/设置不会自动转移；迁移说明必须如实告知用户。

## 品牌图案与 Android 图标资源

启动器、开屏和关于页使用经过压缩的 WebP 资源；启动器自适应背景层透明，源图稿保持原样供后续编辑。

- `artwork/Logo001.jpg`、`artwork/Logo002.png` 保存用户提供的原始图稿，作为后续维护的源文件。
- 应用启动器图标在 Android 8.0+ 使用 `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`，以透明前景 `drawable/ic_launcher_foreground_logo001.webp` 放在自适应图标中央安全区，由 Samsung One UI 9 启动器应用系统圆角蒙版，图标内背景为无网格的纯色 `#E7E8E6`。旧版 Android 的 `mipmap-nodpi/ic_launcher.webp` 用同一 Logo001 重绘为带真正透明外角的 One UI 风格 superellipse 圆角方形。Manifest 的 `android:icon` 和 `android:roundIcon` 均指向 `@mipmap/ic_launcher`；旧版 Android 回退资源继续独立维护。
- `Logo002` 的应用内绘制资源为 `drawable-nodpi/logo002.webp`。原生开屏动画和 Material / Miuix 两套「关于」页面统一引用该资源，并分别控制显示尺寸。
- 不要把开屏/关于页图案与启动器图标资源混用；变更时须同时验证两类显示位置。
- 当前应用身份、`versionCode=11` 与 Owner 签名基线不因图标更新而改变。

- Logo001 每次均从未经修改的根目录原图重新生成，先清理边缘连通的棋盘背景，再裁掉外部透明边；生成的前景保留 Alpha 并完整置于自适应图标安全区，旧版回退图标同样以原稿重绘并做真正透明外角。
- 开屏动画和 Material / Miuix 两套「关于」页面继续统一以 200dp 使用同一份 R.drawable.logo002。开屏图案下移，并把两行标题基线与 Logo 的距离分别收紧；关于页图案组垂直位置略向下调整，Logo 与应用名称间距由 12dp 调整为 4dp。

## 构建与发布约定

- `.github/workflows/build.yml` 执行源码预检、编译、签名及 APK 元数据 / V2 / ZIP 验证。
- 签名 Secrets 由 GitHub Actions 环境提供：`KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`。密钥材料与密码不得写入仓库或维护记录。
- 正式 Aloha 发布由 `main` 上明确的 `release:*` 提交触发，并另需 `ALOHA_RELEASE_TOKEN` 访问 Aloha 专属 Release 仓库。迁移分支只做隔离验证，不得覆盖公开正式 Release。
- `verify_migration.py`、`verify_no_jni.py` 和 `verify_oneuialoha_identity.py` 是构建前检查；CI 成功不等于三星真机已验收。
- 所有身份变化先在隔离分支完成构建与签名检查，再单独评估是否需要 PR。未经明确授权不得更新或合并 `main`。

## 第三方许可证

第三方许可证文件位于 `LICENSE` 与 `third_party/THIRD_PARTY_MOD_LICENSE.txt`。不得为品牌清理而删除或改写许可证正文、版权归属、必要署名或外部上游引用。
