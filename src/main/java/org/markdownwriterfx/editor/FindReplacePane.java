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

package org.markdownwriterfx.editor;

import javafx.scene.control.ToggleButton;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyCode.F3;
import static javafx.scene.input.KeyCode.H;
import static javafx.scene.input.KeyCode.UP;
import static javafx.scene.input.KeyCombination.SHORTCUT_DOWN;
import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;
import static org.fxmisc.wellbehaved.event.InputMap.consume;
import static org.fxmisc.wellbehaved.event.InputMap.sequence;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.textfield.CustomTextField;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.TwoDimensional.Bias;
import org.fxmisc.wellbehaved.event.Nodes;
import org.markdownwriterfx.MarkdownWriterFXApp;
import org.markdownwriterfx.Messages;
import org.markdownwriterfx.util.PrefsBooleanProperty;
import org.markdownwriterfx.util.Range;
import org.markdownwriterfx.util.Utils;
import org.tbee.javafx.scene.layout.fxml.MigPane;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

/**
 * @author Karl Tauber
 */
class FindReplacePane
{
	interface HitsChangeListener {
		void hitsChanged();
	}

	private static PrefsBooleanProperty matchCase = new PrefsBooleanProperty(
			MarkdownWriterFXApp.getState(), "findMatchCase", false);
	private static PrefsBooleanProperty regex = new PrefsBooleanProperty(
			MarkdownWriterFXApp.getState(), "findRegex", false);

	private final List<HitsChangeListener> listeners = new ArrayList<>();
	private final StyleClassedTextArea textArea;
	private final List<Range> hits = new ArrayList<>();
	private int activeHitIndex = -1;
	private String nOfCountFormat;

	private Region overviewRuler;

	FindReplacePane(StyleClassedTextArea textArea) {
		this.textArea = textArea;
	}

	// 'visible' property
	private final SimpleBooleanProperty visible = new SimpleBooleanProperty();
	ReadOnlyBooleanProperty visibleProperty() { return visible; }

	void addListener(HitsChangeListener listener) {
		listeners.add(listener);
	}

	void removeListener(HitsChangeListener listener) {
		listeners.remove(listener);
	}

	private void fireHitsChanged() {
		for (HitsChangeListener listener : listeners)
			listener.hitsChanged();
	}

	List<Range> getHits() {
		return hits;
	}

	Range getActiveHit() {
		return (activeHitIndex >= 0) ? hits.get(activeHitIndex) : null;
	}

	int getActiveHitIndex() {
		return activeHitIndex;
	}

	boolean hasHits() {
		return activeHitIndex >= 0;
	}

	void textChanged() {
		findAll(false);
	}

	private void findAll(boolean selectActiveHit) {
		findAll(textArea.getText(), findField.getText(), selectActiveHit);
	}

	private void findAll(String text, String find, boolean selectActiveHit) {
		findInfoLabel.setText(null);

		if (find.isEmpty()) {
			clearHits();
			return;
		}

		boolean matchCase = matchCaseButton.isSelected();
		boolean regex = regexButton.isSelected();

		hits.clear();

		// find
		if (regex) {
			String pattern = matchCase ? find : ("(?i)" + find);
			try {
				Matcher matcher = Pattern.compile(pattern).matcher(text);
				while (matcher.find())
					hits.add(new Range(matcher.start(), matcher.end()));
			} catch (PatternSyntaxException ex) {
				findInfoLabel.setText(Messages.get("FindReplacePane.infoLabel.regexError"));
			}
		} else {
			int fromIndex = 0;
			int hitIndex;
			while ((hitIndex = matchCase
					? text.indexOf(find, fromIndex)
					: StringUtils.indexOfIgnoreCase(text, find, fromIndex)) >= 0)
			{
				hits.add(new Range(hitIndex, hitIndex + find.length()));
				fromIndex = hitIndex + find.length();
			}
		}

		if (hits.isEmpty()) {
			setActiveHitIndex(-1, selectActiveHit);
			updateOverviewRuler();
			return;
		}

		// find active hit index after current selection
		int anchor = textArea.getAnchor();
		int index = Collections.binarySearch(hits, new Range(anchor, anchor), (r1, r2) -> {
			return r1.end - r2.start;
		});
		if (index < 0) {
			index = -index - 1;
			if (index >= hits.size())
				index = 0; // wrap
		}
		setActiveHitIndex(index, selectActiveHit);
		updateOverviewRuler();
	}

	private void clearHits() {
		hits.clear();
		setActiveHitIndex(-1, false);
		updateOverviewRuler();
	}

