/*
 * Copyright (c) 2018 Karl Tauber <karl at jformdesigner dot com>
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

package org.markdownwriterfx.projects;

import java.io.File;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import org.markdownwriterfx.Messages;

/**
 * A combo box that contains recently opened projects and allows switching active project.
 *
 * @author Karl Tauber
 */
class ProjectsComboBox
	extends ComboBox<File>
{
	private static final File OPEN_FOLDER = new File("");

	ProjectsComboBox() {
		getStyleClass().add("projects-combo-box");
		setVisibleRowCount(20);
		setCellFactory(listView -> new ProjectListCell());

		// let this control grow horizontally
		setMaxWidth(Double.MAX_VALUE);

		// add items
		ObservableList<File> projects = ProjectManager.INSTANCE.getProjects();
		getItems().add(OPEN_FOLDER);
		getItems().addAll(projects);

		// set active project
		setValue(ProjectManager.INSTANCE.getActiveProject());

		// listen to selection changes
		getSelectionModel().selectedItemProperty().addListener((observer, oldProject, newProject) -> {
			if (newProject == OPEN_FOLDER) {
				Platform.runLater(() -> {
					getSelectionModel().select(oldProject);
					ProjectManager.INSTANCE.openProject(getScene().getWindow());
				});
			} else
				ProjectManager.INSTANCE.setActiveProject(newProject);
		});

		// listen to projects changes and update combo box
		projects.addListener((ListChangeListener<File>) change -> {
			while (change.next()) {
				if (change.wasAdded())
					getItems().addAll(change.getAddedSubList());
				if (change.wasRemoved())
					getItems().removeAll(change.getRemoved());
			}
		});

		// listen to active project change and update combo box value
		ProjectManager.INSTANCE.activeProjectProperty().addListener((observer, oldProject, newProject) -> {
			setValue(newProject);
		});
	}

	//---- class ProjectListCell ----------------------------------------------

	private class ProjectListCell
		extends ListCell<File>
	{
		@Override
		protected void updateItem(File item, boolean empty) {
			super.updateItem(item, empty);

			// add/remove separator below "open folder" item
			if (!empty && item == OPEN_FOLDER && ProjectsComboBox.this.getItems().size() > 1)
				getStyleClass().add("open-project");
			else
				getStyleClass().remove("open-project");

			String text = null;
			if (!empty && item != null) {
				text = (item == OPEN_FOLDER)
					? Messages.get("ProjectsComboBox.openProject")
					: item.getAbsolutePath();
			}
			setText(text);
		}
	}
}
