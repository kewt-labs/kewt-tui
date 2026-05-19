/*
* Copyright 2026 lscythe
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
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.STDOUT_FILENO
import platform.posix.TIOCGWINSZ
import platform.posix.ioctl
import platform.posix.winsize

@OptIn(ExperimentalForeignApi::class)
public actual object TerminalSize {
    public actual fun query(): Size {
        memScoped {
            val ws = alloc<winsize>()
            ioctl(STDOUT_FILENO, TIOCGWINSZ, ws.ptr)
            return Size(ws.ws_col.toInt(), ws.ws_row.toInt())
        }
    }
}
