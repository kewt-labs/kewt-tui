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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BufferDiffTest {
    @Test
    fun identicalBuffersProduceEmptyDiff() {
        val a = Buffer(5, 3)
        val b = Buffer(5, 3)
        val diff = BufferDiff()
        assertEquals("", diff.diff(a, b))
    }

    @Test
    fun singleCellChangeProducesOutput() {
        val current = Buffer(5, 3)
        val previous = Buffer(5, 3)
        current.setChar(2, 1, 'X')
        val diff = BufferDiff()
        val output = diff.diff(current, previous)
        assertTrue(output.isNotEmpty())
        assertTrue(output.contains("X"))
    }

    @Test
    fun diffContainsCursorMove() {
        val current = Buffer(10, 5)
        val previous = Buffer(10, 5)
        current.setChar(5, 3, 'A')
        val diff = BufferDiff()
        val output = diff.diff(current, previous)
        // CSI row;colH — row=4 (1-indexed), col=6 (1-indexed)
        assertTrue(output.contains("\u001b[4;6H"))
    }
}
