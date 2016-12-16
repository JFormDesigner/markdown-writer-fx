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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.control.IndexRange;
import javafx.scene.control.ScrollBar;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.markdownwriterfx.preview.MarkdownPreviewPane.PreviewContext;
import org.markdownwriterfx.preview.MarkdownPreviewPane.Renderer;
import org.markdownwriterfx.util.Range;
import org.markdownwriterfx.util.Utils;

/**
 * Markdown AST preview.
 * Prints the AST tree to a text area.
 *
 * @author Karl Tauber
 */
class ASTPreview
	implements MarkdownPreviewPane.Preview
{
	private PreviewStyledTextArea textArea;
	private VirtualizedScrollPane<StyleClassedTextArea> scrollPane;
	private ScrollBar vScrollBar;

	ASTPreview() {
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
	public void update(PreviewContext context, Renderer renderer) {
		oldSelectionStylesMap.clear();

		String ast = renderer.getAST();
		textArea.replaceText(ast, computeHighlighting(ast));

		editorSelectionChanged(context, context.getEditorSelection());
	}

	@Override
	public void scrollY(PreviewContext context, double value) {
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
	public void editorSelectionChanged(PreviewContext context, IndexRange range) {
		List<Range> sequences = context.getRenderer().findSequences(range.getStart(), range.getEnd());

		// restore old styles
		for (Map.Entry<Integer, StyleSpans<Collection<String>>> e : oldSelectionStylesMap.entrySet())
			textArea.setStyleSpans(e.getKey(), e.getValue());
		oldSelectionStylesMap.clear();

		// set new selection styles
		String text = textArea.getText();
		for (Range sequence : sequences) {
			String rangeStr = "[" + sequence.start + ", " + sequence.end;
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

	//---- syntax highlighting ------------------------------------------------

	private static final Pattern PATTERN = Pattern.compile("(?m)^\\s*(\\w+)(\\[.*$)");
	private static final Pattern ATTRIBUTES = Pattern.compile("(?:(\\w+[=:])|(\"[^\"]*\")|(\\d+)|([\\[\\],]))");

	// groups in PATTERN
	private static final int GROUP_NODE_NAME = 1;
	private static final int GROUP_ATTRS = 2;

	// groups in ATTRIBUTES
	private static final int GROUP_ATTR_NAME = 1;
	private static final int GROUP_ATTR_STRING = 2;
	private static final int GROUP_ATTR_NUMBER = 3;
	private static final int GROUP_ATTR_PUNCTATION = 4;

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

			String attributesText = matcher.group(GROUP_ATTRS);
			if(!attributesText.isEmpty()) {
				lastKwEnd = 0;

				Matcher amatcher = ATTRIBUTES.matcher(attributesText);
				while(amatcher.find()) {
					if (amatcher.start() > lastKwEnd)
						spansBuilder.add(Collections.emptyList(), amatcher.start() - lastKwEnd);

					Collection<String> style = null;
					int length = amatcher.end() - amatcher.start();
					if (amatcher.group(GROUP_ATTR_NAME) != null) {
						style = STYLE_ATTR_NAME;
						length--;
					} else if (amatcher.group(GROUP_ATTR_STRING) != null)
						style = STYLE_ATTR_VALUE;
					else if (amatcher.group(GROUP_ATTR_NUMBER) != null)
						style = STYLE_ATTR_VALUE;
					else if (amatcher.group(GROUP_ATTR_PUNCTATION) != null)
						style = STYLE_PUNCTATION;
					else
						style = Collections.emptyList();
					spansBuilder.add(style, length);
					if (style == STYLE_ATTR_NAME)
						spansBuilder.add(STYLE_PUNCTATION, 1);

					lastKwEnd = amatcher.end();
				}
				if(attributesText.length() > lastKwEnd)
					spansBuilder.add(Collections.emptyList(), attributesText.length() - lastKwEnd);
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
