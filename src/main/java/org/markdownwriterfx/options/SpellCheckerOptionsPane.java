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

import javafx.scene.control.ComboBox;
import static javafx.scene.input.KeyCode.DELETE;
import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;
import static org.fxmisc.wellbehaved.event.InputMap.consume;
import static org.fxmisc.wellbehaved.event.InputMap.sequence;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import org.fxmisc.wellbehaved.event.Nodes;
import org.languagetool.Language;
import org.languagetool.Languages;
import org.languagetool.language.AmericanEnglish;
import org.markdownwriterfx.Messages;
import org.markdownwriterfx.controls.BrowseFileButton;
import org.markdownwriterfx.util.Utils;
import org.tbee.javafx.scene.layout.fxml.MigPane;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

/**
 * Spell checker options pane
 *
 * @author Karl Tauber
 */
public class SpellCheckerOptionsPane
	extends MigPane
{
	/** languages that are marked as deprecated in their {@link Language} classes */
	private static final Set<String> DEPRECATED_LANGUAGES = new HashSet<>(Arrays.asList(
		"ast-ES", "be-BY", "da-DK", "de", "en", "sl-SI", "sv", "tl-PH"));

	public SpellCheckerOptionsPane() {
		initComponents();

		languageField.getItems().addAll(getLanguages());

		browseUserDictionaryButton.setBasePath(new File(System.getProperty("user.home")).toPath());
		browseUserDictionaryButton.urlProperty().bindBidirectional(userDictionaryField.textProperty());

		disabledRulesField.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		Nodes.addInputMap(disabledRulesField, sequence(
			consume(keyPressed(DELETE),		this::deleteDisabledRules)
		));

		BooleanBinding disabled = Bindings.not(spellCheckerCheckBox.selectedProperty());
		languageLabel.disableProperty().bind(disabled);
		languageField.disableProperty().bind(disabled);
		userDictionaryLabel.disableProperty().bind(disabled);
		userDictionaryField.disableProperty().bind(disabled);
		browseUserDictionaryButton.disableProperty().bind(disabled);
		userDictionaryNote.disableProperty().bind(disabled);
		disabledRulesLabel.disableProperty().bind(disabled);
		disabledRulesField.disableProperty().bind(disabled);
		disabledRulesNote.disableProperty().bind(disabled);
		disabledRulesNote2.disableProperty().bind(disabled);
	}

	void load() {
		spellCheckerCheckBox.setSelected(Options.isSpellChecker());
		languageField.setValue(shortCode2language(Options.getLanguage()));
		userDictionaryField.setText(Options.getUserDictionary());
		disabledRulesField.getItems().addAll(Options.getDisabledRules());
	}

	void save() {
		Options.setSpellChecker(spellCheckerCheckBox.isSelected());
		Options.setLanguage(language2shortCode(languageField.getValue()));
		Options.setUserDictionary(Utils.defaultIfEmpty(userDictionaryField.getText(), null));

		String[] newDisabledRules = disabledRulesField.getItems().toArray(new String[0]);
		if (!Arrays.equals(newDisabledRules, Options.getDisabledRules()))
			Options.setDisabledRules(newDisabledRules);
	}

	private List<Language> getLanguages() {
		List<Language> languages = new ArrayList<>( Languages.get() );
		languages.removeIf(language -> DEPRECATED_LANGUAGES.contains(language.getShortCodeWithCountryAndVariant()));
		languages.sort((l1, l2) -> l1.getName().compareToIgnoreCase(l2.getName()));
		return languages;
	}

	private Language shortCode2language(String shortCode) {
		try {
			return (shortCode != null)
				? Languages.getLanguageForShortCode(shortCode)
				: Languages.getLanguageForLocale(Locale.getDefault());
		} catch (RuntimeException ex) {
			return new AmericanEnglish();
		}
	}

	private String language2shortCode(Language l) {
		return l.getLocaleWithCountryAndVariant().equals(Locale.getDefault())
			? null
			: l.getShortCodeWithCountryAndVariant();
	}

	private void deleteDisabledRules(KeyEvent e) {
		// remove selected items
		disabledRulesField.getItems().removeAll(disabledRulesField.getSelectionModel().getSelectedItem());
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		spellCheckerCheckBox = new CheckBox();
		languageLabel = new Label();
		languageField = new ComboBox<>();
		userDictionaryLabel = new Label();
		userDictionaryField = new TextField();
		browseUserDictionaryButton = new SpellCheckerOptionsPane.BrowseUserDictionaryButton();
		userDictionaryNote = new Label();
		disabledRulesLabel = new Label();
		disabledRulesField = new ListView<>();
		disabledRulesNote = new Label();
		disabledRulesNote2 = new Label();

		//======== this ========
		setCols("[shrink 0,fill][430,grow,fill]");
		setRows("[][][][]para[250,grow,fill][]0[]");

		//---- spellCheckerCheckBox ----
		spellCheckerCheckBox.setText(Messages.get("SpellCheckerOptionsPane.spellCheckerCheckBox.text"));
		add(spellCheckerCheckBox, "cell 0 0 2 1,alignx left,growx 0");

		//---- languageLabel ----
		languageLabel.setText(Messages.get("SpellCheckerOptionsPane.languageLabel.text"));
		languageLabel.setMnemonicParsing(true);
		add(languageLabel, "cell 0 1");

		//---- languageField ----
		languageField.setVisibleRowCount(20);
		add(languageField, "cell 1 1");

		//---- userDictionaryLabel ----
		userDictionaryLabel.setText(Messages.get("SpellCheckerOptionsPane.userDictionaryLabel.text"));
		userDictionaryLabel.setMnemonicParsing(true);
		add(userDictionaryLabel, "cell 0 2");
		add(userDictionaryField, "cell 1 2");

		//---- browseUserDictionaryButton ----
		browseUserDictionaryButton.setFocusTraversable(false);
		add(browseUserDictionaryButton, "cell 1 2,alignx right,growx 0");

		//---- userDictionaryNote ----
		userDictionaryNote.setText(Messages.get("SpellCheckerOptionsPane.userDictionaryNote.text"));
		userDictionaryNote.setWrapText(true);
		add(userDictionaryNote, "cell 1 3");

		//---- disabledRulesLabel ----
		disabledRulesLabel.setText(Messages.get("SpellCheckerOptionsPane.disabledRulesLabel.text"));
		add(disabledRulesLabel, "cell 0 4,aligny top,growy 0");
		add(disabledRulesField, "cell 1 4");

		//---- disabledRulesNote ----
		disabledRulesNote.setText(Messages.get("SpellCheckerOptionsPane.disabledRulesNote.text"));
		disabledRulesNote.setWrapText(true);
		add(disabledRulesNote, "cell 1 5");

		//---- disabledRulesNote2 ----
		disabledRulesNote2.setText(Messages.get("SpellCheckerOptionsPane.disabledRulesNote2.text"));
		disabledRulesNote2.setWrapText(true);
		add(disabledRulesNote2, "cell 1 6");
		// JFormDesigner - End of component initialization  //GEN-END:initComponents

		// TODO set this in JFormDesigner as soon as it supports labelFor
		userDictionaryLabel.setLabelFor(userDictionaryField);
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private CheckBox spellCheckerCheckBox;
	private Label languageLabel;
	private ComboBox<Language> languageField;
	private Label userDictionaryLabel;
	private TextField userDictionaryField;
	private SpellCheckerOptionsPane.BrowseUserDictionaryButton browseUserDictionaryButton;
	private Label userDictionaryNote;
	private Label disabledRulesLabel;
	private ListView<String> disabledRulesField;
	private Label disabledRulesNote;
	private Label disabledRulesNote2;
	// JFormDesigner - End of variables declaration  //GEN-END:variables

	//---- class BrowseUserDictionaryButton -----------------------------------

	private static class BrowseUserDictionaryButton
		extends BrowseFileButton
	{
		private BrowseUserDictionaryButton() {
			setGraphic(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.ELLIPSIS_H, "1.2em"));
			setTooltip(null);
		}

		@Override
		protected void browse(ActionEvent e) {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle(Messages.get("SpellCheckerOptionsPane.browseUserDictionaryButton.chooser.title"));
			fileChooser.setInitialDirectory(getInitialDirectory());
			File result = fileChooser.showOpenDialog(getScene().getWindow());
			if (result != null)
				setUrl(result.getAbsolutePath());
		}

		@Override
		protected File getInitialDirectory() {
			String url = getUrl();
			if (url != null)
				return new File(url).getParentFile();
			else
				return new File(System.getProperty("user.home"));
		}
	}
}
