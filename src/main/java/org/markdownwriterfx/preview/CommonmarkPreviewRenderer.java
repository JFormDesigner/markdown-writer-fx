/*
 * Copyright (c) 2016 Karl Tauber <karl at jformdesigner dot com>
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
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Image;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.SourceSpan;
import org.commonmark.node.Text;
import org.commonmark.node.Visitor;
import org.commonmark.parser.IncludeSourceSpans;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.AttributeProviderContext;
import org.commonmark.renderer.html.AttributeProviderFactory;
import org.commonmark.renderer.html.HtmlRenderer;
import org.markdownwriterfx.addons.PreviewRendererAddon;
import org.markdownwriterfx.options.MarkdownExtensions;
import org.markdownwriterfx.util.CommonmarkSourcePositions;
import org.markdownwriterfx.util.Range;

/**
 * commonmark-java preview.
 *
 * @author Karl Tauber
 */
class CommonmarkPreviewRenderer
	implements MarkdownPreviewPane.Renderer
{
	private static final ServiceLoader<PreviewRendererAddon> addons = ServiceLoader.load(PreviewRendererAddon.class);

	private String markdownText;
	private com.vladsch.flexmark.util.ast.Node flexAstRoot;
	private Path path;
	private Node astRoot;
	private CommonmarkSourcePositions sourcePositions;
	private String htmlPreview;
	private String htmlSource;
	private String ast;

	@Override
	public void update(String markdownText, com.vladsch.flexmark.util.ast.Node astRoot, Path path) {
		assert markdownText != null;
		assert astRoot != null;

		if (this.flexAstRoot == astRoot)
			return;

		this.markdownText = markdownText;
		this.flexAstRoot = astRoot;
		this.path = path;

		this.astRoot = null;
		sourcePositions = null;
		htmlPreview = null;
		htmlSource = null;
		ast = null;
	}

	@Override
	public String getHtml(boolean source) {
		if (source) {
			if (htmlSource == null)
				htmlSource = toHtml(true);
			return htmlSource;
		} else {
			if (htmlPreview == null)
				htmlPreview = toHtml(false);
			return htmlPreview;
		}
	}

	@Override
	public String getAST() {
		if (ast == null)
			ast = printTree();
		return ast;
	}

	@Override
	public List<Range> findSequences(int startOffset, int endOffset) {
		ArrayList<Range> sequences = new ArrayList<>();

		Node astRoot = toAstRoot();
		if (astRoot == null)
			return sequences;

		Visitor visitor = new AbstractVisitor() {
			@Override
			protected void visitChildren(Node node) {
				Range range = toSourcePositions().get(node);
				if (range != null && isInRange(startOffset, endOffset, range))
					sequences.add(range);

				super.visitChildren(node);
			}
		};
		astRoot.accept(visitor);
		return sequences;
	}

	private boolean isInRange(int start, int end, Range range) {
		if (end == start)
			end++;
		return start < range.end && end > range.start;
	}

	private Node parseMarkdown(String text) {
		Parser parser = Parser.builder()
				.extensions(MarkdownExtensions.getCommonmarkExtensions())
				.includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
				.build();
		return parser.parse(text);
	}

	private Node toAstRoot() {
		if (astRoot == null)
			astRoot = parseMarkdown(markdownText);
		return astRoot;
	}

	private CommonmarkSourcePositions toSourcePositions() {
		if (sourcePositions == null)
			sourcePositions = new CommonmarkSourcePositions(markdownText, toAstRoot());
		return sourcePositions;
	}

	private String toHtml(boolean source) {
		Node astRoot;
		if (addons.iterator().hasNext()) {
			String text = markdownText;

		    for (PreviewRendererAddon addon : addons)
	            text = addon.preParse(text, path);

		    astRoot = parseMarkdown(text);
		} else {
			// no addons --> use cached AST
			astRoot = toAstRoot();
		}

		if (astRoot == null)
			return "";

		HtmlRenderer.Builder builder = HtmlRenderer.builder()
				.extensions(MarkdownExtensions.getCommonmarkExtensions());
		if (!source)
			builder.attributeProviderFactory(new MyAttributeProvider());
		String html = builder.build().render(astRoot);

        for (PreviewRendererAddon addon : addons)
            html = addon.postRender(html, path);

        return html;
	}

	private String printTree() {
		Node astRoot = toAstRoot();
		if (astRoot == null)
			return "";

		StringBuilder buf = new StringBuilder(100);
		printNode(buf, "", astRoot);
		return buf.toString();
	}

	private void printNode(StringBuilder buf, String indent, Node node) {
		buf.append(indent).append(node.getClass().getSimpleName()).append('[');
		Range range = toSourcePositions().get(node);
		if (range != null)
			buf.append(range.start).append(", ").append(range.end);
		buf.append(']');
		List<SourceSpan> sourceSpans = node.getSourceSpans();
		for (SourceSpan sourceSpan : sourceSpans) {
			buf.append(" [")
				.append(sourceSpan.getLineIndex() + 1)
				.append(':')
				.append(sourceSpan.getColumnIndex())
				.append(':')
				.append(sourceSpan.getLength())
				.append(']');
		}
		printAttributes(buf, node);
		buf.append('\n');

		indent += "    ";
		for (Node child = node.getFirstChild(); child != null; child = child.getNext())
			printNode(buf, indent, child);
	}

	private void printAttributes(StringBuilder buf, Node node) {
		if (node instanceof Text)
			printAttribute(buf, "literal", ((Text)node).getLiteral());
		else if (node instanceof Code)
			printAttribute(buf, "literal", ((Code)node).getLiteral());
		else if (node instanceof IndentedCodeBlock)
			printAttribute(buf, "literal", ((IndentedCodeBlock)node).getLiteral());
		else if (node instanceof FencedCodeBlock)
			printAttribute(buf, "literal", ((FencedCodeBlock)node).getLiteral());
		else if (node instanceof HtmlBlock)
			printAttribute(buf, "literal", ((HtmlBlock)node).getLiteral());
		else if (node instanceof HtmlInline)
			printAttribute(buf, "literal", ((HtmlInline)node).getLiteral());
		else if (node instanceof Link) {
			printAttribute(buf, "destination", ((Link)node).getDestination());
			printAttribute(buf, "title", ((Link)node).getTitle());
		} else if (node instanceof Image) {
			printAttribute(buf, "destination", ((Image)node).getDestination());
			printAttribute(buf, "title", ((Image)node).getTitle());
		} else if (node instanceof Heading)
			printAttribute(buf, "level", ((Heading)node).getLevel());
	}

	private void printAttribute(StringBuilder buf, String name, String value) {
		if (value == null)
			return;

		int fromIndex = buf.length();
		if (value.length() > 30) {
			// limit to 30 characters
			com.vladsch.flexmark.util.ast.Node.segmentSpanChars(buf, 0, 1, name,
				value.substring(0, 30), "...", "");
		} else
			com.vladsch.flexmark.util.ast.Node.segmentSpanChars(buf, 0, 1, name, value);

		// change 'name:[0, 1, value]' to 'name=value'
		String posStr = "[0, 1, ";
		int posIndex = buf.indexOf(posStr, fromIndex);
		if (posIndex >= 0) {
			buf.delete(posIndex, posIndex + posStr.length());
			buf.setCharAt(posIndex - 1, '=');
			buf.setLength(buf.length() - 1);
		}
	}

	private void printAttribute(StringBuilder buf, String name, Object value) {
		buf.append(' ').append(name).append('=').append(value);
	}

	//---- class MyAttributeProvider ------------------------------------------

	private class MyAttributeProvider
		implements AttributeProviderFactory, AttributeProvider
	{
		@Override
		public AttributeProvider create(AttributeProviderContext context) {
			return this;
		}

		@Override
		public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
			Range range = toSourcePositions().get(node);
			if (range != null)
				attributes.put("data-pos", range.start + ":" + range.end);
		}
	}
}
