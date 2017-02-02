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
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyEvent;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.richtext.model.TwoDimensional.Bias;
import org.fxmisc.wellbehaved.event.Nodes;
import org.markdownwriterfx.dialogs.ImageDialog;
import org.markdownwriterfx.dialogs.LinkDialog;
import org.markdownwriterfx.options.Options;
import org.markdownwriterfx.util.Utils;
import com.vladsch.flexmark.ast.AutoLink;
import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.BulletListItem;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.DelimitedNode;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.ImageRef;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.LinkNode;
import com.vladsch.flexmark.ast.LinkRef;
import com.vladsch.flexmark.ast.ListBlock;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.MailLink;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ast.NodeVisitor;
import com.vladsch.flexmark.ast.OrderedListItem;
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
	private static final String TASK_LIST_MARKER = "(?:\\[[ xX]\\]\\s+|)";
	private static final String BULLET_LIST_MARKER = "\\s*[*+-]\\s+" + TASK_LIST_MARKER;
	private static final String ORDERED_LIST_MARKER = "\\s*[0-9]+\\.\\s+" + TASK_LIST_MARKER;
	private static final String BLOCK_QUOTE_MARKER = "\\s*(?:>\\s*)+";
	private static final Pattern AUTO_INDENT_PATTERN = Pattern.compile(
			"(" + BULLET_LIST_MARKER + "|" + ORDERED_LIST_MARKER + "|" + BLOCK_QUOTE_MARKER + "|\\s+)(.*)");

	private final MarkdownEditorPane editor;
	private final MarkdownTextArea textArea;

	SmartEdit(MarkdownEditorPane editor, MarkdownTextArea textArea) {
		this.editor = editor;
		this.textArea = textArea;

		Nodes.addInputMap(textArea, sequence(
			consume(keyPressed(ENTER),							this::enterPressed),
			consume(keyPressed(TAB),							this::tabPressed),
			consume(keyPressed(TAB, SHIFT_DOWN),				this::shiftTabPressed),
			consume(keyPressed(BACK_SPACE),						this::backspacePressed),
			consume(keyPressed(D, SHORTCUT_DOWN),				this::deleteLine),
			consume(keyPressed(UP, ALT_DOWN),					this::moveLinesUp),
			consume(keyPressed(DOWN, ALT_DOWN),					this::moveLinesDown),
			consume(keyPressed(UP, SHORTCUT_DOWN, ALT_DOWN),	this::duplicateLinesUp),
			consume(keyPressed(DOWN, SHORTCUT_DOWN, ALT_DOWN),	this::duplicateLinesDown)
		));

//		textArea.selectionProperty().addListener((ob, o, n) ->
//			System.out.println(findNodes(n.getStart(), n.getEnd(), (s, e, node) -> true, true)));

		editor.markdownASTProperty().addListener((ob, o, n) -> updateStateProperties());
		textArea.selectionProperty().addListener((ob, o, n) -> updateStateProperties());
	}

	//---- properties ---------------------------------------------------------

	private boolean updateStatePropertiesRunLaterPending;
	private void updateStateProperties() {
		// avoid too many (and useless) runLater() invocations
		if (updateStatePropertiesRunLaterPending)
			return;
		updateStatePropertiesRunLaterPending = true;

		Platform.runLater(() -> {
			updateStatePropertiesRunLaterPending = false;

			List<Node> nodesAtSelection = findNodesAtSelection((s, e, n) -> true, true, false);

			boolean bold = false;
 			boolean italic = false;
 			boolean code = false;
 			boolean link = false;
 			boolean image = false;
 			boolean unorderedList = false;
 			boolean orderedList = false;
 			boolean blockquote = false;
 			boolean fencedCode = false;
 			boolean header = false;
			for (Node node : nodesAtSelection) {
				if (!bold && node instanceof StrongEmphasis)
					bold = true;
				else if (!italic && node instanceof Emphasis)
					italic = true;
				else if (!code && node instanceof Code)
					code = true;
				else if (!link && (node instanceof Link || node instanceof LinkRef))
					link = true;
				else if (!image && (node instanceof Image || node instanceof ImageRef))
					image = true;
				else if (!unorderedList && node instanceof BulletListItem)
					unorderedList = true;
				else if (!orderedList && node instanceof OrderedListItem)
					orderedList = true;
				else if (!blockquote && node instanceof BlockQuote)
					blockquote = true;
				else if (!fencedCode && node instanceof FencedCodeBlock)
					fencedCode = true;
				else if (!header && node instanceof Heading)
					header = true;
			}
			this.bold.set(bold);
			this.italic.set(italic);
			this.code.set(code);
			this.link.set(link);
			this.image.set(image);
			this.unorderedList.set(unorderedList);
			this.orderedList.set(orderedList);
			this.blockquote.set(blockquote);
			this.fencedCode.set(fencedCode);
			this.header.set(header);
		});
	}

	private final BooleanProperty bold = new SimpleBooleanProperty();
	public BooleanProperty boldProperty() { return bold; }

	private final BooleanProperty italic = new SimpleBooleanProperty();
	public BooleanProperty italicProperty() { return italic; }

	private final BooleanProperty code = new SimpleBooleanProperty();
	public BooleanProperty codeProperty() { return code; }

	private final BooleanProperty link = new SimpleBooleanProperty();
	public BooleanProperty linkProperty() { return link; }

	private final BooleanProperty image = new SimpleBooleanProperty();
	public BooleanProperty imageProperty() { return image; }

	private final BooleanProperty unorderedList = new SimpleBooleanProperty();
	public BooleanProperty unorderedListProperty() { return unorderedList; }

	private final BooleanProperty orderedList = new SimpleBooleanProperty();
	public BooleanProperty orderedListProperty() { return orderedList; }

	private final BooleanProperty blockquote = new SimpleBooleanProperty();
	public BooleanProperty blockquoteProperty() { return blockquote; }

	private final BooleanProperty fencedCode = new SimpleBooleanProperty();
	public BooleanProperty fencedCodeProperty() { return fencedCode; }

	private final BooleanProperty header = new SimpleBooleanProperty();
	public BooleanProperty headerProperty() { return header; }

	//---- enter  -------------------------------------------------------------

	private void enterPressed(KeyEvent e) {
		String currentLine = textArea.getText(textArea.getCurrentParagraph());

		String newText = "\n";
		Matcher matcher = AUTO_INDENT_PATTERN.matcher(currentLine);
		if (matcher.matches()) {
			if (!matcher.group(2).isEmpty()) {
				// indent new line with same whitespace characters and auto-indentable markers as current line
				String indent = matcher.group(1);
				int caretColumn = textArea.getCaretColumn();
				if (caretColumn >= indent.length())
					newText = newText.concat(indent);
				else if (caretColumn > 0)
					newText = newText.concat(indent.substring(0, caretColumn));
			} else {
				// current line contains only whitespace characters and auto-indentable markers
				// --> empty current line
				int caretPosition = textArea.getCaretPosition();
				selectRange(textArea, caretPosition - currentLine.length(), caretPosition);
			}
		}

		// Note: not using replaceSelection(MarkdownTextArea, String) to allow undo merging in this case
		textArea.replaceSelection(newText);
		textArea.requestFollowCaret();
	}

	//---- indent -------------------------------------------------------------

	private void tabPressed(KeyEvent e) {
		List<Node> nodes;
		if (!(nodes = findIndentableNodesAtSelection()).isEmpty())
			indentNodes(nodes, true);
		else if (isIndentSelection())
			indentSelectedLines(true);
		else {
			// Note: not using replaceSelection(MarkdownTextArea, String) to allow undo merging in this case
			textArea.replaceSelection("\t");
			textArea.requestFollowCaret();
		}
	}

	private void shiftTabPressed(KeyEvent e) {
		List<Node> nodes;
		if (!(nodes = findIndentableNodesAtSelection()).isEmpty())
			indentNodes(nodes, false);
		else
			indentSelectedLines(false);
	}

	private void backspacePressed(KeyEvent e) {
		IndexRange selection = textArea.getSelection();
		int start = selection.getStart();
		int end = selection.getEnd();
		if (selection.getLength() > 0) {
			// selection is not empty --> delete selected text
			deleteText(textArea, start, end);
		} else {
			// selection is empty
			int startLine = offsetToLine(start);
			int startLineOffset = lineToStartOffset(startLine);
			if (start > startLineOffset && textArea.getText(startLineOffset, start).trim().isEmpty()) {
				// selection is empty and caret is in leading whitespace of a line,
				// but not at the beginning of a line --> unindent line
				indentSelectedLines(false);
			} else {
				String line = textArea.getText(startLine);
				int startLineEndOffset = startLineOffset + line.length();
				Matcher matcher = (start == startLineEndOffset) ? AUTO_INDENT_PATTERN.matcher(line) : null;
				if (matcher != null && matcher.matches() && matcher.group(2).isEmpty()) {
					// caret is at end of line and line contains only whitespace characters
					// and auto-indentable markers --> empty line
					deleteText(textArea, startLineOffset, startLineEndOffset);
				} else if (start > 0) {
					// delete character before caret
					deleteText(textArea, start - 1, start);
				}
			}
		}
	}

	/**
	 * Returns whether an indent operation should used for the selection.
	 *
	 * Returns true if:
	 *  - selection spans multiple lines
	 *  - selection is empty and caret is in leading whitespace of a line
	 *  - a single line is completely selected (excluding line separator)
	 */
	private boolean isIndentSelection() {
		IndexRange selection = textArea.getSelection();
		int start = selection.getStart();
		int startLine = offsetToLine(start);
		if (selection.getLength() == 0)
			return textArea.getText(lineToStartOffset(startLine), start).trim().isEmpty();
		else {
			int end = selection.getEnd();
			int endLine = offsetToLine(end);
			return endLine > startLine ||
				   lineToStartOffset(startLine) == start && lineToEndOffset(startLine) == end;
		}
	}

	private void indentSelectedLines(boolean right) {
		IndexRange range = getSelectedLinesRange(false);
		String str = textArea.getText(range.getStart(), range.getEnd());
		String newStr = indentText(str, right);

		IndentSelection isel = rememberIndentSelection();
		replaceText(textArea, range.getStart(), range.getEnd(), newStr);
		selectAfterIndent(isel);
	}

	private String indentText(String str, boolean right) {
		String[] lines = str.split("\n");

		String indent = "    ";
		StringBuilder buf = new StringBuilder(str.length() + (right ? (indent.length() * lines.length) : 0));
		for (int i = 0; i < lines.length; i++) {
			if (i > 0)
				buf.append('\n');

			String line = lines[i];
			if (right) {
				if (line.isEmpty() && lines.length > 1)
					continue; // do not indent empty lines if multiple lines are selected

				buf.append(indent).append(line);
			} else {
				int j = 0;
				while (j < line.length() && j < 4 && Character.isWhitespace(line.charAt(j)))
					j++;
				buf.append(line.substring(j));
			}
		}

		// String.split("\n") ignores '\n' at the end of the string --> append '\n' to result
		if (str.endsWith("\n"))
			buf.append('\n');

		return buf.toString();
	}

	/**
	 * Experiment: indent whole list items (including sub lists)
	 *
	 * Disabled because the user experience is not that good
	 * and it is questionable whether this feature makes sense at all.
	 */
	private final boolean indentNodes = false;

	private List<Node> findIndentableNodesAtSelection() {
		if (!indentNodes)
			return Collections.emptyList();

		return findNodesAtSelectedLines((start, end, node) -> {
			if (!(node instanceof ListItem))
				return false;

			// match only if one non-ListBlock child is in range
			for (Node child : node.getChildren()) {
				if (isInNode(start, end, child) && !(child instanceof ListBlock))
					return true;
			}
			return false;
		}, false, false);
	}

	private void indentNodes(List<Node> nodes, boolean right) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < nodes.size(); i++) {
			Node node = nodes.get(i);
			if (i > 0)
				buf.append(textArea.getText(nodes.get(i - 1).getEndOffset(), node.getStartOffset()));

			// indent list items
			if (node instanceof ListItem) {
				String str = node.getChars().toString();
				str = indentText(str, right);
				buf.append(str);
			}
		}

		int start = nodes.get(0).getStartOffset();
		int end = nodes.get(nodes.size() - 1).getEndOffset();

		IndentSelection isel = rememberIndentSelection();
		replaceText(textArea, start, end, buf.toString());
		selectAfterIndent(isel);
	}

	private static class IndentSelection {
		int startLine;
		int endLine;
		int startOffsetFromEnd;
		int endOffsetFromEnd;
	}

	private IndentSelection rememberIndentSelection() {
		IndentSelection isel = new IndentSelection();

		IndexRange selection = textArea.getSelection();
		int start = selection.getStart();
		int end = selection.getEnd();
		isel.startLine = offsetToLine(start);
		isel.endLine = offsetToLine(end);
		isel.startOffsetFromEnd = (start == end || start - lineToStartOffset(isel.startLine) > 0)
				? lineToEndOffset(isel.startLine) - start
				: -1; // beginning of line
		isel.endOffsetFromEnd = lineToEndOffset(isel.endLine) - end;

		return isel;
	}

	private void selectAfterIndent(IndentSelection isel) {
		int start = (isel.startOffsetFromEnd != -1)
				? Math.max(lineToEndOffset(isel.startLine) - isel.startOffsetFromEnd, lineToStartOffset(isel.startLine))
				: lineToStartOffset(isel.startLine);
		int end = Math.max(lineToEndOffset(isel.endLine) - isel.endOffsetFromEnd, lineToStartOffset(isel.endLine));
		selectRange(textArea, start, end);
	}

	//---- delete -------------------------------------------------------------

	private void deleteLine(KeyEvent e) {
		IndexRange selRange = getSelectedLinesRange(true);
		deleteText(textArea, selRange.getStart(), selRange.getEnd());
	}

	//---- move lines ---------------------------------------------------------

	private void moveLinesUp(KeyEvent e) {
		IndexRange selRange = getSelectedLinesRange(true);
		int selStart = selRange.getStart();
		int selEnd = selRange.getEnd();
		if (selStart == 0)
			return;

		int before = offsetToLine(selStart - 1);
		IndexRange beforeRange = linesToRange(before, before, true);
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
		replaceText(textArea, beforeStart, selEnd, selText + beforeText);
		selectRange(textArea, beforeStart, beforeStart + selText.length() - 1);
	}

	private void moveLinesDown(KeyEvent e) {
		IndexRange selRange = getSelectedLinesRange(true);
		int selStart = selRange.getStart();
		int selEnd = selRange.getEnd();
		if (selEnd == textArea.getLength())
			return;

		int after = offsetToLine(selEnd + 1);
		IndexRange afterRange = linesToRange(after, after, true);
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
		replaceText(textArea, selStart, afterEnd, afterText + selText);

		int newSelStart = selStart + afterText.length();
		int newSelEnd = newSelStart + selText.length();
		if (selText.endsWith("\n"))
			newSelEnd--;
		selectRange(textArea, newSelStart, newSelEnd);
	}

	//---- duplicate lines ----------------------------------------------------

	private void duplicateLinesUp(KeyEvent e) {
		duplicateLines(true);
	}

	private void duplicateLinesDown(KeyEvent e) {
		duplicateLines(false);
	}

	private void duplicateLines(boolean up) {
		IndexRange selRange = getSelectedLinesRange(true);
		int selStart = selRange.getStart();
		int selEnd = selRange.getEnd();

		String selText = textArea.getText(selStart, selEnd);
		if (!selText.endsWith("\n"))
			selText += "\n";

		replaceText(textArea, selStart, selStart, selText);

		if (up)
			selectRange(textArea, selStart, selStart + selText.length() - 1);
		else {
			int newSelStart = selStart + selText.length();
			int newSelEnd = newSelStart + selText.length();
			if (selText.endsWith("\n"))
				newSelEnd--;
			selectRange(textArea, newSelStart, newSelEnd);
		}
	}

	//---- surround -----------------------------------------------------------

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

		// replace text and update selection
		replaceText(textArea, start, end, leading + trimmedSelectedText + trailing);
		selectRange(textArea, selStart, selEnd);
	}

	private void surroundSelectionAndReplaceMarker(String leading, String trailing, String hint,
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

		// insert hint text if selection is empty
		if (hint != null && trimmedSelectedText.isEmpty()) {
			trimmedSelectedText = hint;
			selEnd = selStart + hint.length();
		}

		// replace text and update selection
		// Note: using single textArea.replaceText() to avoid multiple changes in undo history
		String before = textArea.getText(openingMarker.getEndOffset(), start);
		String after = textArea.getText(end, closingMarker.getStartOffset());
		replaceText(textArea, openingMarker.getStartOffset(), closingMarker.getEndOffset(),
				newOpeningMarker + before + leading + trimmedSelectedText + trailing + after + newClosingMarker );
		selectRange(textArea, selStart, selEnd);
	}

	private void surroundSelectionInCode(String openCloseMarker, String hint) {
		Code codeNode = findNodeAtSelection((s, e, n) -> n instanceof Code);
		if (codeNode != null)
			surroundSelectionAndReplaceMarker(openCloseMarker, openCloseMarker, hint, codeNode, "<code>", "</code>");
		else
			surroundSelection(openCloseMarker, openCloseMarker, hint);
	}

	//---- delimited inlines --------------------------------------------------

	public void insertBold(String hint) {
		insertDelimited(StrongEmphasis.class, Options.getStrongEmphasisMarker(), hint);
	}

	public void insertItalic(String hint) {
		insertDelimited(Emphasis.class, Options.getEmphasisMarker(), hint);
	}

	public void insertStrikethrough(String hint) {
		insertDelimited(Strikethrough.class, "~~", hint);
	}

	public void insertInlineCode(String hint) {
		insertDelimited(Code.class, "`", hint);
	}

	private void insertDelimited(Class<? extends Node> cls, String openCloseMarker, String hint) {
		List<? extends Node> nodes = findNodesAtSelection((s, e, n) -> cls.isInstance(n), false, false);
		if (nodes.size() > 0) {
			// there is delimited text in current selection --> change them to plain text
			if (nodes.size() == 1 && hint.equals(((DelimitedNode)nodes.get(0)).getText().toString())) {
				// delete node including hint text
				Node node = nodes.get(0);
				deleteText(textArea, node.getStartOffset(), node.getEndOffset());
			} else
				removeDelimiters(nodes);
		} else
			surroundSelectionInCode(openCloseMarker, hint);
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
		replaceText(textArea, start, end, buf.toString());
		selectRange(textArea, start, start + buf.length());
	}

	//---- links --------------------------------------------------------------

	public void insertLink() {
		LinkNode linkNode = findNodeAtSelection((s, e, n) -> n instanceof LinkNode);
		if (linkNode != null && !(linkNode instanceof Link) && !(linkNode instanceof AutoLink) && !(linkNode instanceof MailLink)) {
			// link node at caret is not supported --> insert link before or after
			if (textArea.getCaretPosition() != linkNode.getStartOffset())
				selectRange(textArea, linkNode.getEndOffset(), linkNode.getEndOffset());
			linkNode = null;
		}

		if (linkNode != null)
			selectRange(textArea, linkNode.getStartOffset(), linkNode.getEndOffset());

		LinkDialog dialog = new LinkDialog(editor.getNode().getScene().getWindow(), editor.getParentPath());
		if (linkNode instanceof Link) {
			Link link = (Link) linkNode;
			dialog.init(link.getUrl().toString(), link.getText().toString(), link.getTitle().toString());
		} else if (linkNode instanceof AutoLink)
			dialog.init(((AutoLink) linkNode).getText().toString(), "", "");
		else if (linkNode instanceof MailLink)
			dialog.init(((MailLink) linkNode).getText().toString(), "", "");

		LinkNode linkNode2 = linkNode;
		dialog.showAndWait().ifPresent(result -> {
			if (linkNode2 != null)
				replaceText(textArea, linkNode2.getStartOffset(), linkNode2.getEndOffset(), result);
			else
				replaceSelection(textArea, result);
		});
	}

	public void insertImage() {
		LinkNode linkNode = findNodeAtSelection((s, e, n) -> n instanceof LinkNode);
		if (linkNode != null && !(linkNode instanceof Image)) {
			// link node at caret is not supported --> insert image before or after
			if (textArea.getCaretPosition() != linkNode.getStartOffset())
				selectRange(textArea, linkNode.getEndOffset(), linkNode.getEndOffset());
			linkNode = null;
		}

		Image image = (Image) linkNode;
		if (image != null)
			selectRange(textArea, image.getStartOffset(), image.getEndOffset());

		ImageDialog dialog = new ImageDialog(editor.getNode().getScene().getWindow(), editor.getParentPath());
		if (image != null)
			dialog.init(image.getUrl().toString(), image.getText().toString(), image.getTitle().toString());
		dialog.showAndWait().ifPresent(result -> {
			if (image != null)
				replaceText(textArea, image.getStartOffset(), image.getEndOffset(), result);
			else
				replaceSelection(textArea, result);
		});
	}

	//---- heading ------------------------------------------------------------

	public void insertHeading(int level, String hint) {
		int caretPosition = textArea.getCaretPosition();
		Heading heading = findNodeAtLine(caretPosition, (s, e, n) -> n instanceof Heading);
		if (heading != null) {
			// there is already a heading at current line --> remove heading or change level
			if (level == heading.getLevel()) {
				// same heading level --> remove heading
				if (heading.isAtxHeading())
					deleteText(textArea, heading.getOpeningMarker().getStartOffset(), heading.getText().getStartOffset());
				else if (heading.isSetextHeading())
					deleteText(textArea, heading.getText().getEndOffset(), heading.getClosingMarker().getEndOffset());
			} else {
				// different heading level --> change heading level
				if (heading.isAtxHeading()) {
					// replace ATX opening marker
					String marker = StringUtils.repeat('#', level);
					BasedSequence openingMarker = heading.getOpeningMarker();
					replaceText(textArea, openingMarker.getStartOffset(), openingMarker.getEndOffset(), marker);

					// move caret to end of line
					selectEndOfLine(openingMarker.getStartOffset());
				} else if (heading.isSetextHeading()) {
					BasedSequence closingMarker = heading.getClosingMarker();
					if (level > 2) {
						// new level too large for setext --> change from setext to ATX header
						// Note: using single textArea.replaceText() to avoid multiple changes in undo history
						String newHeading = StringUtils.repeat('#', level) + " " + heading.getText();
						replaceText(textArea, heading.getStartOffset(), heading.getEndOffset(), newHeading);
					} else {
						// replace setext closing marker
						String marker = StringUtils.repeat(level == 1 ? '=' : '-', closingMarker.length());
						replaceText(textArea, closingMarker.getStartOffset(), closingMarker.getEndOffset(), marker);
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
				replaceText(textArea, lineStartOffset, lineStartOffset + currentLine.length(), marker + " " + hint);

				// select hint
				int selStart = lineStartOffset + marker.length() + 1;
				int selEnd = selStart + hint.length();
				selectRange(textArea, selStart, selEnd);
			} else {
				// current line contains text --> insert opening marker
				if (!currentLine.startsWith(" "))
					marker += " ";
				insertText(textArea, lineStartOffset, marker);

				// move caret to end of line
				selectEndOfLine(lineStartOffset);
			}
		}
	}

	//---- lists --------------------------------------------------------------

	public void insertUnorderedList() {
		surroundSelection("\n\n" + Options.getBulletListMarker() + " ", "");
	}

	//---- text modification --------------------------------------------------

	/**
	 * Central method to replace text in editor that prevents undo merging.
	 */
	static void replaceText(MarkdownTextArea textArea, int start, int end, String text) {
		// prevent undo merging with previous text entered by user
		textArea.getUndoManager().preventMerge();

		// replace text
		textArea.replaceText(start, end, text);

		// textArea.replaceText() moves the caret to the end of the selected text, which may
		// it make necessary to scroll if large text is inserted and selectRange() is not called
		textArea.requestFollowCaret();

		// prevent undo merging with following text entered by user
		textArea.getUndoManager().preventMerge();
	}

	static void replaceSelection(MarkdownTextArea textArea, String replacement) {
		IndexRange range = textArea.getSelection();
		replaceText(textArea, range.getStart(), range.getEnd(), replacement);
	}

	static void insertText(MarkdownTextArea textArea, int index, String text) {
		replaceText(textArea, index, index, text);
	}

	static void deleteText(MarkdownTextArea textArea, int start, int end) {
		replaceText(textArea, start, end, "");
	}

	//---- text selection -----------------------------------------------------

	/**
	 * Central method to select text in editor that scrolls to the caret.
	 */
	static void selectRange(MarkdownTextArea textArea, int anchor, int caretPosition) {
		textArea.selectRange(anchor, caretPosition);
		textArea.requestFollowCaret();
	}

	/**
	 * Moves the caret to the end of the line.
	 */
	private void selectEndOfLine(int offsetInLine) {
		int line = offsetToLine(offsetInLine);
		int caretPos = lineToEndOffset(line);
		selectRange(textArea, caretPos, caretPos);
	}

	//---- find nodes ---------------------------------------------------------

	/**
	 * Find single node that completely encloses the current selection and match a predicate.
	 */
	private <T extends Node> T findNodeAtSelection(FindNodePredicate predicate) {
		IndexRange selection = textArea.getSelection();
		int start = selection.getStart();
		int end = selection.getEnd();
		List<T> nodes = findNodes(start, end, predicate, false, false);
		if (nodes.size() != 1)
			return null;

		T node = nodes.get(0);
		BasedSequence text = (node instanceof DelimitedNode) ? ((DelimitedNode)node).getText() : node.getChars();
		return (start >= text.getStartOffset() && end <= text.getEndOffset()) ? node : null;
	}

	/**
	 * Find all nodes that are within the current selection and match a predicate.
	 */
	private <T> List<T> findNodesAtSelection(FindNodePredicate predicate, boolean allowNested, boolean deepest) {
		IndexRange selection = textArea.getSelection();
		return findNodes(selection.getStart(), selection.getEnd(), predicate, allowNested, deepest);
	}

	/**
	 * Find all nodes that are within the current (partly) selected line(s) and match a predicate.
	 */
	private <T> List<T> findNodesAtSelectedLines(FindNodePredicate predicate, boolean allowNested, boolean deepest) {
		IndexRange selRange = getSelectedLinesRange(false);
		return findNodes(selRange.getStart(), selRange.getEnd(), predicate, allowNested, deepest);
	}

	/**
	 * Find all nodes that are within the given range and match a predicate.
	 */
	private <T> List<T> findNodes(int start, int end, FindNodePredicate predicate, boolean allowNested, boolean deepest) {
		Node markdownAST = editor.getMarkdownAST();
		if (markdownAST == null)
			return Collections.emptyList();

		ArrayList<T> nodes = new ArrayList<>();
		NodeVisitor visitor = new NodeVisitor(Collections.emptyList()) {
			@SuppressWarnings("unchecked")
			@Override
			public void visit(Node node) {
				if (isInNode(start, end, node) && predicate.test(start, end, node)) {
					if (deepest) {
						int oldNodesSize = nodes.size();
						visitChildren(node);

						// add only if no other child was added
						if (nodes.size() == oldNodesSize)
							nodes.add((T) node);
						return;
					}

					nodes.add((T) node);

					if (!allowNested)
						return; // do not visit children
				}

				visitChildren(node);
			}
		};
		visitor.visit(markdownAST);
		return nodes;
	}

	private interface FindNodePredicate {
	    boolean test(int start, int end, Node node);
	}

	private boolean isInNode(int start, int end, Node node) {
		if (end == start)
			end++;
		return start < node.getEndOffset() && end > node.getStartOffset();
	}

	/**
	 * Find first node that is at the given offset and match a predicate.
	 */
	@SuppressWarnings("unused")
	private <T> T findNodeAt(int offset, FindNodePredicate predicate) {
		List<T> nodes = findNodes(offset, offset, predicate, false, false);
		return nodes.size() > 0 ? nodes.get(0) : null;
	}

	private <T> T findNodeAtLine(int offsetInLine, FindNodePredicate predicate) {
		int line = offsetToLine(offsetInLine);
		int lineStart = lineToStartOffset(line);
		int lineEnd = lineToEndOffset(line);
		List<T> nodes = findNodes(lineStart, lineEnd, predicate, false, false);
		return nodes.size() > 0 ? nodes.get(0) : null;
	}

	//---- offset/line conversion -------------------------------------------

	/**
	 * Returns the line indices of the first and last line that are (partly) selected.
	 */
	private IndexRange getSelectedLines() {
		IndexRange selection = textArea.getSelection();
		int start = selection.getStart();
		int end = Math.max(selection.getEnd() - 1, start); // excluding line separator
		return new IndexRange(offsetToLine(start), offsetToLine(end));
	}

	/**
	 * Returns start and end character offsets of the lines that are (partly) selected.
	 * The end offset includes the line separator if includeLastLineSeparator is true.
	 */
	private IndexRange getSelectedLinesRange(boolean includeLastLineSeparator) {
		IndexRange selection = getSelectedLines();
		return linesToRange(selection.getStart(), selection.getEnd(), includeLastLineSeparator);
	}

	/**
	 * Returns start and end character offsets of the given lines range.
	 * The end offset includes the line separator if includeLastLineSeparator is true.
	 */
	private IndexRange linesToRange(int firstLine, int lastLine, boolean includeLastLineSeparator) {
		int start = lineToStartOffset(firstLine);
		int end = lineToEndOffset(lastLine);
		if (includeLastLineSeparator && end < textArea.getLength())
			end++; // line separator

		return new IndexRange(start, end);
	}

	/**
	 * Returns the start offset of the given line.
	 */
	private int lineToStartOffset(int line) {
		return textArea.getAbsolutePosition(line, 0);
	}

	/**
	 * Returns the end offset (excluding line separator) of the given line.
	 */
	private int lineToEndOffset(int line) {
		return lineToStartOffset(line) + textArea.getParagraph(line).length();
	}

	/**
	 * Returns the line (paragraph) index for the given character offset.
	 */
	private int offsetToLine(int offset) {
		return textArea.offsetToPosition(offset, Bias.Forward).getMajor();
	}
}
