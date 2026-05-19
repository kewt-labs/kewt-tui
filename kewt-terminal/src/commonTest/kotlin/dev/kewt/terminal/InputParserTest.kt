/*
* Copyright 2026 lscythe
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

class InputParserTest {
    private fun parse(vararg bytes: Int): Event? {
        val parser = InputParser()
        parser.feed(ByteArray(bytes.size) { bytes[it].toByte() }, bytes.size)
        return parser.next()
    }

    @Test
    fun regularChar() {
        val event = parse('a'.code)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Char('a'), event.key)
    }

    @Test
    fun enter() {
        val event = parse(0x0d)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Enter, event.key)
    }

    @Test
    fun backspace() {
        val event = parse(0x7f)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Backspace, event.key)
    }

    @Test
    fun tab() {
        val event = parse(0x09)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Tab, event.key)
    }

    @Test
    fun escape() {
        val event = parse(0x1b)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Escape, event.key)
    }

    @Test
    fun arrowUp() {
        val event = parse(0x1b, '['.code, 'A'.code)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Up, event.key)
    }

    @Test
    fun arrowDown() {
        val event = parse(0x1b, '['.code, 'B'.code)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Down, event.key)
    }

    @Test
    fun delete() {
        val event = parse(0x1b, '['.code, '3'.code, '~'.code)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Delete, event.key)
    }

    @Test
    fun f1() {
        val event = parse(0x1b, 'O'.code, 'P'.code)
        assertIs<KeyEvent>(event)
        assertEquals(Key.F(1), event.key)
    }

    @Test
    fun ctrlC() {
        val event = parse(3)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Char('c'), event.key)
        assertEquals(setOf(KeyModifier.Ctrl), event.modifiers)
    }

    @Test
    fun backTab() {
        val event = parse(0x1b, '['.code, 'Z'.code)
        assertIs<KeyEvent>(event)
        assertEquals(Key.BackTab, event.key)
    }
}
