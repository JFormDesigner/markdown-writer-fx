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
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
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
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.fxmisc.wellbehaved.event.EventHandlerHelper;
import org.fxmisc.wellbehaved.event.EventPattern;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.GlyphsDude;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import java.util.function.Function;

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

	private MenuBar createMenuBar() {
		// File menu
		MenuItem fileNewMenuItem = createMenuItem("New", "Shortcut+N", FILE_ALT, e -> fileNew());
		MenuItem fileOpenMenuItem = createMenuItem("Open...", "Shortcut+O", FOLDER_OPEN_ALT, e -> fileOpen());
		MenuItem fileCloseMenuItem = createMenuItem("Close", "Shortcut+W", null, e -> fileClose());
		MenuItem fileCloseAllMenuItem = createMenuItem("Close All", null, null, e -> fileCloseAll());
		MenuItem fileSaveMenuItem = createMenuItem("Save", "Shortcut+S", FLOPPY_ALT, e -> fileSave());
		MenuItem fileSaveAllMenuItem = createMenuItem("Save All", "Shortcut+Shift+S", null, e -> fileSaveAll());
		MenuItem fileExitMenuItem = createMenuItem("Exit", null, null, e -> fileExit());

		BooleanBinding activeFileEditorIsNull = fileEditorTabPane.activeFileEditorProperty().isNull();
		fileCloseMenuItem.disableProperty().bind(activeFileEditorIsNull);
		fileCloseAllMenuItem.disableProperty().bind(activeFileEditorIsNull);
		fileSaveMenuItem.disableProperty().bind(createActiveBooleanProperty(FileEditor::modifiedProperty).not());
		fileSaveAllMenuItem.disableProperty().bind(Bindings.not(fileEditorTabPane.anyFileEditorModifiedProperty()));

		Menu fileMenu = new Menu("File", null,
				fileNewMenuItem,
				fileOpenMenuItem,
				new SeparatorMenuItem(),
				fileCloseMenuItem,
				fileCloseAllMenuItem,
				new SeparatorMenuItem(),
				fileSaveMenuItem,
				fileSaveAllMenuItem,
				new SeparatorMenuItem(),
				fileExitMenuItem);

		// Edit menu
		MenuItem editUndoMenuItem = createMenuItem("Undo", "Shortcut+Z", UNDO, e -> editUndo());
		MenuItem editRedoMenuItem = createMenuItem("Redo", "Shortcut+Y", REPEAT, e -> editRedo());

		editUndoMenuItem.disableProperty().bind(createActiveBooleanProperty(FileEditor::canUndoProperty).not());
		editRedoMenuItem.disableProperty().bind(createActiveBooleanProperty(FileEditor::canRedoProperty).not());

		Menu editMenu = new Menu("Edit", null,
				editUndoMenuItem,
				editRedoMenuItem);

		// Insert menu
		MenuItem insertBoldMenuItem = createMenuItem("Bold", "Shortcut+B", BOLD, e -> insertBold());
		MenuItem insertItalicMenuItem = createMenuItem("Italic", "Shortcut+I", ITALIC, e -> insertItalic());

		insertBoldMenuItem.disableProperty().bind(activeFileEditorIsNull);
		insertItalicMenuItem.disableProperty().bind(activeFileEditorIsNull);

		Menu insertMenu = new Menu("Insert", null,
				insertBoldMenuItem,
				insertItalicMenuItem);

		// Help menu
		MenuItem helpAboutMenuItem = createMenuItem("About Markdown Writer FX", null, null, e -> helpAbout());

		Menu helpMenu = new Menu("Help", null,
				helpAboutMenuItem);

		return new MenuBar(fileMenu, editMenu, insertMenu, helpMenu);
	}

	private ToolBar createToolBar() {
		Button fileNewButton = createToolBarButton(FILE_ALT, "New", "Shortcut+N", e -> fileNew());
		Button fileOpenButton = createToolBarButton(FOLDER_OPEN_ALT, "Open", "Shortcut+O", e -> fileOpen());
		Button fileSaveButton = createToolBarButton(FLOPPY_ALT, "Save", "Shortcut+S", e -> fileSave());

		Button editUndoButton = createToolBarButton(UNDO, "Undo", "Shortcut+Z", e -> editUndo());
		Button editRedoButton = createToolBarButton(REPEAT, "Redo", "Shortcut+Y", e -> editRedo());

		Button insertBoldButton = createToolBarButton(BOLD, "Bold", "Shortcut+B", e -> insertBold());
		Button insertItalicButton = createToolBarButton(ITALIC, "Italic", "Shortcut+I", e -> insertItalic());

		fileSaveButton.disableProperty().bind(createActiveBooleanProperty(FileEditor::modifiedProperty).not());

		editUndoButton.disableProperty().bind(createActiveBooleanProperty(FileEditor::canUndoProperty).not());
		editRedoButton.disableProperty().bind(createActiveBooleanProperty(FileEditor::canRedoProperty).not());

		BooleanBinding activeFileEditorIsNull = fileEditorTabPane.activeFileEditorProperty().isNull();
		insertBoldButton.disableProperty().bind(activeFileEditorIsNull);
		insertItalicButton.disableProperty().bind(activeFileEditorIsNull);

		return new ToolBar(
				fileNewButton,
				fileOpenButton,
				fileSaveButton,
				new Separator(),
				editUndoButton,
				editRedoButton,
				new Separator(),
				insertBoldButton,
				insertItalicButton);
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

	/**
	 * Creates a boolean property that is bound to another boolean value
	 * of the active editor.
	 */
	private BooleanProperty createActiveBooleanProperty(Function<FileEditor, ObservableBooleanValue> func) {
		BooleanProperty b = new SimpleBooleanProperty();
		FileEditor fileEditor = fileEditorTabPane.getActiveFileEditor();
		if (fileEditor != null)
			b.bind(func.apply(fileEditor));
		fileEditorTabPane.activeFileEditorProperty().addListener((observable, oldFileEditor, newFileEditor) -> {
			b.unbind();
			if (newFileEditor != null)
				b.bind(func.apply(newFileEditor));
			else
				b.set(false);
		});
		return b;
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

	/**
	 * When the editor control (RichTextFX) is focused, it consumes all key events
	 * and the menu accelerators do not work. Looks like a bug to me...
	 * Workaround: install keyboard shortcuts into the editor component.
	 */
	private EventHandler<KeyEvent> editorShortcuts;
	EventHandler<KeyEvent> getEditorShortcuts() {
		if (editorShortcuts != null)
			return editorShortcuts;

		editorShortcuts = EventHandlerHelper
			// File
			.on(EventPattern.keyPressed(KeyCode.N, KeyCombination.SHORTCUT_DOWN)).act(e -> fileNew())
			.on(EventPattern.keyPressed(KeyCode.O, KeyCombination.SHORTCUT_DOWN)).act(e -> fileOpen())
			.on(EventPattern.keyPressed(KeyCode.W, KeyCombination.SHORTCUT_DOWN)).act(e -> fileClose())
			.on(EventPattern.keyPressed(KeyCode.S, KeyCombination.SHORTCUT_DOWN)).act(e -> fileSave())
			.on(EventPattern.keyPressed(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)).act(e -> fileSaveAll())
			// Insert
			.on(EventPattern.keyPressed(KeyCode.B, KeyCombination.SHORTCUT_DOWN)).act(e -> insertBold())
			.on(EventPattern.keyPressed(KeyCode.I, KeyCombination.SHORTCUT_DOWN)).act(e -> insertItalic())
			.create();
		return editorShortcuts;
	}

	//---- File menu ----------------------------------------------------------

	private void fileNew() {
		fileEditorTabPane.newEditor();
	}

	private void fileOpen() {
		fileEditorTabPane.openEditor();
	}

	private void fileClose() {
		fileEditorTabPane.closeEditor(fileEditorTabPane.getActiveFileEditor());
	}

	private void fileCloseAll() {
		fileEditorTabPane.closeAllEditors();
	}

	private void fileSave() {
		fileEditorTabPane.saveEditor(fileEditorTabPane.getActiveFileEditor());
	}

	private void fileSaveAll() {
		fileEditorTabPane.saveAllEditors();
	}

	private void fileExit() {
		Window window = scene.getWindow();
		Event.fireEvent(window, new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
	}

	//---- Edit menu ----------------------------------------------------------

	private void editUndo() {
		fileEditorTabPane.getActiveFileEditor().undo();
	}

	private void editRedo() {
		fileEditorTabPane.getActiveFileEditor().redo();
	}

	//---- Insert menu --------------------------------------------------------

	private void insertBold() {
		fileEditorTabPane.getActiveFileEditor().getEditor().surroundSelection("**", "**");
	}

	private void insertItalic() {
		fileEditorTabPane.getActiveFileEditor().getEditor().surroundSelection("*", "*");
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
