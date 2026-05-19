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
    private val buffer = mutableListOf<Int>()

    public fun feed(byte: ByteArray, count: Int) {
        for (i in 0 until count) buffer.add(byte[i].toInt() and 0xFF)
    }

    public fun next(): Event? {
        if (buffer.isEmpty()) return null
        return when (val b = buffer.removeFirst()) {
            0x1b -> parseEscape()
            0x0d -> KeyEvent(Key.Enter)
            0x7f, 0x08 -> KeyEvent(Key.Backspace)
            0x09 -> KeyEvent(Key.Tab)
            in 1..26 -> KeyEvent(Key.Char(('a' + b - 1)), setOf(KeyModifier.Ctrl))
            in 32..126 -> KeyEvent(Key.Char(b.toChar()))
            else -> null
        }
    }

    private fun parseEscape(): Event? {
        if (buffer.isEmpty()) return KeyEvent(Key.Escape)
        return when (buffer.first()) {
            '['.code -> {
                buffer.removeFirst()
                parseCsi()
            }

            'O'.code -> {
                buffer.removeFirst()
                parseSs3()
            }

            else -> {
                val next = buffer.first()
                if (next in 32..126) {
                    buffer.removeFirst()
                    KeyEvent(Key.Char(next.toChar()), setOf(KeyModifier.Alt))
                } else {
                    KeyEvent(Key.Escape)
                }
            }
        }
    }

    private fun parseCsi(): Event? {
        if (buffer.isEmpty()) return KeyEvent(Key.Escape)

        val params = StringBuilder()
        while (buffer.isNotEmpty() && (buffer.first() in '0'.code..'9'.code || buffer.firstOrNull() == ';'.code)) {
            params.append(buffer.removeFirst().toChar())
        }
        if (buffer.isEmpty()) return null
        val final = buffer.removeFirst().toChar()
        return mapCsi(params.toString(), final)
    }

    private fun mapCsi(params: String, final: Char): Event? {
        val parts = params.split(';')
        val modifiers =
            if (parts.size >= 2) {
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
        if (buffer.isEmpty()) return null
        return when (buffer.removeFirst().toChar()) {
            'P' -> KeyEvent(Key.F(1))
            'Q' -> KeyEvent(Key.F(2))
            'R' -> KeyEvent(Key.F(3))
            'S' -> KeyEvent(Key.F(4))
            else -> null
        }
    }
}
