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

package org.markdownwriterfx.spellchecker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.markdownwriterfx.editor.ParagraphOverlayGraphicFactory.OverlayFactory;
import org.markdownwriterfx.util.Range;

/**
 * Highlights spelling problems
 *
 * @author Karl Tauber
 */
class SpellCheckerOverlayFactory
	extends OverlayFactory
{
	private final Supplier<List<SpellBlockProblems>> spellProblemsSupplier;
	private final boolean wavyLines = true;

	SpellCheckerOverlayFactory(Supplier<List<SpellBlockProblems>> spellProblemsSupplier) {
		this.spellProblemsSupplier = spellProblemsSupplier;
	}

	@Override
	public List<Node> createOverlayNodes(int paragraphIndex) {
		List<SpellBlockProblems> spellProblems = this.spellProblemsSupplier.get();
		if (spellProblems == null || spellProblems.isEmpty())
			return Collections.emptyList();

		StyleClassedTextArea textArea = getTextArea();
		int parStart = textArea.position(paragraphIndex, 0).toOffset();
		int parLength = textArea.getParagraph(paragraphIndex).length() + 1;
		int parEnd = parStart + parLength;

		ArrayList<Node> nodes = new ArrayList<>();
		for (SpellBlockProblems blockProblems : spellProblems) {
			if (!blockProblems.isValid() || blockProblems.getFromPos() >= parEnd || blockProblems.getToPos() < parStart)
				continue; // not in this line

			for (SpellProblem problem : blockProblems.problems) {
				if (!problem.isValid() || problem.getFromPos() >= parEnd || problem.getToPos() < parStart)
					continue; // not in this line

				int start = Math.max(problem.getFromPos() - parStart, 0);
				int end = Math.min(problem.getToPos() - parStart, parLength);
				boolean spellError = problem.isError();

				Path path = new Path();
				if (wavyLines) {
					path.setStroke(spellError ? Color.RED : Color.ORANGE);
					path.setStrokeWidth(1);
					path.setOpacity(0.7);
				} else {
					path.setFill(spellError ? Color.RED : Color.ORANGE);
					path.setStrokeWidth(0);
					path.setOpacity(0.3);
				}
				path.setUserData(new Range(start, end));
				nodes.add(path);
			}
		}

		return nodes;
	}

	@Override
	public void layoutOverlayNodes(int paragraphIndex, List<Node> nodes) {
		Insets insets = getInsets();
		double leftInsets = insets.getLeft();
		double topInsets = insets.getTop();

		for (Node node : nodes) {
			node.setLayoutX(leftInsets);
			node.setLayoutY(topInsets);

			Range range = (Range) node.getUserData();

			if (wavyLines) {
				List<Rectangle2D> boundsList = getAllBounds(range.start, range.end);
				List<PathElement> wavyLinesShape = wavyLines(boundsList);
				((Path)node).getElements().setAll(wavyLinesShape);
			} else {
				PathElement[] shape = getShape(range.start, range.end);
				((Path)node).getElements().setAll(shape);
			}
		}
	}

	private List<PathElement> wavyLines(List<Rectangle2D> boundsList) {
		if (boundsList.size() == 1)
			return wavyLines(boundsList.get(0));

		ArrayList<PathElement> elements = new ArrayList<>();
		for (Rectangle2D bounds : boundsList)
			elements.addAll(wavyLines(bounds));
		return elements;
	}

	private List<PathElement> wavyLines(Rectangle2D bounds) {
		double x = bounds.getMinX();
		double y = bounds.getMaxY() - 2;
		double x2 = bounds.getMaxX();
		int inc = 2;

		ArrayList<PathElement> elements = new ArrayList<>();
		elements.add(new MoveTo(x, y));
		for (int i = 0; i < 10000; i++) {
			// bottom horizontal
			x += 1;
			if (x >= x2)
				break;
			elements.add(new LineTo(x, y));

			// bottom to top
			x += inc;
			y -= inc;
			if (x >= x2)
				break;
			elements.add(new LineTo(x, y));

			// top horizontal
			x += 1;
			if (x >= x2)
				break;
			elements.add(new LineTo(x, y));

			// top to bottom
			x += inc;
			y += inc;
			if (x >= x2)
				break;
			elements.add(new LineTo(x, y));
		}
		return elements;
	}
}
