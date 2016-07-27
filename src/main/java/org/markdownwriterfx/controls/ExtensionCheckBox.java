/*
 * Copyright (c) 2016 Karl Tauber <karl at jformdesigner dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright
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

package org.markdownwriterfx.controls;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.CheckBox;

/**
 * CheckBox that addes/removes an extension name to a list of extensions.
 *
 * @author Karl Tauber
 */
public class ExtensionCheckBox
	extends CheckBox
{
	public ExtensionCheckBox() {
		setOnAction(e -> {
			if (isSelected()) {
				if (!extensionsProperty().contains(getExtension()))
					extensionsProperty().add(getExtension());
			} else
				extensionsProperty().remove(getExtension());
		});

		extensions.addListener((obs, oldExtensions, newExtensions) -> {
			setSelected(newExtensions.contains(getExtension()));
		});
	}

	// 'extension' property
	private final StringProperty extension = new SimpleStringProperty();
	public String getExtension() { return extension.get(); }
	public void setExtension(String extension) { this.extension.set(extension); }
	public void setExtensionClass(Class<?> extension) { this.extension.set(extension.getName()); }
	public StringProperty extensionProperty() { return extension; }

	// 'extensions' property
	private final ListProperty<String> extensions = new SimpleListProperty<>();
	public ListProperty<String> extensionsProperty() { return extensions; }
}
