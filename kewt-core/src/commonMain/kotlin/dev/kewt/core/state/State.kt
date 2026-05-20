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

import kotlin.reflect.KProperty

/**
 * Represents a value that can be observed by the framework.
 *
 * When a [State] is read within a reactive scope (like a view block), the scope
 * becomes a dependency of that state and will be notified when the value changes.
 */
public interface State<out T> {
    /**
     * The current value of the state.
     */
    public val value: T
}

/**
 * A [State] whose value can be updated.
 *
 * Updating the [value] will trigger notifications to all observing scopes.
 */
public interface MutableState<T> : State<T> {
    public override var value: T
}

/**
 * Creates a new [MutableState] initialized with [initial].
 */
public fun <T> mutableStateOf(initial: T): MutableState<T> = MutableStateImpl(initial)

/**
 * Delegate for reading [State] values using the `by` keyword.
 */
public operator fun <T> State<T>.getValue(
    thisRef: Any?,
    property: KProperty<*>,
): T = value

/**
 * Delegate for updating [MutableState] values using the `by` keyword.
 */
public operator fun <T> MutableState<T>.setValue(
    thisRef: Any?,
    property: KProperty<*>,
    value: T,
) {
    this.value = value
}

/**
 * Implementation of [MutableState] that hooks into the [Snapshot] system.
 */
internal class MutableStateImpl<T>(initial: T) : MutableState<T> {
    override var value: T = initial
        get() {
            Snapshot.onRead(this)
            return field
        }
        set(newValue) {
            if (field != newValue) {
                field = newValue
                Snapshot.notifyWrite(this)
            }
        }
}
