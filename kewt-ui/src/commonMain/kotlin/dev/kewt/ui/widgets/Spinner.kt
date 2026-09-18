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

import dev.kewt.core.buffer.Buffer
import dev.kewt.core.buffer.UnicodeWidth
import dev.kewt.core.state.State
import dev.kewt.modifier.Modifier
import dev.kewt.modifier.Style
import dev.kewt.modifier.resolveStyle
import dev.kewt.ui.layout.LayoutNode
import dev.kewt.ui.layout.LayoutType

/**
 * Describes the frames and timing of a spinner animation.
 *
 * @property frames The sequence of strings cycled through while animating.
 * @property intervalMs Milliseconds each frame is displayed.
 */
public data class SpinnerStyle(
    val frames: List<String>,
    val intervalMs: Long,
) {
    init {
        require(frames.isNotEmpty()) { "SpinnerStyle requires at least one frame" }
        require(intervalMs > 0) { "SpinnerStyle interval must be positive" }
    }
}

/**
 * A collection of ready-made spinner animations.
 */
public object Spinners {
    /** Braille dots, the classic CLI spinner. */
    public val Dots: SpinnerStyle =
        SpinnerStyle(listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"), 80)

    /** Rotating ASCII line. */
    public val Line: SpinnerStyle = SpinnerStyle(listOf("-", "\\", "|", "/"), 100)

    /** Rotating arrow. */
    public val Arrow: SpinnerStyle =
        SpinnerStyle(listOf("←", "↖", "↑", "↗", "→", "↘", "↓", "↙"), 100)

    /** Bouncing braille dot. */
    public val Bounce: SpinnerStyle = SpinnerStyle(listOf("⠁", "⠂", "⠄", "⠂"), 80)

    /** Pulsing block. */
    public val Pulse: SpinnerStyle =
        SpinnerStyle(listOf("█", "▓", "▒", "░", "▒", "▓"), 100)
}

internal class SpinnerViewNode(
    val spinnerStyle: SpinnerStyle,
    val modifier: Modifier,
    val tick: State<Long>,
) : ViewNode() {
    override fun toLayoutNode(): LayoutNode {
        var widest = 0
        spinnerStyle.frames.forEach { widest = maxOf(widest, UnicodeWidth.displayWidth(it)) }
        return LayoutNode(
            layoutType = LayoutType.Leaf,
            modifier = modifier,
            intrinsicWidth = widest,
            intrinsicHeight = 1,
        )
    }

    override fun paint(
        buffer: Buffer,
        node: LayoutNode,
        inheritedStyle: Style,
    ) {
        val style = inheritedStyle.merge(modifier.resolveStyle())
        val box = node.contentBox()
        if (box.width <= 0 || box.height <= 0) return

        val frames = spinnerStyle.frames
        val frameIndex = ((tick.value / spinnerStyle.intervalMs) % frames.size).toInt().coerceAtLeast(0)
        buffer.writeString(
            box.x,
            box.y,
            UnicodeWidth.truncate(frames[frameIndex], box.width),
            foreground = style.foreground,
            background = style.background,
            bold = style.bold,
            italic = style.italic,
            underline = style.underline,
            strikethrough = style.strikethrough,
            dim = style.dim,
            blink = style.blink,
            reverse = style.reverse,
            hidden = style.hidden,
        )
    }
}

/**
 * An animated spinner.
 *
 * Animation is driven by the ticker installed by [setContent]; the frame is derived
 * from the clock so spinners stay in sync and require no manual state.
 *
 * @param style Frames and timing, see [Spinners] for presets.
 */
@Suppress("FunctionName")
public fun ViewScope.Spinner(
    style: SpinnerStyle = Spinners.Dots,
    modifier: Modifier = Modifier,
) {
    animationRequested = true
    children.add(SpinnerViewNode(style, modifier, tick))
}
