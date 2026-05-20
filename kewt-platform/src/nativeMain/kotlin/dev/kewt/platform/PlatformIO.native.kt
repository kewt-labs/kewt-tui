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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.posix.STDIN_FILENO
import platform.posix.STDOUT_FILENO
import platform.posix.fflush
import platform.posix.read
import platform.posix.stdout
import platform.posix.write

@OptIn(ExperimentalForeignApi::class)
public actual object PlatformIO {
    public actual fun readBytes(buffer: ByteArray, maxLen: Int): Int {
        buffer.usePinned { pinned ->
            return read(STDIN_FILENO, pinned.addressOf(0), maxLen.convert()).toInt()
        }
    }

    public actual fun writeString(text: String) {
        val bytes = text.encodeToByteArray()
        bytes.usePinned { pinned ->
            write(STDOUT_FILENO, pinned.addressOf(0), bytes.size.convert())
        }
    }

    public actual fun flush() {
        fflush(stdout)
    }

    public actual fun awaitInput(timeoutMs: Int): Boolean = awaitInputInternal(timeoutMs)
}

internal expect fun awaitInputInternal(timeoutMs: Int): Boolean
