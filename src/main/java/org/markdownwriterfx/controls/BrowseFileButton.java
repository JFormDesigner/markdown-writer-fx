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

package org.markdownwriterfx.controls;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.markdownwriterfx.Messages;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;

/**
 * Button that opens a file chooser to select a local file for a URL in markdown.
 *
 * @author Karl Tauber
 */
public class BrowseFileButton
	extends Button
{
	private final List<ExtensionFilter> extensionFilters = new ArrayList<>();

	public BrowseFileButton() {
		setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.FILE_ALT, "1.2em"));
		setTooltip(new Tooltip(Messages.get("BrowseFileButton.tooltip")));
		setOnAction(this::browse);

		disableProperty().bind(basePath.isNull());

		// workaround for a JavaFX bug:
		//   avoid closing the dialog that contains this control when the user
		//   closes the FileChooser or DirectoryChooser using the ESC key
		addEventHandler(KeyEvent.KEY_RELEASED, e-> {
			if (e.getCode() == KeyCode.ESCAPE)
				e.consume();
		});
	}

	public void addExtensionFilter(ExtensionFilter extensionFilter) {
		extensionFilters.add(extensionFilter);
	}

	// 'basePath' property
	private final ObjectProperty<Path> basePath = new SimpleObjectProperty<>();
	public Path getBasePath() { return basePath.get(); }
	public void setBasePath(Path basePath) { this.basePath.set(basePath); }
	public ObjectProperty<Path> basePathProperty() { return basePath; }

	// 'url' property
	private final ObjectProperty<String> url = new SimpleObjectProperty<>();
	public String getUrl() { return url.get(); }
	public void setUrl(String url) { this.url.set(url); }
	public ObjectProperty<String> urlProperty() { return url; }

	protected void browse(ActionEvent e) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(Messages.get("BrowseFileButton.chooser.title"));
		fileChooser.getExtensionFilters().addAll(extensionFilters);
		fileChooser.getExtensionFilters().add(new ExtensionFilter(Messages.get("BrowseFileButton.chooser.allFilesFilter"), "*.*"));
		fileChooser.setInitialDirectory(getInitialDirectory());
		File result = fileChooser.showOpenDialog(getScene().getWindow());
		if (result != null)
			updateUrl(result);
	}

	protected File getInitialDirectory() {
		//TODO build initial directory based on current value of 'url' property
		return getBasePath().toFile();
	}

	protected void updateUrl(File file) {
		String newUrl;
		try {
			newUrl = getBasePath().relativize(file.toPath()).toString();
		} catch (IllegalArgumentException ex) {
			newUrl = file.toString();
		}
		url.set(newUrl.replace('\\', '/'));
	}
}
