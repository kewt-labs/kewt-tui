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

import dev.kewt.terminal.Key
import dev.kewt.terminal.KeyEvent
import dev.kewt.terminal.KeyModifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FocusTest {
    private fun composedScope(): Pair<ViewScope, FocusManager> {
        val scope = ViewScope()
        val manager = scope.rememberFocusManager()
        with(scope) {
            TextInput(TextInputState(), id = "input")
            SelectList(listOf("a", "b", "c"), SelectionState(), id = "list")
        }
        return scope to manager
    }

    @Test
    fun firstRegisteredWidgetReceivesFocus() {
        val (_, manager) = composedScope()
        assertEquals("input", manager.focused.value)
        assertTrue(manager.isFocused("input"))
        assertFalse(manager.isFocused("list"))
    }

    @Test
    fun tabMovesFocusForwardAndWraps() {
        val (scope, manager) = composedScope()
        assertTrue(scope.dispatchKeyEvent(KeyEvent(Key.Tab)))
        assertEquals("list", manager.focused.value)
        assertTrue(scope.dispatchKeyEvent(KeyEvent(Key.Tab)))
        assertEquals("input", manager.focused.value)
    }

    @Test
    fun backTabMovesFocusBackwardAndWraps() {
        val (scope, manager) = composedScope()
        assertTrue(scope.dispatchKeyEvent(KeyEvent(Key.BackTab)))
        assertEquals("list", manager.focused.value)
    }

    @Test
    fun shiftTabIsNotTreatedAsTraversal() {
        val (scope, manager) = composedScope()
        // A modified Tab is offered to the focused handler instead of traversing.
        assertFalse(scope.dispatchKeyEvent(KeyEvent(Key.Tab, setOf(KeyModifier.Shift))))
        assertEquals("input", manager.focused.value)
    }

    @Test
    fun keysRouteToFocusedTextInput() {
        val input = TextInputState()
        val scope = ViewScope().apply {
            rememberFocusManager()
            TextInput(input, id = "input")
        }
        assertTrue(scope.dispatchKeyEvent(KeyEvent(Key.Char('h'))))
        assertTrue(scope.dispatchKeyEvent(KeyEvent(Key.Char('i'))))
        assertEquals("hi", input.text.value)
    }

    @Test
    fun arrowsRouteToFocusedSelectList() {
        val selection = SelectionState()
        val scope = ViewScope().apply {
            rememberFocusManager()
            SelectList(listOf("a", "b", "c"), selection, id = "list")
        }
        assertTrue(scope.dispatchKeyEvent(KeyEvent(Key.Down)))
        assertEquals(1, selection.selectedIndex.value)
        assertTrue(scope.dispatchKeyEvent(KeyEvent(Key.Down)))
        assertEquals(2, selection.selectedIndex.value)
        assertTrue(scope.dispatchKeyEvent(KeyEvent(Key.Up)))
        assertEquals(1, selection.selectedIndex.value)
    }

    @Test
    fun enterFiresOnSelectThroughFocusRouting() {
        val selection = SelectionState(2)
        var selected = -1
        selection.onSelect = { selected = it }
        val scope = ViewScope().apply {
            rememberFocusManager()
            SelectList(listOf("a", "b", "c"), selection, id = "list")
        }
        assertTrue(scope.dispatchKeyEvent(KeyEvent(Key.Enter)))
        assertEquals(2, selected)
    }

    @Test
    fun unhandledKeysAreNotConsumed() {
        val scope = ViewScope().apply {
            rememberFocusManager()
            SelectList(listOf("a"), SelectionState(), id = "list")
        }
        assertFalse(scope.dispatchKeyEvent(KeyEvent(Key.Char('x'))))
    }

    @Test
    fun scopeWithoutFocusManagerConsumesNothing() {
        val scope = ViewScope()
        assertFalse(scope.dispatchKeyEvent(KeyEvent(Key.Tab)))
        assertFalse(scope.dispatchKeyEvent(KeyEvent(Key.Char('x'))))
    }

    @Test
    fun emptyManagerConsumesNothing() {
        val scope = ViewScope().apply { rememberFocusManager() }
        assertFalse(scope.dispatchKeyEvent(KeyEvent(Key.Tab)))
    }

    @Test
    fun requestAndClearFocus() {
        val (_, manager) = composedScope()
        manager.requestFocus("list")
        assertTrue(manager.isFocused("list"))
        manager.clearFocus()
        assertNull(manager.focused.value)
    }

    @Test
    fun focusNextSkipsUnknownIds() {
        val (_, manager) = composedScope()
        manager.requestFocus("nonexistent")
        manager.focusNext()
        // indexOf returns -1, so focus lands on the first registered id.
        assertEquals("input", manager.focused.value)
    }

    @Test
    fun selectionStatePaging() {
        val state = SelectionState()
        val items = List(25) { it.toString() }
        state.handleKey(KeyEvent(Key.PageDown), items.size)
        assertEquals(10, state.selectedIndex.value)
        state.handleKey(KeyEvent(Key.PageDown), items.size)
        assertEquals(20, state.selectedIndex.value)
        state.handleKey(KeyEvent(Key.PageDown), items.size)
        assertEquals(24, state.selectedIndex.value)
        state.handleKey(KeyEvent(Key.PageUp), items.size)
        assertEquals(14, state.selectedIndex.value)
        state.handleKey(KeyEvent(Key.End), items.size)
        assertEquals(24, state.selectedIndex.value)
        state.handleKey(KeyEvent(Key.Home), items.size)
        assertEquals(0, state.selectedIndex.value)
    }

    @Test
    fun selectionStateRejectsModifiedAndEmptyInput() {
        val state = SelectionState()
        assertFalse(state.handleKey(KeyEvent(Key.Down, setOf(KeyModifier.Shift)), 3))
        assertEquals(0, state.selectedIndex.value)
        assertFalse(state.handleKey(KeyEvent(Key.Down), 0))
        assertFalse(state.handleKey(KeyEvent(Key.Char('x')), 3))
    }

    @Test
    fun selectionStateClampsDownAtEnd() {
        val state = SelectionState(2)
        state.handleKey(KeyEvent(Key.Down), 3)
        assertEquals(2, state.selectedIndex.value)
    }

    @Test
    fun selectClampsToItemCount() {
        val state = SelectionState()
        state.select(99, 5)
        assertEquals(4, state.selectedIndex.value)
        state.select(-3, 5)
        assertEquals(0, state.selectedIndex.value)
    }
}
