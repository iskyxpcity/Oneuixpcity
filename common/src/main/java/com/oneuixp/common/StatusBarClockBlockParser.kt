package com.oneuixp.common

/**
 * Splits a status-bar clock pattern into independently styleable blocks without
 * changing the existing DateTimeFormatter-compatible format syntax.
 *
 * Examples:
 * HH:mm -> HH | : | mm
 * HH:mm\nMM月dd日 EEE -> HH | : | mm | \n | MM | 月 | dd | 日  | EEE
 *
 * Quoted DateTimeFormatter literals remain one block, and OneUIXP custom tokens
 * are kept whole instead of being split into ASCII letters.
 */
object StatusBarClockBlockParser {
    data class Block(
        val styleIndex: Int?,
        val source: String,
        val literalText: String? = null,
        val isLineBreak: Boolean = false,
    ) {
        val isStyleable: Boolean
            get() = styleIndex != null && !isLineBreak
    }

    private val customTokens = listOf(
        "CNZODIAC",
        "CNPERIOD",
        "CNLUNAR",
        "CNSEASON",
        "CNTIME",
        "CNYEAR",
    ).sortedByDescending(String::length)

    fun parse(pattern: String): List<Block> {
        if (pattern.isEmpty()) return emptyList()

        val result = mutableListOf<Block>()
        var cursor = 0
        var styleIndex = 0

        fun addStyleBlock(source: String, literalText: String? = null) {
            if (source.isEmpty()) return
            result += Block(
                styleIndex = styleIndex++,
                source = source,
                literalText = literalText,
            )
        }

        while (cursor < pattern.length) {
            val ch = pattern[cursor]

            if (ch == '\n') {
                result += Block(
                    styleIndex = null,
                    source = "\n",
                    literalText = "\n",
                    isLineBreak = true,
                )
                cursor++
                continue
            }

            if (ch == '\'') {
                val start = cursor
                cursor++
                val literal = StringBuilder()
                while (cursor < pattern.length) {
                    if (pattern[cursor] == '\'') {
                        if (cursor + 1 < pattern.length && pattern[cursor + 1] == '\'') {
                            literal.append('\'')
                            cursor += 2
                            continue
                        }
                        cursor++
                        break
                    }
                    literal.append(pattern[cursor])
                    cursor++
                }
                addStyleBlock(pattern.substring(start, cursor), literal.toString())
                continue
            }

            val custom = customTokens.firstOrNull { token ->
                pattern.regionMatches(cursor, token, 0, token.length)
            }
            if (custom != null) {
                addStyleBlock(custom)
                cursor += custom.length
                continue
            }

            if (ch in 'A'..'Z' || ch in 'a'..'z') {
                val start = cursor
                cursor++
                while (cursor < pattern.length && pattern[cursor] == ch) {
                    cursor++
                }
                addStyleBlock(pattern.substring(start, cursor))
                continue
            }

            val start = cursor
            cursor++
            while (cursor < pattern.length) {
                val next = pattern[cursor]
                if (next == '\n' || next == '\'' || next in 'A'..'Z' || next in 'a'..'z') {
                    break
                }
                if (customTokens.any { token ->
                        pattern.regionMatches(cursor, token, 0, token.length)
                    }
                ) {
                    break
                }
                cursor++
            }
            addStyleBlock(
                source = pattern.substring(start, cursor),
                literalText = pattern.substring(start, cursor),
            )
        }

        return result
    }
}
