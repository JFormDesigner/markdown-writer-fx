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

import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.StyleClassedTextArea;

/**
 * Markdown text area.
 *
 * @author Karl Tauber
 */
class MarkdownTextArea
	extends StyleClassedTextArea
{
	public MarkdownTextArea() {
		super(false);
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


	private IndexRange selectLineIfEmpty() {
		IndexRange oldSelection = null;
		if (getSelectedText().isEmpty()) {
			oldSelection = getSelection();
			selectLine();
			nextChar(SelectionPolicy.ADJUST);
		}
		return oldSelection;
	}
}
