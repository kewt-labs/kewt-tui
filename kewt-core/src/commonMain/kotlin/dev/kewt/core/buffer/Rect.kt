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
package dev.kewt.core.buffer

/**
 * An axis-aligned rectangle of terminal cells.
 *
 * Used for clipping, region-based buffer operations, and layout geometry.
 *
 * @property x The left edge (0-indexed column).
 * @property y The top edge (0-indexed row).
 * @property width The number of columns spanned.
 * @property height The number of rows spanned.
 */
public data class Rect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
) {
    /** The column of the left edge. */
    public val left: Int get() = x

    /** The row of the top edge. */
    public val top: Int get() = y

    /** The exclusive column just past the right edge. */
    public val right: Int get() = x + width

    /** The exclusive row just past the bottom edge. */
    public val bottom: Int get() = y + height

    /** Whether the rectangle covers no cells at all. */
    public fun isEmpty(): Boolean = width <= 0 || height <= 0

    /** Whether the given cell coordinates lie inside this rectangle. */
    public fun contains(
        px: Int,
        py: Int,
    ): Boolean = px in x until right && py in y until bottom

    /** Whether this rectangle overlaps [other]. */
    public fun intersects(other: Rect): Boolean = intersection(other) != null

    /**
     * Returns the overlapping area of this rectangle and [other],
     * or null when they do not overlap.
     */
    public fun intersection(other: Rect): Rect? {
        val nx = maxOf(x, other.x)
        val ny = maxOf(y, other.y)
        val nr = minOf(right, other.right)
        val nb = minOf(bottom, other.bottom)
        if (nr <= nx || nb <= ny) return null
        return Rect(nx, ny, nr - nx, nb - ny)
    }

    public companion object {
        /** Intersects two optional rectangles; a null argument means "no constraint". */
        public fun intersectOrNull(
            a: Rect?,
            b: Rect?,
        ): Rect? =
            when {
                a == null -> b
                b == null -> a
                else -> a.intersection(b)
            }
    }
}
