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

public fun <T> mutableStateListOf(vararg items: T): SnapshotStateList<T> = SnapshotStateList(items.toMutableList())

public class SnapshotStateList<T> internal constructor(private val backing: MutableList<T>) : MutableList<T> {
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

    override fun isEmpty(): Boolean {
        read()
        return backing.isEmpty()
    }

    override fun contains(element: T): Boolean {
        read()
        return backing.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        read()
        return backing.containsAll(elements)
    }

    override fun get(index: Int): T {
        read()
        return backing[index]
    }

    override fun indexOf(element: T): Int {
        read()
        return backing.indexOf(element)
    }

    override fun lastIndexOf(element: T): Int {
        read()
        return backing.lastIndexOf(element)
    }

    override fun iterator(): MutableIterator<T> {
        read()
        return backing.iterator()
    }

    override fun listIterator(): MutableListIterator<T> {
        read()
        return backing.listIterator()
    }

    override fun listIterator(index: Int): MutableListIterator<T> {
        read()
        return backing.listIterator(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        read()
        return backing.subList(fromIndex, toIndex)
    }

    override fun add(element: T): Boolean {
        val r = backing.add(element)
        write()
        return r
    }

    override fun add(index: Int, element: T) {
        backing.add(index, element)
        write()
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val r = backing.addAll(elements)
        write()
        return r
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        val r = backing.addAll(index, elements)
        write()
        return r
    }

    override fun remove(element: T): Boolean {
        val r = backing.remove(element)
        write()
        return r
    }

    override fun removeAt(index: Int): T {
        val r = backing.removeAt(index)
        write()
        return r
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        val r = backing.removeAll(elements)
        write()
        return r
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        val r = backing.retainAll(elements)
        write()
        return r
    }

    override fun set(index: Int, element: T): T {
        val r = backing.set(index, element)
        write()
        return r
    }

    override fun clear() {
        backing.clear()
        write()
    }
}
