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
import java.util.function.BiConsumer;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.TextExt;
import org.fxmisc.richtext.model.SegmentOps;
import org.fxmisc.richtext.model.StyledText;
import org.reactfx.util.Either;

/**
 * Markdown text area.
 *
 * @author Karl Tauber
 */
class MarkdownTextArea
	extends GenericStyledArea<Collection<String>, Either<StyledText<Collection<String>>, EmbeddedImage>, Collection<String>>
{
	public MarkdownTextArea() {
		super(
			/* initialParagraphStyle */ Collections.<String>emptyList(),
			/* applyParagraphStyle */ (paragraph, styleClasses) -> paragraph.getStyleClass().addAll(styleClasses),
			/* initialTextStyle */ Collections.<String>emptyList(),
			/* textOps */ StyledText.<Collection<String>>textOps()._or(new EmbeddedImageOps()),
			/* preserveStyle */ false,
			/* nodeFactory */ seg -> createNode(seg, StyledText.textOps(),
				(text, styleClasses) -> text.getStyleClass().addAll(styleClasses))
			);
	}

	private static Node createNode(Either<StyledText<Collection<String>>, EmbeddedImage> seg,
			SegmentOps<StyledText<Collection<String>>, Collection<String>> textSegOps,
			BiConsumer<? super TextExt, Collection<String>> applyStyle)
	{
		return seg.isLeft()
			? StyledTextArea.createStyledTextNode(seg.getLeft(), textSegOps, applyStyle)
			: seg.getRight().createNode();

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
