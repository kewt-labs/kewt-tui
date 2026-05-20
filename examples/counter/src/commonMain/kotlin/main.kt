import dev.kewt.core.kewt
import dev.kewt.core.state.getValue
import dev.kewt.core.state.mutableStateOf
import dev.kewt.core.state.setValue
import dev.kewt.modifier.BorderStyle
import dev.kewt.modifier.Color
import dev.kewt.modifier.Modifier
import dev.kewt.modifier.bold
import dev.kewt.modifier.border
import dev.kewt.modifier.foreground
import dev.kewt.modifier.padding
import dev.kewt.ui.widgets.Box
import dev.kewt.ui.widgets.Column
import dev.kewt.ui.widgets.Text
import dev.kewt.ui.widgets.setContent

fun main(): Unit = kewt {
    var count by mutableStateOf(0)

    onKey('+') { count++ }
    onKey('-') { count-- }
    onKey('q') { exit() }

    setContent {
        Box(modifier = Modifier.border(BorderStyle.Rounded, Color.Cyan)) {
            Column(modifier = Modifier.padding(1)) {
                Text("Kewt Counter", modifier = Modifier.foreground(Color.Cyan).bold())
                Text("")
                Text(
                    "Count: $count",
                    modifier = Modifier.foreground(
                        when {
                            count > 0 -> Color.Green
                            count < 0 -> Color.Red
                            else -> Color.White
                        },
                    ),
                )
                Text("")
                Text("[+] inc [-] dec [q] quit", modifier = Modifier.foreground(Color.BrightBlack))
            }
        }
    }
}
