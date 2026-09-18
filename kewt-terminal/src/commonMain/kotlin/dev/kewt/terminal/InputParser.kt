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

import dev.kewt.platform.currentTimeMs

/**
 * A state-machine-based parser for terminal input bytes.
 *
 * Converts raw bytes from standard input into high-level [Event] objects.
 * Handles multibyte ANSI escape sequences for special keys and modifiers,
 * UTF-8 encoded text, SGR and X10 mouse reporting, focus events, and
 * bracketed paste.
 */
@Suppress("TooManyFunctions")
public class InputParser {
    private var buffer = IntArray(1024)
    private var head = 0
    private var tail = 0
    private var size = 0

    // Bracketed paste state
    private var pasteMode = false
    private val pendingPaste = mutableListOf<Int>()
    private var pasteEndMatchIndex = 0

    // Partial UTF-8 sequence timeout tracking
    private var partialSince = 0L

    /**
     * Whether there are any bytes currently in the buffer that could form an event.
     */
    public val hasEvents: Boolean get() = size > 0

    /**
     * Feeds raw [bytes] into the internal parser buffer.
     *
     * @param count The number of bytes to read from the array.
     */
    public fun feed(
        bytes: ByteArray,
        count: Int,
    ) {
        ensureCapacity(size + count)
        for (i in 0 until count) {
            buffer[tail] = bytes[i].toInt() and 0xFF
            tail = (tail + 1) % buffer.size
            size++
        }
        partialSince = 0L
    }

