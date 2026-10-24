from pathlib import Path
import shutil

ROOT = Path(".")
def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")
def write(rel, value):
    p = ROOT / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(value, encoding="utf-8")
def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one anchor, found {count}")
    return text.replace(old, new, 1)

# 1) Copy the source runtime hook from the supplied archive.
write(
    "hook/src/main/java/com/alohaoneui/hook/ThemeStoreRegion.kt",
    read("extracted/hook/src/main/java/com/alohaoneui/hook/ThemeStoreRegion.kt"),
)

# 2) Add the target package and broadcast action constants.
p = "common/src/main/java/com/alohaoneui/common/Package.kt"
s = read(p)
s = replace_once(s, '    const val THEME_CENTER = "com.samsung.android.themecenter"\n',
                 '    const val THEME_CENTER = "com.samsung.android.themecenter"\n    const val THEME_STORE = "com.samsung.android.themestore"\n',
                 "Package.THEME_STORE")
write(p, s)
write("common/src/main/java/com/alohaoneui/common/ModuleActions.kt", """package com.alohaoneui.common

/** Broadcast sent to the hooked Theme Store process after its region preferences are saved. */
object ModuleActions {
    const val THEME_STORE_REGION_CHANGED = "com.alohaoneui.action.THEME_STORE_REGION_CHANGED"
}
""")

# 3) Add the region settings to the shared preference model.
p = "common/src/main/java/com/alohaoneui/common/Preference.kt"
s = read(p)
s = replace_once(
    s,
    "        val setThemeTrialNeverExpired: Boolean = true,\n",
    """        val setThemeTrialNeverExpired: Boolean = true,
        val setThemeStoreRegion: Boolean = false,
        val themeStoreRegionCountry: String = "KR",
        val themeStoreRegionSalesCode: String = "KOO",
        val themeStoreRegionLocale: String = "ko-KR",
        val themeStoreRegionMcc: String = "450",
        val themeStoreRegionMnc: String = "08",
""",
    "Theme Store preference fields",
)
write(p, s)

# 4) Install the Theme Store hooks for the Store package.
p = "hook/src/main/java/com/alohaoneui/hook/Main.kt"
s = read(p)
s = replace_once(
    s,
    """            Package.THEME_CENTER -> {
                if (preference.other.setThemeTrialNeverExpired) {
                    ThemeCenter.setTrialNeverExpired()
                }
            }

""",
    """            Package.THEME_CENTER -> {
                if (preference.other.setThemeTrialNeverExpired) {
                    ThemeCenter.setTrialNeverExpired()
                }
            }

            Package.THEME_STORE -> {
                // Read the region preference live so a change can apply without reloading LSPosed.
                ThemeStoreRegion.hook(preference.other)
            }

""",
    "Theme Store hook dispatch",
)
write(p, s)

# 5) Add Theme Store to the supported LSPosed scope list.
p = "app/src/main/res/values/statusbar_xposed.xml"
s = read(p)
s = replace_once(
    s,
    "        <item>com.samsung.android.themecenter</item>\n",
    "        <item>com.samsung.android.themecenter</item>\n        <item>com.samsung.android.themestore</item>\n",
    "Theme Store LSPosed scope",
)
write(p, s)

# 6) Add a separate category in the second navigation page's category grid.
p = "app/src/main/java/com/alohaoneui/app/alohaoneui/AlohaOneUIFeatureCatalog.kt"
s = read(p)
s = replace_once(
    s,
    '    ThemeCenter(Package.THEME_CENTER, "Galaxy Themes"),\n',
    '    ThemeCenter(Package.THEME_CENTER, "Galaxy Themes"),\n    ThemeStoreService(Package.THEME_STORE, "主题商店服务"),\n',
    "Theme Store category enum",
)
s = replace_once(
    s,
    """        AlohaOneUICategory.ThemeCenter to toggles(
            "Galaxy Themes",
            "other.setThemeTrialNeverExpired",
        ),
""",
    """        AlohaOneUICategory.ThemeCenter to toggles(
            "Galaxy Themes",
            "other.setThemeTrialNeverExpired",
        ),
        AlohaOneUICategory.ThemeStoreService to toggles(
            "Theme Store Service",
            "other.setThemeStoreRegion",
        ),
""",
    "Theme Store feature count",
)
write(p, s)

