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

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * Tab pane for file editors.
 *
 * @author Karl Tauber
 */
class FileEditorTabPane
{
	private final MainWindow mainWindow;
	private final TabPane tabPane;
	private final ReadOnlyObjectWrapper<FileEditor> activeFileEditor = new ReadOnlyObjectWrapper<>();
	private final ReadOnlyBooleanWrapper activeFileEditorModified = new ReadOnlyBooleanWrapper();

	FileEditorTabPane(MainWindow mainWindow) {
		this.mainWindow = mainWindow;

		tabPane = new TabPane();
		tabPane.setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
		tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
			if (newTab != null) {
				activeFileEditor.set((FileEditor) newTab.getUserData());
				activeFileEditorModified.bind(activeFileEditor.get().modifiedProperty());
			} else {
				activeFileEditor.set(null);
				activeFileEditorModified.unbind();
				activeFileEditorModified.set(false);
			}
		});
	}

	Node getNode() {
		return tabPane;
	}

	ReadOnlyObjectProperty<FileEditor> activeFileEditorProperty() {
		return activeFileEditor.getReadOnlyProperty();
	}

	ReadOnlyBooleanProperty activeFileEditorModifiedProperty() {
		return activeFileEditorModified.getReadOnlyProperty();
	}

	private FileEditor createFileEditor(Path path) {
		FileEditor fileEditor = new FileEditor(path);
		return fileEditor;
	}

	FileEditor newEditor() {
		FileEditor fileEditor = createFileEditor(null);
		Tab tab = fileEditor.getTab();
		tabPane.getTabs().add(tab);
		tabPane.getSelectionModel().select(tab);
		return fileEditor;
	}

	FileEditor[] openEditor() {
		FileChooser fileChooser = createFileChooser("Open Markdown File");
		List<File> selectedFiles = fileChooser.showOpenMultipleDialog(mainWindow.getScene().getWindow());
		if (selectedFiles == null)
			return null;

		FileEditor[] fileEditors = new FileEditor[selectedFiles.size()];
		for (int i = 0; i < selectedFiles.size(); i++) {
			fileEditors[i] = createFileEditor(selectedFiles.get(i).toPath());

			Tab tab = fileEditors[i].getTab();
			tabPane.getTabs().add(tab);

			// select first file
			if (i == 0)
				tabPane.getSelectionModel().select(tab);
		}
		return fileEditors;
	}

	void saveEditor(FileEditor fileEditor) {
		if (fileEditor == null)
			return;

		if (fileEditor.getPath() == null) {
			FileChooser fileChooser = createFileChooser("Save Markdown File");
			File file = fileChooser.showSaveDialog(mainWindow.getScene().getWindow());
			if (file == null)
				return;

			fileEditor.setPath(file.toPath());
		}

		fileEditor.save();
	}

	void closeEditor(FileEditor fileEditor) {
		if (fileEditor == null)
			return;

		Tab tab = fileEditor.getTab();

		Event event = new Event(tab,tab,Tab.TAB_CLOSE_REQUEST_EVENT);
		Event.fireEvent(tab, event);
		if (event.isConsumed())
			return;

		tabPane.getTabs().remove(tab);
		if (tab.getOnClosed() != null)
			Event.fireEvent(tab, new Event(Tab.CLOSED_EVENT));
	}

	private FileChooser createFileChooser(String title) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(title);
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("Markdown Files", "*.md", "*.markdown", "*.txt"),
				new ExtensionFilter("All Files", "*.*"));
		fileChooser.setInitialDirectory(new File("."));
		return fileChooser;
	}
}
