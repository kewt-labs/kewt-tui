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
import dev.kewt.core.state.MutableState
import dev.kewt.core.state.mutableStateOf
import dev.kewt.modifier.Arrangement
import dev.kewt.modifier.BorderModifier
import dev.kewt.modifier.Color
import dev.kewt.modifier.HorizontalAlignment
import dev.kewt.modifier.Modifier
import dev.kewt.modifier.Style
import dev.kewt.modifier.TextAlign
import dev.kewt.modifier.TextOverflow
import dev.kewt.modifier.VerticalAlignment
import dev.kewt.modifier.findElement
import dev.kewt.modifier.resolveStyle
import dev.kewt.terminal.Key
import dev.kewt.terminal.KeyEvent
import dev.kewt.ui.layout.Constraints
import dev.kewt.ui.layout.LayoutNode
import dev.kewt.ui.layout.LayoutType
import dev.kewt.ui.layout.MeasureResult

/**
 * Receiver scope for building a declarative view hierarchy.
 *
 * A persistent instance is kept across renders by [dev.kewt.ui.widgets.setContent],
 * which allows [remember] to cache values between frames.
 */
public class ViewScope {
    internal val children = mutableListOf<ViewNode>()
    internal val cache = mutableMapOf<String, Any?>()

    /**
     * Monotonic clock (millis) updated by the animation ticker while any widget
     * has requested animation. Read by widgets such as [Spinner].
     */
    internal val tick: MutableState<Long> = mutableStateOf(0L)

    /** Set by animating widgets during composition to request the animation ticker. */
    internal var animationRequested: Boolean = false

    /** Focus manager installed via [rememberFocusManager], when present. */
    internal var focusManager: FocusManager? = null

    /**
     * Routes a key event through the focus system.
     *
     * Tab and BackTab move focus; all other keys are offered to the focused
     * component's handler. Returns true when the event was consumed.
     */
    internal fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val manager = focusManager ?: return false
        if (manager.isEmpty()) return false
        if (event.modifiers.isEmpty()) {
            when (event.key) {
                Key.Tab -> {
                    manager.focusNext()
                    return true
                }

                Key.BackTab -> {
                    manager.focusPrevious()
                    return true
                }

                else -> {}
            }
        }
        val focusedId = manager.focused.value ?: return false
        val handler = manager.handlers[focusedId] ?: return false
        return handler(event)
    }
}

/**
 * A node of the declarative view tree. Each node knows how to create its
 * [LayoutNode] for measurement and how to paint itself into a [Buffer].
 *
 * Painting receives the [Style] resolved from all ancestor modifiers, enabling
 * style inheritance: children override only the attributes they specify.
 */
internal sealed class ViewNode {
    abstract fun toLayoutNode(): LayoutNode

    abstract fun paint(
        buffer: Buffer,
        node: LayoutNode,
        inheritedStyle: Style,
    )
}

