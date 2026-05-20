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

class BufferTest {
    @Test
    fun defaultCellIsSpace() {
        val buf = Buffer(10, 5)
        assertEquals(' ', buf.get(0, 0).char)
    }

    @Test
    fun setChar() {
        val buf = Buffer(10, 5)
        buf.setChar(2, 1, 'X', foreground = Color.Red)
        assertEquals('X', buf.get(2, 1).char)
        assertEquals(Color.Red, buf.get(2, 1).foreground)
    }

    @Test
    fun writeString() {
        val buf = Buffer(10, 5)
        buf.writeString(0, 0, "Hi", foreground = Color.Cyan, bold = true)
        assertEquals('H', buf.get(0, 0).char)
        assertEquals('i', buf.get(1, 0).char)
        assertEquals(Color.Cyan, buf.get(0, 0).foreground)
        assertEquals(true, buf.get(0, 0).bold)
    }

    @Test
    fun outOfBoundsIgnored() {
        val buf = Buffer(5, 5)
        buf.setChar(-1, 0, 'X')
        buf.setChar(5, 0, 'X')
        assertEquals(' ', buf.get(0, 0).char)
    }

    @Test
    fun clear() {
        val buf = Buffer(5, 5)
        buf.setChar(0, 0, 'A')
        buf.clear()
        assertEquals(' ', buf.get(0, 0).char)
    }
}
