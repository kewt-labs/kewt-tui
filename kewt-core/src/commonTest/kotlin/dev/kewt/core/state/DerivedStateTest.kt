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
package dev.kewt.core.state

import kotlin.test.Test
import kotlin.test.assertEquals

class DerivedStateTest {
    @Test
    fun computesFromSource() {
        val source = mutableStateOf(3)
        val derived = derivedStateOf { source.value * 2 }
        assertEquals(6, derived.value)
    }

    @Test
    fun recomputesWhenSourceChanges() {
        val source = mutableStateOf(1)
        val derived = derivedStateOf { source.value + 10 }
        assertEquals(11, derived.value)
        source.value = 5
        assertEquals(15, derived.value)
    }

    @Test
    fun cachesWhenSourceUnchanged() {
        var computeCount = 0
        val source = mutableStateOf(1)
        val derived =
            derivedStateOf {
                computeCount++
                source.value
            }

        derived.value
        derived.value
        assertEquals(1, computeCount)
    }
}
