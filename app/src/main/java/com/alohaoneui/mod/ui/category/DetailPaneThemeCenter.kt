package com.alohaoneui.mod.ui.category

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.viewmodel.AlohaOneUIToolsViewModel
import com.alohaoneui.common.ModuleActions
import com.alohaoneui.common.Package
import com.alohaoneui.common.Preference
import com.alohaoneui.mod.ui.component.SwitchItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun DetailPaneThemeCenter(
    uiState: Preference.Other,
    onEvent: (ThemeCenterEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.format_paint),
            title = stringResource(id = R.string.alohaoneui_setThemeTrialNeverExpired_title),
            checked = uiState.setThemeTrialNeverExpired,
            onCheckedChange = { onEvent(ThemeCenterEvent.SetThemeTrialNeverExpired(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.language_us),
            title = stringResource(id = R.string.alohaoneui_setThemeStoreRegion_title),
            summary = stringResource(id = R.string.alohaoneui_setThemeStoreRegion_summary),
            checked = uiState.setThemeStoreRegion,
            onCheckedChange = { onEvent(ThemeCenterEvent.SetThemeStoreRegion(it)) }
        )
        if (uiState.setThemeStoreRegion) {
            ThemeStoreRegionSelector(
                selectedCountry = uiState.themeStoreRegionCountry,
                selectedSalesCode = uiState.themeStoreRegionSalesCode,
                onSelect = { event ->
                    onEvent(event)
                    scope.launch {
                        // The Store keeps its resolved region for the whole process lifetime, so it
                        // has to end itself. It does that on its own after this broadcast — no root
                        // shell, no force-stop. The delay lets the DataStore write and its remote
                        // preference flush land first, so the restarted Store reads the new region.
                        delay(REGION_APPLY_FLUSH_DELAY_MS)
                        runCatching {
                            context.sendBroadcast(
                                Intent(ModuleActions.THEME_STORE_REGION_CHANGED)
                                    .setPackage(Package.THEME_STORE)
                            )
                        }
                    }
                }
            )
        }
    }
}

/** Time for the DataStore write and its remote preference flush to reach the hooked process. */
private const val REGION_APPLY_FLUSH_DELAY_MS = 700L

/**
 * Region grid, ported from LaobaiAppFreezerModule's `ThemeStoreRegionActivity`:
 * wrap-around chips, the selected chip filled with the accent colour.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeStoreRegionSelector(
    selectedCountry: String,
    selectedSalesCode: String,
    onSelect: (ThemeCenterEvent.SelectThemeStoreRegion) -> Unit
) {
    val current = THEME_STORE_REGION_PRESETS.firstOrNull { it.matches(selectedCountry, selectedSalesCode) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        when (LocalUiMode.current) {
            UiMode.Material -> Text(
                text = stringResource(
                    id = R.string.alohaoneui_themeStoreRegion_current,
                    current?.label ?: selectedCountry
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            UiMode.Miuix -> MiuixText(
                text = stringResource(
                    id = R.string.alohaoneui_themeStoreRegion_current,
                    current?.label ?: selectedCountry
                ),
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            THEME_STORE_REGION_PRESETS.forEach { preset ->
                ThemeStoreRegionChip(
                    label = preset.label,
                    selected = preset.matches(selectedCountry, selectedSalesCode),
                    onClick = {
                        onSelect(
                            ThemeCenterEvent.SelectThemeStoreRegion(
                                country = preset.country,
                                salesCode = preset.salesCode,
                                locale = preset.locale,
                                mcc = preset.mcc,
                                mnc = preset.mnc
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeStoreRegionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    when (LocalUiMode.current) {
        UiMode.Material -> {
            val scheme = MaterialTheme.colorScheme
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(
                        if (selected) scheme.primary else scheme.surfaceVariant.copy(alpha = 0.45f),
                        shape
                    )
                    .border(1.dp, if (selected) scheme.primary else scheme.outlineVariant, shape)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    text = label,
                    color = if (selected) scheme.onPrimary else scheme.onSurface,
                    fontSize = 13.5.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
        UiMode.Miuix -> Box(
            modifier = Modifier
                .clip(shape)
                .background(
                    if (selected) colorScheme.primary else colorScheme.surface,
                    shape
                )
                .border(1.dp, if (selected) colorScheme.primary else colorScheme.onSurfaceVariantSummary, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            MiuixText(
                text = label,
                color = if (selected) Color.White else colorScheme.onSurface,
                fontSize = 13.5.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

private data class ThemeStoreRegionPreset(
    val label: String,
    val country: String,
    val salesCode: String,
    val locale: String,
    val mcc: String,
    val mnc: String
) {
    fun matches(country: String, salesCode: String): Boolean =
        this.country.equals(country.trim(), ignoreCase = true) &&
            this.salesCode.equals(salesCode.trim(), ignoreCase = true)
}

/** Same preset table as LaobaiAppFreezerModule's ThemeStoreRegionActivity. */
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
    ThemeStoreRegionPreset("巴西", "BR", "ZTO", "pt-BR", "724", "05")
)

sealed interface ThemeCenterEvent {
    @JvmInline
    value class SetThemeTrialNeverExpired(val value: Boolean) : ThemeCenterEvent

    @JvmInline
    value class SetThemeStoreRegion(val value: Boolean) : ThemeCenterEvent

    /** All five region fields change together: the store needs a consistent combination. */
    data class SelectThemeStoreRegion(
        val country: String,
        val salesCode: String,
        val locale: String,
        val mcc: String,
        val mnc: String
    ) : ThemeCenterEvent
}

fun AlohaOneUIToolsViewModel.onThemeCenterEvent(event: ThemeCenterEvent) {
    updateData { preference ->
        when (event) {
            is ThemeCenterEvent.SetThemeTrialNeverExpired -> preference.copy(
                other = preference.other.copy(
                    setThemeTrialNeverExpired = event.value
                )
            )

            is ThemeCenterEvent.SetThemeStoreRegion -> preference.copy(
                other = preference.other.copy(
                    setThemeStoreRegion = event.value
                )
            )

            is ThemeCenterEvent.SelectThemeStoreRegion -> preference.copy(
                other = preference.other.copy(
                    setThemeStoreRegion = true,
                    themeStoreRegionCountry = event.country,
                    themeStoreRegionSalesCode = event.salesCode,
                    themeStoreRegionLocale = event.locale,
                    themeStoreRegionMcc = event.mcc,
                    themeStoreRegionMnc = event.mnc
                )
            )
        }
    }
}
