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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;
import org.fxmisc.undo.UndoManager;
import org.markdownwriterfx.editor.MarkdownEditorPane;
import org.markdownwriterfx.preview.MarkdownPreviewPane;

/**
 * Editor for a single file.
 *
 * @author Karl Tauber
 */
class FileEditor
{
	private final MainWindow mainWindow;
	private final Tab tab = new Tab();
	private MarkdownEditorPane markdownEditorPane;
	private MarkdownPreviewPane markdownPreviewPane;

	FileEditor(MainWindow mainWindow, Path path) {
		this.mainWindow = mainWindow;
		this.path.set(path);

		// avoid that this is GCed
		tab.setUserData(this);

		this.path.addListener((observable, oldPath, newPath) -> updateTab());
		this.modified.addListener((observable, oldPath, newPath) -> updateTab());
		updateTab();

		tab.setOnSelectionChanged(e -> {
			if(tab.isSelected())
				Platform.runLater(() -> activated());
		});
	}

	Tab getTab() {
		return tab;
	}

	// 'path' property
	private final ObjectProperty<Path> path = new SimpleObjectProperty<>();
	Path getPath() { return path.get(); }
	void setPath(Path path) { this.path.set(path); }
	ObjectProperty<Path> pathProperty() { return path; }

	// 'modified' property
	private final ReadOnlyBooleanWrapper modified = new ReadOnlyBooleanWrapper();
	boolean isModified() { return modified.get(); }
	ReadOnlyBooleanProperty modifiedProperty() { return modified.getReadOnlyProperty(); }

	// 'canUndo' property
	private final BooleanProperty canUndo = new SimpleBooleanProperty();
	BooleanProperty canUndoProperty() { return canUndo; }

	// 'canRedo' property
	private final BooleanProperty canRedo = new SimpleBooleanProperty();
	BooleanProperty canRedoProperty() { return canRedo; }

	private void updateTab() {
		Path path = this.path.get();
		tab.setText((path != null) ? path.getFileName().toString() : "Untitled");
		tab.setTooltip((path != null) ? new Tooltip(path.toString()) : null);
		tab.setGraphic(isModified() ? new Text("*") : null);
	}

	private void activated() {
		if( tab.getTabPane() == null || !tab.isSelected())
			return; // tab is already closed or no longer active

		if (tab.getContent() != null) {
			markdownEditorPane.requestFocus();
			return;
		}

		// load file and create UI when the tab becomes visible the first time

		markdownEditorPane = new MarkdownEditorPane();
		markdownPreviewPane = new MarkdownPreviewPane();

		markdownEditorPane.installEditorShortcuts(mainWindow.getEditorShortcuts());

		load();

		// clear undo history after first load
		markdownEditorPane.getUndoManager().forgetHistory();

		// bind preview to editor
		markdownPreviewPane.markdownASTProperty().bind(markdownEditorPane.markdownASTProperty());
		markdownPreviewPane.scrollYProperty().bind(markdownEditorPane.scrollYProperty());

		// bind the editor undo manager to the properties
		UndoManager undoManager = markdownEditorPane.getUndoManager();
		modified.bind(Bindings.not(undoManager.atMarkedPositionProperty()));
		canUndo.bind(undoManager.undoAvailableProperty());
		canRedo.bind(undoManager.redoAvailableProperty());

		SplitPane splitPane = new SplitPane(markdownEditorPane.getNode(), markdownPreviewPane.getNode());
		tab.setContent(splitPane);

		markdownEditorPane.requestFocus();
	}

	void load() {
		Path path = this.path.get();
		if (path == null)
			return;

		try {
			String markdown = new String(Files.readAllBytes(path));
			markdownEditorPane.setMarkdown(markdown);
			markdownEditorPane.getUndoManager().mark();
		} catch (IOException ex) {
			Alert alert = mainWindow.createAlert(AlertType.ERROR, "Load",
				"Failed to load '%s'.\n\nReason: %s", path, ex.getMessage());
			alert.showAndWait();
		}
	}

	boolean save() {
		String markdown = markdownEditorPane.getMarkdown();
		try {
			Files.write(path.get(), markdown.getBytes());
			markdownEditorPane.getUndoManager().mark();
			return true;
		} catch (IOException ex) {
			Alert alert = mainWindow.createAlert(AlertType.ERROR, "Save",
				"Failed to save '%s'.\n\nReason: %s", path.get(), ex.getMessage());
			alert.showAndWait();
			return false;
		}
	}

	void undo() {
		if (markdownEditorPane != null)
			markdownEditorPane.getUndoManager().undo();
	}

	void redo() {
		if (markdownEditorPane != null)
			markdownEditorPane.getUndoManager().redo();
	}
}
