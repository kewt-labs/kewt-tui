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
import dev.kewt.modifier.BackgroundModifier
import dev.kewt.modifier.BoldModifier
import dev.kewt.modifier.BorderStyle
import dev.kewt.modifier.Color
import dev.kewt.modifier.ForegroundModifier
import dev.kewt.modifier.ItalicModifier
import dev.kewt.modifier.Modifier
import dev.kewt.modifier.StrikethroughModifier
import dev.kewt.modifier.UnderlineModifier
import dev.kewt.modifier.background
import dev.kewt.modifier.bold
import dev.kewt.modifier.border
import dev.kewt.modifier.fillMaxSize
import dev.kewt.modifier.findElement
import dev.kewt.modifier.foreground
import dev.kewt.modifier.padding
import dev.kewt.test.assertCellAt
import dev.kewt.test.assertContainsText
import dev.kewt.ui.layout.Constraints
import dev.kewt.ui.layout.LayoutType
import kotlin.test.Test

class ComposableWidgetTest {
    /**
     * Helper to render a view scope into a buffer for testing.
     */
    private fun renderToBuffer(width: Int, height: Int, content: ViewScope.() -> Unit): Buffer {
        val scope = ViewScope().apply(content)
        val buffer = Buffer(width, height)
        val rootNode = ContainerViewNode(LayoutType.Column, Modifier.fillMaxSize(), scope.children)
        val layoutRoot = rootNode.toLayoutNode()
        layoutRoot.measure(Constraints(maxWidth = width, maxHeight = height))
        layoutRoot.place(0, 0)
        rootNode.paint(buffer, layoutRoot)
        return buffer
    }

    @Test
    fun textNodeRendersContent() {
        val buffer = renderToBuffer(40, 1) {
            Text("Hello")
        }
        buffer.assertContainsText("Hello")
        buffer.assertCellAt(0, 0, 'H')
        buffer.assertCellAt(4, 0, 'o')
    }

    @Test
    fun textWithStyles() {
        val buffer = renderToBuffer(10, 1) {
            Text("Styled", modifier = Modifier.foreground(Color.Red).bold())
        }
        buffer.assertCellAt(0, 0, 'S', foreground = Color.Red, bold = true)
    }

    @Test
    fun columnLayoutsVertically() {
        val buffer = renderToBuffer(10, 5) {
            Column {
                Text("A")
                Text("B")
            }
        }
        buffer.assertCellAt(0, 0, 'A')
        buffer.assertCellAt(0, 1, 'B')
    }

    @Test
    fun rowLayoutsHorizontally() {
        val buffer = renderToBuffer(10, 5) {
            Row {
                Text("A")
                Text("B")
            }
        }
        buffer.assertCellAt(0, 0, 'A')
        buffer.assertCellAt(1, 0, 'B')
    }

    @Test
    fun boxWithBorderDrawsBorder() {
        val buffer = renderToBuffer(10, 5) {
            Box(modifier = Modifier.border(BorderStyle.Rounded).fillMaxSize()) {
                Text("Hi")
            }
        }
        // Border characters (Rounded style)
        buffer.assertCellAt(0, 0, '╭')
        buffer.assertCellAt(9, 0, '╮')
        buffer.assertCellAt(0, 4, '╰')
        buffer.assertCellAt(9, 4, '╯')

        // Content should be at (1,1) due to implicit padding
        buffer.assertCellAt(1, 1, 'H')
        buffer.assertCellAt(2, 1, 'i')
    }

    @Test
    fun paddingOffsetsContent() {
        val buffer = renderToBuffer(10, 5) {
            Column(modifier = Modifier.padding(left = 3, top = 1)) {
                Text("X")
            }
        }
        // Should be at x=3, y=1
        buffer.assertCellAt(3, 1, 'X')
    }

    @Test
    fun backgroundFillsArea() {
        val buffer = renderToBuffer(5, 5) {
            Box(modifier = Modifier.background(Color.Blue).fillMaxSize()) {
                Text(" ")
            }
        }
        // Entire 5x5 area should be Blue
        for (y in 0 until 5) {
            for (x in 0 until 5) {
                buffer.assertCellAt(x, y, background = Color.Blue)
            }
        }
    }
}
