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
 * A bundle of optional style overrides applied to cells during buffer writes.
 *
 * A null property means "preserve the existing value".
 */
internal data class CellPatch(
    val foreground: Color? = null,
    val background: Color? = null,
    val bold: Boolean? = null,
    val italic: Boolean? = null,
    val underline: Boolean? = null,
    val strikethrough: Boolean? = null,
    val dim: Boolean? = null,
    val blink: Boolean? = null,
    val reverse: Boolean? = null,
    val hidden: Boolean? = null,
) {
    val isNoop: Boolean
        get() = foreground == null && background == null && bold == null && italic == null &&
            underline == null && strikethrough == null && dim == null && blink == null &&
            reverse == null && hidden == null
}

/**
 * Represents a grid of characters and their associated styles.
 *
 * The buffer uses primitive arrays to store data internally for efficient management
 * of terminal cell data. Double-width characters (CJK, emoji) occupy two cells: the
 * trailing cell is flagged as a continuation and is skipped by the renderer.
 *
 * Writes can be restricted to a sub-region by setting [clip]; cells outside the clip
 * rectangle are left untouched while text advance continues, so nested containers
 * cannot paint over their siblings.
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
    internal val flags = ShortArray(width * height) { 0 }

    /**
     * When non-null, all write operations are restricted to this rectangle.
     */
    public var clip: Rect? = null

    /**
     * Retrieves the contents of a specific cell as a [Cell] object.
     *
     * Note: This method creates a new [Cell] instance. Direct access to internal
     * arrays can be used to avoid object creation.
     */
    public fun get(
        x: Int,
        y: Int,
    ): Cell {
        if (x !in 0..<width || y !in 0..<height) return Cell()
        val i = y * width + x
        val f = flags[i].toInt()
        return Cell(
            char = chars[i],
            foreground = Color.unpack(fgColors[i]),
            background = Color.unpack(bgColors[i]),
            bold = (f and CellFlag.BOLD) != 0,
            italic = (f and CellFlag.ITALIC) != 0,
            underline = (f and CellFlag.UNDERLINE) != 0,
            strikethrough = (f and CellFlag.STRIKETHROUGH) != 0,
            dim = (f and CellFlag.DIM) != 0,
            blink = (f and CellFlag.BLINK) != 0,
            reverse = (f and CellFlag.REVERSE) != 0,
            hidden = (f and CellFlag.HIDDEN) != 0,
            continuation = (f and CellFlag.WIDE_CONTINUATION) != 0,
        )
    }

    /**
     * Sets the character and styles for a specific cell.
     *
     * Providing null for any style parameter will preserve the existing value in that cell.
     * Writing a character into a cell clears any wide-character continuation flag; when the
     * cell was itself the trailing half of a wide character, the orphaned leading half is
     * blanked, mirroring terminal behavior.
     */
    @Suppress("LongParameterList")
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
        dim: Boolean? = null,
        blink: Boolean? = null,
        reverse: Boolean? = null,
        hidden: Boolean? = null,
    ) {
        if (!inBounds(x, y)) return
        val i = y * width + x
        if (char != null) prepareCellForWrite(x, i)
        if (char != null) chars[i] = char
        applyStyle(
            i,
            CellPatch(foreground, background, bold, italic, underline, strikethrough, dim, blink, reverse, hidden),
        )
    }

    /**
     * Writes a string into the buffer at the specified coordinates.
     *
     * The text is laid out by Unicode code point: zero-width (combining) characters are
     * skipped, double-width characters occupy two cells, and text that exceeds the buffer
     * or clip region is truncated. Providing null for any style parameter will preserve
     * the existing values in those cells.
     */
    @Suppress("LongParameterList", "CyclomaticComplexMethod", "LongMethod")
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
        dim: Boolean? = null,
        blink: Boolean? = null,
        reverse: Boolean? = null,
        hidden: Boolean? = null,
    ) {
        if (y !in 0..<height) return
        val patch =
            CellPatch(foreground, background, bold, italic, underline, strikethrough, dim, blink, reverse, hidden)
        val rowStart = y * width
        var cx = x
        var index = 0
        while (index < text.length) {
            if (cx >= width) return
            val ch = text[index]
            val isPair = ch.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()
            val codePoint =
                if (isPair) {
                    0x10000 + ((ch.code - 0xD800) shl 10) + (text[index + 1].code - 0xDC00)
                } else {
                    ch.code
                }
            val second = if (isPair) text[index + 1] else ' '
            index += if (isPair) 2 else 1

            val cellWidth = UnicodeWidth.of(codePoint)
            if (cellWidth == 0) continue

            val occupiesTwo = isPair || cellWidth == 2
            if (cx < 0) {
                cx += if (occupiesTwo) 2 else 1
                continue
            }
            if (occupiesTwo && cx + 1 >= width) return

            // A wide character is only written when both of its cells are writable
            if (inClip(cx, y) && (!occupiesTwo || inClip(cx + 1, y))) {
                val i = rowStart + cx
                prepareCellForWrite(cx, i)
                chars[i] = ch
                applyStyle(i, patch)
                if (occupiesTwo) {
                    val j = i + 1
                    prepareCellForWrite(cx + 1, j)
                    chars[j] = second
                    applyStyle(j, patch)
                    flags[j] = (flags[j].toInt() or CellFlag.WIDE_CONTINUATION).toShort()
                }
            }
            cx += if (occupiesTwo) 2 else 1
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
     * Fills the entire buffer (respecting [clip]) with the given character and styles.
     *
     * Providing null for any parameter preserves the existing value in each cell.
     */
    @Suppress("LongParameterList")
    public fun fill(
        char: Char? = ' ',
        foreground: Color? = null,
        background: Color? = null,
        bold: Boolean? = null,
        italic: Boolean? = null,
        underline: Boolean? = null,
        strikethrough: Boolean? = null,
        dim: Boolean? = null,
        blink: Boolean? = null,
        reverse: Boolean? = null,
        hidden: Boolean? = null,
    ) {
        fillRect(0, 0, width, height, char, foreground, background, bold, italic, underline, strikethrough, dim, blink, reverse, hidden)
    }

    /**
     * Fills a rectangular region (respecting [clip]) with the given character and styles.
     *
     * Providing null for any parameter preserves the existing value in each cell.
     */
    @Suppress("LongParameterList")
    public fun fillRect(
        x: Int,
        y: Int,
        rectWidth: Int,
        rectHeight: Int,
        char: Char? = null,
        foreground: Color? = null,
        background: Color? = null,
        bold: Boolean? = null,
        italic: Boolean? = null,
        underline: Boolean? = null,
        strikethrough: Boolean? = null,
        dim: Boolean? = null,
        blink: Boolean? = null,
        reverse: Boolean? = null,
        hidden: Boolean? = null,
    ) {
        val patch =
            CellPatch(foreground, background, bold, italic, underline, strikethrough, dim, blink, reverse, hidden)
        val startX = maxOf(x, 0)
        val startY = maxOf(y, 0)
        val endX = minOf(x + rectWidth, width)
        val endY = minOf(y + rectHeight, height)
        for (row in startY until endY) {
            for (col in startX until endX) {
                if (!inClip(col, row)) continue
                val i = row * width + col
                if (char != null) {
                    prepareCellForWrite(col, i)
                    chars[i] = char
                }
                applyStyle(i, patch)
            }
        }
    }

    /**
     * Applies styles to a rectangular region without changing any characters.
     */
    @Suppress("LongParameterList")
    public fun setStyle(
        rect: Rect,
        foreground: Color? = null,
        background: Color? = null,
        bold: Boolean? = null,
        italic: Boolean? = null,
        underline: Boolean? = null,
        strikethrough: Boolean? = null,
        dim: Boolean? = null,
        blink: Boolean? = null,
        reverse: Boolean? = null,
        hidden: Boolean? = null,
    ) {
        fillRect(
            rect.x,
            rect.y,
            rect.width,
            rect.height,
            null,
            foreground,
            background,
            bold,
            italic,
            underline,
            strikethrough,
            dim,
            blink,
            reverse,
            hidden,
        )
    }

    /**
     * Sets the foreground color of a specific cell.
     */
    public fun setForeground(
        x: Int,
        y: Int,
        color: Color,
    ) {
        if (!inBounds(x, y)) return
        fgColors[y * width + x] = Color.pack(color)
    }

    /**
     * Sets the background color of a specific cell.
     */
    public fun setBackground(
        x: Int,
        y: Int,
        color: Color,
    ) {
        if (!inBounds(x, y)) return
        bgColors[y * width + x] = Color.pack(color)
    }

    /**
     * Copies the cells of [other] into this buffer with its top-left corner at ([x], [y]).
     *
     * Cells falling outside this buffer or its [clip] region are skipped.
     */
    public fun merge(
        other: Buffer,
        x: Int = 0,
        y: Int = 0,
    ) {
        for (row in 0 until other.height) {
            val targetY = y + row
            if (targetY !in 0..<height) continue
            for (col in 0 until other.width) {
                val targetX = x + col
                if (!inBounds(targetX, targetY) || !inClip(targetX, targetY)) continue
                val src = row * other.width + col
                val dst = targetY * width + targetX
                chars[dst] = other.chars[src]
                fgColors[dst] = other.fgColors[src]
                bgColors[dst] = other.bgColors[src]
                flags[dst] = other.flags[src]
            }
        }
    }

    /**
     * Returns a new buffer of the requested size with the overlapping content copied over.
     */
    public fun resize(
        newWidth: Int,
        newHeight: Int,
    ): Buffer {
        val resized = Buffer(newWidth, newHeight)
        resized.merge(this)
        return resized
    }

    private fun inBounds(
        x: Int,
        y: Int,
    ): Boolean = x in 0..<width && y in 0..<height

    private fun inClip(
        x: Int,
        y: Int,
    ): Boolean {
        val region = clip ?: return true
        return region.contains(x, y)
    }

    /**
     * Prepares a cell to receive new content by clearing wide-character bookkeeping:
     * - if the cell was the trailing half of a wide character, the orphaned leading
     *   half is blanked;
     * - if the following cell is the trailing half of this cell's wide character, it is
     *   blanked and de-flagged.
     */
    private fun prepareCellForWrite(
        x: Int,
        i: Int,
    ) {
        val f = flags[i].toInt()
        if ((f and CellFlag.WIDE_CONTINUATION) != 0 && x > 0) {
            chars[i - 1] = ' '
        }
        flags[i] = (f and CellFlag.WIDE_CONTINUATION.inv()).toShort()
        if (x + 1 < width) {
            val j = i + 1
            val nf = flags[j].toInt()
            if ((nf and CellFlag.WIDE_CONTINUATION) != 0) {
                chars[j] = ' '
                flags[j] = (nf and CellFlag.WIDE_CONTINUATION.inv()).toShort()
            }
        }
    }

    private fun applyStyle(
        i: Int,
        patch: CellPatch,
    ) {
        if (patch.isNoop) return
        val fg = patch.foreground
        if (fg != null) fgColors[i] = Color.pack(fg)
        val bg = patch.background
        if (bg != null) bgColors[i] = Color.pack(bg)

        var f = flags[i].toInt()
        val bold = patch.bold
        if (bold != null) f = setFlag(f, CellFlag.BOLD, bold)
        val italic = patch.italic
        if (italic != null) f = setFlag(f, CellFlag.ITALIC, italic)
        val underline = patch.underline
        if (underline != null) f = setFlag(f, CellFlag.UNDERLINE, underline)
        val strikethrough = patch.strikethrough
        if (strikethrough != null) f = setFlag(f, CellFlag.STRIKETHROUGH, strikethrough)
        val dim = patch.dim
        if (dim != null) f = setFlag(f, CellFlag.DIM, dim)
        val blink = patch.blink
        if (blink != null) f = setFlag(f, CellFlag.BLINK, blink)
        val reverse = patch.reverse
        if (reverse != null) f = setFlag(f, CellFlag.REVERSE, reverse)
        val hidden = patch.hidden
        if (hidden != null) f = setFlag(f, CellFlag.HIDDEN, hidden)
        flags[i] = f.toShort()
    }

    private fun setFlag(
        value: Int,
        mask: Int,
        on: Boolean,
    ): Int = if (on) value or mask else value and mask.inv()
}
