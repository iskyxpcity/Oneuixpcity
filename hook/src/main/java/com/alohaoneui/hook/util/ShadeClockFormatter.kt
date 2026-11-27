package com.alohaoneui.hook.util

import com.alohaoneui.common.Preference
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ShadeClockFormatter {
    private val lunarMonths = arrayOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
    private val dayPrefixes = arrayOf("初", "十", "廿", "三")
    private val daySuffixes = arrayOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
    private val animals = arrayOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")
    private val stems = charArrayOf('甲', '乙', '丙', '丁', '戊', '己', '庚', '辛', '壬', '癸')
    private val branches = charArrayOf('子', '丑', '寅', '卯', '辰', '巳', '午', '未', '申', '酉', '戌', '亥')

    fun format(config: Preference.SystemUI.StatusBar, dateTime: LocalDateTime = LocalDateTime.now()): String {
        val time = if (config.shadeClockTimeEnabled) DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).format(dateTime) else ""
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
        return rendered.ifBlank { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).format(dateTime) }
    }

    private fun dateText(date: LocalDate, format: Int): String {
        val pattern = when (format) { 1 -> "yyyy-MM-dd"; 2 -> "MM/dd"; else -> "M月d日" }
        return DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(date)
    }

    private fun weekText(dateTime: LocalDateTime, format: Int): String {
        val pattern = when (format) { 1 -> "EEEE"; 2 -> "E"; else -> "EEE" }
        return DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(dateTime)
    }

    private fun lunarText(date: LocalDate, format: Int): String {
        val info = TraditionalChineseCalendar.getLunarInfo(date) ?: return ""
        val lunar = (if (info.isLeapMonth) "闰" else "") + lunarMonths[info.month - 1] + "月" + lunarDay(info.day)
        val offset = info.year - 4
        val zodiacYear = animals[offset.floorMod(12)] + "年"
        val cyclicalYear = "${stems[offset.floorMod(10)]}${branches[offset.floorMod(12)]}年"
        val special = lunarFestival(info.month, info.day) ?: solarFestival(date).orEmpty()
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

    private fun lunarFestival(month: Int, day: Int): String? = when (month to day) {
        1 to 1 -> "春节"; 1 to 15 -> "元宵"; 2 to 2 -> "龙抬头"; 5 to 5 -> "端午"
        7 to 7 -> "七夕"; 7 to 15 -> "中元"; 8 to 15 -> "中秋"; 9 to 9 -> "重阳"
        12 to 8 -> "腊八"; 12 to 23 -> "小年"; 12 to 30 -> "除夕"
        else -> null
    }

    private fun solarFestival(date: LocalDate): String? = when (date.monthValue to date.dayOfMonth) {
        1 to 1 -> "元旦"; 3 to 8 -> "妇女节"; 5 to 1 -> "劳动节"
        6 to 1 -> "儿童节"; 10 to 1 -> "国庆节"; 12 to 25 -> "圣诞节"
        else -> null
    }

    private fun detailText(dateTime: LocalDateTime, style: Int): String {
        val hour = dateTime.hour
        return when (style) {
            1 -> when (hour) {
                in 0..4 -> "凌晨"; in 5..7 -> "早晨"; in 8..10 -> "上午"; in 11..12 -> "中午"
                in 13..17 -> "下午"; in 18..20 -> "傍晚"; in 21..22 -> "晚上"; else -> "深夜"
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
                3, 4, 5 -> "春"; 6, 7, 8 -> "夏"; 9, 10, 11 -> "秋"; else -> "冬"
            }
            else -> ""
        }
    }

    private fun join(vararg values: String): String = values.filter { it.isNotBlank() }.joinToString(" ")
    private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus
}
