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

public interface State<out T> {
    public val value: T
}

public interface MutableState<T> : State<T> {
    public override var value: T
}

public fun <T> mutableStateOf(initial: T): MutableState<T> = MutableStateImpl(initial)

public operator fun <T> State<T>.getValue(
    thisRef: Any?,
    property: KProperty<*>,
): T = value

public operator fun <T> MutableState<T>.setValue(
    thisRef: Any?,
    property: KProperty<*>,
    value: T,
) {
    this.value = value
}

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
