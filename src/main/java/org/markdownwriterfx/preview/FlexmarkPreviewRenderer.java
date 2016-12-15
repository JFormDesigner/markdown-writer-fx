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

import org.markdownwriterfx.options.MarkdownExtensions;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.util.options.Attributes;

/**
 * flexmark-java preview.
 *
 * @author Karl Tauber
 */
class FlexmarkPreviewRenderer
	implements MarkdownPreviewPane.Renderer
{
	private Node astRoot;

	private String htmlPreview;
	private String htmlSource;
	private String ast;

	@Override
	public void update(String markdownText, Node astRoot) {
		if (this.astRoot == astRoot)
			return;

		this.astRoot = astRoot;

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

	private String toHtml(boolean source) {
		if (astRoot == null)
			return "";

		HtmlRenderer.Builder builder = HtmlRenderer.builder()
				.extensions(MarkdownExtensions.getFlexmarkExtensions());
		if (!source)
			builder.attributeProviderFactory(new MyAttributeProvider.Factory());
		return builder.build().render(astRoot);
	}

	private String printTree() {
		if (astRoot == null)
			return "";

		StringBuilder buf = new StringBuilder(100);
		printNode(buf, "", astRoot);
		return buf.toString().replace(Node.SPLICE, "...");
	}

	private void printNode(StringBuilder buf, String indent, Node node) {
		buf.append(indent);
		node.astString(buf, true);
		buf.append('\n');

		indent += "    ";
		for (Node child = node.getFirstChild(); child != null; child = child.getNext())
			printNode(buf, indent, child);
	}

	//---- class MyAttributeProvider ------------------------------------------

	private static class MyAttributeProvider
		implements AttributeProvider
	{
		private static class Factory
			extends IndependentAttributeProviderFactory
		{
			@Override
			public AttributeProvider create(NodeRendererContext context) {
				return new MyAttributeProvider();
			}
		}

		@Override
		public void setAttributes(Node node, AttributablePart part, Attributes attributes) {
			attributes.addValue("data-pos", node.getStartOffset() + ":" + node.getEndOffset());
		}
	}
}
