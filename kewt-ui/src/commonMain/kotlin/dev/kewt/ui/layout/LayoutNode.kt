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

import dev.kewt.core.buffer.Rect
import dev.kewt.modifier.Arrangement
import dev.kewt.modifier.HorizontalAlignment
import dev.kewt.modifier.Modifier
import dev.kewt.modifier.VerticalAlignment

/**
 * The kind of layout a [LayoutNode] performs on its children.
 */
public enum class LayoutType { Box, Row, Column, Leaf }

/**
 * A node in the layout tree.
 *
 * The box model is, from outside in: margin -> border -> padding -> content.
 * [x], [y], [width], and [height] describe the outer box (margins included);
 * [borderBox] and [contentBox] expose the inner rectangles for painting.
 *
 * @property layoutType The kind of layout performed on children.
 * @property children Child nodes, measured and placed by this node.
 * @property modifier The modifier chain decorating this node.
 * @property intrinsicWidth Natural width of a leaf node when no [measureFn] is given.
 * @property intrinsicHeight Natural height of a leaf node when no [measureFn] is given.
 * @property arrangement Distribution of children along the main axis of Row/Column.
 * @property horizontalAlignment Default cross/content horizontal alignment.
 * @property verticalAlignment Default cross/content vertical alignment.
 * @property measureFn Optional custom content measurement for leaf nodes.
 */
