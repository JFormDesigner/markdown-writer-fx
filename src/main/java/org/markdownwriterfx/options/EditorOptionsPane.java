/*
 * Copyright (c) 2016 Karl Tauber <karl at jformdesigner dot com>
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

package org.markdownwriterfx.options;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.text.Font;
import org.markdownwriterfx.Messages;
import org.markdownwriterfx.controls.IntSpinner;
import org.tbee.javafx.scene.layout.fxml.MigPane;

/**
 * Editor options pane
 *
 * @author Karl Tauber
 */
class EditorOptionsPane
	extends MigPane
{
	EditorOptionsPane() {
		initComponents();

		Font titleFont = Font.font(16);
		markersTitle.setFont(titleFont);
		formatTitle.setFont(titleFont);

		strongEmphasisMarkerField.getItems().addAll("**", "__");
		emphasisMarkerField.getItems().addAll("*", "_");
		bulletListMarkerField.getItems().addAll("-", "+", "*");

		wrapLineLengthField.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(Options.MIN_WRAP_LINE_LENGTH, Integer.MAX_VALUE));
		formatOnlyModifiedParagraphsCheckBox.disableProperty().bind(formatOnSaveCheckBox.selectedProperty().not());
	}

	void load() {
		// markers
		strongEmphasisMarkerField.getSelectionModel().select(Options.getStrongEmphasisMarker());
		emphasisMarkerField.getSelectionModel().select(Options.getEmphasisMarker());
		bulletListMarkerField.getSelectionModel().select(Options.getBulletListMarker());

		// format
		wrapLineLengthField.getValueFactory().setValue(Options.getWrapLineLength());
		formatOnSaveCheckBox.setSelected(Options.isFormatOnSave());
		formatOnlyModifiedParagraphsCheckBox.setSelected(Options.isFormatOnlyModifiedParagraphs());
	}

	void save() {
		// markers
		Options.setStrongEmphasisMarker(strongEmphasisMarkerField.getSelectionModel().getSelectedItem());
		Options.setEmphasisMarker(emphasisMarkerField.getSelectionModel().getSelectedItem());
		Options.setBulletListMarker(bulletListMarkerField.getSelectionModel().getSelectedItem());

		// format
		Options.setWrapLineLength(wrapLineLengthField.getValue());
		Options.setFormatOnSave(formatOnSaveCheckBox.isSelected());
		Options.setFormatOnlyModifiedParagraphs(formatOnlyModifiedParagraphsCheckBox.isSelected());
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		markersTitle = new Label();
		var strongEmphasisMarkerLabel = new Label();
		strongEmphasisMarkerField = new ChoiceBox<>();
		var emphasisMarkerLabel = new Label();
		emphasisMarkerField = new ChoiceBox<>();
		var bulletListMarkerLabel = new Label();
		bulletListMarkerField = new ChoiceBox<>();
		formatTitle = new Label();
		wrapLineLengthLabel = new Label();
		wrapLineLengthField = new IntSpinner();
		var wrapLineLengthLabel2 = new Label();
		formatOnSaveCheckBox = new CheckBox();
		formatOnlyModifiedParagraphsCheckBox = new CheckBox();

		//======== this ========
		setLayout("hidemode 3");
		setCols(
			"[indent,fill]0" +
			"[fill]" +
			"[fill]" +
			"[fill]");
		setRows(
			"[]" +
			"[]" +
			"[]" +
			"[]para" +
			"[]" +
			"[]" +
			"[]" +
			"[]");

		//---- markersTitle ----
		markersTitle.setText(Messages.get("EditorOptionsPane.markersTitle.text"));
		add(markersTitle, "cell 0 0 2 1");

		//---- strongEmphasisMarkerLabel ----
		strongEmphasisMarkerLabel.setText(Messages.get("EditorOptionsPane.strongEmphasisMarkerLabel.text"));
		strongEmphasisMarkerLabel.setMnemonicParsing(true);
		strongEmphasisMarkerLabel.setLabelFor(strongEmphasisMarkerField);
		add(strongEmphasisMarkerLabel, "cell 1 1");
		add(strongEmphasisMarkerField, "cell 2 1");

		//---- emphasisMarkerLabel ----
		emphasisMarkerLabel.setText(Messages.get("EditorOptionsPane.emphasisMarkerLabel.text"));
		emphasisMarkerLabel.setMnemonicParsing(true);
		emphasisMarkerLabel.setLabelFor(emphasisMarkerField);
		add(emphasisMarkerLabel, "cell 1 2");
		add(emphasisMarkerField, "cell 2 2");

		//---- bulletListMarkerLabel ----
		bulletListMarkerLabel.setText(Messages.get("EditorOptionsPane.bulletListMarkerLabel.text"));
		bulletListMarkerLabel.setMnemonicParsing(true);
		bulletListMarkerLabel.setLabelFor(bulletListMarkerField);
		add(bulletListMarkerLabel, "cell 1 3");
		add(bulletListMarkerField, "cell 2 3");

		//---- formatTitle ----
		formatTitle.setText(Messages.get("EditorOptionsPane.formatTitle.text"));
		add(formatTitle, "cell 0 4 2 1");

		//---- wrapLineLengthLabel ----
		wrapLineLengthLabel.setText(Messages.get("EditorOptionsPane.wrapLineLengthLabel.text"));
		wrapLineLengthLabel.setMnemonicParsing(true);
		wrapLineLengthLabel.setLabelFor(wrapLineLengthField);
		add(wrapLineLengthLabel, "cell 1 5");
		add(wrapLineLengthField, "cell 2 5");

		//---- wrapLineLengthLabel2 ----
		wrapLineLengthLabel2.setText(Messages.get("EditorOptionsPane.wrapLineLengthLabel2.text"));
		add(wrapLineLengthLabel2, "cell 3 5");

		//---- formatOnSaveCheckBox ----
		formatOnSaveCheckBox.setText(Messages.get("EditorOptionsPane.formatOnSaveCheckBox.text"));
		add(formatOnSaveCheckBox, "cell 1 6 2 1,alignx left,growx 0");

		//---- formatOnlyModifiedParagraphsCheckBox ----
		formatOnlyModifiedParagraphsCheckBox.setText(Messages.get("EditorOptionsPane.formatOnlyModifiedParagraphsCheckBox.text"));
		add(formatOnlyModifiedParagraphsCheckBox, "cell 1 7 2 1,alignx left,growx 0");
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private Label markersTitle;
	private ChoiceBox<String> strongEmphasisMarkerField;
	private ChoiceBox<String> emphasisMarkerField;
	private ChoiceBox<String> bulletListMarkerField;
	private Label formatTitle;
	private Label wrapLineLengthLabel;
	private IntSpinner wrapLineLengthField;
	private CheckBox formatOnSaveCheckBox;
	private CheckBox formatOnlyModifiedParagraphsCheckBox;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
