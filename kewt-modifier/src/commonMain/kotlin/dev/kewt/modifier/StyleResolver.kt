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
 * Collapses all styling elements of a modifier chain into a single [Style].
 *
 * Elements towards the tail of the chain (applied later) win over earlier ones:
 * ```kotlin
 * Modifier.foreground(Color.Red).foreground(Color.Blue).resolveStyle().foreground // Blue
 * ```
 *
 * Boolean attribute modifiers (like [Modifier.bold]) only ever turn attributes on;
 * use [Modifier.style] with an explicit `false` to force an attribute off.
 */
public fun Modifier.resolveStyle(): Style =
    foldIn(Style.Empty) { acc, element ->
        when (element) {
            is ForegroundModifier -> acc.copy(foreground = element.color)
            is BackgroundModifier -> acc.copy(background = element.color)
            is BoldModifier -> acc.copy(bold = true)
            is ItalicModifier -> acc.copy(italic = true)
            is UnderlineModifier -> acc.copy(underline = true)
            is StrikethroughModifier -> acc.copy(strikethrough = true)
            is DimModifier -> acc.copy(dim = true)
            is BlinkModifier -> acc.copy(blink = true)
            is ReverseModifier -> acc.copy(reverse = true)
            is HiddenModifier -> acc.copy(hidden = true)
            is StyleModifier -> acc.merge(element.style)
            else -> acc
        }
    }
