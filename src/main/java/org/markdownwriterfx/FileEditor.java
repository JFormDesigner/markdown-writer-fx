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
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;
import org.fxmisc.undo.UndoManager;
import org.markdownwriterfx.editor.MarkdownEditorPane;
import org.markdownwriterfx.options.Options;
import org.markdownwriterfx.preview.MarkdownPreviewPane;
import org.markdownwriterfx.preview.MarkdownPreviewPane.Type;

/**
 * Editor for a single file.
 *
 * @author Karl Tauber
 */
class FileEditor
{
	private static final long MAX_FILE_SIZE = 500_000;

	private final MainWindow mainWindow;
	private final FileEditorTabPane fileEditorTabPane;
	private final Tab tab = new Tab();
	private SplitPane splitPane;
	private MarkdownEditorPane markdownEditorPane;
	private MarkdownPreviewPane markdownPreviewPane;
	private long lastModified;

	@SuppressWarnings("unchecked")
	FileEditor(MainWindow mainWindow, FileEditorTabPane fileEditorTabPane, Path path) {
		this.mainWindow = mainWindow;
		this.fileEditorTabPane = fileEditorTabPane;
		this.path.set(path);

		// avoid that this is GCed
		tab.setUserData(this);

		this.path.addListener((observable, oldPath, newPath) -> updateTab());
		modified.addListener((observable, oldModified, newModified) -> updateTab());
		updateTab();

		@SuppressWarnings("rawtypes")
		ChangeListener previewTypeListener = (observable, oldValue, newValue) -> updatePreviewType();
		ChangeListener<Boolean> stageFocusedListener = (observable, oldValue, newValue) -> {
			if (newValue)
				reload();
		};

		tab.setOnSelectionChanged(e -> {
			if(tab.isSelected()) {
				Platform.runLater(() -> activated());

				Options.markdownRendererProperty().addListener(previewTypeListener);
				fileEditorTabPane.previewVisible.addListener(previewTypeListener);
				fileEditorTabPane.htmlSourceVisible.addListener(previewTypeListener);
				fileEditorTabPane.markdownAstVisible.addListener(previewTypeListener);
				fileEditorTabPane.externalVisible.addListener(previewTypeListener);

				mainWindow.stageFocusedProperty.addListener(stageFocusedListener);
			} else {
				Options.markdownRendererProperty().removeListener(previewTypeListener);
				fileEditorTabPane.previewVisible.removeListener(previewTypeListener);
				fileEditorTabPane.htmlSourceVisible.removeListener(previewTypeListener);
				fileEditorTabPane.markdownAstVisible.removeListener(previewTypeListener);
				fileEditorTabPane.externalVisible.removeListener(previewTypeListener);

				mainWindow.stageFocusedProperty.removeListener(stageFocusedListener);
			}
		});
	}

	Tab getTab() {
		return tab;
	}

	MarkdownEditorPane getEditor() {
		return markdownEditorPane;
	}

	// 'editor' property
	private final ObjectProperty<MarkdownEditorPane> editor = new SimpleObjectProperty<>();
	ReadOnlyObjectProperty<MarkdownEditorPane> editorProperty() { return editor; }

	// 'path' property
	private final ObjectProperty<Path> path = new SimpleObjectProperty<>();
	Path getPath() { return path.get(); }
	void setPath(Path path) { this.path.set(path); }
	ObjectProperty<Path> pathProperty() { return path; }

