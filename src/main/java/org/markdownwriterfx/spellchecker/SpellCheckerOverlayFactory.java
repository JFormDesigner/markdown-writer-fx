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
import java.util.List;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import org.languagetool.rules.ITSIssueType;
import org.languagetool.rules.RuleMatch;
import org.markdownwriterfx.editor.ParagraphOverlayGraphicFactory.OverlayFactory;

/**
 * Highlights spelling problems
 *
 * @author Karl Tauber
 */
class SpellCheckerOverlayFactory
	extends OverlayFactory
{
	private final Supplier<List<RuleMatch>> spellMatchesSupplier;

	SpellCheckerOverlayFactory(Supplier<List<RuleMatch>> spellMatchesSupplier) {
		this.spellMatchesSupplier = spellMatchesSupplier;
	}

	@Override
	public Node[] createOverlayNodes(int paragraphIndex) {
		List<RuleMatch> spellMatches = this.spellMatchesSupplier.get();
		if (spellMatches == null)
			return null;

		ArrayList<Node> nodes = new ArrayList<>();
		for (RuleMatch match : spellMatches) {
			if (match.getLine() != paragraphIndex)
				continue;

			int start = match.getColumn() - 1;
			int end = match.getEndColumn() - 1;
			boolean spellError = (match.getRule().getLocQualityIssueType() == ITSIssueType.Misspelling);

			PathElement[] shape = getShape(start, end);

			Path path = new Path(shape);
			path.setManaged(false);
			path.setFill(spellError ? Color.RED : Color.ORANGE);
			path.setStrokeWidth(0);
			path.setOpacity(0.3);
			nodes.add(path);
		}

		return nodes.toArray(new Node[nodes.size()]);
	}
}
