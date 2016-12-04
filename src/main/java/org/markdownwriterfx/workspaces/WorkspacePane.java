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

package org.markdownwriterfx.workspaces;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;

import org.fxmisc.livedirs.LiveDirs;
import org.tbee.javafx.scene.layout.fxml.MigPane;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;

/**
 * TODO
 *
 * @author Karl Tauber
 */
public class WorkspacePane
{
	private LiveDirs liveDirs;

	public WorkspacePane() {
	}

	public void dispose() {
		if (liveDirs != null)
			liveDirs.dispose();
	}

	public Node getNode() {
		if (pane == null)
			init();
		return pane;
	}

	private void init() {
		initComponents();

		settingsButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.COG, "1.2em"));
		treeView.setCellFactory(param -> new PathTreeCell());

		try {
			liveDirs = new LiveDirs(null);
			Path dir = Paths.get(".").toAbsolutePath();
			liveDirs.addTopLevelDirectory(dir);

			treeView.setRoot(liveDirs.model().getRoot());
		} catch(IOException ex) {
			ex.printStackTrace();//TODO
		}
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		pane = new MigPane();
		workspacesComboBox = new ComboBox();
		settingsButton = new Button();
		treeView = new TreeView<>();

		//======== pane ========
		{
			pane.setCols("[grow,fill][fill]");
			pane.setRows("[][grow,fill]");
			pane.add(workspacesComboBox, "cell 0 0");
			pane.add(settingsButton, "cell 1 0");

			//---- treeView ----
			treeView.setShowRoot(false);
			pane.add(treeView, "cell 0 1 2 1");
		}
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private MigPane pane;
	private ComboBox workspacesComboBox;
	private Button settingsButton;
	private TreeView<Path> treeView;
	// JFormDesigner - End of variables declaration  //GEN-END:variables

	//---- class PathTreeCell -------------------------------------------------

	private static class PathTreeCell
		extends TreeCell<Path>
	{
		@Override
		protected void updateItem(Path item, boolean empty) {
			super.updateItem(item, empty);

			String text = null;
			Node graphic = null;
			if (!empty) {
				text = item.getFileName().toString();
				graphic = GlyphsDude.createIcon(Files.isDirectory(item)
					? FontAwesomeIcon.FOLDER_ALT
					: FontAwesomeIcon.FILE_ALT);
			}
			setText(text);
			setGraphic(graphic);
		}
	}
}
