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

public sealed class Color {
    public data object Default : Color()

    public data class RGB(val r: Int, val g: Int, val b: Int) : Color() {
        public constructor(hex: Int) : this((hex shr 16) and 0xFF, (hex shr 8) and 0xFF, hex and 0xFF)
    }

    public data class Ansi16(val code: Int) : Color()

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

        public fun rgb(hex: Int): Color = RGB(hex)

        public fun rgb(r: Int, g: Int, b: Int): Color = RGB(r, g, b)
    }
}
