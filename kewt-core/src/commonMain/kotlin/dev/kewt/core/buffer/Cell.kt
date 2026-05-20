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
    }

    /**
     * Checks if this cell has exactly the same character and styles as another.
     */
    public fun sameAs(other: Cell): Boolean =
        char == other.char && foreground == other.foreground &&
            background == other.background && bold == other.bold &&
            italic == other.italic && underline == other.underline &&
            strikethrough == other.strikethrough
}
