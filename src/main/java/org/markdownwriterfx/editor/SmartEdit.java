/*
 * Copyright (c) 2016 Karl Tauber <karl at jformdesigner dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.markdownwriterfx.editor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.markdownwriterfx.dialogs.ImageDialog;
import org.markdownwriterfx.dialogs.LinkDialog;
import org.markdownwriterfx.util.Utils;

/**
 * Smart Markdown text edit methods.
 *
 * @author Karl Tauber
 */
public class SmartEdit
{
	private static final Pattern AUTO_INDENT_PATTERN = Pattern.compile(
			"(\\s*[*+-]\\s+|\\s*[0-9]+\\.\\s+|\\s+)(.*)");

	private final MarkdownEditorPane editor;
	private final StyleClassedTextArea textArea;

	public SmartEdit(MarkdownEditorPane editor, StyleClassedTextArea textArea) {
		this.editor = editor;
		this.textArea = textArea;
	}

	void enterPressed(KeyEvent e) {
		String currentLine = textArea.getText(textArea.getCurrentParagraph());

		String newText = "\n";
		Matcher matcher = AUTO_INDENT_PATTERN.matcher(currentLine);
		if (matcher.matches()) {
			if (!matcher.group(2).isEmpty()) {
				// indent new line with same whitespace characters and list markers as current line
				newText = newText.concat(matcher.group(1));
			} else {
				// current line contains only whitespace characters and list markers
				// --> empty current line
				int caretPosition = textArea.getCaretPosition();
				textArea.selectRange(caretPosition - currentLine.length(), caretPosition);
			}
		}
		textArea.replaceSelection(newText);
	}

	void deleteLine(KeyEvent e) {
		int start = textArea.getCaretPosition() - textArea.getCaretColumn();
		int end = start + textArea.getParagraph(textArea.getCurrentParagraph()).length() + 1;
		textArea.deleteText(start, end);
	}

	public void surroundSelection(String leading, String trailing) {
		surroundSelection(leading, trailing, null);
	}

	public void surroundSelection(String leading, String trailing, String hint) {
		// Note: not using textArea.insertText() to insert leading and trailing
		//       because this would add two changes to undo history

		IndexRange selection = textArea.getSelection();
		int start = selection.getStart();
		int end = selection.getEnd();

		String selectedText = textArea.getSelectedText();

		// remove leading and trailing whitespaces from selected text
		String trimmedSelectedText = selectedText.trim();
		if (trimmedSelectedText.length() < selectedText.length()) {
			start += selectedText.indexOf(trimmedSelectedText);
			end = start + trimmedSelectedText.length();
		}

		// remove leading whitespaces from leading text if selection starts at zero
		if (start == 0)
			leading = Utils.ltrim(leading);

		// remove trailing whitespaces from trailing text if selection ends at text end
		if (end == textArea.getLength())
			trailing = Utils.rtrim(trailing);

		// remove leading line separators from leading text
		// if there are line separators before the selected text
		if (leading.startsWith("\n")) {
			for (int i = start - 1; i >= 0 && leading.startsWith("\n"); i--) {
				if (!"\n".equals(textArea.getText(i, i + 1)))
					break;
				leading = leading.substring(1);
			}
		}

		// remove trailing line separators from trailing or leading text
		// if there are line separators after the selected text
		boolean trailingIsEmpty = trailing.isEmpty();
		String str = trailingIsEmpty ? leading : trailing;
		if (str.endsWith("\n")) {
			for (int i = end; i < textArea.getLength() && str.endsWith("\n"); i++) {
				if (!"\n".equals(textArea.getText(i, i + 1)))
					break;
				str = str.substring(0, str.length() - 1);
			}
			if (trailingIsEmpty)
				leading = str;
			else
				trailing = str;
		}

		int selStart = start + leading.length();
		int selEnd = end + leading.length();

		// insert hint text if selection is empty
		if (hint != null && trimmedSelectedText.isEmpty()) {
			trimmedSelectedText = hint;
			selEnd = selStart + hint.length();
		}

		// prevent undo merging with previous text entered by user
		textArea.getUndoManager().preventMerge();

		// replace text and update selection
		textArea.replaceText(start, end, leading + trimmedSelectedText + trailing);
		textArea.selectRange(selStart, selEnd);
	}

	public void insertLink() {
		LinkDialog dialog = new LinkDialog(editor.getNode().getScene().getWindow(), editor.getParentPath());
		dialog.showAndWait().ifPresent(result -> {
			textArea.replaceSelection(result);
		});
	}

	public void insertImage() {
		ImageDialog dialog = new ImageDialog(editor.getNode().getScene().getWindow(), editor.getParentPath());
		dialog.showAndWait().ifPresent(result -> {
			textArea.replaceSelection(result);
		});
	}
}
