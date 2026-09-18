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
import dev.kewt.modifier.resolveStyle
import dev.kewt.ui.layout.LayoutNode
import dev.kewt.ui.layout.LayoutType
import kotlin.math.roundToInt

private const val DEFAULT_BAR_WIDTH = 20

internal class ProgressBarViewNode(
    val progress: Float,
    val modifier: Modifier,
    val filledChar: Char,
    val emptyChar: Char,
    val label: String?,
) : ViewNode() {
    override fun toLayoutNode(): LayoutNode =
        LayoutNode(
            layoutType = LayoutType.Leaf,
            modifier = modifier,
            intrinsicWidth = DEFAULT_BAR_WIDTH,
            intrinsicHeight = 1,
        )

    override fun paint(
        buffer: Buffer,
        node: LayoutNode,
        inheritedStyle: Style,
    ) {
        val style = inheritedStyle.merge(modifier.resolveStyle())
        val box = node.contentBox()
        if (box.width <= 0 || box.height <= 0) return

        val fraction = progress.coerceIn(0f, 1f)
        val filled = (fraction * box.width).roundToInt().coerceIn(0, box.width)

        if (filled > 0) {
            buffer.fillRect(
                box.x,
                box.y,
                filled,
                box.height,
                filledChar,
                foreground = style.foreground,
                background = style.background,
                bold = style.bold,
                dim = style.dim,
            )
        }
        val rest = box.width - filled
        if (rest > 0) {
            buffer.fillRect(
                box.x + filled,
                box.y,
                rest,
                box.height,
                emptyChar,
                foreground = style.foreground,
                background = style.background,
                bold = style.bold,
                dim = style.dim,
            )
        }

        if (label != null) {
            val startX = box.x + ((box.width - label.length) / 2).coerceAtLeast(0)
            buffer.writeString(
                startX,
                box.y,
                label,
                foreground = style.background,
                background = style.foreground,
                bold = style.bold,
            )
        }
    }
}

/**
 * A horizontal progress bar.
 *
 * The bar fills its layout width, so combine it with
 * [dev.kewt.modifier.fillMaxWidth] or [dev.kewt.modifier.width] to size it.
 *
 * @param progress Completion fraction; values are coerced into 0f..1f.
 * @param filledChar Character drawn for the completed portion.
 * @param emptyChar Character drawn for the remaining portion.
 * @param showPercentage When true, renders the percentage centered on the bar.
 * @param label Overrides [showPercentage] with custom centered text.
 */
@Suppress("FunctionName")
public fun ViewScope.ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    filledChar: Char = '█',
    emptyChar: Char = '░',
    showPercentage: Boolean = false,
    label: String? = null,
) {
    val resolvedLabel = label ?: if (showPercentage) "${(progress.coerceIn(0f, 1f) * 100).roundToInt()}%" else null
    children.add(ProgressBarViewNode(progress, modifier, filledChar, emptyChar, resolvedLabel))
}
