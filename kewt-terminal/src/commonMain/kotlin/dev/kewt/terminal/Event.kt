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
package dev.kewt.terminal

/**
 * Base class for all terminal events.
 */
public sealed class Event

/**
 * Represents a keyboard input event.
 *
 * @property key The key that was pressed.
 * @property modifiers The set of modifiers (Shift, Ctrl, Alt) active during the press.
 */
public data class KeyEvent(
    val key: Key,
    val modifiers: Set<KeyModifier> = emptySet(),
) : Event()

/**
 * Keyboard modifier keys.
 */
public enum class KeyModifier {
    Shift,
    Ctrl,
    Alt,
}

/**
 * Mouse buttons reported by terminal mouse tracking.
 */
public enum class MouseButton {
    Left,
    Middle,
    Right,
    None,
}

/**
 * The kind of interaction described by a [MouseEvent].
 */
public sealed class MouseKind {
    /** A mouse button was pressed. */
    public data class Down(val button: MouseButton) : MouseKind()

    /** A mouse button was released. */
    public data class Up(val button: MouseButton) : MouseKind()

    /** The mouse moved while a button was held down. */
    public data class Drag(val button: MouseButton) : MouseKind()

    /** The mouse moved without any button held down. */
    public data object Moved : MouseKind()

    /** The scroll wheel was moved up. */
    public data object WheelUp : MouseKind()

    /** The scroll wheel was moved down. */
    public data object WheelDown : MouseKind()
}

/**
 * Represents a mouse input event.
 *
 * Coordinates are 0-indexed cell positions relative to the top-left corner of the terminal.
 * Mouse events require mouse capture to be enabled via [Terminal.enableMouseCapture].
 *
 * @property x The horizontal cell position.
 * @property y The vertical cell position.
 * @property kind The type of mouse interaction.
 * @property modifiers The keyboard modifiers held during the interaction.
 */
public data class MouseEvent(
    val x: Int,
    val y: Int,
    val kind: MouseKind,
    val modifiers: Set<KeyModifier> = emptySet(),
) : Event()

/**
 * Represents pasted text, delivered when bracketed paste mode is enabled
 * via [Terminal.enablePasteCapture].
 *
 * @property text The pasted content.
 */
public data class PasteEvent(val text: String) : Event()

/**
 * Represents the terminal window gaining or losing focus.
 *
 * @property gained true when the window was focused, false when it was unfocused.
 */
public data class FocusEvent(val gained: Boolean) : Event()
