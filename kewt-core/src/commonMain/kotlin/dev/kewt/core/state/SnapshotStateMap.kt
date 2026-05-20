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

public fun <K, V> mutableStateMapOf(vararg pairs: Pair<K, V>): SnapshotStateMap<K, V> =
    SnapshotStateMap(pairs.toMap().toMutableMap())

public class SnapshotStateMap<K, V> internal constructor(
    private val backing: MutableMap<K, V>,
) : MutableMap<K, V> {
    private val version = mutableStateOf(0)

    private fun read() {
        version.value
    }

    private fun write() {
        version.value++
    }

    override val size: Int
        get() {
            read()
            return backing.size
        }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            read()
            return backing.entries
        }

    override val keys: MutableSet<K>
        get() {
            read()
            return backing.keys
        }

    override val values: MutableCollection<V>
        get() {
            read()
            return backing.values
        }

    override fun isEmpty(): Boolean {
        read()
        return backing.isEmpty()
    }

    override fun containsKey(key: K): Boolean {
        read()
        return backing.containsKey(key)
    }

    override fun containsValue(value: V): Boolean {
        read()
        return backing.containsValue(value)
    }

    override fun get(key: K): V? {
        read()
        return backing[key]
    }

    override fun put(key: K, value: V): V? {
        val r = backing.put(key, value)
        write()
        return r
    }

    override fun putAll(from: Map<out K, V>) {
        backing.putAll(from)
        write()
    }

    override fun remove(key: K): V? {
        val r = backing.remove(key)
        if (r != null) write()
        return r
    }

    override fun clear() {
        backing.clear()
        write()
    }
}
