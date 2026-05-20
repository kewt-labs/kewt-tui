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
import dev.kewt.terminal.Key
import dev.kewt.terminal.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestTerminalTest {
    @Test
    fun sendKeyQueuesEvent() {
        val t = TestTerminal()
        t.sendChar('x')
        val event = t.read()
        assertIs<KeyEvent>(event)
        assertEquals(Key.Char('x'), event.key)
    }

    @Test
    fun readReturnsNullWhenEmpty() {
        val t = TestTerminal()
        assertNull(t.read())
    }

    @Test
    fun sizeReturnsConfigured() {
        val t = TestTerminal(40, 10)
        assertEquals(40, t.size().width)
        assertEquals(10, t.size().height)
    }

    @Test
    fun cursorTracking() {
        val t = TestTerminal()
        t.moveCursor(5, 10)
        assertEquals(5, t.cursorX)
        assertEquals(10, t.cursorY)
    }

    @Test
    fun titleTracking() {
        val t = TestTerminal()
        t.setTitle("New Title")
        assertEquals("New Title", t.windowTitle)
    }

    @Test
    fun resizeUpdatesSize() {
        val t = TestTerminal(80, 24)
        t.resize(100, 30)
        assertEquals(100, t.size().width)
        assertEquals(30, t.size().height)
    }

    @Test
    fun pollChecksEventQueue() {
        val t = TestTerminal()
        assertFalse(t.poll(0))
        t.sendChar('a')
        assertTrue(t.poll(0))
        t.read()
        assertFalse(t.poll(0))
    }
}

class AssertionsTest {
    @Test
    fun captureSnapshotWorks() {
        val buf = Buffer(5, 2)
        buf.writeString(0, 0, "Hello")
        buf.writeString(0, 1, "World")
        val snap = buf.captureSnapshot()
        assertTrue("Hello" in snap)
        assertTrue("World" in snap)
    }

    @Test
    fun assertContainsTextPasses() {
        val buf = Buffer(10, 1)
        buf.writeString(0, 0, "test")
        buf.assertContainsText("test")
    }

    @Test
    fun assertCellAtPasses() {
        val buf = Buffer(10, 1)
        buf.setChar(
            x = 0,
            y = 0,
            char = 'A',
            foreground = Color.Red,
            background = Color.Blue,
            bold = true,
            italic = true,
            underline = true,
            strikethrough = true,
        )

        buf.assertCellAt(
            x = 0,
            y = 0,
            char = 'A',
            foreground = Color.Red,
            background = Color.Blue,
            bold = true,
            italic = true,
            underline = true,
            strikethrough = true,
        )
    }
}
