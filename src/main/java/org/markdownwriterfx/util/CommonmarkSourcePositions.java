/*
 * Copyright (c) 2016 Karl Tauber <karl at jformdesigner dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
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

package org.markdownwriterfx.util;

import java.util.IdentityHashMap;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.node.Visitor;

/**
 * commonmark-java source positions.
 *
 * @author Karl Tauber
 */
public class CommonmarkSourcePositions
{
	private final IdentityHashMap<Node, Range> positionsMap = new IdentityHashMap<>();

	public CommonmarkSourcePositions(String markdownText, Node astRoot) {
		Visitor visitor = new AbstractVisitor() {
			private int textIndex = 0;

			@Override
			public void visit(Text node) {
				positionForLiteral(node, node.getLiteral());
				super.visit(node);
			}

			@Override
			public void visit(Code node) {
				positionForLiteral(node, node.getLiteral());
				super.visit(node);
			}

			@Override
			public void visit(IndentedCodeBlock node) {
				positionForLiteral(node, node.getLiteral());
				super.visit(node);
			}

			@Override
			public void visit(FencedCodeBlock node) {
				positionForLiteral(node, node.getLiteral());
				super.visit(node);
			}

			@Override
			public void visit(HtmlBlock node) {
				positionForLiteral(node, node.getLiteral());
				super.visit(node);
			}

			@Override
			public void visit(HtmlInline node) {
				positionForLiteral(node, node.getLiteral());
				super.visit(node);
			}

			private void positionForLiteral(Node node, String literal) {
				if (literal == null)
					return;

				// TODO handle escaped characters
				int index = markdownText.indexOf(literal, textIndex);
				if (index >= 0) {
					int end = index + literal.length();
					positionsMap.put(node, new Range(index, end));
					textIndex = end;
				}
			}
		};
		astRoot.accept(visitor);
	}

	public Range get(Node node) {
		Range range = positionsMap.get(node);
		if (range == null && node.getFirstChild() != null) {
			// use startOffset of first child and endOffset of last child
			Range firstRange = get(node.getFirstChild());
			Range lastRange = get(node.getLastChild());
			if (firstRange != null && lastRange != null) {
				range = new Range(firstRange.start, lastRange.end);
				positionsMap.put(node, range);
			}
		}
		return range;
	}
}
