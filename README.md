# 🦎 Kewt (Experimental)

Kewt is an experimental Kotlin TUI (Terminal User Interface) Framework for Kotlin Multiplatform, designed with a focus on developer experience, reactive state, and performance.

> [!WARNING]
> This is an experimental project. The API is unstable and subject to breaking changes. It is not recommended for production use.

## Documentation

The full API documentation is available at:
**[https://kewt-labs.github.io/kewt-tui/](https://kewt-labs.github.io/kewt-tui/)**

## Key Concepts

### 1. Reactive State Management
Kewt implements a snapshot-based reactive system similar to Jetpack Compose. When you wrap your data in `mutableStateOf()`, Kewt automatically tracks which parts of your UI read that data. If the data changes, only the affected components are re-rendered.

### 2. Modifier Pattern
Layout and styling are handled through a chainable `Modifier` system implementing a real box model (margin → border → padding → content). Weights, arrangements, alignment, offsets, and the full attribute set (bold, italic, underline, strikethrough, dim, blink, reverse, hidden) compose declaratively:
```kotlin
Text("Hello", modifier = Modifier.foreground(Color.Cyan).bold().padding(2))

Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text("left", modifier = Modifier.weight(1f))
    Text("right", modifier = Modifier.align(VerticalAlignment.Center))
}
```

### 3. Widget Library
Beyond `Box`, `Row`, `Column`, and `Text` (with word wrap, ellipsis, alignment, and `maxLines`), Kewt ships production-style widgets:

| Widget | Highlights |
| --- | --- |
| `Table` | Auto-sized columns, headers, per-column alignment |
| `TextInput` | Block cursor, horizontal scrolling, Emacs-style Ctrl bindings |
| `SelectList` | Arrow/PageUp/PageDown navigation, scrolling window, theme highlight |
| `ProgressBar` | Fraction or percentage/custom label rendering |
| `Spinner` | Dots, Line, Arrow, Bounce, Pulse presets, driven by the animation ticker |
| `Checkbox`, `Divider`, `Spacer` | Composable building blocks |

Focusable widgets register with `rememberFocusManager()`; `Tab`/`Shift+Tab` traverse and keystrokes route to the focused component automatically.

### 4. Rich Input & Unicode
Kewt parses SGR/X10 mouse events (wheel, drag, motion), bracketed paste, focus in/out, CSI-u key reports, Alt-modified keys, and F1–F20. The cell buffer is Unicode-aware: CJK and emoji occupy two cells, surrogate pairs are never split, and diffing repaints wide characters atomically.

### 5. Double-Buffered Rendering
To ensure a flicker-free experience, Kewt uses a double-buffered renderer. It calculates the difference (diff) between the current frame and the previous one, sending only the minimum required ANSI escape sequences to the terminal. Colors automatically downgrade from TrueColor → 256 → 16 to match what the terminal reports (`COLORTERM`, `NO_COLOR`).

## Installation

Kewt is available via [JitPack](https://jitpack.io/#kewt-labs/kewt).

### 1. Add the repository

Add the JitPack repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add dependencies

Use the Kewt BOM to manage versions:

```kotlin
dependencies {
    implementation(platform("com.github.kewt-labs:kewt-bom:0.1.0"))
    implementation("com.github.kewt-labs:kewt-ui")
}
```

## Examples

- [`examples/counter`](examples/counter) — a minimal reactive counter.
- [`examples/showcase`](examples/showcase) — a single-screen tour of every widget, the focus system, mouse/paste events, and timers. Run it with `./gradlew :examples:showcase:run`.

## Limitations

- **Platform Support**: Optimized for POSIX (macOS, Linux). Windows support is planned but not yet implemented.
- **Unicode**: Wide characters (CJK/emoji) are laid out correctly, but grapheme clusters beyond combining marks (e.g. ZWJ emoji sequences) are rendered per code point.
- **Widgets**: The widget set covers the common terminal needs; advanced composites (tabs, trees, charts) are still evolving.

## License

Copyright 2026 Kewt Labs

Kewt is released under the **Apache License 2.0**. See the [LICENSE](LICENSE) file for more details.
