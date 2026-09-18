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

import dev.kewt.core.state.getValue
import dev.kewt.core.state.mutableStateOf
import dev.kewt.core.state.setValue
import dev.kewt.platform.Size
import dev.kewt.terminal.ColorMode
import dev.kewt.terminal.Event
import dev.kewt.terminal.FocusEvent
import dev.kewt.terminal.Key
import dev.kewt.terminal.KeyEvent
import dev.kewt.terminal.KeyModifier
import dev.kewt.terminal.MouseButton
import dev.kewt.terminal.MouseEvent
import dev.kewt.terminal.MouseKind
import dev.kewt.terminal.PasteEvent
import dev.kewt.terminal.Terminal
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A minimal in-memory [Terminal] so [KewtApp] can be exercised without touching
 * a real TTY. Events are queued up-front; the test must always queue something
 * that triggers `exit()`, otherwise the event loop never terminates.
 */
private class FakeTerminal(
    private var width: Int = 80,
    private var height: Int = 24,
) : Terminal {
    override val colorMode: ColorMode = ColorMode.TrueColor

    val events = ArrayDeque<Event>()
    private val output = StringBuilder()

    var isRawMode: Boolean = false
        private set
    var isCursorHidden: Boolean = false
        private set
    var isMouseCaptureEnabled: Boolean = false
        private set
    var isPasteCaptureEnabled: Boolean = false
        private set
    var windowTitle: String = ""
        private set

    override fun enterRawMode() {
        isRawMode = true
    }

    override fun exitRawMode() {
        isRawMode = false
    }

    override fun size(): Size = Size(width, height)

    fun resize(
        width: Int,
        height: Int,
    ) {
        this.width = width
        this.height = height
    }

    override fun enableMouseCapture() {
        isMouseCaptureEnabled = true
    }

    override fun disableMouseCapture() {
        isMouseCaptureEnabled = false
    }

    override fun enablePasteCapture() {
        isPasteCaptureEnabled = true
    }

    override fun disablePasteCapture() {
        isPasteCaptureEnabled = false
    }

    override fun read(): Event? = events.removeFirstOrNull()

    override fun write(text: String) {
        output.append(text)
    }

    override fun flush() {}

    override fun moveCursor(
        x: Int,
        y: Int,
    ) {}

    override fun hideCursor() {
        isCursorHidden = true
    }

    override fun showCursor() {
        isCursorHidden = false
    }

    override fun clear() {
        output.clear()
    }

    override fun clearLine() {}

    override fun setTitle(title: String) {
        windowTitle = title
    }

    override fun poll(timeoutMs: Int): Boolean = events.isNotEmpty()

    fun output(): String = output.toString()
}

private fun runApp(
    terminal: FakeTerminal = FakeTerminal(),
    content: KewtApp.() -> Unit,
): FakeTerminal {
    runBlocking {
        val app = KewtApp(terminal, this)
        app.run(content)
    }
    return terminal
}

class KewtAppTest {
    @Test
    fun runTogglesRawModeAndCursor() {
        val terminal = FakeTerminal()
        terminal.events.addLast(KeyEvent(Key.Char('q')))
        runApp(terminal) {
            onKey('q') { exit() }
        }
        // Raw mode is entered for the run and cleaned up afterwards.
        assertFalse(terminal.isRawMode)
        assertFalse(terminal.isCursorHidden)
    }

    @Test
    fun initialViewIsRenderedToTerminal() {
        val terminal = FakeTerminal()
        terminal.events.addLast(KeyEvent(Key.Char('q')))
        runApp(terminal) {
            onKey('q') { exit() }
            view { writeString(0, 0, "Hello Kewt") }
        }
        assertTrue("Hello Kewt" in terminal.output())
    }

    @Test
    fun stateChangesTriggerRerender() {
        val terminal = FakeTerminal()
        terminal.events.addLast(KeyEvent(Key.Char('a')))
        terminal.events.addLast(KeyEvent(Key.Char('q')))
        runApp(terminal) {
            var count by mutableStateOf(0)
            onKey('a') { count++ }
            onKey('q') { exit() }
            view { writeString(0, 0, "count=$count") }
        }
        assertTrue("count=1" in terminal.output())
    }

    @Test
    fun invalidateForcesRerenderForPlainVariables() {
        val terminal = FakeTerminal()
        terminal.events.addLast(KeyEvent(Key.Char('a')))
        terminal.events.addLast(KeyEvent(Key.Char('q')))
        runApp(terminal) {
            var plain = 0
            onKey('a') {
                plain = 5
                invalidate()
            }
            onKey('q') { exit() }
            view { writeString(0, 0, "plain=$plain") }
        }
        assertTrue("plain=5" in terminal.output())
    }

    @Test
    fun interceptorConsumesKeyBeforeHandlers() {
        val terminal = FakeTerminal()
        terminal.events.addLast(KeyEvent(Key.Char('q')))
        var plainHandlerRan = false
        runApp(terminal) {
            setKeyInterceptor { event ->
                if (event.key == Key.Char('q')) {
                    exit()
                    true
                } else {
                    false
                }
            }
            onKey('q') { plainHandlerRan = true }
        }
        assertFalse(plainHandlerRan)
    }

