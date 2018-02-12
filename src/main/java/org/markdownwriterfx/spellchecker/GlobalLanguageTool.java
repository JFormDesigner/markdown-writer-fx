/*
 * Copyright (c) 2018 Karl Tauber <karl at jformdesigner dot com>
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

package org.markdownwriterfx.spellchecker;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javafx.beans.InvalidationListener;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.Languages;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.markup.AnnotatedText;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.spelling.SpellingCheckRule;
import org.markdownwriterfx.options.Options;

/**
 * Global language tool used for all editors (because initialization
 * of JLanguageTool is slow and there can be only one active editor).
 *
 * @author Karl Tauber
 */
class GlobalLanguageTool
{
	// global JLanguageTool used in executor
	private JLanguageTool languageTool;

	// global ResultCache used by global JLanguageTool
	private ResultCacheEx cache;

	// global user dictionary
	private UserDictionary userDictionary;

	// global ignored words (keeps ignored words when switching spell checking off and on)
	private static final Set<String> wordsToBeIgnored = new HashSet<>();

	GlobalLanguageTool() {
		Options.spellCheckerProperty().addListener((observer, oldValue, newValue) -> {
			if (!newValue)
				uninitialize();
			requestCheck();
		});

		InvalidationListener optionsListener = e -> {
			uninitialize();
			requestCheck();
		};
		Options.grammarCheckerProperty().addListener(optionsListener);
		Options.languageProperty().addListener(optionsListener);
		Options.userDictionaryProperty().addListener(optionsListener);

		Options.disabledRulesProperty().addListener((observer, oldValue, newValue) -> {
			if (!isInitialized())
				return;

			// remove old disabled rules
			for (String ruleId : oldValue)
				languageTool.enableRule(ruleId);

			// add new disabled rules
			languageTool.disableRules(Arrays.asList(newValue));

			requestCheck();
		});
	}

	boolean isInitialized() {
		return languageTool != null;
	}

	void initialize() {
		if (languageTool != null)
			return;

		// get language
		Language language;
		try {
			String langCode = Options.getLanguage();
			language = (langCode != null)
				? Languages.getLanguageForShortCode(langCode)
				: Languages.getLanguageForLocale(Locale.getDefault());
		} catch (RuntimeException ex) {
			language = new AmericanEnglish();
		}

		// create cache
		cache = new ResultCacheEx(10000, 1, TimeUnit.DAYS);

		// create language tool
		languageTool = new JLanguageTool(language, null, cache);

		// disable rules
		languageTool.disableRules(Arrays.asList(Options.getDisabledRules()));
		if (!Options.isGrammarChecker()) {
			for (Rule rule : languageTool.getAllRules()) {
				if (!rule.isDictionaryBasedSpellingRule())
					languageTool.disableRule(rule.getId());
			}
		}

		// get user dictionary
		userDictionary = new UserDictionary();

		// ignore words
		addIgnoreTokens(userDictionary.getWords());
		addIgnoreTokens(Arrays.asList(wordsToBeIgnored.toArray(new String[wordsToBeIgnored.size()])));
	}

	private void uninitialize() {
		languageTool = null;
		cache = null;
		userDictionary = null;
	}

	// 'checkRequestID' property
	private final SimpleIntegerProperty checkRequestID = new SimpleIntegerProperty(1);
	int getCheckRequestID() { return checkRequestID.get(); }
	IntegerProperty checkRequestIDProperty() { return checkRequestID; }

	/**
	 * Increment checkRequestID property to signal listeners that something has
	 * changed in the language tool and that they should check the text again.
	 */
	private void requestCheck() {
		checkRequestID.set(checkRequestID.get() + 1);
	}

	List<RuleMatch> check(AnnotatedText text)
		throws IllegalStateException, IOException
	{
		// languageTool may be set to null in another thread --> get it only once
		JLanguageTool languageTool = this.languageTool;
		if (languageTool == null)
			throw new IllegalStateException();

		return languageTool.check(text);
	}

	void addToUserDictionary(String word) {
		userDictionary.addWord(word);
		addIgnoreWord(word);
	}

	void ignoreWord(String word) {
		wordsToBeIgnored.add(word);
		addIgnoreWord(word);
	}

	private void addIgnoreWord(String word) {
		cache.invalidate(word);
		addIgnoreTokens(Collections.singletonList(word));
	}

	private void addIgnoreTokens(List<String> words) {
		forEachSpellingCheckRule(rule -> {
			rule.addIgnoreTokens(words);
		});
	}

	private void forEachSpellingCheckRule(Consumer<SpellingCheckRule> action) {
		for (Rule rule : languageTool.getAllActiveRules()) {
			if (rule instanceof SpellingCheckRule)
				action.accept((SpellingCheckRule) rule);
		}
	}
}
