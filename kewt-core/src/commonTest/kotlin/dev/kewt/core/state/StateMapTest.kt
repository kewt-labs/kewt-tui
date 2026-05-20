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

import dev.kewt.core.runtime.Scope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StateMapTest {
    @Test
    fun putTriggersRerender() {
        var dirtyCount = 0
        val scope =
            object : Scope {
                override fun markDirty() {
                    dirtyCount++
                }
            }
        val map = mutableStateMapOf("a" to 1)
        Snapshot.observe(scope) { map.size }
        map["b"] = 2
        assertEquals(1, dirtyCount)
    }

    @Test
    fun removeTriggersRerender() {
        var dirtyCount = 0
        val scope =
            object : Scope {
                override fun markDirty() {
                    dirtyCount++
                }
            }
        val map = mutableStateMapOf("a" to 1, "b" to 2)
        Snapshot.observe(scope) { map.size }
        map.remove("a")
        assertEquals(1, dirtyCount)
    }

    @Test
    fun getReadsCorrectly() {
        val map = mutableStateMapOf("x" to 10, "y" to 20)
        assertEquals(10, map["x"])
        assertNull(map["z"])
    }

    @Test
    fun clearEmptiesMap() {
        val map = mutableStateMapOf("a" to 1, "b" to 2)
        map.clear()
        assertTrue(map.isEmpty())
    }
}
