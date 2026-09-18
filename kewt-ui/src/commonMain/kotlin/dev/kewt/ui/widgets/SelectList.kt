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
import dev.kewt.core.state.MutableState
import dev.kewt.core.state.mutableStateOf
import dev.kewt.modifier.Modifier
import dev.kewt.modifier.Style
import dev.kewt.modifier.resolveStyle
import dev.kewt.terminal.Key
import dev.kewt.terminal.KeyEvent
import dev.kewt.ui.layout.Constraints
import dev.kewt.ui.layout.LayoutNode
import dev.kewt.ui.layout.LayoutType
import dev.kewt.ui.layout.MeasureResult
import dev.kewt.ui.theme.KewtTheme

private const val PAGE_SIZE = 10

/**
 * Observable selection state for a [SelectList] widget.
 *
 * @param initialIndex The initially selected row.
 */
public class SelectionState(initialIndex: Int = 0) {
    /** The index of the selected row. */
    public val selectedIndex: MutableState<Int> = mutableStateOf(initialIndex)

    /** Invoked with the selected index when Enter is pressed. */
    public var onSelect: ((Int) -> Unit)? = null

    /**
     * Applies a key event to the selection: Up/Down move by one row,
     * PageUp/PageDown by ten, Home/End jump to the edges, Enter confirms.
     *
     * @param itemCount Number of rows currently displayed.
     * @return true when the event was consumed.
     */
    public fun handleKey(
        event: KeyEvent,
        itemCount: Int,
    ): Boolean {
        if (itemCount <= 0 || event.modifiers.isNotEmpty()) return false
        val current = selectedIndex.value.coerceIn(0, itemCount - 1)
        return when (event.key) {
            Key.Up -> {
                selectedIndex.value = (current - 1).coerceAtLeast(0)
                true
            }

            Key.Down -> {
                selectedIndex.value = (current + 1).coerceAtMost(itemCount - 1)
                true
            }

            Key.PageUp -> {
                selectedIndex.value = (current - PAGE_SIZE).coerceAtLeast(0)
                true
            }

            Key.PageDown -> {
                selectedIndex.value = (current + PAGE_SIZE).coerceAtMost(itemCount - 1)
                true
            }

            Key.Home -> {
                selectedIndex.value = 0
                true
            }

            Key.End -> {
                selectedIndex.value = itemCount - 1
                true
            }

            Key.Enter -> {
                onSelect?.invoke(current)
                true
            }

            else -> false
        }
    }

    /** Selects [index] programmatically, clamped to the list bounds. */
    public fun select(
        index: Int,
        itemCount: Int,
    ) {
        selectedIndex.value = index.coerceIn(0, (itemCount - 1).coerceAtLeast(0))
    }
}

internal class SelectListViewNode(
    val items: List<String>,
    val state: SelectionState,
    val modifier: Modifier,
    val focused: Boolean,
    val marker: String,
) : ViewNode() {
    private val markerWidth: Int = UnicodeWidth.displayWidth(marker)

    override fun toLayoutNode(): LayoutNode =
        LayoutNode(
            layoutType = LayoutType.Leaf,
            modifier = modifier,
            measureFn = { constraints -> measureList(constraints) },
        )

    private fun measureList(constraints: Constraints): MeasureResult {
        var widest = 0
        items.forEach { widest = maxOf(widest, UnicodeWidth.displayWidth(it) + markerWidth) }
        return MeasureResult(
            width = widest.coerceIn(0, constraints.maxWidth),
            height = items.size.coerceIn(0, constraints.maxHeight),
        )
    }

    override fun paint(
        buffer: Buffer,
        node: LayoutNode,
        inheritedStyle: Style,
    ) {
        val style = inheritedStyle.merge(modifier.resolveStyle())
        val box = node.contentBox()
        if (box.width <= 0 || box.height <= 0 || items.isEmpty()) return

        val selected = state.selectedIndex.value.coerceIn(0, items.size - 1)
        val visibleCount = box.height
        val windowStart = if (selected >= visibleCount) selected - visibleCount + 1 else 0
        val theme = KewtTheme.colors

        items.forEachIndexed { index, item ->
            if (index < windowStart || index >= windowStart + visibleCount) return@forEachIndexed
            val row = box.y + (index - windowStart)
            val isSelected = index == selected
            val prefix = if (isSelected) marker else " ".repeat(markerWidth)
            val text = UnicodeWidth.truncate(prefix + item, box.width)
            if (isSelected && focused) {
                buffer.writeString(
                    box.x,
                    row,
                    text,
                    foreground = theme.onPrimary,
                    background = theme.primary,
                    bold = style.bold,
                )
                val textWidth = UnicodeWidth.displayWidth(text)
                val rest = box.width - textWidth
                if (rest > 0) {
                    buffer.fillRect(box.x + textWidth, row, rest, 1, background = theme.primary)
                }
            } else {
                buffer.writeString(
                    box.x,
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
}

/**
 * A scrollable list of selectable text rows.
 *
 * The selected row is highlighted with the theme's primary colors and prefixed
 * with [marker]. Give the list an [id] together with [rememberFocusManager] to
 * have Up/Down/PageUp/PageDown/Home/End/Enter routed automatically.
 *
 * @param items The row labels.
 * @param state The observable selection state.
 * @param id Optional focus identifier for automatic key routing.
 * @param marker Prefix drawn before the selected row.
 */
@Suppress("FunctionName")
public fun ViewScope.SelectList(
    items: List<String>,
    state: SelectionState,
    modifier: Modifier = Modifier,
    id: String? = null,
    marker: String = "❯ ",
) {
    val manager = focusManager
    val focused =
        if (id != null && manager != null) {
            manager.register(id) { event -> state.handleKey(event, items.size) }
            manager.isFocused(id)
        } else {
            true
        }
    children.add(SelectListViewNode(items, state, modifier, focused, marker))
}
