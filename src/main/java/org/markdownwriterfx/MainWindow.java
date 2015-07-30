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
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.fxmisc.wellbehaved.event.EventHandlerHelper;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.markdownwriterfx.util.Action;
import org.markdownwriterfx.util.ActionUtils;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import java.util.function.Consumer;
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
	private MenuBar menuBar;

	MainWindow() {
		fileEditorTabPane = new FileEditorTabPane(this);

		BorderPane borderPane = new BorderPane();
		borderPane.setPrefSize(800, 800);
		borderPane.setTop(createMenuBarAndToolBar());
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

	private Node createMenuBarAndToolBar() {
		BooleanBinding activeFileEditorIsNull = fileEditorTabPane.activeFileEditorProperty().isNull();

		// File actions
		Action fileNewAction = new Action("New", "Shortcut+N", FILE_ALT, e -> fileNew());
		Action fileOpenAction = new Action("Open...", "Shortcut+O", FOLDER_OPEN_ALT, e -> fileOpen());
		Action fileCloseAction = new Action("Close", "Shortcut+W", null, e -> fileClose(), activeFileEditorIsNull);
		Action fileCloseAllAction = new Action("Close All", null, null, e -> fileCloseAll(), activeFileEditorIsNull);
		Action fileSaveAction = new Action("Save", "Shortcut+S", FLOPPY_ALT, e -> fileSave(),
				createActiveBooleanProperty(FileEditor::modifiedProperty).not());
		Action fileSaveAllAction = new Action("Save All", "Shortcut+Shift+S", null, e -> fileSaveAll(),
				Bindings.not(fileEditorTabPane.anyFileEditorModifiedProperty()));
		Action fileExitAction = new Action("Exit", null, null, e -> fileExit());

		// Edit actions
		Action editUndoAction = new Action("Undo", "Shortcut+Z", UNDO, e -> editUndo(),
				createActiveBooleanProperty(FileEditor::canUndoProperty).not());
		Action editRedoAction = new Action("Redo", "Shortcut+Y", REPEAT, e -> editRedo(),
				createActiveBooleanProperty(FileEditor::canRedoProperty).not());

		// Insert actions
		Action insertBoldAction = new Action("Bold", "Shortcut+B", BOLD, e -> insertBold(), activeFileEditorIsNull);
		Action insertItalicAction = new Action("Italic", "Shortcut+I", ITALIC, e -> insertItalic(), activeFileEditorIsNull);

		// Help actions
		Action helpAboutAction = new Action("About Markdown Writer FX", null, null, e -> helpAbout());


		//---- MenuBar ----

		Menu fileMenu = ActionUtils.createMenu("File",
				fileNewAction,
				fileOpenAction,
				null,
				fileCloseAction,
				fileCloseAllAction,
				null,
				fileSaveAction,
				fileSaveAllAction,
				null,
				fileExitAction);

		Menu editMenu = ActionUtils.createMenu("Edit",
				editUndoAction,
				editRedoAction);

		Menu insertMenu = ActionUtils.createMenu("Insert",
				insertBoldAction,
				insertItalicAction);

		Menu helpMenu = ActionUtils.createMenu("Help",
				helpAboutAction);

		menuBar = new MenuBar(fileMenu, editMenu, insertMenu, helpMenu);


		//---- ToolBar ----

		ToolBar toolBar = ActionUtils.createToolBar(
				fileNewAction,
				fileOpenAction,
				fileSaveAction,
				null,
				editUndoAction,
				editRedoAction,
				null,
				insertBoldAction,
				insertItalicAction);

		return new VBox(menuBar, toolBar);
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

		EventHandlerHelper.Builder<KeyEvent> builder = null;
		for (Menu menu : menuBar.getMenus()) {
			for (MenuItem menuItem : menu.getItems()) {
				KeyCombination accelerator = menuItem.getAccelerator();
				if (accelerator != null) {
					Consumer<? super KeyEvent> action = e -> menuItem.getOnAction().handle(null);
					if (builder != null)
						builder = builder.on(EventPattern.keyPressed(accelerator)).act(action);
					else
						builder = EventHandlerHelper.on(EventPattern.keyPressed(accelerator)).act(action);
				}
			}
		}
		editorShortcuts = builder.create();
		return editorShortcuts;
	}

	//---- File actions -------------------------------------------------------

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

	//---- Edit actions -------------------------------------------------------

	private void editUndo() {
		fileEditorTabPane.getActiveFileEditor().undo();
	}

	private void editRedo() {
		fileEditorTabPane.getActiveFileEditor().redo();
	}

	//---- Insert actions -----------------------------------------------------

	private void insertBold() {
		fileEditorTabPane.getActiveFileEditor().getEditor().surroundSelection("**", "**");
	}

	private void insertItalic() {
		fileEditorTabPane.getActiveFileEditor().getEditor().surroundSelection("*", "*");
	}

	//---- Help actions -------------------------------------------------------

	private void helpAbout() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("About");
		alert.setHeaderText("Markdown Writer FX");
		alert.setContentText("Copyright (c) 2015 Karl Tauber <karl at jformdesigner dot com>\nAll rights reserved.");
		alert.initOwner(getScene().getWindow());

		alert.showAndWait();
	}
}
