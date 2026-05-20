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
package dev.kewt.modifier

import kotlin.test.Test
import kotlin.test.assertEquals

class ColorPackingTest {
    @Test
    fun ansi16Packing() {
        for (i in 0..15) {
            val original = Color.Ansi16(i)
            val packed = Color.pack(original)
            val unpacked = Color.unpack(packed)
            assertEquals(original, unpacked, "Failed for ANSI 16 code $i")
        }
    }

    @Test
    fun ansi256Packing() {
        for (i in 0..255) {
            val original = Color.Ansi256(i)
            val packed = Color.pack(original)
            val unpacked = Color.unpack(packed)
            assertEquals(original, unpacked, "Failed for ANSI 256 code $i")
        }
    }

    @Test
    fun rgbPacking() {
        val colors = listOf(
            Color.RGB(255, 0, 0),
            Color.RGB(0, 255, 0),
            Color.RGB(0, 0, 255),
            Color.RGB(123, 45, 67),
            Color.RGB(0xABCDEF),
        )
        for (original in colors) {
            val packed = Color.pack(original)
            val unpacked = Color.unpack(packed)
            assertEquals(original, unpacked, "Failed for RGB color $original")
        }
    }

    @Test
    fun defaultPacking() {
        val original = Color.Default
        val packed = Color.pack(original)
        val unpacked = Color.unpack(packed)
        assertEquals(original, unpacked)
    }
}
