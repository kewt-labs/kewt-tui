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

public class Buffer(
    public val width: Int,
    public val height: Int,
) {
    private val cells = Array(width * height) { Cell() }

    public fun get(x: Int, y: Int): Cell {
        if (x !in 0..<width || y !in 0..<height) return Cell()
        return cells[y * width + x]
    }

    public fun setChar(
        x: Int,
        y: Int,
        char: Char,
        foreground: Color = Color.Default,
        background: Color = Color.Default,
    ) {
        if (x !in 0..<width || y !in 0..<height) return
        val cell = cells[y * width + x]
        cell.char = char
        cell.foreground = foreground
        cell.background = background
    }

    public fun writeString(
        x: Int,
        y: Int,
        text: String,
        foreground: Color = Color.Default,
        background: Color = Color.Default,
        bold: Boolean = false,
        italic: Boolean = false,
        underline: Boolean = false,
    ) {
        if (y !in 0..<height) return
        var cx = x
        for (ch in text) {
            if (cx >= width) return
            if (cx >= 0) {
                val cell = cells[y * width + cx]
                cell.char = ch
                cell.foreground = foreground
                cell.background = background
                cell.bold = bold
                cell.italic = italic
                cell.underline = underline
            }
            cx++
        }
    }

    public fun clear() {
        cells.forEach { it.reset() }
    }
}
