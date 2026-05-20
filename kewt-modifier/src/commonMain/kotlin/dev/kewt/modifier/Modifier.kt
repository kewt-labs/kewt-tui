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
package dev.kewt.modifier

/**
 * An ordered, immutable collection of elements that decorate or augment a UI component.
 *
 * Modifiers are used to configure layout behavior, visual appearance, and other properties.
 * They are chained together using the [then] operator.
 */
public interface Modifier {
    /**
     * Accumulates a value by applying an [operation] to each element in the modifier chain,
     * starting from the head (outermost) and moving towards the tail (innermost).
     */
    public fun <R> foldIn(initial: R, operation: (R, Element) -> R): R

    /**
     * Accumulates a value by applying an [operation] to each element in the modifier chain,
     * starting from the tail (innermost) and moving towards the head (outermost).
     */
    public fun <R> foldOut(initial: R, operation: (Element, R) -> R): R

    /**
     * Chains this modifier with [other].
     *
     * @return A new modifier representing the combination of both.
     */
    public infix fun then(other: Modifier): Modifier = if (other === Companion) this else CombinedModifier(this, other)

    /**
     * A single element in a [Modifier] chain.
     */
    public interface Element : Modifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = operation(initial, this)

        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R = operation(this, initial)
    }

    /**
     * The empty [Modifier] that contains no elements.
     */
    public companion object : Modifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = initial
        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R = initial
    }
}

/**
 * Flattens the recursive [Modifier] chain into a linear list of elements.
 */
public fun Modifier.flatten(): List<Modifier.Element> {
    val result = mutableListOf<Modifier.Element>()
    foldIn(Unit) { _, element ->
        result.add(element)
    }
    return result
}

/**
 * Combines two [Modifier] instances into a single chain.
 */
private class CombinedModifier(val outer: Modifier, val inner: Modifier) : Modifier {
    override fun <R> foldIn(initial: R, operation: (R, Modifier.Element) -> R): R =
        inner.foldIn(outer.foldIn(initial, operation), operation)

    override fun <R> foldOut(initial: R, operation: (Modifier.Element, R) -> R): R =
        outer.foldOut(inner.foldOut(initial, operation), operation)
}

/**
 * Finds the last element of type [T] in the modifier chain.
 */
public inline fun <reified T : Modifier.Element> Modifier.findElement(): T? {
    var result: T? = null
    foldIn(Unit) { _, element ->
        if (element is T) result = element
    }
    return result
}