    /**
     * Attempts to parse the next event from the internal buffer.
     *
     * @return The parsed [Event], or null if no complete event is available.
     */
    public fun next(): Event? {
        if (pasteMode) return nextPasteEvent()
        if (size == 0) return null
        return when (val b = peek()) {
            0x1b -> parseEscape()

            0x0d, 0x0a -> {
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

            0x00 -> {
                consume()
                KeyEvent(Key.Char(' '), setOf(KeyModifier.Ctrl))
            }

            in 1..26 -> {
                consume()
                KeyEvent(Key.Char(('a' + b - 1)), setOf(KeyModifier.Ctrl))
            }

            in 28..31 -> {
                consume()
                KeyEvent(Key.Char(('\\' + b - 28)), setOf(KeyModifier.Ctrl))
            }

            in 32..126 -> {
                consume()
                KeyEvent(Key.Char(b.toChar()))
            }

            else -> parseUtf8(b)
        }
    }

    /**
     * Discards any buffered bytes and resets parser state.
     */
    public fun reset() {
        head = 0
        tail = 0
        size = 0
        pasteMode = false
        pendingPaste.clear()
        pasteEndMatchIndex = 0
        partialSince = 0L
    }

    // ---------------------------------------------------------------------
    // UTF-8
    // ---------------------------------------------------------------------

    private fun parseUtf8(lead: Int): Event? {
            val needed = when {
                lead in 0xC2..0xDF -> 2

                lead in 0xE0..0xEF -> 3

                lead in 0xF0..0xF4 -> 4

                else -> {
                // Invalid lead byte (continuation byte or overlong encoding) - drop it
                consume()
                return null
            }
        }

        if (size < needed) {
            // Wait briefly for the rest of the sequence, then drop the lead byte
            if (partialTimedOut()) consume()
            return null
        }

        val mask = when (needed) {
            2 -> 0x1F
            3 -> 0x0F
            else -> 0x07
        }
        for (k in 1 until needed) {
            if (buffer[(head + k) % buffer.size] and 0xC0 != 0x80) {
                // Malformed sequence - drop the lead byte only
                consume()
                return null
            }
        }

        var codePoint = lead and mask
        consume()
        for (k in 1 until needed) {
            codePoint = (codePoint shl 6) or (consume() and 0x3F)
        }
        partialSince = 0L

        return if (codePoint <= 0xFFFF) {
            KeyEvent(Key.Char(codePoint.toChar()))
        } else {
            KeyEvent(Key.Text(codePointToString(codePoint)))
        }
    }

    // ---------------------------------------------------------------------
    // Escape sequences
    // ---------------------------------------------------------------------

    private fun parseEscape(): Event? {
        val snapshotHead = head
        val snapshotSize = size

        consume() // consume 0x1b

        if (size == 0) return KeyEvent(Key.Escape)

        return when (peek()) {
            '['.code -> {
                consume()
                parseCsi(snapshotHead, snapshotSize)
            }

            'O'.code -> {
                consume()
                parseSs3(snapshotHead, snapshotSize)
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

    private fun parseCsi(
        snapshotHead: Int,
        snapshotSize: Int,
    ): Event? {
        // Optional private-mode prefix: '<' (mouse), '>' (kitty), '=' or '?'
        var prefix = 0
        while (size > 0) {
            val b = peek()
            if (b == '<'.code || b == '='.code || b == '>'.code || b == '?'.code) {
                prefix = b
                consume()
            } else {
                break
            }
        }

        val params = StringBuilder()
        while (size > 0 && (peek() in '0'.code..'9'.code || peek() == ';'.code || peek() == ':'.code)) {
            params.append(consume().toChar())
        }

        // Skip intermediate bytes (e.g. the space in CSI SP q)
        while (size > 0 && peek() in 0x20..0x2F) {
            consume()
        }

        if (size == 0) {
            // Partial sequence - backtrack and wait for more bytes.
            // After a timeout, drop the incomplete sequence to avoid getting stuck.
            if (partialTimedOut()) return null
            restore(snapshotHead, snapshotSize)
            return null
        }

        val final = consume().toChar()

        // Legacy X10 mouse needs three trailing bytes
        if (final == 'M' && prefix == 0) {
            if (size < 3) {
                if (partialTimedOut()) return null
                restore(snapshotHead, snapshotSize)
                return null
            }
            partialSince = 0L
            return parseLegacyMouse()
        }

        partialSince = 0L
        return mapCsi(prefix, params.toString(), final)
    }

    private fun mapCsi(
        prefix: Int,
        params: String,
        final: Char,
    ): Event? {
        val parts = params.split(';')
        val first = parts.first().substringBefore(':')
        val modifierCode = if (parts.size >= 2) parts[1].substringBefore(':').toIntOrNull() ?: 1 else 1
        val modifiers = decodeModifiers(modifierCode)

        return when (final) {
            'A' -> KeyEvent(Key.Up, modifiers)

            'B' -> KeyEvent(Key.Down, modifiers)

            'C' -> KeyEvent(Key.Right, modifiers)

            'D' -> KeyEvent(Key.Left, modifiers)

            'F' -> KeyEvent(Key.End, modifiers)

            'H' -> KeyEvent(Key.Home, modifiers)

            'Z' -> KeyEvent(Key.BackTab, modifiers)

            'I' -> FocusEvent(true)

            'O' -> FocusEvent(false)

            // Mouse sequences carry their modifiers inside the button code, not
            // in the second parameter (which is the X coordinate).
            'M', 'm' -> if (prefix == '<'.code) parseSgrMouse(parts, final == 'm') else null

            'u' -> parseKitty(first, modifiers)

            '~' -> mapTildeKey(first, modifiers)

            else -> null
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun mapTildeKey(
        param: String,
        modifiers: Set<KeyModifier>,
    ): Event? {
        val code = param.toIntOrNull() ?: return null
        val key = when (code) {
            1, 7 -> Key.Home

            2 -> Key.Insert

            3 -> Key.Delete

            4, 8 -> Key.End

            5 -> Key.PageUp

            6 -> Key.PageDown

            11 -> Key.F(1)

            12 -> Key.F(2)

            13 -> Key.F(3)

            14 -> Key.F(4)

            15 -> Key.F(5)

            17 -> Key.F(6)

            18 -> Key.F(7)

            19 -> Key.F(8)

            20 -> Key.F(9)

            21 -> Key.F(10)

            23 -> Key.F(11)

            24 -> Key.F(12)

            25 -> Key.F(13)

            26 -> Key.F(14)

            28 -> Key.F(15)

            29 -> Key.F(16)

            31 -> Key.F(17)

            32 -> Key.F(18)

            33 -> Key.F(19)

            34 -> Key.F(20)


            200 -> {
                // Bracketed paste start
                pasteMode = true
                pendingPaste.clear()
                pasteEndMatchIndex = 0
                return null
            }

            else -> return null
        }
        return KeyEvent(key, modifiers)
    }

    private fun parseKitty(
        codePointParam: String,
        modifiers: Set<KeyModifier>,
    ): Event? {
        val codePoint = codePointParam.toIntOrNull() ?: return null
        val key = when (codePoint) {
            9 -> if (KeyModifier.Shift in modifiers) Key.BackTab else Key.Tab
            13 -> Key.Enter
            27 -> Key.Escape
            127 -> Key.Backspace
            else -> codePointToKey(codePoint) ?: return null
        }
        return KeyEvent(key, modifiers)
    }

    /**
     * Maps a Unicode code point reported by the CSI-u protocol onto a [Key].
     */
    private fun codePointToKey(codePoint: Int): Key? =
        when {
            codePoint < 0 -> null
            codePoint <= 0xFFFF -> Key.Char(codePoint.toChar())
            else -> Key.Text(codePointToString(codePoint))
        }

    private fun parseSgrMouse(
        parts: List<String>,
        released: Boolean,
    ): Event? {
        if (parts.size < 3) return null
        val cb = parts[0].toIntOrNull() ?: return null
        val x = (parts[1].toIntOrNull() ?: return null) - 1
        val y = (parts[2].toIntOrNull() ?: return null) - 1

        val wheel = cb and 64 != 0
        val motion = cb and 32 != 0
        val buttonBits = cb and 3

        val modifiers = buildSet {
            if (cb and 4 != 0) add(KeyModifier.Shift)
            if (cb and 8 != 0) add(KeyModifier.Alt)
            if (cb and 16 != 0) add(KeyModifier.Ctrl)
        }

        val kind = when {
            wheel -> if (buttonBits == 0) MouseKind.WheelUp else MouseKind.WheelDown
            released -> MouseKind.Up(buttonOf(buttonBits))
            motion -> if (buttonBits == 3) MouseKind.Moved else MouseKind.Drag(buttonOf(buttonBits))
            else -> MouseKind.Down(buttonOf(buttonBits))
        }
        return MouseEvent(x, y, kind, modifiers)
    }

    private fun parseLegacyMouse(): Event? {
        val rawCb = consume()
        val cx = consume()
        val cy = consume()

        // X10 reports every field with a +32 bias; remove it before decoding.
        val cb = rawCb - 32

        val wheel = cb and 64 != 0
        val motion = cb and 32 != 0
        val buttonBits = cb and 3

        val modifiers = buildSet {
            if (cb and 4 != 0) add(KeyModifier.Shift)
            if (cb and 8 != 0) add(KeyModifier.Alt)
            if (cb and 16 != 0) add(KeyModifier.Ctrl)
        }

        val kind = when {
            wheel -> if (buttonBits == 0) MouseKind.WheelUp else MouseKind.WheelDown
            motion -> if (buttonBits == 3) MouseKind.Moved else MouseKind.Drag(buttonOf(buttonBits))
            buttonBits == 3 -> MouseKind.Up(MouseButton.None)
            else -> MouseKind.Down(buttonOf(buttonBits))
        }
        return MouseEvent(cx - 33, cy - 33, kind, modifiers)
    }

    private fun buttonOf(bits: Int): MouseButton = when (bits) {
        0 -> MouseButton.Left
        1 -> MouseButton.Middle
        2 -> MouseButton.Right
        else -> MouseButton.None
    }

    private fun parseSs3(
        snapshotHead: Int,
        snapshotSize: Int,
    ): Event? {
        if (size == 0) {
            // "ESC O" without a final byte is ambiguous: it is either an incomplete
            // SS3 sequence or Alt+O. Wait briefly, then fall back to Alt+O.
            if (partialTimedOut()) return KeyEvent(Key.Char('O'), setOf(KeyModifier.Alt))
            restore(snapshotHead, snapshotSize)
            return null
        }
        partialSince = 0L
        return when (consume().toChar()) {
            'P' -> KeyEvent(Key.F(1))
            'Q' -> KeyEvent(Key.F(2))
            'R' -> KeyEvent(Key.F(3))
            'S' -> KeyEvent(Key.F(4))
            'A' -> KeyEvent(Key.Up)
            'B' -> KeyEvent(Key.Down)
            'C' -> KeyEvent(Key.Right)
            'D' -> KeyEvent(Key.Left)
            'H' -> KeyEvent(Key.Home)
            'F' -> KeyEvent(Key.End)
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

    // ---------------------------------------------------------------------
    // Bracketed paste
    // ---------------------------------------------------------------------

    private fun nextPasteEvent(): Event? {
        while (size > 0) {
            val b = peek()
            if (b == PASTE_END[pasteEndMatchIndex]) {
                consume()
                pasteEndMatchIndex++
                if (pasteEndMatchIndex == PASTE_END.size) {
                    pasteMode = false
                    pasteEndMatchIndex = 0
                    val text = decodeUtf8Bytes(pendingPaste)
                    pendingPaste.clear()
                    return PasteEvent(text)
                }
            } else {
                // Any partially matched terminator bytes are regular paste content
                for (k in 0 until pasteEndMatchIndex) {
                    pendingPaste.add(PASTE_END[k])
                }
                pasteEndMatchIndex = 0
                if (b == PASTE_END[0]) continue
                consume()
                pendingPaste.add(b)
            }
        }
        return null
    }

    // ---------------------------------------------------------------------
    // Ring buffer plumbing
    // ---------------------------------------------------------------------

    /**
     * Tracks how long the parser has been waiting for the continuation of an
     * incomplete escape sequence. Returns true once the wait exceeds the timeout,
     * at which point the caller should give up on the partial sequence.
     */
    private fun partialTimedOut(): Boolean {
        val now = currentTimeMs()
        if (partialSince == 0L) {
            partialSince = now
            return false
        }
        if (now - partialSince > PARTIAL_SEQUENCE_TIMEOUT_MS) {
            partialSince = 0L
            return true
        }
        return false
    }

    private fun peek(): Int = buffer[head]

    private fun consume(): Int {
        val b = buffer[head]
        head = (head + 1) % buffer.size
        size--
        return b
    }

    private fun restore(
        snapshotHead: Int,
        snapshotSize: Int,
    ) {
        head = snapshotHead
        size = snapshotSize
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

    private companion object {
        const val PARTIAL_SEQUENCE_TIMEOUT_MS = 100L
        val PASTE_END = intArrayOf(0x1b, '['.code, '2'.code, '0'.code, '1'.code, '~'.code)
    }
}

/**
 * Converts a Unicode code point into the equivalent Java [String] (surrogate pair when needed).
 */
internal fun codePointToString(codePoint: Int): String {
    if (codePoint <= 0xFFFF) return codePoint.toChar().toString()
    val value = codePoint - 0x10000
    val high = (0xD800 + (value shr 10)).toChar()
    val low = (0xDC00 + (value and 0x3FF)).toChar()
    return "$high$low"
}

/**
 * Decodes a list of raw UTF-8 bytes into a [String], replacing malformed
 * sequences with the Unicode replacement character.
 */
internal fun decodeUtf8Bytes(bytes: List<Int>): String {
    val sb = StringBuilder()
    var i = 0
    while (i < bytes.size) {
        val b = bytes[i]
        if (b < 0x80) {
            sb.append(b.toChar())
            i++
            continue
        }
        val needed = when {
            b in 0xC2..0xDF -> 2
            b in 0xE0..0xEF -> 3
            b in 0xF0..0xF4 -> 4
            else -> 0
        }
        if (needed == 0 || i + needed > bytes.size) {
            sb.append('\uFFFD')
            i++
            continue
        }
        val mask = when (needed) {
            2 -> 0x1F
            3 -> 0x0F
            else -> 0x07
        }
        var codePoint = b and mask
        var valid = true
        for (k in 1 until needed) {
            val c = bytes[i + k]
            if (c and 0xC0 != 0x80) {
                valid = false
                break
            }
            codePoint = (codePoint shl 6) or (c and 0x3F)
        }
        if (!valid) {
            sb.append('\uFFFD')
            i++
            continue
        }
        if (codePoint <= 0xFFFF) sb.append(codePoint.toChar()) else sb.append(codePointToString(codePoint))
        i += needed
    }
    return sb.toString()
}
