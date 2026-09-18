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
import dev.kewt.core.buffer.UnicodeWidth
import dev.kewt.modifier.Modifier
import dev.kewt.modifier.Style
import dev.kewt.modifier.resolveStyle
import dev.kewt.ui.layout.Constraints
import dev.kewt.ui.layout.LayoutNode
import dev.kewt.ui.layout.LayoutType
import dev.kewt.ui.layout.MeasureResult

internal class SpacerViewNode(
    val modifier: Modifier,
) : ViewNode() {
    override fun toLayoutNode(): LayoutNode =
        LayoutNode(
            layoutType = LayoutType.Leaf,
            modifier = modifier,
        )

    override fun paint(
        buffer: Buffer,
        node: LayoutNode,
        inheritedStyle: Style,
    ) {
        val style = inheritedStyle.merge(modifier.resolveStyle())
        val background = style.background
        if (background != null) {
            val box = node.borderBox()
            buffer.fillRect(box.x, box.y, box.width, box.height, background = background)
        }
    }
}

/**
 * An empty element that occupies space defined by its modifier
 * (for example `Modifier.height(1)` or `Modifier.weight(1f)`).
 */
@Suppress("FunctionName")
public fun ViewScope.Spacer(modifier: Modifier = Modifier) {
    children.add(SpacerViewNode(modifier))
}

internal class DividerViewNode(
    val modifier: Modifier,
    val char: Char,
    val vertical: Boolean,
) : ViewNode() {
    override fun toLayoutNode(): LayoutNode =
        LayoutNode(
            layoutType = LayoutType.Leaf,
            modifier = modifier,
            measureFn = { constraints -> measureDivider(constraints) },
        )

    private fun measureDivider(constraints: Constraints): MeasureResult =
        if (vertical) {
            val h = if (constraints.maxHeight == Int.MAX_VALUE) 0 else constraints.maxHeight
            MeasureResult(minOf(1, constraints.maxWidth), h)
        } else {
            val w = if (constraints.maxWidth == Int.MAX_VALUE) 0 else constraints.maxWidth
            MeasureResult(w, minOf(1, constraints.maxHeight))
        }

    override fun paint(
        buffer: Buffer,
        node: LayoutNode,
        inheritedStyle: Style,
    ) {
        val style = inheritedStyle.merge(modifier.resolveStyle())
        val box = node.contentBox()
        if (vertical) {
            for (row in 0 until box.height) {
                buffer.setChar(
                    box.x,
                    box.y + row,
                    char,
                    foreground = style.foreground,
                    background = style.background,
                )
            }
        } else if (box.width > 0) {
            buffer.writeString(
                box.x,
                box.y,
                char.toString().repeat(box.width),
                foreground = style.foreground,
                background = style.background,
            )
        }
    }
}

/**
 * A thin line that separates content.
 *
 * Horizontal dividers stretch to the available width; vertical ones to the
 * available height.
 *
 * @param char The character the line is drawn with; defaults to `─` for
 * horizontal and `│` for vertical dividers.
 * @param vertical When true, draws a vertical rule instead of a horizontal one.
 */
@Suppress("FunctionName")
public fun ViewScope.Divider(
    modifier: Modifier = Modifier,
    char: Char? = null,
    vertical: Boolean = false,
) {
    val resolved = char ?: if (vertical) '│' else '─'
    children.add(DividerViewNode(modifier, resolved, vertical))
}

internal class CheckboxViewNode(
    val checked: Boolean,
    val label: String,
    val modifier: Modifier,
    val checkedChar: Char,
    val uncheckedChar: Char,
) : ViewNode() {
    override fun toLayoutNode(): LayoutNode {
        val box = if (label.isNotEmpty()) "[ ] " else "[ ]"
        return LayoutNode(
            layoutType = LayoutType.Leaf,
            modifier = modifier,
            intrinsicWidth = box.length + UnicodeWidth.displayWidth(label),
            intrinsicHeight = 1,
        )
    }

    override fun paint(
        buffer: Buffer,
        node: LayoutNode,
        inheritedStyle: Style,
    ) {
        val style = inheritedStyle.merge(modifier.resolveStyle())
        val box = node.contentBox()
        val mark = if (checked) checkedChar else uncheckedChar
        val text = if (label.isNotEmpty()) "[$mark] $label" else "[$mark]"
        buffer.writeString(
            box.x,
            box.y,
            UnicodeWidth.truncate(text, box.width),
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

/**
 * A checkbox rendered as `[x] label` / `[ ] label`.
 *
 * The widget is display-only: toggle [checked] from your own key handling
 * (or from a focused [SelectList]-style controller).
 */
@Suppress("FunctionName")
public fun ViewScope.Checkbox(
    checked: Boolean,
    label: String = "",
    modifier: Modifier = Modifier,
    checkedChar: Char = 'x',
    uncheckedChar: Char = ' ',
) {
    children.add(CheckboxViewNode(checked, label, modifier, checkedChar, uncheckedChar))
}
