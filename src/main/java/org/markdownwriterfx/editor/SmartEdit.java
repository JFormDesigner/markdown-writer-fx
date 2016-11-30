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

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyCombination.*;
import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;
import static org.fxmisc.wellbehaved.event.InputMap.consume;
import static org.fxmisc.wellbehaved.event.InputMap.sequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyEvent;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.TwoDimensional.Bias;
import org.fxmisc.wellbehaved.event.Nodes;
import org.markdownwriterfx.dialogs.ImageDialog;
import org.markdownwriterfx.dialogs.LinkDialog;
import org.markdownwriterfx.util.Utils;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.DelimitedNode;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ast.NodeVisitor;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough;
import com.vladsch.flexmark.util.sequence.BasedSequence;

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

	SmartEdit(MarkdownEditorPane editor, StyleClassedTextArea textArea) {
		this.editor = editor;
		this.textArea = textArea;

		Nodes.addInputMap(textArea, sequence(
			consume(keyPressed(ENTER),							this::enterPressed),
			consume(keyPressed(D, SHORTCUT_DOWN),				this::deleteLine),
			consume(keyPressed(UP, ALT_DOWN),					this::moveLinesUp),
			consume(keyPressed(DOWN, ALT_DOWN),					this::moveLinesDown),
			consume(keyPressed(UP, SHORTCUT_DOWN, ALT_DOWN),	this::duplicateLinesUp),
			consume(keyPressed(DOWN, SHORTCUT_DOWN, ALT_DOWN),	this::duplicateLinesDown)
		));
	}

	private void enterPressed(KeyEvent e) {
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

	private void deleteLine(KeyEvent e) {
		IndexRange selRange = selectedLinesRange();
		textArea.deleteText(selRange.getStart(), selRange.getEnd());
	}

	private void moveLinesUp(KeyEvent e) {
		IndexRange selRange = selectedLinesRange();
		int selStart = selRange.getStart();
		int selEnd = selRange.getEnd();
		if (selStart == 0)
			return;

		int before = offsetToLine(selStart - 1);
		IndexRange beforeRange = linesRange(before, before);
		int beforeStart = beforeRange.getStart();
		int beforeEnd = beforeRange.getEnd();

		String beforeText = textArea.getText(beforeStart, beforeEnd);
		String selText = textArea.getText(selStart, selEnd);
		if (!selText.endsWith("\n")) {
			selText += "\n";
			if (beforeText.endsWith("\n"))
				beforeText = beforeText.substring(0, beforeText.length() - 1);
		}

		// Note: using single textArea.replaceText() to avoid multiple changes in undo history
		textArea.replaceText(beforeStart, selEnd, selText + beforeText);
		textArea.selectRange(beforeStart, beforeStart + selText.length() - 1);
	}

	private void moveLinesDown(KeyEvent e) {
		IndexRange selRange = selectedLinesRange();
		int selStart = selRange.getStart();
		int selEnd = selRange.getEnd();
		if (selEnd == textArea.getLength())
			return;

		int after = offsetToLine(selEnd + 1);
		IndexRange afterRange = linesRange(after, after);
		int afterStart = afterRange.getStart();
		int afterEnd = afterRange.getEnd();

		String selText = textArea.getText(selStart, selEnd);
		String afterText = textArea.getText(afterStart, afterEnd);
		if (!afterText.endsWith("\n")) {
			afterText += "\n";
			if (selText.endsWith("\n"))
				selText = selText.substring(0, selText.length() - 1);
		}

		// Note: using single textArea.replaceText() to avoid multiple changes in undo history
		textArea.replaceText(selStart, afterEnd, afterText + selText);

		int newSelStart = selStart + afterText.length();
		int newSelEnd = newSelStart + selText.length();
		if (selText.endsWith("\n"))
			newSelEnd--;
		textArea.selectRange(newSelStart, newSelEnd);
	}

	private void duplicateLinesUp(KeyEvent e) {
		duplicateLines(true);
	}

	private void duplicateLinesDown(KeyEvent e) {
		duplicateLines(false);
	}

	private void duplicateLines(boolean up) {
		IndexRange selRange = selectedLinesRange();
		int selStart = selRange.getStart();
		int selEnd = selRange.getEnd();

		String selText = textArea.getText(selStart, selEnd);
		if (!selText.endsWith("\n"))
			selText += "\n";

		textArea.replaceText(selStart, selStart, selText);

		if (up)
			textArea.selectRange(selStart, selStart + selText.length() - 1);
		else {
			int newSelStart = selStart + selText.length();
			int newSelEnd = newSelStart + selText.length();
			if (selText.endsWith("\n"))
				newSelEnd--;
			textArea.selectRange(newSelStart, newSelEnd);
		}
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

	private void surroundSelectionAndReplaceMarker(String leading, String trailing,
			DelimitedNode node, String newOpeningMarker, String newClosingMarker)
	{
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

		BasedSequence openingMarker = node.getOpeningMarker();
		BasedSequence closingMarker = node.getClosingMarker();

		int selStart = start + leading.length() + (newOpeningMarker.length() - openingMarker.length());
		int selEnd = selStart + trimmedSelectedText.length();

		// prevent undo merging with previous text entered by user
		textArea.getUndoManager().preventMerge();

		// replace text and update selection
		// Note: using single textArea.replaceText() to avoid multiple changes in undo history
		String before = textArea.getText(openingMarker.getEndOffset(), start);
		String after = textArea.getText(end, closingMarker.getStartOffset());
		textArea.replaceText(openingMarker.getStartOffset(), closingMarker.getEndOffset(),
				newOpeningMarker + before + leading + trimmedSelectedText + trailing + after + newClosingMarker );
		textArea.selectRange(selStart, selEnd);
	}

	private void surroundSelectionInCode(String openCloseMarker) {
		Code codeNode = findNodeAtSelection(Code.class);
		if (codeNode != null)
			surroundSelectionAndReplaceMarker(openCloseMarker, openCloseMarker, codeNode, "<code>", "</code>");
		else
			surroundSelection(openCloseMarker, openCloseMarker);
	}

	public void insertBold() {
		// prevent undo merging with previous text entered by user
		textArea.getUndoManager().preventMerge();

		List<StrongEmphasis> nodes = findNodesAtSelection(StrongEmphasis.class);
		if (nodes.size() > 0) {
			// there is bold text in current selection --> change them to plain text
			removeDelimiters(nodes);
		} else
			surroundSelectionInCode("**");
	}

	public void insertItalic() {
		// prevent undo merging with previous text entered by user
		textArea.getUndoManager().preventMerge();

		List<Emphasis> nodes = findNodesAtSelection(Emphasis.class);
		if (nodes.size() > 0) {
			// there is bold text in current selection --> change them to plain text
			removeDelimiters(nodes);
		} else
			surroundSelectionInCode("_");
	}

	public void insertStrikethrough() {
		// prevent undo merging with previous text entered by user
		textArea.getUndoManager().preventMerge();

		List<Strikethrough> nodes = findNodesAtSelection(Strikethrough.class);
		if (nodes.size() > 0) {
			// there is bold text in current selection --> change them to plain text
			removeDelimiters(nodes);
		} else
			surroundSelectionInCode("~~");
	}

	private <T extends Node> void removeDelimiters(List<T> nodes) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < nodes.size(); i++) {
			T node = nodes.get(i);
			if (i > 0)
				buf.append(textArea.getText(nodes.get(i - 1).getEndOffset(), node.getStartOffset()));
			buf.append(((DelimitedNode)node).getText());
		}

		int start = nodes.get(0).getStartOffset();
		int end = nodes.get(nodes.size() - 1).getEndOffset();
		textArea.replaceText(start, end, buf.toString());
		textArea.selectRange(start, start + buf.length());
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

	public void insertHeading(int level, String hint) {
		// prevent undo merging with previous text entered by user
		textArea.getUndoManager().preventMerge();

		int caretPosition = textArea.getCaretPosition();
		Heading heading = findNodeAtLine(caretPosition, Heading.class);
		if (heading != null) {
			// there is already a heading at current line --> remove heading or change level
			if (level == heading.getLevel()) {
				// same heading level --> remove heading
				if (heading.isAtxHeading())
					textArea.deleteText(heading.getOpeningMarker().getStartOffset(), heading.getText().getStartOffset());
				else if (heading.isSetextHeading())
					textArea.deleteText(heading.getText().getEndOffset(), heading.getClosingMarker().getEndOffset());
			} else {
				// different heading level --> change heading level
				if (heading.isAtxHeading()) {
					// replace ATX opening marker
					String marker = StringUtils.repeat('#', level);
					BasedSequence openingMarker = heading.getOpeningMarker();
					textArea.replaceText(openingMarker.getStartOffset(), openingMarker.getEndOffset(), marker);

					// move caret to end of line
					selectEndOfLine(openingMarker.getStartOffset());
				} else if (heading.isSetextHeading()) {
					BasedSequence closingMarker = heading.getClosingMarker();
					if (level > 2) {
						// new level too large for setext --> change from setext to ATX header
						// Note: using single textArea.replaceText() to avoid multiple changes in undo history
						String newHeading = StringUtils.repeat('#', level) + " " + heading.getText();
						textArea.replaceText(heading.getStartOffset(), heading.getEndOffset(), newHeading);
					} else {
						// replace setext closing marker
						String marker = StringUtils.repeat(level == 1 ? '=' : '-', closingMarker.length());
						textArea.replaceText(closingMarker.getStartOffset(), closingMarker.getEndOffset(), marker);
					}
				}
			}
		} else {
			// new heading
			int lineStartOffset = caretPosition - textArea.getCaretColumn();
			String marker = StringUtils.repeat('#', level);
			String currentLine = textArea.getText(textArea.getCurrentParagraph());
			if (currentLine.trim().isEmpty()) {
				// current line is empty --> insert opening marker and hint
				textArea.replaceText(lineStartOffset, lineStartOffset + currentLine.length(), marker + " " + hint);

				// select hint
				int selStart = lineStartOffset + marker.length() + 1;
				int selEnd = selStart + hint.length();
				textArea.selectRange(selStart, selEnd);
			} else {
				// current line contains text --> insert opening marker
				if (!currentLine.startsWith(" "))
					marker += " ";
				textArea.insertText(lineStartOffset, marker);

				// move caret to end of line
				selectEndOfLine(lineStartOffset);
			}
		}
	}

	/**
	 * Find single node (of a specific class) that completely encloses the current selection.
	 */
	private <T extends Node> T findNodeAtSelection(Class<T> nodeClass) {
		IndexRange selection = textArea.getSelection();
		int start = selection.getStart();
		int end = selection.getEnd();
		List<T> nodes = findNodes(start, end, nodeClass);
		if (nodes.size() != 1)
			return null;

		T node = nodes.get(0);
		BasedSequence text = (node instanceof DelimitedNode) ? ((DelimitedNode)node).getText() : node.getChars();
		return (start >= text.getStartOffset() && end <= text.getEndOffset()) ? node : null;
	}

	/**
	 * Find all nodes of a specific class that are within the current selection.
	 */
	private <T> List<T> findNodesAtSelection(Class<T> nodeClass) {
		IndexRange selection = textArea.getSelection();
		return findNodes(selection.getStart(), selection.getEnd(), nodeClass);
	}

	/**
	 * Find all nodes of a specific class that are within the given range.
	 */
	private <T> List<T> findNodes(int start, int end, Class<T> nodeClass) {
		Node markdownAST = editor.getMarkdownAST();
		if (markdownAST == null)
			return Collections.emptyList();

		ArrayList<T> nodes = new ArrayList<>();
		NodeVisitor visitor = new NodeVisitor(Collections.emptyList()) {
			@SuppressWarnings("unchecked")
			@Override
			public void visit(Node node) {
				if (start <= node.getEndOffset() && end >= node.getStartOffset() && nodeClass.isInstance(node))
					nodes.add((T) node);
				else
					visitChildren(node);
			}
		};
		visitor.visit(markdownAST);
		return nodes;
	}

	/**
	 * Find first node of a specific class that is at the given offset.
	 */
	@SuppressWarnings("unused")
	private <T> T findNodeAt(int offset, Class<T> nodeClass) {
		List<T> nodes = findNodes(offset, offset, nodeClass);
		return nodes.size() > 0 ? nodes.get(0) : null;
	}

	private <T> T findNodeAtLine(int offsetInLine, Class<T> nodeClass) {
		int line = textArea.offsetToPosition(offsetInLine, Bias.Forward).getMajor();
		int lineStart = textArea.getAbsolutePosition(line, 0);
		int lineEnd = lineStart + textArea.getParagraph(line).length();
		List<T> nodes = findNodes(lineStart, lineEnd, nodeClass);
		return nodes.size() > 0 ? nodes.get(0) : null;
	}

	/**
	 * Moves the caret to the end of the line.
	 */
	private void selectEndOfLine(int offsetInLine) {
		int line = textArea.offsetToPosition(offsetInLine, Bias.Forward).getMajor();
		int caretPos = textArea.getAbsolutePosition(line, textArea.getParagraph(line).length());
		textArea.selectRange(caretPos, caretPos);
	}

	/**
	 * Returns the line (paragraph) number for the given character offset.
	 */
	private int offsetToLine(int offset) {
		return textArea.offsetToPosition(offset, Bias.Forward).getMajor();
	}

	/**
	 * Returns the line numbers of the first and last line that are (partly) selected.
	 */
	private IndexRange selectedLines() {
		IndexRange selection = textArea.getSelection();
		int start = selection.getStart();
		int end = Math.max(selection.getEnd() - 1, start); // excluding line separator
		return new IndexRange(offsetToLine(start), offsetToLine(end));
	}

	/**
	 * Returns start and end character offsets of the lines that are (partly) selected.
	 * The end offset includes the line separator.
	 */
	private IndexRange selectedLinesRange() {
		IndexRange selection = selectedLines();
		return linesRange(selection.getStart(), selection.getEnd());
	}

	/**
	 * Returns start and end character offsets of the given lines range.
	 * The end offset includes the line separator.
	 */
	private IndexRange linesRange(int firstLine, int lastLine) {
		int start = textArea.getAbsolutePosition(firstLine, 0);
		int end = textArea.getAbsolutePosition(lastLine, textArea.getParagraph(lastLine).length());
		if (end < textArea.getLength())
			end++; // line separator

		return new IndexRange(start, end);
	}
}
