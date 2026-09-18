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
import dev.kewt.terminal.KeyModifier
import dev.kewt.ui.layout.LayoutNode
import dev.kewt.ui.layout.LayoutType
import dev.kewt.ui.layout.MeasureResult

private const val MIN_INPUT_WIDTH = 10

/**
 * Observable state and editing logic for a [TextInput] widget.
 *
 * The state can be mutated programmatically, or fed key events directly via
 * [handleKey]. When the widget is composed with a focus `id` and a
 * [FocusManager], key routing happens automatically.
 *
 * @param initial The initial text content.
 * @param maxLength Maximum number of characters accepted; further input is ignored.
 */
@Suppress("TooManyFunctions")
public class TextInputState(
    initial: String = "",
    public val maxLength: Int = Int.MAX_VALUE,
) {
    /** The current text content. */
    public val text: MutableState<String> = mutableStateOf(initial.take(maxLength))

    /** The cursor position as a character index into [text]. */
    public val cursor: MutableState<Int> = mutableStateOf(text.value.length)

    /** Invoked when Enter is pressed while the input has focus. */
    public var onSubmit: ((String) -> Unit)? = null

    /** Invoked after every successful content mutation. */
    public var onChange: ((String) -> Unit)? = null

    /**
     * Applies a key event to the input.
     *
     * Supports printable characters, Enter, Backspace, Delete, arrow keys, Home/End,
     * and Emacs-style Ctrl bindings (a/e/b/f/h/d/u/k/w).
     *
     * @return true when the event was consumed.
     */
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    public fun handleKey(event: KeyEvent): Boolean {
        val mods = event.modifiers
        val ctrlOnly = mods.size == 1 && KeyModifier.Ctrl in mods
        return when (val key = event.key) {
            is Key.Char ->
                when {
                    mods.isEmpty() -> {
                        insert(key.c.toString())
                        true
                    }

                    ctrlOnly -> handleCtrlChar(key.c)

                    else -> false
                }

            is Key.Text ->
                if (mods.isEmpty()) {
                    insert(key.text)
                    true
                } else {
                    false
                }

            Key.Enter ->
                if (mods.isEmpty()) {
                    onSubmit?.invoke(text.value)
                    true
                } else {
                    false
                }

            Key.Backspace ->
                if (mods.isEmpty()) {
                    backspace()
                    true
                } else {
                    false
                }

            Key.Delete ->
                if (mods.isEmpty() || ctrlOnly) {
                    delete()
                    true
                } else {
                    false
                }

            Key.Left ->
                if (mods.isEmpty() || ctrlOnly) {
                    moveLeft()
                    true
                } else {
                    false
                }

            Key.Right ->
                if (mods.isEmpty() || ctrlOnly) {
                    moveRight()
                    true
                } else {
                    false
                }

            Key.Home ->
                if (mods.isEmpty() || ctrlOnly) {
                    moveCursorHome()
                    true
                } else {
                    false
                }

            Key.End ->
                if (mods.isEmpty() || ctrlOnly) {
                    moveCursorEnd()
                    true
                } else {
                    false
                }

            else -> false
        }
    }

    private fun handleCtrlChar(c: Char): Boolean =
        when (c) {
            'a' -> {
                moveCursorHome()
                true
            }

            'e' -> {
                moveCursorEnd()
                true
            }

            'b' -> {
                moveLeft()
                true
            }

            'f' -> {
                moveRight()
                true
            }

            'h' -> {
                backspace()
                true
            }

            'd' -> {
                delete()
                true
            }

            'u' -> {
                deleteToStart()
                true
            }

            'k' -> {
                deleteToEnd()
                true
            }

            'w' -> {
                deleteWordBefore()
                true
            }

            else -> false
        }

    /** Replaces the content, clamping it to [maxLength] and resetting the cursor. */
    public fun setText(newText: String) {
        text.value = if (newText.length > maxLength) newText.take(maxLength) else newText
        cursor.value = text.value.length
        onChange?.invoke(text.value)
    }

    /** Inserts [input] at the cursor position. */
    public fun insert(input: String) {
        if (input.isEmpty()) return
        val current = text.value
        val position = normalize(current, cursor.value)
        val room = maxLength - current.length
        if (room <= 0) return
        val addition = if (input.length > room) input.take(room) else input
        text.value = current.take(position) + addition + current.drop(position)
        cursor.value = position + addition.length
        onChange?.invoke(text.value)
    }

    /** Deletes the character before the cursor. */
    public fun backspace() {
        val current = text.value
        val position = normalize(current, cursor.value)
        if (position <= 0) return
        val removeCount =
            if (position >= 2 && current[position - 1].isLowSurrogate() && current[position - 2].isHighSurrogate()) {
                2
            } else {
                1
            }
        text.value = current.take(position - removeCount) + current.drop(position)
        cursor.value = position - removeCount
        onChange?.invoke(text.value)
    }

    /** Deletes the character after the cursor. */
    public fun delete() {
        val current = text.value
        val position = normalize(current, cursor.value)
        if (position >= current.length) return
        val removeCount =
            if (position + 1 < current.length &&
                current[position].isHighSurrogate() &&
                current[position + 1].isLowSurrogate()
            ) {
                2
            } else {
                1
            }
        text.value = current.take(position) + current.drop(position + removeCount)
        cursor.value = position
        onChange?.invoke(text.value)
    }

    /** Moves the cursor one character to the left. */
    public fun moveLeft() {
        val current = text.value
        val position = normalize(current, cursor.value)
        if (position <= 0) return
        val step =
            if (position >= 2 && current[position - 1].isLowSurrogate() && current[position - 2].isHighSurrogate()) {
                2
            } else {
                1
            }
        cursor.value = position - step
    }

    /** Moves the cursor one character to the right. */
    public fun moveRight() {
        val current = text.value
        val position = normalize(current, cursor.value)
        if (position >= current.length) return
        val step =
            if (position + 1 < current.length &&
                current[position].isHighSurrogate() &&
                current[position + 1].isLowSurrogate()
            ) {
                2
            } else {
                1
            }
        cursor.value = position + step
    }

    /** Moves the cursor to the start of the text. */
    public fun moveCursorHome() {
        cursor.value = 0
    }

    /** Moves the cursor to the end of the text. */
    public fun moveCursorEnd() {
        cursor.value = text.value.length
    }

    /** Deletes everything before the cursor (Ctrl+U). */
    public fun deleteToStart() {
        val current = text.value
        val position = normalize(current, cursor.value)
        if (position <= 0) return
        text.value = current.drop(position)
        cursor.value = 0
        onChange?.invoke(text.value)
    }

    /** Deletes everything after the cursor (Ctrl+K). */
    public fun deleteToEnd() {
        val current = text.value
        val position = normalize(current, cursor.value)
        if (position >= current.length) return
        text.value = current.take(position)
        cursor.value = position
        onChange?.invoke(text.value)
    }

    /** Deletes the word immediately before the cursor (Ctrl+W). */
    public fun deleteWordBefore() {
        val current = text.value
        val position = normalize(current, cursor.value)
        if (position <= 0) return
        var start = position
        while (start > 0 && current[start - 1] == ' ') start--
        while (start > 0 && current[start - 1] != ' ') start--
        text.value = current.take(start) + current.drop(position)
        cursor.value = start
        onChange?.invoke(text.value)
    }

    /**
     * Clamps [position] into range and pushes it past the end of a surrogate pair
     * so the cursor never splits a code point.
     */
    private fun normalize(
        value: String,
        position: Int,
    ): Int {
        val p = position.coerceIn(0, value.length)
        return if (p > 0 && p < value.length && value[p].isLowSurrogate() && value[p - 1].isHighSurrogate()) {
            p + 1
        } else {
            p
        }
    }
}

