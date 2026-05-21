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
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import platform.posix.F_GETFL
import platform.posix.F_SETFL
import platform.posix.O_NONBLOCK
import platform.posix.TCSAFLUSH
import platform.posix.cfmakeraw
import platform.posix.fcntl
import platform.posix.memcpy
import platform.posix.tcgetattr
import platform.posix.tcsetattr
import platform.posix.termios

@OptIn(ExperimentalForeignApi::class)
public actual object RawMode {
    private val original = ByteArray(sizeOf<termios>().toInt())
    private var saved = false
    private var originalFlags = 0

    public actual fun enter() {
        memScoped {
            val fd = findTtyFd()
            if (fd < 0) return

            val term = alloc<termios>()
            if (tcgetattr(fd, term.ptr) == 0) {
                // Manually copy the memory to our buffer to save the state
                original.usePinned { pinned ->
                    memcpy(pinned.addressOf(0), term.ptr, sizeOf<termios>().convert())
                }
                saved = true

                val raw = alloc<termios>()
                tcgetattr(fd, raw.ptr)
                cfmakeraw(raw.ptr)
                tcsetattr(fd, TCSAFLUSH, raw.ptr)

                // Set stdin to non-blocking so read() returns immediately
                originalFlags = fcntl(fd, F_GETFL)
                fcntl(fd, F_SETFL, originalFlags or O_NONBLOCK)
            }
        }
    }

    public actual fun exit() {
        if (saved) {
            memScoped {
                val fd = findTtyFd()
                if (fd < 0) return

                // Restore blocking mode
                fcntl(fd, F_SETFL, originalFlags)

                // Reconstruct termios from our buffer
                val term = alloc<termios>()
                original.usePinned { pinned ->
                    memcpy(term.ptr, pinned.addressOf(0), sizeOf<termios>().convert())
                }
                tcsetattr(fd, TCSAFLUSH, term.ptr)

                saved = false
            }
        }
    }
}