@Suppress("TooManyFunctions")
public class LayoutNode(
    public val layoutType: LayoutType,
    public val children: List<LayoutNode> = emptyList(),
    public val modifier: Modifier = Modifier,
    public val intrinsicWidth: Int = 0,
    public val intrinsicHeight: Int = 0,
    public val arrangement: Arrangement = Arrangement.Start,
    public val horizontalAlignment: HorizontalAlignment = HorizontalAlignment.Left,
    public val verticalAlignment: VerticalAlignment = VerticalAlignment.Top,
    public val measureFn: ((Constraints) -> MeasureResult)? = null,
) {
    public var x: Int = 0
    public var y: Int = 0
    public var width: Int = 0
    public var height: Int = 0

    private var props: LayoutProps = LayoutProps.EMPTY

    /**
     * The rectangle covered by the border decoration (the outer box minus margins).
     *
     * [x] and [y] already point at the top-left corner of this rectangle; the
     * [width] and [height] properties span the outer box, margins included.
     * Valid after [measure] has been called.
     */
    public fun borderBox(): Rect =
        Rect(
            x = x,
            y = y,
            width = (width - props.marginLeft - props.marginRight).coerceAtLeast(0),
            height = (height - props.marginTop - props.marginBottom).coerceAtLeast(0),
        )

    /**
     * The rectangle available for children (the border box minus border and padding).
     *
     * Valid after [measure] has been called.
     */
    public fun contentBox(): Rect {
        val box = borderBox()
        val inset = props.borderThickness
        return Rect(
            x = box.x + inset + props.paddingLeft,
            y = box.y + inset + props.paddingTop,
            width = (box.width - inset * 2 - props.paddingLeft - props.paddingRight).coerceAtLeast(0),
            height = (box.height - inset * 2 - props.paddingTop - props.paddingBottom).coerceAtLeast(0),
        )
    }

    /**
     * Measures this node and its children against [constraints].
     *
     * @return The outer size of the node, margins included.
     */
    @Suppress("LongMethod")
    public fun measure(constraints: Constraints): MeasureResult {
        val p = modifier.resolveLayoutParams()
        props = p

        val outerMaxW = (constraints.maxWidth - p.marginLeft - p.marginRight).coerceAtLeast(0)
        val outerMaxH = (constraints.maxHeight - p.marginTop - p.marginBottom).coerceAtLeast(0)
        val outerMinW = (constraints.minWidth - p.marginLeft - p.marginRight).coerceIn(0, outerMaxW)
        val outerMinH = (constraints.minHeight - p.marginTop - p.marginBottom).coerceIn(0, outerMaxH)

        val decoW = p.borderThickness * 2 + p.paddingLeft + p.paddingRight
        val decoH = p.borderThickness * 2 + p.paddingTop + p.paddingBottom
        val innerMaxW = (outerMaxW - decoW).coerceAtLeast(0)
        val innerMaxH = (outerMaxH - decoH).coerceAtLeast(0)
        val innerMinW = (outerMinW - decoW).coerceIn(0, innerMaxW)
        val innerMinH = (outerMinH - decoH).coerceIn(0, innerMaxH)

        val content = when (layoutType) {
            LayoutType.Leaf -> measureLeaf(innerMinW, innerMaxW, innerMinH, innerMaxH)
            LayoutType.Box -> measureBox(innerMaxW, innerMaxH, innerMinW, innerMinH)
            LayoutType.Row -> measureRow(innerMaxW, innerMaxH, innerMinW, innerMinH)
            LayoutType.Column -> measureColumn(innerMaxW, innerMaxH, innerMinW, innerMinH)
        }

        var boxW = content.width + decoW
        var boxH = content.height + decoH
        val explicitW = p.explicitWidth
        val explicitH = p.explicitHeight
        if (explicitW != null) boxW = explicitW
        if (explicitH != null) boxH = explicitH
        val fillW = p.fillWidth
        val fillH = p.fillHeight
        if (explicitW == null && fillW != null) boxW = (outerMaxW * fillW).toInt()
        if (explicitH == null && fillH != null) boxH = (outerMaxH * fillH).toInt()
        boxW = boxW.coerceIn(outerMinW, outerMaxW)
        boxH = boxH.coerceIn(outerMinH, outerMaxH)

        width = boxW + p.marginLeft + p.marginRight
        height = boxH + p.marginTop + p.marginBottom
        return MeasureResult(width, height)
    }

    /**
     * Positions this node inside an outer box whose top-left corner is at ([px], [py]).
     *
     * Margins push the node's border box inward; offsets shift it without affecting
     * siblings.
     */
    public fun place(
        px: Int,
        py: Int,
    ) {
        x = px + props.marginLeft + props.offsetX
        y = py + props.marginTop + props.offsetY
        when (layoutType) {
            LayoutType.Box -> placeBox()
            LayoutType.Row -> placeRow()
            LayoutType.Column -> placeColumn()
            LayoutType.Leaf -> {}
        }
    }

    private fun measureLeaf(
        innerMinW: Int,
        innerMaxW: Int,
        innerMinH: Int,
        innerMaxH: Int,
    ): MeasureResult {
        val fn = measureFn
        return if (fn != null) {
            val r = fn(
                Constraints(
                    minWidth = innerMinW,
                    maxWidth = innerMaxW,
                    minHeight = innerMinH,
                    maxHeight = innerMaxH,
                ),
            )
            MeasureResult(
                r.width.coerceIn(innerMinW, innerMaxW),
                r.height.coerceIn(innerMinH, innerMaxH),
            )
        } else {
            MeasureResult(
                intrinsicWidth.coerceIn(innerMinW, innerMaxW),
                intrinsicHeight.coerceIn(innerMinH, innerMaxH),
            )
        }
    }

    private fun measureBox(
        innerMaxW: Int,
        innerMaxH: Int,
        innerMinW: Int,
        innerMinH: Int,
    ): MeasureResult {
        var mw = 0
        var mh = 0
        val childConstraints = Constraints(maxWidth = innerMaxW, maxHeight = innerMaxH)
        children.forEach { child ->
            val r = child.measure(childConstraints)
            mw = maxOf(mw, r.width)
            mh = maxOf(mh, r.height)
        }
        return MeasureResult(
            width = mw.coerceIn(innerMinW, innerMaxW),
            height = mh.coerceIn(innerMinH, innerMaxH),
        )
    }

    @Suppress("LongMethod")
    private fun measureRow(
        innerMaxW: Int,
        innerMaxH: Int,
        innerMinW: Int,
        innerMinH: Int,
    ): MeasureResult {
        if (children.isEmpty()) {
            return MeasureResult(0.coerceIn(innerMinW, innerMaxW), 0.coerceIn(innerMinH, innerMaxH))
        }

        val weights = FloatArray(children.size) { children[it].modifier.weightValue() }
        var totalWeight = 0f
        weights.forEach { w -> if (w > 0f) totalWeight += w }

        var fixedWidth = 0
        var maxHeight = 0

        if (totalWeight <= 0f) {
            val childConstraints = Constraints(maxWidth = innerMaxW, maxHeight = innerMaxH)
            children.forEach { child ->
                val r = child.measure(childConstraints)
                fixedWidth += r.width
                maxHeight = maxOf(maxHeight, r.height)
            }
            return MeasureResult(
                width = fixedWidth.coerceIn(innerMinW, innerMaxW),
                height = maxHeight.coerceIn(innerMinH, innerMaxH),
            )
        }

        // Measure unweighted children first, then distribute the remaining space
        children.forEachIndexed { index, child ->
            if (weights[index] <= 0f) {
                val remaining = (innerMaxW - fixedWidth).coerceAtLeast(0)
                val r = child.measure(Constraints(maxWidth = remaining, maxHeight = innerMaxH))
                fixedWidth += r.width
                maxHeight = maxOf(maxHeight, r.height)
            }
        }
        val remaining = (innerMaxW - fixedWidth).coerceAtLeast(0)
        var allocated = 0
        children.forEachIndexed { index, child ->
            if (weights[index] > 0f) {
                val share = (remaining * weights[index] / totalWeight).toInt()
                val r = child.measure(Constraints(minWidth = share, maxWidth = share, maxHeight = innerMaxH))
                allocated += r.width
                maxHeight = maxOf(maxHeight, r.height)
            }
        }
        return MeasureResult(
            width = (fixedWidth + allocated).coerceIn(innerMinW, innerMaxW),
            height = maxHeight.coerceIn(innerMinH, innerMaxH),
        )
    }

    @Suppress("LongMethod")
    private fun measureColumn(
        innerMaxW: Int,
        innerMaxH: Int,
        innerMinW: Int,
        innerMinH: Int,
    ): MeasureResult {
        if (children.isEmpty()) {
            return MeasureResult(0.coerceIn(innerMinW, innerMaxW), 0.coerceIn(innerMinH, innerMaxH))
        }

        val weights = FloatArray(children.size) { children[it].modifier.weightValue() }
        var totalWeight = 0f
        weights.forEach { w -> if (w > 0f) totalWeight += w }

        var fixedHeight = 0
        var maxWidth = 0

        if (totalWeight <= 0f) {
            val childConstraints = Constraints(maxWidth = innerMaxW, maxHeight = innerMaxH)
            children.forEach { child ->
                val r = child.measure(childConstraints)
                fixedHeight += r.height
                maxWidth = maxOf(maxWidth, r.width)
            }
            return MeasureResult(
                width = maxWidth.coerceIn(innerMinW, innerMaxW),
                height = fixedHeight.coerceIn(innerMinH, innerMaxH),
            )
        }

        children.forEachIndexed { index, child ->
            if (weights[index] <= 0f) {
                val remaining = (innerMaxH - fixedHeight).coerceAtLeast(0)
                val r = child.measure(Constraints(maxWidth = innerMaxW, maxHeight = remaining))
                fixedHeight += r.height
                maxWidth = maxOf(maxWidth, r.width)
            }
        }
        val remaining = (innerMaxH - fixedHeight).coerceAtLeast(0)
        var allocated = 0
        children.forEachIndexed { index, child ->
            if (weights[index] > 0f) {
                val share = (remaining * weights[index] / totalWeight).toInt()
                val r = child.measure(Constraints(maxWidth = innerMaxW, minHeight = share, maxHeight = share))
                allocated += r.height
                maxWidth = maxOf(maxWidth, r.width)
            }
        }
        return MeasureResult(
            width = maxWidth.coerceIn(innerMinW, innerMaxW),
            height = (fixedHeight + allocated).coerceIn(innerMinH, innerMaxH),
        )
    }

    private fun placeBox() {
        val content = contentBox()
        children.forEach { child ->
            val alignH = child.props.alignH ?: horizontalAlignment
            val alignV = child.props.alignV ?: verticalAlignment
            val freeW = (content.width - child.width).coerceAtLeast(0)
            val freeH = (content.height - child.height).coerceAtLeast(0)
            val ox = when (alignH) {
                HorizontalAlignment.Left -> 0
                HorizontalAlignment.Center -> freeW / 2
                HorizontalAlignment.Right -> freeW
            }
            val oy = when (alignV) {
                VerticalAlignment.Top -> 0
                VerticalAlignment.Center -> freeH / 2
                VerticalAlignment.Bottom -> freeH
            }
            child.place(content.x + ox, content.y + oy)
        }
    }

    private fun placeRow() {
        if (children.isEmpty()) return
        val content = contentBox()
        var totalWidth = 0
        children.forEach { totalWidth += it.width }
        val free = (content.width - totalWidth).coerceAtLeast(0)

        var gap = 0
        var cx = content.x
        when (arrangement) {
            Arrangement.Start -> {}
            Arrangement.Center -> cx += free / 2
            Arrangement.End -> cx += free
            Arrangement.SpaceBetween -> if (children.size > 1) gap = free / (children.size - 1)
            Arrangement.SpaceAround -> {
                gap = free / children.size
                cx += gap / 2
            }

            Arrangement.SpaceEvenly -> {
                gap = free / (children.size + 1)
                cx += gap
            }
        }

        children.forEach { child ->
            val alignV = child.props.alignV ?: verticalAlignment
            val freeH = (content.height - child.height).coerceAtLeast(0)
            val oy = when (alignV) {
                VerticalAlignment.Top -> 0
                VerticalAlignment.Center -> freeH / 2
                VerticalAlignment.Bottom -> freeH
            }
            child.place(cx, content.y + oy)
            cx += child.width + gap
        }
    }

    private fun placeColumn() {
        if (children.isEmpty()) return
        val content = contentBox()
        var totalHeight = 0
        children.forEach { totalHeight += it.height }
        val free = (content.height - totalHeight).coerceAtLeast(0)

        var gap = 0
        var cy = content.y
        when (arrangement) {
            Arrangement.Start -> {}
            Arrangement.Center -> cy += free / 2
            Arrangement.End -> cy += free
            Arrangement.SpaceBetween -> if (children.size > 1) gap = free / (children.size - 1)
            Arrangement.SpaceAround -> {
                gap = free / children.size
                cy += gap / 2
            }

            Arrangement.SpaceEvenly -> {
                gap = free / (children.size + 1)
                cy += gap
            }
        }

        children.forEach { child ->
            val alignH = child.props.alignH ?: horizontalAlignment
            val freeW = (content.width - child.width).coerceAtLeast(0)
            val ox = when (alignH) {
                HorizontalAlignment.Left -> 0
                HorizontalAlignment.Center -> freeW / 2
                HorizontalAlignment.Right -> freeW
            }
            child.place(content.x + ox, cy)
            cy += child.height + gap
        }
    }
}
