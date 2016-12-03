Markdown Writer FX Change Log
=============================

## 0.5

- Move selected lines up or down with `Alt+Up` or `Alt+Down`.
- Duplicate selected lines up or down with `Ctrl+Alt+Up` or `Ctrl+Alt+Down`.
- Indent or unindent lines with `Tab` or `Shift+Tab`. `Backspace` key unindents
  line if caret is in leading whitespace of a line.
- Auto-indent for block quotes (`Enter` key adds block quote markers to new line).
- Smart "Insert > Header X": Support toggle header and change header level.
- Smarter "Insert > Bold/Italic/Strikethrough/Inline Code": if selection contains
  bold/italic/strikethrough/inline-code text, then it will be changed to plain text.
- Improved "Insert > Link/Image" allows editing existing links/images in dialogs.
- Support Autolinks (link without text) and Email links.
- Fixed background color of selected text in editor.


## 0.4

- Find and Replace added.
- Show line numbers in editor (if enabled in Options dialog).
- "Save As" command added to menu bar.
- Automatically reload externally changed markdown files (on window activation).
- Smarter "Insert > Bold/Italic/Strikethrough": if used within inline code
  block, then backticks will be changed to `<code>` and `</code>`.


## 0.3

- Replaced Markdown processor [pegdown] with [commonmark-java]
  and [flexmark-java], which are much faster and implement the [CommonMark] specification.
  [flexmark-java] is always used in the editor for syntax highlighting. 
  The preview can use [commonmark-java] or [flexmark-java] (switchable in the toolbar).
- Redesigned main window UI (modern flat look).
- Removed the tabs "Preview", "HTML Source" and "Markdown AST" from the bottom
  of the preview. Instead added actions to "View" menu and toolbar (at the right side).
- Possibility to hide the Preview (deselect toggle button in toolbar).
- Quickly enable/disable markdown extensions with popover window from toolbar.
- Syntax highlighting improved.
- Use monospaced font in editor.
- Increase/decrease editor font size with `Ctrl++`/`Ctrl+-`. Reset with `Ctrl+0`.
- Configurable editor font family and size in Options dialog.
- Configurable Markdown filename extensions in Options dialog (fixes #2).
- Support \*.svg in image chooser dialog.
- RichTextFX updated to version 0.7-M2.


## 0.2

- RichTextFX (and dependencies) updated to version 0.6.10 (fixes bugs and memory leaks).
- pegdown Markdown parser updated to version 1.6.
- Added five new pegdown 1.6 extension flags to Markdown Options tab.
- Minor improvements.


## 0.1

- Initial release


[CommonMark]: http://commonmark.org/
[commonmark-java]: https://github.com/atlassian/commonmark-java
[flexmark-java]: https://github.com/vsch/flexmark-java
[pegdown]: https://github.com/sirthias/pegdown
