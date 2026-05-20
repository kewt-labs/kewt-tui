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

import dev.kewt.modifier.Modifier
import dev.kewt.modifier.padding
import kotlin.test.Test
import kotlin.test.assertEquals

class LayoutTest {
    @Test
    fun columnPlacesChildrenVertically() {
        val children =
            listOf(
                LayoutNode(LayoutType.Leaf, intrinsicWidth = 5, intrinsicHeight = 1),
                LayoutNode(LayoutType.Leaf, intrinsicWidth = 5, intrinsicHeight = 1),
                LayoutNode(LayoutType.Leaf, intrinsicWidth = 5, intrinsicHeight = 1),
            )
        val column = LayoutNode(LayoutType.Column, children = children)
        column.measure(Constraints(maxWidth = 80, maxHeight = 24))
        column.place(0, 0)

        assertEquals(0, children[0].y)
        assertEquals(1, children[1].y)
        assertEquals(2, children[2].y)
    }

    @Test
    fun rowPlacesChildrenHorizontally() {
        val children =
            listOf(
                LayoutNode(LayoutType.Leaf, intrinsicWidth = 3, intrinsicHeight = 1),
                LayoutNode(LayoutType.Leaf, intrinsicWidth = 4, intrinsicHeight = 1),
            )
        val row = LayoutNode(LayoutType.Row, children = children)
        row.measure(Constraints(maxWidth = 80, maxHeight = 24))
        row.place(0, 0)

        assertEquals(0, children[0].x)
        assertEquals(3, children[1].x)
    }

    @Test
    fun paddingAffectsChildPlacement() {
        val child = LayoutNode(LayoutType.Leaf, intrinsicWidth = 5, intrinsicHeight = 1)
        val box =
            LayoutNode(
                LayoutType.Box,
                children = listOf(child),
                modifier = Modifier.padding(left = 2, top = 1),
            )
        box.measure(Constraints(maxWidth = 80, maxHeight = 24))
        box.place(0, 0)

        assertEquals(2, child.x)
        assertEquals(1, child.y)
    }

    @Test
    fun columnMeasuresCorrectSize() {
        val children =
            listOf(
                LayoutNode(LayoutType.Leaf, intrinsicWidth = 10, intrinsicHeight = 2),
                LayoutNode(LayoutType.Leaf, intrinsicWidth = 8, intrinsicHeight = 3),
            )
        val column = LayoutNode(LayoutType.Column, children = children)
        val result = column.measure(Constraints(maxWidth = 80, maxHeight = 24))

        assertEquals(10, result.width) // max child width
        assertEquals(5, result.height) // sum of heights
    }

    @Test
    fun multiplePaddingModifiersAccumulate() {
        val child = LayoutNode(LayoutType.Leaf, intrinsicWidth = 1, intrinsicHeight = 1)
        val box = LayoutNode(
            LayoutType.Box,
            children = listOf(child),
            modifier = Modifier.padding(1).padding(2),
        )
        box.measure(Constraints(maxWidth = 80, maxHeight = 24))
        box.place(0, 0)

        // Total padding should be 1 + 2 = 3
        assertEquals(3, child.x)
        assertEquals(3, child.y)
    }
}
