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

package org.markdownwriterfx.editor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;
import org.fxmisc.richtext.StyleClassedTextArea;

/**
 * A paragraph graphic factory for StyleClassedTextArea that is able to lay out
 * nodes over paragraph texts.
 *
 * Normally paragraph graphics are displayed left to the paragraph text.
 * E.g. used for line numbers. This factory creates a zero size graphic node,
 * which lays out (outside of its bounds) its unmanaged children over paragraph text.
 *
 * @author Karl Tauber
 */
class ParagraphOverlayGraphicFactory
	implements IntFunction<Node>
{
	private final StyleClassedTextArea textArea;
	private final List<OverlayFactory> overlayFactories = new ArrayList<>();

	ParagraphOverlayGraphicFactory(StyleClassedTextArea textArea) {
		this.textArea = textArea;
	}

	void addOverlayFactory(OverlayFactory overlayFactory) {
		overlayFactories.add(overlayFactory);
		update();
	}

	void removeOverlayFactory(OverlayFactory overlayFactory) {
		overlayFactories.remove(overlayFactory);
		update();
	}

	void update() {
		// temporary remove paragraph graphic factory to update the view
		IntFunction<? extends Node> factory = textArea.getParagraphGraphicFactory();
		textArea.setParagraphGraphicFactory(null);
		textArea.setParagraphGraphicFactory(factory);
	}

	@Override
	public Node apply(int paragraphIndex) {
		return overlayFactories.isEmpty() ? null : new ParagraphGraphic(paragraphIndex);
	}

	//---- class ParagraphGraphic ---------------------------------------------

	private class ParagraphGraphic
		extends Pane
	{
		private final int paragraphIndex;

		ParagraphGraphic(int paragraphIndex) {
			this.paragraphIndex = paragraphIndex;

			setPrefWidth(0);
			setPrefHeight(0);

			// make this node is the first child so that its nodes are rendered
			// 'under' the paragraph text
			parentProperty().addListener((observable, oldParent, newParent) -> {
				if (newParent != null && newParent.getChildrenUnmodifiable().get(0) != this) {
					@SuppressWarnings("unchecked")
					ObservableList<Node> children = (ObservableList<Node>) invoke(mGetChildren, newParent);
					children.remove(this);
					children.add(0, this);
				}
			});
		}

		@Override
		protected void layoutChildren() {
			update();
		}

		private void update() {
			getChildren().clear();

			if (getParent() == null)
				return;

			Node paragraphTextNode = getParent().lookup(".paragraph-text");
			Insets insets = ((Region)paragraphTextNode).getInsets();
			double leftInsets = insets.getLeft();
			double topInsets = insets.getTop();

			for (OverlayFactory overlayFactory : overlayFactories) {
				overlayFactory.init(textArea, paragraphTextNode);
				Node[] nodes = overlayFactory.createOverlayNodes(paragraphIndex);
				if (nodes == null)
					continue;

				for (Node node : nodes) {
					node.setManaged(false);
					if (leftInsets != 0)
						node.setLayoutX(node.getLayoutX() + leftInsets);
					if (topInsets != 0)
						node.setLayoutY(node.getLayoutY() + topInsets);
				}
				getChildren().addAll(nodes);
			}
		}
	}

	//---- class OverlayFactory -----------------------------------------------

	static abstract class OverlayFactory
	{
		private StyleClassedTextArea textArea;
		private Node paragraphTextNode;

		private void init(StyleClassedTextArea textArea, Node paragraphTextNode) {
			this.textArea = textArea;
			this.paragraphTextNode = paragraphTextNode;
		}

		abstract Node[] createOverlayNodes(int paragraphIndex);

		protected StyleClassedTextArea getTextArea() {
			return textArea;
		}

		protected PathElement[] getShape(int start, int end) {
			return (PathElement[]) invoke(mGetRangeShape, paragraphTextNode, start, end);
		}

		protected Rectangle2D getBounds(int start, int end) {
			PathElement[] shape = getShape(start, end);
			double minX = 0, minY = 0, maxX = 0, maxY = 0;
			for (PathElement pathElement : shape) {
				if (pathElement instanceof MoveTo) {
					MoveTo moveTo = (MoveTo) pathElement;
					minX = maxX = moveTo.getX();
					minY = maxY = moveTo.getY();
				} else if (pathElement instanceof LineTo) {
					LineTo lineTo = (LineTo) pathElement;
					double x = lineTo.getX();
					double y = lineTo.getY();
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					maxX = Math.max(maxX, x);
					maxY = Math.max(maxY, y);
				}
			}
			return new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
		}
	}

	//---- reflection utilities -----------------------------------------------

	private static Method mGetChildren;
	private static Method mGetRangeShape;

	static {
		try {
			mGetChildren = Parent.class.getDeclaredMethod("getChildren");
			mGetChildren.setAccessible(true);

			Class<?> textFlowExtClass = Class.forName("org.fxmisc.richtext.skin.TextFlowExt");
			mGetRangeShape = textFlowExtClass.getDeclaredMethod("getRangeShape", int.class, int.class);
			mGetRangeShape.setAccessible(true);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static Object invoke(Method m, Object obj, Object... args) {
		try {
			return m.invoke(obj, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
			throw new RuntimeException(ex);
		}
	}
}