internal class TextViewNode(
    val content: String,
    val modifier: Modifier,
    val overflow: TextOverflow,
    val textAlign: TextAlign,
    val maxLines: Int,
) : ViewNode() {
    override fun toLayoutNode(): LayoutNode =
        LayoutNode(
            layoutType = LayoutType.Leaf,
            modifier = modifier,
            measureFn = { constraints -> measureText(constraints) },
        )

    private fun measureText(constraints: Constraints): MeasureResult {
        val lines = layoutLines(constraints.maxWidth)
        var widest = 0
        lines.forEach { widest = maxOf(widest, UnicodeWidth.displayWidth(it)) }
        return MeasureResult(
            width = widest.coerceIn(0, constraints.maxWidth),
            height = lines.size.coerceIn(0, constraints.maxHeight),
        )
    }

    private fun layoutLines(maxWidth: Int): List<String> {
        val lines = mutableListOf<String>()
        content.lines().forEach { raw ->
            when (overflow) {
                TextOverflow.Wrap ->
                    if (maxWidth > 0) {
                        wrapLine(raw, maxWidth, lines)
                    } else {
                        lines.add(raw)
                    }

                TextOverflow.Ellipsis -> lines.add(UnicodeWidth.truncate(raw, maxWidth, ellipsis = true))
                TextOverflow.Clip -> lines.add(UnicodeWidth.truncate(raw, maxWidth))
            }
        }
        return if (lines.size > maxLines) lines.take(maxLines) else lines
    }

    override fun paint(
        buffer: Buffer,
        node: LayoutNode,
        inheritedStyle: Style,
    ) {
        val style = inheritedStyle.merge(modifier.resolveStyle())
        val box = node.contentBox()
        if (box.width <= 0 || box.height <= 0) return

        layoutLines(box.width).forEachIndexed { index, line ->
            if (index >= box.height) return@forEachIndexed
            val lineWidth = UnicodeWidth.displayWidth(line)
            val startX = when (textAlign) {
                TextAlign.Left -> box.x
                TextAlign.Center -> box.x + ((box.width - lineWidth) / 2).coerceAtLeast(0)
                TextAlign.Right -> box.x + (box.width - lineWidth).coerceAtLeast(0)
            }
            buffer.writeString(
                x = startX,
                y = box.y + index,
                text = line,
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

internal class ContainerViewNode(
    val layoutType: LayoutType,
    val modifier: Modifier,
    val childNodes: List<ViewNode>,
    val arrangement: Arrangement = Arrangement.Start,
    val horizontalAlignment: HorizontalAlignment = HorizontalAlignment.Left,
    val verticalAlignment: VerticalAlignment = VerticalAlignment.Top,
) : ViewNode() {
    override fun toLayoutNode(): LayoutNode =
        LayoutNode(
            layoutType = layoutType,
            children = childNodes.map { it.toLayoutNode() },
            modifier = modifier,
            arrangement = arrangement,
            horizontalAlignment = horizontalAlignment,
            verticalAlignment = verticalAlignment,
        )

    override fun paint(
        buffer: Buffer,
        node: LayoutNode,
        inheritedStyle: Style,
    ) {
        val style = inheritedStyle.merge(modifier.resolveStyle())
        val borderBox = node.borderBox()
        val previousClip = buffer.clip
        buffer.clip = Rect.intersectOrNull(previousClip, borderBox)

        val borderMod = modifier.findElement<BorderModifier>()
        if (borderMod != null && borderBox.width >= 2 && borderBox.height >= 2) {
            val borderColor =
                if (borderMod.color != Color.Default) {
                    borderMod.color
                } else {
                    style.foreground ?: Color.Default
                }
            renderBorder(
                buffer,
                borderBox.x,
                borderBox.y,
                borderBox.width,
                borderBox.height,
                borderMod.style,
                borderColor,
                borderMod.title,
                borderMod.titleAlignment,
            )
        }

        val background = style.background
        if (background != null) {
            buffer.fillRect(
                borderBox.x,
                borderBox.y,
                borderBox.width,
                borderBox.height,
                background = background,
            )
        }

        val content = node.contentBox()
        buffer.clip = Rect.intersectOrNull(previousClip, content)
        childNodes.forEachIndexed { index, child ->
            child.paint(buffer, node.children[index], style)
        }
        buffer.clip = previousClip
    }
}

/**
 * Adds a text element.
 *
 * Multi-line content (containing `\n`) is laid out line by line. [overflow] controls
 * what happens when a line is wider than the available space, [textAlign] aligns lines
 * horizontally within the element box, and [maxLines] caps the number of rendered lines.
 */
@Suppress("FunctionName")
public fun ViewScope.Text(
    content: String,
    modifier: Modifier = Modifier,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign = TextAlign.Left,
    maxLines: Int = Int.MAX_VALUE,
) {
    children.add(TextViewNode(content, modifier, overflow, textAlign, maxLines.coerceAtLeast(1)))
}

/**
 * A container that arranges children vertically, top to bottom.
 *
 * @param verticalArrangement Distribution of children along the vertical axis.
 * @param horizontalAlignment Default cross-axis alignment of children.
 */
@Suppress("FunctionName")
public fun ViewScope.Column(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement = Arrangement.Start,
    horizontalAlignment: HorizontalAlignment = HorizontalAlignment.Left,
    content: ViewScope.() -> Unit,
) {
    val scope = ViewScope().apply(content)
    children.add(
        ContainerViewNode(
            LayoutType.Column,
            modifier,
            scope.children,
            arrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
        ),
    )
}

/**
 * A container that arranges children horizontally, left to right.
 *
 * Children carrying [dev.kewt.modifier.weight] share the remaining width proportionally.
 *
 * @param horizontalArrangement Distribution of children along the horizontal axis.
 * @param verticalAlignment Default cross-axis alignment of children.
 */
@Suppress("FunctionName")
public fun ViewScope.Row(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement = Arrangement.Start,
    verticalAlignment: VerticalAlignment = VerticalAlignment.Top,
    content: ViewScope.() -> Unit,
) {
    val scope = ViewScope().apply(content)
    children.add(
        ContainerViewNode(
            LayoutType.Row,
            modifier,
            scope.children,
            arrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
        ),
    )
}

/**
 * A container that stacks children on top of each other.
 *
 * When a border is applied via [dev.kewt.modifier.border], the layout automatically
 * insets children by one cell so they never overlap the border. Children carrying
 * [dev.kewt.modifier.align] are positioned within the content area.
 *
 * @param horizontalAlignment Default horizontal placement of children.
 * @param verticalAlignment Default vertical placement of children.
 */
@Suppress("FunctionName")
public fun ViewScope.Box(
    modifier: Modifier = Modifier,
    horizontalAlignment: HorizontalAlignment = HorizontalAlignment.Left,
    verticalAlignment: VerticalAlignment = VerticalAlignment.Top,
    content: ViewScope.() -> Unit,
) {
    val scope = ViewScope().apply(content)
    children.add(
        ContainerViewNode(
            LayoutType.Box,
            modifier,
            scope.children,
            horizontalAlignment = horizontalAlignment,
            verticalAlignment = verticalAlignment,
        ),
    )
}

/**
 * Builds a view hierarchy into a [width] x [height] character grid and returns
 * the rendered snapshot. Useful for previews and tests.
 */
public fun buildView(
    width: Int,
    height: Int,
    content: ViewScope.() -> Unit,
): String {
    val scope = ViewScope().apply(content)
    val buffer = Buffer(width, height)

    // Wrap in a root Column to match renderComposableView behavior
    val rootNode = ContainerViewNode(LayoutType.Column, Modifier, scope.children)
    val layoutRoot = rootNode.toLayoutNode()

    layoutRoot.measure(Constraints(maxWidth = width, maxHeight = height))
    layoutRoot.place(0, 0)
    rootNode.paint(buffer, layoutRoot, Style.Empty)

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
