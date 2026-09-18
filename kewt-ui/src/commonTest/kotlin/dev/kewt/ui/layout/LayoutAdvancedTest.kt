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
import dev.kewt.modifier.BorderStyle
import dev.kewt.modifier.HorizontalAlignment
import dev.kewt.modifier.Modifier
import dev.kewt.modifier.VerticalAlignment
import dev.kewt.modifier.align
import dev.kewt.modifier.border
import dev.kewt.modifier.fillMaxHeight
import dev.kewt.modifier.fillMaxSize
import dev.kewt.modifier.fillMaxWidth
import dev.kewt.modifier.height
import dev.kewt.modifier.margin
import dev.kewt.modifier.offset
import dev.kewt.modifier.padding
import dev.kewt.modifier.weight
import dev.kewt.modifier.width
import kotlin.test.Test
import kotlin.test.assertEquals

class LayoutAdvancedTest {
    private fun leaf(
        w: Int,
        h: Int,
        modifier: Modifier = Modifier,
    ) = LayoutNode(LayoutType.Leaf, modifier = modifier, intrinsicWidth = w, intrinsicHeight = h)

    @Test
    fun rowDistributesWeightEvenly() {
        val a = leaf(0, 1, Modifier.weight(1f))
        val b = leaf(0, 1, Modifier.weight(1f))
        val row = LayoutNode(LayoutType.Row, children = listOf(a, b), modifier = Modifier.fillMaxWidth())
        row.measure(Constraints(maxWidth = 10, maxHeight = 5))
        row.place(0, 0)
        assertEquals(5, a.width)
        assertEquals(5, b.width)
        assertEquals(0, a.x)
        assertEquals(5, b.x)
    }

    @Test
    fun rowWeightRespectsFixedChildren() {
        val fixed = leaf(2, 1)
        val weighted = leaf(0, 1, Modifier.weight(1f))
        val row = LayoutNode(LayoutType.Row, children = listOf(fixed, weighted), modifier = Modifier.fillMaxWidth())
        row.measure(Constraints(maxWidth = 10, maxHeight = 5))
        row.place(0, 0)
        assertEquals(2, fixed.width)
        assertEquals(8, weighted.width)
        assertEquals(2, weighted.x)
    }

    @Test
    fun rowWeightProportionalSplit() {
        val a = leaf(0, 1, Modifier.weight(1f))
        val b = leaf(0, 1, Modifier.weight(3f))
        val row = LayoutNode(LayoutType.Row, children = listOf(a, b), modifier = Modifier.fillMaxWidth())
        row.measure(Constraints(maxWidth = 20, maxHeight = 5))
        assertEquals(5, a.width)
        assertEquals(15, b.width)
    }

    @Test
    fun columnDistributesWeightVertically() {
        val a = leaf(1, 0, Modifier.weight(1f))
        val b = leaf(1, 0, Modifier.weight(2f))
        val column = LayoutNode(LayoutType.Column, children = listOf(a, b), modifier = Modifier.fillMaxHeight())
        column.measure(Constraints(maxWidth = 5, maxHeight = 9))
        column.place(0, 0)
        assertEquals(3, a.height)
        assertEquals(6, b.height)
        assertEquals(0, a.y)
        assertEquals(3, b.y)
    }

    @Test
    fun boxCenterAlignsContent() {
        val child = leaf(2, 1)
        val box = LayoutNode(
            LayoutType.Box,
            children = listOf(child),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = HorizontalAlignment.Center,
            verticalAlignment = VerticalAlignment.Center,
        )
        box.measure(Constraints(maxWidth = 10, maxHeight = 5))
        box.place(0, 0)
        assertEquals(4, child.x)
        assertEquals(2, child.y)
    }

    @Test
    fun childAlignmentOverridesContainer() {
        val child = leaf(2, 1, Modifier.align(HorizontalAlignment.Right))
        val box = LayoutNode(
            LayoutType.Box,
            children = listOf(child),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = HorizontalAlignment.Center,
        )
        box.measure(Constraints(maxWidth = 10, maxHeight = 5))
        box.place(0, 0)
        assertEquals(8, child.x)
    }

    @Test
    fun rowCrossAxisAlignment() {
        val tall = leaf(1, 3)
        val small = leaf(1, 1, Modifier.align(VerticalAlignment.Bottom))
        val row = LayoutNode(LayoutType.Row, children = listOf(tall, small))
        row.measure(Constraints(maxWidth = 10, maxHeight = 10))
        row.place(0, 0)
        assertEquals(0, tall.y)
        assertEquals(2, small.y)
    }

