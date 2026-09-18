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
import dev.kewt.core.buffer.Rect
import dev.kewt.core.buffer.UnicodeWidth
import dev.kewt.modifier.Modifier
import dev.kewt.modifier.Style
import dev.kewt.modifier.TextAlign
import dev.kewt.modifier.resolveStyle
import dev.kewt.ui.layout.Constraints
import dev.kewt.ui.layout.LayoutNode
import dev.kewt.ui.layout.LayoutType
import dev.kewt.ui.layout.MeasureResult

internal class TableViewNode(
    val headers: List<String>?,
    val rows: List<List<String>>,
    val modifier: Modifier,
    val columnSpacing: Int,
    val headerStyle: Style,
    val columnAlignments: List<TextAlign>,
) : ViewNode() {
    private fun columnWidths(): IntArray {
        var count = headers?.size ?: 0
        rows.forEach { row -> count = maxOf(count, row.size) }
        val widths = IntArray(count)
        headers?.forEachIndexed { index, header ->
            if (index < count) widths[index] = maxOf(widths[index], UnicodeWidth.displayWidth(header))
        }
        rows.forEach { row ->
            row.forEachIndexed { index, cell ->
                if (index < count) widths[index] = maxOf(widths[index], UnicodeWidth.displayWidth(cell))
            }
        }
        return widths
    }

    override fun toLayoutNode(): LayoutNode =
        LayoutNode(
            layoutType = LayoutType.Leaf,
            modifier = modifier,
            measureFn = { constraints -> measureTable(constraints) },
        )

    private fun measureTable(constraints: Constraints): MeasureResult {
        val widths = columnWidths()
        val spacingTotal = if (widths.isEmpty()) 0 else columnSpacing * (widths.size - 1)
        val totalWidth = widths.sum() + spacingTotal
        val totalHeight = rows.size + (if (headers != null) 1 else 0)
        return MeasureResult(
            width = totalWidth.coerceIn(0, constraints.maxWidth),
            height = totalHeight.coerceIn(0, constraints.maxHeight),
        )
    }

    override fun paint(
        buffer: Buffer,
        node: LayoutNode,
        inheritedStyle: Style,
    ) {
        val style = inheritedStyle.merge(modifier.resolveStyle())
        val box = node.contentBox()
        if (box.width <= 0 || box.height <= 0) return

        val widths = columnWidths()
        if (widths.isEmpty()) return

        val columnX = IntArray(widths.size)
        var cursor = box.x
        widths.forEachIndexed { index, w ->
            columnX[index] = cursor
            cursor += w + columnSpacing
        }

        var row = box.y
        val headerCells = headers
        if (headerCells != null && row < box.y + box.height) {
            paintRow(buffer, row, headerCells, widths, columnX, box, style.merge(headerStyle))
            row++
        }
        rows.forEach { cells ->
            if (row >= box.y + box.height) return@forEach
            paintRow(buffer, row, cells, widths, columnX, box, style)
            row++
        }
    }

    private fun paintRow(
        buffer: Buffer,
        row: Int,
        cells: List<String>,
        widths: IntArray,
        columnX: IntArray,
        box: Rect,
        style: Style,
    ) {
        cells.forEachIndexed { index, cell ->
            if (index >= widths.size) return@forEachIndexed
            val columnWidth = widths[index]
            val alignment = columnAlignments.getOrElse(index) { TextAlign.Left }
            val available = box.x + box.width - columnX[index]
            if (available <= 0) return@forEachIndexed
            val effectiveWidth = minOf(columnWidth, available)
            val text = UnicodeWidth.truncate(cell, effectiveWidth)
            val textWidth = UnicodeWidth.displayWidth(text)
            val offset = when (alignment) {
                TextAlign.Left -> 0
                TextAlign.Center -> (effectiveWidth - textWidth) / 2
                TextAlign.Right -> effectiveWidth - textWidth
            }
            val startX = columnX[index] + offset.coerceAtLeast(0)
            buffer.writeString(
                startX,
                row,
                text,
                foreground = style.foreground,
                background = style.background,
                bold = style.bold,
                italic = style.italic,
                underline = style.underline,
                strikethrough = style.strikethrough,
                dim = style.dim,
                blink = style.blink,
                reverse = style.reverse,
                hidden = style.hidden,
            )
        }
    }
}

/**
 * A text table with automatically sized columns.
 *
 * Column widths are derived from the widest cell in each column. Cells wider than
 * their column are truncated; rows beyond the layout height are not rendered.
 *
 * @param rows The data rows.
 * @param headers Optional header row, rendered with [headerStyle].
 * @param columnSpacing Number of blank cells between columns.
 * @param headerStyle Style merged on top of the widget style for the header row.
 * @param columnAlignments Per-column text alignment; defaults to left when absent.
 */
@Suppress("FunctionName")
public fun ViewScope.Table(
    rows: List<List<String>>,
    headers: List<String>? = null,
    modifier: Modifier = Modifier,
    columnSpacing: Int = 2,
    headerStyle: Style = Style(bold = true),
    columnAlignments: List<TextAlign> = emptyList(),
) {
    children.add(TableViewNode(headers, rows, modifier, columnSpacing.coerceAtLeast(0), headerStyle, columnAlignments))
}
