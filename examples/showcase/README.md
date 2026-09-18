# Kewt Showcase Example

A single-screen tour of the Kewt widget library: layout weights, borders with
titles, styled and wrapped text, progress bars, spinners, checkboxes, a focusable
text input and select list, a table, plus mouse, paste, and focus routing.

## Overview

This example demonstrates:
1. **Box model layout**: `Row`/`Column` with `weight`, `border`, and `padding`.
2. **Widgets**: `ProgressBar`, `Spinner`, `Checkbox`, `Divider`, `Spacer`, `Table`,
   `TextInput`, `SelectList`.
3. **Focus system**: `rememberFocusManager` routes `Tab`/`Shift+Tab` between the
   input and the list; printable keys go to the focused widget.
4. **Events**: mouse coordinates, bracketed paste insertion, timers via `every`.
5. **Theming & styling**: colors, bold/underline, bordered panels, embedded
   border titles.

## Controls

| Key | Action |
| --- | --- |
| `Tab` / `Shift+Tab` | Move focus between widgets |
| `Ctrl+T` | Toggle the checkbox |
| `Ctrl+C` / `Esc` | Quit |
| Paste | Inserts into the focused text input |

## How to Run

From the root of the project, run:

```bash
./gradlew :examples:showcase:run
```