	void findPrevious() {
		if (hits.size() < 1)
			return;

		int previous = activeHitIndex - 1;
		if (previous < 0)
			previous = hits.size() - 1;

		setActiveHitIndex(previous, true);
	}

	void findNext() {
		if (hits.size() < 1)
			return;

		int next = activeHitIndex + 1;
		if (next >= hits.size())
			next = 0;

		setActiveHitIndex(next, true);
	}

	private void setActiveHitIndex(int index, boolean selectActiveHit) {
		int oldActiveHitIndex = activeHitIndex;
		activeHitIndex = index;

		update();

		if (selectActiveHit)
			selectActiveHit();

		if (oldActiveHitIndex < 0 && activeHitIndex < 0)
			return; // not necessary to fire event

		fireHitsChanged();
	}

	private void selectActiveHit() {
		if (activeHitIndex < 0)
			return;

		Range activeHit = getActiveHit();
		textArea.selectRange(activeHit.start, activeHit.end);
	}

	private void replace() {
		Utils.error(replaceField, false);
		replaceInfoLabel.setText(null);

		Range activeHit = getActiveHit();
		if (activeHit == null)
			return;

		String replace = replaceField.getText();
		Pattern regexReplacePattern = regexReplacePattern();
		if (regexReplacePattern != null) {
			replace = regexReplace(regexReplacePattern, activeHit, replace);
			if (replace == null)
				return; // error
		}
		textArea.replaceText(activeHit.start, activeHit.end, replace);

		selectActiveHit();
	}

	private void replaceAll() {
		Utils.error(replaceField, false);
		replaceInfoLabel.setText(null);

		if (hits.isEmpty())
			return;

		// Note: using single textArea.replaceText() to avoid multiple changes to undo history

		final String replace = replaceField.getText();
		Range first = hits.get(0);
		Range last = hits.get(hits.size() - 1);
		Pattern regexReplacePattern = regexReplacePattern();

		int estimatedSize = last.end - first.start + (replace.length() * hits.size());
		StringBuilder buf = new StringBuilder(estimatedSize);
		Range prev = null;
		for (Range hit : hits) {
			if (prev != null)
				buf.append(textArea.getText(prev.end, hit.start));

			String replace2 = replace;
			if (regexReplacePattern != null) {
				replace2 = regexReplace(regexReplacePattern, hit, replace);
				if (replace2 == null)
					return; // error
			}
			buf.append(replace2);
			prev = hit;
		}

		textArea.replaceText(first.start, last.end, buf.toString());

		int caret = first.start + buf.length();
		textArea.selectRange(caret, caret);
		textArea.requestFocus();
	}

	private Pattern regexReplacePattern() {
		if (!regexButton.isSelected())
			return null;

		String replace = replaceField.getText();
		if (replace.indexOf('$') < 0 && replace.indexOf('\\') < 0) {
			// replacement does not contain special characters
			// --> no need to do regex replace
			return null;
		}

		String find = findField.getText();
		String pattern = matchCaseButton.isSelected() ? find : ("(?i)" + find);
		try {
			return Pattern.compile(pattern);
		} catch (PatternSyntaxException ex) {
			return null;
		}
	}

	private String regexReplace(Pattern regexReplacePattern, Range hit, String replace) {
		try {
			String text = textArea.getText(hit.start, hit.end);
			return regexReplacePattern.matcher(text).replaceFirst(replace);
		} catch (IllegalArgumentException|IndexOutOfBoundsException ex) {
			Utils.error(replaceField, true);
			replaceInfoLabel.setText(ex.getMessage());
			return null;
		}
	}

