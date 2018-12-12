Markdown Writer FX Change Log
=============================

## Unreleased

- New "Stylesheets" tab in Options dialog allows specifying additional CSS for
  preview.
- Project specific options: Select "Store in project" checkbox in Options dialog
  to save options in per project (in file `<project-root>/.markdownwriterfx`).
- "Format only modified paragraphs on Save" checkbox added to Options dialog.
- Fixed behavior of `PageUp` and `PageDown` keys, which now always scroll a
  whole page up or down.
- Fixed behavior of `Ctrl+RIGHT`, which now moves caret to beginning of next
  word.
- Disabled "Save As" for read-only editors (binary or too-large files).


## 0.11

- Project Manager:
  - open any folder as project
  - project pane on left side of main window shows file tree of current project
  - combo box at top of project pane allows quick switching between projects
  - single-click on file in project tree opens file in preview mode (indicated
    by italic font in tab title), which will be replaced when opening another
    file in preview mode
  - double-click opens file in regular mode
  - drag-and-drop image/markdown file from project tree to markdown editor
    inserts a markdown link to that image/markdown file
- (experimental) Embedded images in markdown editor:
  - the leading '!' character of the markdown image description is replaced with
    the image
  - enable/disable in Options dialog (default is disabled)
  - works only for local images
  - embedded images are scaled down to maximum size of 200x200 pixels
- Smarter "Insert > Bold/Italic/Strikethrough/Inline Code": if selection is
  empty, then the word at the caret will be used as selection.
- Move selected lines down (with `Alt+Down`) no longer eats empty lines.
- Reformat fixes:
  - Avoid wrapping before blockquote and list markers.
  - Wrapping of block quotes.
- (experimental) Reformat selected paragraphs with `Ctrl+Shift+Alt+F` wraps
  lines at 80 characters.
- "Format All" and "Format Selection" commands added to "Edit" menu.
- Configurable formatting wrap line length in Options dialog.
- "Format on Save" checkbox added to Options dialog.
- [Prism] syntax highlighter updated to version 1.15.0.


## 0.10

- Editor improvements/fixes:
  - fixed `Home` and `End` keys in wrapped lines
  - improved undo/redo
  - fixed selection painting of wrapped lines
  - keep text selected after drag-and-drop
  - fixed occasional inconsistent heights of empty lines
- (experimental) Reformat all paragraphs with `Ctrl+Shift+F` wraps lines at 80
  characters.
- Updated [commonmark-java] and [flexmark-java] to [CommonMark] spec 0.28.
- Fixed `IndexOutOfBoundsException` in editor when reloading externally changed
  text.
- Fixed `IndexOutOfBoundsException` in HTML Source preview when deleting text in
  editor.


## 0.9

- Fixed "Space key after dead keys inserts a space character" (issue #20).


## 0.8

- Open markdown files by dropping them to main window.
- Support Definition lists extension (FlexMark only).
- "Cut", "Copy", "Paste" and "Select All" commands added to "Edit" menu.
- "Cut" and "Copy" now cut/copy whole line if selection is empty.
- Context menu added to editor.
- RichTextFX updated to version 0.7-M3.
- Fixed horizontal scrollbar in AST preview.


## 0.7

- Highlight paragraph in Preview that contain the caret of the editor.
- Show source positions for CommonMark in Markdown AST view.
- Reordered items in "Insert" menu and toolbar.
- Formatting toolbar items (bold, italic, etc) are highlighted if the caret in
  the editor is at formatted text.


## 0.6

- Syntax highlighting for HTML/XML/SVG/MathML in editor.
- Syntax highlighting for fenced code blocks in Preview that supports
  [120 languages](http://prismjs.com/#languages-list) (issue #9).
- Syntax highlighting in HTML source view.
- Syntax highlighting in Markdown AST view.
- Highlight ranges in Markdown AST view that contain the selection of the
  editor.


## 0.5

- Move selected lines up or down with `Alt+Up` or `Alt+Down`.
- Duplicate selected lines up or down with `Ctrl+Alt+Up` or `Ctrl+Alt+Down`.
- Indent or unindent lines with `Tab` or `Shift+Tab`. `Backspace` key unindents
  line if caret is in leading whitespace of a line.
- Auto-indent for block quotes and GFM task lists (`Enter` key adds block quote
  markers or task list markers to new line).
- Smart "Insert > Header X": Support toggle header and change header level.
- Smarter "Insert > Bold/Italic/Strikethrough/Inline Code": if selection
  contains bold/italic/strikethrough/inline-code text, then it will be changed
  to plain text.
- Improved "Insert > Link/Image" allows editing existing links/images in
  dialogs.
- Support Autolinks (link without text) and Email links.
- Configurable bold, italic and unordered list markers in Options dialog.
- Fixed background color of selected text in editor.
- Updated [commonmark-java] and [flexmark-java] to [CommonMark] spec 0.27.


## 0.4

- Find and Replace added.
- Show line numbers in editor (if enabled in Options dialog).
- "Save As" command added to menu bar.
- Automatically reload externally changed markdown files (on window activation).
- Smarter "Insert > Bold/Italic/Strikethrough": if used within inline code
  block, then backticks will be changed to `<code>` and `</code>`.


## 0.3

- Replaced Markdown processor [pegdown] with [commonmark-java] and
  [flexmark-java], which are much faster and implement the [CommonMark]
  specification. [flexmark-java] is always used in the editor for syntax
  highlighting. The preview can use [commonmark-java] or [flexmark-java]
  (switchable in the toolbar).
- Redesigned main window UI (modern flat look).
- Removed the tabs "Preview", "HTML Source" and "Markdown AST" from the bottom
  of the preview. Instead added actions to "View" menu and toolbar (at the right
  side).
- Possibility to hide the Preview (deselect toggle button in toolbar).
- Quickly enable/disable markdown extensions with popover window from toolbar.
- Syntax highlighting improved.
- Use monospaced font in editor.
- Increase/decrease editor font size with `Ctrl++`/`Ctrl+-`. Reset with
  `Ctrl+0`.
- Configurable editor font family and size in Options dialog.
- Configurable Markdown filename extensions in Options dialog (fixes #2).
- Support \*.svg in image chooser dialog.
- RichTextFX updated to version 0.7-M2.


## 0.2

- RichTextFX (and dependencies) updated to version 0.6.10 (fixes bugs and memory
  leaks).
- pegdown Markdown parser updated to version 1.6.
- Added five new pegdown 1.6 extension flags to Markdown Options tab.
- Minor improvements.


## 0.1

- Initial release


[CommonMark]: http://commonmark.org/
[commonmark-java]: https://github.com/atlassian/commonmark-java
[flexmark-java]: https://github.com/vsch/flexmark-java
[pegdown]: https://github.com/sirthias/pegdown
[Prism]: https://github.com/PrismJS/prism
