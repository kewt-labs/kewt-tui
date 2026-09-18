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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BufferWideCharTest {
    @Test
    fun wideCharacterOccupiesTwoCells() {
        val buf = Buffer(10, 2)
        buf.writeString(0, 0, "你好")
        assertEquals('你', buf.get(0, 0).char)
        assertTrue(buf.get(1, 0).continuation)
        assertEquals('好', buf.get(2, 0).char)
        assertTrue(buf.get(3, 0).continuation)
    }

    @Test
    fun narrowOverwriteClearsContinuation() {
        val buf = Buffer(10, 2)
        buf.writeString(0, 0, "你")
        buf.writeString(0, 0, "ab")
        assertEquals('a', buf.get(0, 0).char)
        assertEquals('b', buf.get(1, 0).char)
        assertFalse(buf.get(1, 0).continuation)
    }

    @Test
    fun writingIntoSecondHalfBlanksParent() {
        val buf = Buffer(10, 2)
        buf.writeString(0, 0, "你")
        buf.writeString(1, 0, "x")
        // The orphaned leading half is blanked, like terminals do
        assertEquals(' ', buf.get(0, 0).char)
        assertEquals('x', buf.get(1, 0).char)
    }

    @Test
    fun astralCharacterStoredAsSurrogatePair() {
        val buf = Buffer(10, 2)
        buf.writeString(0, 0, "\uD83D\uDE00") // 😀
        assertTrue(buf.get(0, 0).char.isHighSurrogate())
        assertTrue(buf.get(1, 0).char.isLowSurrogate())
        assertTrue(buf.get(1, 0).continuation)
    }

    @Test
    fun combiningCharactersAreSkipped() {
        val buf = Buffer(10, 2)
        buf.writeString(0, 0, "e\u0301x")
        assertEquals('e', buf.get(0, 0).char)
        assertEquals('x', buf.get(1, 0).char)
    }

    @Test
    fun wideCharacterAtEdgeIsTruncated() {
        val buf = Buffer(3, 1)
        buf.writeString(2, 0, "你")
        assertEquals(' ', buf.get(2, 0).char)
    }

    @Test
    fun clipRestrictsWrites() {
        val buf = Buffer(6, 1)
        buf.clip = Rect(2, 0, 2, 1)
        buf.writeString(0, 0, "abcdef")
        assertEquals(' ', buf.get(0, 0).char)
        assertEquals(' ', buf.get(1, 0).char)
        assertEquals('c', buf.get(2, 0).char)
        assertEquals('d', buf.get(3, 0).char)
        assertEquals(' ', buf.get(4, 0).char)
    }

    @Test
    fun clipSkipsSplitWideCharacter() {
        val buf = Buffer(6, 1)
        buf.clip = Rect(1, 0, 1, 1)
        buf.writeString(0, 0, "你")
        // Neither half fits fully inside the clip, so nothing is written
        assertEquals(' ', buf.get(0, 0).char)
        assertEquals(' ', buf.get(1, 0).char)
    }

    @Test
    fun fillRectAppliesStyles() {
        val buf = Buffer(5, 2)
        buf.fillRect(1, 0, 2, 2, '*', foreground = Color.Red)
        assertEquals('*', buf.get(1, 0).char)
        assertEquals(Color.Red, buf.get(2, 1).foreground)
        assertEquals(' ', buf.get(0, 0).char)
        assertEquals(' ', buf.get(3, 0).char)
    }

    @Test
    fun fillCoversWholeBuffer() {
        val buf = Buffer(3, 3)
        buf.writeString(0, 0, "abc")
        buf.fill()
        assertEquals(' ', buf.get(0, 0).char)
    }

    @Test
    fun setStyleKeepsCharacters() {
        val buf = Buffer(4, 1)
        buf.writeString(0, 0, "abcd")
        buf.setStyle(Rect(1, 0, 2, 1), background = Color.Blue)
        assertEquals('b', buf.get(1, 0).char)
        assertEquals(Color.Blue, buf.get(2, 0).background)
        assertEquals(Color.Default, buf.get(0, 0).background)
    }

    @Test
    fun mergeCopiesCells() {
        val src = Buffer(2, 1)
        src.writeString(0, 0, "hi", foreground = Color.Green)
        val dst = Buffer(5, 3)
        dst.merge(src, 2, 1)
        assertEquals('h', dst.get(2, 1).char)
        assertEquals(Color.Green, dst.get(3, 1).foreground)
        assertEquals(' ', dst.get(0, 0).char)
    }

    @Test
    fun resizePreservesOverlap() {
        val buf = Buffer(4, 2)
        buf.writeString(0, 0, "abcd")
        val resized = buf.resize(2, 2)
        assertEquals('a', resized.get(0, 0).char)
        assertEquals('b', resized.get(1, 0).char)
        assertEquals(2, resized.width)
    }

    @Test
    fun newAttributesRoundTrip() {
        val buf = Buffer(2, 1)
        buf.setChar(0, 0, 'x', dim = true, blink = true, reverse = true, hidden = true)
        val cell = buf.get(0, 0)
        assertTrue(cell.dim)
        assertTrue(cell.blink)
        assertTrue(cell.reverse)
        assertTrue(cell.hidden)
    }
}
