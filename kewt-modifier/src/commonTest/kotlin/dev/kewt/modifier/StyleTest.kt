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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StyleTest {
    @Test
    fun mergeOverridesOnlySpecifiedProperties() {
        val base = Style(foreground = Color.Red, bold = true)
        val overlay = Style(foreground = Color.Blue)
        val merged = base.merge(overlay)
        assertEquals(Color.Blue, merged.foreground)
        assertEquals(true, merged.bold)
        assertNull(merged.background)
    }

    @Test
    fun mergeCanTurnAttributesOff() {
        val base = Style(bold = true)
        val merged = base.merge(Style(bold = false))
        assertEquals(false, merged.bold)
    }

    @Test
    fun resolveStyleLastElementWins() {
        val style = Modifier
            .foreground(Color.Red)
            .foreground(Color.Blue)
            .resolveStyle()
        assertEquals(Color.Blue, style.foreground)
    }

    @Test
    fun resolveStyleAccumulatesAttributes() {
        val style = Modifier
            .bold()
            .dim()
            .underline()
            .reverse()
            .background(Color.Green)
            .resolveStyle()
        assertEquals(true, style.bold)
        assertEquals(true, style.dim)
        assertEquals(true, style.underline)
        assertEquals(true, style.reverse)
        assertEquals(Color.Green, style.background)
    }

    @Test
    fun resolveStyleFromStyleModifier() {
        val style = Modifier
            .foreground(Color.Red)
            .style(Style(background = Color.Blue, italic = true))
            .resolveStyle()
        assertEquals(Color.Red, style.foreground)
        assertEquals(Color.Blue, style.background)
        assertEquals(true, style.italic)
    }

    @Test
    fun emptyModifierResolvesToEmptyStyle() {
        assertEquals(Style.Empty, Modifier.resolveStyle())
    }

    @Test
    fun hexStringParsing() {
        assertEquals(Color.RGB(255, 136, 0), Color.fromHex("#FF8800"))
        assertEquals(Color.RGB(255, 136, 0), Color.fromHex("FF8800"))
        assertEquals(Color.RGB(255, 255, 255), Color.fromHex("#FFF"))
        assertNull(Color.fromHex("nope"))
        assertNull(Color.fromHex("#12345"))
    }

    @Test
    fun rgbComponentsAreClamped() {
        val packed = Color.pack(Color.RGB(300, -5, 0))
        assertEquals(Color.RGB(255, 0, 0), Color.unpack(packed))
    }

    @Test
    fun grayRamp() {
        assertEquals(Color.Ansi256(232), Color.gray(0))
        assertEquals(Color.Ansi256(255), Color.gray(23))
        assertEquals(Color.Ansi256(232), Color.gray(-10))
    }

    @Test
    fun ansiFactoriesCoerce() {
        assertEquals(Color.Ansi16(15), Color.ansi16(99))
        assertEquals(Color.Ansi256(0), Color.ansi256(-3))
    }

    @Test
    fun sizeModifierSetsBothDimensions() {
        val flat = Modifier.size(3, 4).flatten()
        assertEquals(2, flat.size)
        assertTrue(flat[0] is WidthModifier)
        assertTrue(flat[1] is HeightModifier)
    }

    @Test
    fun borderModifierCarriesTitle() {
        val border = Modifier
            .border(BorderStyle.Rounded, Color.Cyan, "Title", HorizontalAlignment.Center)
            .findElement<BorderModifier>()
        assertEquals("Title", border?.title)
        assertEquals(HorizontalAlignment.Center, border?.titleAlignment)
    }
}
