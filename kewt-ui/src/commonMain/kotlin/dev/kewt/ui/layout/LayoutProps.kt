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
package dev.kewt.ui.layout

import dev.kewt.modifier.AlignmentModifier
import dev.kewt.modifier.BorderModifier
import dev.kewt.modifier.FillMaxHeightModifier
import dev.kewt.modifier.FillMaxWidthModifier
import dev.kewt.modifier.HeightModifier
import dev.kewt.modifier.HorizontalAlignment
import dev.kewt.modifier.MarginModifier
import dev.kewt.modifier.Modifier
import dev.kewt.modifier.OffsetModifier
import dev.kewt.modifier.PaddingModifier
import dev.kewt.modifier.VerticalAlignment
import dev.kewt.modifier.WeightModifier
import dev.kewt.modifier.WidthModifier
import dev.kewt.modifier.findElement

/**
 * The layout-relevant properties of a modifier chain, resolved in a single pass.
 */
internal class LayoutProps(
    val paddingLeft: Int = 0,
    val paddingRight: Int = 0,
    val paddingTop: Int = 0,
    val paddingBottom: Int = 0,
    val marginLeft: Int = 0,
    val marginRight: Int = 0,
    val marginTop: Int = 0,
    val marginBottom: Int = 0,
    val explicitWidth: Int? = null,
    val explicitHeight: Int? = null,
    val fillWidth: Float? = null,
    val fillHeight: Float? = null,
    val borderThickness: Int = 0,
    val alignH: HorizontalAlignment? = null,
    val alignV: VerticalAlignment? = null,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
) {
    companion object {
        val EMPTY = LayoutProps()
    }
}

/**
 * Walks the modifier chain once and accumulates every layout property.
 *
 * Padding, margin, and offset accumulate; fixed sizes, fills, borders, and
 * alignment use the last occurrence in the chain.
 */
@Suppress("CyclomaticComplexMethod", "LongMethod")
internal fun Modifier.resolveLayoutParams(): LayoutProps {
    var paddingLeft = 0
    var paddingRight = 0
    var paddingTop = 0
    var paddingBottom = 0
    var marginLeft = 0
    var marginRight = 0
    var marginTop = 0
    var marginBottom = 0
    var explicitWidth: Int? = null
    var explicitHeight: Int? = null
    var fillWidth: Float? = null
    var fillHeight: Float? = null
    var borderThickness = 0
    var alignH: HorizontalAlignment? = null
    var alignV: VerticalAlignment? = null
    var offsetX = 0
    var offsetY = 0

    foldIn(Unit) { _, element ->
        when (element) {
            is PaddingModifier -> {
                paddingLeft += element.left
                paddingRight += element.right
                paddingTop += element.top
                paddingBottom += element.bottom
            }

            is MarginModifier -> {
                marginLeft += element.left
                marginRight += element.right
                marginTop += element.top
                marginBottom += element.bottom
            }

            is WidthModifier -> explicitWidth = element.width
            is HeightModifier -> explicitHeight = element.height
            is FillMaxWidthModifier -> fillWidth = element.fraction
            is FillMaxHeightModifier -> fillHeight = element.fraction
            is BorderModifier -> borderThickness = 1

            is AlignmentModifier -> {
                val h = element.horizontal
                if (h != null) alignH = h
                val v = element.vertical
                if (v != null) alignV = v
            }

            is OffsetModifier -> {
                offsetX += element.x
                offsetY += element.y
            }

            else -> {}
        }
    }

    return LayoutProps(
        paddingLeft = paddingLeft,
        paddingRight = paddingRight,
        paddingTop = paddingTop,
        paddingBottom = paddingBottom,
        marginLeft = marginLeft,
        marginRight = marginRight,
        marginTop = marginTop,
        marginBottom = marginBottom,
        explicitWidth = explicitWidth,
        explicitHeight = explicitHeight,
        fillWidth = fillWidth,
        fillHeight = fillHeight,
        borderThickness = borderThickness,
        alignH = alignH,
        alignV = alignV,
        offsetX = offsetX,
        offsetY = offsetY,
    )
}

/**
 * Returns the layout weight of a modifier chain, or 0 when no weight is set.
 */
internal fun Modifier.weightValue(): Float = findElement<WeightModifier>()?.weight ?: 0f
