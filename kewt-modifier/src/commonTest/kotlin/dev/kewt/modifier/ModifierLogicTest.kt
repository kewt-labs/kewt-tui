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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ModifierLogicTest {
    @Test
    fun accumulatePadding() {
        val modifier = Modifier
            .padding(all = 1)
            .padding(left = 2, top = 3)
            .padding(horizontal = 5)

        val total = modifier.totalPadding()

        // 1 (all) + 2 (left) + 5 (horizontal) = 8
        assertEquals(8, total.left)
        // 1 (all) + 5 (horizontal) = 6
        assertEquals(6, total.right)
        // 1 (all) + 3 (top) = 4
        assertEquals(4, total.top)
        // 1 (all) = 1
        assertEquals(1, total.bottom)
    }

    @Test
    fun accumulateMargin() {
        val modifier = Modifier
            .margin(all = 10)
            .margin(all = 5)

        val total = modifier.totalMargin()
        assertEquals(15, total.left)
        assertEquals(15, total.right)
        assertEquals(15, total.top)
        assertEquals(15, total.bottom)
    }

    @Test
    fun accumulateOffset() {
        val modifier = Modifier
            .offset(x = 1, y = 2)
            .offset(x = 10, y = 20)

        val total = modifier.totalOffset()
        assertEquals(11, total.x)
        assertEquals(22, total.y)
    }

    @Test
    fun flattenChain() {
        val modifier = Modifier
            .padding(1)
            .bold()
            .background(Color.Red)

        val flat = modifier.flatten()
        assertEquals(3, flat.size)
        assertIs<PaddingModifier>(flat[0])
        assertIs<BoldModifier>(flat[1])
        assertIs<BackgroundModifier>(flat[2])
    }

    @Test
    fun findElementReturnsLast() {
        val modifier = Modifier
            .foreground(Color.Red)
            .padding(1)
            .foreground(Color.Blue)

        val fg = modifier.findElement<ForegroundModifier>()
        assertEquals(Color.Blue, fg?.color)
    }
}
