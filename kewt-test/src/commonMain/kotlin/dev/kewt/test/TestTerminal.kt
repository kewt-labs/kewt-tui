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
package dev.kewt.test

import dev.kewt.platform.Size
import dev.kewt.terminal.Event
import dev.kewt.terminal.Key
import dev.kewt.terminal.KeyEvent
import dev.kewt.terminal.KeyModifier
import dev.kewt.terminal.Terminal

/**
 * A mock implementation of [Terminal] for use in unit and integration tests.
 *
 * It allows simulating user input, tracking terminal state changes (like cursor movement
 * or raw mode transitions), and capturing raw ANSI output.
 *
 * @param width The initial width of the terminal.
 * @param height The initial height of the terminal.
 */
public class TestTerminal(
    private var width: Int = 80,
    private var height: Int = 24,
) : Terminal {
    private val eventQueue = ArrayDeque<Event>()
    private val output = StringBuilder()

    /** The current horizontal position of the cursor (0-indexed). */
    public var cursorX: Int = 0
        private set

    /** The current vertical position of the cursor (0-indexed). */
    public var cursorY: Int = 0
        private set

    /** Whether the terminal is currently in raw mode. */
    public var isRawMode: Boolean = false
        private set

    /** Whether the terminal cursor is currently hidden. */
    public var isCursorHidden: Boolean = false
        private set

    /** The current window title. */
    public var windowTitle: String = ""
        private set

    override fun enterRawMode() {
        isRawMode = true
    }

    override fun exitRawMode() {
        isRawMode = false
    }

    override fun size(): Size = Size(width, height)

    override fun read(): Event? = eventQueue.removeFirstOrNull()

    override fun write(text: String) {
        output.append(text)
    }

    override fun flush() {}

    override fun moveCursor(x: Int, y: Int) {
        cursorX = x
        cursorY = y
    }

    override fun hideCursor() {
        isCursorHidden = true
    }

    override fun showCursor() {
        isCursorHidden = false
    }

    override fun clear() {
        output.clear()
    }

    override fun clearLine() {}

    override fun setTitle(title: String) {
        windowTitle = title
    }

    /**
     * In a test environment, poll returns true immediately if there are pending events.
     */
    override fun poll(timeoutMs: Int): Boolean = eventQueue.isNotEmpty()

    /**
     * Simulates the user pressing a specific key.
     */
    public fun sendKey(
        key: Key,
        modifiers: Set<KeyModifier> = emptySet(),
    ) {
        eventQueue.addLast(KeyEvent(key, modifiers))
    }

    /**
     * Simulates the user typing a single character.
     */
    public fun sendChar(c: Char) {
        eventQueue.addLast(KeyEvent(Key.Char(c)))
    }

    /**
     * Simulates a terminal resize event.
     */
    public fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    /**
     * Returns the raw ANSI output captured since the last clear.
     */
    public fun output(): String = output.toString()
}
