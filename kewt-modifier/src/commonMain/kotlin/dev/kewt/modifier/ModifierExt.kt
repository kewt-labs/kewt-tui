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

public data class PaddingModifier(
    val left: Int = 0,
    val right: Int = 0,
    val top: Int = 0,
    val bottom: Int = 0,
) : Modifier.Element

public fun Modifier.padding(all: Int): Modifier = then(PaddingModifier(all, all, all, all))

public fun Modifier.padding(
    horizontal: Int = 0,
    vertical: Int = 0,
): Modifier =
    then(PaddingModifier(horizontal, horizontal, vertical, vertical))

public fun Modifier.padding(
    left: Int = 0,
    right: Int = 0,
    top: Int = 0,
    bottom: Int = 0,
): Modifier =
    then(PaddingModifier(left, right, top, bottom))

public data class MarginModifier(
    val left: Int = 0,
    val right: Int = 0,
    val top: Int = 0,
    val bottom: Int = 0,
) : Modifier.Element

public fun Modifier.margin(all: Int): Modifier = then(MarginModifier(all, all, all, all))

public fun Modifier.margin(
    horizontal: Int = 0,
    vertical: Int = 0,
): Modifier =
    then(MarginModifier(horizontal, horizontal, vertical, vertical))

public fun Modifier.margin(
    left: Int = 0,
    right: Int = 0,
    top: Int = 0,
    bottom: Int = 0,
): Modifier = then(MarginModifier(left, right, top, bottom))

public data class WidthModifier(val width: Int) : Modifier.Element

public data class HeightModifier(val height: Int) : Modifier.Element

public data class FillMaxWidthModifier(val fraction: Float = 1f) : Modifier.Element

public data class FillMaxHeightModifier(val fraction: Float = 1f) : Modifier.Element

public fun Modifier.width(width: Int): Modifier = then(WidthModifier(width))

public fun Modifier.height(height: Int): Modifier = then(HeightModifier(height))

public fun Modifier.fillMaxWidth(fraction: Float = 1f): Modifier = then(FillMaxWidthModifier(fraction))

public fun Modifier.fillMaxHeight(fraction: Float = 1f): Modifier = then(FillMaxHeightModifier(fraction))

public fun Modifier.fillMaxSize(fraction: Float = 1f): Modifier = fillMaxWidth(fraction).fillMaxHeight(fraction)

public data object WrapContentWidthModifier : Modifier.Element

public data object WrapContentHeightModifier : Modifier.Element

public fun Modifier.wrapContentWidth(): Modifier = then(WrapContentWidthModifier)

public fun Modifier.wrapContentHeight(): Modifier = then(WrapContentHeightModifier)

public fun Modifier.wrapContentSize(): Modifier = wrapContentWidth().wrapContentHeight()

public data class WeightModifier(val weight: Float) : Modifier.Element

public fun Modifier.weight(weight: Float): Modifier = then(WeightModifier(weight))

public data class ForegroundModifier(val color: Color) : Modifier.Element

public data class BackgroundModifier(val color: Color) : Modifier.Element

public data object BoldModifier : Modifier.Element

public data object ItalicModifier : Modifier.Element

public data object UnderlineModifier : Modifier.Element

public data object StrikethroughModifier : Modifier.Element

public data class BorderModifier(
    val style: BorderStyle,
    val color: Color = Color.Default,
) : Modifier.Element

public fun Modifier.foreground(color: Color): Modifier = then(ForegroundModifier(color))

public fun Modifier.background(color: Color): Modifier = then(BackgroundModifier(color))

public fun Modifier.bold(): Modifier = then(BoldModifier)

public fun Modifier.italic(): Modifier = then(ItalicModifier)

public fun Modifier.underline(): Modifier = then(UnderlineModifier)

public fun Modifier.strikethrough(): Modifier = then(StrikethroughModifier)

public fun Modifier.border(
    style: BorderStyle,
    color: Color = Color.Default,
): Modifier = then(BorderModifier(style, color))

public enum class HorizontalAlignment { Left, Center, Right }
public enum class VerticalAlignment { Top, Center, Bottom }

public data class AlignmentModifier(
    val horizontal: HorizontalAlignment? = null,
    val vertical: VerticalAlignment? = null,
) : Modifier.Element

public fun Modifier.align(horizontal: HorizontalAlignment): Modifier = then(AlignmentModifier(horizontal = horizontal))
public fun Modifier.align(vertical: VerticalAlignment): Modifier = then(AlignmentModifier(vertical = vertical))
public fun Modifier.align(horizontal: HorizontalAlignment, vertical: VerticalAlignment): Modifier =
    then(AlignmentModifier(horizontal, vertical))

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
