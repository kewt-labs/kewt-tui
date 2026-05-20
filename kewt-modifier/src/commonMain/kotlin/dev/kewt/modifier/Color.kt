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
package dev.kewt.modifier

/**
 * Represents a color that can be used for foreground or background styling in a terminal.
 *
 * Supports various color formats including standard ANSI 16, ANSI 256, and TrueColor (RGB).
 */
public sealed class Color {
    /** The terminal's default color. */
    public data object Default : Color()

    /**
     * A TrueColor representation using 8-bit red, green, and blue components.
     */
    public data class RGB(val r: Int, val g: Int, val b: Int) : Color() {
        public constructor(hex: Int) : this((hex shr 16) and 0xFF, (hex shr 8) and 0xFF, hex and 0xFF)
    }

    /**
     * A standard ANSI 16 color.
     *
     * @property code The color code (0-15).
     */
    public data class Ansi16(val code: Int) : Color()

    /**
     * An extended ANSI 256 color.
     *
     * @property code The color code (0-255).
     */
    public data class Ansi256(val code: Int) : Color()

    public companion object {
        public val Black: Color = Ansi16(0)
        public val Red: Color = Ansi16(1)
        public val Green: Color = Ansi16(2)
        public val Yellow: Color = Ansi16(3)
        public val Blue: Color = Ansi16(4)
        public val Magenta: Color = Ansi16(5)
        public val Cyan: Color = Ansi16(6)
        public val White: Color = Ansi16(7)
        public val BrightBlack: Color = Ansi16(8)
        public val BrightRed: Color = Ansi16(9)
        public val BrightGreen: Color = Ansi16(10)
        public val BrightYellow: Color = Ansi16(11)
        public val BrightBlue: Color = Ansi16(12)
        public val BrightMagenta: Color = Ansi16(13)
        public val BrightCyan: Color = Ansi16(14)
        public val BrightWhite: Color = Ansi16(15)

        /** Creates a [Color.RGB] instance from a hex value. */
        public fun rgb(hex: Int): Color = RGB(hex)

        /** Creates a [Color.RGB] instance from individual R, G, and B components. */
        public fun rgb(r: Int, g: Int, b: Int): Color = RGB(r, g, b)

        /**
         * Packs a Color into a primitive Int for internal storage.
         */
        public fun pack(color: Color): Int = when (color) {
            Default -> 0

            is Ansi16 -> color.code + 1

            is Ansi256 -> color.code + 17

            is RGB -> {
                val rgb = (color.r shl 16) or (color.g shl 8) or color.b
                rgb or (1 shl 31) // Set sign bit to indicate RGB
            }
        }

        /**
         * Unpacks a Color from a packed primitive Int.
         */
        public fun unpack(packed: Int): Color = when {
            packed == 0 -> Default

            packed in 1..16 -> Ansi16(packed - 1)

            packed in 17..272 -> Ansi256(packed - 17)

            packed < 0 -> {
                val rgb = packed and (1 shl 31).inv()
                RGB((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF)
            }

            else -> Default
        }
    }
}
