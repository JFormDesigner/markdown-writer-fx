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

import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import org.markdownwriterfx.Messages;
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

		strongEmphasisMarkerField.getItems().addAll("**", "__");
		emphasisMarkerField.getItems().addAll("*", "_");
		bulletListMarkerField.getItems().addAll("-", "+", "*");
	}

	void load() {
		// markers
		strongEmphasisMarkerField.getSelectionModel().select(Options.getStrongEmphasisMarker());
		emphasisMarkerField.getSelectionModel().select(Options.getEmphasisMarker());
		bulletListMarkerField.getSelectionModel().select(Options.getBulletListMarker());
	}

	void save() {
		// markers
		Options.setStrongEmphasisMarker(strongEmphasisMarkerField.getSelectionModel().getSelectedItem());
		Options.setEmphasisMarker(emphasisMarkerField.getSelectionModel().getSelectedItem());
		Options.setBulletListMarker(bulletListMarkerField.getSelectionModel().getSelectedItem());
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		markersTitle = new Label();
		Label strongEmphasisMarkerLabel = new Label();
		strongEmphasisMarkerField = new ChoiceBox<>();
		Label emphasisMarkerLabel = new Label();
		emphasisMarkerField = new ChoiceBox<>();
		Label bulletListMarkerLabel = new Label();
		bulletListMarkerField = new ChoiceBox<>();

		//======== this ========
		setLayout("hidemode 3");
		setCols("[indent,fill]0[fill][fill]");
		setRows("[][][][]");

		//---- markersTitle ----
		markersTitle.setText(Messages.get("EditorOptionsPane.markersTitle.text"));
		add(markersTitle, "cell 0 0 2 1");

		//---- strongEmphasisMarkerLabel ----
		strongEmphasisMarkerLabel.setText(Messages.get("EditorOptionsPane.strongEmphasisMarkerLabel.text"));
		strongEmphasisMarkerLabel.setMnemonicParsing(true);
		add(strongEmphasisMarkerLabel, "cell 1 1");
		add(strongEmphasisMarkerField, "cell 2 1");

		//---- emphasisMarkerLabel ----
		emphasisMarkerLabel.setText(Messages.get("EditorOptionsPane.emphasisMarkerLabel.text"));
		emphasisMarkerLabel.setMnemonicParsing(true);
		add(emphasisMarkerLabel, "cell 1 2");
		add(emphasisMarkerField, "cell 2 2");

		//---- bulletListMarkerLabel ----
		bulletListMarkerLabel.setText(Messages.get("EditorOptionsPane.bulletListMarkerLabel.text"));
		bulletListMarkerLabel.setMnemonicParsing(true);
		add(bulletListMarkerLabel, "cell 1 3");
		add(bulletListMarkerField, "cell 2 3");
		// JFormDesigner - End of component initialization  //GEN-END:initComponents

		// TODO set this in JFormDesigner as soon as it supports labelFor
		strongEmphasisMarkerLabel.setLabelFor(strongEmphasisMarkerField);
		emphasisMarkerLabel.setLabelFor(emphasisMarkerField);
		bulletListMarkerLabel.setLabelFor(bulletListMarkerField);
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private Label markersTitle;
	private ChoiceBox<String> strongEmphasisMarkerField;
	private ChoiceBox<String> emphasisMarkerField;
	private ChoiceBox<String> bulletListMarkerField;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
