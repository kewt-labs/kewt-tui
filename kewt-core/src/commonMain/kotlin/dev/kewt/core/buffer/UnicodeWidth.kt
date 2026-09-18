/*
* Copyright 2026 Kewt Labs
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* */
package dev.kewt.core.buffer

import kotlin.text.UnicodeCategory

/**
 * Heuristic Unicode width calculations for terminal rendering.
 *
 * Terminals render East-Asian wide and fullwidth characters (as well as most emoji)
 * across two cells, and combining marks across zero cells. This object approximates
 * `wcwidth` behavior with static range tables, which is sufficient for layout and
 * truncation decisions.
 */
public object UnicodeWidth {
    /**
     * Returns the number of terminal cells a code point occupies:
     * 0 for combining/format characters, 2 for wide characters, and 1 otherwise.
     */
    public fun of(codePoint: Int): Int {
        if (codePoint < 0x20 || codePoint == 0x7F) return 0
        if (codePoint < 0x7F) return 1

        // Zero-width code points
        if (codePoint in ZERO_WIDTH_RANGES) return 0

        // Wide (double-cell) code points
        if (codePoint in WIDE_RANGES) return 2

        // Combining marks and format characters in the BMP
        if (codePoint <= 0xFFFF) {
            val category = codePoint.toChar().category
            if (category == UnicodeCategory.NON_SPACING_MARK ||
                category == UnicodeCategory.ENCLOSING_MARK ||
                category == UnicodeCategory.FORMAT
            ) {
                return 0
            }
        }
        return 1
    }

    /** Returns the number of terminal cells a single [Char] occupies. */
    public fun ofChar(ch: Char): Int {
        if (ch.isLowSurrogate()) return 0
        return of(ch.code)
    }

    /** Returns the total number of terminal cells [text] occupies. */
    public fun displayWidth(text: String): Int {
        var width = 0
        var index = 0
        while (index < text.length) {
            val ch = text[index]
            if (ch.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()) {
                val codePoint =
                    0x10000 + ((ch.code - 0xD800) shl 10) + (text[index + 1].code - 0xDC00)
                width += of(codePoint)
                index += 2
            } else {
                width += ofChar(ch)
                index++
            }
        }
        return width
    }

    /**
     * Truncates [text] so it occupies at most [maxWidth] terminal cells.
     *
     * When [ellipsis] is true and truncation happens, the last cell is replaced
     * with the ellipsis character `…`.
     */
    public fun truncate(
        text: String,
        maxWidth: Int,
        ellipsis: Boolean = false,
    ): String {
        if (maxWidth <= 0) return ""
        if (displayWidth(text) <= maxWidth) return text

        val limit = if (ellipsis) maxWidth - 1 else maxWidth
        val sb = StringBuilder()
        var width = 0
        var index = 0
        while (index < text.length) {
            val ch = text[index]
            val isPair = ch.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()
            val codePoint =
                if (isPair) {
                    0x10000 + ((ch.code - 0xD800) shl 10) + (text[index + 1].code - 0xDC00)
                } else {
                    ch.code
                }
            val w = of(codePoint)
            if (width + w > limit) break
            sb.append(ch)
            if (isPair) sb.append(text[index + 1])
            width += w
            index += if (isPair) 2 else 1
        }
        if (ellipsis && width <= limit) sb.append('…')
        return sb.toString()
    }

    /**
     * Inclusive [start, end] code point ranges rendered with zero width (combining
     * marks, format characters, ZWJ), encoded as a compact hex table.
     */
    private val ZERO_WIDTH_RANGES: IntArray =
        parseRangeTable(
            "00AD,0300-036F,0483-0489,0591-05BD,0610-061A,064B-065F,0670,06D6-06DC," +
                "0711,0730-074A,07A6-07B0,0900-0903,093A,0951-0957,0E31,0E34-0E3A," +
                "0EB1,200B-200F,2028-202E,2060-2064,20D0-20F0,FE00-FE0F,FE20-FE2F," +
                "FEFF,FFF9-FFFB,1F3FB-1F3FF,E0001-E01EF",
        )

    /**
     * Inclusive [start, end] code point ranges rendered across two cells (East Asian
     * Wide/Fullwidth, emoji), encoded as a compact hex table.
     */
    private val WIDE_RANGES: IntArray =
        parseRangeTable(
            "1100-115F,231A-231B,2329-232A,23E9-23EC,25FD-25FE,2614-2615,2648-2653," +
                "267F,2693,26A1,26AA-26AB,26BD-26BE,26C4-26C5,26CE,26D4,26EA," +
                "26F2-26F3,26F5,26FA,26FD,2705,270A-270B,2728,274C,274E,2753-2755," +
                "2757,2795-2797,27B0,27BF,2B1B-2B1C,2B50,2B55,2E80-303E,3041-33FF," +
                "3400-4DBF,4E00-9FFF,A000-A4CF,A960-A97F,AC00-D7A3,F900-FAFF," +
                "FE10-FE19,FE30-FE6F,FF00-FF60,FFE0-FFE6,16FE0-16FE4,17000-187F7," +
                "1F000-1F0FF,1F100-1F1FF,1F200-1F64F,1F680-1F6FF,1F900-1F9FF," +
                "1FA70-1FAFF,20000-2FFFD,30000-3FFFD",
        )

    /**
     * Expands a table of `START` / `START-END` hex tokens into a flat array of
     * inclusive [start, end] integer pairs, keeping the pair order of the table.
     */
    private fun parseRangeTable(table: String): IntArray {
        val tokens = table.split(',')
        val ranges = IntArray(tokens.size * 2)
        tokens.forEachIndexed { index, token ->
            val bounds = token.split('-')
            val start = bounds[0].toInt(16)
            val end = if (bounds.size > 1) bounds[1].toInt(16) else start
            ranges[index * 2] = start
            ranges[index * 2 + 1] = end
        }
        return ranges
    }

    /**
     * Checks whether [value] falls into any of the inclusive [start, end] pairs in [ranges].
     */
    private operator fun IntArray.contains(value: Int): Boolean {
        var low = 0
        var high = size / 2 - 1
        while (low <= high) {
            val mid = (low + high) / 2
            val start = this[mid * 2]
            val end = this[mid * 2 + 1]
            when {
                value < start -> high = mid - 1
                value > end -> low = mid + 1
                else -> return true
            }
        }
        return false
    }
}
