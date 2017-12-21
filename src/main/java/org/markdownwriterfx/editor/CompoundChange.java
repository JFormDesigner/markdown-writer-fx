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

import java.util.function.Consumer;
import org.fxmisc.richtext.GenericStyledArea;

/**
 * Helper that combines multiple changes to a rich text area into a single change.
 * This is necessary to keep undo history clean.
 *
 * Usage:
 * <pre>
 *     CompoundChange.run(textArea, changer -> {
 *         changer.replaceText(0, 10, "abc");
 *         changer.replaceText(20, 20, "123");
 *         changer.replaceText(20, 20, "123");
 *     } );
 * </pre>
 *
 * @author Karl Tauber
 */
public class CompoundChange
	implements CharSequence
{
	private final String originalText;
	private StringBuilder newText;

	public static void run(GenericStyledArea<?, ?, ?> textArea, Consumer<CompoundChange> changer) {
		CompoundChange compoundChange = new CompoundChange(textArea.getText());
		changer.accept(compoundChange);

		if (!compoundChange.hasChanged())
			return;

		// prevent undo merging with previous text entered by user
		textArea.getUndoManager().preventMerge();

		// replace text
		compoundChange.applyChanges(textArea);

		// textArea.replaceText() moves the caret to the end of the selected text, which may
		// it make necessary to scroll if large text is inserted and selectRange() is not called
		textArea.requestFollowCaret();

		// prevent undo merging with following text entered by user
		textArea.getUndoManager().preventMerge();
	}

	private CompoundChange(String text) {
		this.originalText = text;
	}

	private boolean hasChanged() {
		return newText != null;
	}

	private void applyChanges(GenericStyledArea<?, ?, ?> textArea) {
		assert textArea.getText().equals(originalText);

		int start = 0;
		int end = originalText.length();
		int newEnd = newText.length();

		// trim leading equal characters
		while (start < newEnd && start < end) {
			if (newText.charAt(start) != originalText.charAt(start))
				break;
			start++;
		}

		// trim trailing equal characters
		while (newEnd > start && end > start) {
			if (newText.charAt(newEnd - 1) != originalText.charAt(end - 1))
				break;
			newEnd--;
			end--;
		}

		textArea.replaceText(start, end, newText.substring(start, newEnd));
	}

	@Override
	public int length() {
		return (newText != null) ? newText.length() : originalText.length();
	}

	@Override
	public char charAt(int index) {
		return (newText != null) ? newText.charAt(index) : originalText.charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return (newText != null) ? newText.subSequence(start, end) : originalText.subSequence(start, end);
	}

	public String getText() {
		return (newText != null) ? newText.toString() : originalText;
	}

	public void replaceText(int start, int end, String text) {
		if (newText == null)
			newText = new StringBuilder(originalText);

		newText.replace(start, end, text);
	}

	@Override
	public String toString() {
		return getText();
	}
}
