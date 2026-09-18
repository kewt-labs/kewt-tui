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
import dev.kewt.terminal.ColorMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BufferDiffStyleTest {
    @Test
    fun rgbBackgroundUsesBackgroundSequence() {
        val current = Buffer(3, 1)
        val previous = Buffer(3, 1)
        current.setChar(0, 0, 'X', background = Color.RGB(1, 2, 3))
        val output = BufferDiff().diff(current, previous)
        assertTrue(output.contains("\u001b[48;2;1;2;3m"), "Expected 48;2 background sequence in: $output")
        assertFalse(output.contains("\u001b[38;2;1;2;3m"), "Foreground sequence must not be used for background")
    }

    @Test
    fun rgbForegroundUsesForegroundSequence() {
        val current = Buffer(3, 1)
        val previous = Buffer(3, 1)
        current.setChar(0, 0, 'X', foreground = Color.RGB(9, 8, 7))
        val output = BufferDiff().diff(current, previous)
        assertTrue(output.contains("\u001b[38;2;9;8;7m"))
    }

    @Test
    fun dimAndBlinkAndReverseAndHiddenSequences() {
        val current = Buffer(4, 1)
        val previous = Buffer(4, 1)
        current.setChar(0, 0, 'a', dim = true)
        current.setChar(1, 0, 'b', blink = true)
        current.setChar(2, 0, 'c', reverse = true)
        current.setChar(3, 0, 'd', hidden = true)
        val output = BufferDiff().diff(current, previous)
        assertTrue(output.contains("\u001b[2m"))
        assertTrue(output.contains("\u001b[5m"))
        assertTrue(output.contains("\u001b[7m"))
        assertTrue(output.contains("\u001b[8m"))
    }

    @Test
    fun attributeTurnOffSequences() {
        val current = Buffer(4, 1)
        val previous = Buffer(4, 1)
        previous.setChar(0, 0, 'a', reverse = true, underline = true)
        current.setChar(0, 0, 'a')
        val output = BufferDiff().diff(current, previous)
        assertTrue(output.contains("\u001b[27m"))
        assertTrue(output.contains("\u001b[24m"))
    }

    @Test
    fun boldToDimResetsSharedCode() {
        val current = Buffer(2, 1)
        val previous = Buffer(2, 1)
        previous.setChar(0, 0, 'a', bold = true)
        current.setChar(0, 0, 'a', dim = true)
        val output = BufferDiff().diff(current, previous)
        val resetIndex = output.indexOf("\u001b[22m")
        val dimIndex = output.indexOf("\u001b[2m")
        assertTrue(resetIndex >= 0, "Expected shared reset code 22")
        assertTrue(dimIndex > resetIndex, "Dim must be re-enabled after the shared reset")
    }

    @Test
    fun differentSizesDoNotCrash() {
        val current = Buffer(5, 5)
        val previous = Buffer(4, 4)
        current.writeString(1, 1, "hello")
        val output = BufferDiff().diff(current, previous)
        assertTrue(output.contains("hello"))
    }

    @Test
    fun wideCharacterIsEmittedOnceWithPairAdvance() {
        val current = Buffer(6, 1)
        val previous = Buffer(6, 1)
        current.writeString(0, 0, "你")
        current.setChar(2, 0, 'X')
        val output = BufferDiff().diff(current, previous)
        // One move to (1,1), the wide char, then X without an intermediate move
        assertTrue(output.startsWith("\u001b[1;1H"))
        assertTrue(output.contains("你"))
        assertTrue(output.contains("X"))
        assertFalse(output.contains("\u001b[1;2H"), "Continuation cell must not be addressed directly")
        assertFalse(output.contains("\u001b[1;3H"), "Cursor should continue directly after the wide char")
    }

    @Test
    fun astralPairEmitsBothSurrogates() {
        val current = Buffer(6, 1)
        val previous = Buffer(6, 1)
        current.writeString(0, 0, "\uD83D\uDE00")
        val output = BufferDiff().diff(current, previous)
        assertTrue(output.contains("\uD83D\uDE00"))
    }

    @Test
    fun noColorModeStripsColors() {
        val current = Buffer(2, 1)
        val previous = Buffer(2, 1)
        current.setChar(0, 0, 'X', foreground = Color.RGB(10, 20, 30))
        val output = BufferDiff(ColorMode.NoColor).diff(current, previous)
        assertFalse(output.contains("38;2"))
        assertTrue(output.contains("X"))
    }

    @Test
    fun basicModeDowngradesTo16Colors() {
        val current = Buffer(2, 1)
        val previous = Buffer(2, 1)
        current.setChar(0, 0, 'X', foreground = Color.RGB(255, 0, 0))
        val output = BufferDiff(ColorMode.Basic).diff(current, previous)
        // Bright red = 91
        assertTrue(output.contains("\u001b[91m"), "Expected downgraded bright red, got: $output")
    }

    @Test
    fun extendedModeDowngradesTo256Colors() {
        val current = Buffer(2, 1)
        val previous = Buffer(2, 1)
        current.setChar(0, 0, 'X', foreground = Color.RGB(255, 255, 255))
        val output = BufferDiff(ColorMode.Extended).diff(current, previous)
        assertTrue(output.contains("\u001b[38;5;231m"))
    }

    @Test
    fun unchangedCellsProduceNoOutput() {
        val a = Buffer(3, 3)
        val b = Buffer(3, 3)
        a.writeString(0, 0, "你")
        b.writeString(0, 0, "你")
        assertEquals("", BufferDiff().diff(a, b))
    }
}
