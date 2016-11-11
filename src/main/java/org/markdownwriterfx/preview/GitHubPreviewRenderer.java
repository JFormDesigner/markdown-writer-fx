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

import java.io.IOException;
import java.io.Reader;
import org.apache.commons.io.IOUtils;
import org.kohsuke.github.GitHub;
import com.vladsch.flexmark.ast.Node;

/**
 * GitHub preview.
 *
 * @author Karl Tauber
 */
class GitHubPreviewRenderer
	implements MarkdownPreviewPane.Renderer
{
	private String markdownText;
	private String html;

	@Override
	public void update(String markdownText, Node astRoot) {
		if (this.markdownText == markdownText)
			return;

		this.markdownText = markdownText;

		html = null;
	}

	@Override
	public String getHtml() {
		if (html == null)
			html = toHtml();
		return html;
	}

	@Override
	public String getAST() {
		return "not supported";
	}

	private String toHtml() {
		if (markdownText == null)
			return "";

		// TODO execute asynchron
		// TODO better CSS; e.g. https://github.com/sindresorhus/github-markdown-css

		try {
			GitHub gitHub = GitHub.connectAnonymously();
			Reader reader = gitHub.renderMarkdown((markdownText != null) ? markdownText : "");
			String html = IOUtils.toString(reader);
			reader.close();
			return html;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "<b>Error: " + e.getMessage() + "</b>";
		}
	}
}
