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

/**
 * Represents a grid of characters and their associated styles.
 *
 * The buffer uses primitive arrays to store data internally for efficient management
 * of terminal cell data.
 *
 * @property width The horizontal dimension of the buffer in characters.
 * @property height The vertical dimension of the buffer in characters.
 */
public class Buffer(
    public val width: Int,
    public val height: Int,
) {
    internal val chars = CharArray(width * height) { ' ' }
    internal val fgColors = IntArray(width * height) { 0 }
    internal val bgColors = IntArray(width * height) { 0 }
    internal val flags = ByteArray(width * height) { 0 }

    /**
     * Retrieves the contents of a specific cell as a [Cell] object.
     *
     * Note: This method creates a new [Cell] instance. Direct access to internal
     * arrays can be used to avoid object creation.
     */
    public fun get(x: Int, y: Int): Cell {
        if (x !in 0..<width || y !in 0..<height) return Cell()
        val i = y * width + x
        val f = flags[i].toInt()
        return Cell(
            char = chars[i],
            foreground = Color.unpack(fgColors[i]),
            background = Color.unpack(bgColors[i]),
            bold = (f and 1) != 0,
            italic = (f and 2) != 0,
            underline = (f and 4) != 0,
            strikethrough = (f and 8) != 0,
        )
    }

    /**
     * Sets the character and styles for a specific cell.
     *
     * Providing null for any style parameter will preserve the existing value in that cell.
     */
    public fun setChar(
        x: Int,
        y: Int,
        char: Char? = null,
        foreground: Color? = null,
        background: Color? = null,
        bold: Boolean? = null,
        italic: Boolean? = null,
        underline: Boolean? = null,
        strikethrough: Boolean? = null,
    ) {
        if (x !in 0..<width || y !in 0..<height) return
        val i = y * width + x

        if (char != null) chars[i] = char
        if (foreground != null) fgColors[i] = Color.pack(foreground)
        if (background != null) bgColors[i] = Color.pack(background)

        if (bold != null || italic != null || underline != null || strikethrough != null) {
            var f = flags[i].toInt()
            if (bold != null) f = if (bold) f or 1 else f and 1.inv()
            if (italic != null) f = if (italic) f or 2 else f and 2.inv()
            if (underline != null) f = if (underline) f or 4 else f and 4.inv()
            if (strikethrough != null) f = if (strikethrough) f or 8 else f and 8.inv()
            flags[i] = f.toByte()
        }
    }

    /**
     * Writes a string into the buffer at the specified coordinates.
     *
     * Text that exceeds the buffer width will be truncated.
     * Providing null for any style parameter will preserve the existing value in those cells.
     */
    public fun writeString(
        x: Int,
        y: Int,
        text: String,
        foreground: Color? = null,
        background: Color? = null,
        bold: Boolean? = null,
        italic: Boolean? = null,
        underline: Boolean? = null,
        strikethrough: Boolean? = null,
    ) {
        if (y !in 0..<height) return
        var cx = x
        for (ch in text) {
            if (cx >= width) return
            if (cx >= 0) {
                setChar(cx, y, ch, foreground, background, bold, italic, underline, strikethrough)
            }
            cx++
        }
    }

    /**
     * Resets the entire buffer to default characters and styles.
     */
    public fun clear() {
        chars.fill(' ')
        fgColors.fill(0)
        bgColors.fill(0)
        flags.fill(0)
    }

    /**
     * Sets the background color of a specific cell.
     */
    public fun setBackground(
        x: Int,
        y: Int,
        color: Color,
    ) {
        if (x !in 0..<width || y !in 0..<height) return
        bgColors[y * width + x] = Color.pack(color)
    }
}
