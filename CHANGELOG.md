# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Widgets**: `Table` (auto-sized columns, headers, per-column alignment), `TextInput`
  (block cursor, scrolling, Emacs-style Ctrl bindings), `SelectList` (keyboard navigation,
  scrolling window, theme highlight), `ProgressBar` (percentage/custom labels), `Spinner`
  (presets: Dots, Line, Arrow, Bounce, Pulse), `Checkbox`, `Divider` (horizontal/vertical),
  and `Spacer`.
- **Text layout**: word wrapping (`TextOverflow.Wrap`), ellipsis truncation, clip,
  horizontal text alignment (`TextAlign`), and `maxLines`.
- **Style system**: immutable `Style` with full attribute set (bold, italic, underline,
  strikethrough, dim, blink, reverse, hidden), style inheritance through the view tree,
  `Modifier.style`, and per-attribute modifier shortcuts.
- **Colors**: hex parsing (`Color.fromHex`), gray ramp, coerced `ansi16`/`ansi256`
  factories, and clamped RGB packing.
- **Layout**: real box model (margin → border → padding → content), `weight` distribution
  in `Row`/`Column`, `Arrangement` (Start/Center/End/SpaceBetween/SpaceAround/SpaceEvenly),
  child alignment overrides, `offset`, `fillMaxWidth`/`fillMaxHeight` fractions,
  `margin`, and border titles with alignment.
- **Focus system**: `rememberFocusManager` with Tab/Shift-Tab traversal and automatic key
  routing to the focused widget; app-level `setKeyInterceptor`.
- **Input**: SGR and X10 mouse protocols, wheel/drag/motion events, bracketed paste,
  focus in/out events, CSI-u keyboard protocol, Alt-modified keys, F1–F20, and non-ASCII
  text input (supplementary plane via `Key.Text`).
- **Unicode**: display-width tables for CJK/emoji/wide characters, surrogate-pair aware
  buffer writes, and continuation-cell handling in diffing.
- **Terminal**: `NO_COLOR`/`COLORTERM` aware color detection, window title control,
  mouse/paste capture toggles.
- **Testing**: `TestTerminal` gained mouse/paste/focus simulation, capture-flag tracking,
  and color-mode configuration; `assertCellAt` now checks dim/blink/reverse/hidden.
- **Example**: new `examples/showcase` app touring every widget and the focus system.

### Fixed
- `BufferDiff` emitted foreground codes (`38;2`) for RGB backgrounds; now emits `48;2`.
- `BufferDiff` threw `ArrayIndexOutOfBoundsException` when buffers had mismatched sizes.
- SGR mouse reports decoded the X coordinate as the modifier bitmask; modifiers now come
  exclusively from the callback bits (Shift/Alt/Ctrl).
- `ColorMode.detect` ignored the `NO_COLOR` convention.
- `remember(key) { null }` recomputed on every render; nulls are now cached.
- Crash screen threw on terminals shorter than 10 rows.
- `setContent` measured with unbounded height, so `fillMaxHeight` never resolved.
- Leaf padding was ignored during measurement; `Weight`/`Alignment`/`Offset` modifiers had
  no effect; borders required a manual `padding(1)` hack.
- Word wrap produced spurious empty lines and split words when a wrap landed on a space.

### Changed
- `LayoutNode` exposes `borderBox()`/`contentBox()` rectangles; `x`/`y` point at the
  border box origin and `width`/`height` include margins.
- `renderViewScope`/`buildView` clip painting to each container's content box.

## [0.1.0] - 2026-05-21

### Added
- Initial framework release with support for reactive state and ANSI terminal interaction.
- Layout system with Box, Row, and Column support.
- Modifier system for styling and layout constraints.
- Multi-platform support for macOS and Linux.