    @Test
    fun columnArrangementSpaceBetween() {
        val a = leaf(1, 1)
        val b = leaf(1, 1)
        val column = LayoutNode(
            LayoutType.Column,
            children = listOf(a, b),
            modifier = Modifier.fillMaxHeight(),
            arrangement = Arrangement.SpaceBetween,
        )
        column.measure(Constraints(maxWidth = 5, maxHeight = 9))
        column.place(0, 0)
        assertEquals(0, a.y)
        assertEquals(8, b.y)
    }

    @Test
    fun rowArrangementCenter() {
        val a = leaf(1, 1)
        val b = leaf(1, 1)
        val row = LayoutNode(
            LayoutType.Row,
            children = listOf(a, b),
            modifier = Modifier.fillMaxWidth(),
            arrangement = Arrangement.Center,
        )
        row.measure(Constraints(maxWidth = 10, maxHeight = 5))
        row.place(0, 0)
        assertEquals(4, a.x)
        assertEquals(5, b.x)
    }

    @Test
    fun rowArrangementEnd() {
        val a = leaf(1, 1)
        val row = LayoutNode(
            LayoutType.Row,
            children = listOf(a),
            modifier = Modifier.fillMaxWidth(),
            arrangement = Arrangement.End,
        )
        row.measure(Constraints(maxWidth = 10, maxHeight = 5))
        row.place(0, 0)
        assertEquals(9, a.x)
    }

    @Test
    fun borderInsetsChildrenOnAnyContainer() {
        val child = leaf(2, 1)
        val column = LayoutNode(
            LayoutType.Column,
            children = listOf(child),
            modifier = Modifier.border(BorderStyle.Light).width(6).height(4),
        )
        column.measure(Constraints(maxWidth = 10, maxHeight = 10))
        column.place(0, 0)
        // Border occupies one cell on each side
        assertEquals(1, child.x)
        assertEquals(1, child.y)
        assertEquals(Rect(0, 0, 6, 4), column.borderBox())
        assertEquals(Rect(1, 1, 4, 2), column.contentBox())
    }

    @Test
    fun marginIsExcludedFromBorderBox() {
        val node = leaf(2, 1, Modifier.margin(2))
        val box = LayoutNode(LayoutType.Box, children = listOf(node))
        box.measure(Constraints(maxWidth = 20, maxHeight = 20))
        box.place(0, 0)
        assertEquals(2, node.x)
        assertEquals(2, node.y)
        assertEquals(6, node.width) // 2 content + 4 margin
        assertEquals(Rect(2, 2, 2, 1), node.borderBox())
    }

    @Test
    fun offsetShiftsNodeWithoutAffectingSiblings() {
        val a = leaf(1, 1, Modifier.offset(x = 2, y = 1))
        val b = leaf(1, 1)
        val row = LayoutNode(LayoutType.Row, children = listOf(a, b))
        row.measure(Constraints(maxWidth = 10, maxHeight = 5))
        row.place(0, 0)
        assertEquals(2, a.x)
        assertEquals(1, a.y)
        assertEquals(1, b.x)
        assertEquals(0, b.y)
    }

    @Test
    fun paddingOnLeafReservesSpace() {
        val node = leaf(2, 1, Modifier.padding(1))
        node.measure(Constraints(maxWidth = 10, maxHeight = 10))
        node.place(0, 0)
        assertEquals(4, node.width)
        assertEquals(3, node.height)
        assertEquals(Rect(1, 1, 2, 1), node.contentBox())
    }

    @Test
    fun explicitWidthIncludesPadding() {
        val node = leaf(2, 1, Modifier.width(8).padding(1))
        node.measure(Constraints(maxWidth = 20, maxHeight = 20))
        assertEquals(8, node.width)
        assertEquals(6, node.contentBox().width)
    }

    @Test
    fun fillFraction() {
        val node = leaf(1, 1, Modifier.fillMaxWidth(0.5f))
        node.measure(Constraints(maxWidth = 10, maxHeight = 10))
        assertEquals(5, node.width)
    }

    @Test
    fun emptyRowMeasuresZero() {
        val row = LayoutNode(LayoutType.Row)
        val result = row.measure(Constraints(maxWidth = 10, maxHeight = 10))
        assertEquals(0, result.width)
        assertEquals(0, result.height)
    }

    @Test
    fun minConstraintsAreHonored() {
        val node = LayoutNode(LayoutType.Leaf, intrinsicWidth = 1, intrinsicHeight = 1)
        val result = node.measure(Constraints(minWidth = 5, maxWidth = 10, minHeight = 3, maxHeight = 10))
        assertEquals(5, result.width)
        assertEquals(3, result.height)
    }
}
