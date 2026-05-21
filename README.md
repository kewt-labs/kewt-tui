# 🦎 Kewt (Experimental)

Kewt is an experimental Kotlin TUI (Terminal User Interface) Framework for Kotlin Multiplatform, designed with a focus on developer experience, reactive state, and performance.

> [!WARNING]
> This is an experimental project. The API is unstable and subject to breaking changes. It is not recommended for production use.

## Documentation

The full API documentation is available at:
**[https://kewt-labs.github.io/kewt/](https://kewt-labs.github.io/kewt/)**

## Key Concepts

### 1. Reactive State Management
Kewt implements a snapshot-based reactive system similar to Jetpack Compose. When you wrap your data in `mutableStateOf()`, Kewt automatically tracks which parts of your UI read that data. If the data changes, only the affected components are re-rendered.

### 2. Modifier Pattern
Layout and styling are handled through a chainable `Modifier` system. You can easily apply padding, alignment, colors, and borders in a clean, declarative way:
```kotlin
Text("Hello", modifier = Modifier.foreground(Color.Cyan).bold().padding(2))
```

### 3. Double-Buffered Rendering
To ensure a flicker-free experience, Kewt uses a double-buffered renderer. It calculates the difference (diff) between the current frame and the previous one, sending only the minimum required ANSI escape sequences to the terminal.

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

## Limitations

- **Platform Support**: Optimized for POSIX (macOS, Linux). Windows support is planned but not yet implemented.
- **Unicode**: Basic support for UTF-8; complex wide characters (emojis/CJK) may have alignment issues in some terminals.
- **Widgets**: Initial set includes `Box`, `Row`, `Column`, and `Text`. More complex widgets (inputs, lists, etc.) are in development.

## License

Copyright 2026 Kewt Labs

Kewt is released under the **Apache License 2.0**. See the [LICENSE](LICENSE) file for more details.
