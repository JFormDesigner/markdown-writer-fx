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

package org.markdownwriterfx;

import java.util.prefs.Preferences;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.markdownwriterfx.options.Options;
import org.markdownwriterfx.util.StageState;

/**
 * Markdown Writer FX application.
 *
 * @author Karl Tauber
 */
public class MarkdownWriterFXApp
	extends Application
{
	private static Application app;

	private MainWindow mainWindow;
	@SuppressWarnings("unused")
	private StageState stageState;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		app = this;
		Options.load(getOptions());

		mainWindow = new MainWindow();

		stageState = new StageState(primaryStage, getState());

		primaryStage.getIcons().addAll(
				new Image("org/markdownwriterfx/markdownwriterfx16.png"),
				new Image("org/markdownwriterfx/markdownwriterfx32.png"),
				new Image("org/markdownwriterfx/markdownwriterfx128.png"),
				new Image("org/markdownwriterfx/markdownwriterfx256.png"),
				new Image("org/markdownwriterfx/markdownwriterfx512.png"));
		primaryStage.setTitle("Markdown Writer FX");
		primaryStage.setScene(mainWindow.getScene());
		primaryStage.show();
	}

	public static void showDocument(String uri) {
		app.getHostServices().showDocument(uri);
	}

	static private Preferences getPrefsRoot() {
		return Preferences.userRoot().node("markdownwriterfx");
	}

	static Preferences getOptions() {
		return getPrefsRoot().node("options");
	}

	public static Preferences getState() {
		return getPrefsRoot().node("state");
	}
}
