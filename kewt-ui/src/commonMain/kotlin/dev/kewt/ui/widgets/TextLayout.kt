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
package dev.kewt.ui.widgets

import dev.kewt.core.buffer.UnicodeWidth

/**
 * Greedily wraps [line] at word boundaries so that every output line occupies at
 * most [maxWidth] terminal cells, appending results to [out].
 *
 * Words longer than [maxWidth] are hard-broken; spaces that would land at a wrap
 * point are dropped. Requires [maxWidth] to be positive.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod", "NestedBlockDepth")
internal fun wrapLine(
    line: String,
    maxWidth: Int,
    out: MutableList<String>,
) {
    if (UnicodeWidth.displayWidth(line) <= maxWidth) {
        out.add(line)
        return
    }

    val current = StringBuilder()
    var currentWidth = 0
    var lastSpace = -1
    var index = 0
    while (index < line.length) {
        val ch = line[index]

        if (ch == ' ') {
            if (currentWidth + 1 > maxWidth) {
                // The separator does not fit: wrap here and drop the space.
                if (currentWidth > 0) {
                    out.add(current.toString())
                    current.setLength(0)
                    currentWidth = 0
                    lastSpace = -1
                }
            } else {
                lastSpace = current.length
                current.append(' ')
                currentWidth += 1
            }
            index += 1
            continue
        }

        val isPair = ch.isHighSurrogate() && index + 1 < line.length && line[index + 1].isLowSurrogate()
        val codePoint =
            if (isPair) {
                0x10000 + ((ch.code - 0xD800) shl 10) + (line[index + 1].code - 0xDC00)
            } else {
                ch.code
            }
        val w = UnicodeWidth.of(codePoint)
        val step = if (isPair) 2 else 1

        if (w == 0) {
            // Combining marks attach to the previous grapheme and take no cells.
            current.append(ch)
            if (isPair) current.append(line[index + 1])
            index += step
            continue
        }

        if (currentWidth + w > maxWidth && currentWidth > 0) {
            if (lastSpace > 0) {
                // Break at the last word boundary.
                val head = current.substring(0, lastSpace)
                val tail = current.substring(lastSpace + 1)
                out.add(head)
                current.setLength(0)
                current.append(tail)
                currentWidth = UnicodeWidth.displayWidth(tail)
                lastSpace = -1
            } else {
                // No usable boundary: hard-break the overlong word.
                out.add(current.toString())
                current.setLength(0)
                currentWidth = 0
                lastSpace = -1
            }
            // The carried-over tail may itself already be too wide for [w].
            while (currentWidth + w > maxWidth && currentWidth > 0) {
                out.add(current.toString())
                current.setLength(0)
                currentWidth = 0
            }
        }

        current.append(ch)
        if (isPair) current.append(line[index + 1])
        currentWidth += w
        index += step
    }
    if (current.isNotEmpty()) out.add(current.toString())
}
