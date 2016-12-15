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

package org.markdownwriterfx.preview;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.control.IndexRange;
import javafx.scene.control.ScrollBar;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.markdownwriterfx.preview.MarkdownPreviewPane.Renderer;
import org.markdownwriterfx.util.Utils;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ast.NodeVisitor;
import com.vladsch.flexmark.util.sequence.BasedSequence;

/**
 * Markdown AST preview.
 * Prints the AST tree to a text area.
 *
 * @author Karl Tauber
 */
class ASTPreview
	implements MarkdownPreviewPane.Preview
{
	private final MarkdownPreviewPane previewPane;
	private PreviewStyledTextArea textArea;
	private VirtualizedScrollPane<StyleClassedTextArea> scrollPane;
	private ScrollBar vScrollBar;

	ASTPreview(MarkdownPreviewPane previewPane) {
		this.previewPane = previewPane;
	}

	private void createNodes() {
		textArea = new PreviewStyledTextArea();
		textArea.getStyleClass().add("ast-preview");
		textArea.getStylesheets().add("org/markdownwriterfx/prism.css");

		scrollPane = new VirtualizedScrollPane<>(textArea);
	}

	@Override
	public javafx.scene.Node getNode() {
		if (scrollPane == null)
			createNodes();
		return scrollPane;
	}

	@Override
	public void update(Renderer renderer, Path path) {
		oldSelectionStylesMap.clear();

		String ast = renderer.getAST();
		textArea.replaceText(ast, computeHighlighting(ast));

		selectionChanged(textArea.getSelection());
	}

	@Override
	public void scrollY(double value) {
		if (vScrollBar == null)
			vScrollBar = Utils.findVScrollBar(scrollPane);
		if (vScrollBar == null)
			return;

		double maxValue = vScrollBar.maxProperty().get();
		vScrollBar.setValue(maxValue * value);
	}

	//---- selection highlighting ---------------------------------------------

	private static final Collection<String> STYLE_SELECTION = Collections.singleton("selection");

	private final HashMap<Integer, StyleSpans<Collection<String>>> oldSelectionStylesMap = new HashMap<>();

	@Override
	public void selectionChanged(IndexRange range) {
		ArrayList<BasedSequence> sequences = findSequences(range.getStart(), range.getEnd());

		// restore old styles
		for (Map.Entry<Integer, StyleSpans<Collection<String>>> e : oldSelectionStylesMap.entrySet())
			textArea.setStyleSpans(e.getKey(), e.getValue());
		oldSelectionStylesMap.clear();

		// set new selection styles
		String text = textArea.getText();
		for (BasedSequence sequence : sequences) {
			String rangeStr = "[" + sequence.getStartOffset() + ", " + sequence.getEndOffset();
			int index = 0;
			while ((index = text.indexOf(rangeStr, index)) >= 0) {
				int endIndex = index + rangeStr.length() + 1;
				char after = text.charAt(endIndex - 1);
				if ((after == ']' || after == ',') && !oldSelectionStylesMap.containsKey(index)) {
					oldSelectionStylesMap.put(index, textArea.getStyleSpans(index, endIndex));
					textArea.setStyle(index, endIndex, STYLE_SELECTION);
				}
				index = endIndex;
			}
		}
	}

	private ArrayList<BasedSequence> findSequences(int startOffset, int endOffset) {
		ArrayList<BasedSequence> sequences = new ArrayList<>();
		NodeVisitor visitor = new NodeVisitor(Collections.emptyList()) {
			@Override
			public void visit(Node node) {
				if (isInSequence(startOffset, endOffset, node.getChars()))
					sequences.add(node.getChars());

				for (BasedSequence segment : node.getSegments()) {
					if (isInSequence(startOffset, endOffset, segment))
						sequences.add(segment);
				}

				visitChildren(node);
			}
		};
		visitor.visit(previewPane.getMarkdownAST());
		return sequences;
	}

	private boolean isInSequence(int start, int end, BasedSequence sequence) {
		if (end == start)
			end++;
		return start < sequence.getEndOffset() && end > sequence.getStartOffset();
	}

	//---- syntax highlighting ------------------------------------------------

	private static final Pattern PATTERN = Pattern.compile("(?m)^\\s*(\\w+)"
			+ "(?:(?<COMMONMARK>\\{(.*)\\})"
			+   "|(?<FLEXMARK>\\[.*\\]))");

	private static final Pattern COMMONMARK_ATTRIBUTES = Pattern.compile("(\\w+\\h*)(=)(\\h*[^,]*)");
	private static final Pattern FLEXMARK_ATTRIBUTES = Pattern.compile("(?:(\\w+:)|(\"[^\"]*\")|(\\d+)|([\\[\\],]))");

	// groups in PATTERN
	private static final int GROUP_NODE_NAME = 1;
	private static final int GROUP_COMMONMARK = 2;
	private static final int GROUP_COMMONMARK_ATTRS = 3;
	private static final int GROUP_FLEXMARK = 4;

	// groups in COMMONMARK_ATTRIBUTES
	private static final int GROUP_COMMONMARK_ATTR_NAME = 1;
	private static final int GROUP_COMMONMARK_EQUAL_SYMBOL = 2;
	private static final int GROUP_COMMONMARK_ATTR_VALUE = 3;

	// groups in FLEXMARK_ATTRIBUTES
	private static final int GROUP_FLEXMARK_ATTR_NAME = 1;
	private static final int GROUP_FLEXMARK_STRING = 2;
	private static final int GROUP_FLEXMARK_NUMBER = 3;
	private static final int GROUP_FLEXMARK_PUNCTATION = 4;

	private static final Collection<String> STYLE_PUNCTATION = Arrays.asList("punctuation", "token");
	private static final Collection<String> STYLE_NODE       = Arrays.asList("tag", "token");
	private static final Collection<String> STYLE_ATTR_NAME  = Arrays.asList("attr-name", "token");
	private static final Collection<String> STYLE_ATTR_VALUE = Arrays.asList("attr-value", "token");

	private static StyleSpans<Collection<String>> computeHighlighting(String text) {
		Matcher matcher = PATTERN.matcher(text);
		int lastKwEnd = 0;
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		while(matcher.find()) {
			spansBuilder.add(Collections.emptyList(), matcher.start(GROUP_NODE_NAME) - lastKwEnd);
			spansBuilder.add(STYLE_NODE, groupLength(matcher, GROUP_NODE_NAME));

			String str;
			if((str = matcher.group(GROUP_COMMONMARK_ATTRS)) != null) {
				String attributesText = str;

				spansBuilder.add(STYLE_PUNCTATION, matcher.start(GROUP_COMMONMARK_ATTRS) - matcher.start(GROUP_COMMONMARK));

				if(!attributesText.isEmpty()) {
					lastKwEnd = 0;

					Matcher amatcher = COMMONMARK_ATTRIBUTES.matcher(attributesText);
					while(amatcher.find()) {
						spansBuilder.add(STYLE_PUNCTATION, amatcher.start() - lastKwEnd);
						spansBuilder.add(STYLE_ATTR_NAME, groupLength(amatcher, GROUP_COMMONMARK_ATTR_NAME));
						spansBuilder.add(STYLE_PUNCTATION, groupLength(amatcher, GROUP_COMMONMARK_EQUAL_SYMBOL));
						spansBuilder.add(STYLE_ATTR_VALUE, groupLength(amatcher, GROUP_COMMONMARK_ATTR_VALUE));
						lastKwEnd = amatcher.end();
					}
					if(attributesText.length() > lastKwEnd)
						spansBuilder.add(Collections.emptyList(), attributesText.length() - lastKwEnd);
				}

				spansBuilder.add(STYLE_PUNCTATION, matcher.end(GROUP_COMMONMARK) - matcher.end(GROUP_COMMONMARK_ATTRS));
			} else if((str = matcher.group(GROUP_FLEXMARK)) != null) {
				String attributesText = str;
				if(!attributesText.isEmpty()) {
					lastKwEnd = 0;

					Matcher amatcher = FLEXMARK_ATTRIBUTES.matcher(attributesText);
					while(amatcher.find()) {
						if (amatcher.start() > lastKwEnd)
							spansBuilder.add(Collections.emptyList(), amatcher.start() - lastKwEnd);

						Collection<String> style = null;
						if (amatcher.group(GROUP_FLEXMARK_ATTR_NAME) != null)
							style = STYLE_ATTR_NAME;
						else if (amatcher.group(GROUP_FLEXMARK_STRING) != null)
							style = STYLE_ATTR_VALUE;
						else if (amatcher.group(GROUP_FLEXMARK_NUMBER) != null)
							style = STYLE_ATTR_VALUE;
						else if (amatcher.group(GROUP_FLEXMARK_PUNCTATION) != null)
							style = STYLE_PUNCTATION;
						else
							style = Collections.emptyList();
						spansBuilder.add(style, amatcher.end() - amatcher.start());

						lastKwEnd = amatcher.end();
					}
					if(attributesText.length() > lastKwEnd)
						spansBuilder.add(Collections.emptyList(), attributesText.length() - lastKwEnd);
				}
			}
			lastKwEnd = matcher.end();
		}
		spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
		return spansBuilder.create();
	}

	private static int groupLength(Matcher matcher, int group) {
		return matcher.end(group) - matcher.start(group);
	}
}
