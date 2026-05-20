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
import kotlin.concurrent.AtomicReference

/**
 * Central orchestrator for the reactive state system.
 *
 * Snapshot tracks which [Scope]s read which [State] objects and ensures that
 * when a state changes, all dependent scopes are notified.
 */
public object Snapshot {
    internal var currentScope: Scope? = null

    // Use thread-safe map storage with Copy-on-Write pattern for multithreaded safety
    private val observers = AtomicReference<Map<State<*>, Set<Scope>>>(emptyMap())

    /**
     * Records read access to a [state] object within the current active scope.
     */
    internal fun onRead(state: State<*>) {
        val scope = currentScope ?: return

        while (true) {
            val old = observers.value
            val currentSet = old[state] ?: emptySet()
            if (scope in currentSet) return

            val new = old + (state to (currentSet + scope))
            if (observers.compareAndSet(old, new)) break
        }
    }

    /**
     * Notifies all scopes that depend on the given [state] that its value has changed.
     *
     * Dependent scopes are removed from the tracking map after notification to prevent
     * memory leaks. They will re-register themselves during their next execution.
     */
    internal fun notifyWrite(state: State<*>) {
        var impactedScopes: Set<Scope>?

        while (true) {
            val old = observers.value
            impactedScopes = old[state]
            if (impactedScopes == null) return

            val new = old - state
            if (observers.compareAndSet(old, new)) break
        }

        impactedScopes.forEach { it.markDirty() }
    }

    /**
     * Explicitly removes a [scope] from all state tracking.
     *
     * This is used during widget disposal to ensure no references to the scope remain.
     */
    internal fun removeScope(scope: Scope) {
        while (true) {
            val old = observers.value
            val new = old.mapValues { it.value - scope }.filterValues { it.isNotEmpty() }
            if (observers.compareAndSet(old, new)) break
        }
    }

    /**
     * Executes a block of code while tracking all state reads into the provided [scope].
     */
    internal inline fun <T> observe(
        scope: Scope,
        block: () -> T,
    ): T {
        val previous = currentScope
        currentScope = scope
        try {
            return block()
        } finally {
            currentScope = previous
        }
    }
}
