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

public fun <T> mutableStateListOf(vararg items: T): SnapshotStateList<T> =
    SnapshotStateList(items.toList())

public class SnapshotStateList<T> internal constructor(initial: List<T>) : MutableList<T> {
    private val version = mutableStateOf(0)
    private val backing = AtomicReference<List<T>>(initial)

    private fun read() {
        version.value
    }

    private fun write(update: (List<T>) -> List<T>) {
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

    override fun isEmpty(): Boolean {
        read()
        return backing.value.isEmpty()
    }

    override fun contains(element: T): Boolean {
        read()
        return backing.value.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        read()
        return backing.value.containsAll(elements)
    }

    override fun get(index: Int): T {
        read()
        return backing.value[index]
    }

    override fun indexOf(element: T): Int {
        read()
        return backing.value.indexOf(element)
    }

    override fun lastIndexOf(element: T): Int {
        read()
        return backing.value.lastIndexOf(element)
    }

    override fun add(element: T): Boolean {
        write { it + element }
        return true
    }

    override fun add(index: Int, element: T) {
        write {
            val list = it.toMutableList()
            list.add(index, element)
            list
        }
    }

    override fun addAll(elements: Collection<T>): Boolean {
        write { it + elements }
        return true
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        write {
            val list = it.toMutableList()
            list.addAll(index, elements)
            list
        }
        return true
    }

    override fun remove(element: T): Boolean {
        var removed = false
        write {
            val list = it.toMutableList()
            removed = list.remove(element)
            list
        }
        return removed
    }

    override fun removeAt(index: Int): T {
        var result: T? = null
        write {
            val list = it.toMutableList()
            result = list.removeAt(index)
            list
        }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        var removed = false
        write {
            val list = it.toMutableList()
            removed = list.removeAll(elements)
            list
        }
        return removed
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        var changed = false
        write {
            val list = it.toMutableList()
            changed = list.retainAll(elements)
            list
        }
        return changed
    }

    override fun set(index: Int, element: T): T {
        var oldElement: T? = null
        write {
            val list = it.toMutableList()
            oldElement = list.set(index, element)
            list
        }
        @Suppress("UNCHECKED_CAST")
        return oldElement as T
    }

    override fun clear() {
        write { emptyList() }
    }

    // Iterators must return a snapshot of the current state to be thread-safe
    override fun iterator(): MutableIterator<T> = listIterator()

    override fun listIterator(): MutableListIterator<T> = listIterator(0)

    override fun listIterator(index: Int): MutableListIterator<T> {
        read()
        return backing.value.toMutableList().listIterator(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        read()
        return backing.value.subList(fromIndex, toIndex).toMutableList()
    }
}
