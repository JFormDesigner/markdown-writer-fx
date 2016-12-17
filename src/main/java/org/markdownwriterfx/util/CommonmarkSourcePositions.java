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
import org.apache.commons.lang3.StringUtils;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.Code;
import org.commonmark.node.Delimited;
import org.commonmark.node.Document;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Image;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.ListItem;
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
			public void visit(Document node) {
				super.visit(node);

				positionsMap.put(node, new Range(0, markdownText.length()));
			}

			@Override
			public void visit(Text node) {
				positionForLiteral(node, node.getLiteral());
			}

			@Override
			public void visit(Link node) {
				super.visit(node);

				if (Utils.safeEquals(node.getDestination(), ((Text)node.getFirstChild()).getLiteral())) {
					// Syntax: <destination> or without <> if autolinks extension is enabled
					Range range = get(node);
					if (range != null && isAt(range.start - 1, '<') && isAt(range.end, '>'))
						positionsMap.put(node, new Range(range.start - 1, range.end + 1));
				} else {
					// Syntax: [text](destination "title")
					sanitizeLinkOrImage(node, node.getDestination(), node.getTitle(), false);
				}
			}

			@Override
			public void visit(Image node) {
				super.visit(node);

				// Syntax: ![text](destination "title")
				sanitizeLinkOrImage(node, node.getDestination(), node.getTitle(), true);
			}

			private void sanitizeLinkOrImage(Node node, String destination, String title, boolean image) {
				Range range = get(node);
				if (range == null)
					return;

				int start = range.start;
				int end = range.end;
				//TODO support ref
				Range destRange = rangeForText(node, destination);
				Range titleRange = (title != null) ? rangeForText(node, title) : null;
				if (titleRange != null) {
					end = titleRange.end;
					if (isAt(end, '"'))
						end++;
				} else if (destRange != null)
					end = destRange.end;

				if (isAt(start - 1, '['))
					start--;
				if (image && isAt(start - 1, '!'))
					start--;

				end = skipWhitespaceAfter(end);
				if (isAt(end, ')'))
					end++;

				positionsMap.put(node, new Range(start, end));
			}

			@Override
			public void visit(Code node) {
				Range range = rangeForText(node, node.getLiteral());
				if (range != null) {
					int start = skipWhitespaceBefore(range.start);
					int end = skipWhitespaceAfter(range.end);
					while (isAt(start - 1, '`') && isAt(end, '`')) {
						start--;
						end++;
					}
					positionsMap.put(node, new Range(start, end));
				}
			}

			@Override
			public void visit(Heading node) {
				super.visit(node);

				Range range = get(node);
				if (range != null) {
					int start = skipSpacesBefore(range.start);
					int end = skipSpacesAfter(range.end);
					if (isAt(start - 1, '#')) {
						// ATX heading
						for (int i = 0; i < node.getLevel(); i++) {
							if (!isAt(start - 1, '#'))
								break;
							start--;
						}
					} else {
						// Setext heading
						if (isAt(end, '\n'))
							end++;
						end = skipSpacesAfter(end);
						char ch = markdownText.charAt(end);
						if (ch == '=' || ch == '-') {
							end++;
							for (int i = end; i < markdownText.length(); i++) {
								if (markdownText.charAt(i) != ch)
									break;
								end++;
							}
						}
					}
					positionsMap.put(node, new Range(start, end));
				} else {
					// ATX heading without contents
					positionForLiteral(node, StringUtils.repeat('#', node.getLevel()));
				}
			}

			@Override
			public void visit(ListItem node) {
				super.visit(node);

				Range range = get(node);
				if (range != null) {
					int start = skipWhitespaceBefore(range.start);
					if (isAt(start - 1, '-') || isAt(start - 1, '+') || isAt(start - 1, '*'))
						start--;
					else if (isAt(start - 1, '.') || isAt(start - 1, ')')) {
						start--;
						for (int i = start; i > 0; i--) {
							if (!Character.isDigit(markdownText.charAt(i - 1)))
								break;
							start--;
						}
					}
					positionsMap.put(node, new Range(start, range.end));
				}
			}

			@Override
			public void visit(BlockQuote node) {
				super.visit(node);

				Range range = get(node);
				if (range != null) {
					int start = skipWhitespaceBefore(range.start);
					if (isAt(start - 1, '>'))
						start--;
					positionsMap.put(node, new Range(start, range.end));
				}
			}

			@Override
			public void visit(IndentedCodeBlock node) {
				//TODO literal does not contain indent of next lines
				positionForLiteral(node, node.getLiteral());
			}

			@Override
			public void visit(FencedCodeBlock node) {
				Range range = rangeForText(node, node.getLiteral());
				if (range != null) {
					int start = skipWhitespaceBefore(range.start);
					int end = skipWhitespaceAfter(range.end);

					for (int i = start; i > 0; i--) {
						char ch = markdownText.charAt(i - 1);
						if ((ch == '`' || ch == '~') && isAt(i - 3, 3, ch)) {
							start = i - 3;
							break;
						}
					}

					if (isAt(end, 3, '`') || isAt(end, 3, '~'))
						end += 3;
					positionsMap.put(node, new Range(start, end));
				}
			}

			@Override
			public void visit(HtmlBlock node) {
				positionForLiteral(node, node.getLiteral());
			}

			@Override
			public void visit(HtmlInline node) {
				positionForLiteral(node, node.getLiteral());
			}

			@Override
			protected void visitChildren(Node node) {
				super.visitChildren(node);

				// sanitize Emphasis, StrongEmphasis, Strikethrough and Ins
				if (node instanceof Delimited) {
					Range range = get(node);
					if (range != null) {
						positionsMap.put(node, new Range(
							range.start - ((Delimited)node).getOpeningDelimiter().length(),
							range.end + ((Delimited)node).getClosingDelimiter().length()));
					}
				}
			}

			private void positionForLiteral(Node node, String text) {
				Range range = rangeForText(node, text);
				if (range != null)
					positionsMap.put(node, range);
			}

			private Range rangeForText(Node node, String text) {
				if (text == null || text.length() == 0)
					return null;

				// TODO handle escaped characters
				int textLength = text.length();
				int index = indexOfEscaped(markdownText, text, textIndex);
				if (index < 0)
					return null;

				// include leading escape characters
				for (int i = index - 1; i >= 0; i--) {
					if (markdownText.charAt(i) != '\\')
						break;
					index--;
					textLength++;
				}

				int end = index + textLength;
				textIndex = end;
				return new Range(index, end);
			}

			private int indexOfEscaped(String str, String searchStr, int fromIndex) {
				int index = str.indexOf(searchStr, fromIndex);
				if (index >= 0)
					return index;

				// maybe the markdown text contains escape characters, but the search string does not
				int strLength = str.length();
				int searchStrLength = searchStr.length();
				char firstSearchChar = searchStr.charAt(0);
				for (index = fromIndex; (index = str.indexOf(firstSearchChar, index)) >= 0; index++) {
					if (index + searchStrLength > strLength)
						return -1;

					int i = 1;
					for (int j = index + 1; i < searchStrLength && j < strLength; i++, j++) {
						char searchChar = searchStr.charAt(i);
						char ch = str.charAt(j);
						if (ch == '\\' && ch != searchChar) {
							// skip escape character
							j++;
							if (j < strLength)
								ch = str.charAt(j);
						}
						if (ch != searchChar)
							break;
					}
					if (i == searchStrLength)
						return index;
				}
				return -1;
			}

			private boolean isAt(int offset, char ch) {
				if (offset < 0 || offset >= markdownText.length())
					return false;
				return markdownText.charAt(offset) == ch;
			}

			private boolean isAt(int offset, int count, char ch) {
				for (int i = 0; i < count; i++) {
					if (!isAt(offset + i, ch))
						return false;
				}
				return true;
			}

			private int skipWhitespaceBefore(int offset) {
				for (int i = offset; i > 0; i--) {
					if (!Character.isWhitespace(markdownText.charAt(i - 1)))
						return i;
				}
				return 0;
			}

			private int skipWhitespaceAfter(int offset) {
				for (int i = offset; i < markdownText.length(); i++) {
					if (!Character.isWhitespace(markdownText.charAt(i)))
						return i;
				}
				return markdownText.length();
			}

			private int skipSpacesBefore(int offset) {
				for (int i = offset; i > 0; i--) {
					char ch = markdownText.charAt(i - 1);
					if (ch != ' ' && ch != '\t')
						return i;
				}
				return 0;
			}

			private int skipSpacesAfter(int offset) {
				for (int i = offset; i < markdownText.length(); i++) {
					char ch = markdownText.charAt(i);
					if (ch != ' ' && ch != '\t')
						return i;
				}
				return markdownText.length();
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
