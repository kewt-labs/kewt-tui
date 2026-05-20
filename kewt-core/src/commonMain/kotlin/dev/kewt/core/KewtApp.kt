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

public fun kewt(
    terminal: Terminal = AnsiTerminal(),
    content: KewtApp.() -> Unit,
) {
    runBlocking {
        val app = KewtApp(terminal, this)
        app.run(content)
    }
}

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

    private val renderScope =
        object : Scope {
            override fun markDirty() {
                needsRender = true
            }
        }

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

    private fun render() {
        needsRender = false
        currentBuffer.clear()
        val fn = viewFn ?: return
        Snapshot.observe(renderScope) {
            fn.invoke(currentBuffer)
        }
    }

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

    public fun view(block: Buffer.() -> Unit) {
        viewFn = block
    }

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

    public fun quit() {
        running = false
    }

    public fun onKey(
        char: Char,
        handler: () -> Unit,
    ) {
        keyHandlers[char] = handler
    }

    public fun onKeyEvent(handler: (KeyEvent) -> Unit) {
        keyEventHandler = handler
    }

    public fun onTab(handler: () -> Unit) {
        tabHandler = handler
    }

    public fun onBackTab(handler: () -> Unit) {
        backTabHandler = handler
    }

    public fun onKey(
        key: Key,
        modifiers: Set<KeyModifier>,
        handler: () -> Unit,
    ) {
        keyComboHandlers.add(KeyCombo(key, modifiers, handler))
    }
}

private class KeyCombo(
    val key: Key,
    val modifiers: Set<KeyModifier>,
    val handler: () -> Unit,
)
