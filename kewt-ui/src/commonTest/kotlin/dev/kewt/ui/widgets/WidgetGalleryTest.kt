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
import dev.kewt.modifier.HorizontalAlignment
import dev.kewt.modifier.Modifier
import dev.kewt.modifier.Style
import dev.kewt.modifier.TextAlign
import dev.kewt.modifier.fillMaxSize
import dev.kewt.modifier.height
import dev.kewt.modifier.weight
import dev.kewt.modifier.width
import dev.kewt.test.assertCellAt
import dev.kewt.test.assertContainsText
import dev.kewt.ui.layout.Constraints
import dev.kewt.ui.layout.LayoutType
import dev.kewt.ui.theme.KewtTheme
import kotlin.test.Test
import kotlin.test.assertEquals

class WidgetGalleryTest {
    private fun renderToBuffer(
        width: Int,
        height: Int,
        tickMs: Long = 0L,
        content: ViewScope.() -> Unit,
    ): Buffer {
        val scope = ViewScope().apply(content)
        scope.tick.value = tickMs
        val buffer = Buffer(width, height)
        val rootNode = ContainerViewNode(LayoutType.Column, Modifier.fillMaxSize(), scope.children)
        val layoutRoot = rootNode.toLayoutNode()
        layoutRoot.measure(Constraints(maxWidth = width, maxHeight = height))
        layoutRoot.place(0, 0)
        rootNode.paint(buffer, layoutRoot, Style.Empty)
        return buffer
    }

    @Test
    fun spacerReservesSpace() {
        val buffer = renderToBuffer(10, 1) {
            Row {
                Spacer(Modifier.width(3).height(1))
                Text("X")
            }
        }
        buffer.assertCellAt(0, 0, ' ')
        buffer.assertCellAt(3, 0, 'X')
    }

    @Test
    fun horizontalDividerSpansWidth() {
        val buffer = renderToBuffer(8, 3) {
            Divider(char = '=')
        }
        buffer.assertCellAt(0, 0, '=')
        buffer.assertCellAt(7, 0, '=')
        buffer.assertCellAt(0, 1, ' ')
    }

    @Test
    fun verticalDividerSpansHeight() {
        val buffer = renderToBuffer(10, 4) {
            Row(modifier = Modifier.fillMaxSize()) {
                Divider(vertical = true)
                Text("A")
            }
        }
        buffer.assertCellAt(0, 0, '│')
        buffer.assertCellAt(0, 3, '│')
        buffer.assertCellAt(1, 0, 'A')
    }

    @Test
    fun checkboxRendersCheckedState() {
        val buffer = renderToBuffer(10, 1) {
            Checkbox(checked = true, label = "ok")
        }
        buffer.assertCellAt(0, 0, '[')
        buffer.assertCellAt(1, 0, 'x')
        buffer.assertCellAt(2, 0, ']')
        buffer.assertCellAt(4, 0, 'o')
    }

    @Test
    fun checkboxRendersUncheckedCustomChars() {
        val buffer = renderToBuffer(10, 1) {
            Checkbox(checked = false, label = "y", checkedChar = '✓', uncheckedChar = '·')
        }
        buffer.assertCellAt(1, 0, '·')
        buffer.assertCellAt(4, 0, 'y')
    }

    @Test
    fun progressBarFillsProportionally() {
        val buffer = renderToBuffer(10, 1) {
            ProgressBar(0.5f, modifier = Modifier.width(10))
        }
        buffer.assertCellAt(0, 0, '█')
        buffer.assertCellAt(4, 0, '█')
        buffer.assertCellAt(5, 0, '░')
        buffer.assertCellAt(9, 0, '░')
    }

    @Test
    fun progressBarClampsOutOfRangeValues() {
        val over = renderToBuffer(4, 1) {
            ProgressBar(2f, modifier = Modifier.width(4))
        }
        over.assertCellAt(3, 0, '█')
        val under = renderToBuffer(4, 1) {
            ProgressBar(-1f, modifier = Modifier.width(4))
        }
        under.assertCellAt(0, 0, '░')
        under.assertCellAt(3, 0, '░')
    }

