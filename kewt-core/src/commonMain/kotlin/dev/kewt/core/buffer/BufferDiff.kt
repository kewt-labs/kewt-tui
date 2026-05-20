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
 *
 * @param colorMode The [ColorMode] supported by the target terminal.
 */
public class BufferDiff(private val colorMode: ColorMode = ColorMode.TrueColor) {
    private val out = StringBuilder()

    /**
     * Generates an ANSI string that transforms the [previous] buffer into the [current] one.
     */
    public fun diff(current: Buffer, previous: Buffer): String {
        out.clear()
        var lastX = -1
        var lastY = -1
        var lastFg = 0
        var lastBg = 0
        var lastFlags = 0

        for (y in 0 until current.height) {
            val rowOffset = y * current.width
            for (x in 0 until current.width) {
                val i = rowOffset + x

                // Compare primitive data directly
                if (current.chars[i] == previous.chars[i] &&
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

                // Turn off attributes
                if ((lastFlags and 1) != 0 && (currFlags and 1) == 0) out.append("\u001b[22m")
                if ((lastFlags and 2) != 0 && (currFlags and 2) == 0) out.append("\u001b[23m")
                if ((lastFlags and 4) != 0 && (currFlags and 4) == 0) out.append("\u001b[24m")
                if ((lastFlags and 8) != 0 && (currFlags and 8) == 0) out.append("\u001b[29m")

                // Turn on attributes
                if ((lastFlags and 1) == 0 && (currFlags and 1) != 0) out.append("\u001b[1m")
                if ((lastFlags and 2) == 0 && (currFlags and 2) != 0) out.append("\u001b[3m")
                if ((lastFlags and 4) == 0 && (currFlags and 4) != 0) out.append("\u001b[4m")
                if ((lastFlags and 8) == 0 && (currFlags and 8) != 0) out.append("\u001b[9m")

                if (currFg != lastFg) {
                    appendForeground(Color.unpack(currFg))
                }
                if (currBg != lastBg) {
                    appendBackground(Color.unpack(currBg))
                }

                out.append(current.chars[i])

                lastX = x + 1
                lastY = y
                lastFg = currFg
                lastBg = currBg
                lastFlags = currFlags
            }
        }

        if (out.isNotEmpty()) out.append("\u001b[0m")
        return out.toString()
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
            is Color.RGB -> out.append("\u001b[38;2;${c.r};${c.g};${c.b}m")
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

    private fun rgbToAnsi256(r: Int, g: Int, b: Int): Int {
        if (r == g && g == b) {
            if (r < 8) return 16
            if (r > 248) return 231
            return ((r - 8) / 247.0 * 24).toInt() + 232
        }
        return 16 + (36 * (r / 51)) + (6 * (g / 51)) + (b / 51)
    }

    private fun rgbToAnsi16(r: Int, g: Int, b: Int): Int {
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
