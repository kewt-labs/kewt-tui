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
 * An element that adds padding around the content of a UI component.
 */
public data class PaddingModifier(
    val left: Int = 0,
    val right: Int = 0,
    val top: Int = 0,
    val bottom: Int = 0,
) : Modifier.Element

/** Adds [all] characters of padding on all sides. */
public fun Modifier.padding(all: Int): Modifier = then(PaddingModifier(all, all, all, all))

/** Adds [horizontal] and [vertical] padding. */
public fun Modifier.padding(
    horizontal: Int = 0,
    vertical: Int = 0,
): Modifier =
    then(PaddingModifier(horizontal, horizontal, vertical, vertical))

/** Adds padding for each side individually. */
public fun Modifier.padding(
    left: Int = 0,
    right: Int = 0,
    top: Int = 0,
    bottom: Int = 0,
): Modifier =
    then(PaddingModifier(left, right, top, bottom))

/**
 * An element that adds margin outside the boundary of a UI component.
 */
public data class MarginModifier(
    val left: Int = 0,
    val right: Int = 0,
    val top: Int = 0,
    val bottom: Int = 0,
) : Modifier.Element

/** Adds [all] characters of margin on all sides. */
public fun Modifier.margin(all: Int): Modifier = then(MarginModifier(all, all, all, all))

/** Adds [horizontal] and [vertical] margin. */
public fun Modifier.margin(
    horizontal: Int = 0,
    vertical: Int = 0,
): Modifier =
    then(MarginModifier(horizontal, horizontal, vertical, vertical))

/** Adds margin for each side individually. */
public fun Modifier.margin(
    left: Int = 0,
    right: Int = 0,
    top: Int = 0,
    bottom: Int = 0,
): Modifier = then(MarginModifier(left, right, top, bottom))

/** An element that constrains the width of a UI component. */
public data class WidthModifier(val width: Int) : Modifier.Element

/** An element that constrains the height of a UI component. */
public data class HeightModifier(val height: Int) : Modifier.Element

/** An element that requests a fraction of the available width. */
public data class FillMaxWidthModifier(val fraction: Float = 1f) : Modifier.Element

/** An element that requests a fraction of the available height. */
public data class FillMaxHeightModifier(val fraction: Float = 1f) : Modifier.Element

/** Constrains the width to a fixed [width]. */
public fun Modifier.width(width: Int): Modifier = then(WidthModifier(width))

/** Constrains the height to a fixed [height]. */
public fun Modifier.height(height: Int): Modifier = then(HeightModifier(height))

/** Requests to fill the available width, optionally by a [fraction]. */
public fun Modifier.fillMaxWidth(fraction: Float = 1f): Modifier = then(FillMaxWidthModifier(fraction))

/** Requests to fill the available height, optionally by a [fraction]. */
public fun Modifier.fillMaxHeight(fraction: Float = 1f): Modifier = then(FillMaxHeightModifier(fraction))

/** Requests to fill all available space, optionally by a [fraction]. */
public fun Modifier.fillMaxSize(fraction: Float = 1f): Modifier = fillMaxWidth(fraction).fillMaxHeight(fraction)

/** An element that signals the component should fit its content horizontally. */
public data object WrapContentWidthModifier : Modifier.Element

/** An element that signals the component should fit its content vertically. */
public data object WrapContentHeightModifier : Modifier.Element

/** Sets horizontal sizing to fit content. */
public fun Modifier.wrapContentWidth(): Modifier = then(WrapContentWidthModifier)

/** Sets vertical sizing to fit content. */
public fun Modifier.wrapContentHeight(): Modifier = then(WrapContentHeightModifier)

/** Sets both horizontal and vertical sizing to fit content. */
public fun Modifier.wrapContentSize(): Modifier = wrapContentWidth().wrapContentHeight()

/** An element used in layout containers to distribute remaining space proportionally. */
public data class WeightModifier(val weight: Float) : Modifier.Element

/** Assigns a relative [weight] for layout distribution. */
public fun Modifier.weight(weight: Float): Modifier = then(WeightModifier(weight))

/** An element that sets the foreground (text) color. */
public data class ForegroundModifier(val color: Color) : Modifier.Element

/** An element that sets the background color. */
public data class BackgroundModifier(val color: Color) : Modifier.Element

/** An element that applies bold styling. */
public data object BoldModifier : Modifier.Element

/** An element that applies italic styling. */
public data object ItalicModifier : Modifier.Element

/** An element that applies underline styling. */
public data object UnderlineModifier : Modifier.Element

/** An element that applies strikethrough styling. */
public data object StrikethroughModifier : Modifier.Element

/**
 * An element that adds a border around a component.
 */
public data class BorderModifier(
    val style: BorderStyle,
    val color: Color = Color.Default,
) : Modifier.Element

/** Sets the foreground [color]. */
public fun Modifier.foreground(color: Color): Modifier = then(ForegroundModifier(color))

/** Sets the background [color]. */
public fun Modifier.background(color: Color): Modifier = then(BackgroundModifier(color))

/** Applies bold styling. */
public fun Modifier.bold(): Modifier = then(BoldModifier)

/** Applies italic styling. */
public fun Modifier.italic(): Modifier = then(ItalicModifier)

/** Applies underline styling. */
public fun Modifier.underline(): Modifier = then(UnderlineModifier)

/** Applies strikethrough styling. */
public fun Modifier.strikethrough(): Modifier = then(StrikethroughModifier)

/** Adds a border with the specified [style] and [color]. */
public fun Modifier.border(
    style: BorderStyle,
    color: Color = Color.Default,
): Modifier = then(BorderModifier(style, color))

/** Possible horizontal alignment options. */
public enum class HorizontalAlignment { Left, Center, Right }

/** Possible vertical alignment options. */
public enum class VerticalAlignment { Top, Center, Bottom }

/**
 * An element that defines the alignment of a component within its parent.
 */
public data class AlignmentModifier(
    val horizontal: HorizontalAlignment? = null,
    val vertical: VerticalAlignment? = null,
) : Modifier.Element

/** Sets the [horizontal] alignment. */
public fun Modifier.align(horizontal: HorizontalAlignment): Modifier = then(AlignmentModifier(horizontal = horizontal))

/** Sets the [vertical] alignment. */
public fun Modifier.align(vertical: VerticalAlignment): Modifier = then(AlignmentModifier(vertical = vertical))

/** Sets both [horizontal] and [vertical] alignment. */
public fun Modifier.align(horizontal: HorizontalAlignment, vertical: VerticalAlignment): Modifier =
    then(AlignmentModifier(horizontal, vertical))

/**
 * Accumulates all [PaddingModifier] elements in the chain into a single result.
 */
public fun Modifier.totalPadding(): PaddingModifier = foldIn(PaddingModifier()) { acc, element ->
    if (element is PaddingModifier) {
        acc.copy(
            left = acc.left + element.left,
            right = acc.right + element.right,
            top = acc.top + element.top,
            bottom = acc.bottom + element.bottom,
        )
    } else {
        acc
    }
}

/**
 * Accumulates all [MarginModifier] elements in the chain into a single result.
 */
public fun Modifier.totalMargin(): MarginModifier = foldIn(MarginModifier()) { acc, element ->
    if (element is MarginModifier) {
        acc.copy(
            left = acc.left + element.left,
            right = acc.right + element.right,
            top = acc.top + element.top,
            bottom = acc.bottom + element.bottom,
        )
    } else {
        acc
    }
}