	private void updateOverviewRuler() {
		if (overviewRuler == null) {
			if (hits.isEmpty())
				return;

			ScrollBar vScrollBar = Utils.findVScrollBar(textArea.getParent());
			if (vScrollBar == null)
				return;

			overviewRuler = (Region) vScrollBar.lookup(".track");
			if (overviewRuler == null)
				return;

			overviewRuler.heightProperty().addListener((ob) -> updateOverviewRuler());
		}

		if (hits.isEmpty() || !overviewRuler.isVisible()) {
			overviewRuler.setStyle(null);
			return;
		}

		int hitCount = hits.size();
		double height = overviewRuler.getHeight();
		int lineCount = textArea.getParagraphs().size();

		// compute top insets of hits
		int[] markerY = new int[hitCount];
		int[] markerHeight = new int[hitCount];
		boolean hasMergedMarker = false;
		int markerCount = 0;
		int previousY = -100;
		for (Range hit : hits) {
			int line = textArea.offsetToPosition(hit.start, Bias.Backward).getMajor();
			int y = (int) (height * line / lineCount);
			if (y < 0 || y == previousY)
				continue; // avoid duplicates

			if (y - 1 == previousY) {
				// merge with previous
				markerHeight[markerCount - 1]++;
				hasMergedMarker = true;
				previousY = y;
				continue;
			}

			previousY = y;

			markerY[markerCount] = y;
			markerHeight[markerCount] = 1;
			markerCount++;
		}

		// build CSS border width and colors
		StringBuilder buf = new StringBuilder();
		buf.append("-fx-border-width: ");
		if (hasMergedMarker) {
			for (int i = 0; i < markerCount; i++) {
				if (markerHeight[i] > 1)
					buf.append(markerHeight[i]).append( " 0 0 0,");
			}
		}
		buf.append( "1 0 0 0; -fx-border-color: ");
		for (int i = 0; i < markerCount; i++) {
			if (i > 0)
				buf.append(',');
			buf.append("-mwfx-hit");
		}

		// build CSS border insets
		buf.append("; -fx-border-insets: ");
		if (hasMergedMarker) {
			for (int i = 0; i < markerCount; i++) {
				if (markerHeight[i] > 1)
					buf.append(markerY[i]).append(" 0 0 0,");
			}
		}
		for (int i = 0; i < markerCount; i++) {
			if (markerHeight[i] == 1)
				buf.append(markerY[i]).append(" 0 0 0,");
		}
		buf.setLength(buf.length() - 1); // remove last ','

		overviewRuler.setStyle(buf.toString());
	}

	private void update() {
		Utils.error(findField, activeHitIndex < 0 && !findField.getText().isEmpty());

		nOfHitCountLabel.setText(findField.getText().isEmpty()
				? ""
				: MessageFormat.format(nOfCountFormat, activeHitIndex + 1, hits.size()));

		boolean disabled = hits.isEmpty();
		previousButton.setDisable(disabled);
		nextButton.setDisable(disabled);
	}

