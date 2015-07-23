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
import java.util.List;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.GlyphsDude;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

/**
 * Main window containing a tab pane in the center for file editors.
 *
 * @author Karl Tauber
 */
class MainWindow
{
	private Scene scene;
	private final BorderPane borderPane = new BorderPane();
	private final TabPane tabPane = new TabPane();

	public MainWindow() {
		borderPane.setPrefSize(800, 800);
		borderPane.setTop(new VBox(createMenuBar(), createToolBar()));
		borderPane.setCenter(tabPane);

		tabPane.setTabClosingPolicy(TabClosingPolicy.ALL_TABS);

		fileNew();
	}

	Scene getScene() {
		if(scene == null)
			scene = new Scene(borderPane);
		return scene;
	}

	private MenuBar createMenuBar() {
		// File menu
		MenuItem fileNewMenuItem = createMenuItem("New", "Shortcut+N", FILE_ALT, e -> fileNew());
		MenuItem fileOpenMenuItem = createMenuItem("Open...", "Shortcut+O", FOLDER_OPEN_ALT, e -> fileOpen());
		MenuItem fileSaveMenuItem = createMenuItem("Save", "Shortcut+S", FLOPPY_ALT, e -> fileSave());
		MenuItem fileCloseMenuItem = createMenuItem("Close", "Shortcut+W", null, e -> fileClose());
		MenuItem fileExitMenuItem = createMenuItem("Exit", null, null, e -> fileExit());

		Menu fileMenu = new Menu("File", null,
				fileNewMenuItem,
				fileOpenMenuItem,
				fileSaveMenuItem,
				fileCloseMenuItem,
				new SeparatorMenuItem(),
				fileExitMenuItem);

		// Help menu
		MenuItem helpAboutMenuItem = createMenuItem("About Markdown Writer FX", null, null, e -> helpAbout());

		Menu helpMenu = new Menu("Help", null,
				helpAboutMenuItem);

		return new MenuBar(fileMenu, helpMenu);
	}

	private ToolBar createToolBar() {
		Button fileNewButton = createToolBarButton(FILE_ALT, "New", "Shortcut+N", e -> fileNew());
		Button fileOpenButton = createToolBarButton(FOLDER_OPEN_ALT, "Open", "Shortcut+O", e -> fileOpen());
		Button fileSaveButton = createToolBarButton(FLOPPY_ALT, "Save", "Shortcut+S", e -> fileSave());

		return new ToolBar(
				fileNewButton,
				fileOpenButton,
				fileSaveButton);
	}

	private MenuItem createMenuItem(String text, String accelerator,
		GlyphIcons icon, EventHandler<ActionEvent> action)
	{
		MenuItem menuItem = new MenuItem(text);
		if(accelerator != null)
			menuItem.setAccelerator(KeyCombination.valueOf(accelerator));
		if(icon != null)
			menuItem.setGraphic(GlyphsDude.createIcon(icon));
		menuItem.setOnAction(action);
		return menuItem;
	}

	private Button createToolBarButton(GlyphIcons icon, String tooltip,
		String accelerator, EventHandler<ActionEvent> action)
	{
		Button button = new Button();
		button.setGraphic(GlyphsDude.createIcon(icon, "1.2em"));
		if(accelerator != null)
			tooltip = tooltip + " (" + KeyCombination.valueOf(accelerator).getDisplayText() + ')';
		button.setTooltip(new Tooltip(tooltip));
		button.setOnAction(action);
		return button;
	}

	private void fileNew() {
		Tab tab = new FileEditor(null).getTab();
		tabPane.getTabs().add(tab);
		tabPane.getSelectionModel().select(tab);
	}

	private void fileOpen() {
		FileChooser fileChooser = createFileChooser("Open Markdown File");
		List<File> selectedFiles = fileChooser.showOpenMultipleDialog(getScene().getWindow());
		if(selectedFiles == null)
			return;

		for (File file : selectedFiles) {
			Tab tab = new FileEditor(file.toPath()).getTab();
			tabPane.getTabs().add(tab);

			// select first file
			if(file == selectedFiles.get(0))
				tabPane.getSelectionModel().select(tab);
		}
	}

	private void fileSave() {
		FileEditor activeFileEditor = (FileEditor) tabPane.getSelectionModel().getSelectedItem().getUserData();
		if (activeFileEditor == null)
			return;

		if (activeFileEditor.getPath() == null) {
			FileChooser fileChooser = createFileChooser("Save Markdown File");
			File file = fileChooser.showSaveDialog(getScene().getWindow());
			if (file == null)
				return;

			activeFileEditor.setPath(file.toPath());
		}

		activeFileEditor.save();
	}

	private void fileClose() {
		Tab tab = tabPane.getSelectionModel().getSelectedItem();
		if(tab == null)
			return;

		Event event = new Event(tab,tab,Tab.TAB_CLOSE_REQUEST_EVENT);
		Event.fireEvent(tab, event);
		if(event.isConsumed())
			return;

		tabPane.getTabs().remove(tab);
		if(tab.getOnClosed() != null)
			Event.fireEvent(tab, new Event(Tab.CLOSED_EVENT));
	}

	private void fileExit() {
		Platform.exit();
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

	private void helpAbout() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("About");
		alert.setHeaderText("Markdown Writer FX");
		alert.setContentText("Copyright (c) 2015 Karl Tauber <karl at jformdesigner dot com>\nAll rights reserved.");

		alert.showAndWait();
	}
}
