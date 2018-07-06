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

				if (node.getFirstChild() instanceof Text &&
					Utils.safeEquals(node.getDestination(), ((Text)node.getFirstChild()).getLiteral()))
				{
					// Syntax: <destination> or without <> if autolinks extension is enabled
					Range range = get(node);
					if (range != null && isAt(range.start - 1, '<') && isAt(range.end, '>'))
						positionsMap.put(node, new Range(range.start - 1, range.end + 1));
				} else {
					// Syntax: [text](destination "title") or [text]
					sanitizeLinkOrImage(node, node.getDestination(), node.getTitle(), false);
				}
			}

			@Override
			public void visit(Image node) {
				super.visit(node);

				// Syntax: ![text](destination "title") or ![text]
				sanitizeLinkOrImage(node, node.getDestination(), node.getTitle(), true);
			}

			private void sanitizeLinkOrImage(Node node, String destination, String title, boolean image) {
				Range range = get(node);
				if (range == null)
					return;

				int start = range.start;
				int end = range.end;

				if (isAt(start - 1, '['))
					start--;
				if (image && isAt(start - 1, '!'))
					start--;

				if (isAt(end, ']'))
					end++;

				end = findEndOfLinkOrImage(end, destination, title);

				positionsMap.put(node, new Range(start, end));
			}

			private int findEndOfLinkOrImage(int end, String destination, String title) {
				if (!isAt(end, '('))
					return end; // reference link

				int end2 = skipSpacesAfter(end + 1);
				if ((end2 = equalsAtEscaped(markdownText, destination, end2)) < 0)
					return end;

				end2 = skipSpacesAfter(end2);

				if (title != null) {
					if (!isAt(end2++, '"'))
						return end;

					end2 = skipSpacesAfter(end2);
					if ((end2 = equalsAtEscaped(markdownText, title, end2)) < 0)
						return end;

					end2 = skipSpacesAfter(end2);
					if (!isAt(end2++, '"'))
						return end;
				}

				if (!isAt(end2++, ')'))
					return end;

				textIndex = end2;
				return end2;
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
				Range range = rangeForCode(node, node.getLiteral());
				if (range != null)
					positionsMap.put(node, range);
			}

			@Override
			public void visit(FencedCodeBlock node) {
				for (int i = textIndex; i < markdownText.length(); i++) {
					char ch = markdownText.charAt(i);
					if ((ch == '`' || ch == '~') && isAt(i, 3, ch)) {
						// found fenced code start
						int start = i;
						int end = markdownText.length();

						int nlIndex = markdownText.indexOf('\n', i);
						if (nlIndex >= 0) {
							textIndex = nlIndex + 1;
							Range range = rangeForCode(node, node.getLiteral());
							if (range != null) {
								end = skipWhitespaceAfter(range.end);
								while (isAt(end, ch))
									end++;
								textIndex = end;
							}
						}
						positionsMap.put(node, new Range(start, end));
						break;
					}
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

				Range range = indexOfEscaped(markdownText, text, textIndex);
				if (range == null)
					return null;

				int start = range.start;
				int end = range.end;

				// include leading escape characters
				for (int i = start - 1; i >= 0; i--) {
					if (markdownText.charAt(i) != '\\')
						break;
					start--;
				}

				textIndex = end;
				return new Range(start, end);
			}

			private Range indexOfEscaped(String str, String searchStr, int fromIndex) {
				int index = str.indexOf(searchStr, fromIndex);
				if (index >= 0)
					return new Range(index, index + searchStr.length());

				// maybe the markdown text contains escape characters, but the search string does not
				char firstSearchChar = searchStr.charAt(0);
				for (index = fromIndex; (index = str.indexOf(firstSearchChar, index)) >= 0; index++) {
					int end;
					if ((end = equalsAtEscaped(str, searchStr, index)) >= 0)
						return new Range(index, end);
				}
				return null;
			}

			private Range rangeForCode(Node node, String text) {
				if (text == null || text.length() == 0)
					return null;

				int start = skipWhitespaceAfter(textIndex);
				int i = skipWhitespaceAfter(text, 0);
				int j = start;
				do {
					int ilen = lineLength(text, i);
					int jlen = lineLength(markdownText, j);
					if (ilen != jlen || !equalsAt(markdownText, j, text, i, ilen))
						return null;

					i = skipSpacesAfter(text, i + ilen + 1);
					j = skipSpacesAfter(markdownText, j + jlen + 1);
				} while (i < text.length() && j < markdownText.length());

				textIndex = j;
				return new Range(start, j);
			}

			private int lineLength(String str, int fromIndex) {
				int index = str.indexOf('\n', fromIndex);
				return (index >= 0 ? index : str.length()) - fromIndex;
			}

			/**
			 * Checks whether parts of two strings are equal.
			 */
			private boolean equalsAt(String str1, int index1, String str2, int index2, int length2) {
				int str1Length = str1.length();
				int str2Length = Math.min(index2 + length2, str2.length());
				if (index1 + length2 > str1Length)
					return false;

				int i1 = index1;
				int i2 = index2;
				for (; i1 < str1Length && i2 < str2Length; i1++, i2++) {
					if (str1.charAt(i1) != str2.charAt(i2))
						break;
				}
				return (i2 == str2Length);
			}

			/**
			 * Checks whether `searchStr` is in `str` at `index`.
			 * Considers markdown escaping in `str`.
			 * Returns the end index of the matched string including escape characters; otherwise -1
			 */
			private int equalsAtEscaped(String str, String searchStr, int index) {
				int strLength = str.length();
				int searchStrLength = searchStr.length();
				if (index + searchStrLength > strLength)
					return -1;

				int i = 0;
				int j = index;
				for (; i < searchStrLength && j < strLength; i++, j++) {
					char searchChar = searchStr.charAt(i);
					char ch = str.charAt(j);
					if (ch != searchChar) {
						if (ch == '\\') {
							// skip escape character
							j++;
							if (j < strLength)
								ch = str.charAt(j);
							if (ch == searchChar)
								continue;
						} else if (ch == '\n' && searchChar == ' ') {
							// skip line break in inline
							continue;
						}
						break;
					}
				}
				return (i == searchStrLength) ? j : -1;
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
				return skipWhitespaceAfter(markdownText, offset);
			}

			private int skipWhitespaceAfter(String str, int offset) {
				for (int i = offset; i < str.length(); i++) {
					if (!Character.isWhitespace(str.charAt(i)))
						return i;
				}
				return str.length();
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
				return skipSpacesAfter(markdownText, offset);
			}

			private int skipSpacesAfter(String str, int offset) {
				for (int i = offset; i < str.length(); i++) {
					char ch = str.charAt(i);
					if (ch != ' ' && ch != '\t')
						return i;
				}
				return str.length();
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
