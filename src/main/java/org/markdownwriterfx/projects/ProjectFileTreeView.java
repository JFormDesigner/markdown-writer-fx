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
import java.nio.file.Path;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.markdownwriterfx.controls.FileTreeItem;
import org.markdownwriterfx.controls.FileTreeView;
import org.markdownwriterfx.util.Utils;

/**
 * A tree view of directories and files of a project.
 *
 * @author Karl Tauber
 */
class ProjectFileTreeView
	extends FileTreeView
{
	private static final String KEY_SELECTION = "selection";
	private static final String KEY_EXPANDED = "expanded";

	private boolean inSetRoot;

	ProjectFileTreeView() {
		setShowRoot(false);

		getSelectionModel().selectedItemProperty().addListener((observer, oldSelectedItem, newSelectedItem) -> {
			saveSelection();
		});

		projectChanged(ProjectManager.INSTANCE.getActiveProject());
		ProjectManager.INSTANCE.activeProjectProperty().addListener((observer, oldProject, newProject) -> {
			projectChanged(newProject);
		});
	}

	private void projectChanged(File activeProject) {
		if (activeProject == null) {
			setRoot(null);
			return;
		}

		FileTreeItem newRoot = new FileTreeItem(activeProject);

		inSetRoot = true;
		try {
			setRoot(newRoot);
		} finally {
			inSetRoot = false;
		}

		loadExpanded();
		loadSelection();

		newRoot.addEventHandler(TreeItem.branchExpandedEvent(), event -> saveExpanded());
		newRoot.addEventHandler(TreeItem.branchCollapsedEvent(), event -> saveExpanded());
	}

	private void loadExpanded() {
		File project = getRoot().getValue();
		Preferences projectState = ProjectManager.INSTANCE.getProjectState(project);
		if (projectState == null)
			return;

		String[] expanded = Utils.getPrefsStrings(projectState, KEY_EXPANDED);
		List<File> expandedDirectories = Stream.of(expanded)
			.map(relativePath -> new File(project, relativePath))
			.collect(Collectors.toList());
		setExpandedDirectories(expandedDirectories);
	}

	private void saveExpanded() {
		Platform.runLater(() -> {
			File project = getRoot().getValue();
			Preferences projectState = ProjectManager.INSTANCE.getProjectState(project);
			if (projectState == null)
				return;

			List<File> expandedDirectories = getExpandedDirectories();
			Path projectPath = project.toPath();
			Utils.putPrefsStrings(projectState, KEY_EXPANDED, expandedDirectories.stream()
				.filter(f -> f != project)
				.map(f -> projectPath.relativize(f.toPath()).toString())
				.toArray(String[]::new));
		});
	}

	private void loadSelection() {
		Preferences projectState = getProjectState();
		if (projectState == null)
			return;

		String path = projectState.get(KEY_SELECTION, null);
		if (path != null) {
			File f = new File(path);
			List<TreeItem<File>> items = findItems(item -> item.getValue().equals(f));
			if (!items.isEmpty())
				getSelectionModel().select(items.get(0));
		}
	}

	private void saveSelection() {
		if (inSetRoot)
			return;

		Preferences projectState = getProjectState();
		if (projectState == null)
			return;

		TreeItem<File> selectedItem = getSelectionModel().getSelectedItem();
		String path = (selectedItem != null) ? selectedItem.getValue().getAbsolutePath() : null;
		Utils.putPrefs(projectState, KEY_SELECTION, path, null);
	}

	private Preferences getProjectState() {
		TreeItem<File> root = getRoot();
		return (root != null)
			? ProjectManager.INSTANCE.getProjectState(root.getValue())
			: null;
	}
}
