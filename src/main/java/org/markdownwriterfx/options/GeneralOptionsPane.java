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
import java.util.List;
import java.util.SortedMap;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.markdownwriterfx.Messages;
import org.markdownwriterfx.util.Item;
import org.markdownwriterfx.util.Utils;
import org.tbee.javafx.scene.layout.fxml.MigPane;

/**
 * General options pane
 *
 * @author Karl Tauber
 */
class GeneralOptionsPane
	extends MigPane
{
	@SuppressWarnings("unchecked")
	GeneralOptionsPane() {
		initComponents();

		Font titleFont = Font.font(16);
		editorSettingsLabel.setFont(titleFont);
		fileSettingsLabel.setFont(titleFont);
		addonsSettingsLabel.setFont(titleFont);

		// font family
		fontFamilyField.getItems().addAll(getMonospacedFonts());
		fontFamilyField.getSelectionModel().select(0);
		fontFamilyField.setButtonCell(new FontListCell());
		fontFamilyField.setCellFactory(p -> new FontListCell());

		// font size
		fontSizeField.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(Options.MIN_FONT_SIZE, Options.MAX_FONT_SIZE));

		// line separator
		String defaultLineSeparator = System.getProperty( "line.separator", "\n" );
		String defaultLineSeparatorStr = defaultLineSeparator.replace("\r", "CR").replace("\n", "LF");
		lineSeparatorField.getItems().addAll(
			new Item<>(Messages.get("GeneralOptionsPane.platformDefault", defaultLineSeparatorStr), null),
			new Item<>(Messages.get("GeneralOptionsPane.sepWindows"), "\r\n"),
			new Item<>(Messages.get("GeneralOptionsPane.sepUnix"), "\n"));

		// encoding
		encodingField.getItems().addAll(getAvailableEncodings());

		// file extensions
		markdownFileExtensionsField.setPromptText(Options.DEF_MARKDOWN_FILE_EXTENSIONS);
	}

	void hideInternal() {
		addonsSettingsLabel.setVisible( false );
		addonsPathLabel.setVisible( false );
		addonsPathField.setVisible( false );
		addonsPathLabel2.setVisible( false );
	}

	/**
	 * Return a list of all the mono-spaced fonts on the system.
	 *
	 * @author David D. Clark http://clarkonium.net/2015/07/finding-mono-spaced-fonts-in-javafx/
	 */
	private static Collection<String> getMonospacedFonts() {

		// Compare the layout widths of two strings. One string is composed
		// of "thin" characters, the other of "wide" characters. In mono-spaced
		// fonts the widths should be the same.

		final Text thinTxt = new Text("1 l"); // note the space
		final Text thikTxt = new Text("MWX");

		List<String> fontFamilyList = Font.getFamilies();
		List<String> monospacedFonts = new ArrayList<>();

		for (String fontFamilyName : fontFamilyList) {
			Font font = Font.font(fontFamilyName, FontWeight.NORMAL, FontPosture.REGULAR, 14.0d);
			thinTxt.setFont(font);
			thikTxt.setFont(font);
			if (thinTxt.getLayoutBounds().getWidth() == thikTxt.getLayoutBounds().getWidth())
				monospacedFonts.add(fontFamilyName);
		}

		return monospacedFonts;
	}

	private Collection<Item<String>> getAvailableEncodings() {
		SortedMap<String, Charset> availableCharsets = Charset.availableCharsets();

		ArrayList<Item<String>> values = new ArrayList<>(1 + availableCharsets.size());
		values.add(new Item<>(Messages.get("GeneralOptionsPane.platformDefault", Charset.defaultCharset().name()), null));
		for (String name : availableCharsets.keySet())
			values.add(new Item<>(name, name));
		return values;
	}

	void load() {
		// editor settings
		fontFamilyField.getSelectionModel().select(Options.getFontFamily());
		fontSizeField.getValueFactory().setValue(Options.getFontSize());
		showLineNoCheckBox.setSelected(Options.isShowLineNo());
		showWhitespaceCheckBox.setSelected(Options.isShowWhitespace());
		showImagesEmbeddedCheckBox.setSelected(Options.isShowImagesEmbedded());

		// file settings
		lineSeparatorField.setValue(new Item<>(Options.getLineSeparator(), Options.getLineSeparator()));
		encodingField.setValue(new Item<>(Options.getEncoding(), Options.getEncoding()));
		markdownFileExtensionsField.setText(Options.getMarkdownFileExtensions());

		// addons settings
		addonsPathField.setText( Options.getAddonsPath() );
	}

	void save() {
		// editor settings
		Options.setFontFamily(fontFamilyField.getSelectionModel().getSelectedItem());
		Options.setFontSize(fontSizeField.getValue());
		Options.setShowLineNo(showLineNoCheckBox.isSelected());
		Options.setShowWhitespace(showWhitespaceCheckBox.isSelected());
		Options.setShowImagesEmbedded(showImagesEmbeddedCheckBox.isSelected());

		// file settings
		Options.setLineSeparator(lineSeparatorField.getValue().value);
		Options.setEncoding(encodingField.getValue().value);
		Options.setMarkdownFileExtensions(Utils.defaultIfEmpty(
				markdownFileExtensionsField.getText().trim(),
				Options.DEF_MARKDOWN_FILE_EXTENSIONS));

		// addons settings
		Options.setAddonsPath( Utils.defaultIfEmpty( addonsPathField.getText().trim(), null ) );
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		editorSettingsLabel = new Label();
		fontFamilyLabel = new Label();
		fontFamilyField = new ComboBox<>();
		fontSizeLabel = new Label();
		fontSizeField = new Spinner<>();
		showLineNoCheckBox = new CheckBox();
		showWhitespaceCheckBox = new CheckBox();
		showImagesEmbeddedCheckBox = new CheckBox();
		fileSettingsLabel = new Label();
		var lineSeparatorLabel = new Label();
		lineSeparatorField = new ComboBox<>();
		var lineSeparatorLabel2 = new Label();
		var encodingLabel = new Label();
		encodingField = new ComboBox<>();
		var markdownFileExtensionsLabel = new Label();
		markdownFileExtensionsField = new TextField();
		addonsSettingsLabel = new Label();
		addonsPathLabel = new Label();
		addonsPathField = new TextField();
		addonsPathLabel2 = new Label();

		//======== this ========
		setCols(
			"[indent,fill]0" +
			"[fill]" +
			"[fill]" +
			"[grow,fill]");
		setRows(
			"[]" +
			"[]" +
			"[]" +
			"[]" +
			"[]" +
			"[]para" +
			"[]" +
			"[]" +
			"[]" +
			"[]para" +
			"[]" +
			"[]" +
			"[]");

		//---- editorSettingsLabel ----
		editorSettingsLabel.setText(Messages.get("GeneralOptionsPane.editorSettingsLabel.text"));
		add(editorSettingsLabel, "cell 0 0 2 1");

		//---- fontFamilyLabel ----
		fontFamilyLabel.setText(Messages.get("GeneralOptionsPane.fontFamilyLabel.text"));
		fontFamilyLabel.setMnemonicParsing(true);
		add(fontFamilyLabel, "cell 1 1");

		//---- fontFamilyField ----
		fontFamilyField.setVisibleRowCount(20);
		add(fontFamilyField, "cell 2 1");

		//---- fontSizeLabel ----
		fontSizeLabel.setText(Messages.get("GeneralOptionsPane.fontSizeLabel.text"));
		fontSizeLabel.setMnemonicParsing(true);
		add(fontSizeLabel, "cell 1 2");
		add(fontSizeField, "cell 2 2,alignx left,growx 0");

		//---- showLineNoCheckBox ----
		showLineNoCheckBox.setText(Messages.get("GeneralOptionsPane.showLineNoCheckBox.text"));
		add(showLineNoCheckBox, "cell 1 3 3 1,alignx left,growx 0");

		//---- showWhitespaceCheckBox ----
		showWhitespaceCheckBox.setText(Messages.get("GeneralOptionsPane.showWhitespaceCheckBox.text"));
		add(showWhitespaceCheckBox, "cell 1 4 3 1,alignx left,growx 0");

		//---- showImagesEmbeddedCheckBox ----
		showImagesEmbeddedCheckBox.setText(Messages.get("GeneralOptionsPane.showImagesEmbeddedCheckBox.text"));
		add(showImagesEmbeddedCheckBox, "cell 1 5 3 1,alignx left,growx 0");

		//---- fileSettingsLabel ----
		fileSettingsLabel.setText(Messages.get("GeneralOptionsPane.fileSettingsLabel.text"));
		add(fileSettingsLabel, "cell 0 6 2 1");

		//---- lineSeparatorLabel ----
		lineSeparatorLabel.setText(Messages.get("GeneralOptionsPane.lineSeparatorLabel.text"));
		lineSeparatorLabel.setMnemonicParsing(true);
		add(lineSeparatorLabel, "cell 1 7");
		add(lineSeparatorField, "cell 2 7");

		//---- lineSeparatorLabel2 ----
		lineSeparatorLabel2.setText(Messages.get("GeneralOptionsPane.lineSeparatorLabel2.text"));
		add(lineSeparatorLabel2, "cell 3 7");

		//---- encodingLabel ----
		encodingLabel.setText(Messages.get("GeneralOptionsPane.encodingLabel.text"));
		encodingLabel.setMnemonicParsing(true);
		add(encodingLabel, "cell 1 8");

		//---- encodingField ----
		encodingField.setVisibleRowCount(20);
		add(encodingField, "cell 2 8");

		//---- markdownFileExtensionsLabel ----
		markdownFileExtensionsLabel.setText(Messages.get("GeneralOptionsPane.markdownFileExtensionsLabel.text"));
		markdownFileExtensionsLabel.setMnemonicParsing(true);
		add(markdownFileExtensionsLabel, "cell 1 9");
		add(markdownFileExtensionsField, "cell 2 9 2 1");

		//---- addonsSettingsLabel ----
		addonsSettingsLabel.setText(Messages.get("GeneralOptionsPane.addonsSettingsLabel.text"));
		add(addonsSettingsLabel, "cell 0 10 2 1");

		//---- addonsPathLabel ----
		addonsPathLabel.setText(Messages.get("GeneralOptionsPane.addonsPathLabel.text"));
		add(addonsPathLabel, "cell 1 11");
		add(addonsPathField, "cell 2 11 2 1");

		//---- addonsPathLabel2 ----
		addonsPathLabel2.setText(Messages.get("GeneralOptionsPane.addonsPathLabel2.text"));
		add(addonsPathLabel2, "cell 2 12 2 1");
		// JFormDesigner - End of component initialization  //GEN-END:initComponents

		// TODO set this in JFormDesigner as soon as it supports labelFor
		fontFamilyLabel.setLabelFor(fontFamilyField);
		fontSizeLabel.setLabelFor(fontSizeField);
		lineSeparatorLabel.setLabelFor(lineSeparatorField);
		encodingLabel.setLabelFor(encodingField);
		markdownFileExtensionsLabel.setLabelFor(markdownFileExtensionsField);
		addonsPathLabel.setLabelFor( addonsPathField );
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private Label editorSettingsLabel;
	private Label fontFamilyLabel;
	private ComboBox<String> fontFamilyField;
	private Label fontSizeLabel;
	private Spinner<Integer> fontSizeField;
	private CheckBox showLineNoCheckBox;
	private CheckBox showWhitespaceCheckBox;
	private CheckBox showImagesEmbeddedCheckBox;
	private Label fileSettingsLabel;
	private ComboBox<Item<String>> lineSeparatorField;
	private ComboBox<Item<String>> encodingField;
	private TextField markdownFileExtensionsField;
	private Label addonsSettingsLabel;
	private Label addonsPathLabel;
	private TextField addonsPathField;
	private Label addonsPathLabel2;
	// JFormDesigner - End of variables declaration  //GEN-END:variables

	//---- class FontListCell -------------------------------------------------

	private static class FontListCell
		extends ListCell<String>
	{
		@Override
		protected void updateItem(String fontFamily, boolean empty) {
			super.updateItem(fontFamily, empty);

			if (!empty) {
				setText(fontFamily);
				setFont(Font.font(fontFamily));
			} else
				setText(null);
		}
	}
}
