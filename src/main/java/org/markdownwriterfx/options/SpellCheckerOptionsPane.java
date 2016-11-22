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
import org.markdownwriterfx.Messages;
import org.tbee.javafx.scene.layout.fxml.MigPane;

/**
 * Spell checker options pane
 *
 * @author Karl Tauber
 */
public class SpellCheckerOptionsPane
	extends MigPane
{
	public SpellCheckerOptionsPane() {
		initComponents();
	}

	void load() {
		spellCheckerCheckBox.setSelected(Options.isSpellChecker());
	}

	void save() {
		Options.setSpellChecker(spellCheckerCheckBox.isSelected());
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		spellCheckerCheckBox = new CheckBox();

		//======== this ========
		setCols("[fill]");
		setRows("[]");

		//---- spellCheckerCheckBox ----
		spellCheckerCheckBox.setText(Messages.get("SpellCheckerOptionsPane.spellCheckerCheckBox.text"));
		add(spellCheckerCheckBox, "cell 0 0,alignx left,growx 0");
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private CheckBox spellCheckerCheckBox;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
