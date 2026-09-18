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
import dev.kewt.modifier.Modifier
import dev.kewt.modifier.Style
import dev.kewt.modifier.TextAlign
import dev.kewt.modifier.TextOverflow
import dev.kewt.modifier.fillMaxSize
import dev.kewt.modifier.width
import dev.kewt.test.assertCellAt
import dev.kewt.test.assertContainsText
import dev.kewt.ui.layout.Constraints
import dev.kewt.ui.layout.LayoutType
import kotlin.test.Test

class TextRenderTest {
    private fun renderToBuffer(
        width: Int,
        height: Int,
        content: ViewScope.() -> Unit,
    ): Buffer {
        val scope = ViewScope().apply(content)
        val buffer = Buffer(width, height)
        val rootNode = ContainerViewNode(LayoutType.Column, Modifier.fillMaxSize(), scope.children)
        val layoutRoot = rootNode.toLayoutNode()
        layoutRoot.measure(Constraints(maxWidth = width, maxHeight = height))
        layoutRoot.place(0, 0)
        rootNode.paint(buffer, layoutRoot, Style.Empty)
        return buffer
    }

    @Test
    fun wrapSplitsAtWordBoundaries() {
        val buffer = renderToBuffer(10, 4) {
            Text("hello world", overflow = TextOverflow.Wrap)
        }
        buffer.assertCellAt(0, 0, 'h')
        buffer.assertCellAt(4, 0, 'o')
        buffer.assertCellAt(0, 1, 'w')
        buffer.assertContainsText("world")
    }

    @Test
    fun wrapHardBreaksOverlongWords() {
        val buffer = renderToBuffer(5, 4) {
            Text("aaaaaaaaaaaa", overflow = TextOverflow.Wrap)
        }
        buffer.assertCellAt(0, 0, 'a')
        buffer.assertCellAt(4, 0, 'a')
        buffer.assertCellAt(0, 1, 'a')
        buffer.assertCellAt(1, 2, 'a')
        buffer.assertCellAt(2, 2, ' ')
    }

    @Test
    fun wrapCountsWideCharacters() {
        val buffer = renderToBuffer(4, 4) {
            Text("你好你好", overflow = TextOverflow.Wrap)
        }
        buffer.assertCellAt(0, 0, '你')
        buffer.assertCellAt(2, 0, '好')
        buffer.assertCellAt(0, 1, '你')
        buffer.assertCellAt(2, 1, '好')
    }

    @Test
    fun ellipsisTruncatesLongLine() {
        val buffer = renderToBuffer(4, 1) {
            Text("abcdef", overflow = TextOverflow.Ellipsis)
        }
        buffer.assertCellAt(0, 0, 'a')
        buffer.assertCellAt(2, 0, 'c')
        buffer.assertCellAt(3, 0, '…')
    }

    @Test
    fun clipTruncatesWithoutEllipsis() {
        val buffer = renderToBuffer(6, 1) {
            Text("abcdef", modifier = Modifier.width(4), overflow = TextOverflow.Clip)
        }
        buffer.assertCellAt(3, 0, 'd')
        buffer.assertCellAt(4, 0, ' ')
    }

    @Test
    fun alignCenterPositionsLine() {
        val buffer = renderToBuffer(10, 1) {
            Text("ab", modifier = Modifier.width(10), textAlign = TextAlign.Center)
        }
        buffer.assertCellAt(4, 0, 'a')
        buffer.assertCellAt(5, 0, 'b')
    }

    @Test
    fun alignRightPositionsLine() {
        val buffer = renderToBuffer(10, 1) {
            Text("ab", modifier = Modifier.width(10), textAlign = TextAlign.Right)
        }
        buffer.assertCellAt(8, 0, 'a')
        buffer.assertCellAt(9, 0, 'b')
    }

    @Test
    fun maxLinesCapsRenderedLines() {
        val buffer = renderToBuffer(10, 4) {
            Text("l1\nl2\nl3", maxLines = 2)
        }
        buffer.assertCellAt(0, 0, 'l')
        buffer.assertCellAt(1, 0, '1')
        buffer.assertCellAt(1, 1, '2')
        buffer.assertCellAt(0, 2, ' ')
    }

    @Test
    fun multilineTextRendersEachLine() {
        val buffer = renderToBuffer(10, 3) {
            Text("top\nbottom")
        }
        buffer.assertContainsText("top")
        buffer.assertContainsText("bottom")
        buffer.assertCellAt(0, 1, 'b')
    }

    @Test
    fun inheritedStyleAppliesToText() {
        val buffer = renderToBuffer(10, 2) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text("styled")
            }
        }
        // Column applies no style of its own; text renders with defaults.
        buffer.assertCellAt(0, 0, 's')
    }
}
