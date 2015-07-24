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

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
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
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
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
	private final Scene scene;
	private final FileEditorTabPane fileEditorTabPane;

	MainWindow() {
		fileEditorTabPane = new FileEditorTabPane(this);

		BorderPane borderPane = new BorderPane();
		borderPane.setPrefSize(800, 800);
		borderPane.setTop(new VBox(createMenuBar(), createToolBar()));
		borderPane.setCenter(fileEditorTabPane.getNode());

		scene = new Scene(borderPane);
		scene.windowProperty().addListener((observable, oldWindow, newWindow) -> {
			newWindow.setOnCloseRequest(e -> {
				if (!fileEditorTabPane.closeAllEditors())
					e.consume();
			});
		});
	}

	Scene getScene() {
		return scene;
	}

	private ReadOnlyObjectProperty<FileEditor> activeFileEditor() {
		return fileEditorTabPane.activeFileEditorProperty();
	}

	private ReadOnlyBooleanProperty activeFileEditorModified() {
		return fileEditorTabPane.activeFileEditorModifiedProperty();
	}

	private MenuBar createMenuBar() {
		// File menu
		MenuItem fileNewMenuItem = createMenuItem("New", "Shortcut+N", FILE_ALT, e -> fileNew());
		MenuItem fileOpenMenuItem = createMenuItem("Open...", "Shortcut+O", FOLDER_OPEN_ALT, e -> fileOpen());
		MenuItem fileCloseMenuItem = createMenuItem("Close", "Shortcut+W", null, e -> fileClose());
		MenuItem fileSaveMenuItem = createMenuItem("Save", "Shortcut+S", FLOPPY_ALT, e -> fileSave());
		MenuItem fileExitMenuItem = createMenuItem("Exit", null, null, e -> fileExit());

		fileCloseMenuItem.disableProperty().bind(activeFileEditor().isNull());
		fileSaveMenuItem.disableProperty().bind(Bindings.not(activeFileEditorModified()));

		Menu fileMenu = new Menu("File", null,
				fileNewMenuItem,
				fileOpenMenuItem,
				new SeparatorMenuItem(),
				fileCloseMenuItem,
				new SeparatorMenuItem(),
				fileSaveMenuItem,
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

		fileSaveButton.disableProperty().bind(Bindings.not(activeFileEditorModified()));

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
		button.setFocusTraversable(false);
		button.setOnAction(action);
		return button;
	}

	Alert createAlert(AlertType alertType, String title,
		String contentTextFormat, Object... contentTextArgs)
	{
		Alert alert = new Alert(alertType);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(String.format(contentTextFormat, contentTextArgs));
		alert.initOwner(getScene().getWindow());
		return alert;
	}

	//---- File menu ----------------------------------------------------------

	private void fileNew() {
		fileEditorTabPane.newEditor();
	}

	private void fileOpen() {
		fileEditorTabPane.openEditor();
	}

	private void fileClose() {
		fileEditorTabPane.closeEditor(activeFileEditor().get());
	}

	private void fileSave() {
		fileEditorTabPane.saveEditor(activeFileEditor().get());
	}

	private void fileExit() {
		Window window = scene.getWindow();
		Event.fireEvent(window, new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
	}

	//---- Help menu ----------------------------------------------------------

	private void helpAbout() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("About");
		alert.setHeaderText("Markdown Writer FX");
		alert.setContentText("Copyright (c) 2015 Karl Tauber <karl at jformdesigner dot com>\nAll rights reserved.");
		alert.initOwner(getScene().getWindow());

		alert.showAndWait();
	}
}
