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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import org.markdownwriterfx.Messages;
import org.markdownwriterfx.util.Utils;

/**
 * A combo box that contains recently opened projects and allows switching active project.
 *
 * @author Karl Tauber
 */
class ProjectsComboBox
	extends ComboBox<File>
{
	private static final File OPEN_FOLDER = new File("");
	private static final Comparator<File> PROJECT_COMPARATOR = (f1, f2) -> f1.getPath().compareToIgnoreCase(f2.getPath());

	private boolean doNotHidePopupOnce;

	ProjectsComboBox() {
		getStyleClass().add("projects-combo-box");
		setVisibleRowCount(20);
		setButtonCell(new ProjectButtonCell());
		setCellFactory(listView -> new ProjectListCell());

		// let this control grow horizontally
		setMaxWidth(Double.MAX_VALUE);

		// update tooltip
		valueProperty().addListener((observer, oldProject, newProject) -> {
			setTooltip((newProject != null) ? new Tooltip(newProject.getAbsolutePath()) : null);
		});

		// add items
		ObservableList<File> projects = ProjectManager.getProjects();
		projects.sort(PROJECT_COMPARATOR);
		getItems().add(OPEN_FOLDER);
		getItems().addAll(projects);

		// set active project
		setValue(ProjectManager.getActiveProject());

		// listen to selection changes
		getSelectionModel().selectedItemProperty().addListener((observer, oldProject, newProject) -> {
			if (newProject == OPEN_FOLDER) {
				Platform.runLater(() -> {
					// closing last active project automatically selects this item
					// --> activate first project
					if (oldProject != null && !getItems().contains(oldProject)) {
						ProjectManager.setActiveProject((getItems().size() > 1) ? getItems().get(1) : null);
						return;
					}

					getSelectionModel().select(oldProject);
					ProjectManager.openProject(getScene().getWindow());
				});
			} else
				ProjectManager.setActiveProject(newProject);
		});

		// listen to projects changes and update combo box
		projects.addListener((ListChangeListener<File>) change -> {
			while (change.next()) {
				if (change.wasAdded()) {
					for (File addedProject : change.getAddedSubList())
						Utils.addSorted(getItems(), addedProject, PROJECT_COMPARATOR);
				}
				if (change.wasRemoved())
					getItems().removeAll(change.getRemoved());
			}
		});

		// listen to active project change and update combo box value
		ProjectManager.activeProjectProperty().addListener((observer, oldProject, newProject) -> {
			setValue(newProject);
		});
	}

	@Override
	public void hide() {
		if (doNotHidePopupOnce) {
			doNotHidePopupOnce = false;
			return;
		}

		super.hide();
	}

	//---- class ProjectButtonCell --------------------------------------------

	private class ProjectButtonCell
		extends ListCell<File>
	{
		@Override
		protected void updateItem(File item, boolean empty) {
			super.updateItem(item, empty);

			setText((!empty && item != null) ? buildUniqueName(item) : null);
		}

		private String buildUniqueName(File item) {
			String name = item.getName();

			// find other projects with same name
			List<File> projects = getItems().stream()
				.filter(f -> !f.equals(item) && f.getName().equals(name))
				.collect(Collectors.toList());
			if (projects.isEmpty())
				return name;

			// there are more than one project with the same name
			// --> go up folder hierarchy and find a unique name
			File itemParent = item.getParentFile();
			while (itemParent != null) {
				// parent of projects
				projects = projects.stream()
					.map(f -> f.getParentFile())
					.filter(f -> f != null)
					.collect(Collectors.toList());

				String n = itemParent.getName();
				if (n.isEmpty())
					break;

				// find unique parent name
				long equalCount = projects.stream()
					.filter(f -> f.getName().equals(n))
					.count();
				if (equalCount == 0)
					return name + " [" + itemParent.getName() + "]";

				itemParent = itemParent.getParentFile();
			}

			// fallback: append path
			return name + " - " + item.getParent();
		}
	}

	//---- class ProjectListCell ----------------------------------------------

	private class ProjectListCell
		extends ListCell<File>
	{
		private final StackPane closeButton = new StackPane();

		ProjectListCell() {
			closeButton.getStyleClass().add("close-project-button");
			closeButton.setPrefSize(16, 16);
			closeButton.setOnMousePressed( event -> {
				event.consume();
				doNotHidePopupOnce = true;
				ProjectManager.getProjects().remove(getItem());
			});
		}

		@Override
		protected void updateItem(File item, boolean empty) {
			super.updateItem(item, empty);

			// add/remove separator below "open folder" item
			if (!empty && item == OPEN_FOLDER && ProjectsComboBox.this.getItems().size() > 1)
				getStyleClass().add("open-project");
			else
				getStyleClass().remove("open-project");

			String text = null;
			Node graphic = null;
			if (!empty && item != null) {
				text = (item == OPEN_FOLDER)
					? Messages.get("ProjectsComboBox.openProject")
					: item.getAbsolutePath();

				graphic = closeButton;
				closeButton.setVisible(item != OPEN_FOLDER);
			}
			setText(text);
			setGraphic(graphic);
		}
	}
}
