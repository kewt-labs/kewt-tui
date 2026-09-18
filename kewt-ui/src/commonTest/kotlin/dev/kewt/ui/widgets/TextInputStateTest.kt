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
package dev.kewt.ui.widgets

import dev.kewt.terminal.Key
import dev.kewt.terminal.KeyEvent
import dev.kewt.terminal.KeyModifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextInputStateTest {
    private fun ctrl(c: Char) = KeyEvent(Key.Char(c), setOf(KeyModifier.Ctrl))

    @Test
    fun startsWithCursorAtEnd() {
        val state = TextInputState("abc")
        assertEquals("abc", state.text.value)
        assertEquals(3, state.cursor.value)
    }

    @Test
    fun insertAppendsAtCursor() {
        val state = TextInputState("ac")
        state.cursor.value = 1
        state.insert("b")
        assertEquals("abc", state.text.value)
        assertEquals(2, state.cursor.value)
    }

    @Test
    fun charKeyInserts() {
        val state = TextInputState()
        assertTrue(state.handleKey(KeyEvent(Key.Char('x'))))
        assertTrue(state.handleKey(KeyEvent(Key.Char('y'))))
        assertEquals("xy", state.text.value)
    }

    @Test
    fun textKeyInsertsSupplementaryCharacters() {
        val state = TextInputState()
        assertTrue(state.handleKey(KeyEvent(Key.Text("\uD83D\uDE42"))))
        assertEquals("\uD83D\uDE42", state.text.value)
        assertEquals(2, state.cursor.value)
    }

    @Test
    fun backspaceRemovesCharacterBeforeCursor() {
        val state = TextInputState("abc")
        assertTrue(state.handleKey(KeyEvent(Key.Backspace)))
        assertEquals("ab", state.text.value)
        assertEquals(2, state.cursor.value)
    }

    @Test
    fun backspaceAtStartIsNoOp() {
        val state = TextInputState("abc")
        state.cursor.value = 0
        state.backspace()
        assertEquals("abc", state.text.value)
    }

    @Test
    fun deleteRemovesCharacterAfterCursor() {
        val state = TextInputState("abc")
        state.cursor.value = 1
        assertTrue(state.handleKey(KeyEvent(Key.Delete)))
        assertEquals("ac", state.text.value)
        assertEquals(1, state.cursor.value)
    }

    @Test
    fun deleteAtEndIsNoOp() {
        val state = TextInputState("abc")
        state.delete()
        assertEquals("abc", state.text.value)
    }

    @Test
    fun arrowKeysMoveCursor() {
        val state = TextInputState("abc")
        state.handleKey(KeyEvent(Key.Left))
        assertEquals(2, state.cursor.value)
        state.handleKey(KeyEvent(Key.Right))
        assertEquals(3, state.cursor.value)
        state.handleKey(KeyEvent(Key.Right))
        assertEquals(3, state.cursor.value)
    }

    @Test
    fun homeAndEndJumpToEdges() {
        val state = TextInputState("abc")
        assertTrue(state.handleKey(KeyEvent(Key.Home)))
        assertEquals(0, state.cursor.value)
        assertTrue(state.handleKey(KeyEvent(Key.End)))
        assertEquals(3, state.cursor.value)
    }

    @Test
    fun ctrlShortcutsMirrorEmacsBindings() {
        val state = TextInputState("hello")
        state.handleKey(ctrl('a'))
        assertEquals(0, state.cursor.value)
        state.handleKey(ctrl('e'))
        assertEquals(5, state.cursor.value)
        state.handleKey(ctrl('b'))
        assertEquals(4, state.cursor.value)
        state.handleKey(ctrl('f'))
        assertEquals(5, state.cursor.value)
        state.handleKey(ctrl('h'))
        assertEquals("hell", state.text.value)
        state.cursor.value = 2
        state.handleKey(ctrl('d'))
        assertEquals("hel", state.text.value)
    }

    @Test
    fun ctrlUDeletesToStart() {
        val state = TextInputState("hello")
        state.cursor.value = 2
        assertTrue(state.handleKey(ctrl('u')))
        assertEquals("llo", state.text.value)
        assertEquals(0, state.cursor.value)
    }

    @Test
    fun ctrlKDeletesToEnd() {
        val state = TextInputState("hello")
        state.cursor.value = 2
        assertTrue(state.handleKey(ctrl('k')))
        assertEquals("he", state.text.value)
        assertEquals(2, state.cursor.value)
    }

    @Test
    fun ctrlWDeletesWordBeforeCursor() {
        val state = TextInputState("hello world")
        assertTrue(state.handleKey(ctrl('w')))
        assertEquals("hello ", state.text.value)
        assertEquals(6, state.cursor.value)
        state.handleKey(ctrl('w'))
        assertEquals("", state.text.value)
        assertEquals(0, state.cursor.value)
    }

    @Test
    fun unknownCtrlCharIsNotConsumed() {
        val state = TextInputState("x")
        assertFalse(state.handleKey(ctrl('z')))
        assertEquals("x", state.text.value)
    }

    @Test
    fun modifiedCharIsNotConsumed() {
        val state = TextInputState()
        assertFalse(state.handleKey(KeyEvent(Key.Char('x'), setOf(KeyModifier.Shift))))
        assertEquals("", state.text.value)
    }

    @Test
    fun maxLengthRejectsFurtherInput() {
        val state = TextInputState("abc", maxLength = 5)
        state.insert("de")
        assertEquals("abcde", state.text.value)
        state.insert("f")
        assertEquals("abcde", state.text.value)
        state.handleKey(KeyEvent(Key.Char('g')))
        assertEquals("abcde", state.text.value)
    }

    @Test
    fun initialTextIsClampedToMaxLength() {
        val state = TextInputState("abcdef", maxLength = 3)
        assertEquals("abc", state.text.value)
    }

    @Test
    fun setTextClampsAndResetsCursor() {
        val state = TextInputState("", maxLength = 3)
        state.setText("abcdef")
        assertEquals("abc", state.text.value)
        assertEquals(3, state.cursor.value)
    }

    @Test
    fun enterInvokesOnSubmit() {
        val state = TextInputState("cmd")
        var submitted: String? = null
        state.onSubmit = { submitted = it }
        assertTrue(state.handleKey(KeyEvent(Key.Enter)))
        assertEquals("cmd", submitted)
    }

    @Test
    fun mutationsFireOnChange() {
        val state = TextInputState()
        val seen = mutableListOf<String>()
        state.onChange = { seen.add(it) }
        state.handleKey(KeyEvent(Key.Char('a')))
        state.handleKey(KeyEvent(Key.Backspace))
        assertEquals(listOf("a", ""), seen)
    }

    @Test
    fun backspaceRemovesWholeSurrogatePair() {
        val state = TextInputState("a\uD83D\uDE42")
        assertEquals(3, state.cursor.value)
        state.backspace()
        assertEquals("a", state.text.value)
        assertEquals(1, state.cursor.value)
    }

    @Test
    fun moveLeftStepsOverSurrogatePair() {
        val state = TextInputState("a\uD83D\uDE42")
        state.moveLeft()
        assertEquals(1, state.cursor.value)
    }

    @Test
    fun navigationKeysAreIgnoredForSelection() {
        val state = TextInputState("abc")
        assertFalse(state.handleKey(KeyEvent(Key.Up)))
        assertFalse(state.handleKey(KeyEvent(Key.Tab)))
    }
}
