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

import dev.kewt.modifier.Color
import dev.kewt.terminal.ColorMode

/**
 * Calculates the difference between two [Buffer] instances and generates ANSI escape sequences.
 *
 * This class identifies changed cells and updates styles using additive ANSI sequences.
 * Wide-character continuation cells are never emitted on their own; they are rendered
 * as part of their leading cell.
 *
 * @param colorMode The [ColorMode] supported by the target terminal.
 */
public class BufferDiff(private val colorMode: ColorMode = ColorMode.TrueColor) {
    private val out = StringBuilder()

    /**
     * Generates an ANSI string that transforms the [previous] buffer into the [current] one.
     *
     * When the buffers have different dimensions (for example right after a resize),
     * every cell of [current] that has no counterpart in [previous] is treated as changed.
     */
    @Suppress("LongMethod", "CyclomaticComplexMethod", "NestedBlockDepth")
    public fun diff(
        current: Buffer,
        previous: Buffer,
    ): String {
        out.clear()
        val sameSize = current.width == previous.width && current.height == previous.height
        var lastX = -1
        var lastY = -1
        var lastFg = 0
        var lastBg = 0
        var lastFlags = 0

        for (y in 0 until current.height) {
            val rowOffset = y * current.width
            for (x in 0 until current.width) {
                val i = rowOffset + x

                // Continuation cells are emitted together with their leading cell
                if ((current.flags[i].toInt() and CellFlag.WIDE_CONTINUATION) != 0) continue

                // Compare primitive data directly
                if (sameSize &&
                    current.chars[i] == previous.chars[i] &&
                    current.fgColors[i] == previous.fgColors[i] &&
                    current.bgColors[i] == previous.bgColors[i] &&
                    current.flags[i] == previous.flags[i]
                ) {
                    continue
                }

                if (lastY != y || lastX != x) {
                    out.append("\u001b[${y + 1};${x + 1}H")
                }

                val currFlags = current.flags[i].toInt()
                val currFg = current.fgColors[i]
                val currBg = current.bgColors[i]

                appendAttributeChanges(lastFlags, currFlags)

                if (currFg != lastFg) {
                    appendForeground(Color.unpack(currFg))
                }
                if (currBg != lastBg) {
                    appendBackground(Color.unpack(currBg))
                }

                // Detect a double-width cell: either an astral surrogate pair or a
                // character whose trailing half is flagged as a continuation cell.
                val nextInRow = x + 1 < current.width
                val pair = nextInRow &&
                    current.chars[i].isHighSurrogate() &&
                    current.chars[i + 1].isLowSurrogate()
                val wide = nextInRow &&
                    (current.flags[i + 1].toInt() and CellFlag.WIDE_CONTINUATION) != 0

                out.append(current.chars[i])
                if (pair) out.append(current.chars[i + 1])

                when {
                    pair -> {
                        val codePoint =
                            0x10000 +
                                ((current.chars[i].code - 0xD800) shl 10) +
                                (current.chars[i + 1].code - 0xDC00)
                        if (UnicodeWidth.of(codePoint) == 2) {
                            lastX = x + 2
                            lastY = y
                        } else {
                            // Narrow astral glyph occupying two cells: the terminal cursor
                            // only advances one column, so force absolute positioning next.
                            lastX = -1
                            lastY = -1
                        }
                    }

                    wide -> {
                        lastX = x + 2
                        lastY = y
                    }

                    else -> {
                        lastX = x + 1
                        lastY = y
                    }
                }
                lastFg = currFg
                lastBg = currBg
                lastFlags = currFlags
            }
        }

        if (out.isNotEmpty()) out.append("\u001b[0m")
        return out.toString()
    }

