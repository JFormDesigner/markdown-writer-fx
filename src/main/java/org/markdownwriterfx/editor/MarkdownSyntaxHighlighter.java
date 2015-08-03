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
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.StyleSpans;
import org.fxmisc.richtext.StyleSpansBuilder;
import org.pegdown.ast.*;

/**
 * Markdown syntax highlighter.
 *
 * Uses pegdown AST.
 *
 * @author Karl Tauber
 */
class MarkdownSyntaxHighlighter
	implements Visitor
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
		caption,
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
	private boolean inTableHeader;

	static void highlight(StyleClassedTextArea textArea, RootNode astRoot) {
		assert StyleClass.values().length <= 32;
		assert Platform.isFxApplicationThread();

		textArea.setStyleSpans(0, new MarkdownSyntaxHighlighter()
				.computeHighlighting(astRoot, textArea.getLength()));
	}

	private MarkdownSyntaxHighlighter() {
	}

	private StyleSpans<Collection<String>> computeHighlighting(RootNode astRoot, int textLength) {
		styleClassBits = new int[textLength];

		// visit all nodes
		astRoot.accept(this);

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

	@Override
	public void visit(AbbreviationNode node) {
		// noting to do here
	}

	@Override
	public void visit(AnchorLinkNode node) {
		// noting to do here
	}

	@Override
	public void visit(AutoLinkNode node) {
		setStyleClass(node, StyleClass.a);
	}

	@Override
	public void visit(BlockQuoteNode node) {
		setStyleClass(node, StyleClass.blockquote);
		visitChildren(node);
	}

	@Override
	public void visit(BulletListNode node) {
		setStyleClass(node, StyleClass.ul);
		visitChildren(node);
	}

	@Override
	public void visit(CodeNode node) {
		setStyleClass(node, StyleClass.code);
	}

	@Override
	public void visit(DefinitionListNode node) {
		setStyleClass(node, StyleClass.dl);
		visitChildren(node);
	}

	@Override
	public void visit(DefinitionNode node) {
		setStyleClass(node, StyleClass.dd);
		visitChildren(node);
	}

	@Override
	public void visit(DefinitionTermNode node) {
		setStyleClass(node, StyleClass.dt);
		visitChildren(node);
	}

	@Override
	public void visit(ExpImageNode node) {
		setStyleClass(node, StyleClass.img);
		visitChildren(node);
	}

	@Override
	public void visit(ExpLinkNode node) {
		setStyleClass(node, StyleClass.a);
		visitChildren(node);
	}

	@Override
	public void visit(HeaderNode node) {
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
		if (!node.getChildren().isEmpty() &&
				node.getChildren().get(0).getStartIndex() == node.getStartIndex())
			setStyleClass(node, StyleClass.monospace);

		visitChildren(node);
	}

	@Override
	public void visit(HtmlBlockNode node) {
		setStyleClass(node, StyleClass.html);
	}

	@Override
	public void visit(InlineHtmlNode node) {
		setStyleClass(node, StyleClass.html);
	}

	@Override
	public void visit(ListItemNode node) {
		setStyleClass(node, StyleClass.li);
		visitChildren(node);
	}

	@Override
	public void visit(MailLinkNode node) {
		setStyleClass(node, StyleClass.a);
	}

	@Override
	public void visit(OrderedListNode node) {
		setStyleClass(node, StyleClass.ol);
		visitChildren(node);
	}

	@Override
	public void visit(ParaNode node) {
		visitChildren(node);
	}

	@Override
	public void visit(QuotedNode node) {
		// noting to do here
	}

	@Override
	public void visit(ReferenceNode node) {
		// noting to do here
	}

	@Override
	public void visit(RefImageNode node) {
		setStyleClass(node, StyleClass.img);
		visitChildren(node);
	}

	@Override
	public void visit(RefLinkNode node) {
		setStyleClass(node, StyleClass.a);
		visitChildren(node);
	}

	@Override
	public void visit(RootNode node) {
		visitChildren(node);
	}

	@Override
	public void visit(SimpleNode node) {
		// noting to do here
	}

	@Override
	public void visit(SpecialTextNode node) {
		// noting to do here
	}

	@Override
	public void visit(StrikeNode node) {
		setStyleClass(node, StyleClass.del);
		visitChildren(node);
	}

	@Override
	public void visit(StrongEmphSuperNode node) {
		if (node.isClosed())
			setStyleClass(node, node.isStrong() ? StyleClass.strong : StyleClass.em);
		// else sequence was not closed, treat open chars as ordinary chars

		visitChildren(node);
	}

	@Override
	public void visit(TableBodyNode node) {
		setStyleClass(node, StyleClass.tbody);
		visitChildren(node);
	}

	@Override
	public void visit(TableCaptionNode node) {
		setStyleClass(node, StyleClass.caption);
		visitChildren(node);
	}

	@Override
	public void visit(TableCellNode node) {
		setStyleClass(node, inTableHeader ? StyleClass.th : StyleClass.td);
		visitChildren(node);
	}

	@Override
	public void visit(TableColumnNode node) {
		// noting to do here
	}

	@Override
	public void visit(TableHeaderNode node) {
		setStyleClass(node, StyleClass.thead);

		inTableHeader = true;
		visitChildren(node);
		inTableHeader = false;
	}

	@Override
	public void visit(TableNode node) {
		setStyleClass(node, StyleClass.table);
		visitChildren(node);
	}

	@Override
	public void visit(TableRowNode node) {
		setStyleClass(node, StyleClass.tr);
		visitChildren(node);
	}

	@Override
	public void visit(VerbatimNode node) {
		setStyleClass(node, StyleClass.pre);
	}

	@Override
	public void visit(WikiLinkNode node) {
		setStyleClass(node, StyleClass.a);
	}

	@Override
	public void visit(TextNode node) {
		// noting to do here
	}

	@Override
	public void visit(SuperNode node) {
		visitChildren(node);
	}

	@Override
	public void visit(Node node) {
		// ignore custom Node implementations
	}

	private void visitChildren(SuperNode node) {
		for (Node child : node.getChildren())
			child.accept(this);
	}

	private void setStyleClass(Node node, StyleClass styleClass) {
		// because PegDownProcessor.prepareSource() adds two trailing newlines
		// to the text before parsing, we need to limit the end index
		int start = node.getStartIndex();
		int end = Math.min(node.getEndIndex(), styleClassBits.length);
		int styleBit = 1 << styleClass.ordinal();

		for (int i = start; i < end; i++)
			styleClassBits[i] |= styleBit;
	}
}
