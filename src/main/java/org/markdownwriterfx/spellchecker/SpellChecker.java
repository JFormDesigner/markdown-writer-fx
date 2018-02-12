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

import static javafx.scene.input.KeyCode.COMMA;
import static javafx.scene.input.KeyCode.PERIOD;
import static javafx.scene.input.KeyCombination.SHORTCUT_DOWN;
import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;
import static org.fxmisc.wellbehaved.event.InputMap.consume;
import static org.fxmisc.wellbehaved.event.InputMap.sequence;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.TextFlow;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.wellbehaved.event.Nodes;
import org.languagetool.markup.AnnotatedText;
import org.languagetool.markup.AnnotatedTextBuilder;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.spelling.SpellingCheckRule;
import org.markdownwriterfx.Messages;
import org.markdownwriterfx.addons.SpellCheckerAddon;
import org.markdownwriterfx.editor.MarkdownEditorPane;
import org.markdownwriterfx.editor.ParagraphOverlayGraphicFactory;
import org.markdownwriterfx.options.Options;
import org.markdownwriterfx.util.Range;
import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.reactfx.util.FxTimer;
import org.reactfx.util.Timer;
import org.reactfx.util.Try;
import com.vladsch.flexmark.ast.Block;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ast.NodeVisitor;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.Text;

/**
 * Spell checker for an instance of StyleClassedTextArea
 *
 * @author Karl Tauber
 */
public class SpellChecker
{
	private final MarkdownEditorPane editor;
	private final StyleClassedTextArea textArea;
	private final ParagraphOverlayGraphicFactory overlayGraphicFactory;
	private final InvalidationListener optionsListener;
	private final ChangeListener<Number> checkRequestIDListener;
	private ContextMenu quickFixMenu;
	private int lastQuickFixNavigationDirection;

	private List<SpellBlockProblems> spellProblems;

	private Subscription textChangesSubscribtion;
	private SpellCheckerOverlayFactory spellCheckerOverlayFactory;

	// global executor used for all spell checking
	private static ExecutorService executor;

	// global language tool used in executor for all spell checking
	private static final GlobalLanguageTool languageTool = new GlobalLanguageTool();

	private static final ServiceLoader<SpellCheckerAddon> addons = ServiceLoader.load(SpellCheckerAddon.class);

	public SpellChecker(MarkdownEditorPane editor, StyleClassedTextArea textArea,
		ParagraphOverlayGraphicFactory overlayGraphicFactory)
	{
		this.editor = editor;
		this.textArea = textArea;
		this.overlayGraphicFactory = overlayGraphicFactory;

		Nodes.addInputMap(textArea, sequence(
			consume(keyPressed(PERIOD, SHORTCUT_DOWN),		this::navigateNext),
			consume(keyPressed(COMMA, SHORTCUT_DOWN),		this::navigatePrevious)
		));

		enableDisable();

		// listen to option changes
		optionsListener = e -> {
			if (isEditorClosed())
				return; // editor closed but not yet GCed

			enableDisable();
		};
		WeakInvalidationListener weakOptionsListener = new WeakInvalidationListener(optionsListener);
		Options.spellCheckerProperty().addListener(weakOptionsListener);

		// listen to checkRequestID changes
		checkRequestIDListener = (observer, oldValue, newValue) -> {
			if (isEditorClosed())
				return; // editor closed but not yet GCed

			spellProblems = null;

			checkAsync(true);
		};
		languageTool.checkRequestIDProperty().addListener(new WeakChangeListener<>(checkRequestIDListener));
	}