    /**
     * Emits the SGR sequences required to move from [lastFlags] to [currFlags].
     *
     * Bold and dim share the same reset code (22), so they are handled as a group.
     */
    private fun appendAttributeChanges(
        lastFlags: Int,
        currFlags: Int,
    ) {
        val boldDimMask = CellFlag.BOLD or CellFlag.DIM
        if ((lastFlags and boldDimMask) != (currFlags and boldDimMask)) {
            if ((lastFlags and boldDimMask) != 0) out.append("\u001b[22m")
            if ((currFlags and CellFlag.BOLD) != 0) out.append("\u001b[1m")
            if ((currFlags and CellFlag.DIM) != 0) out.append("\u001b[2m")
        }
        appendToggle(lastFlags, currFlags, CellFlag.ITALIC, "\u001b[3m", "\u001b[23m")
        appendToggle(lastFlags, currFlags, CellFlag.UNDERLINE, "\u001b[4m", "\u001b[24m")
        appendToggle(lastFlags, currFlags, CellFlag.BLINK, "\u001b[5m", "\u001b[25m")
        appendToggle(lastFlags, currFlags, CellFlag.REVERSE, "\u001b[7m", "\u001b[27m")
        appendToggle(lastFlags, currFlags, CellFlag.HIDDEN, "\u001b[8m", "\u001b[28m")
        appendToggle(lastFlags, currFlags, CellFlag.STRIKETHROUGH, "\u001b[9m", "\u001b[29m")
    }

    private fun appendToggle(
        lastFlags: Int,
        currFlags: Int,
        mask: Int,
        on: String,
        off: String,
    ) {
        val wasOn = (lastFlags and mask) != 0
        val isOn = (currFlags and mask) != 0
        if (wasOn && !isOn) out.append(off)
        if (!wasOn && isOn) out.append(on)
    }

    private fun appendForeground(color: Color) {
        when (val c = downgrade(color)) {
            Color.Default -> out.append("\u001b[39m")
            is Color.Ansi16 -> out.append("\u001b[${if (c.code < 8) 30 + c.code else 90 + c.code - 8}m")
            is Color.Ansi256 -> out.append("\u001b[38;5;${c.code}m")
            is Color.RGB -> out.append("\u001b[38;2;${c.r};${c.g};${c.b}m")
        }
    }

    private fun appendBackground(color: Color) {
        when (val c = downgrade(color)) {
            Color.Default -> out.append("\u001b[49m")
            is Color.Ansi16 -> out.append("\u001b[${if (c.code < 8) 40 + c.code else 100 + c.code - 8}m")
            is Color.Ansi256 -> out.append("\u001b[48;5;${c.code}m")
            is Color.RGB -> out.append("\u001b[48;2;${c.r};${c.g};${c.b}m")
        }
    }

    /**
     * Maps a high-resolution color to a lower-resolution one supported by the current [colorMode].
     */
    private fun downgrade(color: Color): Color = when (colorMode) {
        ColorMode.TrueColor -> color

        ColorMode.Extended -> when (color) {
            is Color.RGB -> Color.Ansi256(rgbToAnsi256(color.r, color.g, color.b))
            else -> color
        }

        ColorMode.Basic -> when (color) {
            is Color.RGB -> Color.Ansi16(rgbToAnsi16(color.r, color.g, color.b))
            is Color.Ansi256 -> Color.Ansi16(ansi256To16(color.code))
            else -> color
        }

        ColorMode.NoColor -> Color.Default
    }

    private fun rgbToAnsi256(
        r: Int,
        g: Int,
        b: Int,
    ): Int {
        if (r == g && g == b) {
            if (r < 8) return 16
            if (r > 248) return 231
            return ((r - 8) / 247.0 * 24).toInt() + 232
        }
        return 16 + (36 * (r / 51)) + (6 * (g / 51)) + (b / 51)
    }

    private fun rgbToAnsi16(
        r: Int,
        g: Int,
        b: Int,
    ): Int {
        val isBright = r > 128 || g > 128 || b > 128
        val threshold = if (isBright) 128 else 0
        val ri = if (r > threshold) 1 else 0
        val gi = if (g > threshold) 2 else 0
        val bi = if (b > threshold) 4 else 0
        return (ri or gi or bi) + (if (isBright) 8 else 0)
    }

    private fun ansi256To16(code: Int): Int = when {
        code < 16 -> code

        code in 232..255 -> if (code < 244) 0 else 7

        else -> {
            val r = (code - 16) / 36
            val g = ((code - 16) % 36) / 6
            val b = (code - 16) % 6
            rgbToAnsi16(r * 51, g * 51, b * 51)
        }
    }
}
