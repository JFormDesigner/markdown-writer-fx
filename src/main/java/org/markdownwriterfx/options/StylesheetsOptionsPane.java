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

package org.markdownwriterfx.options;

import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.markdownwriterfx.Messages;
import org.markdownwriterfx.util.Utils;
import org.tbee.javafx.scene.layout.fxml.MigPane;

/**
 * Stylesheets options pane
 *
 * @author Karl Tauber
 */
class StylesheetsOptionsPane
	extends MigPane
{
	StylesheetsOptionsPane() {
		initComponents();
	}

	void load() {
		additionalCSSField.setText(Options.getAdditionalCSS());
	}

	void save() {
		Options.setAdditionalCSS(Utils.trimAndDefaultIfEmpty(additionalCSSField.getText(), null));
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		var additionalCSSLabel = new Label();
		additionalCSSField = new TextArea();

		//======== this ========
		setLayout("hidemode 3");
		setCols(
			"[fill]");
		setRows(
			"[]" +
			"[grow,fill]");

		//---- additionalCSSLabel ----
		additionalCSSLabel.setText(Messages.get("StylesheetsOptionsPane.additionalCSSLabel.text"));
		additionalCSSLabel.setLabelFor(additionalCSSField);
		additionalCSSLabel.setMnemonicParsing(true);
		add(additionalCSSLabel, "cell 0 0");
		add(additionalCSSField, "cell 0 1");
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private TextArea additionalCSSField;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
