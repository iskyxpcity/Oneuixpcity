package com.alohaoneui.hook.util

import com.alohaoneui.common.Preference
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

object ShadeClockFormatter {
    private val lunarMonths = arrayOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
    private val dayPrefixes = arrayOf("初", "十", "廿", "三")
    private val daySuffixes = arrayOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
    private val animals = arrayOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")
    private val stems = charArrayOf('甲', '乙', '丙', '丁', '戊', '己', '庚', '辛', '壬', '癸')
    private val branches = charArrayOf('子', '丑', '寅', '卯', '辰', '巳', '午', '未', '申', '酉', '戌', '亥')
    private val solarTerms = arrayOf(
        "小寒", "大寒", "立春", "雨水", "惊蛰", "春分", "清明", "谷雨",
        "立夏", "小满", "芒种", "夏至", "小暑", "大暑", "立秋", "处暑",
        "白露", "秋分", "寒露", "霜降", "立冬", "小雪", "大雪", "冬至",
    )
    private val solarTermInfo = intArrayOf(
        0, 21208, 42467, 63836, 85337, 107014, 128867, 150921,
        173149, 195551, 218072, 240693, 263343, 285989, 308563, 331033,
        353350, 375494, 397447, 419210, 440795, 462224, 483532, 504758,
    )

    fun format(
        config: Preference.SystemUI.StatusBar,
        dateTime: LocalDateTime = LocalDateTime.now(),
        showSeconds: Boolean = false,
    ): String {
        val timePattern = if (showSeconds) "HH:mm:ss" else "HH:mm"
        val time = if (config.shadeClockTimeEnabled) DateTimeFormatter.ofPattern(timePattern, Locale.getDefault()).format(dateTime) else ""
        if (!config.shadeClockCustomEnabled) return time
        val date = if (config.shadeClockDateEnabled) dateText(dateTime.toLocalDate(), config.shadeClockDateFormat) else ""
        val lunar = if (config.shadeClockLunarEnabled) lunarText(dateTime.toLocalDate(), config.shadeClockLunarFormat) else ""
        val week = if (config.shadeClockWeekEnabled) weekText(dateTime, config.shadeClockWeekFormat) else ""
        val detail = detailText(dateTime, config.shadeClockDetailStyle)
        val parts = listOf(
            time to config.shadeClockTimeLine,
            date to config.shadeClockDateLine,
            lunar to config.shadeClockLunarLine,
            week to config.shadeClockWeekLine,
            detail to config.shadeClockDetailLine,
        ).filter { it.first.isNotBlank() }
        val rendered = if (config.shadeClockLayoutStyle == 1) {
            val top = parts.filter { it.second == 0 }.joinToString(" ") { it.first }
            val bottom = parts.filter { it.second != 0 }.joinToString(" ") { it.first }
            when {
                top.isNotBlank() && bottom.isNotBlank() -> "$top\n$bottom"
                top.isNotBlank() -> top
                else -> bottom
            }
        } else parts.joinToString(" ") { it.first }
        return rendered.ifBlank { DateTimeFormatter.ofPattern(timePattern, Locale.getDefault()).format(dateTime) }
    }

    private fun dateText(date: LocalDate, format: Int): String {
        val pattern = when (format) { 1 -> "yyyy-MM-dd"; 2 -> "MM/dd"; else -> "M月d日" }
        return DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(date)
    }

    private fun weekText(dateTime: LocalDateTime, format: Int): String {
        val day = dateTime.dayOfWeek.value - 1
        return when (format) {
            1 -> arrayOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")[day]
            2 -> arrayOf("一", "二", "三", "四", "五", "六", "日")[day]
            else -> DateTimeFormatter.ofPattern("EEE", Locale.getDefault()).format(dateTime)
        }
    }

    private fun lunarText(date: LocalDate, format: Int): String {
        val info = TraditionalChineseCalendar.getLunarInfo(date) ?: return ""
        val lunar = (if (info.isLeapMonth) "闰" else "") + lunarMonths[info.month - 1] + "月" + lunarDay(info.day)
        val offset = info.year - 4
        val zodiacYear = animals[offset.floorMod(12)] + "年"
        val cyclicalYear = stems[offset.floorMod(10)].toString() + branches[offset.floorMod(12)] + "(" + animals[offset.floorMod(12)] + ")年"
        val specialParts = buildList {
            if (!info.isLeapMonth) lunarFestival(info.month, info.day, date)?.let(::add)
            solarFestival(date)?.let(::add)
            solarTerm(date)?.let(::add)
        }.distinct()
        val special = specialParts.joinToString(" ")
        return when (format) {
            1 -> join(zodiacYear, lunar, special)
            2 -> join(lunar, zodiacYear)
            3 -> join(cyclicalYear, lunar, special)
            4 -> lunar
            5 -> join(zodiacYear, lunar)
            6 -> join(cyclicalYear, lunar)
            else -> join(lunar, special)
        }
    }

