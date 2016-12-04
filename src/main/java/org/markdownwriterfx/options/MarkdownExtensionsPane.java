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

import java.util.Arrays;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import org.controlsfx.control.ToggleSwitch;
import org.markdownwriterfx.options.Options.RendererType;
import org.tbee.javafx.scene.layout.fxml.MigPane;

/**
 * Markdown extensions pane
 *
 * @author Karl Tauber
 */
public class MarkdownExtensionsPane
	extends MigPane
{
	private static class Ext {
		final String id;
		final String displayName;
		ToggleSwitch toggleSwitch;

		Ext(String id, String displayName) {
			this.id = id;
			this.displayName = displayName;
		}
	}

	private final Ext[] extensions;
	private final ListProperty<String> enabledExtensions = new SimpleListProperty<>();

	public MarkdownExtensionsPane() {
		this(false);
	}

	public MarkdownExtensionsPane(boolean popover) {
		setLayout(popover ? "insets dialog" : "insets 0");

		// get IDs of all available extensions
		String[] ids = MarkdownExtensions.ids();

		// create sorted array of available extensions
		extensions = Arrays.stream(ids)
			.map(id -> new Ext(id, MarkdownExtensions.displayName(id)))
			.sorted((e1, e2) -> e1.displayName.compareTo(e2.displayName))
			.toArray(Ext[]::new);

		// create toggle switches for all available extensions
		RendererType rendererType = Options.getMarkdownRenderer();
		for (Ext ext : extensions) {
			boolean available = MarkdownExtensions.isAvailable(rendererType, ext.id);
			if (popover && !available)
				continue;

			ext.toggleSwitch = new ToggleSwitch(ext.displayName);
			ext.toggleSwitch.selectedProperty().addListener((ob, oldSelected, newSelected) -> {
				if (newSelected) {
					if (!enabledExtensions.contains(ext.id))
						enabledExtensions.add(ext.id);
				} else
					enabledExtensions.remove(ext.id);
			});

			if (!popover && !available)
				ext.toggleSwitch.setDisable(true);

			add(ext.toggleSwitch, "grow, wrap");
		}

		// listener that updates toggle switch selection and option property
		enabledExtensions.addListener((obs, oldExtensions, newExtensions) -> {
			for (Ext ext : extensions) {
				if (ext.toggleSwitch != null)
					ext.toggleSwitch.setSelected(newExtensions.contains(ext.id));
			}

			if (popover)
				save();
		});

		// initialize from option property
		enabledExtensions.set(FXCollections.observableArrayList(Options.getMarkdownExtensions()));
	}

	void rendererTypeChanged(RendererType rendererType) {
		for (Ext ext : extensions)
			ext.toggleSwitch.setDisable(!MarkdownExtensions.isAvailable(rendererType, ext.id));
	}

	void save() {
		String[] newMarkdownExtensions = enabledExtensions.get().sorted().toArray(new String[0]);
		if (!Arrays.equals(newMarkdownExtensions, Options.getMarkdownExtensions()))
			Options.setMarkdownExtensions(newMarkdownExtensions);
	}
}
