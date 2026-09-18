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

class UnicodeWidthTest {
    @Test
    fun asciiIsSingleWidth() {
        assertEquals(1, UnicodeWidth.of('A'.code))
        assertEquals(1, UnicodeWidth.of('~'.code))
    }

    @Test
    fun cjkIsDoubleWidth() {
        assertEquals(2, UnicodeWidth.of('你'.code))
        assertEquals(2, UnicodeWidth.of('日'.code))
        assertEquals(2, UnicodeWidth.of('한'.code))
    }

    @Test
    fun fullwidthFormsAreDoubleWidth() {
        assertEquals(2, UnicodeWidth.of('Ａ'.code)) // Ａ fullwidth A
    }

    @Test
    fun combiningMarksAreZeroWidth() {
        assertEquals(0, UnicodeWidth.of(0x0301)) // combining acute
        assertEquals(0, UnicodeWidth.of(0x200B)) // zero width space
    }

    @Test
    fun emojiIsDoubleWidth() {
        assertEquals(2, UnicodeWidth.of(0x1F600)) // 😀
    }

    @Test
    fun controlCharactersAreZeroWidth() {
        assertEquals(0, UnicodeWidth.of(0x07))
        assertEquals(0, UnicodeWidth.of(0x7F))
    }

    @Test
    fun displayWidthMixesWidths() {
        assertEquals(4, UnicodeWidth.displayWidth("a你b"))
        assertEquals(5, UnicodeWidth.displayWidth("hello"))
        assertEquals(2, UnicodeWidth.displayWidth("\uD83D\uDE00"))
        assertEquals(1, UnicodeWidth.displayWidth("e\u0301")) // e + combining acute
    }

    @Test
    fun truncateClipsToWidth() {
        assertEquals("hel", UnicodeWidth.truncate("hello", 3))
        assertEquals("hello", UnicodeWidth.truncate("hello", 10))
        assertEquals("", UnicodeWidth.truncate("hello", 0))
    }

    @Test
    fun truncateWithEllipsis() {
        assertEquals("he…", UnicodeWidth.truncate("hello", 3, ellipsis = true))
        assertEquals("hello", UnicodeWidth.truncate("hello", 5, ellipsis = true))
    }

    @Test
    fun truncateRespectsWideCharacters() {
        assertEquals("你", UnicodeWidth.truncate("你好吗", 2))
        assertEquals("你好", UnicodeWidth.truncate("你好吗", 4))
        assertEquals("你…", UnicodeWidth.truncate("你好吗", 3, ellipsis = true))
    }

    @Test
    fun truncateKeepsAstralPairsIntact() {
        val truncated = UnicodeWidth.truncate("\uD83D\uDE00abc", 3)
        assertEquals("\uD83D\uDE00a", truncated)
    }
}
