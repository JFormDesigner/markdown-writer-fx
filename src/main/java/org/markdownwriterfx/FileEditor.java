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

package org.markdownwriterfx;

import java.nio.file.Path;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import org.markdownwriterfx.editor.MarkdownEditorPane;
import org.markdownwriterfx.preview.MarkdownPreviewPane;

/**
 * Editor for a single file.
 *
 * @author Karl Tauber
 */
class FileEditor
{
	private final Tab tab = new Tab();
	private MarkdownEditorPane markdownEditorPane;
	private MarkdownPreviewPane markdownPreviewPane;

	FileEditor(Path path) {
		// avoid that this is GCed
		tab.setUserData(this);

		tab.setText((path != null) ? path.getFileName().toString() : "New Document");
		tab.setTooltip((path != null) ? new Tooltip(path.toString()) : null);

		tab.setOnSelectionChanged(e -> {
			if(tab.isSelected())
				activated();
		});
	}

	Tab getTab() {
		return tab;
	}

	private void activated() {
		if(tab.getContent() != null)
			return;

		// load file and create UI when the tab becomes visible the first time

		markdownEditorPane = new MarkdownEditorPane();
		markdownPreviewPane = new MarkdownPreviewPane();

		//TODO
		markdownEditorPane.setMarkdown("# h1\n\n## h2\n\nsome **bold** text\n\n* ul 1\n* ul 2\n* ul 3");

		// bind preview to editor
		markdownPreviewPane.markdownASTProperty().bind(markdownEditorPane.markdownASTProperty());

		SplitPane splitPane = new SplitPane(markdownEditorPane.getNode(), markdownPreviewPane.getNode());
		tab.setContent(splitPane);
	}
}
