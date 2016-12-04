/*
 * Copyright (c) 2016 Karl Tauber <karl at jformdesigner dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.DirectoryChooser;

import org.fxmisc.livedirs.LiveDirs;
import org.tbee.javafx.scene.layout.fxml.MigPane;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;

/* *
 * @author Karl Tauber
 */
public class WorkspacePane {
	private final FileEditorTabPane fileEditorTabPane;
	private MigPane pane;
	private ComboBox workspacesComboBox;
	private Button openDirButton;
	private Button settingsButton;
	private LiveDirs liveDirs;
	private TreeView<Path> treeView;
	private Path lastDirPath;

	public WorkspacePane(FileEditorTabPane fileEditorTabPane) {
		this.fileEditorTabPane = fileEditorTabPane;
	}

	private void init() {
		initComponents();

		String lastDirectory = MarkdownWriterFXApp.getState().get("lastDirectory", null);
		File dir = new File((lastDirectory != null) ? lastDirectory : ".");
		Path dirPath = dir.toPath();
		setDir(dirPath);
	}

	private void initComponents() {
		pane = new MigPane();
		pane.setCols("[grow,fill][fill][fill]");
		pane.setRows("[][grow,fill]");

		workspacesComboBox = new ComboBox();
		openDirButton = new Button();
		openDirButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.FOLDER_OPEN_ALT, "1.2em"));
		openDirButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				DirectoryChooser chooser = new DirectoryChooser();
				chooser.setTitle("JavaFX Projects");
				chooser.setInitialDirectory(lastDirPath.toFile());
				File chosenDir = chooser.showDialog(null);
				if (chosenDir != null) {
					setDir(chosenDir.toPath());
				}
			}
		});
		settingsButton = new Button();
		settingsButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.COG, "1.2em"));

		treeView = new TreeView<>();
		treeView.setCellFactory(param -> new PathTreeCell());
		treeView.setShowRoot(false);
		treeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
			@Override
			public void changed(ObservableValue observable, Object oldValue, Object newValue) {
				TreeItem<Path> selectedItem = (TreeItem<Path>) newValue;
				if (selectedItem != null) {
					File selectedFile = selectedItem.getValue().toFile();
					if (selectedFile.isFile()) {
						List<File> selectedFiles = new ArrayList<>();
						selectedFiles.add(selectedItem.getValue().toFile());
						fileEditorTabPane.openEditors(selectedFiles, 0);
					}
				}
			}
		});

		pane.add(workspacesComboBox, "cell 0 0");
		pane.add(openDirButton, "cell 1 0");
		pane.add(settingsButton, "cell 2 0");
		pane.add(treeView, "cell 0 1 3 1");
	}

	public Node getNode() {
		if (pane == null)
			init();
		return pane;
	}

	public void setDir(Path dirPath) {
		try {
			lastDirPath = dirPath;
			liveDirs = new LiveDirs(null);
			liveDirs.addTopLevelDirectory(lastDirPath);
			treeView.setRoot(liveDirs.model().getRoot());
			MarkdownWriterFXApp.getState().put("lastDirectory", dirPath.toString());
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public void dispose() {
		if (liveDirs != null) {
			liveDirs.dispose();
		}
	}

	private static class PathTreeCell extends TreeCell<Path> {
		@Override
		protected void updateItem(Path item, boolean empty) {
			super.updateItem(item, empty);

			String text = null;
			Node graphic = null;
			if (!empty) {
				text = item.getFileName().toString();
				graphic = GlyphsDude
						.createIcon(Files.isDirectory(item) ? FontAwesomeIcon.FOLDER_ALT : FontAwesomeIcon.FILE_ALT);
			}
			setText(text);
			setGraphic(graphic);
		}
	}
}
