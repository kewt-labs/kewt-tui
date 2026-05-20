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

import dev.kewt.platform.Size

/**
 * The high-level interface for terminal interaction.
 *
 * Provides methods for controlling the terminal state, managing the cursor,
 * and handling input/output.
 */
public interface Terminal {
    /** Enters terminal raw mode and initializes state. */
    public fun enterRawMode()

    /** Exits raw mode and restores the terminal state. */
    public fun exitRawMode()

    /** Returns the current physical size of the terminal. */
    public fun size(): Size

    /**
     * Attempts to read the next input [Event].
     *
     * @return The next event, or null if none are available.
     */
    public fun read(): Event?

    /** Writes raw text to the terminal. */
    public fun write(text: String)

    /** Flushes any pending output to the terminal. */
    public fun flush()

    /** Moves the cursor to the specified coordinates (0-indexed). */
    public fun moveCursor(x: Int, y: Int)

    /** Hides the terminal cursor. */
    public fun hideCursor()

    /** Shows the terminal cursor. */
    public fun showCursor()

    /** Clears the entire terminal screen. */
    public fun clear()

    /** Clears the current line. */
    public fun clearLine()

    /**
     * Waits for input to become available.
     *
     * @param timeoutMs Maximum time to wait in milliseconds.
     * @return true if input is available, false otherwise.
     */
    public fun poll(timeoutMs: Int): Boolean
}