	// 'readOnly' property
	private final ReadOnlyBooleanWrapper readOnly = new ReadOnlyBooleanWrapper();
	boolean isReadOnly() { return readOnly.get(); }
	ReadOnlyBooleanProperty readOnlyProperty() { return readOnly.getReadOnlyProperty(); }

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
		tab.setText((path != null) ? path.getFileName().toString() : Messages.get("FileEditor.untitled"));
		tab.setTooltip((path != null) ? new Tooltip(path.toString()) : null);
		tab.setGraphic(isModified() ? new Text("*") : null);
	}

	private boolean updatePreviewTypePending;
	private void updatePreviewType() {
		if (markdownPreviewPane == null)
			return;

		// avoid too many (and useless) runLater() invocations
		if (updatePreviewTypePending)
			return;
		updatePreviewTypePending = true;

		Platform.runLater(() -> {
			updatePreviewTypePending = false;

			MarkdownPreviewPane.Type previewType = getPreviewType();

			markdownPreviewPane.setRendererType(Options.getMarkdownRenderer());
			markdownPreviewPane.setType(previewType);

			// add/remove previewPane from splitPane
			ObservableList<Node> splitItems = splitPane.getItems();
			Node previewPane = markdownPreviewPane.getNode();
			if (previewType != Type.None) {
				if (!splitItems.contains(previewPane))
					splitItems.add(previewPane);
			} else
				splitItems.remove(previewPane);
		});
	}

	private MarkdownPreviewPane.Type getPreviewType() {
		MarkdownPreviewPane.Type previewType = Type.None;
		if (fileEditorTabPane.previewVisible.get())
			previewType = MarkdownPreviewPane.Type.Web;
		else if (fileEditorTabPane.htmlSourceVisible.get())
			previewType = MarkdownPreviewPane.Type.Source;
		else if (fileEditorTabPane.markdownAstVisible.get())
			previewType = MarkdownPreviewPane.Type.Ast;
		else if (fileEditorTabPane.externalVisible.get() && MarkdownPreviewPane.hasExternalPreview())
		    previewType = MarkdownPreviewPane.Type.External;
		return previewType;
	}

	private void activated() {
		if( tab.getTabPane() == null || !tab.isSelected())
			return; // tab is already closed or no longer active

		if (tab.getContent() != null) {
			reload();
			updatePreviewType();
			markdownEditorPane.requestFocus();
			return;
		}

		// load file and create UI when the tab becomes visible the first time

		markdownEditorPane = new MarkdownEditorPane();
		markdownPreviewPane = new MarkdownPreviewPane();

		markdownEditorPane.pathProperty().bind(path);

		load();

		// clear undo history after first load
		markdownEditorPane.getUndoManager().forgetHistory();

		// bind preview to editor
		markdownPreviewPane.pathProperty().bind(pathProperty());
		markdownPreviewPane.markdownTextProperty().bind(markdownEditorPane.markdownTextProperty());
		markdownPreviewPane.markdownASTProperty().bind(markdownEditorPane.markdownASTProperty());
		markdownPreviewPane.editorSelectionProperty().bind(markdownEditorPane.selectionProperty());
		markdownPreviewPane.scrollYProperty().bind(markdownEditorPane.scrollYProperty());

		// bind properties
		readOnly.bind(markdownEditorPane.readOnlyProperty());

		// bind the editor undo manager to the properties
		UndoManager<?> undoManager = markdownEditorPane.getUndoManager();
		modified.bind(Bindings.not(undoManager.atMarkedPositionProperty()));
		canUndo.bind(undoManager.undoAvailableProperty());
		canRedo.bind(undoManager.redoAvailableProperty());

		splitPane = new SplitPane(markdownEditorPane.getNode());
		if (getPreviewType() != MarkdownPreviewPane.Type.None)
			splitPane.getItems().add(markdownPreviewPane.getNode());
		tab.setContent(splitPane);

		updatePreviewType();
		markdownEditorPane.requestFocus();

		// update 'editor' property
		editor.set(markdownEditorPane);
	}

	void load() {
		Path path = this.path.get();
		if (path == null || markdownEditorPane == null)
			return;

		lastModified = path.toFile().lastModified();

		try {
			String markdown = null;
			boolean readOnly = false;

			long fileSize = Files.size(path);

			if (fileSize > MAX_FILE_SIZE) {
				markdown = Messages.get("FileEditor.tooLarge", fileSize, MAX_FILE_SIZE);
				readOnly = true;
			} else {
				// load file
				byte[] bytes = Files.readAllBytes(path);

				// decode file
				if (Options.getEncoding() != null) {
					try {
						markdown = new String(bytes, Options.getEncoding());
					} catch (UnsupportedEncodingException ex) {
						// fallback
						markdown = new String(bytes);
					}
				} else
					markdown = new String(bytes);

				// check whether this is a binary file
				if (markdown.indexOf(0) >= 0) {
					markdown = Messages.get("FileEditor.binary", fileSize);
					readOnly = true;
				}
			}

			markdownEditorPane.setReadOnly(readOnly);
			markdownEditorPane.setMarkdown(markdown);
			markdownEditorPane.getUndoManager().mark();
		} catch (IOException ex) {
			Alert alert = mainWindow.createAlert(AlertType.ERROR,
				Messages.get("FileEditor.loadFailed.title"),
				Messages.get("FileEditor.loadFailed.message"), path, ex.getMessage());
			alert.showAndWait();
		}
	}

	boolean save() {
		if (Options.isFormatOnSave())
			markdownEditorPane.getSmartEdit().format(false);

		String markdown = markdownEditorPane.getMarkdown();

		byte[] bytes;
		if (Options.getEncoding() != null) {
			try {
				bytes = markdown.getBytes(Options.getEncoding());
			} catch (UnsupportedEncodingException ex) {
				// fallback
				bytes = markdown.getBytes();
			}
		} else
			bytes = markdown.getBytes();

		try {
			Files.write(path.get(), bytes);
			lastModified = path.get().toFile().lastModified();
			markdownEditorPane.getUndoManager().mark();
			return true;
		} catch (IOException ex) {
			Alert alert = mainWindow.createAlert(AlertType.ERROR,
				Messages.get("FileEditor.saveFailed.title"),
				Messages.get("FileEditor.saveFailed.message"), path.get(), ex.getMessage());
			alert.showAndWait();
			return false;
		}
	}

	private void reload() {
		Path path = this.path.get();
		if (path == null || lastModified == path.toFile().lastModified())
			return;
		lastModified = path.toFile().lastModified();

		// check whether file has been removed
		if (!Files.exists(path)) {
			if (isModified()) {
				Alert alert = mainWindow.createAlert(AlertType.WARNING,
					Messages.get("FileEditor.removedAlert.title"),
					Messages.get("FileEditor.removedAlert.message", path));
				ButtonType saveButtonType = new ButtonType(Messages.get("FileEditor.removedAlert.saveButton"), ButtonData.OK_DONE);
				alert.getButtonTypes().setAll(saveButtonType, ButtonType.CLOSE);

				ButtonType result = alert.showAndWait().get();
				if (result == saveButtonType) {
					fileEditorTabPane.saveEditorAs(this);
					return;
				}
			}

			// close editor
			Platform.runLater(() -> fileEditorTabPane.closeEditor(this, false));
			return;
		}

		if( isModified() ) {
			Alert alert = mainWindow.createAlert(AlertType.WARNING,
				Messages.get("FileEditor.reloadAlert.title"),
				Messages.get("FileEditor.reloadAlert.message", path));
			alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

			ButtonType result = alert.showAndWait().get();
			if (result != ButtonType.YES)
				return;
		}

		load();
	}
}
