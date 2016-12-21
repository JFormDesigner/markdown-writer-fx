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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.IntFunction;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
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
public class ParagraphOverlayGraphicFactory
	implements IntFunction<Node>
{
	private final StyleClassedTextArea textArea;
	private final List<OverlayFactory> overlayFactories = new ArrayList<>();
	private final List<IntFunction<Node>> gutterFactories = new ArrayList<>();

	public ParagraphOverlayGraphicFactory(StyleClassedTextArea textArea) {
		this.textArea = textArea;
	}

	public void addOverlayFactory(OverlayFactory overlayFactory) {
		overlayFactories.add(overlayFactory);
		update();
	}

	public void removeOverlayFactory(OverlayFactory overlayFactory) {
		overlayFactories.remove(overlayFactory);
		update();
	}

	void addGutterFactory(IntFunction<Node> gutterFactory) {
		gutterFactories.add(gutterFactory);
		update();
	}

	void removeGutterFactory(IntFunction<Node> gutterFactory) {
		gutterFactories.remove(gutterFactory);
		update();
	}

	public void update() {
		// temporary remove paragraph graphic factory to update the view
		IntFunction<? extends Node> factory = textArea.getParagraphGraphicFactory();
		textArea.setParagraphGraphicFactory(null);
		textArea.setParagraphGraphicFactory(factory);
	}

	@Override
	public Node apply(int paragraphIndex) {
		return overlayFactories.isEmpty() && gutterFactories.isEmpty()
				? null
				: new ParagraphGraphic(paragraphIndex);
	}

	//---- class ParagraphGraphic ---------------------------------------------

	private class ParagraphGraphic
		extends Pane
	{
		private final int paragraphIndex;
		private final Node gutter;
		private final IdentityHashMap<OverlayFactory, List<Node>> overlayNodesMap
			= new IdentityHashMap<>(overlayFactories.size());
		private Node paragraphTextNode;

		ParagraphGraphic(int paragraphIndex) {
			this.paragraphIndex = paragraphIndex;

			getStyleClass().add("paragraph-graphic");

			if (gutterFactories.size() > 0) {
				if (gutterFactories.size() > 1) {
					HBox gutterBox = new HBox();
					for (IntFunction<Node> gutterFactory : gutterFactories)
						gutterBox.getChildren().add(gutterFactory.apply(paragraphIndex));
					gutter = gutterBox;
				} else
					gutter = gutterFactories.get(0).apply(paragraphIndex);
				gutter.getStyleClass().add("gutter");
				getChildren().add(gutter);
			} else
				gutter = null;

			parentProperty().addListener((observable, oldParent, newParent) -> {
				// this node also "need layout" if parent "needs layout"
				if (newParent != null) {
					newParent.needsLayoutProperty().addListener((ob, o, n) -> {
						if (n)
							setNeedsLayout(true);
					});
				}
			});
		}

		@Override
		protected double computePrefWidth(double height) {
			return (gutter != null) ? gutter.prefWidth(height) : 0;
		}

		@Override
		protected double computePrefHeight(double width) {
			return (gutter != null) ? gutter.prefHeight(width) : 0;
		}

		@Override
		protected void layoutChildren() {
			// layout gutter
			if (gutter != null) {
				double gutterWidth = gutter.prefWidth(-1);
				layoutInArea(gutter, 0, 0, gutterWidth, getHeight(), -1, null, true, true, HPos.LEFT, VPos.TOP);
			}

			// create overlay nodes
			if (overlayNodesMap.isEmpty() && !overlayFactories.isEmpty())
				createOverlayNodes();

			// layout overlay nodes
			layoutOverlayNodes();
		}

		private void createOverlayNodes() {

			paragraphTextNode = getParent().lookup(".paragraph-text");

			for (OverlayFactory overlayFactory : overlayFactories) {
				overlayFactory.init(textArea, paragraphTextNode, gutter);
				List<Node> nodes = overlayFactory.createOverlayNodes(paragraphIndex);
				overlayNodesMap.put(overlayFactory, nodes);

				for (Node node : nodes)
					node.setManaged(false);

				getChildren().addAll(nodes);
			}
		}

		private void layoutOverlayNodes() {
			for (OverlayFactory overlayFactory : overlayFactories) {
				overlayFactory.init(textArea, paragraphTextNode, gutter);
				overlayFactory.layoutOverlayNodes(paragraphIndex, overlayNodesMap.get(overlayFactory));
			}
		}
	}

	//---- class OverlayFactory -----------------------------------------------

	public static abstract class OverlayFactory
	{
		private StyleClassedTextArea textArea;
		private Node paragraphTextNode;
		private Node gutter;
		private double gutterWidth;

		private void init(StyleClassedTextArea textArea, Node paragraphTextNode, Node gutter) {
			this.textArea = textArea;
			this.paragraphTextNode = paragraphTextNode;
			this.gutter = gutter;
			this.gutterWidth = -1;
		}

		public abstract List<Node> createOverlayNodes(int paragraphIndex);
		public abstract void layoutOverlayNodes(int paragraphIndex, List<Node> nodes);

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

		protected Insets getInsets() {
			Insets insets = ((Region)paragraphTextNode).getInsets();
			if (gutter != null) {
				if (gutterWidth < 0)
					gutterWidth = gutter.prefWidth(-1);
				insets = new Insets(insets.getTop(), insets.getRight(), insets.getBottom(), gutterWidth + insets.getLeft());
			}
			return insets;
		}
	}

	//---- reflection utilities -----------------------------------------------

	private static Method mGetChildren;
	private static Method mGetRangeShape;

	static {
		try {
			mGetChildren = Parent.class.getDeclaredMethod("getChildren");
			mGetChildren.setAccessible(true);

			Class<?> textFlowExtClass = Class.forName("org.fxmisc.richtext.TextFlowExt");
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
