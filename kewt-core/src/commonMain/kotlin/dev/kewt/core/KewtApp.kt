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
package dev.kewt.core

import dev.kewt.core.buffer.Buffer
import dev.kewt.core.buffer.BufferDiff
import dev.kewt.core.runtime.Scope
import dev.kewt.core.state.Snapshot
import dev.kewt.modifier.Color
import dev.kewt.terminal.AnsiTerminal
import dev.kewt.terminal.Event
import dev.kewt.terminal.Key
import dev.kewt.terminal.KeyEvent
import dev.kewt.terminal.KeyModifier
import dev.kewt.terminal.Terminal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.time.Duration

/**
 * Entry point for creating and running a Kewt TUI application.
 *
 * This function initializes the terminal, sets up the application scope, and starts the main event loop.
 *
 * @param terminal The [Terminal] implementation to use. Defaults to [AnsiTerminal].
 * @param content A lambda that configures the [KewtApp] instance.
 */
public fun kewt(
    terminal: Terminal = AnsiTerminal(),
    content: KewtApp.() -> Unit,
) {
    runBlocking {
        val app = KewtApp(terminal, this)
        app.run(content)
    }
}

/**
 * Represents a Kewt application instance.
 *
 * Managed the application lifecycle, input dispatching, state observation, and rendering.
 *
 * @property terminal The terminal instance used for IO.
 * @param parentScope The coroutine scope from which the application scope is derived.
 */
