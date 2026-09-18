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
 * A data container representing the visual state of a single terminal cell.
 */
public data class Cell(
    public var char: Char = ' ',
    public var foreground: Color = Color.Default,
    public var background: Color = Color.Default,
    public var bold: Boolean = false,
    public var italic: Boolean = false,
    public var underline: Boolean = false,
    public var strikethrough: Boolean = false,
    public var dim: Boolean = false,
    public var blink: Boolean = false,
    public var reverse: Boolean = false,
    public var hidden: Boolean = false,
    /**
     * True when this cell is the trailing half of a double-width character
     * and does not render content of its own.
     */
    public var continuation: Boolean = false,
) {
    /**
     * Resets the cell to its default state.
     */
    public fun reset() {
        char = ' '
        foreground = Color.Default
        background = Color.Default
        bold = false
        italic = false
        underline = false
        strikethrough = false
        dim = false
        blink = false
        reverse = false
        hidden = false
        continuation = false
    }

    /**
     * Checks if this cell has exactly the same character and styles as another.
     */
    public fun sameAs(other: Cell): Boolean =
        char == other.char && foreground == other.foreground &&
            background == other.background && bold == other.bold &&
            italic == other.italic && underline == other.underline &&
            strikethrough == other.strikethrough && dim == other.dim &&
            blink == other.blink && reverse == other.reverse &&
            hidden == other.hidden && continuation == other.continuation
}

/**
 * Bit flags used to pack boolean cell attributes into a primitive [Short].
 */
internal object CellFlag {
    const val BOLD = 1
    const val ITALIC = 2
    const val UNDERLINE = 4
    const val STRIKETHROUGH = 8
    const val DIM = 16
    const val BLINK = 32
    const val REVERSE = 64
    const val HIDDEN = 128
    const val WIDE_CONTINUATION = 256
}