	Node getNode() {
		if (pane != null)
			return pane;

		initComponents();

		pane.getStyleClass().add("find-replace");
		findField.getStyleClass().add("find");
		previousButton.getStyleClass().addAll("previous", "flat-button");
		nextButton.getStyleClass().addAll("next", "flat-button");
		matchCaseButton.getStyleClass().add("flat-button");
		regexButton.getStyleClass().add("flat-button");
		closeButton.getStyleClass().addAll("close", "flat-button");
		findInfoLabel.getStyleClass().add("info");
		replaceInfoLabel.getStyleClass().add("info");

		previousButton.setGraphic(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.CHEVRON_UP));
		nextButton.setGraphic(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.CHEVRON_DOWN));
		closeButton.setGraphic(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.CLOSE));

		previousButton.setTooltip(new Tooltip(Messages.get("FindReplacePane.previousButton.tooltip")));
		nextButton.setTooltip(new Tooltip(Messages.get("FindReplacePane.nextButton.tooltip")));
		matchCaseButton.setTooltip(new Tooltip(Messages.get("FindReplacePane.matchCaseButton.tooltip")));
		regexButton.setTooltip(new Tooltip(Messages.get("FindReplacePane.regexButton.tooltip")));
		closeButton.setTooltip(new Tooltip(Messages.get("FindReplacePane.closeButton.tooltip")));

		findField.setLeft(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.SEARCH));
		findField.setRight(nOfHitCountLabel);
		findField.textProperty().addListener((ov, o, n) -> findAll(true));
		Nodes.addInputMap(findField, sequence(
				// don't know why, but Ctrl+H (set in menubar) does not work if findField has focus
				consume(keyPressed(H, SHORTCUT_DOWN), e -> show(true, false)),
				// don't know why, but F3 (set in menubar) does not work if findField has focus
				consume(keyPressed(F3),		e -> findNext()),

				consume(keyPressed(UP),		e -> findPrevious()),
				consume(keyPressed(DOWN),	e -> findNext()),
				consume(keyPressed(ENTER),	e -> findNext()),
				consume(keyPressed(ESCAPE),	e -> hide())));
		previousButton.setOnAction(e -> findPrevious());
		nextButton.setOnAction(e -> findNext());
		closeButton.setOnAction(e -> hide());

		matchCaseButton.setOnAction(e -> {
			findAll(true);
			matchCase.set(matchCaseButton.isSelected());
		} );
		regexButton.setOnAction(e -> {
			findAll(true);
			regex.set(regexButton.isSelected());
		});
		matchCaseButton.setSelected(matchCase.get());
		regexButton.setSelected(regex.get());

		nOfCountFormat = nOfHitCountLabel.getText();


		replacePane.setVisible(false);

		replaceField.setLeft(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.RETWEET));
		Nodes.addInputMap(replaceField, sequence(
				// don't know why, but F3 (set in menubar) does not work if replaceField has focus
				consume(keyPressed(F3),		e -> findNext()),

				consume(keyPressed(UP),		e -> findPrevious()),
				consume(keyPressed(DOWN),	e -> findNext()),
				consume(keyPressed(ENTER),	e -> replace()),
				consume(keyPressed(ENTER, SHORTCUT_DOWN), e -> replaceAll()),
				consume(keyPressed(ESCAPE),	e -> hide())));
		replaceButton.setOnAction(e -> replace());
		replaceAllButton.setOnAction(e -> replaceAll());

		update();

		return pane;
	}

	void show(boolean replace, boolean findSelection) {
		if (replace)
			replacePane.setVisible(true);

		boolean oldVisible = visible.get();
		visible.set(true);
		textChanged();

		if (findSelection) {
			String selectedText = textArea.getSelectedText();
			if (!selectedText.isEmpty() && selectedText.indexOf('\n') < 0)
				findField.setText(selectedText);
		}

		if (replace && oldVisible)
			replaceField.requestFocus();
		else
			findField.requestFocus();
	}

	void hide() {
		visible.set(false);
		replacePane.setVisible(false);
		clearHits();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		pane = new MigPane();
		findField = new CustomTextField();
		previousButton = new Button();
		nextButton = new Button();
		matchCaseButton = new ToggleButton();
		regexButton = new ToggleButton();
		findInfoLabel = new Label();
		closeButton = new Button();
		replacePane = new MigPane();
		replaceField = new CustomTextField();
		replaceButton = new Button();
		replaceAllButton = new Button();
		replaceInfoLabel = new Label();
		nOfHitCountLabel = new Label();

		//======== pane ========
		{
			pane.setLayout("insets 0,hidemode 3");
			pane.setCols("[fill][fill]0[fill][pref:n,fill]1px[pref:n,fill][grow,shrinkprio 200,fill][fill]");
			pane.setRows("[fill]0[]");

			//---- findField ----
			findField.setPromptText(Messages.get("FindReplacePane.findField.promptText"));
			pane.add(findField, "cell 0 0,width 200:200:200");

			//---- previousButton ----
			previousButton.setFocusTraversable(false);
			pane.add(previousButton, "cell 1 0");

			//---- nextButton ----
			nextButton.setFocusTraversable(false);
			pane.add(nextButton, "cell 2 0");

			//---- matchCaseButton ----
			matchCaseButton.setText("Aa");
			matchCaseButton.setFocusTraversable(false);
			pane.add(matchCaseButton, "cell 3 0");

			//---- regexButton ----
			regexButton.setText(".*");
			regexButton.setFocusTraversable(false);
			pane.add(regexButton, "cell 4 0");
			pane.add(findInfoLabel, "cell 5 0");

			//---- closeButton ----
			closeButton.setFocusTraversable(false);
			pane.add(closeButton, "cell 6 0");

			//======== replacePane ========
			{
				replacePane.setLayout("insets rel 0 0 0");
				replacePane.setCols("[fill][pref:n,fill][pref:n,fill][grow,shrinkprio 200,fill]");
				replacePane.setRows("[]");

				//---- replaceField ----
				replaceField.setPromptText(Messages.get("FindReplacePane.replaceField.promptText"));
				replacePane.add(replaceField, "cell 0 0,width 200:200:200");

				//---- replaceButton ----
				replaceButton.setText(Messages.get("FindReplacePane.replaceButton.text"));
				replaceButton.setFocusTraversable(false);
				replacePane.add(replaceButton, "cell 1 0");

				//---- replaceAllButton ----
				replaceAllButton.setText(Messages.get("FindReplacePane.replaceAllButton.text"));
				replaceAllButton.setFocusTraversable(false);
				replacePane.add(replaceAllButton, "cell 2 0");
				replacePane.add(replaceInfoLabel, "cell 3 0");
			}
			pane.add(replacePane, "cell 0 1 7 1");
		}

		//---- nOfHitCountLabel ----
		nOfHitCountLabel.setText(Messages.get("FindReplacePane.nOfHitCountLabel.text"));
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private MigPane pane;
	private CustomTextField findField;
	private Button previousButton;
	private Button nextButton;
	private ToggleButton matchCaseButton;
	private ToggleButton regexButton;
	private Label findInfoLabel;
	private Button closeButton;
	private MigPane replacePane;
	private CustomTextField replaceField;
	private Button replaceButton;
	private Button replaceAllButton;
	private Label replaceInfoLabel;
	private Label nOfHitCountLabel;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
