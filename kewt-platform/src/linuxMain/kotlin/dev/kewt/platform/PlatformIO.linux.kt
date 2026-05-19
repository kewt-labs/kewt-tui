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
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.POLLIN
import platform.posix.STDIN_FILENO
import platform.posix.poll
import platform.posix.pollfd

@OptIn(ExperimentalForeignApi::class)
internal actual fun awaitInputInternal(timeoutMs: Int): Boolean {
    memScoped {
        val pfd = alloc<pollfd>()
        pfd.fd = STDIN_FILENO
        pfd.events = POLLIN.convert()
        return poll(pfd.ptr, 1.convert(), timeoutMs.convert()) > 0
    }
}
