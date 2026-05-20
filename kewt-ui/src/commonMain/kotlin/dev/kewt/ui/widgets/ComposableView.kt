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

public fun KewtApp.setContent(content: ViewScope.() -> Unit) {
    val persistentScope = ViewScope()
    view {
        persistentScope.children.clear()
        persistentScope.content()
        renderViewScope(this, persistentScope)
    }
}

internal fun renderViewScope(buffer: Buffer, scope: ViewScope) {
    val rootNode = ContainerViewNode(LayoutType.Column, Modifier, scope.children)
    val layoutRoot = rootNode.toLayoutNode()

    layoutRoot.measure(Constraints(maxWidth = buffer.width, maxHeight = Int.MAX_VALUE))

    layoutRoot.place(0, 0)
    rootNode.paint(buffer, layoutRoot)
}
