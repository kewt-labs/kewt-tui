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

import dev.kewt.core.KewtApp
import dev.kewt.core.buffer.Buffer
import dev.kewt.modifier.Modifier
import dev.kewt.ui.layout.Constraints
import dev.kewt.ui.layout.LayoutType

private const val VIEW_SCOPE_KEY = "dev.kewt.ui.ViewScope"

/**
 * Sets the high-level UI content for the application.
 *
 * This function initializes a persistent [ViewScope] that manages component state
 * and memoization across render passes.
 *
 * @param content A lambda that defines the UI hierarchy.
 */
public fun KewtApp.setContent(content: ViewScope.() -> Unit) {
    // Retrieve or create a persistent ViewScope stored in the app attributes
    @Suppress("UNCHECKED_CAST")
    val persistentScope = attributes.getOrPut(VIEW_SCOPE_KEY) { ViewScope() } as ViewScope

    view {
        persistentScope.children.clear()
        persistentScope.content()
        renderViewScope(this, persistentScope)
    }
}

/**
 * Internal renderer that converts a [ViewScope] hierarchy into buffer characters.
 */
internal fun renderViewScope(buffer: Buffer, scope: ViewScope) {
    val rootNode = ContainerViewNode(LayoutType.Column, Modifier, scope.children)
    val layoutRoot = rootNode.toLayoutNode()

    layoutRoot.measure(Constraints(maxWidth = buffer.width, maxHeight = Int.MAX_VALUE))

    // Starting from top-left (0,0)
    layoutRoot.place(0, 0)
    rootNode.paint(buffer, layoutRoot)
}