# 7) Keep the requested category label instead of replacing it with the installed app label.
p = "app/src/main/java/com/alohaoneui/app/ui/screen/tools/AlohaOneUIUiResources.kt"
s = read(p)
s = replace_once(
    s,
    """    return remember(category, context.resources.configuration.locales) {
        val packageManager = context.packageManager
""",
    """    return remember(category, context.resources.configuration.locales) {
        if (category == AlohaOneUICategory.ThemeStoreService) {
            return@remember resolveString(context, listOf("alohaoneui_category_themestoreservice"))
                ?: category.fallbackTitle
        }
        val packageManager = context.packageManager
""",
    "Theme Store fixed category title",
)
write(p, s)

# 8) Route the category to a separate page; keep Galaxy Themes' trial setting untouched.
p = "app/src/main/java/com/alohaoneui/app/ui/screen/tools/AlohaOneUICategoryScreen.kt"
s = read(p)
s = replace_once(
    s,
    "import com.alohaoneui.mod.ui.category.DetailPaneThemeCenter\n",
    "import com.alohaoneui.mod.ui.category.DetailPaneThemeCenter\nimport com.alohaoneui.mod.ui.category.DetailPaneThemeStoreService\n",
    "Theme Store page import",
)
s = replace_once(
    s,
    "import com.alohaoneui.mod.ui.category.onThemeCenterEvent\n",
    "import com.alohaoneui.mod.ui.category.onThemeCenterEvent\nimport com.alohaoneui.mod.ui.category.onThemeStoreServiceEvent\n",
    "Theme Store event import",
)
s = replace_once(
    s,
    "            AlohaOneUICategory.ThemeCenter -> DetailPaneThemeCenter(preference.other, toolsViewModel::onThemeCenterEvent, modifier)\n",
    "            AlohaOneUICategory.ThemeCenter -> DetailPaneThemeCenter(preference.other, toolsViewModel::onThemeCenterEvent, modifier)\n            AlohaOneUICategory.ThemeStoreService -> DetailPaneThemeStoreService(preference.other, toolsViewModel::onThemeStoreServiceEvent, modifier)\n",
    "Theme Store category route",
)
write(p, s)

# 9) Localized strings. Preserve all existing text and add only the strings used by this feature.
p = "app/src/main/res/values/alohaoneui_strings.xml"
s = read(p)
s = replace_once(
    s,
    '    <string name="alohaoneui_setThemeTrialNeverExpired_title">Never expire theme and icon trials</string>\n',
    '    <string name="alohaoneui_setThemeTrialNeverExpired_title">Never expire theme and icon trials</string>\n'
    '    <string name="alohaoneui_category_themestoreservice">Theme Store Service</string>\n'
    '    <string name="alohaoneui_setThemeStoreRegion_title">Change Theme Store region</string>\n'
    '    <string name="alohaoneui_setThemeStoreRegion_summary">Spoof country, CSC, locale and MCC/MNC. Reopen Theme Store to see the new region.</string>\n'
    '    <string name="alohaoneui_themeStoreRegion_current">Current region: %1$s</string>\n',
    "English Theme Store strings",
)
write(p, s)

p = "app/src/main/res/values-zh-rCN/alohaoneui_strings.xml"
s = read(p)
s = replace_once(
    s,
    '    <string name="alohaoneui_setThemeTrialNeverExpired_title">主题和图标试用永不过期</string>\n',
    '    <string name="alohaoneui_setThemeTrialNeverExpired_title">主题和图标试用永不过期</string>\n'
    '    <string name="alohaoneui_category_themestoreservice">主题商店服务</string>\n'
    '    <string name="alohaoneui_setThemeStoreRegion_title">切换主题商店地区</string>\n'
    '    <string name="alohaoneui_setThemeStoreRegion_summary">伪装国家、CSC、语言与 MCC/MNC。切换后重新打开主题商店即可。</string>\n'
    '    <string name="alohaoneui_themeStoreRegion_current">当前地区：%1$s</string>\n',
    "Chinese Theme Store strings",
)
write(p, s)

