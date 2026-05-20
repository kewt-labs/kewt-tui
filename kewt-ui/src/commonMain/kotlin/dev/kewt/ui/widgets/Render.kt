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

import dev.kewt.core.buffer.Buffer
import dev.kewt.modifier.BorderStyle
import dev.kewt.modifier.Color

public fun renderText(
    buffer: Buffer,
    x: Int,
    y: Int,
    text: String,
    fg: Color = Color.Default,
    bg: Color = Color.Default,
    bold: Boolean = false,
    italic: Boolean = false,
    underline: Boolean = false,
    strikethrough: Boolean = false,
) {
    buffer.writeString(
        x = x,
        y = y,
        text = text,
        foreground = fg,
        background = bg,
        bold = bold,
        italic = italic,
        underline = underline,
        strikethrough = strikethrough,
    )
}

public fun renderBorder(
    buffer: Buffer,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    style: BorderStyle,
    color: Color = Color.Default,
) {
    buffer.setChar(x, y, style.topLeft, color)
    buffer.setChar(x + width - 1, y, style.topRight, color)
    buffer.setChar(x, y + height - 1, style.bottomLeft, color)
    buffer.setChar(x + width - 1, y + height - 1, style.bottomRight, color)
    for (cx in (x + 1) until (x + width - 1)) {
        buffer.setChar(cx, y, style.horizontal, color)
        buffer.setChar(cx, y + height - 1, style.horizontal, color)
    }
    for (cy in (y + 1) until (y + height - 1)) {
        buffer.setChar(x, cy, style.vertical, color)
        buffer.setChar(x + width - 1, cy, style.vertical, color)
    }
}
