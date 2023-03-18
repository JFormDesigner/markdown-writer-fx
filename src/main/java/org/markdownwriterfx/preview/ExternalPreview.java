/*
 * Copyright (c) 2017 Karl Tauber <karl at jformdesigner dot com>
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

import java.util.Iterator;
import java.util.ServiceLoader;

import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;

import org.markdownwriterfx.Messages;
import org.markdownwriterfx.addons.PreviewViewAddon;
import org.markdownwriterfx.preview.MarkdownPreviewPane.PreviewContext;
import org.markdownwriterfx.preview.MarkdownPreviewPane.Renderer;
import org.markdownwriterfx.util.Addons;

/**
 * External preview (provided by addon).
 *
 * @author Karl Tauber
 */
class ExternalPreview
	implements MarkdownPreviewPane.Preview
{
	private static final boolean hasExternalPreview =
		ServiceLoader.load( PreviewViewAddon.class, Addons.getAddonsClassLoader() ).iterator().hasNext();

	private PreviewViewAddon previewView;

	ExternalPreview() {
		// Not using a static field for service loader here because each instance of this class
		// requires a new instance of PreviewViewAddon.
		// This allows PreviewViewAddon implementations to store information in fields.
		ServiceLoader<PreviewViewAddon> addons = ServiceLoader.load( PreviewViewAddon.class, Addons.getAddonsClassLoader() );
		Iterator<PreviewViewAddon> it = addons.iterator();
		if (it.hasNext())
			previewView = it.next();
	}

	static boolean hasExternalPreview() {
		return hasExternalPreview;
	}

	@Override
	public javafx.scene.Node getNode() {
		if (previewView != null)
			return previewView.getNode();
		else
			return new Label(Messages.get("ExternalPreview.notAvailable"));
	}

	@Override
	public void update(PreviewContext context, Renderer renderer) {
		if (previewView != null)
			previewView.update(context.getMarkdownText(), context.getPath());
	}

	@Override
	public void scrollY(PreviewContext context, double value) {
		if (previewView != null)
			previewView.scrollY(value);
	}

	@Override
	public void editorSelectionChanged(PreviewContext context, IndexRange range) {
		if (previewView != null)
			previewView.editorSelectionChanged(range);
	}
}
