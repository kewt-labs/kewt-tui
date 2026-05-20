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
package dev.kewt.ui.widgets

/**
 * A unique sentinel object used to represent an uninitialized cache value.
 */
private object Unset

/**
 * Remembers a value computed by [calculation].
 *
 * The value is stored in the [ViewScope] cache and will be reused across re-renders
 * as long as the key remains the same.
 */
@Suppress("UNCHECKED_CAST")
public fun <T> ViewScope.remember(key: String, calculation: () -> T): T {
    val value = cache.getOrPut(key) { calculation() }
    return value as T
}

/**
 * Remembers a value computed by [calculation], recomputing it only if [input] changes.
 *
 * @param key A unique identifier for this cached value.
 * @param input A dependency that triggers recomputation when its value changes.
 * @param calculation The logic to produce the value.
 */
@Suppress("UNCHECKED_CAST")
public fun <T> ViewScope.remember(key: String, input: Any?, calculation: () -> T): T {
    val inputKey = "${key}__input"
    val prevInput = cache.getLastInput(inputKey)

    return if (prevInput != Unset && prevInput == input) {
        cache[key] as T
    } else {
        val value = calculation()
        cache[key] = value
        cache[inputKey] = input
        value
    }
}

/**
 * Internal helper to retrieve the last stored input, handling nulls correctly via [Unset].
 */
private fun MutableMap<String, Any?>.getLastInput(key: String): Any? {
    return if (containsKey(key)) get(key) else Unset
}
