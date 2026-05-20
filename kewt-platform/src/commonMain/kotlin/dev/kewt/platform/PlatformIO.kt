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
package dev.kewt.platform

/**
 * Provides low-level input and output operations for the terminal.
 */
public expect object PlatformIO {
    /**
     * Reads up to [maxLen] bytes into the provided [buffer].
     *
     * @return The number of bytes actually read.
     */
    public fun readBytes(
        buffer: ByteArray,
        maxLen: Int,
    ): Int

    /**
     * Writes the provided [text] to the standard output.
     */
    public fun writeString(text: String)

    /**
     * Flushes the standard output buffer.
     */
    public fun flush()

    /**
     * Waits for input to become available on stdin.
     *
     * @param timeoutMs The maximum time to wait in milliseconds. -1 for infinite.
     * @return true if input is available, false if the timeout was reached.
     */
    public fun awaitInput(timeoutMs: Int): Boolean
}
