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

import dev.kewt.modifier.FillMaxHeightModifier
import dev.kewt.modifier.FillMaxWidthModifier
import dev.kewt.modifier.HeightModifier
import dev.kewt.modifier.Modifier
import dev.kewt.modifier.WidthModifier
import dev.kewt.modifier.WrapContentHeightModifier
import dev.kewt.modifier.WrapContentWidthModifier
import dev.kewt.modifier.findElement
import dev.kewt.modifier.totalMargin
import dev.kewt.modifier.totalPadding

public enum class LayoutType { Box, Row, Column, Leaf }

public class LayoutNode(
    public val layoutType: LayoutType,
    public val children: List<LayoutNode> = emptyList(),
    public val modifier: Modifier = Modifier,
    public val intrinsicWidth: Int = 0,
    public val intrinsicHeight: Int = 0,
) {
    public var x: Int = 0
    public var y: Int = 0
    public var width: Int = 0
    public var height: Int = 0

    public fun measure(constraints: Constraints): MeasureResult {
        val pad = modifier.totalPadding()
        val pl = pad.left
        val pr = pad.right
        val pt = pad.top
        val pb = pad.bottom

        val margin = modifier.totalMargin()
        val ml = margin.left
        val mr = margin.right
        val mt = margin.top
        val mb = margin.bottom

        // Adjust constraints for margin
        val outerConstraints = Constraints(
            minWidth = constraints.minWidth,
            maxWidth = (constraints.maxWidth - ml - mr).coerceAtLeast(0),
            minHeight = constraints.minHeight,
            maxHeight = (constraints.maxHeight - mt - mb).coerceAtLeast(0),
        )

        val explicitWidth = modifier.findElement<WidthModifier>()?.width
        val explicitHeight = modifier.findElement<HeightModifier>()?.height

        val result = when (layoutType) {
            LayoutType.Leaf -> {
                MeasureResult(
                    intrinsicWidth.coerceAtMost(outerConstraints.maxWidth),
                    intrinsicHeight.coerceAtMost(outerConstraints.maxHeight),
                )
            }

            LayoutType.Box -> {
                measureBox(outerConstraints, pl, pr, pt, pb)
            }

            LayoutType.Row -> {
                measureRow(outerConstraints, pl, pr, pt, pb)
            }

            LayoutType.Column -> {
                measureColumn(outerConstraints, pl, pr, pt, pb)
            }
        }

        val fillW = modifier.findElement<FillMaxWidthModifier>()
        val fillH = modifier.findElement<FillMaxHeightModifier>()
        val wrapW = modifier.findElement<WrapContentWidthModifier>() != null
        val wrapH = modifier.findElement<WrapContentHeightModifier>() != null

        width = when {
            explicitWidth != null -> explicitWidth
            wrapW -> result.width.coerceAtMost(outerConstraints.maxWidth)
            fillW != null -> (outerConstraints.maxWidth * fillW.fraction).toInt()
            else -> result.width.coerceAtMost(outerConstraints.maxWidth)
        }
        height = when {
            explicitHeight != null -> explicitHeight
            wrapH -> result.height.coerceAtMost(outerConstraints.maxHeight)
            fillH != null -> (outerConstraints.maxHeight * fillH.fraction).toInt()
            else -> result.height.coerceAtMost(outerConstraints.maxHeight)
        }

        // Add margins back for final reported size if needed,
        // but typically parents only care about the node's bounds.
        return MeasureResult(width + ml + mr, height + mt + mb)
    }

    public fun place(
        px: Int,
        py: Int,
    ) {
        val margin = modifier.totalMargin()
        x = px + margin.left
        y = py + margin.top

        val pad = modifier.totalPadding()
        val pl = pad.left
        val pt = pad.top

        when (layoutType) {
            LayoutType.Box -> {
                children.forEach { it.place(x + pl, y + pt) }
            }

            LayoutType.Row -> {
                var cx = x + pl
                children.forEach {
                    it.place(cx, y + pt)
                    cx += it.width // Use total width (including margin if returned by measure)
                }
            }

            LayoutType.Column -> {
                var cy = y + pt
                children.forEach {
                    it.place(x + pl, cy)
                    cy += it.height
                }
            }

            LayoutType.Leaf -> {}
        }
    }

    private fun measureBox(
        c: Constraints,
        pl: Int,
        pr: Int,
        pt: Int,
        pb: Int,
    ): MeasureResult {
        var mw = 0
        var mh = 0
        val inner = Constraints(
            maxWidth = (c.maxWidth - pl - pr).coerceAtLeast(0),
            maxHeight = (c.maxHeight - pt - pb).coerceAtLeast(0),
        )
        children.forEach { child ->
            val r = child.measure(inner)
            mw = maxOf(mw, r.width)
            mh = maxOf(mh, r.height)
        }
        return MeasureResult(
            width = (mw + pl + pr).coerceIn(c.minWidth, c.maxWidth),
            height = (mh + pt + pb).coerceIn(c.minHeight, c.maxHeight),
        )
    }

    private fun measureRow(
        c: Constraints,
        pl: Int,
        pr: Int,
        pt: Int,
        pb: Int,
    ): MeasureResult {
        var tw = 0
        var mh = 0
        val innerW = (c.maxWidth - pl - pr).coerceAtLeast(0)
        val innerH = (c.maxHeight - pt - pb).coerceAtLeast(0)
        children.forEach { child ->
            val r = child.measure(Constraints(maxWidth = innerW, maxHeight = innerH))
            tw += r.width
            mh = maxOf(mh, r.height)
        }
        return MeasureResult(
            width = (tw + pl + pr).coerceIn(c.minWidth, c.maxWidth),
            height = (mh + pt + pb).coerceIn(c.minHeight, c.maxHeight),
        )
    }

    private fun measureColumn(
        c: Constraints,
        pl: Int,
        pr: Int,
        pt: Int,
        pb: Int,
    ): MeasureResult {
        var mw = 0
        var th = 0
        val innerW = (c.maxWidth - pl - pr).coerceAtLeast(0)
        val innerH = (c.maxHeight - pt - pb).coerceAtLeast(0)
        children.forEach { child ->
            val r = child.measure(Constraints(maxWidth = innerW, maxHeight = innerH))
            mw = maxOf(mw, r.width)
            th += r.height
        }
        return MeasureResult(
            width = (mw + pl + pr).coerceIn(c.minWidth, c.maxWidth),
            height = (th + pt + pb).coerceIn(c.minHeight, c.maxHeight),
        )
    }
}
