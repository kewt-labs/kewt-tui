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
package dev.kewt.ui.theme

import dev.kewt.core.state.getValue
import dev.kewt.core.state.mutableStateOf
import dev.kewt.core.state.setValue
import dev.kewt.modifier.BorderStyle
import dev.kewt.modifier.Color

/**
 * Defines the color palette for a theme.
 */
public data class ColorScheme(
    val primary: Color = Color.rgb(0x1DB27B),
    val onPrimary: Color = Color.rgb(0x14162F),
    val secondary: Color = Color.rgb(0x14162F),
    val onSecondary: Color = Color.White,
    val background: Color = Color.Default,
    val onBackground: Color = Color.White,
    val surface: Color = Color.Ansi256(236),
    val onSurface: Color = Color.White,
    val error: Color = Color.Red,
    val border: Color = Color.Ansi256(240),
    val focusedBorder: Color = Color.rgb(0x1DB27B),
)

/**
 * Encapsulates all visual styling data for a theme.
 */
public data class KewtThemeData(
    val colors: ColorScheme = ColorScheme(),
    val borderStyle: BorderStyle = BorderStyle.Rounded,
)

/** The default dark theme configuration. */
public val DarkTheme: KewtThemeData = KewtThemeData()

/** A light theme configuration using branded colors. */
public val LightTheme: KewtThemeData = KewtThemeData(
    colors = ColorScheme(
        primary = Color.rgb(0x1DB27B),
        onPrimary = Color.rgb(0x14162F),
        secondary = Color.rgb(0x14162F),
        onSecondary = Color.White,
        background = Color.White,
        onBackground = Color.rgb(0x14162F),
        surface = Color.Ansi256(254),
        onSurface = Color.rgb(0x14162F),
        error = Color.Red,
        border = Color.Ansi256(245),
        focusedBorder = Color.rgb(0x1DB27B),
    ),
)

/**
 * Provides access to the current theme state.
 *
 * Changing the [current] theme data will automatically trigger a re-render
 * of UI components that depend on theme values.
 */
public object KewtTheme {
    /** The active theme data. */
    public var current: KewtThemeData by mutableStateOf(DarkTheme)

    /** Shortcut to access the current color scheme. */
    public val colors: ColorScheme get() = current.colors

    /** Shortcut to access the current border style. */
    public val borderStyle: BorderStyle get() = current.borderStyle
}
