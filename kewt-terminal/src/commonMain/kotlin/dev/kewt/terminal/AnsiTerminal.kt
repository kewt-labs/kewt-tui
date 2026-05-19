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

import dev.kewt.platform.PlatformIO
import dev.kewt.platform.PosixSignal
import dev.kewt.platform.RawMode
import dev.kewt.platform.SignalHandler
import dev.kewt.platform.Size
import dev.kewt.platform.TerminalSize

public class AnsiTerminal(
    public val colorMode: ColorMode = ColorMode.detect(),
) : Terminal {
    private val parser = InputParser()
    private val readBuf = ByteArray(256)
    private var cachedSize = Size(80, 24)

    override fun enterRawMode() {
        RawMode.enter()
        cachedSize = TerminalSize.query()
        SignalHandler.register(PosixSignal.SIGWINCH) {
            cachedSize = TerminalSize.query()
        }
        write("\u001b[?1049h")
    }

    override fun exitRawMode() {
        write("\u001b[?1049l")
        flush()
        SignalHandler.unregister(PosixSignal.SIGWINCH)
        RawMode.exit()
    }

    override fun size(): Size = cachedSize

    override fun read(): Event? {
        val n = PlatformIO.readBytes(readBuf, readBuf.size)
        if (n <= 0) return null
        parser.feed(readBuf, n)
        return parser.next()
    }

    override fun write(text: String) {
        PlatformIO.writeString(text)
    }

    override fun flush() {
        PlatformIO.flush()
    }

    override fun moveCursor(x: Int, y: Int) {
        write("\u001b[${y + 1};${x + 1}H")
    }

    override fun hideCursor() {
        write("\u001b[?25l")
    }

    override fun showCursor() {
        write("\u001b[?25h")
    }

    override fun clear() {
        write("\u001b[2J\u001b[H")
    }

    override fun clearLine() {
        write("\u001b[2K")
    }
}
