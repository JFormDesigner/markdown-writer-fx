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
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.markdownwriterfx.editor.MarkdownEditorPane;
import org.markdownwriterfx.editor.SmartEdit;
import org.markdownwriterfx.options.MarkdownExtensionsPane;
import org.markdownwriterfx.options.Options;
import org.markdownwriterfx.options.Options.RendererType;
import org.markdownwriterfx.options.OptionsDialog;
import org.markdownwriterfx.preview.MarkdownPreviewPane;
import org.markdownwriterfx.projects.ProjectManager;
import org.markdownwriterfx.projects.ProjectPane;
import org.markdownwriterfx.util.Action;
import org.markdownwriterfx.util.ActionUtils;
import org.markdownwriterfx.util.Utils;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

/**
 * Main window containing a tab pane in the center for file editors.
 *
 * @author Karl Tauber
 */
class MainWindow
{
	private final Scene scene;
	private final ProjectPane projectPane;
	private final FileEditorTabPane fileEditorTabPane;
	private final FileEditorManager fileEditorManager;
	private MenuBar menuBar;
	private Node extensionsButton;
	final BooleanProperty stageFocusedProperty = new SimpleBooleanProperty();

	MainWindow() {
		fileEditorTabPane = new FileEditorTabPane(this);
		fileEditorManager = new FileEditorManager(fileEditorTabPane);
		projectPane = new ProjectPane(fileEditorManager);

		SplitPane splitPane = new SplitPane(projectPane.getNode(), fileEditorTabPane.getNode());
		SplitPane.setResizableWithParent(projectPane.getNode(), false);
		splitPane.setDividerPosition(0, 0.2);

		BorderPane borderPane = new BorderPane();
		borderPane.getStyleClass().add("main");
		borderPane.setPrefSize(800, 800);
		borderPane.setTop(createMenuBarAndToolBar());
		borderPane.setCenter(splitPane);

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

		Utils.fixSpaceAfterDeadKey(scene);

		// workaround for a bad JavaFX behavior: menu bar always grabs focus when ALT key is pressed,
		// but should grab it when ALT key is releases (as all other UI toolkits do) to give other
		// controls the chance to use Alt+Key shortcuts.
		scene.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
			if (e.isAltDown())
				e.consume();
		});

		// open markdown files dropped to main window
		scene.setOnDragOver(e -> {
			if (e.getDragboard().hasFiles())
				e.acceptTransferModes(TransferMode.COPY);
			e.consume();
		});
		scene.setOnDragDropped(e -> {
			boolean success = false;
			if (e.getDragboard().hasFiles()) {
				fileEditorTabPane.openEditors(e.getDragboard().getFiles(), 0);
				success = true;
			}
			e.setDropCompleted(success);
			e.consume();
		});

		Platform.runLater(() -> stageFocusedProperty.bind(scene.getWindow().focusedProperty()));
	}

	Scene getScene() {
		return scene;
	}

	private Node createMenuBarAndToolBar() {
		BooleanBinding activeFileEditorIsNull = fileEditorTabPane.activeFileEditorProperty().isNull();

		// File actions
		Action fileNewAction = new Action(Messages.get("MainWindow.fileNewAction"), "Shortcut+N", FILE_ALT, e -> fileNew());
		Action fileOpenAction = new Action(Messages.get("MainWindow.fileOpenAction"), "Shortcut+O", FOLDER_OPEN_ALT, e -> fileOpen());
		Action fileOpenProjectAction = new Action(Messages.get("MainWindow.fileOpenProjectAction"), "Shortcut+Shift+O", FOLDER_OPEN, e -> fileOpenProject());
		Action fileCloseAction = new Action(Messages.get("MainWindow.fileCloseAction"), "Shortcut+W", null, e -> fileClose(), activeFileEditorIsNull);
		Action fileCloseAllAction = new Action(Messages.get("MainWindow.fileCloseAllAction"), null, null, e -> fileCloseAll(), activeFileEditorIsNull);
		Action fileSaveAction = new Action(Messages.get("MainWindow.fileSaveAction"), "Shortcut+S", FLOPPY_ALT, e -> fileSave(),
				createActiveBooleanProperty(FileEditor::modifiedProperty).not());
		Action fileSaveAsAction = new Action(Messages.get("MainWindow.fileSaveAsAction"), null, null, e -> fileSaveAs(), activeFileEditorIsNull);
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
		Action editCutAction = new Action(Messages.get("MainWindow.editCutAction"), "Shortcut+X", CUT,
				e -> getActiveEditor().cut(),
				activeFileEditorIsNull);
		Action editCopyAction = new Action(Messages.get("MainWindow.editCopyAction"), "Shortcut+C", COPY,
				e -> getActiveEditor().copy(),
				activeFileEditorIsNull);
		Action editPasteAction = new Action(Messages.get("MainWindow.editPasteAction"), "Shortcut+V", PASTE,
				e -> getActiveEditor().paste(),
				activeFileEditorIsNull);
		Action editSelectAllAction = new Action(Messages.get("MainWindow.editSelectAllAction"), "Shortcut+A", null,
				e -> getActiveEditor().selectAll(),
				activeFileEditorIsNull);
		Action editFindAction = new Action(Messages.get("MainWindow.editFindAction"), "Shortcut+F", SEARCH,
				e -> getActiveEditor().find(false),
				activeFileEditorIsNull);
		Action editReplaceAction = new Action(Messages.get("MainWindow.editReplaceAction"), "Shortcut+H", RETWEET,
				e -> getActiveEditor().find(true),
				activeFileEditorIsNull);
		Action editFindNextAction = new Action(Messages.get("MainWindow.editFindNextAction"), "F3", null,
				e -> getActiveEditor().findNextPrevious(true),
				activeFileEditorIsNull);
		Action editFindPreviousAction = new Action(Messages.get("MainWindow.editFindPreviousAction"), "Shift+F3", null,
				e -> getActiveEditor().findNextPrevious(false),
				activeFileEditorIsNull);

		Action editFormatAllAction = new Action(Messages.get("MainWindow.editFormatAll"), "Shortcut+Shift+F", null,
				e -> getActiveSmartEdit().format(false),
				activeFileEditorIsNull);
		Action editFormatSelectionAction = new Action(Messages.get("MainWindow.editFormatSelection"), "Shortcut+Shift+Alt+F", null,
				e -> getActiveSmartEdit().format(true),
				activeFileEditorIsNull);

		// View actions
		Action viewPreviewAction = new Action(Messages.get("MainWindow.viewPreviewAction"), null, EYE,
				null, null, fileEditorTabPane.previewVisible);
		Action viewHtmlSourceAction = new Action(Messages.get("MainWindow.viewHtmlSourceAction"), null, HTML5,
				null, null, fileEditorTabPane.htmlSourceVisible);
		Action viewMarkdownAstAction = new Action(Messages.get("MainWindow.viewMarkdownAstAction"), null, SITEMAP,
				null, null, fileEditorTabPane.markdownAstVisible);
		Action viewExternalAction = MarkdownPreviewPane.hasExternalPreview()
			? new Action(Messages.get("MainWindow.viewExternalAction"), null, EXTERNAL_LINK,
		        null, null, fileEditorTabPane.externalVisible)
			: null;

		// Insert actions
		Action insertBoldAction = new Action(Messages.get("MainWindow.insertBoldAction"), "Shortcut+B", BOLD,
				e -> getActiveSmartEdit().insertBold(Messages.get("MainWindow.insertBoldText")),
				activeFileEditorIsNull);
		Action insertItalicAction = new Action(Messages.get("MainWindow.insertItalicAction"), "Shortcut+I", ITALIC,
				e -> getActiveSmartEdit().insertItalic(Messages.get("MainWindow.insertItalicText")),
				activeFileEditorIsNull);
		Action insertStrikethroughAction = new Action(Messages.get("MainWindow.insertStrikethroughAction"), "Shortcut+T", STRIKETHROUGH,
				e -> getActiveSmartEdit().insertStrikethrough(Messages.get("MainWindow.insertStrikethroughText")),
				activeFileEditorIsNull);
		Action insertCodeAction = new Action(Messages.get("MainWindow.insertCodeAction"), "Shortcut+K", CODE,
				e -> getActiveSmartEdit().insertInlineCode(Messages.get("MainWindow.insertCodeText")),
				activeFileEditorIsNull);

		Action insertLinkAction = new Action(Messages.get("MainWindow.insertLinkAction"), "Shortcut+L", LINK,
				e -> getActiveSmartEdit().insertLink(),
				activeFileEditorIsNull);
		Action insertImageAction = new Action(Messages.get("MainWindow.insertImageAction"), "Shortcut+G", PICTURE_ALT,
				e -> getActiveSmartEdit().insertImage(),
				activeFileEditorIsNull);

		Action insertUnorderedListAction = new Action(Messages.get("MainWindow.insertUnorderedListAction"), "Shortcut+U", LIST_UL,
				e -> getActiveSmartEdit().insertUnorderedList(),
				activeFileEditorIsNull);
		Action insertOrderedListAction = new Action(Messages.get("MainWindow.insertOrderedListAction"), "Shortcut+Shift+U", LIST_OL,
				e -> getActiveSmartEdit().surroundSelection("\n\n1. ", ""),
				activeFileEditorIsNull);
		Action insertBlockquoteAction = new Action(Messages.get("MainWindow.insertBlockquoteAction"), "Ctrl+Q", QUOTE_LEFT, // not Shortcut+Q because of conflict on Mac
				e -> getActiveSmartEdit().surroundSelection("\n\n> ", ""),
				activeFileEditorIsNull);
		Action insertFencedCodeBlockAction = new Action(Messages.get("MainWindow.insertFencedCodeBlockAction"), "Shortcut+Shift+K", FILE_CODE_ALT,
				e -> getActiveSmartEdit().surroundSelection("\n\n```\n", "\n```\n\n", Messages.get("MainWindow.insertFencedCodeBlockText")),
				activeFileEditorIsNull);

		Action insertHeader1Action = new Action(Messages.get("MainWindow.insertHeader1Action"), "Shortcut+1", HEADER,
				e -> getActiveSmartEdit().insertHeading(1, Messages.get("MainWindow.insertHeader1Text")),
				activeFileEditorIsNull);
		Action insertHeader2Action = new Action(Messages.get("MainWindow.insertHeader2Action"), "Shortcut+2", HEADER,
				e -> getActiveSmartEdit().insertHeading(2, Messages.get("MainWindow.insertHeader2Text")),
				activeFileEditorIsNull);
		Action insertHeader3Action = new Action(Messages.get("MainWindow.insertHeader3Action"), "Shortcut+3", HEADER,
				e -> getActiveSmartEdit().insertHeading(3, Messages.get("MainWindow.insertHeader3Text")),
				activeFileEditorIsNull);
		Action insertHeader4Action = new Action(Messages.get("MainWindow.insertHeader4Action"), "Shortcut+4", HEADER,
				e -> getActiveSmartEdit().insertHeading(4, Messages.get("MainWindow.insertHeader4Text")),
				activeFileEditorIsNull);
		Action insertHeader5Action = new Action(Messages.get("MainWindow.insertHeader5Action"), "Shortcut+5", HEADER,
				e -> getActiveSmartEdit().insertHeading(5, Messages.get("MainWindow.insertHeader5Text")),
				activeFileEditorIsNull);
		Action insertHeader6Action = new Action(Messages.get("MainWindow.insertHeader6Action"), "Shortcut+6", HEADER,
				e -> getActiveSmartEdit().insertHeading(6, Messages.get("MainWindow.insertHeader6Text")),
				activeFileEditorIsNull);

		Action insertHorizontalRuleAction = new Action(Messages.get("MainWindow.insertHorizontalRuleAction"), null, null,
				e -> getActiveSmartEdit().surroundSelection("\n\n---\n\n", ""),
				activeFileEditorIsNull);

		// Tools actions
		Action toolsOptionsAction = new Action(Messages.get("MainWindow.toolsOptionsAction"), "Shortcut+,", null, e -> toolsOptions());

		// Help actions
		Action helpAboutAction = new Action(Messages.get("MainWindow.helpAboutAction"), null, null, e -> helpAbout());


		//---- MenuBar ----

		Menu fileMenu = ActionUtils.createMenu(Messages.get("MainWindow.fileMenu"),
				fileNewAction,
				fileOpenAction,
				fileOpenProjectAction,
				null,
				fileCloseAction,
				fileCloseAllAction,
				null,
				fileSaveAction,
				fileSaveAsAction,
				fileSaveAllAction,
				null,
				fileExitAction);

		Menu editMenu = ActionUtils.createMenu(Messages.get("MainWindow.editMenu"),
				editUndoAction,
				editRedoAction,
				null,
				editCutAction,
				editCopyAction,
				editPasteAction,
				editSelectAllAction,
				null,
				editFindAction,
				editReplaceAction,
				null,
				editFindNextAction,
				editFindPreviousAction,
				null,
				editFormatAllAction,
				editFormatSelectionAction);

		Menu viewMenu = ActionUtils.createMenu(Messages.get("MainWindow.viewMenu"),
				viewPreviewAction,
				viewHtmlSourceAction,
				viewMarkdownAstAction);
		if (viewExternalAction != null)
			viewMenu.getItems().add(ActionUtils.createMenuItem(viewExternalAction));

		Menu insertMenu = ActionUtils.createMenu(Messages.get("MainWindow.insertMenu"),
				insertBoldAction,
				insertItalicAction,
				insertStrikethroughAction,
				insertCodeAction,
				null,
				insertLinkAction,
				insertImageAction,
				null,
				insertUnorderedListAction,
				insertOrderedListAction,
				insertBlockquoteAction,
				insertFencedCodeBlockAction,
				null,
				insertHeader1Action,
				insertHeader2Action,
				insertHeader3Action,
				insertHeader4Action,
				insertHeader5Action,
				insertHeader6Action,
				null,
				insertHorizontalRuleAction);

		Menu toolsMenu = ActionUtils.createMenu(Messages.get("MainWindow.toolsMenu"),
				toolsOptionsAction);

		Menu helpMenu = ActionUtils.createMenu(Messages.get("MainWindow.helpMenu"),
				helpAboutAction);

		menuBar = new MenuBar(fileMenu, editMenu, viewMenu, insertMenu, toolsMenu, helpMenu);


		//---- ToolBar ----

		ToolBar toolBar = ActionUtils.createToolBar(
				fileNewAction,
				fileOpenAction,
				fileOpenProjectAction,
				fileSaveAction,
				null,
				editUndoAction,
				editRedoAction,
				null,
				new Action(insertBoldAction, createActiveEditBooleanProperty(SmartEdit::boldProperty)),
				new Action(insertItalicAction, createActiveEditBooleanProperty(SmartEdit::italicProperty)),
				new Action(insertCodeAction, createActiveEditBooleanProperty(SmartEdit::codeProperty)),
				null,
				new Action(insertLinkAction, createActiveEditBooleanProperty(SmartEdit::linkProperty)),
				new Action(insertImageAction, createActiveEditBooleanProperty(SmartEdit::imageProperty)),
				null,
				new Action(insertUnorderedListAction, createActiveEditBooleanProperty(SmartEdit::unorderedListProperty)),
				new Action(insertOrderedListAction, createActiveEditBooleanProperty(SmartEdit::orderedListProperty)),
				new Action(insertBlockquoteAction, createActiveEditBooleanProperty(SmartEdit::blockquoteProperty)),
				new Action(insertFencedCodeBlockAction, createActiveEditBooleanProperty(SmartEdit::fencedCodeProperty)),
				null,
				new Action(insertHeader1Action, createActiveEditBooleanProperty(SmartEdit::headerProperty)));

		// horizontal spacer
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		toolBar.getItems().add(spacer);

		// preview renderer type choice box
		ChoiceBox<RendererType> previewRenderer = new ChoiceBox<>();
		previewRenderer.setFocusTraversable(false);
		previewRenderer.getItems().addAll(RendererType.values());
		previewRenderer.getSelectionModel().select(Options.getMarkdownRenderer());
		previewRenderer.getSelectionModel().selectedItemProperty().addListener((ob, o, n) -> {
			Options.setMarkdownRenderer(n);
		});
		Options.markdownRendererProperty().addListener((ob, o, n) -> {
			previewRenderer.getSelectionModel().select(n);
		});
		toolBar.getItems().add(previewRenderer);

		// markdown extensions popover
		String title = Messages.get("MainWindow.MarkdownExtensions");
		extensionsButton = ActionUtils.createToolBarButton(
				new Action(title, null, COG, e -> {
					PopOver popOver = new PopOver();
					popOver.setTitle(title);
					popOver.setHeaderAlwaysVisible(true);
					popOver.setArrowLocation(ArrowLocation.TOP_CENTER);
					popOver.setContentNode(new MarkdownExtensionsPane(true));
					popOver.show(extensionsButton);
				}));
		toolBar.getItems().add(extensionsButton);
		toolBar.getItems().add(new Separator());

		// preview actions
		Node[] previewButtons = ActionUtils.createToolBarButtons(
				viewPreviewAction,
				viewHtmlSourceAction,
				viewMarkdownAstAction);
		ToggleGroup viewGroup = new ToggleGroup();
		for (Node n : previewButtons)
			((ToggleButton)n).setToggleGroup(viewGroup);
		toolBar.getItems().addAll(previewButtons);

		if (viewExternalAction != null) {
			ButtonBase externalPreviewButton = ActionUtils.createToolBarButton(viewExternalAction);
			((ToggleButton)externalPreviewButton).setToggleGroup(viewGroup);
			toolBar.getItems().add(externalPreviewButton);
		}

		return new VBox(menuBar, toolBar);
	}

	private MarkdownEditorPane getActiveEditor() {
		return fileEditorTabPane.getActiveFileEditor().getEditor();
	}

	private SmartEdit getActiveSmartEdit() {
		return getActiveEditor().getSmartEdit();
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

	/**
	 * Creates a boolean property that is bound to another boolean value
	 * of the active editor's SmartEdit.
	 */
	private BooleanProperty createActiveEditBooleanProperty(Function<SmartEdit, ObservableBooleanValue> func) {
		BooleanProperty b = new SimpleBooleanProperty() {
			@Override
			public void set(boolean newValue) {
				// invoked when the user invokes an action
				// do not try to change SmartEdit properties because this
				// would throw a "bound value cannot be set" exception
			}
		};

		ChangeListener<? super FileEditor> listener = (observable, oldFileEditor, newFileEditor) -> {
			b.unbind();
			if (newFileEditor != null) {
				if (newFileEditor.getEditor() != null)
					b.bind(func.apply(newFileEditor.getEditor().getSmartEdit()));
				else {
					newFileEditor.editorProperty().addListener((ob, o, n) -> {
						b.bind(func.apply(n.getSmartEdit()));
					});
				}
			} else
				b.set(false);
		};
		FileEditor fileEditor = fileEditorTabPane.getActiveFileEditor();
		listener.changed(null, null, fileEditor);
		fileEditorTabPane.activeFileEditorProperty().addListener(listener);
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

	private void fileOpenProject() {
		ProjectManager.openProject(scene.getWindow());
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

	private void fileSaveAs() {
		fileEditorTabPane.saveEditorAs(fileEditorTabPane.getActiveFileEditor());
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
		String version = null;
		Package pkg = this.getClass().getPackage();
		if (pkg != null)
			version = pkg.getImplementationVersion();
		if (version == null)
			version = "(dev)";

		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(Messages.get("MainWindow.about.title"));
		alert.setHeaderText(Messages.get("MainWindow.about.headerText"));
		alert.setContentText(Messages.get("MainWindow.about.contentText", version));
		alert.setGraphic(new ImageView(new Image("org/markdownwriterfx/markdownwriterfx32.png")));
		alert.initOwner(getScene().getWindow());
		alert.getDialogPane().setPrefWidth(420);

		alert.showAndWait();
	}
}
