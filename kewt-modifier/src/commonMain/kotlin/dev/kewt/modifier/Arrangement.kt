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
 * Describes how children are distributed along the main axis of a layout container.
 *
 * Used by `Row` (horizontal main axis) and `Column` (vertical main axis).
 */
public enum class Arrangement {
    /** Children are packed at the start of the main axis. */
    Start,

    /** Children are packed in the center of the main axis. */
    Center,

    /** Children are packed at the end of the main axis. */
    End,

    /** Free space is distributed evenly between children (no leading/trailing space). */
    SpaceBetween,

    /** Free space is distributed evenly around each child (half-space at the edges). */
    SpaceAround,

    /** Free space is distributed evenly between and around all children. */
    SpaceEvenly,
}

/**
 * Horizontal alignment options for text content.
 */
public enum class TextAlign {
    Left,
    Center,
    Right,
}

/**
 * Describes what happens when text does not fit into its allotted width.
 */
public enum class TextOverflow {
    /** Text is cut off at the boundary. */
    Clip,

    /** Text is cut off at the boundary and an ellipsis (…) is appended. */
    Ellipsis,

    /** Text is wrapped onto multiple lines at word boundaries. */
    Wrap,
}
