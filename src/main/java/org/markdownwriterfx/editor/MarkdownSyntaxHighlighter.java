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

import java.util.Collections;
import javafx.application.Platform;
import org.fxmisc.richtext.StyleClassedTextArea;
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
	private final StyleClassedTextArea textArea;
	private final int textLength;

	static void highlight(StyleClassedTextArea textArea, RootNode astRoot) {
		new MarkdownSyntaxHighlighter(textArea).toStyles(astRoot);
	}

	private MarkdownSyntaxHighlighter(StyleClassedTextArea textArea) {
		this.textArea = textArea;
		this.textLength = textArea.getLength();
	}

	private void toStyles(RootNode astRoot) {
		assert Platform.isFxApplicationThread();

		textArea.clearStyle(0, textLength);
		astRoot.accept(this);
	}

	@Override
	public void visit(AbbreviationNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(AnchorLinkNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(AutoLinkNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(BlockQuoteNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(BulletListNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(CodeNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(DefinitionListNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(DefinitionNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(DefinitionTermNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ExpImageNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ExpLinkNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(HeaderNode node) {
		setStyleClass(node, "h" + node.getLevel());
	}

	@Override
	public void visit(HtmlBlockNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(InlineHtmlNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ListItemNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(MailLinkNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OrderedListNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ParaNode node) {
		// TODO Auto-generated method stub
		visitChildren(node);
	}

	@Override
	public void visit(QuotedNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ReferenceNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(RefImageNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(RefLinkNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(RootNode node) {
		// TODO Auto-generated method stub
		visitChildren(node);
	}

	@Override
	public void visit(SimpleNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(SpecialTextNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(StrikeNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(StrongEmphSuperNode node) {
		setStyleClass(node, node.isStrong() ? "strong" : "em");
	}

	@Override
	public void visit(TableBodyNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(TableCaptionNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(TableCellNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(TableColumnNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(TableHeaderNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(TableNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(TableRowNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(VerbatimNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(WikiLinkNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(TextNode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(SuperNode node) {
		// TODO Auto-generated method stub
		visitChildren(node);
	}

	@Override
	public void visit(Node node) {
		// TODO Auto-generated method stub

	}

	private void visitChildren(SuperNode node) {
		for (Node child : node.getChildren())
			child.accept(this);
	}

	private void setStyleClass(Node node, String styleClass) {
		try {
			// because PegDownProcessor.prepareSource() adds two trailing newlines
			// to the text before parsing, we need to limit the end index
			textArea.setStyle(node.getStartIndex(),
					Math.min(node.getEndIndex(), textLength),
					Collections.singleton(styleClass));
		} catch(IllegalArgumentException ex) {
			ex.printStackTrace();
		}
	}
}
