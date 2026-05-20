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
package dev.kewt.test

import dev.kewt.core.buffer.Buffer
import dev.kewt.modifier.Color
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Captures the characters in the [Buffer] as a literal string representation.
 */
public fun Buffer.captureSnapshot(): String {
    val sb = StringBuilder()
    for (y in 0 until height) {
        for (x in 0 until width) {
            sb.append(get(x, y).char)
        }
        if (y < height - 1) sb.append('\n')
    }
    return sb.toString()
}

/**
 * Asserts that the buffer contains the specified [text] anywhere in its character grid.
 */
public fun Buffer.assertContainsText(text: String) {
    val snapshot = captureSnapshot()
    assertTrue(text in snapshot, "Expected '$text' in buffer but not found. \nBuffer:\n$snapshot")
}

/**
 * Asserts the state of a specific cell in the buffer.
 */
public fun Buffer.assertCellAt(
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
    val cell = get(x, y)
    val msg = "At ($x, $y):"

    if (char != null) assertEquals(char, cell.char, "$msg char mismatch")
    if (foreground != null) assertEquals(foreground, cell.foreground, "$msg foreground mismatch")
    if (background != null) assertEquals(background, cell.background, "$msg background mismatch")
    if (bold != null) assertEquals(bold, cell.bold, "$msg bold mismatch")
    if (italic != null) assertEquals(italic, cell.italic, "$msg italic mismatch")
    if (underline != null) assertEquals(underline, cell.underline, "$msg underline mismatch")
    if (strikethrough != null) assertEquals(strikethrough, cell.strikethrough, "$msg strikethrough mismatch")
}
