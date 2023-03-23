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

package org.markdownwriterfx.dialogs;

import java.nio.file.Path;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import org.markdownwriterfx.Messages;
import org.markdownwriterfx.controls.BrowseFileButton;
import org.markdownwriterfx.controls.EscapeTextField;
import org.markdownwriterfx.util.Utils;
import org.tbee.javafx.scene.layout.fxml.MigPane;

/**
 * Dialog to enter a markdown image.
 *
 * @author Karl Tauber
 */
public class ImageDialog
	extends Dialog<String>
{
	private final StringProperty image = new SimpleStringProperty();

	public ImageDialog(Window owner, Path basePath) {
		setTitle(Messages.get("ImageDialog.title"));
		initOwner(owner);
		setResizable(true);

		initComponents();

		linkBrowseFileButton.setBasePath(basePath);
		linkBrowseFileButton.addExtensionFilter(new ExtensionFilter(Messages.get("ImageDialog.chooser.imagesFilter"), "*.png", "*.gif", "*.jpg", "*.svg"));
		linkBrowseFileButton.urlProperty().bindBidirectional(urlField.escapedTextProperty());

		DialogPane dialogPane = getDialogPane();
		dialogPane.setContent(pane);
		dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		dialogPane.lookupButton(ButtonType.OK).disableProperty().bind(
				urlField.escapedTextProperty().isEmpty()
					.or(textField.escapedTextProperty().isEmpty()));

		Utils.fixSpaceAfterDeadKey(dialogPane.getScene());

		image.bind(Bindings.when(titleField.escapedTextProperty().isNotEmpty())
				.then(Bindings.format("![%s](%s \"%s\")", textField.escapedTextProperty(), urlField.escapedTextProperty(), titleField.escapedTextProperty()))
				.otherwise(Bindings.format("![%s](%s)", textField.escapedTextProperty(), urlField.escapedTextProperty())));
		previewField.textProperty().bind(image);

		setResultConverter(dialogButton -> {
			return (dialogButton == ButtonType.OK) ? image.get() : null;
		});

		Platform.runLater(() -> {
			urlField.requestFocus();

			if (urlField.getText().startsWith("http://"))
				urlField.selectRange("http://".length(), urlField.getLength());
		});
	}

	public void init(String url, String text, String title) {
		urlField.setText(url);
		textField.setText(text);
		titleField.setText(title);
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		pane = new MigPane();
		var urlLabel = new Label();
		urlField = new EscapeTextField();
		linkBrowseFileButton = new BrowseFileButton();
		var textLabel = new Label();
		textField = new EscapeTextField();
		var titleLabel = new Label();
		titleField = new EscapeTextField();
		var previewLabel = new Label();
		previewField = new Label();

		//======== pane ========
		{
			pane.setCols(
				"[shrink 0,fill]" +
				"[400,grow,fill]");
			pane.setRows(
				"[]" +
				"[]" +
				"[]" +
				"[]");

			//---- urlLabel ----
			urlLabel.setText(Messages.get("ImageDialog.urlLabel.text"));
			urlLabel.setLabelFor(urlField);
			urlLabel.setMnemonicParsing(true);
			pane.add(urlLabel, "cell 0 0");

			//---- urlField ----
			urlField.setEscapeCharacters("()");
			urlField.setText("http://yourlink.com");
			urlField.setPromptText("http://yourlink.com");
			pane.add(urlField, "cell 1 0");

			//---- linkBrowseFileButton ----
			linkBrowseFileButton.setFocusTraversable(false);
			pane.add(linkBrowseFileButton, "cell 1 0,alignx center,growx 0");

			//---- textLabel ----
			textLabel.setText(Messages.get("ImageDialog.textLabel.text"));
			textLabel.setLabelFor(textField);
			textLabel.setMnemonicParsing(true);
			pane.add(textLabel, "cell 0 1");

			//---- textField ----
			textField.setEscapeCharacters("[]");
			pane.add(textField, "cell 1 1");

			//---- titleLabel ----
			titleLabel.setText(Messages.get("ImageDialog.titleLabel.text"));
			titleLabel.setLabelFor(titleField);
			titleLabel.setMnemonicParsing(true);
			pane.add(titleLabel, "cell 0 2");
			pane.add(titleField, "cell 1 2");

			//---- previewLabel ----
			previewLabel.setText(Messages.get("ImageDialog.previewLabel.text"));
			pane.add(previewLabel, "cell 0 3");
			pane.add(previewField, "cell 1 3");
		}
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private MigPane pane;
	private EscapeTextField urlField;
	private BrowseFileButton linkBrowseFileButton;
	private EscapeTextField textField;
	private EscapeTextField titleField;
	private Label previewField;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