    @Test
    fun comboHandlerReceivesModifiedKeys() {
        val terminal = FakeTerminal()
        terminal.events.addLast(KeyEvent(Key.Char('c'), setOf(KeyModifier.Ctrl)))
        terminal.events.addLast(KeyEvent(Key.Char('q')))
        var comboFired = false
        runApp(terminal) {
            onKey(Key.Char('c'), setOf(KeyModifier.Ctrl)) { comboFired = true }
            onKey('q') { exit() }
        }
        assertTrue(comboFired)
    }

    @Test
    fun tabAndBackTabRouteToDedicatedHandlers() {
        val terminal = FakeTerminal()
        terminal.events.addLast(KeyEvent(Key.Tab))
        terminal.events.addLast(KeyEvent(Key.BackTab))
        terminal.events.addLast(KeyEvent(Key.Char('q')))
        var tabs = 0
        var backTabs = 0
        runApp(terminal) {
            onTab { tabs++ }
            onBackTab { backTabs++ }
            onKey('q') { exit() }
        }
        assertEquals(1, tabs)
        assertEquals(1, backTabs)
    }

    @Test
    fun namedKeysMapToControlCharacters() {
        val terminal = FakeTerminal()
        terminal.events.addLast(KeyEvent(Key.Enter))
        terminal.events.addLast(KeyEvent(Key.Backspace))
        terminal.events.addLast(KeyEvent(Key.Escape))
        terminal.events.addLast(KeyEvent(Key.Char('q')))
        val seen = mutableListOf<Char>()
        runApp(terminal) {
            onKey('\r') { seen.add('\r') }
            onKey('\b') { seen.add('\b') }
            onKey('\u001b') { seen.add('\u001b') }
            onKey('q') { exit() }
        }
        assertEquals(listOf('\r', '\b', '\u001b'), seen)
    }

    @Test
    fun keyEventHandlerSeesEveryKeyEvent() {
        val terminal = FakeTerminal()
        terminal.events.addLast(KeyEvent(Key.Char('x')))
        terminal.events.addLast(KeyEvent(Key.Char('q')))
        val seen = mutableListOf<Key>()
        runApp(terminal) {
            onKeyEvent { seen.add(it.key) }
            onKey('q') { exit() }
        }
        assertEquals(listOf<Key>(Key.Char('x'), Key.Char('q')), seen)
    }

    @Test
    fun mousePasteAndFocusEventsAreDispatched() {
        val terminal = FakeTerminal()
        terminal.events.addLast(MouseEvent(3, 4, MouseKind.Down(MouseButton.Left)))
        terminal.events.addLast(PasteEvent("pasted"))
        terminal.events.addLast(FocusEvent(false))
        terminal.events.addLast(KeyEvent(Key.Char('q')))
        var mouse: MouseEvent? = null
        var pasted: String? = null
        var gained: Boolean? = null
        runApp(terminal) {
            enableMouse()
            enablePaste()
            onMouse { mouse = it }
            onPaste { pasted = it }
            onFocus { gained = it }
            onKey('q') { exit() }
        }
        assertEquals(3, mouse?.x)
        assertEquals(4, mouse?.y)
        assertEquals(MouseKind.Down(MouseButton.Left), mouse?.kind)
        assertEquals("pasted", pasted)
        assertEquals(false, gained)
        assertTrue(terminal.isMouseCaptureEnabled)
        assertTrue(terminal.isPasteCaptureEnabled)
    }

    @Test
    fun resizeRebuildsBuffersAndNotifies() {
        val terminal = FakeTerminal()
        terminal.events.addLast(KeyEvent(Key.Char('r')))
        terminal.events.addLast(KeyEvent(Key.Char('q')))
        var resizedTo: Size? = null
        var observedSize: Size? = null
        runApp(terminal) {
            onResize { resizedTo = it }
            onKey('r') { terminal.resize(40, 12) }
            onKey('q') { exit() }
            view {
                observedSize = Size(width, height)
                writeString(0, 0, "x")
            }
        }
        assertEquals(Size(40, 12), resizedTo)
        assertEquals(Size(40, 12), observedSize)
    }

    @Test
    fun setTitlePropagatesToTerminal() {
        val terminal = FakeTerminal()
        terminal.events.addLast(KeyEvent(Key.Char('q')))
        runApp(terminal) {
            setTitle("Kewt Demo")
            onKey('q') { exit() }
        }
        assertEquals("Kewt Demo", terminal.windowTitle)
    }

    @Test
    fun viewCrashRendersCrashScreenAndExitsOnAnyKey() {
        val terminal = FakeTerminal()
        terminal.events.addLast(KeyEvent(Key.Char('q')))
        runApp(terminal) {
            view { error("boom") }
        }
        val output = terminal.output()
        assertTrue("KEWT FRAMEWORK CRASH" in output)
        assertTrue("boom" in output)
        assertFalse(terminal.isRawMode)
    }

    @Test
    fun attributesPersistArbitraryData() {
        val terminal = FakeTerminal()
        terminal.events.addLast(KeyEvent(Key.Char('q')))
        var read: String? = null
        runApp(terminal) {
            attributes["key"] = "value"
            read = attributes["key"] as String?
            onKey('q') { exit() }
        }
        assertEquals("value", read)
    }

    @Test
    fun appSizeMatchesTerminal() {
        val terminal = FakeTerminal(width = 30, height = 10)
        terminal.events.addLast(KeyEvent(Key.Char('q')))
        var appSize: Size? = null
        runApp(terminal) {
            appSize = size
            onKey('q') { exit() }
        }
        assertEquals(Size(30, 10), appSize)
    }
}
