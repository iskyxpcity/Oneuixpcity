package com.alohaoneui.app.ui.screen.colorpalette

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
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
import com.materialkolor.hct.Hct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ThemeSchemeEditorScreen(index: Int) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val existing = uiState.materialColorSchemes.getOrNull(index)
    var name by remember(index, existing?.name) { mutableStateOf(existing?.name.orEmpty()) }
    var keyColor by remember(index, existing?.keyColor, uiState.keyColor) { mutableIntStateOf(existing?.keyColor ?: uiState.keyColor) }
    var colorStyle by remember(index, existing?.colorStyle, uiState.colorStyle) { mutableStateOf(existing?.colorStyle ?: uiState.colorStyle) }
    var contrast by remember(index, existing?.contrastLevel, uiState.materialContrastLevel) { mutableFloatStateOf(existing?.contrastLevel ?: uiState.materialContrastLevel) }
    var hex by remember(keyColor) { mutableStateOf(if (keyColor == 0) "" else (keyColor and 0x00FFFFFF).toString(16).uppercase().padStart(6, '0')) }
    val hctBase = if (keyColor == 0) MaterialTheme.colorScheme.primary.toArgb() else keyColor
    val initialHct = remember(index, hctBase) { Hct.fromInt(hctBase) }
    var hue by remember(index, hctBase) { mutableFloatStateOf(initialHct.hue.toFloat()) }
    var chroma by remember(index, hctBase) { mutableFloatStateOf(initialHct.chroma.toFloat()) }
    var tone by remember(index, hctBase) { mutableFloatStateOf(initialHct.tone.toFloat()) }
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
    val sliderSeedColor = Hct.from(hue = hue.toDouble(), chroma = chroma.toDouble(), tone = tone.toDouble()).toInt()
    val previewColor = if (selectedTab == 1 && hex.isBlank()) sliderSeedColor else parsedHex ?: keyColor.takeIf { it != 0 } ?: MaterialTheme.colorScheme.primary.toArgb()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val extracted = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }?.let { bitmap ->
                        try {
                            val stepX = (bitmap.width / 32).coerceAtLeast(1)
                            val stepY = (bitmap.height / 32).coerceAtLeast(1)
                            var red = 0L; var green = 0L; var blue = 0L; var count = 0L
                            for (y in 0 until bitmap.height step stepY) for (x in 0 until bitmap.width step stepX) {
                                val pixel = bitmap.getPixel(x, y)
                                if (((pixel ushr 24) and 0xFF) >= 128) {
                                    red += (pixel ushr 16) and 0xFF
                                    green += (pixel ushr 8) and 0xFF
                                    blue += pixel and 0xFF
                                    count++
                                }
                            }
                            if (count == 0L) null else (0xFF shl 24) or ((red / count).toInt() shl 16) or ((green / count).toInt() shl 8) or (blue / count).toInt()
                        } finally { bitmap.recycle() }
                    }
                }.getOrNull()
            }
            if (extracted != null) {
                keyColor = extracted
                hex = (extracted and 0x00FFFFFF).toString(16).uppercase().padStart(6, '0')
            }
        }
    }

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
            Surface(modifier = Modifier.fillMaxWidth().height(72.dp), shape = MaterialTheme.shapes.large, color = Color(previewColor)) {}
            TabRow(selectedTabIndex = selectedTab) {
                listOf(stringResource(R.string.theme_picker_palette), stringResource(R.string.theme_picker_sliders), stringResource(R.string.theme_picker_spectrum), stringResource(R.string.theme_picker_image))
                    .forEachIndexed { i, label -> Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(label) }) }
            }
            when (selectedTab) {
                0 -> {
                    Text(stringResource(R.string.theme_picker_palette), style = MaterialTheme.typography.titleMedium)
                    keyColorOptions.chunked(4).forEach { rowColors ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            rowColors.forEach { color ->
                                Surface(
                                    modifier = Modifier.weight(1f).height(52.dp).clickable {
                                        keyColor = color
                                        hex = (color and 0x00FFFFFF).toString(16).uppercase().padStart(6, '0')
                                    },
                                    shape = CircleShape,
                                    color = Color(color),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (keyColor == color) 3.dp else 1.dp,
                                        color = if (keyColor == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    ),
                                ) {}
                            }
                            repeat(4 - rowColors.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Button(onClick = { keyColor = 0; hex = "" }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.theme_editor_use_current)) }
                }
                1 -> {
                    Text(stringResource(R.string.theme_editor_hue), style = MaterialTheme.typography.titleMedium)
                    Text("${hue.toInt()}°")
                    Slider(value = hue, onValueChange = { hue = it; hex = "" }, valueRange = 0f..360f)
                    Text(stringResource(R.string.theme_editor_chroma), style = MaterialTheme.typography.titleMedium)
                    Text(chroma.toInt().toString())
                    Slider(value = chroma, onValueChange = { chroma = it; hex = "" }, valueRange = 0f..120f)
                    Text(stringResource(R.string.theme_editor_tone), style = MaterialTheme.typography.titleMedium)
                    Text(tone.toInt().toString())
                    Slider(value = tone, onValueChange = { tone = it; hex = "" }, valueRange = 0f..100f)
                }
                2 -> {
                    Text(stringResource(R.string.theme_picker_spectrum), style = MaterialTheme.typography.titleMedium)
                    (listOf(20.0, 40.0, 60.0, 80.0, 100.0)).forEach { selectedChroma ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            (0..11).forEach { step ->
                                val color = Hct.from(hue = step * 30.0, chroma = selectedChroma, tone = 55.0).toInt()
                                Surface(
                                    modifier = Modifier.weight(1f).height(34.dp).clickable {
                                        keyColor = color
                                        hex = (color and 0x00FFFFFF).toString(16).uppercase().padStart(6, '0')
                                    },
                                    shape = MaterialTheme.shapes.small,
                                    color = Color(color),
                                    border = if (keyColor == color) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null,
                                ) {}
                            }
                        }
                    }
                }
                else -> {
                    Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp).align(Alignment.CenterHorizontally))
                    Text(stringResource(R.string.theme_picker_choose_image), style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.theme_picker_choose_image))
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
                    if (existing != null && !existing.name.equals(name.trim(), ignoreCase = true)) {
                        viewModel.deleteMaterialColorScheme(existing.name)
                    }
                    viewModel.saveMaterialColorScheme(
                        MaterialColorScheme(
                            name = name,
                            keyColor = if (selectedTab == 1 && hex.isBlank()) sliderSeedColor else parsedHex ?: keyColor,
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