# 10) Native Aloha OneUI page: SwitchItem + SelectItem use the project's Material/Miuix adapters.
write("app/src/main/java/com/alohaoneui/mod/ui/category/DetailPaneThemeStoreService.kt", r'''package com.alohaoneui.mod.ui.category

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.viewmodel.AlohaOneUIToolsViewModel
import com.alohaoneui.common.ModuleActions
import com.alohaoneui.common.Package
import com.alohaoneui.common.Preference
import com.alohaoneui.mod.ui.component.SelectItem
import com.alohaoneui.mod.ui.component.SwitchItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DetailPaneThemeStoreService(
    uiState: Preference.Other,
    onEvent: (ThemeStoreServiceEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedIndex = THEME_STORE_REGION_PRESETS.indexOfFirst {
        it.matches(uiState.themeStoreRegionCountry, uiState.themeStoreRegionSalesCode)
    }.coerceAtLeast(0)

    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.language_us),
            title = stringResource(id = R.string.alohaoneui_setThemeStoreRegion_title),
            summary = stringResource(id = R.string.alohaoneui_setThemeStoreRegion_summary),
            checked = uiState.setThemeStoreRegion,
            onCheckedChange = { onEvent(ThemeStoreServiceEvent.SetEnabled(it)) },
        )
        if (uiState.setThemeStoreRegion) {
            SelectItem(
                title = stringResource(
                    id = R.string.alohaoneui_themeStoreRegion_current,
                    THEME_STORE_REGION_PRESETS[selectedIndex].label,
                ),
                icon = ImageVector.vectorResource(id = R.drawable.language_us),
                entries = THEME_STORE_REGION_PRESETS.map { it.label },
                selectedIndex = selectedIndex,
                onSelectedIndexChange = { index ->
                    val preset = THEME_STORE_REGION_PRESETS[index]
                    onEvent(
                        ThemeStoreServiceEvent.SelectRegion(
                            country = preset.country,
                            salesCode = preset.salesCode,
                            locale = preset.locale,
                            mcc = preset.mcc,
                            mnc = preset.mnc,
                        ),
                    )
                    scope.launch {
                        delay(REGION_APPLY_FLUSH_DELAY_MS)
                        runCatching {
                            context.sendBroadcast(
                                Intent(ModuleActions.THEME_STORE_REGION_CHANGED)
                                    .setPackage(Package.THEME_STORE),
                            )
                        }
                    }
                },
            )
        }
    }
}

private const val REGION_APPLY_FLUSH_DELAY_MS = 700L

private data class ThemeStoreRegionPreset(
    val label: String,
    val country: String,
    val salesCode: String,
    val locale: String,
    val mcc: String,
    val mnc: String,
) {
    fun matches(country: String, salesCode: String): Boolean =
        this.country.equals(country.trim(), ignoreCase = true) &&
            this.salesCode.equals(salesCode.trim(), ignoreCase = true)
}

private val THEME_STORE_REGION_PRESETS = listOf(
    ThemeStoreRegionPreset("韩国", "KR", "KOO", "ko-KR", "450", "08"),
    ThemeStoreRegionPreset("中国香港", "HK", "TGY", "zh-HK", "454", "00"),
    ThemeStoreRegionPreset("日本", "JP", "XJP", "ja-JP", "440", "10"),
    ThemeStoreRegionPreset("中国台湾", "TW", "BRI", "zh-TW", "466", "92"),
    ThemeStoreRegionPreset("新加坡", "SG", "XSP", "en-SG", "525", "01"),
    ThemeStoreRegionPreset("美国", "US", "XAA", "en-US", "310", "260"),
    ThemeStoreRegionPreset("英国", "GB", "BTU", "en-GB", "234", "15"),
    ThemeStoreRegionPreset("德国", "DE", "DBT", "de-DE", "262", "01"),
    ThemeStoreRegionPreset("法国", "FR", "XEF", "fr-FR", "208", "01"),
    ThemeStoreRegionPreset("意大利", "IT", "ITV", "it-IT", "222", "01"),
    ThemeStoreRegionPreset("西班牙", "ES", "PHE", "es-ES", "214", "07"),
    ThemeStoreRegionPreset("葡萄牙", "PT", "TPH", "pt-PT", "268", "01"),
    ThemeStoreRegionPreset("荷兰", "NL", "PHN", "nl-NL", "204", "04"),
    ThemeStoreRegionPreset("波兰", "PL", "XEO", "pl-PL", "260", "02"),
    ThemeStoreRegionPreset("澳大利亚", "AU", "XSA", "en-AU", "505", "01"),
    ThemeStoreRegionPreset("加拿大", "CA", "XAC", "en-CA", "302", "720"),
    ThemeStoreRegionPreset("印度", "IN", "INS", "en-IN", "404", "45"),
    ThemeStoreRegionPreset("泰国", "TH", "THL", "th-TH", "520", "00"),
    ThemeStoreRegionPreset("越南", "VN", "XXV", "vi-VN", "452", "04"),
    ThemeStoreRegionPreset("马来西亚", "MY", "XME", "ms-MY", "502", "12"),
    ThemeStoreRegionPreset("菲律宾", "PH", "XTC", "en-PH", "515", "02"),
    ThemeStoreRegionPreset("印尼", "ID", "XID", "id-ID", "510", "10"),
    ThemeStoreRegionPreset("阿联酋", "AE", "XSG", "ar-AE", "424", "03"),
    ThemeStoreRegionPreset("土耳其", "TR", "TUR", "tr-TR", "286", "01"),
    ThemeStoreRegionPreset("巴西", "BR", "ZTO", "pt-BR", "724", "05"),
)

sealed interface ThemeStoreServiceEvent {
    @JvmInline
    value class SetEnabled(val value: Boolean) : ThemeStoreServiceEvent

    data class SelectRegion(
        val country: String,
        val salesCode: String,
        val locale: String,
        val mcc: String,
        val mnc: String,
    ) : ThemeStoreServiceEvent
}

fun AlohaOneUIToolsViewModel.onThemeStoreServiceEvent(event: ThemeStoreServiceEvent) {
    updateData { preference ->
        when (event) {
            is ThemeStoreServiceEvent.SetEnabled -> preference.copy(
                other = preference.other.copy(setThemeStoreRegion = event.value),
            )
            is ThemeStoreServiceEvent.SelectRegion -> preference.copy(
                other = preference.other.copy(
                    setThemeStoreRegion = true,
                    themeStoreRegionCountry = event.country,
                    themeStoreRegionSalesCode = event.salesCode,
                    themeStoreRegionLocale = event.locale,
                    themeStoreRegionMcc = event.mcc,
                    themeStoreRegionMnc = event.mnc,
                ),
            )
        }
    }
}
''')

# Verify the new category is routed and all resource references exist before cleanup/commit.
catalog = read("app/src/main/java/com/alohaoneui/app/alohaoneui/AlohaOneUIFeatureCatalog.kt")
screen = read("app/src/main/java/com/alohaoneui/app/ui/screen/tools/AlohaOneUICategoryScreen.kt")
hook = read("hook/src/main/java/com/alohaoneui/hook/Main.kt")
assert "ThemeStoreService(Package.THEME_STORE" in catalog
assert "AlohaOneUICategory.ThemeStoreService to toggles" in catalog
assert "AlohaOneUICategory.ThemeStoreService -> DetailPaneThemeStoreService" in screen
assert "ThemeStoreRegion.hook(preference.other)" in hook
assert "com.samsung.android.themestore" in read("app/src/main/res/values/statusbar_xposed.xml")
print("Theme Store Service source integration patch applied; all anchor checks passed.")
