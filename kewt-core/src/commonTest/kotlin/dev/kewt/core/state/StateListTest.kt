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
import kotlin.test.assertTrue

class StateListTest {
    @Test
    fun addTriggersRerender() {
        var dirtyCount = 0
        val scope =
            object : Scope {
                override fun markDirty() {
                    dirtyCount++
                }
            }
        val list = mutableStateListOf("a", "b")
        Snapshot.observe(scope) { list.size }
        list.add("c")
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
        val list = mutableStateListOf("a", "b")
        Snapshot.observe(scope) { list.size }
        list.remove("a")
        assertEquals(1, dirtyCount)
    }

    @Test
    fun getReadsCorrectly() {
        val list = mutableStateListOf("x", "y", "z")
        assertEquals("y", list[1])
        assertEquals(3, list.size)
    }

    @Test
    fun setUpdatesElement() {
        val list = mutableStateListOf("a", "b")
        list[0] = "c"
        assertEquals("c", list[0])
    }

    @Test
    fun clearEmptiesList() {
        val list = mutableStateListOf("a", "b", "c")
        list.clear()
        assertTrue(list.isEmpty())
    }
}
