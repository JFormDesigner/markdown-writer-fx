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

import java.lang.ref.SoftReference;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import javafx.scene.control.IndexRange;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.reactfx.util.Either;
import com.vladsch.flexmark.ast.ImageRef;
import com.vladsch.flexmark.ast.LinkNodeBase;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.Visitor;

/**
 * @author Karl Tauber
 */
class EmbeddedImage
{
	private static final int MAX_SIZE = 200;
	private static final int ERROR_SIZE = 16;

	private static final HashMap<String, SoftReference<Image>> imageCache = new HashMap<>();

	final Path basePath;
	final String url;
	final String text;

	EmbeddedImage(Path basePath, String url, String text) {
		this.basePath = basePath;
		this.url = url;
		this.text = text;
	}

	javafx.scene.Node createNode() {
		String imageUrl;
		try {
			imageUrl = (basePath != null)
				? basePath.resolve(url).toUri().toString()
				: "file:" + url;
		} catch (InvalidPathException ex) {
			return createErrorNode();
		}

		// load image
		Image image = loadImage(imageUrl);
		if (image.isError())
			return createErrorNode(); // loading failed

		// create image view
		ImageView view = new ImageView(image);
		view.setPreserveRatio(true);
		view.setFitWidth(Math.min(image.getWidth(),MAX_SIZE));
		view.setFitHeight(Math.min(image.getHeight(),MAX_SIZE));
		return view;
	}

	private javafx.scene.Node createErrorNode() {
		Polyline errorNode = new Polyline(
			0, 0,  ERROR_SIZE, 0,  ERROR_SIZE, ERROR_SIZE,  0, ERROR_SIZE,  0, 0,	// rectangle
			ERROR_SIZE, ERROR_SIZE,  0, ERROR_SIZE,  ERROR_SIZE, 0);				// cross
		errorNode.setStroke(Color.RED); //TODO use CSS
		return errorNode;
	}

	private static Image loadImage(String imageUrl) {
		cleanUpImageCache();

		Image image = null;
		SoftReference<Image> imageRef = imageCache.get(imageUrl);
		if (imageRef != null)
			image = imageRef.get();
		if (image == null) {
			image = new Image(imageUrl);
			imageCache.put(imageUrl, new SoftReference<>(image));
		}
		return image;
	}

	private static void cleanUpImageCache() {
		Iterator<SoftReference<Image>> it = imageCache.values().iterator();
		while (it.hasNext()) {
			if (it.next().get() == null)
				it.remove();
		}
	}

	static void replaceImageSegments(MarkdownTextArea textArea, Node astRoot, Path basePath) {
		// remember current selection (because textArea.replace() changes selection)
		IndexRange selection = textArea.getSelection();

		// replace first character of image markup with an EmbeddedImage object
		HashSet<EmbeddedImage> addedImages = new HashSet<>();
		NodeVisitor visitor = new NodeVisitor(Collections.emptyList()) {
			@Override
			protected void processNode(Node node, boolean withChildren, BiConsumer<Node, Visitor<Node>> processor) {
				if (node instanceof com.vladsch.flexmark.ast.Image ||
					node instanceof ImageRef)
				{
					LinkNodeBase linkNode = (node instanceof ImageRef)
						? ((ImageRef)node).getReferenceNode(astRoot.getDocument())
						: (com.vladsch.flexmark.ast.Image) node;
					if (linkNode == null)
						return; // reference not found

					String url = linkNode.getUrl().toString();
					if (url.startsWith("http:") || url.startsWith("https:"))
						return; // do not embed external images

					int start = node.getStartOffset();
					int end = start + 1;

					EmbeddedImage embeddedImage = new EmbeddedImage(basePath,
							url, textArea.getText(start, end));
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
		removeImageSegments(textArea, image -> !addedImages.contains(image));

		// restore selection
		if (!selection.equals(textArea.getSelection()))
			textArea.selectRange(selection.getStart(), selection.getEnd());
	}

	static void removeAllImageSegments(MarkdownTextArea textArea) {
		removeImageSegments(textArea, image -> true);
	}

	private static void removeImageSegments(MarkdownTextArea textArea, Predicate<EmbeddedImage> filter) {
		HashMap<Integer, String> removedImages = new HashMap<>();
		int index = 0;
		for (Paragraph<?, Either<String, EmbeddedImage>, ?> par : textArea.getDocument().getParagraphs()) {
			for (Either<String, EmbeddedImage> seg : par.getSegments()) {
				if (seg.isRight() && filter.test(seg.getRight()))
					removedImages.put(index, seg.getRight().text);

				index += seg.isLeft() ? seg.getLeft().length() : seg.getRight().text.length();
			}
			index++;
		}
		for (Map.Entry<Integer, String> e : removedImages.entrySet()) {
			int start = e.getKey();
			String text = e.getValue();
			textArea.replaceText(start, start + text.length(), text);
		}
	}
}
