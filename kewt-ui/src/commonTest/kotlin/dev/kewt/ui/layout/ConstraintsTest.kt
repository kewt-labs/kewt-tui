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

import kotlin.test.Test
import kotlin.test.assertEquals

class ConstraintsTest {
    @Test
    fun constrainClampsValues() {
        val c = Constraints(minWidth = 5, maxWidth = 20, minHeight = 3, maxHeight = 10)
        assertEquals(5 to 3, c.constrain(2, 1))
        assertEquals(10 to 7, c.constrain(10, 7))
        assertEquals(20 to 10, c.constrain(50, 50))
    }

    @Test
    fun fixedConstraints() {
        val c = Constraints.fixed(10, 5)
        assertEquals(10, c.minWidth)
        assertEquals(10, c.maxWidth)
        assertEquals(5, c.minHeight)
        assertEquals(5, c.maxHeight)
    }
}
