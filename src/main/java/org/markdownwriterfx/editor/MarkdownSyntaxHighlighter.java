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
import java.util.HashMap;
import javafx.application.Platform;
import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough;
import com.vladsch.flexmark.ext.gfm.tables.TableBlock;
import com.vladsch.flexmark.ext.gfm.tables.TableBody;
import com.vladsch.flexmark.ext.gfm.tables.TableCell;
import com.vladsch.flexmark.ext.gfm.tables.TableHead;
import com.vladsch.flexmark.ext.gfm.tables.TableRow;
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
	/*private*/ enum StyleClass {
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
		br,

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
		tr,
		th,
		td,

		// misc
		html,
		monospace,
		reference,
	};

	private static final HashMap<Class<? extends Node>, StyleClass> node2style = new HashMap<>();

	static {
		// inlines
		node2style.put(StrongEmphasis.class, StyleClass.strong);
		node2style.put(Emphasis.class, StyleClass.em);
		node2style.put(Strikethrough.class, StyleClass.del);
		node2style.put(Link.class, StyleClass.a);
		node2style.put(LinkRef.class, StyleClass.a);
		node2style.put(Image.class, StyleClass.img);
		node2style.put(Code.class, StyleClass.code);
		node2style.put(HardLineBreak.class, StyleClass.br);

		// blocks
		node2style.put(FencedCodeBlock.class, StyleClass.pre);
		node2style.put(IndentedCodeBlock.class, StyleClass.pre);
		node2style.put(BlockQuote.class, StyleClass.blockquote);

		// lists
		node2style.put(BulletList.class, StyleClass.ul);
		node2style.put(OrderedList.class, StyleClass.ol);
		node2style.put(ListItem.class, StyleClass.li);

		// tables
		node2style.put(TableBlock.class, StyleClass.table);
		node2style.put(TableHead.class, StyleClass.thead);
		node2style.put(TableBody.class, StyleClass.tbody);
		node2style.put(TableRow.class, StyleClass.tr);

		// misc
		node2style.put(HtmlBlock.class, StyleClass.html);
		node2style.put(HtmlInline.class, StyleClass.html);
		node2style.put(Reference.class, StyleClass.reference);
	}

	private ArrayList<StyleRange> styleRanges;

	static void highlight(StyleClassedTextArea textArea, Node astRoot) {
		assert Platform.isFxApplicationThread();

		assert textArea.getText().length() == textArea.getLength();
		textArea.setStyleSpans(0, new MarkdownSyntaxHighlighter()
				.computeHighlighting(astRoot, textArea.getText()));
	}

	private MarkdownSyntaxHighlighter() {
	}

	private StyleSpans<Collection<String>> computeHighlighting(Node astRoot, String text) {
		styleRanges = new ArrayList<>();

		// visit all nodes
		NodeVisitor visitor = new NodeVisitor(
			new VisitHandler<>(Heading.class, this::visit),
			new VisitHandler<>(TableCell.class, this::visit))
		{
			@Override
			public void visit(Node node) {
				Class<? extends Node> nodeClass = node.getClass();
				StyleClass style = node2style.get(nodeClass);
				if (style != null)
					setStyleClass(node, style);
				else {
					VisitHandler<?> handler = myCustomHandlersMap.get(nodeClass);
					if (handler != null)
						handler.visit(node);
				}

				visitChildren(node);
			}
		};
		visitor.visit(astRoot);

		// build style spans
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		if (text.length() > 0) {
			int spanStart = 0;
			for (StyleRange range : styleRanges) {
				if (range.begin > spanStart)
					spansBuilder.add(Collections.emptyList(), range.begin - spanStart);
				spansBuilder.add(toStyleClasses(range.styleBits), range.end - range.begin);
				spanStart = range.end;
			}
			if (spanStart < text.length())
				spansBuilder.add(Collections.emptyList(), text.length() - spanStart);
		} else
			spansBuilder.add(Collections.emptyList(), 0);
		return spansBuilder.create();
	}

	private Collection<String> toStyleClasses(long bits) {
		if (bits == 0)
			return Collections.emptyList();

		Collection<String> styleClasses = new ArrayList<>(1);
		for (StyleClass styleClass : StyleClass.values()) {
			if ((bits & (1 << styleClass.ordinal())) != 0)
				styleClasses.add(styleClass.name());
		}
		return styleClasses;
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

	private void visit(TableCell node) {
		setStyleClass(node, node.isHeader() ?  StyleClass.th : StyleClass.td);
	}

	private void setStyleClass(Node node, StyleClass styleClass) {
		int start = node.getStartOffset();
		int end = node.getEndOffset();

		addStyledRange(styleRanges, start, end, styleClass);
	}

	/**
	 * Adds a style range to styleRanges.
	 *
	 * Makes sure that the ranges are sorted by begin index
	 * and that there are no overlapping ranges.
	 * In case the added range overlaps, existing ranges are split.
	 *
	 * @param begin the beginning index, inclusive
	 * @param end   the ending index, exclusive
	 */
	/*private*/ static void addStyledRange(ArrayList<StyleRange> styleRanges, int begin, int end, StyleClass styleClass) {
		final int styleBits = 1 << styleClass.ordinal();
		final int lastIndex = styleRanges.size() - 1;

		// check whether list is empty
		if (styleRanges.isEmpty()) {
			styleRanges.add(new StyleRange(begin, end, styleBits));
			return;
		}

		// check whether new range is after last range
		final StyleRange lastRange = styleRanges.get(lastIndex);
		if (begin >= lastRange.end) {
			styleRanges.add(new StyleRange(begin, end, styleBits));
			return;
		}

		// walk existing ranges from last to first
		for (int i = lastIndex; i >= 0; i--) {
			StyleRange range = styleRanges.get(i);
			if (end <= range.begin) {
				// new range is before existing range (no overlapping) --> nothing yet to do
				continue;
			}

			if (begin >= range.end) {
				// existing range is before new range (no overlapping)

				if (begin < styleRanges.get(i+1).begin) {
					// new range starts after this range (may overlap next range) --> add
					int end2 = Math.min(end, styleRanges.get(i+1).begin);
					styleRanges.add(i + 1, new StyleRange(begin, end2, styleBits));
				}

				break; // done
			}

			if (end > range.end) {
				// new range ends after this range (may overlap next range) --> add
				int end2 = (i == lastIndex) ? end : Math.min(end, styleRanges.get(i+1).begin);
				if (end2 > range.end)
					styleRanges.add(i + 1, new StyleRange(range.end, end2, styleBits));
			}

			if (begin < range.end && end > range.begin) {
				// the new range overlaps the existing range somewhere

				if (begin <= range.begin && end >= range.end) {
					// new range completely overlaps existing range --> merge style bits
					styleRanges.set(i, new StyleRange(range.begin, range.end, range.styleBits | styleBits));
				} else if (begin <= range.begin && end < range.end) {
					// new range overlaps at the begin with existing range --> split range
					styleRanges.set(i, new StyleRange(range.begin, end, range.styleBits | styleBits));
					styleRanges.add(i + 1, new StyleRange(end, range.end, range.styleBits));
				} else if (begin > range.begin && end >= range.end) {
					// new range overlaps at the end with existing range --> split range
					styleRanges.set(i, new StyleRange(range.begin, begin, range.styleBits));
					styleRanges.add(i + 1, new StyleRange(begin, range.end, range.styleBits | styleBits));
				} else if (begin > range.begin && end < range.end) {
					// new range is in existing range --> split range
					styleRanges.set(i, new StyleRange(range.begin, begin, range.styleBits));
					styleRanges.add(i + 1, new StyleRange(begin, end, range.styleBits | styleBits));
					styleRanges.add(i + 2, new StyleRange(end, range.end, range.styleBits));
				}
			}
		}

		// check whether new range starts before first range
		if (begin < styleRanges.get(0).begin) {
			// add new range (part) before first range
			int end2 = Math.min(end, styleRanges.get(0).begin);
			styleRanges.add(0, new StyleRange(begin, end2, styleBits));
		}
	}

	//---- class StyleRange ---------------------------------------------------

	/*private*/ static class StyleRange
	{
		final int begin;		// inclusive
		final int end;			// exclusive
		final long styleBits;	// 1 << StyleClass.ordinal()

		StyleRange(int begin, int end, long styleBits) {
			this.begin = begin;
			this.end = end;
			this.styleBits = styleBits;
		}
	}
}
