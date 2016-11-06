/*
 * Copyright (c) 2015 Karl Tauber <karl at jformdesigner dot com>
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import javafx.application.Platform;
import com.vladsch.flexmark.ast.*;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

/**
 * Markdown syntax highlighter.
 *
 * Uses flexmark-java AST.
 *
 * @author Karl Tauber
 */
class MarkdownSyntaxHighlighter
{
	private enum StyleClass {
		// headers
		h1,
		h2,
		h3,
		h4,
		h5,
		h6,

		// inlines
		strong,
		em,
		del,
		a,
		img,
		code,

		// blocks
		pre,
		blockquote,

		// lists
		ul,
		ol,
		li,
		dl,
		dt,
		dd,

		// tables
		table,
		thead,
		tbody,
		th,
		tr,
		td,

		// misc
		html,
		monospace,
	};

	/**
	 * style bits (1 << StyleClass.ordinal()) for each character
	 * simplifies implementation of overlapping styles
	 */
	private int[] styleClassBits;

	private int lineCount;
	private int[] linePositions;

	static void highlight(StyleClassedTextArea textArea, Node astRoot) {
		assert StyleClass.values().length <= 32;
		assert Platform.isFxApplicationThread();

		assert textArea.getText().length() == textArea.getLength();
		textArea.setStyleSpans(0, new MarkdownSyntaxHighlighter()
				.computeHighlighting(astRoot, textArea.getText()));
	}

	private MarkdownSyntaxHighlighter() {
	}

	private StyleSpans<Collection<String>> computeHighlighting(Node astRoot, String text) {
		initLinePositions(text);

		styleClassBits = new int[text.length()];

		// visit all nodes
		NodeVisitor visitor = new NodeVisitor(
			new VisitHandler<>(BlockQuote.class, this::visit),
			new VisitHandler<>(BulletList.class, this::visit),
			new VisitHandler<>(Code.class, this::visit),
			new VisitHandler<>(Heading.class, this::visit),
			new VisitHandler<>(HtmlBlock.class, this::visit),
			new VisitHandler<>(HtmlInline.class, this::visit),
			new VisitHandler<>(ListItem.class, this::visit),
			new VisitHandler<>(OrderedList.class, this::visit),
			new VisitHandler<>(Image.class, this::visit),
			new VisitHandler<>(Link.class, this::visit),
			new VisitHandler<>(Emphasis.class, this::visit),
			new VisitHandler<>(StrongEmphasis.class, this::visit),
			new VisitHandler<>(FencedCodeBlock.class, this::visit),
			new VisitHandler<>(IndentedCodeBlock.class, this::visit))
		{
			@Override
			public void visit(Node node) {
				VisitHandler<?> handler = myCustomHandlersMap.get(node.getClass());
				if (handler != null)
					handler.visit(node);

				visitChildren(node);
			}
		};
		visitor.visit(astRoot);

		// build style spans
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		if (styleClassBits.length > 0) {
			int spanStart = 0;
			int previousBits = styleClassBits[0];

			for (int i = 1; i < styleClassBits.length; i++) {
				int bits = styleClassBits[i];
				if (bits == previousBits)
					continue;

				spansBuilder.add(toStyleClasses(previousBits), i - spanStart);

				spanStart = i;
				previousBits = bits;
			}
			spansBuilder.add(toStyleClasses(previousBits), styleClassBits.length - spanStart);
		} else
			spansBuilder.add(Collections.emptyList(), 0);
		return spansBuilder.create();
	}

	private void initLinePositions(String text) {
		lineCount = 1;
		linePositions = new int[Math.max(text.length() / 20, 10)];

		int newlineIndex = 0;
		while((newlineIndex = text.indexOf('\n', newlineIndex)) >= 0) {
			if (lineCount >= linePositions.length) {
				// grow line positions array
				int[] linePositions2 = new int[linePositions.length + (linePositions.length >> 1)];
				System.arraycopy(linePositions, 0, linePositions2, 0, linePositions.length);
				linePositions = linePositions2;
			}

			linePositions[lineCount++] = ++newlineIndex;
		}
	}

	private Collection<String> toStyleClasses(int bits) {
		if (bits == 0)
			return Collections.emptyList();

		Collection<String> styleClasses = new ArrayList<>(1);
		for (StyleClass styleClass : StyleClass.values()) {
			if ((bits & (1 << styleClass.ordinal())) != 0)
				styleClasses.add(styleClass.name());
		}
		return styleClasses;
	}

	private void visit(BlockQuote node) {
		setStyleClass(node, StyleClass.blockquote);
	}

	private void visit(BulletList node) {
		setStyleClass(node, StyleClass.ul);
	}

	private void visit(Code node) {
		setStyleClass(node, StyleClass.code);
	}

	private void visit(Heading node) {
		StyleClass styleClass;
		switch (node.getLevel()) {
			case 1: styleClass = StyleClass.h1; break;
			case 2: styleClass = StyleClass.h2; break;
			case 3: styleClass = StyleClass.h3; break;
			case 4: styleClass = StyleClass.h4; break;
			case 5: styleClass = StyleClass.h5; break;
			case 6: styleClass = StyleClass.h6; break;
			default: return;
		}
		setStyleClass(node, styleClass);

		// use monospace font for underlined headers
		if (node.isSetextHeading())
			setStyleClass(node, StyleClass.monospace);
	}

	private void visit(HtmlBlock node) {
		setStyleClass(node, StyleClass.html);
	}

	private void visit(HtmlInline node) {
		setStyleClass(node, StyleClass.html);
	}

	private void visit(ListItem node) {
		setStyleClass(node, StyleClass.li);
	}

	private void visit(OrderedList node) {
		setStyleClass(node, StyleClass.ol);
	}

	private void visit(Image node) {
		setStyleClass(node, StyleClass.img);
	}

	private void visit(Link node) {
		setStyleClass(node, StyleClass.a);
	}

	private void visit(Emphasis node) {
		setStyleClass(node, StyleClass.em);
	}

	private void visit(StrongEmphasis node) {
		setStyleClass(node, StyleClass.strong);
	}

	private void visit(FencedCodeBlock node) {
		setStyleClass(node, StyleClass.pre);
	}

	private void visit(IndentedCodeBlock node) {
		setStyleClass(node, StyleClass.pre);
	}

/*TODO
	@Override
	public void visit(CustomBlock node) {
		if (node instanceof TableBlock)
			setStyleClass(node, StyleClass.table);

		super.visit(node);
	}

	@Override
	public void visit(CustomNode node) {
		if (node instanceof Strikethrough)
			setStyleClass(node, StyleClass.del);
		else if (node instanceof TableHead)
			setStyleClass(node, StyleClass.thead);
		else if (node instanceof TableBody)
			setStyleClass(node, StyleClass.tbody);
		else if (node instanceof TableRow)
			setStyleClass(node, StyleClass.tr);
		else if (node instanceof TableCell)
			setStyleClass(node, ((TableCell)node).isHeader() ?  StyleClass.th : StyleClass.td);

		super.visit(node);
	}
*/

	private void setStyleClass(Node node, StyleClass styleClass) {
		int start = node.getStartOffset();
		int end = node.getEndOffset();
		int styleBit = 1 << styleClass.ordinal();

		for (int i = start; i < end; i++)
			styleClassBits[i] |= styleBit;
	}
}
