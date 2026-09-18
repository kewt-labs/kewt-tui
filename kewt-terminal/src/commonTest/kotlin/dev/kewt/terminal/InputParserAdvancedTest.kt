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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InputParserAdvancedTest {
    private fun parser(vararg bytes: Int): InputParser {
        val parser = InputParser()
        parser.feed(ByteArray(bytes.size) { bytes[it].toByte() }, bytes.size)
        return parser
    }

    private fun parse(vararg bytes: Int): Event? = parser(*bytes).next()

    @Test
    fun utf8TwoByteCharacter() {
        val event = parse(0xC3, 0xA9) // é
        assertIs<KeyEvent>(event)
        assertEquals(Key.Char('é'), event.key)
    }

    @Test
    fun utf8ThreeByteCharacter() {
        val event = parse(0xE4, 0xBD, 0xA0) // 你
        assertIs<KeyEvent>(event)
        assertEquals(Key.Char('你'), event.key)
    }

    @Test
    fun utf8FourByteCharacterBecomesTextKey() {
        val event = parse(0xF0, 0x9F, 0x98, 0x80) // 😀
        assertIs<KeyEvent>(event)
        assertEquals(Key.Text("\uD83D\uDE00"), event.key)
    }

    @Test
    fun invalidUtf8LeadByteIsDropped() {
        val event = parse(0x80)
        assertNull(event)
    }

    @Test
    fun ctrlSpace() {
        val event = parse(0x00)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Char(' '), event.key)
        assertEquals(setOf(KeyModifier.Ctrl), event.modifiers)
    }

    @Test
    fun lineFeedIsEnter() {
        val event = parse(0x0A)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Enter, event.key)
    }

    @Test
    fun ctrlBracket() {
        val event = parse(29)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Char(']'), event.key)
        assertEquals(setOf(KeyModifier.Ctrl), event.modifiers)
    }

    @Test
    fun insertKey() {
        val event = parse(0x1b, '['.code, '2'.code, '~'.code)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Insert, event.key)
    }

    @Test
    fun homeAndEndTildeVariants() {
        val home = parse(0x1b, '['.code, '1'.code, '~'.code)
        assertIs<KeyEvent>(home)
        assertEquals(Key.Home, home.key)

        val end = parse(0x1b, '['.code, '4'.code, '~'.code)
        assertIs<KeyEvent>(end)
        assertEquals(Key.End, end.key)
    }

    @Test
    fun functionKeysFiveAndAbove() {
        val f5 = parse(0x1b, '['.code, '1'.code, '5'.code, '~'.code)
        assertIs<KeyEvent>(f5)
        assertEquals(Key.F(5), f5.key)

        val f13 = parse(0x1b, '['.code, '2'.code, '5'.code, '~'.code)
        assertIs<KeyEvent>(f13)
        assertEquals(Key.F(13), f13.key)
    }

    @Test
    fun f1ThroughF4ViaTildeCodes() {
        val f1 = parse(0x1b, '['.code, '1'.code, '1'.code, '~'.code)
        assertIs<KeyEvent>(f1)
        assertEquals(Key.F(1), f1.key)
    }

    @Test
    fun kittyProtocolCtrlA() {
        val event = parse(0x1b, '['.code, '9'.code, '7'.code, ';'.code, '5'.code, 'u'.code)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Char('a'), event.key)
        assertEquals(setOf(KeyModifier.Ctrl), event.modifiers)
    }

    @Test
    fun kittyProtocolShiftTabIsBackTab() {
        val event = parse(0x1b, '['.code, '9'.code, ';'.code, '2'.code, 'u'.code)
        assertIs<KeyEvent>(event)
        assertEquals(Key.BackTab, event.key)
    }

    @Test
    fun sgrMouseLeftDown() {
        val event = parse(0x1b, '['.code, '<'.code, '0'.code, ';'.code, '5'.code, ';'.code, '7'.code, 'M'.code)
        assertIs<MouseEvent>(event)
        assertEquals(4, event.x)
        assertEquals(6, event.y)
        assertEquals(MouseKind.Down(MouseButton.Left), event.kind)
    }

    @Test
    fun sgrMouseRelease() {
        val event = parse(0x1b, '['.code, '<'.code, '0'.code, ';'.code, '1'.code, ';'.code, '1'.code, 'm'.code)
        assertIs<MouseEvent>(event)
        assertEquals(MouseKind.Up(MouseButton.Left), event.kind)
    }

    @Test
    fun sgrMouseWheelAndModifiers() {
        val wheel = parse(0x1b, '['.code, '<'.code, '6'.code, '4'.code, ';'.code, '1'.code, ';'.code, '1'.code, 'M'.code)
        assertIs<MouseEvent>(wheel)
        assertEquals(MouseKind.WheelUp, wheel.kind)

        val ctrlClick =
            parse(0x1b, '['.code, '<'.code, '1'.code, '6'.code, ';'.code, '2'.code, ';'.code, '3'.code, 'M'.code)
        assertIs<MouseEvent>(ctrlClick)
        assertEquals(MouseKind.Down(MouseButton.Left), ctrlClick.kind)
        assertEquals(setOf(KeyModifier.Ctrl), ctrlClick.modifiers)
    }

    @Test
    fun sgrMouseDragAndMove() {
        val drag = parse(0x1b, '['.code, '<'.code, '3'.code, '2'.code, ';'.code, '1'.code, ';'.code, '1'.code, 'M'.code)
        assertIs<MouseEvent>(drag)
        assertEquals(MouseKind.Drag(MouseButton.Left), drag.kind)

        val move = parse(0x1b, '['.code, '<'.code, '3'.code, '5'.code, ';'.code, '1'.code, ';'.code, '1'.code, 'M'.code)
        assertIs<MouseEvent>(move)
        assertEquals(MouseKind.Moved, move.kind)
    }

    @Test
    fun legacyX10Mouse() {
        // ESC [ M followed by cb=32 (left button), cx=33 (column 1), cy=34 (row 2)
        val event = parse(0x1b, '['.code, 'M'.code, 32, 33, 34)
        assertIs<MouseEvent>(event)
        assertEquals(0, event.x)
        assertEquals(1, event.y)
        assertEquals(MouseKind.Down(MouseButton.Left), event.kind)
    }

    @Test
    fun focusEvents() {
        val gained = parse(0x1b, '['.code, 'I'.code)
        assertEquals(FocusEvent(true), gained)

        val lost = parse(0x1b, '['.code, 'O'.code)
        assertEquals(FocusEvent(false), lost)
    }

    @Test
    fun bracketedPaste() {
        val parser = InputParser()
        val start = byteArrayOf(0x1b, '['.code.toByte(), '2'.code.toByte(), '0'.code.toByte(), '0'.code.toByte(), '~'.code.toByte())
        parser.feed(start, start.size)
        // Entering paste mode does not produce an event yet
        assertNull(parser.next())

        val payload = "hi".encodeToByteArray()
        parser.feed(payload, payload.size)
        assertNull(parser.next())

        val end = byteArrayOf(0x1b, '['.code.toByte(), '2'.code.toByte(), '0'.code.toByte(), '1'.code.toByte(), '~'.code.toByte())
        parser.feed(end, end.size)
        val event = parser.next()
        assertIs<PasteEvent>(event)
        assertEquals("hi", event.text)
    }

    @Test
    fun bracketedPasteWithEscapeInContent() {
        val parser = InputParser()
        val bytes =
            byteArrayOf(
                0x1b,
                '['.code.toByte(),
                '2'.code.toByte(),
                '0'.code.toByte(),
                '0'.code.toByte(),
                '~'.code.toByte(),
                'a'.code.toByte(),
                0x1b,
                'b'.code.toByte(),
                0x1b,
                '['.code.toByte(),
                '2'.code.toByte(),
                '0'.code.toByte(),
                '1'.code.toByte(),
                '~'.code.toByte(),
            )
        parser.feed(bytes, bytes.size)
        assertNull(parser.next()) // enters paste mode
        val event = parser.next()
        assertIs<PasteEvent>(event)
        assertEquals("a\u001bb", event.text)
    }

    @Test
    fun bracketedPasteDecodesUtf8() {
        val parser = InputParser()
        val start = byteArrayOf(0x1b, '['.code.toByte(), '2'.code.toByte(), '0'.code.toByte(), '0'.code.toByte(), '~'.code.toByte())
        parser.feed(start, start.size)
        parser.next()
        val payload = "你".encodeToByteArray()
        parser.feed(payload, payload.size)
        val end = byteArrayOf(0x1b, '['.code.toByte(), '2'.code.toByte(), '0'.code.toByte(), '1'.code.toByte(), '~'.code.toByte())
        parser.feed(end, end.size)
        val event = parser.next()
        assertIs<PasteEvent>(event)
        assertEquals("你", event.text)
    }

    @Test
    fun ss3ArrowKeys() {
        val up = parse(0x1b, 'O'.code, 'A'.code)
        assertIs<KeyEvent>(up)
        assertEquals(Key.Up, up.key)
    }

    @Test
    fun altChar() {
        val event = parse(0x1b, 'x'.code)
        assertIs<KeyEvent>(event)
        assertEquals(Key.Char('x'), event.key)
        assertEquals(setOf(KeyModifier.Alt), event.modifiers)
    }

    @Test
    fun unknownCsiIsConsumedWithoutEvent() {
        val parser = parser(0x1b, '['.code, '9'.code, '9'.code, 'q'.code, 'z'.code)
        assertNull(parser.next())
        val event = parser.next()
        assertIs<KeyEvent>(event)
        assertEquals(Key.Char('z'), event.key)
    }

    @Test
    fun hasEventsReflectsBuffer() {
        val parser = InputParser()
        assertFalse(parser.hasEvents)
        parser.feed(byteArrayOf('a'.code.toByte()), 1)
        assertTrue(parser.hasEvents)
    }

    @Test
    fun resetDropsBufferedBytes() {
        val parser = parser('a'.code, 'b'.code)
        parser.reset()
        assertNull(parser.next())
        assertFalse(parser.hasEvents)
    }
}
