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

package org.markdownwriterfx.controls;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/**
 * Lays out children in center and bottom positions.
 * Uses animation to slide-in bottom node.
 *
 * @author Karl Tauber
 */
public class BottomSlidePane
	extends Pane
{
	private final DoubleProperty bottomVisibility = new SimpleDoubleProperty(1);

	public BottomSlidePane() {
		ChangeListener<? super Node> addRemoveChildListener = (ov, oldNode, newNode) -> {
			if (oldNode != null)
				getChildren().remove(oldNode);
			if (newNode != null) {
				getChildren().add(newNode);

				if (ov == bottomProperty())
					showBottom();
			}
		};
		centerProperty().addListener(addRemoveChildListener);
		bottomProperty().addListener(addRemoveChildListener);
		bottomVisibility.addListener(ob -> requestLayout());
	}

	public BottomSlidePane(Node center) {
		this();
		setCenter(center);
	}

	// 'center' property
	private final ObjectProperty<Node> center = new SimpleObjectProperty<>();
	public Node getCenter() { return center.get(); }
	public void setCenter(Node center) { this.center.set(center); }
	public ObjectProperty<Node> centerProperty() { return center; }

	// 'bottom' property
	private final ObjectProperty<Node> bottom = new SimpleObjectProperty<>();
	public Node getBottom() { return bottom.get(); }
	public void setBottom(Node bottom) { this.bottom.set(bottom); }
	public ObjectProperty<Node> bottomProperty() { return bottom; }

	private void showBottom() {
		bottomVisibility.set(0);
		KeyValue keyValue = new KeyValue(bottomVisibility, 1);
		Duration duration = Duration.millis(100);
		new Timeline(new KeyFrame(duration, keyValue))
			.play();
	}

	@Override
	protected void layoutChildren() {
		double width = getWidth();
		double height = getHeight();

		Node center = getCenter();
		Node bottom = getBottom();

		double bottomHeight = 0;
		if (bottom != null) {
			double bottomPrefHeight = bottom.prefHeight(-1);
			bottomHeight = bottomPrefHeight * bottomVisibility.get();

			layoutInArea(bottom, 0, height - bottomHeight, width, bottomPrefHeight, 0, HPos.CENTER, VPos.CENTER);
		}

		if (center != null)
			layoutInArea(center, 0, 0, width, height - bottomHeight, 0, HPos.CENTER, VPos.CENTER);
	}
}
