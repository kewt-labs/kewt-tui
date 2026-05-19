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
import kotlinx.cinterop.staticCFunction
import platform.posix.SIGINT
import platform.posix.SIGTERM
import platform.posix.SIGWINCH
import platform.posix.SIG_DFL
import platform.posix.signal

private val handlers = mutableMapOf<Int, () -> Unit>()

@OptIn(ExperimentalForeignApi::class)
public actual object SignalHandler {
    public actual fun register(signal: PosixSignal, handler: () -> Unit) {
        val code = signal.toCode()
        handlers[code] = handler
        signal(code, staticCFunction { sig -> handlers[sig]?.invoke() })
    }

    public actual fun unregister(signal: PosixSignal) {
        val code = signal.toCode()
        handlers.remove(code)
        signal(code, SIG_DFL)
    }
}

private fun PosixSignal.toCode(): Int = when (this) {
    PosixSignal.SIGINT -> SIGINT
    PosixSignal.SIGTERM -> SIGTERM
    PosixSignal.SIGWINCH -> SIGWINCH
}
