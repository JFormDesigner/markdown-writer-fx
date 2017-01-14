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

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.StyledText;
import org.reactfx.util.Either;
import com.vladsch.flexmark.ast.NodeVisitor;

/**
 * @author Karl Tauber
 */
class EmbeddedImage
{
	private static final int MAX_SIZE = 200;
	private static final int ERROR_SIZE = 16;

	final Path basePath;
	final com.vladsch.flexmark.ast.Image node;
	final String text;
	final Collection<String> style;

	EmbeddedImage(Path basePath, com.vladsch.flexmark.ast.Image node, String text, Collection<String> style) {
		this.basePath = basePath;
		this.node = node;
		this.text = text;
		this.style = style;
	}

	Node createNode() {
		String url = node.getUrl().toString();
		String imageUrl = (basePath != null)
				? basePath.resolve(url).toUri().toString()
				: "file:" + url;

		//TODO cache
		// load image
		Image image = new Image(imageUrl);

		if (image.isError()) {
			// loading failed
			Polyline errorView = new Polyline(
				0, 0,  ERROR_SIZE, 0,  ERROR_SIZE, ERROR_SIZE,  0, ERROR_SIZE,  0, 0,	// rectangle
				ERROR_SIZE, ERROR_SIZE,  0, ERROR_SIZE,  ERROR_SIZE, 0);				// cross
			errorView.setStroke(Color.RED); //TODO use CSS
			return errorView;
		}

		ImageView view = new ImageView(image);
		view.setPreserveRatio(true);
		view.setFitWidth(Math.min(image.getWidth(),MAX_SIZE));
		view.setFitHeight(Math.min(image.getHeight(),MAX_SIZE));
		return view;
	}

	static void replaceImageSegments(MarkdownTextArea textArea, com.vladsch.flexmark.ast.Node astRoot, Path basePath) {
		// remember current selection (because textArea.replace() changes selection)
		IndexRange selection = textArea.getSelection();

		// replace first character of image markup with an EmbeddedImage object
		HashSet<EmbeddedImage> addedImages = new HashSet<>();
		NodeVisitor visitor = new NodeVisitor(Collections.emptyList()) {
			@Override
			public void visit(com.vladsch.flexmark.ast.Node node) {
				//TODO support ImageRef
				if (node instanceof com.vladsch.flexmark.ast.Image) {
					int start = node.getStartOffset();
					int end = start + 1;

					EmbeddedImage embeddedImage = new EmbeddedImage(basePath,
							(com.vladsch.flexmark.ast.Image) node, textArea.getText(start, end), null);
					addedImages.add(embeddedImage);

					textArea.replace(start, end, ReadOnlyStyledDocument.fromSegment(
							Either.right(embeddedImage),
							Collections.<String>emptyList(),
							Collections.<String>emptyList(),
							textArea.getSegOps()));
				} else
					visitChildren(node);
			}
		};
		visitor.visit(astRoot);

		// remove obsolete EmbeddedImage objects
		HashMap<Integer, String> removedImages = new HashMap<>();
		int index = 0;
		for (Paragraph<Collection<String>, Either<StyledText<Collection<String>>, EmbeddedImage>, Collection<String>> par : textArea.getDocument().getParagraphs()) {
			for (Either<StyledText<Collection<String>>, EmbeddedImage> seg : par.getSegments()) {
				if (seg.isRight() && !addedImages.contains(seg.getRight()))
					removedImages.put(index, seg.getRight().text);

				index += seg.isLeft() ? seg.getLeft().getText().length() : seg.getRight().text.length();
			}
			index++;
		}
		for (Map.Entry<Integer, String> e : removedImages.entrySet()) {
			int start = e.getKey();
			String text = e.getValue();
			textArea.replaceText(start, start + text.length(), text);
		}

		// restore selection
		if (!selection.equals(textArea.getSelection()))
			textArea.selectRange(selection.getStart(), selection.getEnd());
	}
}
