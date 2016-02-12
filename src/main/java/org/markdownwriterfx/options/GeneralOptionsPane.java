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

package org.markdownwriterfx.options;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.SortedMap;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import org.markdownwriterfx.Messages;
import org.markdownwriterfx.util.Item;
import org.tbee.javafx.scene.layout.fxml.MigPane;

/**
 * General options pane
 *
 * @author Karl Tauber
 */
public class GeneralOptionsPane
	extends MigPane
{
	@SuppressWarnings("unchecked")
	public GeneralOptionsPane() {
		initComponents();

		String defaultLineSeparator = System.getProperty( "line.separator", "\n" );
		String defaultLineSeparatorStr = defaultLineSeparator.replace("\r", "CR").replace("\n", "LF");
		lineSeparatorField.getItems().addAll(
			new Item<String>( Messages.get("GeneralOptionsPane.platformDefault", defaultLineSeparatorStr), null ),
			new Item<String>( Messages.get("GeneralOptionsPane.sepWindows"), "\r\n" ),
			new Item<String>( Messages.get("GeneralOptionsPane.sepUnix"), "\n" ));

		encodingField.getItems().addAll(getAvailableEncodings());
	}

	private Collection<Item<String>> getAvailableEncodings() {
		SortedMap<String, Charset> availableCharsets = Charset.availableCharsets();

		ArrayList<Item<String>> values = new ArrayList<>(1 + availableCharsets.size());
		values.add(new Item<String>(Messages.get("GeneralOptionsPane.platformDefault", Charset.defaultCharset().name()), null));
		for (String name : availableCharsets.keySet())
			values.add(new Item<String>(name, name));
		return values;
	}

	void load() {
		lineSeparatorField.setValue(new Item<String>(Options.getLineSeparator(), Options.getLineSeparator()));
		encodingField.setValue(new Item<String>(Options.getEncoding(), Options.getEncoding()));

		showWhitespaceCheckBox.setSelected(Options.isShowWhitespace());
	}

	void save() {
		Options.setLineSeparator(lineSeparatorField.getValue().value);
		Options.setEncoding(encodingField.getValue().value);

		Options.setShowWhitespace(showWhitespaceCheckBox.isSelected());
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		Label lineSeparatorLabel = new Label();
		lineSeparatorField = new ComboBox<>();
		Label lineSeparatorLabel2 = new Label();
		Label encodingLabel = new Label();
		encodingField = new ComboBox<>();
		showWhitespaceCheckBox = new CheckBox();

		//======== this ========
		setCols("[fill][fill][fill]");
		setRows("[][]para[]");

		//---- lineSeparatorLabel ----
		lineSeparatorLabel.setText(Messages.get("GeneralOptionsPane.lineSeparatorLabel.text"));
		lineSeparatorLabel.setMnemonicParsing(true);
		add(lineSeparatorLabel, "cell 0 0");
		add(lineSeparatorField, "cell 1 0");

		//---- lineSeparatorLabel2 ----
		lineSeparatorLabel2.setText(Messages.get("GeneralOptionsPane.lineSeparatorLabel2.text"));
		add(lineSeparatorLabel2, "cell 2 0");

		//---- encodingLabel ----
		encodingLabel.setText(Messages.get("GeneralOptionsPane.encodingLabel.text"));
		encodingLabel.setMnemonicParsing(true);
		add(encodingLabel, "cell 0 1");

		//---- encodingField ----
		encodingField.setVisibleRowCount(20);
		add(encodingField, "cell 1 1");

		//---- showWhitespaceCheckBox ----
		showWhitespaceCheckBox.setText(Messages.get("GeneralOptionsPane.showWhitespaceCheckBox.text"));
		add(showWhitespaceCheckBox, "cell 0 2 3 1,growx 0,alignx left");
		// JFormDesigner - End of component initialization  //GEN-END:initComponents

		// TODO set this in JFormDesigner as soon as it supports labelFor
		lineSeparatorLabel.setLabelFor(lineSeparatorField);
		encodingLabel.setLabelFor(encodingField);
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private ComboBox<Item<String>> lineSeparatorField;
	private ComboBox<Item<String>> encodingField;
	private CheckBox showWhitespaceCheckBox;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
