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
import javafx.scene.Node;
import javafx.scene.control.Button;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.StyledText;
import org.reactfx.util.Either;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.NodeVisitor;

/**
 * @author Karl Tauber
 */
class EmbeddedImage
{
	final String text;
	final Collection<String> style;

	EmbeddedImage(String text, Collection<String> style) {
		this.text = text;
		this.style = style;
	}

	Node createNode() {
		//TODO
		return new Button(text);
	}

	static void replaceImageSegments(MarkdownTextArea textArea, com.vladsch.flexmark.ast.Node astRoot) {
		NodeVisitor visitor = new NodeVisitor(Collections.emptyList()) {
			@Override
			public void visit(com.vladsch.flexmark.ast.Node node) {
				if (node instanceof Image) {
					String text = textArea.getText(node.getStartOffset(), node.getEndOffset());
					ReadOnlyStyledDocument<Collection<String>, Either<StyledText<Collection<String>>, EmbeddedImage>, Collection<String>> doc
						= ReadOnlyStyledDocument.fromSegment(Either.right(new EmbeddedImage(text, null)),
							Collections.<String>emptyList(), Collections.<String>emptyList(), textArea.getSegOps());
					textArea.replace(node.getStartOffset(), node.getEndOffset(), doc);
				} else
					visitChildren(node);
			}
		};
		visitor.visit(astRoot);
	}
}
