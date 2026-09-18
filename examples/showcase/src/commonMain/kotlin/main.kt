import dev.kewt.core.kewt
import dev.kewt.core.state.getValue
import dev.kewt.core.state.mutableStateOf
import dev.kewt.core.state.setValue
import dev.kewt.modifier.BorderStyle
import dev.kewt.modifier.Color
import dev.kewt.modifier.HorizontalAlignment
import dev.kewt.modifier.Modifier
import dev.kewt.modifier.TextAlign
import dev.kewt.modifier.TextOverflow
import dev.kewt.modifier.bold
import dev.kewt.modifier.border
import dev.kewt.modifier.fillMaxHeight
import dev.kewt.modifier.fillMaxSize
import dev.kewt.modifier.fillMaxWidth
import dev.kewt.modifier.foreground
import dev.kewt.modifier.height
import dev.kewt.modifier.padding
import dev.kewt.modifier.underline
import dev.kewt.modifier.weight
import dev.kewt.modifier.width
import dev.kewt.terminal.Key
import dev.kewt.terminal.KeyModifier
import dev.kewt.ui.widgets.Box
import dev.kewt.ui.widgets.Checkbox
import dev.kewt.ui.widgets.Column
import dev.kewt.ui.widgets.Divider
import dev.kewt.ui.widgets.ProgressBar
import dev.kewt.ui.widgets.Row
import dev.kewt.ui.widgets.SelectList
import dev.kewt.ui.widgets.SelectionState
import dev.kewt.ui.widgets.Spacer
import dev.kewt.ui.widgets.Spinner
import dev.kewt.ui.widgets.Spinners
import dev.kewt.ui.widgets.Table
import dev.kewt.ui.widgets.Text
import dev.kewt.ui.widgets.TextInput
import dev.kewt.ui.widgets.TextInputState
import dev.kewt.ui.widgets.remember
import dev.kewt.ui.widgets.rememberFocusManager
import dev.kewt.ui.widgets.setContent
import kotlin.time.Duration.Companion.milliseconds

private val LANGUAGES = listOf("Kotlin", "Rust", "Go", "Zig", "C", "Hare", "Odin")

private val MODULE_ROWS =
    listOf(
        listOf("kewt-core", "buffers, diffing, state"),
        listOf("kewt-modifier", "styling, box model"),
        listOf("kewt-terminal", "ANSI, input parsing"),
        listOf("kewt-ui", "layout, widgets, focus"),
        listOf("kewt-test", "headless assertions"),
    )

fun main(): Unit =
    kewt {
        setTitle("Kewt Showcase")
        enableMouse()
        enablePaste()

        val input = TextInputState("edit me")
        val selection = SelectionState(0)

        var checked by mutableStateOf(true)
        var progress by mutableStateOf(0f)
        var mouseInfo by mutableStateOf("mouse: idle")

        onMouse { mouseInfo = "mouse: ${it.x},${it.y}" }
        onPaste { input.insert(it) }

        // Printable keys belong to the focused TextInput, so the app-level
        // shortcuts use combinations the input never consumes.
        onKey(Key.Char('c'), setOf(KeyModifier.Ctrl)) { exit() }
        onKey('\u001b') { exit() }
        onKey(Key.Char('t'), setOf(KeyModifier.Ctrl)) { checked = !checked }

        every(120.milliseconds) {
            progress = if (progress >= 1f) 0f else progress + 0.02f
        }

        setContent {
            val focus = rememberFocusManager()
            val languages = remember("languages") { LANGUAGES }

            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    "Kewt Showcase",
                    modifier = Modifier.foreground(Color.Cyan).bold(),
                )
                Divider()

                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(BorderStyle.Rounded, Color.Magenta)
                            .padding(1),
                    ) {
                        Text("Widgets", modifier = Modifier.bold().underline())
                        Spacer(Modifier.height(1))
                        Checkbox(checked = checked, label = "checkbox (Ctrl+T)")
                        Spacer(Modifier.height(1))
                        ProgressBar(
                            progress,
                            modifier = Modifier.fillMaxWidth(),
                            showPercentage = true,
                        )
                        Spacer(Modifier.height(1))
                        Row {
                            Spinner(Spinners.Dots)
                            Text(" animating spinner")
                        }
                        Divider()
                        Text(
                            "Text wraps inside its box using the real box model: " +
                                "margin, border, padding, weights, and alignment.",
                            overflow = TextOverflow.Wrap,
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(BorderStyle.Light, Color.Blue)
                            .padding(1),
                    ) {
                        Text("Focus & Input", modifier = Modifier.bold().underline())
                        Spacer(Modifier.height(1))
                        TextInput(input, modifier = Modifier.fillMaxWidth(), id = "input")
                        Spacer(Modifier.height(1))
                        SelectList(
                            languages,
                            selection,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            id = "list",
                        )
                        Text(
                            "selected: ${languages.getOrElse(selection.selectedIndex.value) { "-" }}",
                            modifier = Modifier.foreground(Color.Green),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStyle.Double, Color.Yellow, "modules", HorizontalAlignment.Center),
                ) {
                    Table(
                        rows = MODULE_ROWS,
                        headers = listOf("module", "provides"),
                        modifier = Modifier.padding(horizontal = 1),
                        columnSpacing = 3,
                        columnAlignments = listOf(TextAlign.Left, TextAlign.Right),
                    )
                }

                Divider()
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "[Tab] focus  [Ctrl+C/Esc] quit  [paste] inserts",
                        modifier = Modifier.foreground(Color.BrightBlack),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(mouseInfo, modifier = Modifier.foreground(Color.BrightBlack))
                    Spacer(Modifier.height(1).width(2))
                    Text(
                        "focus: ${focus.focused.value ?: "none"}",
                        modifier = Modifier.foreground(Color.BrightBlack),
                    )
                }
            }
        }
    }
