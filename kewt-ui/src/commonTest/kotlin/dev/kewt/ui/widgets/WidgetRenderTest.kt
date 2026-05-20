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

import dev.kewt.core.buffer.Buffer
import dev.kewt.modifier.BorderStyle
import dev.kewt.modifier.Color
import dev.kewt.test.assertCellAt
import kotlin.test.Test

class TextWidgetTest {
    @Test
    fun rendersTextAtPosition() {
        val buf = Buffer(20, 5)
        renderText(buf, 2, 1, "Hello")
        buf.assertCellAt(2, 1, 'H')
        buf.assertCellAt(3, 1, 'e')
        buf.assertCellAt(6, 1, 'o')
    }

    @Test
    fun rendersWithColor() {
        val buf = Buffer(20, 5)
        renderText(buf, 0, 0, "Hi", fg = Color.Cyan)
        buf.assertCellAt(0, 0, 'H', foreground = Color.Cyan)
    }
}

class BorderWidgetTest {
    @Test
    fun rendersRoundedBorder() {
        val buf = Buffer(10, 4)
        renderBorder(buf, 0, 0, 10, 4, BorderStyle.Rounded)
        buf.assertCellAt(0, 0, '╭')
        buf.assertCellAt(9, 0, '╮')
        buf.assertCellAt(0, 3, '╰')
        buf.assertCellAt(9, 3, '╯')
        buf.assertCellAt(1, 0, '─')
        buf.assertCellAt(0, 1, '│')
    }

    @Test
    fun rendersDoubleBorder() {
        val buf = Buffer(5, 3)
        renderBorder(buf, 0, 0, 5, 3, BorderStyle.Double)
        buf.assertCellAt(0, 0, '╔')
        buf.assertCellAt(4, 2, '╝')
    }
}
