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
package dev.kewt.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InputParserModifierTest {
    private fun parse(vararg bytes: Int): Event? {
        val parser = InputParser()
        parser.feed(ByteArray(bytes.size) { bytes[it].toByte() }, bytes.size)
        return parser.next()
    }

    @Test
    fun ctrlRightArrow() {
        // ESC [ 1; 5 C
        val event = parse(0x1b, '['.code, '1'.code, ';'.code, '5'.code, 'C'.code)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Right, event.key)
        assertEquals(setOf(KeyModifier.Ctrl), event.modifiers)
    }

    @Test
    fun ctrlLeftArrow() {
        // ESC [ 1; 5 D
        val event = parse(0x1b, '['.code, '1'.code, ';'.code, '5'.code, 'D'.code)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Left, event.key)
        assertEquals(setOf(KeyModifier.Ctrl), event.modifiers)
    }

    @Test
    fun shiftUp() {
        // ESC [ 1; 2 A
        val event = parse(0x1b, '['.code, '1'.code, ';'.code, '2'.code, 'A'.code)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Up, event.key)
        assertEquals(setOf(KeyModifier.Shift), event.modifiers)
    }

    @Test
    fun altDown() {
        // ESC [ 1; 3 B
        val event = parse(0x1b, '['.code, '1'.code, ';'.code, '3'.code, 'B'.code)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Down, event.key)
        assertEquals(setOf(KeyModifier.Alt), event.modifiers)
    }

    @Test
    fun ctrlShiftRight() {
        // ESC [ 1; 6 C (modifier 6 = Ctrl+Shift)
        val event = parse(0x1b, '['.code, '1'.code, ';'.code, '6'.code, 'C'.code)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Right, event.key)
        assertEquals(setOf(KeyModifier.Ctrl, KeyModifier.Shift), event.modifiers)
    }

    @Test
    fun altChar() {
        // ESC followed by a regular char = Alt+char
        val event = parse(0x1b, 'x'.code)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Char('x'), event.key)
        assertEquals(setOf(KeyModifier.Alt), event.modifiers)
    }

    @Test
    fun ctrlDeleteWithModifier() {
        // ESC [ 3; 5 ~ (Ctrl+Delete)
        val event = parse(0x1b, '['.code, '3'.code, ';'.code, '5'.code, '~'.code)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Delete, event.key)
        assertEquals(setOf(KeyModifier.Ctrl), event.modifiers)
    }
}
