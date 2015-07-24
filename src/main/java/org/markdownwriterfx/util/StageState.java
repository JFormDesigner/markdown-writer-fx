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

package org.markdownwriterfx.util;

import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * Saves and restores Stage state (window bounds, maximized, fullScreen).
 *
 * @author Karl Tauber
 */
public class StageState
{
	private final Stage stage;
	private final Preferences state;

	private Rectangle normalBounds;
	private boolean runLaterPending;

	public StageState(Stage stage, Preferences state) {
		this.stage = stage;
		this.state = state;

		restore();

		stage.addEventHandler(WindowEvent.WINDOW_HIDING, e -> save());

		stage.xProperty().addListener((ob, o, n) -> boundsChanged());
		stage.yProperty().addListener((ob, o, n) -> boundsChanged());
		stage.widthProperty().addListener((ob, o, n) -> boundsChanged());
		stage.heightProperty().addListener((ob, o, n) -> boundsChanged());
	}

	private void save() {
		Rectangle bounds = isNormalState() ? getStageBounds() : normalBounds;
		if (bounds != null) {
			state.putDouble("windowX", bounds.getX());
			state.putDouble("windowY", bounds.getY());
			state.putDouble("windowWidth", bounds.getWidth());
			state.putDouble("windowHeight", bounds.getHeight());
		}
		state.putBoolean("windowMaximized", stage.isMaximized());
		state.putBoolean("windowFullScreen", stage.isFullScreen());
	}

	private void restore() {
		double x = state.getDouble("windowX", Double.NaN);
		double y = state.getDouble("windowY", Double.NaN);
		double w = state.getDouble("windowWidth", Double.NaN);
		double h = state.getDouble("windowHeight", Double.NaN);
		boolean maximized = state.getBoolean("windowMaximized", false);
		boolean fullScreen = state.getBoolean("windowFullScreen", false);

		if (!Double.isNaN(x) && !Double.isNaN(y)) {
			stage.setX(x);
			stage.setY(y);
		} // else: default behavior is center on screen

		if (!Double.isNaN(w) && !Double.isNaN(h)) {
			stage.setWidth(w);
			stage.setHeight(h);
		} // else: default behavior is use scene size

		if (fullScreen != stage.isFullScreen())
			stage.setFullScreen(fullScreen);
		if (maximized != stage.isMaximized())
			stage.setMaximized(maximized);
	}

	/**
	 * Remembers the window bounds when the window
	 * is not iconified, maximized or in fullScreen.
	 */
	private void boundsChanged() {
		// avoid too many (and useless) runLater() invocations
		if (runLaterPending)
			return;
		runLaterPending = true;

		// must use runLater() to ensure that change of all properties
		// (x, y, width, height, iconified, maximized and fullScreen)
		// has finished
		Platform.runLater(() -> {
			runLaterPending = false;

			if (isNormalState())
				normalBounds = getStageBounds();
		});
	}

	private boolean isNormalState() {
		return !stage.isIconified() && !stage.isMaximized() && !stage.isFullScreen();
	}

	private Rectangle getStageBounds() {
		return new Rectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
	}
}
