/*
 * Copyright (c) 2017 Karl Tauber <karl at jformdesigner dot com>
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

import static org.junit.Assert.*;
import java.util.List;

import org.junit.Test;
import com.vladsch.flexmark.ast.Document;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.Pair;

/**
 * @author Karl Tauber
 */
public class TestSmartFormat
{
	@Test
	public void simple() {
		testFormat(10,
			"123 567 901 345 789 123 567 90",

			"123 567\n" +
			"901 345\n" +
			"789 123\n" +
			"567 90");

		testFormat(10,
			"1234567890 2345678901 34567890",

			"1234567890\n" +
			"2345678901\n" +
			"34567890");
	}

	@Test
	public void bulletLists() {
		testFormat(10,
			"- 123 567 901 345 789 123 567 90",

			"- 123 567\n" +
			"  901 345\n" +
			"  789 123\n" +
			"  567 90");

		testFormat(10,
			"  * 1234567890 2345678901 34567890",

			"  * 1234567890\n" +
			"    2345678901\n" +
			"    34567890");
	}

	@Test
	public void bulletListsNested() {
		testFormat(10,
			"- 123 567 901 345 789 123 567 90\n" +
			"  + abc de fgeh ijk lm",

			"- 123 567\n" +
			"  901 345\n" +
			"  789 123\n" +
			"  567 90\n" +
			"  + abc de\n" +
			"    fgeh\n" +
			"    ijk lm");
	}

	private void testFormat(int wrapLength, String input, String expected) {
		// parse markdown
		Document document = Parser.builder().build().parse(input);

		// format
		List<Pair<Paragraph, String>> formattedParagraphs = new SmartFormat(null, null)
			.formatParagraphs(document, wrapLength);

		// build result
		StringBuilder actual = new StringBuilder(input);
		for (int i = formattedParagraphs.size() - 1; i >= 0; i--) {
			Pair<Paragraph, String> pair = formattedParagraphs.get(i);
			Paragraph paragraph = pair.getFirst();
			String newText = pair.getSecond();

			int startOffset = paragraph.getStartOffset();
			int endOffset = paragraph.getEndOffset();
			if (paragraph.getChars().endsWith("\n"))
				endOffset--;

			actual.replace(startOffset, endOffset, newText);
		}

		// check
		assertEquals(expected, actual.toString());
	}
}
