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
package dev.kewt.modifier

/**
 * An immutable bundle of visual attributes, similar to a `Style` in lipgloss or ratatui.
 *
 * Every property is nullable: a `null` value means "not specified", so styles can be
 * layered. When two styles are combined with [merge], only the specified (non-null)
 * properties of the other style override the receiver.
 *
 * Example:
 * ```kotlin
 * val base = Style(foreground = Color.Cyan, bold = true)
 * val overlay = Style(background = Color.rgb(0x222222))
 * val combined = base.merge(overlay) // cyan bold text on dark gray
 * ```
 */
public data class Style(
    val foreground: Color? = null,
    val background: Color? = null,
    val bold: Boolean? = null,
    val italic: Boolean? = null,
    val underline: Boolean? = null,
    val strikethrough: Boolean? = null,
    val dim: Boolean? = null,
    val blink: Boolean? = null,
    val reverse: Boolean? = null,
    val hidden: Boolean? = null,
) {
    /**
     * Combines this style with [other].
     *
     * Non-null properties of [other] take precedence; null properties fall back
     * to the values of this style.
     */
    public fun merge(other: Style): Style =
        Style(
            foreground = other.foreground ?: foreground,
            background = other.background ?: background,
            bold = other.bold ?: bold,
            italic = other.italic ?: italic,
            underline = other.underline ?: underline,
            strikethrough = other.strikethrough ?: strikethrough,
            dim = other.dim ?: dim,
            blink = other.blink ?: blink,
            reverse = other.reverse ?: reverse,
            hidden = other.hidden ?: hidden,
        )

    public companion object {
        /** An empty style that specifies no attributes. */
        public val Empty: Style = Style()

        /** Creates a style with a foreground [color]. */
        public fun foreground(color: Color): Style = Style(foreground = color)

        /** Creates a style with a background [color]. */
        public fun background(color: Color): Style = Style(background = color)

        /** Creates a style with bold text. */
        public fun bold(): Style = Style(bold = true)
    }
}
