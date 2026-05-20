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
import dev.kewt.modifier.BorderModifier
import dev.kewt.modifier.Color
import dev.kewt.modifier.ForegroundModifier
import dev.kewt.modifier.ItalicModifier
import dev.kewt.modifier.Modifier
import dev.kewt.modifier.StrikethroughModifier
import dev.kewt.modifier.UnderlineModifier
import dev.kewt.modifier.findElement
import dev.kewt.modifier.padding
import dev.kewt.ui.layout.Constraints
import dev.kewt.ui.layout.LayoutNode
import dev.kewt.ui.layout.LayoutType

public class ViewScope {
    internal val children = mutableListOf<ViewNode>()
    internal val cache = mutableMapOf<String, Any?>()
}

internal sealed class ViewNode {
    abstract fun toLayoutNode(): LayoutNode
    abstract fun paint(buffer: Buffer, node: LayoutNode)
}

internal class TextViewNode(
    val content: String,
    val modifier: Modifier,
) : ViewNode() {
    override fun toLayoutNode(): LayoutNode = LayoutNode(
        layoutType = LayoutType.Leaf,
        intrinsicWidth = content.length,
        intrinsicHeight = 1,
        modifier = modifier,
    )

    override fun paint(buffer: Buffer, node: LayoutNode) {
        val fg = modifier.findElement<ForegroundModifier>()?.color
        val bg = modifier.findElement<BackgroundModifier>()?.color
        val bold = if (modifier.findElement<BoldModifier>() != null) true else null
        val italic = if (modifier.findElement<ItalicModifier>() != null) true else null
        val underline = if (modifier.findElement<UnderlineModifier>() != null) true else null
        val strikethrough = if (modifier.findElement<StrikethroughModifier>() != null) true else null

        buffer.writeString(
            x = node.x,
            y = node.y,
            text = content,
            foreground = fg,
            background = bg,
            bold = bold,
            italic = italic,
            underline = underline,
            strikethrough = strikethrough,
        )
    }
}

internal class ContainerViewNode(
    val layoutType: LayoutType,
    val modifier: Modifier,
    val childNodes: List<ViewNode>,
) : ViewNode() {
    override fun toLayoutNode(): LayoutNode = LayoutNode(
        layoutType = layoutType,
        children = childNodes.map { it.toLayoutNode() },
        modifier = modifier,
    )

    override fun paint(buffer: Buffer, node: LayoutNode) {
        // Draw border if present
        val borderMod = modifier.findElement<BorderModifier>()
        if (borderMod != null) {
            renderBorder(buffer, node.x, node.y, node.width, node.height, borderMod.style, borderMod.color)
        }

        // Fill background color if present
        val bgMod = modifier.findElement<BackgroundModifier>()
        if (bgMod != null) {
            for (by in node.y until node.y + node.height) {
                for (bx in node.x until node.x + node.width) {
                    buffer.setBackground(bx, by, bgMod.color)
                }
            }
        }

        // Paint children
        childNodes.forEachIndexed { i, child ->
            child.paint(buffer, node.children[i])
        }
    }
}

@Suppress("FunctionName")
public fun ViewScope.Text(content: String, modifier: Modifier = Modifier) {
    children.add(TextViewNode(content, modifier))
}

@Suppress("FunctionName")
public fun ViewScope.Column(modifier: Modifier = Modifier, content: ViewScope.() -> Unit) {
    val scope = ViewScope().apply(content)
    children.add(ContainerViewNode(LayoutType.Column, modifier, scope.children))
}

@Suppress("FunctionName")
public fun ViewScope.Row(modifier: Modifier = Modifier, content: ViewScope.() -> Unit) {
    val scope = ViewScope().apply(content)
    children.add(ContainerViewNode(LayoutType.Row, modifier, scope.children))
}

/**
 * A container that stacks children on top of each other.
 *
 * If a border is applied via [Modifier.border], implicit padding of 1 is added
 * to prevent children from overlapping the border.
 */
@Suppress("FunctionName")
public fun ViewScope.Box(modifier: Modifier = Modifier, content: ViewScope.() -> Unit) {
    val scope = ViewScope().apply(content)
    // If border is present, add implicit padding of 1
    val borderMod = modifier.findElement<BorderModifier>()
    val effectiveModifier = if (borderMod != null) modifier.then(Modifier.padding(1)) else modifier
    children.add(ContainerViewNode(LayoutType.Box, effectiveModifier, scope.children))
}

public fun buildView(width: Int, height: Int, content: ViewScope.() -> Unit): String {
    val scope = ViewScope().apply(content)
    val buffer = Buffer(width, height)

    // Wrap in a root Column to match renderComposableView behavior
    val rootNode = ContainerViewNode(LayoutType.Column, Modifier, scope.children)
    val layoutRoot = rootNode.toLayoutNode()

    // Measure without height constraint to get natural height
    layoutRoot.measure(Constraints(maxWidth = width, maxHeight = Int.MAX_VALUE))

    layoutRoot.place(0, 0)

    // Paint
    rootNode.paint(buffer, layoutRoot)

    return buffer.captureSnapshot()
}

private fun Buffer.captureSnapshot(): String {
    val sb = StringBuilder()
    for (y in 0 until height) {
        val row = StringBuilder()
        for (x in 0 until width) row.append(get(x, y).char)
        sb.appendLine(row.toString().trimEnd())
    }
    return sb.toString().trimEnd('\n') + "\n"
}
