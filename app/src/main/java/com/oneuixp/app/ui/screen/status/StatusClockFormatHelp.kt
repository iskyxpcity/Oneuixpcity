package com.oneuixp.app.ui.screen.status

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.oneuixp.app.R
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import io.github.oneuixp.hook.util.StatusBarClockFormatter
import io.github.oneuixp.ui.component.OneuiXPActionButton
import java.time.LocalDateTime
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

data class StatusClockFormatPreset(
    val labelRes: Int,
    val token: String,
)

val STATUS_CLOCK_FORMAT_PRESETS = listOf(
    StatusClockFormatPreset(R.string.oneuixp_clockFormatPreset_period, "CNPERIOD"),
    StatusClockFormatPreset(R.string.oneuixp_clockFormatPreset_hour24, "HH"),
    StatusClockFormatPreset(R.string.oneuixp_clockFormatPreset_hour12, "hh"),
    StatusClockFormatPreset(R.string.oneuixp_clockFormatPreset_minute, "mm"),
    StatusClockFormatPreset(R.string.oneuixp_clockFormatPreset_second, "ss"),
    StatusClockFormatPreset(R.string.oneuixp_clockFormatPreset_month, "MM"),
    StatusClockFormatPreset(R.string.oneuixp_clockFormatPreset_day, "dd"),
    StatusClockFormatPreset(R.string.oneuixp_clockFormatPreset_weekday, "EEE"),
    StatusClockFormatPreset(R.string.oneuixp_clockFormatPreset_space, " "),
    StatusClockFormatPreset(R.string.oneuixp_clockFormatPreset_newLine, "\n"),
    StatusClockFormatPreset(R.string.oneuixp_clockFormatPreset_lunar, "CNLUNAR"),
    StatusClockFormatPreset(R.string.oneuixp_clockFormatPreset_traditionalTime, "CNTIME"),
    StatusClockFormatPreset(R.string.oneuixp_clockFormatPreset_lunarYear, "CNYEAR"),
    StatusClockFormatPreset(R.string.oneuixp_clockFormatPreset_zodiac, "CNZODIAC"),
    StatusClockFormatPreset(R.string.oneuixp_clockFormatPreset_season, "CNSEASON"),
)

@Composable
private fun ClockFormatHelpText(
    text: String,
    modifier: Modifier = Modifier,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> MaterialText(
            text = text,
            modifier = modifier,
            style = MaterialTheme.typography.bodyMedium,
        )
        UiMode.Miuix -> MiuixText(
            text = text,
            modifier = modifier,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
        )
    }
}

@Composable
private fun ClockFormatHelpContent(
    onDismiss: () -> Unit,
) {
    val now = LocalDateTime.now()
    val examples = listOf(
        "HH:mm",
        "CNPERIOD HH:mm",
        "HH:mm\nMM月dd日 EEE",
        "CNPERIOD hh:mm\nCNLUNAR EEE",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        ClockFormatHelpText(
            text = stringResource(id = R.string.oneuixp_clockFormatHelp_intro),
            modifier = Modifier.padding(bottom = 10.dp),
        )

        listOf(
            "HH" to R.string.oneuixp_clockFormatHelp_hour24,
            "hh" to R.string.oneuixp_clockFormatHelp_hour12,
            "mm" to R.string.oneuixp_clockFormatHelp_minute,
            "ss" to R.string.oneuixp_clockFormatHelp_second,
            "yyyy" to R.string.oneuixp_clockFormatHelp_year,
            "MM" to R.string.oneuixp_clockFormatHelp_month,
            "dd" to R.string.oneuixp_clockFormatHelp_day,
            "EEE" to R.string.oneuixp_clockFormatHelp_weekday,
            "CNPERIOD" to R.string.oneuixp_clockFormatHelp_period,
            "CNLUNAR" to R.string.oneuixp_clockFormatHelp_lunar,
            "CNTIME" to R.string.oneuixp_clockFormatHelp_traditionalTime,
            "CNYEAR" to R.string.oneuixp_clockFormatHelp_lunarYear,
            "CNZODIAC" to R.string.oneuixp_clockFormatHelp_zodiac,
            "CNSEASON" to R.string.oneuixp_clockFormatHelp_season,
        ).forEach { (token, descriptionRes) ->
            ClockFormatHelpText(
                text = "$token  ·  " + stringResource(id = descriptionRes),
                modifier = Modifier.padding(vertical = 3.dp),
            )
        }

        ClockFormatHelpText(
            text = stringResource(id = R.string.oneuixp_clockFormatHelp_examples),
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
        examples.forEach { pattern ->
            val preview = runCatching {
                StatusBarClockFormatter.format(pattern, now).replace("\n", " / ")
            }.getOrElse { "—" }
            ClockFormatHelpText(
                text = "$pattern  →  $preview",
                modifier = Modifier.padding(vertical = 3.dp),
            )
        }

        OneuiXPActionButton(
            text = stringResource(id = android.R.string.ok),
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            primary = true,
        )
    }
}

@Composable
fun StatusClockFormatHelpDialog(
    onDismiss: () -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column {
                    MaterialText(
                        text = stringResource(id = R.string.oneuixp_clockFormatHelp_title),
                        modifier = Modifier.padding(
                            start = 24.dp,
                            end = 24.dp,
                            top = 22.dp,
                            bottom = 4.dp,
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    ClockFormatHelpContent(onDismiss)
                }
            }
        }

        UiMode.Miuix -> WindowDialog(
            show = true,
            title = stringResource(id = R.string.oneuixp_clockFormatHelp_title),
            onDismissRequest = onDismiss,
            content = { ClockFormatHelpContent(onDismiss) },
        )
    }
}
