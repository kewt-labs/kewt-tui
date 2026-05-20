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
