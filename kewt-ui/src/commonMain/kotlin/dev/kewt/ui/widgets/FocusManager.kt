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

import dev.kewt.core.state.MutableState
import dev.kewt.core.state.mutableStateOf
import dev.kewt.terminal.KeyEvent

private const val FOCUS_MANAGER_KEY = "kewt:focusManager"

/**
 * Tracks which focusable widget currently receives keyboard input.
 *
 * Widgets such as [TextInput] and [SelectList] register themselves when they are
 * composed with a non-null `id`. When [setContent] is used, Tab moves focus to the
 * next registered widget, Shift-Tab to the previous one, and every other key is
 * offered to the focused widget before any application-level key handler runs.
 *
 * Obtain an instance via [rememberFocusManager].
 */
public class FocusManager internal constructor() {
    internal val ids = mutableListOf<String>()
    internal val handlers = mutableMapOf<String, (KeyEvent) -> Boolean>()

    /** The id of the currently focused widget, or null when nothing is focused. */
    public val focused: MutableState<String?> = mutableStateOf(null)

    /** Whether the widget with [id] currently has focus. */
    public fun isFocused(id: String): Boolean = focused.value == id

    /** Moves focus to the widget with [id]. */
    public fun requestFocus(id: String) {
        focused.value = id
    }

    /** Removes focus from all widgets. */
    public fun clearFocus() {
        focused.value = null
    }

    /** Moves focus to the next registered widget, wrapping around. */
    public fun focusNext() {
        if (ids.isEmpty()) return
        val current = focused.value
        val index = if (current != null) ids.indexOf(current) else -1
        focused.value = ids[(index + 1) % ids.size]
    }

    /** Moves focus to the previous registered widget, wrapping around. */
    public fun focusPrevious() {
        if (ids.isEmpty()) return
        val current = focused.value
        val index = if (current != null) ids.indexOf(current) else 0
        focused.value = ids[(index - 1 + ids.size) % ids.size]
    }

    internal fun isEmpty(): Boolean = ids.isEmpty()

    internal fun register(
        id: String,
        handler: ((KeyEvent) -> Boolean)?,
    ) {
        if (id !in ids) ids.add(id)
        if (handler != null) handlers[id] = handler
        if (focused.value == null) focused.value = id
    }
}

/**
 * Returns the [FocusManager] associated with this scope, creating it on first use.
 *
 * The returned manager is remembered across renders; calling this function also
 * activates automatic Tab/Shift-Tab focus traversal and key routing for widgets
 * composed with a focus `id`.
 */
public fun ViewScope.rememberFocusManager(): FocusManager {
    val manager = remember(FOCUS_MANAGER_KEY) { FocusManager() }
    focusManager = manager
    return manager
}