internal class TextInputViewNode(
    val state: TextInputState,
    val modifier: Modifier,
    val focused: Boolean,
) : ViewNode() {
    override fun toLayoutNode(): LayoutNode =
        LayoutNode(
            layoutType = LayoutType.Leaf,
            modifier = modifier,
            measureFn = { constraints ->
                val natural = (UnicodeWidth.displayWidth(state.text.value) + 1).coerceAtLeast(MIN_INPUT_WIDTH)
                MeasureResult(
                    width = natural.coerceIn(0, constraints.maxWidth),
                    height = minOf(1, constraints.maxHeight),
                )
            },
        )

    override fun paint(
        buffer: Buffer,
        node: LayoutNode,
        inheritedStyle: Style,
    ) {
        val style = inheritedStyle.merge(modifier.resolveStyle())
        val box = node.contentBox()
        if (box.width <= 0 || box.height <= 0) return

        val value = state.text.value
        val cursorPos = state.cursor.value.coerceIn(0, value.length)

        // Scroll horizontally so that the cursor stays inside the visible window,
        // keeping at least one cell reserved for the cursor block.
        var startIndex = cursorPos
        var used = 0
        while (startIndex > 0) {
            val w = UnicodeWidth.ofChar(value[startIndex - 1])
            if (used + w > box.width - 1) break
            used += w
            startIndex--
        }

        val visible = UnicodeWidth.truncate(value.substring(startIndex), box.width)
        buffer.writeString(
            box.x,
            box.y,
            visible,
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

        if (focused) {
            val cursorX = box.x + UnicodeWidth.displayWidth(value.substring(startIndex, cursorPos))
            if (cursorX < box.x + box.width) {
                buffer.setChar(cursorX, box.y, reverse = true)
            }
        }
    }
}

/**
 * A single-line text input field with a block cursor.
 *
 * Give the field an [id] together with [rememberFocusManager] to have keystrokes
 * routed automatically (Tab moves between focused widgets). Without an id the
 * field is always focused and consumes printable keys through the focus system
 * only if it is the registered handler; otherwise drive it yourself:
 * ```kotlin
 * onKeyEvent { state.handleKey(it) }
 * ```
 *
 * @param state The observable editing state.
 * @param id Optional focus identifier for automatic key routing.
 */
@Suppress("FunctionName")
public fun ViewScope.TextInput(
    state: TextInputState,
    modifier: Modifier = Modifier,
    id: String? = null,
) {
    val manager = focusManager
    val focused =
        if (id != null && manager != null) {
            manager.register(id, state::handleKey)
            manager.isFocused(id)
        } else {
            true
        }
    children.add(TextInputViewNode(state, modifier, focused))
}
