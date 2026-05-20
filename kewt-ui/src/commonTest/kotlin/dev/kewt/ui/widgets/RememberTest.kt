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
package dev.kewt.ui.widgets

import kotlin.test.Test
import kotlin.test.assertEquals

class RememberTest {
    @Test
    fun rememberCachesValue() {
        var computeCount = 0
        val scope = ViewScope()

        // First call computes
        val v1 = scope.remember("key") {
            computeCount++
            "hello"
        }
        assertEquals("hello", v1)
        assertEquals(1, computeCount)

        // Second call returns cached
        val v2 = scope.remember("key") {
            computeCount++
            "world"
        }
        assertEquals("hello", v2)
        assertEquals(1, computeCount)
    }

    @Test
    fun rememberWithInputRecomputesOnChange() {
        val scope = ViewScope()

        val v1 = scope.remember("key", "input1") { "result1" }
        assertEquals("result1", v1)

        // Same input — cached
        val v2 = scope.remember("key", "input1") { "result2" }
        assertEquals("result1", v2)

        // Different input — recomputes
        val v3 = scope.remember("key", "input2") { "result3" }
        assertEquals("result3", v3)
    }

    @Test
    fun differentKeysAreSeparate() {
        val scope = ViewScope()

        val a = scope.remember("a") { 1 }
        val b = scope.remember("b") { 2 }
        assertEquals(1, a)
        assertEquals(2, b)
    }
}
