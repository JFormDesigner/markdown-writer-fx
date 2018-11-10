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
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import org.markdownwriterfx.MarkdownWriterFXApp;
import org.markdownwriterfx.Messages;
import org.markdownwriterfx.util.Utils;

/**
 * Project manager.
 *
 * @author Karl Tauber
 */
public class ProjectManager
{
	public static final ProjectManager INSTANCE = new ProjectManager();

	private static final String KEY_PROJECTS = "projects";
	private static final String KEY_PATH = "path";
	private static final String KEY_ACTIVE_PROJECT = "active";
	private static final String KEY_LAST_PROJECT_DIRECTORY = "lastDirectory";

	ProjectManager() {
		Preferences state = getProjectsState();

		// load recent projects
		projects.addAll(getRecentProjects());

		// save active project on change
		activeProjectProperty().addListener((observer, oldProject, newProject) -> {
			Utils.putPrefs(state, KEY_ACTIVE_PROJECT, (newProject != null) ? newProject.getAbsolutePath() : null, null);

			// add to recent projects
			if (newProject != null && !projects.contains(newProject))
				projects.add(newProject);
		});

		// save recent projects on change
		projects.addListener((ListChangeListener<File>) change -> {
			while (change.next()) {
				if (change.wasAdded()) {
					for (File f : change.getAddedSubList()) {
						getProjectState(f);
					}
				}
				if (change.wasRemoved()) {
					for (File f : change.getRemoved())
						removeProjectState(f);
				}
			}
		});

		// initialize active project
		String activeProjectName = state.get(KEY_ACTIVE_PROJECT, null);
		if (activeProjectName != null)
			setActiveProject(new File(activeProjectName));
		if (getActiveProject() == null && !projects.isEmpty())
			setActiveProject(projects.get(0));
	}

	// 'activeProject' property
	private final ObjectProperty<File> activeProject = new SimpleObjectProperty<>();
	public File getActiveProject() { return activeProject.get(); }
	public void setActiveProject(File activeProject) { this.activeProject.set(activeProject); }
	public ObjectProperty<File> activeProjectProperty() { return activeProject; }

	// 'projects' property
	private final ObservableList<File> projects = FXCollections.observableArrayList();
	ObservableList<File> getProjects() { return projects; }

	public void openProject(Window ownerWindow) {
		DirectoryChooser fileChooser = new DirectoryChooser();
		fileChooser.setTitle(Messages.get("ProjectManager.openChooser.title"));

		Preferences state = getProjectsState();
		String lastProjectDirectory = state.get(KEY_LAST_PROJECT_DIRECTORY, null);
		File file = new File((lastProjectDirectory != null) ? lastProjectDirectory : ".");
		if (!file.isDirectory())
			file = new File(".");
		fileChooser.setInitialDirectory(file);

		File selectedFile = fileChooser.showDialog(ownerWindow);
		if (selectedFile == null)
			return;

		state.put(KEY_LAST_PROJECT_DIRECTORY, selectedFile.getAbsolutePath());

		setActiveProject(selectedFile);
	}

	public Preferences getActiveProjectState() {
		return getProjectState(getActiveProject());
	}

	public Preferences getProjectState(File project) {
		return getProjectState(project, true);
	}

	private Preferences getProjectState(File project, boolean create) {
		Preferences state = getProjectsState();

		try {
			String[] childrenNames = state.childrenNames();
			for (String childName : childrenNames) {
				Preferences child = state.node(childName);
				String path = child.get(KEY_PATH, null);
				if (path != null && project.equals(new File(path)))
					return child;
			}

			if (!create)
				return null;

			int lastID = 0;
			for (String childName : childrenNames) {
				try {
					int childID = Integer.parseInt(childName);
					if (childID > lastID)
						lastID = childID;
				} catch (NumberFormatException ex) {
					// ignore
				}
			}

			Preferences newNode = state.node(String.valueOf(lastID + 1));
			newNode.put(KEY_PATH, project.getAbsolutePath());
			return newNode;
		} catch (BackingStoreException ex) {
			// ignore
			ex.printStackTrace();
			return null;
		}
	}

	private void removeProjectState(File project) {
		Preferences projectState = getProjectState(project, false);
		if (projectState != null) {
			try {
				projectState.removeNode();
			} catch (BackingStoreException ex) {
				// ignore
				ex.printStackTrace();
			}
		}
	}

	private List<File> getRecentProjects() {
		Preferences state = getProjectsState();
		ArrayList<File> projects = new ArrayList<>();

		try {
			String[] childrenNames = state.childrenNames();
			for (String childName : childrenNames) {
				Preferences child = state.node(childName);
				String path = child.get(KEY_PATH, null);
				if (path != null)
					projects.add(new File(path));
			}
		} catch (BackingStoreException ex) {
			// ignore
			ex.printStackTrace();
		}

		return projects;
	}

	private Preferences getProjectsState() {
		return MarkdownWriterFXApp.getState().node(KEY_PROJECTS);
	}
}
