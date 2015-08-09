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

package org.markdownwriterfx.spellchecker;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import org.fxmisc.richtext.PlainTextChange;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;
import org.markdownwriterfx.editor.ParagraphOverlayGraphicFactory;
import org.markdownwriterfx.options.Options;
import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.reactfx.util.Try;

/**
 * Spell checker for an instance of StyleClassedTextArea
 *
 * @author Karl Tauber
 */
public class SpellChecker
{
	private final StyleClassedTextArea textArea;
	private final ParagraphOverlayGraphicFactory overlayGraphicFactory;
	private final InvalidationListener optionsListener;
	private final ObjectProperty<List<RuleMatch>> ruleMatches = new SimpleObjectProperty<>();

	private Subscription textChangesSubscribtion;
	private SpellCheckerOverlayFactory spellCheckerOverlayFactory;

	// global executor used for all spell checking
	private static ExecutorService executor;

	// global JLanguageTool used in executor
	private static JLanguageTool languageTool;

	public SpellChecker(StyleClassedTextArea textArea, ParagraphOverlayGraphicFactory overlayGraphicFactory) {
		this.textArea = textArea;
		this.overlayGraphicFactory = overlayGraphicFactory;

		enableDisable();

		// listen to option changes
		optionsListener = e -> {
			if (textArea.getScene() == null)
				return; // editor closed but not yet GCed

			if (e == Options.spellCheckerProperty())
				enableDisable();
		};
		WeakInvalidationListener weakOptionsListener = new WeakInvalidationListener(optionsListener);
		Options.spellCheckerProperty().addListener(weakOptionsListener);
	}

	private void enableDisable() {
		boolean spellChecker = Options.isSpellChecker();
		if (spellChecker && spellCheckerOverlayFactory == null) {
			if (executor == null) {
				executor = Executors.newSingleThreadExecutor(runnable -> {
					Thread thread = Executors.defaultThreadFactory().newThread(runnable);
					thread.setDaemon(true); // allow quitting app without shutting down executor
					return thread;
				});
			}

	        EventStream<PlainTextChange> textChanges = textArea.plainTextChanges();
			textChangesSubscribtion = textChanges
				.successionEnds(Duration.ofMillis(500))
				.supplyTask(this::checkAsync)
				.awaitLatest(textChanges)
				.subscribe(this::checkFinished);

	        spellCheckerOverlayFactory = new SpellCheckerOverlayFactory(() -> ruleMatches.get());
			overlayGraphicFactory.addOverlayFactory(spellCheckerOverlayFactory);

			//TODO check current text
		} else if (!spellChecker && spellCheckerOverlayFactory != null) {
			textChangesSubscribtion.unsubscribe();
			textChangesSubscribtion = null;

			overlayGraphicFactory.removeOverlayFactory(spellCheckerOverlayFactory);
			spellCheckerOverlayFactory = null;

			languageTool = null;

			if (executor != null) {
				executor.shutdown();
				executor = null;
			}
		}
	}

	private Task<List<RuleMatch>> checkAsync() {
        String text = textArea.getText();
        Task<List<RuleMatch>> task = new Task<List<RuleMatch>>() {
            @Override
            protected List<RuleMatch> call() throws Exception {
                return check(text);
            }
        };
        executor.execute(task);
        return task;
    }

	private void checkFinished(Try<List<RuleMatch>> result) {
		if (overlayGraphicFactory == null)
			return; // ignore result; user turned spell checking off

		if (result.isSuccess()) {
			ruleMatches.set(result.get());
			overlayGraphicFactory.update();
		} else {
			//TODO
			result.getFailure().printStackTrace();
		}
	}

	private List<RuleMatch> check(String text) throws IOException {
		if (languageTool == null)
			languageTool = new JLanguageTool(new AmericanEnglish());
		return languageTool.check(text);
	}
}
