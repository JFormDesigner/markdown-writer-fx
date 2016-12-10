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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javafx.application.Platform;
import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.ext.abbreviation.Abbreviation;
import com.vladsch.flexmark.ext.abbreviation.AbbreviationBlock;
import com.vladsch.flexmark.ext.aside.AsideBlock;
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough;
import com.vladsch.flexmark.ext.gfm.tables.TableBlock;
import com.vladsch.flexmark.ext.gfm.tables.TableBody;
import com.vladsch.flexmark.ext.gfm.tables.TableCell;
import com.vladsch.flexmark.ext.gfm.tables.TableHead;
import com.vladsch.flexmark.ext.gfm.tables.TableRow;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListItem;
import com.vladsch.flexmark.ext.wikilink.WikiLink;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.richtext.model.TwoDimensional.Bias;
import org.markdownwriterfx.util.Range;

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
		aside,

		// lists
		ul,
		ol,
		li,
		liopen,
		liopentask,
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
		reference,
		abbrdef,
		abbr,
	};

	private static final HashMap<Long, Collection<String>> styleClassesCache = new HashMap<>();
	private static final HashMap<Class<? extends Node>, StyleClass> node2style = new HashMap<>();
	private static final HashMap<Class<? extends Node>, StyleClass> node2lineStyle = new HashMap<>();

	static {
		// inlines
		node2style.put(StrongEmphasis.class, StyleClass.strong);
		node2style.put(Emphasis.class, StyleClass.em);
		node2style.put(Strikethrough.class, StyleClass.del);
		node2style.put(Link.class, StyleClass.a);
		node2style.put(LinkRef.class, StyleClass.a);
		node2style.put(AutoLink.class, StyleClass.a);
		node2style.put(MailLink.class, StyleClass.a);
		node2style.put(WikiLink.class, StyleClass.a);
		node2style.put(Image.class, StyleClass.img);
		node2style.put(ImageRef.class, StyleClass.img);
		node2style.put(Code.class, StyleClass.code);
		node2style.put(HardLineBreak.class, StyleClass.br);

		// blocks
		node2lineStyle.put(FencedCodeBlock.class, StyleClass.pre);
		node2style.put(FencedCodeBlock.class, StyleClass.pre);
		node2lineStyle.put(IndentedCodeBlock.class, StyleClass.pre);
		node2style.put(IndentedCodeBlock.class, StyleClass.pre);
		node2style.put(BlockQuote.class, StyleClass.blockquote);
		node2style.put(AsideBlock.class, StyleClass.aside);

		// lists
		node2style.put(BulletList.class, StyleClass.ul);
		node2style.put(OrderedList.class, StyleClass.ol);
		node2style.put(BulletListItem.class, StyleClass.li);
		node2style.put(OrderedListItem.class, StyleClass.li);
		node2style.put(TaskListItem.class, StyleClass.li);

		// tables
		node2lineStyle.put(TableBlock.class, StyleClass.table);
		node2style.put(TableHead.class, StyleClass.thead);
		node2style.put(TableBody.class, StyleClass.tbody);
		node2style.put(TableRow.class, StyleClass.tr);

		// misc
		node2style.put(HtmlBlock.class, StyleClass.html);
		node2style.put(HtmlInline.class, StyleClass.html);
		node2style.put(Reference.class, StyleClass.reference);
		node2style.put(AbbreviationBlock.class, StyleClass.abbrdef);
		node2style.put(Abbreviation.class, StyleClass.abbr);
	}

	private final StyleClassedTextArea textArea;
	private ArrayList<StyleRange> styleRanges;
	private ArrayList<StyleRange> lineStyleRanges;

	static void highlight(StyleClassedTextArea textArea, Node astRoot, List<ExtraStyledRanges> extraStyledRanges) {
		assert Platform.isFxApplicationThread();

		assert textArea.getText().length() == textArea.getLength();
		new MarkdownSyntaxHighlighter(textArea).highlight(astRoot, extraStyledRanges);
	}

	private MarkdownSyntaxHighlighter(StyleClassedTextArea textArea) {
		this.textArea = textArea;
	}

	private void highlight(Node astRoot, List<ExtraStyledRanges> extraStyledRanges) {
		styleRanges = new ArrayList<>();
		lineStyleRanges = new ArrayList<>();

		// visit all nodes
		NodeVisitor visitor = new NodeVisitor(
			new VisitHandler<>(Heading.class, this::visit),
			new VisitHandler<>(BulletListItem.class, this::visit),
			new VisitHandler<>(OrderedListItem.class, this::visit),
			new VisitHandler<>(TaskListItem.class, this::visit),
			new VisitHandler<>(TableCell.class, this::visit))
		{
			@Override
			public void visit(Node node) {
				Class<? extends Node> nodeClass = node.getClass();

				StyleClass style = node2style.get(nodeClass);
				if (style != null)
					setStyleClass(node, style);

				StyleClass lineStyle = node2lineStyle.get(nodeClass);
				if (lineStyle != null)
					setLineStyleClass(node, lineStyle);

				VisitHandler<?> handler = myCustomHandlersMap.get(nodeClass);
				if (handler != null)
					handler.visit(node);

				visitChildren(node);
			}
		};
		visitor.visit(astRoot);

		// add extra styled ranges
		if (extraStyledRanges != null) {
			long extraStyleBits = 1L << StyleClass.values().length;
			for (ExtraStyledRanges extraStyledRange : extraStyledRanges) {
				for (Range extraRange : extraStyledRange.ranges) {
					addStyledRange(styleRanges, extraRange.start, extraRange.end, extraStyleBits);
				}
				extraStyleBits <<= 1;
			}

			// need to clear cache
			styleClassesCache.clear();
		}

		// set text styles
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		int textLength = textArea.getLength();
		if (textLength > 0) {
			int spanStart = 0;
			for (StyleRange range : styleRanges) {
				if (range.begin > spanStart)
					spansBuilder.add(Collections.emptyList(), range.begin - spanStart);
				spansBuilder.add(toStyleClasses(range.styleBits, extraStyledRanges), range.end - range.begin);
				spanStart = range.end;
			}
			if (spanStart < textLength)
				spansBuilder.add(Collections.emptyList(), textLength - spanStart);
		} else
			spansBuilder.add(Collections.emptyList(), 0);
		textArea.setStyleSpans(0, spansBuilder.create());

		// set line styles
		int start = 0;
		for (StyleRange range : lineStyleRanges) {
			if (range.begin > start)
				setParagraphStyle(start, range.begin, Collections.emptyList());
			setParagraphStyle(range.begin, range.end, toStyleClasses(range.styleBits, null));
			start = range.end;
		}
		int lineCount = textArea.getParagraphs().size();
		if (start < lineCount)
			setParagraphStyle(start, lineCount, Collections.emptyList());
	}

	private void setParagraphStyle(int start, int end, Collection<String> ps) {
		for (int i = start; i < end; i++) {
			Paragraph<Collection<String>, Collection<String>> paragraph = textArea.getParagraph(i);
			if (ps != paragraph.getParagraphStyle())
				setParagraphStyle(paragraph, i, ps);
		}
	}

	private void setParagraphStyle(Paragraph<?,?> paragraph, int paragraphIndex, Collection<String> paragraphStyle) {
		if (paragraphStyleField != null) {
			// because StyledTextArea.setParagraphStyle() is very very slow,
			// especially if invoked many times, we (try to) go the "short way"
			try {
				paragraphStyleField.set(paragraph, paragraphStyle);
				return;
			} catch (Exception ex) {
				// ignore
			}
		}

		textArea.setParagraphStyle(paragraphIndex, paragraphStyle);
	}

	private static Field paragraphStyleField;
	static {
		try {
			paragraphStyleField = Paragraph.class.getDeclaredField("paragraphStyle");
			paragraphStyleField.setAccessible(true);
		} catch (Exception e) {
			// ignore
		}
	}

	private Collection<String> toStyleClasses(long bits, List<ExtraStyledRanges> extraStyledRanges) {
		if (bits == 0)
			return Collections.emptyList();

		Collection<String> styleClasses = styleClassesCache.get(bits);
		if (styleClasses != null)
			return styleClasses;

		styleClasses = new ArrayList<>(1);
		for (StyleClass styleClass : StyleClass.values()) {
			if ((bits & (1L << styleClass.ordinal())) != 0)
				styleClasses.add(styleClass.name());
		}
		if (extraStyledRanges != null) {
			long extraStyleBits = 1L << StyleClass.values().length;
			for (ExtraStyledRanges extraStyledRange : extraStyledRanges) {
				if ((bits & extraStyleBits) != 0)
					styleClasses.add(extraStyledRange.styleClass);
				extraStyleBits <<= 1;
			}
		}
		styleClassesCache.put(bits, styleClasses);
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
	}

	private void visit(ListItem node) {
		setStyleClass(node.getOpeningMarker(), StyleClass.liopen);
	}

	private void visit(TaskListItem node) {
		setStyleClass(node.getOpeningMarker(), StyleClass.liopen);
		setStyleClass(node.getTaskOpeningMarker(), StyleClass.liopentask);
	}

	private void visit(TableCell node) {
		setStyleClass(node, node.isHeader() ?  StyleClass.th : StyleClass.td);
	}

	private void setStyleClass(Node node, StyleClass styleClass) {
		setStyleClass(node.getChars(), styleClass);
	}

	private void setStyleClass(BasedSequence sequence, StyleClass styleClass) {
		int start = sequence.getStartOffset();
		int end = sequence.getEndOffset();

		addStyledRange(styleRanges, start, end, styleClass);
	}

	private void setLineStyleClass(Node node, StyleClass styleClass) {
		int start = textArea.offsetToPosition(node.getStartOffset(), Bias.Forward).getMajor();
		int end = textArea.offsetToPosition(node.getEndOffset() - 1, Bias.Forward).getMajor() + 1;

		addStyledRange(lineStyleRanges, start, end, styleClass);
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
		long styleBits = 1L << styleClass.ordinal();
		addStyledRange(styleRanges, begin, end, styleBits);
	}

	private static void addStyledRange(ArrayList<StyleRange> styleRanges, int begin, int end, long styleBits) {
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

	//---- class ExtraStyledRanges --------------------------------------------

	static class ExtraStyledRanges {
		final String styleClass;
		final List<Range> ranges;

		ExtraStyledRanges(String styleClass, List<Range> ranges) {
			this.styleClass = styleClass;
			this.ranges = ranges;
		}
	}
}
