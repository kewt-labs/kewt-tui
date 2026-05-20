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

import kotlin.concurrent.AtomicReference

public fun <K, V> mutableStateMapOf(vararg pairs: Pair<K, V>): SnapshotStateMap<K, V> =
    SnapshotStateMap(pairs.toMap())

public class SnapshotStateMap<K, V> internal constructor(initial: Map<K, V>) : MutableMap<K, V> {
    private val version = mutableStateOf(0)
    private val backing = AtomicReference<Map<K, V>>(initial)

    private fun read() {
        version.value
    }

    private fun write(update: (Map<K, V>) -> Map<K, V>) {
        while (true) {
            val old = backing.value
            val new = update(old)
            if (backing.compareAndSet(old, new)) break
        }
        version.value++
    }

    override val size: Int
        get() {
            read()
            return backing.value.size
        }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            read()
            return backing.value.toMutableMap().entries
        }

    override val keys: MutableSet<K>
        get() {
            read()
            return backing.value.toMutableMap().keys
        }

    override val values: MutableCollection<V>
        get() {
            read()
            return backing.value.toMutableMap().values
        }

    override fun isEmpty(): Boolean {
        read()
        return backing.value.isEmpty()
    }

    override fun containsKey(key: K): Boolean {
        read()
        return backing.value.containsKey(key)
    }

    override fun containsValue(value: V): Boolean {
        read()
        return backing.value.containsValue(value)
    }

    override fun get(key: K): V? {
        read()
        return backing.value[key]
    }

    override fun put(key: K, value: V): V? {
        var old: V? = null
        write {
            val map = it.toMutableMap()
            old = map.put(key, value)
            map
        }
        return old
    }

    override fun putAll(from: Map<out K, V>) {
        write { it + from }
    }

    override fun remove(key: K): V? {
        var old: V? = null
        write {
            val map = it.toMutableMap()
            old = map.remove(key)
            map
        }
        return old
    }

    override fun clear() {
        write { emptyMap() }
    }
}
