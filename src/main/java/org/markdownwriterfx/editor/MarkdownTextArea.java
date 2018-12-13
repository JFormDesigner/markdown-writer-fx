/*
 * Copyright (c) 2017 Karl Tauber <karl at jformdesigner dot com>
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.TextExt;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.SegmentOps;
import org.fxmisc.richtext.model.StyledSegment;
import org.reactfx.util.Either;

/**
 * Markdown text area.
 *
 * @author Karl Tauber
 */
class MarkdownTextArea
	extends GenericStyledArea<Collection<String>, Either<String, EmbeddedImage>, Collection<String>>
{
	public MarkdownTextArea() {
		super(
			/* initialParagraphStyle */ Collections.<String>emptyList(),
			/* applyParagraphStyle */ (paragraph, styleClasses) -> paragraph.getStyleClass().addAll(styleClasses),
			/* initialTextStyle */ Collections.<String>emptyList(),
			/* textOps */ SegmentOps.<Collection<String>>styledTextOps()._or(new EmbeddedImageOps<Collection<String>>(), (s1, s2) -> Optional.empty()),
			/* preserveStyle */ false,
			/* nodeFactory */ seg -> createNode(seg,
				(text, styleClasses) -> text.getStyleClass().addAll(styleClasses))
			);
	}

	private static Node createNode(StyledSegment<Either<String, EmbeddedImage>, Collection<String>> seg,
			BiConsumer<? super TextExt, Collection<String>> applyStyle)
	{
		return seg.getSegment().unify(
				text -> StyledTextArea.createStyledTextNode(text, seg.getStyle(), applyStyle),
				EmbeddedImage::createNode);
	}

	@Override
	public void cut() {
		selectLineIfEmpty();
		super.cut();
	}

	@Override
	public void copy() {
		IndexRange oldSelection = selectLineIfEmpty();
		super.copy();
		if (oldSelection != null)
			selectRange(oldSelection.getStart(), oldSelection.getEnd());
	}

	@Override
	public void undo() {
		@SuppressWarnings("unchecked")
		List<PlainTextChange> nextUndo = (List<PlainTextChange>) getUndoManager().getNextUndo();
		if (nextUndo != null && !nextUndo.isEmpty()) {
			PlainTextChange change = findFirstChange(nextUndo);
			int selStart = change.getPosition();
			int selEnd = change.getRemovalEnd();

			super.undo();

			// select first change
			selectRange(Math.min(selStart, getLength()), Math.min(selEnd, getLength()));
		} else
			super.undo();
	}

	@Override
	public void redo() {
		@SuppressWarnings("unchecked")
		List<PlainTextChange> nextRedo = (List<PlainTextChange>) getUndoManager().getNextRedo();
		if (nextRedo != null && !nextRedo.isEmpty()) {
			PlainTextChange change = findFirstChange(nextRedo);
			int selStart = change.getPosition();
			int selEnd = change.getInsertionEnd();

			super.redo();

			// select first change
			selectRange(Math.min(selStart, getLength()), Math.min(selEnd, getLength()));
		} else
			super.redo();
	}

	private PlainTextChange findFirstChange(List<PlainTextChange> changes) {
		PlainTextChange firstChange = null;
		for (PlainTextChange change : changes) {
			if (firstChange == null || change.getPosition() < firstChange.getPosition())
				firstChange = change;
		}
		return firstChange;
	}

	private IndexRange selectLineIfEmpty() {
		IndexRange oldSelection = null;
		if (getSelectedText().isEmpty()) {
			oldSelection = getSelection();
			selectLine();
			nextChar(SelectionPolicy.ADJUST);
		}
		return oldSelection;
	}

	@Override
	public void wordBreaksForwards(int n, SelectionPolicy selectionPolicy) {
		super.wordBreaksForwards(n, selectionPolicy);

		// change behavior of Ctrl+RIGHT:
		//   old behavior: move caret to the end of the current word
		//   new behavior: move caret to the beginning of the next word
		String text = getText();
		int caretPosition = getCaretPosition();
		int newCaretPosition = caretPosition;
		for (int i = caretPosition; i < text.length(); i++) {
			char ch = text.charAt(i);
			if (ch == ' ' || ch == '\t')
				newCaretPosition++;
			else
				break;
		}
		if (newCaretPosition != caretPosition)
			moveTo(newCaretPosition, selectionPolicy);
	}

	@Override
	public void prevPage(SelectionPolicy selectionPolicy) {
		disableFollowCaret = true;

		// change behavior of PAGE_UP key:
		//   old behavior: move caret visible lines count up (scrolling depends on caret position)
		//   new behavior: scroll one page up and move caret visible lines count up
		try {
			int firstVisible = firstVisibleParToAllParIndex();
			int lastVisible = lastVisibleParToAllParIndex();
			int caretParagraph = getCurrentParagraph();
			int caretColumn = getCaretColumn();

			showParagraphAtBottom(firstVisible - 1);

			// TODO improve handling of wrapped lines and tabs
			int newCaretParagraph = Math.max(firstVisible - (lastVisible - caretParagraph), 0);
			if (caretParagraph == lastVisible && newCaretParagraph > 0)
				newCaretParagraph--;
			int newCaretColumn = Math.min(caretColumn, getParagraphLength(newCaretParagraph));
			moveTo(newCaretParagraph, newCaretColumn, selectionPolicy);
		} catch (AssertionError e) {
			// may be thrown in textArea.visibleParToAllParIndex()
			// occurs if the last line is empty and and the text fits into
			// the visible area (no vertical scroll bar shown)
			// --> ignore
			super.prevPage(selectionPolicy);
		}
	}

	@Override
	public void nextPage(SelectionPolicy selectionPolicy) {
		disableFollowCaret = true;

		// change behavior of PAGE_DOWN key:
		//   old behavior: move caret visible lines count down (scrolling depends on caret position)
		//   new behavior: scroll one page down and move caret visible lines count down
		try {
			int firstVisible = firstVisibleParToAllParIndex();
			int lastVisible = lastVisibleParToAllParIndex();
			int caretParagraph = getCurrentParagraph();
			int caretColumn = getCaretColumn();

			showParagraphAtTop(lastVisible);

			// TODO improve handling of wrapped lines and tabs
			int newCaretParagraph = Math.min(lastVisible + (caretParagraph - firstVisible), getParagraphs().size() - 1);
			int newCaretColumn = Math.min(caretColumn, getParagraphLength(newCaretParagraph));
			moveTo(newCaretParagraph, newCaretColumn, selectionPolicy);
		} catch (AssertionError e) {
			// may be thrown in textArea.visibleParToAllParIndex()
			// occurs if the last line is empty and and the text fits into
			// the visible area (no vertical scroll bar shown)
			// --> ignore
			super.nextPage(selectionPolicy);
		}
	}

	private boolean disableFollowCaret;

	@Override
	public void requestFollowCaret() {
		if (disableFollowCaret) {
			disableFollowCaret = false;
			requestLayout();
		} else
			super.requestFollowCaret();
	}
}
