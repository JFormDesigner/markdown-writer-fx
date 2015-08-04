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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.fxmisc.wellbehaved.event.EventHandlerHelper;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.markdownwriterfx.editor.MarkdownEditorPane;
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
		scene.getStylesheets().add("org/markdownwriterfx/MarkdownWriter.css");
		scene.windowProperty().addListener((observable, oldWindow, newWindow) -> {
			newWindow.setOnCloseRequest(e -> {
				if (!fileEditorTabPane.closeAllEditors())
					e.consume();
			});

			// workaround for a bug in JavaFX: unselect menubar if window looses focus
			newWindow.focusedProperty().addListener((obs, oldFocused, newFocused) -> {
				if (!newFocused) {
					// send an ESC key event to the menubar
					menuBar.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED,
							KeyEvent.CHAR_UNDEFINED, "", KeyCode.ESCAPE,
							false, false, false, false));
				}
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
		Action editUndoAction = new Action("Undo", "Shortcut+Z", UNDO,
				e -> getActiveEditor().undo(),
				createActiveBooleanProperty(FileEditor::canUndoProperty).not());
		Action editRedoAction = new Action("Redo", "Shortcut+Y", REPEAT,
				e -> getActiveEditor().redo(),
				createActiveBooleanProperty(FileEditor::canRedoProperty).not());

		// Insert actions
		Action insertBoldAction = new Action("Bold", "Shortcut+B", BOLD,
				e -> getActiveEditor().surroundSelection("**", "**"),
				activeFileEditorIsNull);
		Action insertItalicAction = new Action("Italic", "Shortcut+I", ITALIC,
				e -> getActiveEditor().surroundSelection("*", "*"),
				activeFileEditorIsNull);
		Action insertStrikethroughAction = new Action("Strikethrough", "Shortcut+T", STRIKETHROUGH,
				e -> getActiveEditor().surroundSelection("~~", "~~"),
				activeFileEditorIsNull);
		Action insertBlockquoteAction = new Action("Blockquote", "Shortcut+Q", QUOTE_LEFT,
				e -> getActiveEditor().surroundSelection("\n\n> ", ""),
				activeFileEditorIsNull);
		Action insertCodeAction = new Action("Inline Code", "Shortcut+K", CODE,
				e -> getActiveEditor().surroundSelection("`", "`"),
				activeFileEditorIsNull);
		Action insertFencedCodeBlockAction = new Action("Fenced Code Block", "Shortcut+Shift+K", FILE_CODE_ALT,
				e -> getActiveEditor().surroundSelection("\n\n```\n", "\n```\n\n", "enter code here"),
				activeFileEditorIsNull);

		Action insertHeader1Action = new Action("Header 1", "Shortcut+1", HEADER,
				e -> getActiveEditor().surroundSelection("\n\n# ", "", "header 1"),
				activeFileEditorIsNull);
		Action insertHeader2Action = new Action("Header 2", "Shortcut+2", HEADER,
				e -> getActiveEditor().surroundSelection("\n\n## ", "", "header 2"),
				activeFileEditorIsNull);
		Action insertHeader3Action = new Action("Header 3", "Shortcut+3", HEADER,
				e -> getActiveEditor().surroundSelection("\n\n### ", "", "header 3"),
				activeFileEditorIsNull);
		Action insertHeader4Action = new Action("Header 4", "Shortcut+4", HEADER,
				e -> getActiveEditor().surroundSelection("\n\n#### ", "", "header 4"),
				activeFileEditorIsNull);
		Action insertHeader5Action = new Action("Header 5", "Shortcut+5", HEADER,
				e -> getActiveEditor().surroundSelection("\n\n##### ", "", "header 5"),
				activeFileEditorIsNull);
		Action insertHeader6Action = new Action("Header 6", "Shortcut+6", HEADER,
				e -> getActiveEditor().surroundSelection("\n\n###### ", "", "header 6"),
				activeFileEditorIsNull);

		Action insertUnorderedListAction = new Action("Unordered List", "Shortcut+U", LIST_UL,
				e -> getActiveEditor().surroundSelection("\n\n* ", ""),
				activeFileEditorIsNull);
		Action insertOrderedListAction = new Action("Ordered List", "Shortcut+Shift+O", LIST_OL,
				e -> getActiveEditor().surroundSelection("\n\n1. ", ""),
				activeFileEditorIsNull);
		Action insertHorizontalRuleAction = new Action("Horizontal Rule", "Shortcut+H", null,
				e -> getActiveEditor().surroundSelection("\n\n---\n\n", ""),
				activeFileEditorIsNull);

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
				insertItalicAction,
				insertStrikethroughAction,
				insertBlockquoteAction,
				insertCodeAction,
				insertFencedCodeBlockAction,
				null,
				insertHeader1Action,
				insertHeader2Action,
				insertHeader3Action,
				insertHeader4Action,
				insertHeader5Action,
				insertHeader6Action,
				null,
				insertUnorderedListAction,
				insertOrderedListAction,
				insertHorizontalRuleAction);

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
				insertItalicAction,
				insertBlockquoteAction,
				insertCodeAction,
				insertFencedCodeBlockAction,
				null,
				insertHeader1Action,
				null,
				insertUnorderedListAction,
				insertOrderedListAction);

		return new VBox(menuBar, toolBar);
	}

	private MarkdownEditorPane getActiveEditor() {
		return fileEditorTabPane.getActiveFileEditor().getEditor();
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