    @Test
    fun progressBarShowsPercentage() {
        val buffer = renderToBuffer(10, 1) {
            ProgressBar(0.25f, modifier = Modifier.width(10), showPercentage = true)
        }
        buffer.assertCellAt(3, 0, '2')
        buffer.assertCellAt(4, 0, '5')
        buffer.assertCellAt(5, 0, '%')
    }

    @Test
    fun spinnerAdvancesWithTick() {
        val first = renderToBuffer(4, 1) {
            Spinner(Spinners.Line)
        }
        first.assertCellAt(0, 0, '-')

        val second = renderToBuffer(4, 1, tickMs = 150L) {
            Spinner(Spinners.Line)
        }
        second.assertCellAt(0, 0, '\\')
    }

    @Test
    fun tableRendersHeadersAndRows() {
        val buffer = renderToBuffer(20, 4) {
            Table(
                rows = listOf(listOf("a", "bb"), listOf("ccc", "d")),
                headers = listOf("H1", "H2"),
            )
        }
        // Columns: width 3 and 2, spacing 2 -> second column starts at x=5.
        buffer.assertCellAt(0, 0, 'H', bold = true)
        buffer.assertCellAt(5, 0, 'H')
        buffer.assertCellAt(0, 1, 'a')
        buffer.assertCellAt(5, 1, 'b')
        buffer.assertCellAt(0, 2, 'c')
        buffer.assertCellAt(5, 2, 'd')
    }

    @Test
    fun tableAlignsColumnsRight() {
        val buffer = renderToBuffer(20, 2) {
            Table(
                rows = listOf(listOf("a", "bb"), listOf("ccc", "d")),
                columnAlignments = listOf(TextAlign.Right),
            )
        }
        // First column has width 3; "a" is right-aligned to x=2.
        buffer.assertCellAt(2, 0, 'a')
        buffer.assertCellAt(0, 1, 'c')
    }

    @Test
    fun selectListHighlightsSelection() {
        val state = SelectionState(1)
        val buffer = renderToBuffer(20, 3) {
            SelectList(listOf("One", "Two", "Three"), state)
        }
        val theme = KewtTheme.colors
        buffer.assertCellAt(0, 1, '❯', foreground = theme.onPrimary, background = theme.primary)
        buffer.assertCellAt(2, 1, 'T', foreground = theme.onPrimary, background = theme.primary)
        buffer.assertContainsText("Three")
        buffer.assertCellAt(0, 0, ' ')
    }

    @Test
    fun selectListScrollsWindowToSelection() {
        val state = SelectionState(4)
        val buffer = renderToBuffer(10, 3) {
            SelectList(listOf("a", "b", "c", "d", "e"), state, marker = "> ")
        }
        // Window shows items 2..4; the selection sits on the last visible row.
        buffer.assertCellAt(2, 0, 'c')
        buffer.assertCellAt(2, 1, 'd')
        buffer.assertCellAt(0, 2, '>')
        buffer.assertCellAt(2, 2, 'e')
    }

    @Test
    fun textInputRendersTextAndCursor() {
        val state = TextInputState("hi")
        val buffer = renderToBuffer(10, 1) {
            TextInput(state)
        }
        buffer.assertCellAt(0, 0, 'h')
        buffer.assertCellAt(1, 0, 'i')
        // Block cursor sits after the last character.
        buffer.assertCellAt(2, 0, reverse = true)
    }

    @Test
    fun rowWithWeightedChildrenSplitsSpace() {
        val buffer = renderToBuffer(10, 1) {
            Row {
                Text("L", modifier = Modifier.weight(1f))
                Text("R", modifier = Modifier.weight(1f))
            }
        }
        buffer.assertCellAt(0, 0, 'L')
        buffer.assertCellAt(5, 0, 'R')
    }

    @Test
    fun columnHorizontalAlignmentCentersChildren() {
        val buffer = renderToBuffer(9, 2) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = HorizontalAlignment.Center,
            ) {
                Text("ab")
            }
        }
        // Content width 9, text width 2 -> offset (9-2)/2 = 3.
        assertEquals('a', buffer.get(3, 0).char)
    }
}
