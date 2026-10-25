package com.alohaoneui.app.ui.screen.colorpalette

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alohaoneui.app.R
import com.alohaoneui.app.data.repository.MaterialColorScheme
import com.alohaoneui.app.ui.navigation3.LocalNavigator
import com.alohaoneui.app.ui.theme.keyColorOptions
import com.alohaoneui.app.ui.viewmodel.SettingsViewModel
import com.materialkolor.PaletteStyle

@Composable
fun ThemeSchemeEditorScreen(index: Int) {
    val navigator = LocalNavigator.current
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val existing = uiState.materialColorSchemes.getOrNull(index)
    var name by remember(index, existing?.name) { mutableStateOf(existing?.name.orEmpty()) }
    var keyColor by remember(index, existing?.keyColor, uiState.keyColor) { mutableIntStateOf(existing?.keyColor ?: uiState.keyColor) }
    var colorStyle by remember(index, existing?.colorStyle, uiState.colorStyle) { mutableStateOf(existing?.colorStyle ?: uiState.colorStyle) }
    var contrast by remember(index, existing?.contrastLevel, uiState.materialContrastLevel) { mutableFloatStateOf(existing?.contrastLevel ?: uiState.materialContrastLevel) }
    var hex by remember(keyColor) { mutableStateOf(if (keyColor == 0) "" else (keyColor and 0x00FFFFFF).toString(16).uppercase().padStart(6, '0')) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var expandedStyle by remember { mutableStateOf(false) }
    val styles = listOf(PaletteStyle.Expressive, PaletteStyle.TonalSpot, PaletteStyle.Neutral, PaletteStyle.Vibrant, PaletteStyle.Rainbow, PaletteStyle.FruitSalad, PaletteStyle.Monochrome, PaletteStyle.Fidelity, PaletteStyle.Content)
    val styleLabels = styles.map { style ->
        stringResource(id = when (style) {
            PaletteStyle.Content -> R.string.settings_palette_style_content
            PaletteStyle.Expressive -> R.string.settings_palette_style_expressive
            PaletteStyle.Fidelity -> R.string.settings_palette_style_fidelity
            PaletteStyle.FruitSalad -> R.string.settings_palette_style_fruit_salad
            PaletteStyle.Monochrome -> R.string.settings_palette_style_monochrome
            PaletteStyle.Neutral -> R.string.settings_palette_style_neutral
            PaletteStyle.Rainbow -> R.string.settings_palette_style_rainbow
            PaletteStyle.TonalSpot -> R.string.settings_palette_style_tonal_spot
            PaletteStyle.Vibrant -> R.string.settings_palette_style_vibrant
        })
    }
    val parsedHex = hex.trim().removePrefix("#").takeIf { it.length == 6 && it.all { c -> c.digitToIntOrNull(16) != null } }?.toLongOrNull(16)?.toInt()?.or(0xFF000000.toInt())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (index >= 0) R.string.theme_scheme_edit else R.string.theme_scheme_add)) },
                navigationIcon = { IconButton(onClick = { navigator.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.theme_saved_scheme_name)) }, singleLine = true)
            Box {
                OutlinedTextField(
                    value = styleLabels.getOrElse(styles.indexOfFirst { it.name == colorStyle }.coerceAtLeast(0)) { styleLabels.first() },
                    onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().clickable { expandedStyle = true },
                    label = { Text(stringResource(R.string.settings_color_style)) },
                )
                DropdownMenu(expanded = expandedStyle, onDismissRequest = { expandedStyle = false }) {
                    styles.forEachIndexed { i, style -> DropdownMenuItem(text = { Text(styleLabels[i]) }, onClick = { colorStyle = style.name; expandedStyle = false }) }
                }
            }
            TabRow(selectedTabIndex = selectedTab) {
                listOf(stringResource(R.string.theme_picker_palette), stringResource(R.string.theme_picker_sliders), stringResource(R.string.theme_picker_spectrum))
                    .forEachIndexed { i, label -> Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(label) }) }
            }
            when (selectedTab) {
                0 -> {
                    Text(stringResource(R.string.theme_picker_palette), style = MaterialTheme.typography.titleMedium)
                    keyColorOptions.chunked(4).forEach { rowColors ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            rowColors.forEach { color ->
                                Button(onClick = { keyColor = color; hex = (color and 0x00FFFFFF).toString(16).uppercase().padStart(6, '0') }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Filled.Palette, contentDescription = null, tint = Color(color))
                                }
                            }
                            repeat(4 - rowColors.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Button(onClick = { keyColor = 0; hex = "" }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.theme_editor_use_current)) }
                }
                1 -> {
                    Text(stringResource(R.string.theme_editor_hue), style = MaterialTheme.typography.titleMedium)
                    var hue by remember(keyColor) { mutableFloatStateOf(0f) }
                    Text("${hue.toInt()}°")
                    Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
                    Text(stringResource(R.string.theme_editor_chroma), style = MaterialTheme.typography.titleMedium)
                    var chroma by remember(keyColor) { mutableFloatStateOf(48f) }
                    Text(chroma.toInt().toString())
                    Slider(value = chroma, onValueChange = { chroma = it }, valueRange = 0f..120f)
                    Text(stringResource(R.string.theme_editor_tone), style = MaterialTheme.typography.titleMedium)
                    var tone by remember(keyColor) { mutableFloatStateOf(50f) }
                    Text(tone.toInt().toString())
                    Slider(value = tone, onValueChange = { tone = it }, valueRange = 0f..100f)
                }
                else -> {
                    Text(stringResource(R.string.theme_picker_spectrum), style = MaterialTheme.typography.titleMedium)
                    keyColorOptions.forEach { color ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { keyColor = color; hex = (color and 0x00FFFFFF).toString(16).uppercase().padStart(6, '0') }) { Icon(Icons.Filled.Tune, contentDescription = null, tint = Color(color)) }
                            Text("#" + (color and 0x00FFFFFF).toString(16).uppercase().padStart(6, '0'))
                        }
                    }
                }
            }
            OutlinedTextField(value = hex, onValueChange = { hex = it.take(7) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.theme_editor_hex)) }, isError = hex.isNotBlank() && parsedHex == null, singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_material_contrast), modifier = Modifier.weight(1f))
                Text(String.format(java.util.Locale.ROOT, "%+.1f", contrast))
            }
            Slider(value = contrast, onValueChange = { contrast = it }, valueRange = -1f..1f)
            Button(
                onClick = {
                    viewModel.saveMaterialColorScheme(
                        MaterialColorScheme(
                            name = name,
                            keyColor = parsedHex ?: keyColor,
                            colorStyle = colorStyle,
                            colorSpec = existing?.colorSpec ?: uiState.colorSpec,
                            contrastMode = existing?.contrastMode ?: uiState.materialContrastMode,
                            contrastLevel = contrast,
                        )
                    )
                    navigator.pop()
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.theme_action_save)) }
        }
    }
}