public class KewtApp internal constructor(
    private val terminal: Terminal,
    parentScope: CoroutineScope,
) {
    private val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob())
    private val diff = BufferDiff()
    private var currentBuffer = Buffer(1, 1)
    private var previousBuffer = Buffer(1, 1)
    private var running = true
    private var needsRender = true
    private val keyHandlers = mutableMapOf<Char, () -> Unit>()
    private val keyComboHandlers = mutableListOf<KeyCombo>()
    private var keyEventHandler: ((KeyEvent) -> Unit)? = null
    private var tabHandler: (() -> Unit)? = null
    private var backTabHandler: (() -> Unit)? = null
    private var viewFn: (Buffer.() -> Unit)? = null
    private var crashException: Throwable? = null

    /**
     * A map of arbitrary attributes associated with this application instance.
     * Can be used by framework extensions to store persistent state.
     */
    public val attributes: MutableMap<String, Any?> = mutableMapOf()

    private val renderScope =
        object : Scope {
            override fun markDirty() {
                needsRender = true
            }
        }

    /**
     * Initializes the terminal state and enters the main loop.
     */
    internal suspend fun run(content: KewtApp.() -> Unit) {
        terminal.enterRawMode()
        terminal.hideCursor()
        terminal.clear()

        val size = terminal.size()
        currentBuffer = Buffer(size.width, size.height)
        previousBuffer = Buffer(size.width, size.height)

        content()

        render()
        flush()

        try {
            loop()
        } finally {
            terminal.showCursor()
            terminal.exitRawMode()
            scope.cancel()
        }
    }

    /**
     * The main event loop that handles window resizing, input polling, and rendering.
     */
    private suspend fun loop() {
        while (running) {
            val size = terminal.size()
            if (size.width != currentBuffer.width || size.height != currentBuffer.height) {
                currentBuffer = Buffer(size.width, size.height)
                previousBuffer = Buffer(size.width, size.height)
                terminal.clear()
                needsRender = true
            }

            // Wait for input with a short timeout to allow for other tasks (like 'every' timer)
            if (terminal.poll(16)) {
                val event = terminal.read()
                if (event != null) dispatchEvent(event)
            }

            if (needsRender) {
                render()
                flush()
            }

            yield()
        }
    }

    /**
     * Dispatches input events to the registered handlers.
     */
    private fun dispatchEvent(event: Event) {
        when (event) {
            is KeyEvent -> {
                keyComboHandlers.forEach { combo ->
                    if (combo.key == event.key && combo.modifiers == event.modifiers) {
                        combo.handler()
                    }
                }

                val key = event.key
                if (event.modifiers.isEmpty()) {
                    when (key) {
                        is Key.Char -> keyHandlers[key.c]?.invoke()
                        Key.Tab -> (tabHandler ?: keyHandlers['\t'])?.invoke()
                        Key.BackTab -> backTabHandler?.invoke()
                        Key.Enter -> keyHandlers['\r']?.invoke()
                        Key.Backspace -> keyHandlers['\b']?.invoke()
                        Key.Escape -> keyHandlers['\u001b']?.invoke()
                        else -> {}
                    }
                }
                keyEventHandler?.invoke(event)
            }
        }
    }

    /**
     * Executes the view function and captures state dependencies.
     */
    private fun render() {
        needsRender = false
        currentBuffer.clear()

        if (crashException != null) {
            renderCrashScreen(crashException!!)
            return
        }

        val fn = viewFn ?: return
        try {
            Snapshot.observe(renderScope) {
                fn.invoke(currentBuffer)
            }
        } catch (e: Throwable) {
            crashException = e
            renderCrashScreen(e)
        }
    }

    /**
     * Renders a crash screen when an unhandled exception occurs in the view block.
     */
    private fun renderCrashScreen(e: Throwable) {
        currentBuffer.clear()
        val bg = Color.Blue
        val fg = Color.White

        for (y in 0 until currentBuffer.height) {
            for (x in 0 until currentBuffer.width) {
                currentBuffer.setChar(x, y, ' ', foreground = fg, background = bg)
            }
        }

        currentBuffer.writeString(2, 2, "KEWT FRAMEWORK CRASH", foreground = fg, background = bg, bold = true)
        currentBuffer.writeString(2, 4, "An unhandled exception occurred in the view block:", foreground = fg, background = bg)
        currentBuffer.writeString(2, 6, e.toString(), foreground = Color.BrightYellow, background = bg, bold = true)

        val stackTrace = e.stackTraceToString().lines().take(currentBuffer.height - 10)
        stackTrace.forEachIndexed { i, line ->
            currentBuffer.writeString(2, 8 + i, line.take(currentBuffer.width - 4), foreground = fg, background = bg)
        }

        currentBuffer.writeString(2, currentBuffer.height - 2, "Press any key to exit...", foreground = fg, background = bg, italic = true)

        onKeyEvent { exit() }
    }

    /**
     * Diffs the current buffer against the previous one and writes changes to the terminal.
     */
    private fun flush() {
        val output = diff.diff(currentBuffer, previousBuffer)
        if (output.isNotEmpty()) {
            terminal.write(output)
            terminal.flush()
        }
        val temp = previousBuffer
        previousBuffer = currentBuffer
        currentBuffer = temp
        currentBuffer.clear()
    }

    /**
     * Sets the main view function for the application.
     *
     * @param block A lambda that defines how to draw the application state into the [Buffer].
     */
    public fun view(block: Buffer.() -> Unit) {
        viewFn = block
    }

    /**
     * Schedules a repeated background task.
     *
     * @param interval The delay between executions.
     * @param block The task to execute.
     */
    public fun every(
        interval: Duration,
        block: () -> Unit,
    ) {
        scope.launch {
            while (isActive) {
                delay(interval)
                if (!running) break
                block()
            }
        }
    }

    /**
     * Stops the application and exits the main loop.
     */
    public fun exit() {
        running = false
    }

    /**
     * Registers a handler for a simple character key press.
     */
    public fun onKey(
        char: Char,
        handler: () -> Unit,
    ) {
        keyHandlers[char] = handler
    }

    /**
     * Registers a low-level key event handler.
     */
    public fun onKeyEvent(handler: (KeyEvent) -> Unit) {
        keyEventHandler = handler
    }

    /**
     * Registers a handler for the Tab key.
     */
    public fun onTab(handler: () -> Unit) {
        tabHandler = handler
    }

    /**
     * Registers a handler for the Shift-Tab (BackTab) key.
     */
    public fun onBackTab(handler: () -> Unit) {
        backTabHandler = handler
    }

    /**
     * Registers a handler for a specific key combination (key plus modifiers).
     */
    public fun onKey(
        key: Key,
        modifiers: Set<KeyModifier>,
        handler: () -> Unit,
    ) {
        keyComboHandlers.add(KeyCombo(key, modifiers, handler))
    }
}

/**
 * Represents a specific key combination used for input handling.
 */
private class KeyCombo(
    val key: Key,
    val modifiers: Set<KeyModifier>,
    val handler: () -> Unit,
)
