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

public class BufferDiff(private val colorMode: ColorMode = ColorMode.TrueColor) {
    private val out = StringBuilder()

    public fun diff(current: Buffer, previous: Buffer): String {
        out.clear()
        var lastX = -1
        var lastY = -1
        var lastForeground: Color? = null
        var lastBackground: Color? = null
        var lastBold = false

        for (y in 0 until current.height) {
            for (x in 0 until current.width) {
                val curr = current.get(x, y)
                val prev = previous.get(x, y)
                if (curr.sameAs(prev)) continue

                if (lastY != y || lastX != x) {
                    out.append("\u001b[${y + 1};${x + 1}H")
                }

                if (curr.bold != lastBold || curr.foreground != lastForeground || curr.background != lastBackground) {
                    out.append("\u001b[0m")
                    if (curr.bold) out.append("\u001b[1m")
                    if (curr.italic) out.append("\u001b[3m")
                    if (curr.underline) out.append("\u001b[4m")
                    if (curr.strikethrough) out.append("\u001b[9m")
                    appendForeground(curr.foreground)
                    appendBackground(curr.background)
                    lastForeground = curr.foreground
                    lastBackground = curr.background
                    lastBold = curr.bold
                }

                out.append(curr.char)
                lastX = x + 1
                lastY = y
            }
        }

        if (out.isNotEmpty()) out.append("\u001b[0m")
        return out.toString()
    }

    private fun appendForeground(color: Color) {
        when (color) {
            Color.Default -> {}
            is Color.Ansi16 -> out.append("\u001b[${if (color.code < 8) 30 + color.code else 90 + color.code - 8}m")
            is Color.Ansi256 -> out.append("\u001b[38;5;${color.code}m")
            is Color.RGB -> out.append("\u001b[38;2;${color.r};${color.g};${color.b}m")
        }
    }

    private fun appendBackground(color: Color) {
        when (color) {
            Color.Default -> {}
            is Color.Ansi16 -> out.append("\u001b[${if (color.code < 8) 40 + color.code else 100 + color.code - 8}m")
            is Color.Ansi256 -> out.append("\u001b[48;5;${color.code}m")
            is Color.RGB -> out.append("\u001b[48;2;${color.r};${color.g};${color.b}m")
        }
    }
}