    private fun lunarDay(day: Int): String = when (day) {
        10 -> "初十"; 20 -> "二十"; 30 -> "三十"
        in 1..30 -> dayPrefixes[(day - 1) / 10] + daySuffixes[(day - 1) % 10]
        else -> ""
    }

    private fun lunarFestival(month: Int, day: Int, date: LocalDate): String? {
        return when {
            month == 1 && day == 1 -> "春节"
            month == 1 && day == 15 -> "元宵"
            month == 2 && day == 2 -> "龙抬头"
            month == 5 && day == 5 -> "端午"
            month == 7 && day == 7 -> "七夕"
            month == 7 && day == 15 -> "中元"
            month == 8 && day == 15 -> "中秋"
            month == 9 && day == 9 -> "重阳"
            month == 12 && day == 8 -> "腊八"
            month == 12 && day == 23 -> "小年"
            month == 12 && TraditionalChineseCalendar.getLunarInfo(date.plusDays(1))?.let { it.month == 1 && it.day == 1 } == true -> "除夕"
            else -> null
        }
    }

    private fun solarFestival(date: LocalDate): String? = when (date.monthValue to date.dayOfMonth) {
        1 to 1 -> "元旦"; 2 to 14 -> "情人节"; 3 to 8 -> "妇女节"; 4 to 1 -> "愚人节"
        5 to 1 -> "劳动节"; 6 to 1 -> "儿童节"; 10 to 1 -> "国庆"
        12 to 24 -> "平安夜"; 12 to 25 -> "圣诞"
        else -> null
    }

    private fun solarTerm(date: LocalDate): String? {
        val firstIndex = (date.monthValue - 1) * 2
        val firstDay = solarTermDay(date.year, firstIndex)
        if (date.dayOfMonth == firstDay) return solarTerms[firstIndex]
        val secondDay = solarTermDay(date.year, firstIndex + 1)
        return if (date.dayOfMonth == secondDay) solarTerms[firstIndex + 1] else null
    }

    private fun solarTermDay(year: Int, index: Int): Int {
        val base = GregorianCalendar(1900, Calendar.JANUARY, 6, 2, 5, 0).timeInMillis
        val termMillis = 31_556_925_974.7 * (year - 1900) + solarTermInfo[index] * 60_000L
        val calendar = GregorianCalendar().apply { timeInMillis = base + termMillis.toLong() }
        return calendar.get(Calendar.DAY_OF_MONTH)
    }

    private fun detailText(dateTime: LocalDateTime, style: Int): String {
        val hour = dateTime.hour
        return when (style) {
            1 -> when (hour) {
                in 0..3 -> "凌晨"; in 4..5 -> "黎明"; 6 -> "拂晓"; 7 -> "早晨"
                in 8..10 -> "上午"; in 11..13 -> "中午"; in 14..15 -> "下午"
                in 16..17 -> "黄昏"; in 18..19 -> "傍晚"; in 20..21 -> "晚上"
                in 22..23 -> "午夜"; else -> ""
            }
            2 -> {
                val branch = when (hour) {
                    23, 0 -> "子"; in 1..2 -> "丑"; in 3..4 -> "寅"; in 5..6 -> "卯"
                    in 7..8 -> "辰"; in 9..10 -> "巳"; in 11..12 -> "午"; in 13..14 -> "未"
                    in 15..16 -> "申"; in 17..18 -> "酉"; in 19..20 -> "戌"; else -> "亥"
                }
                "${branch}时"
            }
            3 -> when (dateTime.monthValue) {
                3, 4, 5 -> "春季"; 6, 7, 8 -> "夏季"; 9, 10, 11 -> "秋季"; else -> "冬季"
            }
            else -> ""
        }
    }

    private fun join(vararg values: String): String = values.filter { it.isNotBlank() }.joinToString(" ")
    private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus
}
