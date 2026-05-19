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

public data class BorderStyle(
    val topLeft: Char,
    val topRight: Char,
    val bottomLeft: Char,
    val bottomRight: Char,
    val horizontal: Char,
    val vertical: Char,
) {
    public companion object {
        public val Plain: BorderStyle = BorderStyle(' ', ' ', ' ', ' ', '-', '|')
        public val Rounded: BorderStyle = BorderStyle('╭', '╮', '╰', '╯', '─', '│')
        public val Double: BorderStyle = BorderStyle('╔', '╗', '╚', '╝', '═', '║')
        public val Heavy: BorderStyle = BorderStyle('┏', '┓', '┗', '┛', '━', '┃')
        public val Light: BorderStyle = BorderStyle('┌', '┐', '└', '┘', '─', '│')
    }
}
