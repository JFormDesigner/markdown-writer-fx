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

import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyCode.UP;
import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;
import static org.fxmisc.wellbehaved.event.InputMap.consume;
import static org.fxmisc.wellbehaved.event.InputMap.sequence;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.controlsfx.control.textfield.CustomTextField;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.wellbehaved.event.Nodes;
import org.markdownwriterfx.Messages;
import org.markdownwriterfx.util.Range;
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

	private final List<HitsChangeListener> listeners = new ArrayList<>();
	private final StyleClassedTextArea textArea;
	private final List<Range> hits = new ArrayList<>();
	private int activeHitIndex = -1;
	private String nOfCountFormat;

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
		findAll(textArea.getText(), findField.getText(), false);
	}

	private void findAll(String text, String find, boolean selectActiveHit) {
		if (find.isEmpty()) {
			clearHits();
			return;
		}

		//TODO ignore case
		hits.clear();
		int fromIndex = 0;
		int hitIndex;
		while ((hitIndex = text.indexOf(find, fromIndex)) >= 0) {
			hits.add(new Range(hitIndex, hitIndex + find.length()));
			fromIndex = hitIndex + find.length();
		}
		setActiveHitIndex(hits.isEmpty() ? -1 : 0, selectActiveHit);
	}

	private void clearHits() {
		if (hits.isEmpty())
			return;

		hits.clear();
		setActiveHitIndex(-1, false);
	}

	private void findPrevious() {
		if (hits.size() < 1)
			return;

		int previous = activeHitIndex - 1;
		if (previous < 0)
			previous = hits.size() - 1;

		setActiveHitIndex(previous, true);
	}

	private void findNext() {
		if (hits.size() < 1)
			return;

		int next = activeHitIndex + 1;
		if (next >= hits.size())
			next = 0;

		setActiveHitIndex(next, true);
	}

	private void setActiveHitIndex(int index, boolean selectActiveHit) {
		activeHitIndex = index;

		update();
		if (selectActiveHit && activeHitIndex >= 0) {
			Range activeHit = getActiveHit();
			textArea.selectRange(activeHit.start, activeHit.end);
		}
		fireHitsChanged();
	}

	private void update() {
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
		previousButton.getStyleClass().addAll("previous", "flat-button");
		nextButton.getStyleClass().addAll("next", "flat-button");
		closeButton.getStyleClass().addAll("close", "flat-button");

		previousButton.setGraphic(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.CHEVRON_UP));
		nextButton.setGraphic(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.CHEVRON_DOWN));
		closeButton.setGraphic(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.CLOSE));

		previousButton.setTooltip(new Tooltip(Messages.get("FindReplacePane.previousButton.tooltip")));
		nextButton.setTooltip(new Tooltip(Messages.get("FindReplacePane.nextButton.tooltip")));
		closeButton.setTooltip(new Tooltip(Messages.get("FindReplacePane.closeButton.tooltip")));

		findField.setLeft(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.SEARCH));
		findField.setRight(nOfHitCountLabel);
		findField.textProperty().addListener((ov, o, n) -> findAll(textArea.getText(), n, true));
		Nodes.addInputMap(findField, sequence(
				consume(keyPressed(UP),		e -> findPrevious()),
				consume(keyPressed(DOWN),	e -> findNext()),
				consume(keyPressed(ENTER),	e -> findNext()),
				consume(keyPressed(ESCAPE),	e -> hide())));
		previousButton.setOnAction(e -> findPrevious());
		nextButton.setOnAction(e -> findNext());
		closeButton.setOnAction(e -> hide());

		nOfCountFormat = nOfHitCountLabel.getText();

		update();

		return pane;
	}

	void show() {
		visible.set(true);
		textChanged();
		findField.requestFocus();
	}

	void hide() {
		visible.set(false);
		clearHits();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		pane = new MigPane();
		findField = new CustomTextField();
		previousButton = new Button();
		nextButton = new Button();
		closeButton = new Button();
		nOfHitCountLabel = new Label();

		//======== pane ========
		{
			pane.setLayout("insets 0,hidemode 3");
			pane.setCols("[fill]0[fill]0[fill][grow,fill][fill]");
			pane.setRows("[fill]");

			//---- findField ----
			findField.setFocusTraversable(false);
			pane.add(findField, "cell 0 0,width :250:250");

			//---- previousButton ----
			previousButton.setFocusTraversable(false);
			pane.add(previousButton, "cell 1 0");

			//---- nextButton ----
			nextButton.setFocusTraversable(false);
			pane.add(nextButton, "cell 2 0");

			//---- closeButton ----
			closeButton.setFocusTraversable(false);
			pane.add(closeButton, "cell 4 0");
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
	private Button closeButton;
	private Label nOfHitCountLabel;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
