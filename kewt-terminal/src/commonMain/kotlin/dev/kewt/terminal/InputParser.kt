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

public class InputParser {
    private var buffer = IntArray(1024)
    private var head = 0
    private var tail = 0
    private var size = 0

    public val hasEvents: Boolean get() = size > 0

    public fun feed(bytes: ByteArray, count: Int) {
        ensureCapacity(size + count)
        for (i in 0 until count) {
            buffer[tail] = bytes[i].toInt() and 0xFF
            tail = (tail + 1) % buffer.size
            size++
        }
    }

    public fun next(): Event? {
        if (size == 0) return null
        return when (val b = peek()) {
            0x1b -> parseEscape()

            0x0d -> {
                consume()
                KeyEvent(Key.Enter)
            }

            0x7f, 0x08 -> {
                consume()
                KeyEvent(Key.Backspace)
            }

            0x09 -> {
                consume()
                KeyEvent(Key.Tab)
            }

            in 1..26 -> {
                consume()
                KeyEvent(Key.Char(('a' + b - 1)), setOf(KeyModifier.Ctrl))
            }

            in 32..126 -> {
                consume()
                KeyEvent(Key.Char(b.toChar()))
            }

            else -> {
                consume()
                null
            }
        }
    }

    private fun parseEscape(): Event? {
        val originalHead = head
        val originalSize = size

        consume() // consume 0x1b

        if (size == 0) return KeyEvent(Key.Escape)

        return when (peek()) {
            '['.code -> {
                consume()
                parseCsi() ?: run {
                    // Partial sequence, backtrack
                    head = originalHead
                    size = originalSize
                    null
                }
            }

            'O'.code -> {
                consume()
                parseSs3() ?: run {
                    head = originalHead
                    size = originalSize
                    null
                }
            }

            else -> {
                val next = peek()
                if (next in 32..126) {
                    consume()
                    KeyEvent(Key.Char(next.toChar()), setOf(KeyModifier.Alt))
                } else {
                    KeyEvent(Key.Escape)
                }
            }
        }
    }

    private fun parseCsi(): Event? {
        val params = StringBuilder()
        while (size > 0 && (peek() in '0'.code..'9'.code || peek() == ';'.code)) {
            params.append(consume().toChar())
        }
        if (size == 0) return null
        val final = consume().toChar()
        return mapCsi(params.toString(), final)
    }

    private fun mapCsi(params: String, final: Char): Event? {
        val parts = params.split(';')
        val modifiers = if (parts.size >= 2) {
            decodeModifiers(parts.last().toIntOrNull() ?: 1)
        } else {
            emptySet()
        }
        val param = parts.first()

        return when (final) {
            'A' -> KeyEvent(Key.Up, modifiers)

            'B' -> KeyEvent(Key.Down, modifiers)

            'C' -> KeyEvent(Key.Right, modifiers)

            'D' -> KeyEvent(Key.Left, modifiers)

            'H' -> KeyEvent(Key.Home, modifiers)

            'F' -> KeyEvent(Key.End, modifiers)

            'Z' -> KeyEvent(Key.BackTab, modifiers)

            '~' -> {
                val key = when (param) {
                    "3" -> Key.Delete
                    "5" -> Key.PageUp
                    "6" -> Key.PageDown
                    "15" -> Key.F(5)
                    "17" -> Key.F(6)
                    "18" -> Key.F(7)
                    "19" -> Key.F(8)
                    "20" -> Key.F(9)
                    "21" -> Key.F(10)
                    "23" -> Key.F(11)
                    "24" -> Key.F(12)
                    else -> return null
                }
                KeyEvent(key, modifiers)
            }

            else -> null
        }
    }

    private fun decodeModifiers(code: Int): Set<KeyModifier> {
        val flags = code - 1
        val mods = mutableSetOf<KeyModifier>()
        if (flags and 1 != 0) mods.add(KeyModifier.Shift)
        if (flags and 2 != 0) mods.add(KeyModifier.Alt)
        if (flags and 4 != 0) mods.add(KeyModifier.Ctrl)
        return mods
    }

    private fun parseSs3(): Event? {
        if (size == 0) return null
        return when (consume().toChar()) {
            'P' -> KeyEvent(Key.F(1))
            'Q' -> KeyEvent(Key.F(2))
            'R' -> KeyEvent(Key.F(3))
            'S' -> KeyEvent(Key.F(4))
            else -> null
        }
    }

    private fun peek(): Int = buffer[head]

    private fun consume(): Int {
        val b = buffer[head]
        head = (head + 1) % buffer.size
        size--
        return b
    }

    private fun ensureCapacity(required: Int) {
        if (required <= buffer.size) return
        var newCapacity = buffer.size * 2
        while (newCapacity < required) newCapacity *= 2

        val newBuffer = IntArray(newCapacity)
        for (i in 0 until size) {
            newBuffer[i] = buffer[(head + i) % buffer.size]
        }
        buffer = newBuffer
        head = 0
        tail = size
    }
}
