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

class SnapshotTest {
    @Test
    fun readInsideScopeTracksDependency() {
        var dirtyCount = 0
        val scope =
            object : Scope {
                override fun markDirty() {
                    dirtyCount++
                }
            }
        val state = mutableStateOf(0)

        Snapshot.observe(scope) { state.value }
        state.value = 1

        assertEquals(1, dirtyCount)
    }

    @Test
    fun readOutsideScopeDoesNotTrack() {
        var dirtyCount = 0
        val scope =
            object : Scope {
                override fun markDirty() {
                    dirtyCount++
                }
            }
        val state = mutableStateOf(0)

        // Read outside observe
        state.value
        state.value = 1

        assertEquals(0, dirtyCount)
    }

    @Test
    fun sameValueDoesNotNotify() {
        var dirtyCount = 0
        val scope =
            object : Scope {
                override fun markDirty() {
                    dirtyCount++
                }
            }
        val state = mutableStateOf(5)

        Snapshot.observe(scope) { state.value }
        state.value = 5 // same value

        assertEquals(0, dirtyCount)
    }
}
