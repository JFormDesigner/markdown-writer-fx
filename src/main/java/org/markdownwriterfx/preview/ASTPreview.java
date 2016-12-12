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
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.control.ScrollBar;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.markdownwriterfx.preview.MarkdownPreviewPane.Renderer;
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
	private final PreviewStyledTextArea textArea = new PreviewStyledTextArea();
	private final VirtualizedScrollPane<StyleClassedTextArea> scrollPane = new VirtualizedScrollPane<>(textArea);
	private ScrollBar vScrollBar;

	ASTPreview() {
		textArea.getStylesheets().add("org/markdownwriterfx/preview/HtmlSourcePreview.css");
	}

	@Override
	public javafx.scene.Node getNode() {
		return scrollPane;
	}

	@Override
	public void update(Renderer renderer, Path path) {
		String ast = renderer.getAST();
		textArea.replaceText(ast, computeHighlighting(ast));
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

	private static final Collection<String> STYLE_PUNCTATION = Collections.singleton("punctuation");
	private static final Collection<String> STYLE_NODE       = Collections.singleton("tag");
	private static final Collection<String> STYLE_ATTR_NAME  = Collections.singleton("attr-name");
	private static final Collection<String> STYLE_ATTR_VALUE = Collections.singleton("attr-value");

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