	private boolean isEditorClosed() {
		return textArea.getScene() == null;
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

			// listen to text changes and invoke spell checker after a delay
			EventStream<PlainTextChange> textChanges = textArea.plainTextChanges();
			textChangesSubscribtion = textChanges
				.hook(this::updateSpellRangeOffsets)
				.successionEnds(Duration.ofMillis(500))
				.supplyTask(() -> checkAsync(false))
				.awaitLatest(textChanges)
				.subscribe(this::checkFinished);

			spellCheckerOverlayFactory = new SpellCheckerOverlayFactory(() -> spellProblems);
			overlayGraphicFactory.addOverlayFactory(spellCheckerOverlayFactory);

			checkAsync(true);

		} else if (!spellChecker && spellCheckerOverlayFactory != null) {
			textChangesSubscribtion.unsubscribe();
			textChangesSubscribtion = null;

			overlayGraphicFactory.removeOverlayFactory(spellCheckerOverlayFactory);
			spellCheckerOverlayFactory = null;

			spellProblems = null;

			if (executor != null) {
				executor.shutdown();
				executor = null;
			}
		}
	}

	private Task<List<SpellBlockProblems>> checkAsync(boolean invokeFinished) {
		Node astRoot = editor.getMarkdownAST();
		boolean updatePeriodically = (spellProblems == null || spellProblems.isEmpty());

		Task<List<SpellBlockProblems>> task = new Task<List<SpellBlockProblems>>() {
			@Override
			protected List<SpellBlockProblems> call() throws Exception {
				return check(astRoot, updatePeriodically);
			}
			@Override
			protected void succeeded() {
				if (invokeFinished)
					checkFinished(Try.success(getValue()));
			}
			@Override
			protected void failed() {
				if (invokeFinished)
					checkFinished(Try.failure(getException()));
			}
		};
		executor.execute(task);
		return task;
	}

	private void checkFinished(Try<List<SpellBlockProblems>> result) {
		if (isEditorClosed())
			return;

		if (overlayGraphicFactory == null)
			return; // ignore result; user turned spell checking off

		if (result.isSuccess()) {
			spellProblems = result.get();
			overlayGraphicFactory.update();
		} else {
			//TODO
			result.getFailure().printStackTrace();
		}
	}

	private List<SpellBlockProblems> check(Node astRoot, boolean updatePeriodically) throws IOException {
		// find nodes that should be checked
		ArrayList<Node> nodesToCheck = new ArrayList<>();
		NodeVisitor visitor = new NodeVisitor(Collections.emptyList()) {
			@Override
			public void visit(Node node) {
				if (node instanceof Paragraph || node instanceof Heading)
					nodesToCheck.add(node);

				if (node instanceof Block)
					visitChildren(node);
			}
		};
		visitor.visit(astRoot);

		if (nodesToCheck.isEmpty())
			return Collections.emptyList();

		// initialize language tool
		languageTool.initialize();

		ArrayList<SpellBlockProblems> spellProblems = new ArrayList<>();

		// start timer to update overlays periodically during a lengthy check (on initial run)
		// using FxTimer instead of Timeline because FxTimer makes sure
		// the action is not executed after invoking FxTimer.stop(),
		// which may happen for Timeline
		// see http://tomasmikula.github.io/blog/2014/06/04/timers-in-javafx-and-reactfx.html
		Timer timer = updatePeriodically
			? FxTimer.runPeriodically(Duration.ofMillis(350), () -> {
				ArrayList<SpellBlockProblems> spellProblems2;
				synchronized (spellProblems) {
					spellProblems2 = new ArrayList<>(spellProblems);
				}
				checkFinished(Try.success(spellProblems2));
			}) : null;

		// check spelling of nodes
		try {
			for (Node node : nodesToCheck) {
				if (isEditorClosed())
					return null;

				AnnotatedText annotatedText = annotatedNodeText(node);
				List<RuleMatch> ruleMatches;
				try {
					ruleMatches = languageTool.check(annotatedText);
				} catch (IllegalStateException ex) {
					return null; // user turned spell checking off
				}

				SpellBlockProblems problem = new SpellBlockProblems(node.getStartOffset(), node.getEndOffset(), ruleMatches);
				synchronized (spellProblems) {
					spellProblems.add(problem);
				}
			}
		} finally {
			if (timer != null)
				timer.stop();
		}

		return spellProblems;
	}

	private AnnotatedText annotatedNodeText(Node node) {
		AnnotatedTextBuilder builder = new AnnotatedTextBuilder();
		NodeVisitor visitor = new NodeVisitor(Collections.emptyList()) {
			int prevTextEnd = node.getStartOffset();

			@Override
			public void visit(Node node) {
				if (node instanceof Text)
					addText(node.getStartOffset(), node.getChars().toString());
				else if (node instanceof Code)
					addText(((Code)node).getText().getStartOffset(), ((Code)node).getText().toString());
				else if (node instanceof SoftLineBreak)
					addText(node.getStartOffset(), " ");
				else if (node instanceof HardLineBreak)
					addText(node.getStartOffset(), "\n");
				else
					visitChildren(node);
			}

			private void addText(int start, String text) {
				if (start > prevTextEnd)
					builder.addMarkup(getMarkupFiller(start - prevTextEnd));

				Range[] ranges = null;
				for (SpellCheckerAddon addon : addons) {
					ranges = addon.getAnnotatedRanges(text);
					if (ranges != null)
						break;
				}

				if (ranges != null) {
					int i = 0;
					for (Range range : ranges) {
						if (i < range.start)
							builder.addText(text.substring(i, range.start));
						builder.addMarkup(getMarkupFiller(range.end - range.start));
						i = range.end;
					}
					if (i < text.length())
						builder.addText(text.substring(i));
				} else
					builder.addText(text);
				prevTextEnd = start + text.length();
			}
		};
		visitor.visit(node);
		return builder.build();
	}

	private static final ArrayList<String> markupFiller = new ArrayList<>();
	private String getMarkupFiller(int length) {
		if (markupFiller.isEmpty()) {
			for (int i = 1; i <= 16; i++)
				markupFiller.add(StringUtils.repeat('#', i));
		}

		if (length <= markupFiller.size())
			return markupFiller.get(length - 1);
		return StringUtils.repeat('#', length);
	}

	private void updateSpellRangeOffsets(PlainTextChange e) {
		if (spellProblems == null)
			return;

		int position = e.getPosition();
		int inserted = e.getInserted().length();
		int removed = e.getRemoved().length();

		for (SpellBlockProblems blockProblems : spellProblems)
			blockProblems.updateOffsets(position, inserted, removed);
	}

	//---- context menu -------------------------------------------------------

	private void showQuickFixMenu() {
		editor.hideContextMenu();

		ContextMenu quickFixMenu = new ContextMenu();
		initQuickFixMenu(quickFixMenu, textArea.getCaretPosition(), false);

		if (quickFixMenu.getItems().isEmpty())
			return;

		Optional<Bounds> caretBounds = textArea.getCaretBounds();
		if (!caretBounds.isPresent())
			return;

		// show context menu
		quickFixMenu.show(textArea, caretBounds.get().getMaxX(), caretBounds.get().getMaxY());
		this.quickFixMenu = quickFixMenu;
	}

	public void initContextMenu(ContextMenu contextMenu, int characterIndex) {
		initQuickFixMenu(contextMenu, characterIndex, true);
		lastQuickFixNavigationDirection = 0;

		ObservableList<MenuItem> menuItems = contextMenu.getItems();

		// add separator (if necessary)
		if (!menuItems.isEmpty())
			menuItems.add(new SeparatorMenuItem());

		// add "Check Spelling and Grammar" item
		CheckMenuItem checkSpellingItem = new CheckMenuItem(Messages.get("SpellChecker.checkSpelling"));
		checkSpellingItem.selectedProperty().bindBidirectional(Options.spellCheckerProperty());
		menuItems.add(checkSpellingItem);
	}

	private void initQuickFixMenu(ContextMenu contextMenu, int characterIndex, boolean addNavigation) {
		if (!languageTool.isInitialized())
			return;

		// find problems
		List<SpellProblem> problems = findProblemsAt(characterIndex);

		// create menu items
		ArrayList<MenuItem> newItems = new ArrayList<>();
		for (SpellProblem problem : problems) {
			CustomMenuItem problemItem = new SeparatorMenuItem();
			problemItem.setContent(buildMessage(problem.getMessage()));
			newItems.add(problemItem);

			List<String> suggestedReplacements = problem.getSuggestedReplacements();
			if (!suggestedReplacements.isEmpty()) {
				// add suggestion items
				int count = 0;
				for (String suggestedReplacement : suggestedReplacements) {
					MenuItem item = new MenuItem(suggestedReplacement);
					item.getStyleClass().add("spell-menu-suggestion");
					item.setOnAction(e -> {
						textArea.replaceText(problem.getFromPos(), problem.getToPos(), suggestedReplacement);
						navigateNextPrevious();
					});
					newItems.add(item);

					// limit number of suggestions
					count++;
					if (count >= 20)
						break;
				}
			} else {
				// add "No suggestions available" item
				MenuItem item = new MenuItem(Messages.get("SpellChecker.noSuggestionsAvailable"));
				item.setDisable(true);
				newItems.add(item);
			}

			String word = textArea.getText(problem.getFromPos(), problem.getToPos());
			if (problem.isTypo() && !word.contains(" ")) {
				// add separator
				newItems.add(new SeparatorMenuItem());

				// add "Add to Dictionary" item
				MenuItem addDictItem = new MenuItem(Messages.get("SpellChecker.addToDictionary"));
				addDictItem.setOnAction(e -> {
					languageTool.addToUserDictionary(word);
					checkAsync(true);
					navigateNextPrevious();
				});
				newItems.add(addDictItem);

				// add "Ignore Word" item
				MenuItem ignoreItem = new MenuItem(Messages.get("SpellChecker.ignoreWord"));
				ignoreItem.setOnAction(e -> {
					languageTool.ignoreWord(word);
					checkAsync(true);
					navigateNextPrevious();
				});
				newItems.add(ignoreItem);
			}

			Rule rule = problem.getRule();
			if( !(rule instanceof SpellingCheckRule) ) {
				// add separator
				newItems.add(new SeparatorMenuItem());

				// add "Disable Rule" item
				MenuItem disableRuleItem = new MenuItem(Messages.get("SpellChecker.disableRule", rule.getDescription()));
				disableRuleItem.setOnAction(e -> {
					disableRule(rule.getId());
					navigateNextPrevious();
				});
				newItems.add(disableRuleItem);
			}
		}

		if (addNavigation) {
			// add separator (if necessary)
			if (!newItems.isEmpty())
				newItems.add(new SeparatorMenuItem());

			// add "Next Problem" item
			MenuItem nextProblemItem = new MenuItem(Messages.get("SpellChecker.nextProblem"));
			nextProblemItem.setAccelerator(KeyCombination.valueOf("Shortcut+."));
			nextProblemItem.setOnAction(e -> {
				navigateNext(null);
			});
			newItems.add(nextProblemItem);

			// add "Next Problem" item
			MenuItem previousProblemItem = new MenuItem(Messages.get("SpellChecker.previousProblem"));
			previousProblemItem.setAccelerator(KeyCombination.valueOf("Shortcut+,"));
			previousProblemItem.setOnAction(e -> {
				navigatePrevious(null);
			});
			newItems.add(previousProblemItem);
		}

		ObservableList<MenuItem> menuItems = contextMenu.getItems();

		// add separator (if necessary)
		if (!newItems.isEmpty() && !menuItems.isEmpty())
			newItems.add(new SeparatorMenuItem());

		// add new menu items to context menu
		menuItems.addAll(0, newItems);
	}

	public void hideContextMenu() {
		if (quickFixMenu != null) {
			quickFixMenu.hide();
			quickFixMenu = null;
		}
	}

	private static final Pattern SUGGESTION_PATTERN = Pattern.compile("<suggestion>(.*?)</suggestion>");

	private TextFlow buildMessage(String message) {
		ArrayList<javafx.scene.text.Text> texts = new ArrayList<>();
		Matcher matcher = SUGGESTION_PATTERN.matcher(message);
		int pos = 0;
		while (matcher.find(pos)) {
			int start = matcher.start();
			if (start > pos)
				texts.add(new javafx.scene.text.Text(message.substring(pos, start)));

			javafx.scene.text.Text text = new javafx.scene.text.Text(matcher.group(1));
			text.getStyleClass().add("spell-menu-message-suggestion");
			texts.add(new javafx.scene.text.Text("\""));
			texts.add(text);
			texts.add(new javafx.scene.text.Text("\""));

			pos = matcher.end();
		}
		if (pos < message.length())
			texts.add(new javafx.scene.text.Text(message.substring(pos)));

		TextFlow textFlow = new TextFlow(texts.toArray(new javafx.scene.text.Text[texts.size()])) {
			@Override
			protected double computePrefWidth(double height) {
				// limit width to 300
				return Math.min(super.computePrefWidth(height), 300);
			}
			@Override
			protected double computePrefHeight(double width) {
				// compute height based on maximum width
				return super.computePrefHeight(300);
			}
		};
		textFlow.getStyleClass().add("spell-menu-message");
		return textFlow;
	}

	private void disableRule(String ruleId) {
		// add to options (which triggers re-checking)
		List<String> disabledRules = new ArrayList<>(Arrays.asList(Options.getDisabledRules()));
		if (!disabledRules.contains(ruleId)) {
			disabledRules.add(ruleId);
			Options.setDisabledRules(disabledRules.toArray(new String[disabledRules.size()]));
		}
	}

	//---- navigation ---------------------------------------------------------

	private void navigateNextPrevious() {
		if (lastQuickFixNavigationDirection == 0)
			return;

		Platform.runLater(() -> {
			if (lastQuickFixNavigationDirection > 0)
				navigateNext(null);
			else if (lastQuickFixNavigationDirection < 0)
				navigatePrevious(null);
		});
	}

	private void navigateNext(KeyEvent e) {
		if (spellProblems == null)
			return;

		lastQuickFixNavigationDirection = 1;
		SpellProblem problem = findNextProblemAt(textArea.getSelection().getStart());
		if (problem == null)
			problem = findNextProblemAt(0);
		if (problem == null)
			return;

		selectProblem(problem);
		showQuickFixMenu();
	}

	private void navigatePrevious(KeyEvent e) {
		if (spellProblems == null)
			return;

		lastQuickFixNavigationDirection = -1;
		SpellProblem problem = findPreviousProblemAt(textArea.getSelection().getEnd());
		if (problem == null)
			problem = findPreviousProblemAt(textArea.getLength());
		if (problem == null)
			return;

		selectProblem(problem);
		showQuickFixMenu();
	}

	//---- utility ------------------------------------------------------------

	private void selectProblem(SpellProblem problem) {
		textArea.selectRange(problem.getFromPos(), problem.getToPos());
		editor.scrollCaretToVisible();
	}

	private List<SpellProblem> findProblemsAt(int index) {
		if (index < 0 || spellProblems == null || spellProblems.isEmpty())
			return Collections.emptyList();

		ArrayList<SpellProblem> result = new ArrayList<>();
		for (SpellBlockProblems blockProblems : spellProblems) {
			if (!blockProblems.contains(index))
				continue;

			for (SpellProblem problem : blockProblems.problems) {
				if (problem.contains(index))
					result.add(problem);
			}
		}
		return result;
	}

	private SpellProblem findNextProblemAt(int index) {
		for (SpellBlockProblems blockProblems : spellProblems) {
			if (index > blockProblems.getToPos())
				continue; // index is after block

			for (SpellProblem problem : blockProblems.problems) {
				if (index < problem.getFromPos())
					return problem;
			}
		}
		return null;
	}

	private SpellProblem findPreviousProblemAt(int index) {
		for (int i = spellProblems.size() - 1; i >= 0; i--) {
			SpellBlockProblems blockProblems = spellProblems.get(i);
			if (index < blockProblems.getFromPos())
				continue; // index is before block

			for (int j = blockProblems.problems.size() - 1; j >= 0; j--) {
				SpellProblem problem = blockProblems.problems.get(j);
				if (index > problem.getToPos())
					return problem;
			}
		}
		return null;
	}
}
