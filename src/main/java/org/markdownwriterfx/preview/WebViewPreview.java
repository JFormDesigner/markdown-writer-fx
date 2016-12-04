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
import javafx.scene.web.WebView;
import org.markdownwriterfx.preview.MarkdownPreviewPane.Renderer;

/**
 * WebView preview.
 *
 * @author Karl Tauber
 */
class WebViewPreview
	implements MarkdownPreviewPane.Preview
{
	private final WebView webView = new WebView();
	private int lastScrollX;
	private int lastScrollY;

	WebViewPreview() {
		webView.setFocusTraversable(false);
	}

	@Override
	public javafx.scene.Node getNode() {
		return webView;
	}

	@Override
	public void update(Renderer renderer, Path path) {
		if (!webView.getEngine().getLoadWorker().isRunning()) {
			// get window.scrollX and window.scrollY from web engine,
			// but only no worker is running (in this case the result would be zero)
			Object scrollXobj = webView.getEngine().executeScript("window.scrollX");
			Object scrollYobj = webView.getEngine().executeScript("window.scrollY");
			lastScrollX = (scrollXobj instanceof Number) ? ((Number)scrollXobj).intValue() : 0;
			lastScrollY = (scrollYobj instanceof Number) ? ((Number)scrollYobj).intValue() : 0;
		}

		String base = (path != null)
				? ("<base href=\"" + path.getParent().toUri().toString() + "\">\n")
				: "";
		String scrollScript = (lastScrollX > 0 || lastScrollY > 0)
				? ("  onload='window.scrollTo("+lastScrollX+", "+lastScrollY+");'")
				: "";

		webView.getEngine().loadContent(
			"<!DOCTYPE html>\n"
			+ "<html>\n"
			+ "<head>\n"
			+ "<link rel=\"stylesheet\" href=\"" + getClass().getResource("markdownpad-github.css") + "\">\n"
			+ base
			+ "</head>\n"
			+ "<body" + scrollScript + ">\n"
			+ renderer.getHtml()
			+ "</body>\n"
			+ "</html>");
	}

	@Override
	public void scrollY(double value) {
		webView.getEngine().executeScript(
			"window.scrollTo(0, (document.body.scrollHeight - window.innerHeight) * "+value+");");
	}
}
