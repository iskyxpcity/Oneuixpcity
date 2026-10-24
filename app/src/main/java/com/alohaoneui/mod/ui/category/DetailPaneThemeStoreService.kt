package com.alohaoneui.mod.ui.category

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
