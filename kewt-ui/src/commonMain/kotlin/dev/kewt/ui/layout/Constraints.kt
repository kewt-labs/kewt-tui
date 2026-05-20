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
package dev.kewt.ui.layout

public data class Constraints(
    val minWidth: Int = 0,
    val maxWidth: Int = Int.MAX_VALUE,
    val minHeight: Int = 0,
    val maxHeight: Int = Int.MAX_VALUE,
) {
    public fun constrain(
        width: Int,
        height: Int,
    ): Pair<Int, Int> = width.coerceIn(minWidth, maxWidth) to height.coerceIn(minHeight, maxHeight)

    public fun clampWidth(width: Int): Int = width.coerceIn(minWidth, maxWidth)
    public fun clampHeight(height: Int): Int = height.coerceIn(minHeight, maxHeight)

    public companion object {
        public fun fixed(
            width: Int,
            height: Int,
        ): Constraints = Constraints(
            minWidth = width,
            maxWidth = width,
            minHeight = height,
            maxHeight = height,
        )
    }
}

public data class MeasureResult(
    val width: Int,
    val height: Int,
)
