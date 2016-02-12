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

import java.text.MessageFormat;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.markdownwriterfx.editor.MarkdownEditorPane;
import org.markdownwriterfx.options.OptionsDialog;
import org.markdownwriterfx.util.Action;
import org.markdownwriterfx.util.ActionUtils;
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
		Action fileNewAction = new Action(Messages.get("MainWindow.fileNewAction"), "Shortcut+N", FILE_ALT, e -> fileNew());
		Action fileOpenAction = new Action(Messages.get("MainWindow.fileOpenAction"), "Shortcut+O", FOLDER_OPEN_ALT, e -> fileOpen());
		Action fileCloseAction = new Action(Messages.get("MainWindow.fileCloseAction"), "Shortcut+W", null, e -> fileClose(), activeFileEditorIsNull);
		Action fileCloseAllAction = new Action(Messages.get("MainWindow.fileCloseAllAction"), null, null, e -> fileCloseAll(), activeFileEditorIsNull);
		Action fileSaveAction = new Action(Messages.get("MainWindow.fileSaveAction"), "Shortcut+S", FLOPPY_ALT, e -> fileSave(),
				createActiveBooleanProperty(FileEditor::modifiedProperty).not());
		Action fileSaveAllAction = new Action(Messages.get("MainWindow.fileSaveAllAction"), "Shortcut+Shift+S", null, e -> fileSaveAll(),
				Bindings.not(fileEditorTabPane.anyFileEditorModifiedProperty()));
		Action fileExitAction = new Action(Messages.get("MainWindow.fileExitAction"), null, null, e -> fileExit());

		// Edit actions
		Action editUndoAction = new Action(Messages.get("MainWindow.editUndoAction"), "Shortcut+Z", UNDO,
				e -> getActiveEditor().undo(),
				createActiveBooleanProperty(FileEditor::canUndoProperty).not());
		Action editRedoAction = new Action(Messages.get("MainWindow.editRedoAction"), "Shortcut+Y", REPEAT,
				e -> getActiveEditor().redo(),
				createActiveBooleanProperty(FileEditor::canRedoProperty).not());

		// Insert actions
		Action insertBoldAction = new Action(Messages.get("MainWindow.insertBoldAction"), "Shortcut+B", BOLD,
				e -> getActiveEditor().surroundSelection("**", "**"),
				activeFileEditorIsNull);
		Action insertItalicAction = new Action(Messages.get("MainWindow.insertItalicAction"), "Shortcut+I", ITALIC,
				e -> getActiveEditor().surroundSelection("*", "*"),
				activeFileEditorIsNull);
		Action insertStrikethroughAction = new Action(Messages.get("MainWindow.insertStrikethroughAction"), "Shortcut+T", STRIKETHROUGH,
				e -> getActiveEditor().surroundSelection("~~", "~~"),
				activeFileEditorIsNull);
		Action insertBlockquoteAction = new Action(Messages.get("MainWindow.insertBlockquoteAction"), "Ctrl+Q", QUOTE_LEFT, // not Shortcut+Q because of conflict on Mac
				e -> getActiveEditor().surroundSelection("\n\n> ", ""),
				activeFileEditorIsNull);
		Action insertCodeAction = new Action(Messages.get("MainWindow.insertCodeAction"), "Shortcut+K", CODE,
				e -> getActiveEditor().surroundSelection("`", "`"),
				activeFileEditorIsNull);
		Action insertFencedCodeBlockAction = new Action(Messages.get("MainWindow.insertFencedCodeBlockAction"), "Shortcut+Shift+K", FILE_CODE_ALT,
				e -> getActiveEditor().surroundSelection("\n\n```\n", "\n```\n\n", Messages.get("MainWindow.insertFencedCodeBlockText")),
				activeFileEditorIsNull);

		Action insertLinkAction = new Action(Messages.get("MainWindow.insertLinkAction"), "Shortcut+L", LINK,
				e -> getActiveEditor().insertLink(),
				activeFileEditorIsNull);
		Action insertImageAction = new Action(Messages.get("MainWindow.insertImageAction"), "Shortcut+G", PICTURE_ALT,
				e -> getActiveEditor().insertImage(),
				activeFileEditorIsNull);

		Action insertHeader1Action = new Action(Messages.get("MainWindow.insertHeader1Action"), "Shortcut+1", HEADER,
				e -> getActiveEditor().surroundSelection("\n\n# ", "", Messages.get("MainWindow.insertHeader1Text")),
				activeFileEditorIsNull);
		Action insertHeader2Action = new Action(Messages.get("MainWindow.insertHeader2Action"), "Shortcut+2", HEADER,
				e -> getActiveEditor().surroundSelection("\n\n## ", "", Messages.get("MainWindow.insertHeader2Text")),
				activeFileEditorIsNull);
		Action insertHeader3Action = new Action(Messages.get("MainWindow.insertHeader3Action"), "Shortcut+3", HEADER,
				e -> getActiveEditor().surroundSelection("\n\n### ", "", Messages.get("MainWindow.insertHeader3Text")),
				activeFileEditorIsNull);
		Action insertHeader4Action = new Action(Messages.get("MainWindow.insertHeader4Action"), "Shortcut+4", HEADER,
				e -> getActiveEditor().surroundSelection("\n\n#### ", "", Messages.get("MainWindow.insertHeader4Text")),
				activeFileEditorIsNull);
		Action insertHeader5Action = new Action(Messages.get("MainWindow.insertHeader5Action"), "Shortcut+5", HEADER,
				e -> getActiveEditor().surroundSelection("\n\n##### ", "", Messages.get("MainWindow.insertHeader5Text")),
				activeFileEditorIsNull);
		Action insertHeader6Action = new Action(Messages.get("MainWindow.insertHeader6Action"), "Shortcut+6", HEADER,
				e -> getActiveEditor().surroundSelection("\n\n###### ", "", Messages.get("MainWindow.insertHeader6Text")),
				activeFileEditorIsNull);

		Action insertUnorderedListAction = new Action(Messages.get("MainWindow.insertUnorderedListAction"), "Shortcut+U", LIST_UL,
				e -> getActiveEditor().surroundSelection("\n\n* ", ""),
				activeFileEditorIsNull);
		Action insertOrderedListAction = new Action(Messages.get("MainWindow.insertOrderedListAction"), "Shortcut+Shift+O", LIST_OL,
				e -> getActiveEditor().surroundSelection("\n\n1. ", ""),
				activeFileEditorIsNull);
		Action insertHorizontalRuleAction = new Action(Messages.get("MainWindow.insertHorizontalRuleAction"), "Shortcut+H", null,
				e -> getActiveEditor().surroundSelection("\n\n---\n\n", ""),
				activeFileEditorIsNull);

		// Tools actions
		Action toolsOptionsAction = new Action(Messages.get("MainWindow.toolsOptionsAction"), "Shortcut+,", null, e -> toolsOptions());

		// Help actions
		Action helpAboutAction = new Action(Messages.get("MainWindow.helpAboutAction"), null, null, e -> helpAbout());


		//---- MenuBar ----

		Menu fileMenu = ActionUtils.createMenu(Messages.get("MainWindow.fileMenu"),
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

		Menu editMenu = ActionUtils.createMenu(Messages.get("MainWindow.editMenu"),
				editUndoAction,
				editRedoAction);

		Menu insertMenu = ActionUtils.createMenu(Messages.get("MainWindow.insertMenu"),
				insertBoldAction,
				insertItalicAction,
				insertStrikethroughAction,
				insertBlockquoteAction,
				insertCodeAction,
				insertFencedCodeBlockAction,
				null,
				insertLinkAction,
				insertImageAction,
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

		Menu toolsMenu = ActionUtils.createMenu(Messages.get("MainWindow.toolsMenu"),
				toolsOptionsAction);

		Menu helpMenu = ActionUtils.createMenu(Messages.get("MainWindow.helpMenu"),
				helpAboutAction);

		menuBar = new MenuBar(fileMenu, editMenu, insertMenu, toolsMenu, helpMenu);


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
				insertLinkAction,
				insertImageAction,
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
		alert.setContentText(MessageFormat.format(contentTextFormat, contentTextArgs));
		alert.initOwner(getScene().getWindow());
		return alert;
	}

	//---- File actions -------------------------------------------------------

	private void fileNew() {
		fileEditorTabPane.newEditor();
	}

	private void fileOpen() {
		fileEditorTabPane.openEditor();
	}

	private void fileClose() {
		fileEditorTabPane.closeEditor(fileEditorTabPane.getActiveFileEditor(), true);
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

	//---- Tools actions ------------------------------------------------------

	private void toolsOptions() {
		OptionsDialog dialog = new OptionsDialog(getScene().getWindow());
		dialog.showAndWait();
	}

	//---- Help actions -------------------------------------------------------

	private void helpAbout() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(Messages.get("MainWindow.about.title"));
		alert.setHeaderText(Messages.get("MainWindow.about.headerText"));
		alert.setContentText(Messages.get("MainWindow.about.contentText"));
		alert.setGraphic(new ImageView(new Image("org/markdownwriterfx/markdownwriterfx32.png")));
		alert.initOwner(getScene().getWindow());

		alert.showAndWait();
	}
}
