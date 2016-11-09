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

package org.markdownwriterfx.editor;

import static org.junit.Assert.*;
import static org.markdownwriterfx.editor.MarkdownSyntaxHighlighter.addStyledRange;

import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.markdownwriterfx.editor.MarkdownSyntaxHighlighter.StyleClass;
import org.markdownwriterfx.editor.MarkdownSyntaxHighlighter.StyleRange;

/**
 * @author Karl Tauber
 */
public class TestStyleRanges
{
	private ArrayList<StyleRange> styleRanges;

	@Before
	public void initialize() {
		styleRanges = new ArrayList<>();
	}

	@Test
	public void single() {
		// 012345678901234567890123456789
		// 11111
		addStyledRange(styleRanges, 0, 5, StyleClass.h1);
		assertStyleRanges(new R(0, 5, StyleClass.h1));
	}

	@Test
	public void single2() {
		// 012345678901234567890123456789
		//           1111111111
		addStyledRange(styleRanges, 10, 15, StyleClass.h1);
		assertStyleRanges(new R(10, 15, StyleClass.h1));
	}

	@Test
	public void two() {
		// 012345678901234567890123456789
		// 11111
		//           22222
		addStyledRange(styleRanges, 0, 5, StyleClass.h1);
		addStyledRange(styleRanges, 10, 15, StyleClass.h2);
		assertStyleRanges(
				new R(0, 5, StyleClass.h1),
				new R(10, 15, StyleClass.h2));
	}

	@Test
	public void three() {
		// 012345678901234567890123456789
		// 11111
		//           22222
		//                3333333333
		addStyledRange(styleRanges, 0, 5, StyleClass.h1);
		addStyledRange(styleRanges, 10, 15, StyleClass.h2);
		addStyledRange(styleRanges, 15, 25, StyleClass.h3);
		assertStyleRanges(
				new R(0, 5, StyleClass.h1),
				new R(10, 15, StyleClass.h2),
				new R(15, 25, StyleClass.h3));
	}

	@Test
	public void overlapAtEnd() {
		// 012345678901234567890123456789
		//           1111111111
		//                2222222222
		addStyledRange(styleRanges, 10, 20, StyleClass.h1);
		addStyledRange(styleRanges, 15, 25, StyleClass.h2);
		assertStyleRanges(
				new R(10, 15, StyleClass.h1),
				new R(15, 20, StyleClass.h1, StyleClass.h2),
				new R(20, 25, StyleClass.h2));
	}

	@Test
	public void overlapAtBegin() {
		// 012345678901234567890123456789
		//           1111111111
		//      2222222222
		addStyledRange(styleRanges, 10, 20, StyleClass.h1);
		addStyledRange(styleRanges, 5, 15, StyleClass.h2);
		assertStyleRanges(
				new R(5, 10, StyleClass.h2),
				new R(10, 15, StyleClass.h1, StyleClass.h2),
				new R(15, 20, StyleClass.h1));
	}

	@Test
	public void overlapAtBeginAndEnd() {
		// 012345678901234567890123456789
		//           1111111111
		//      22222222222222222222
		addStyledRange(styleRanges, 10, 20, StyleClass.h1);
		addStyledRange(styleRanges, 5, 25, StyleClass.h2);
		assertStyleRanges(
				new R(5, 10, StyleClass.h2),
				new R(10, 20, StyleClass.h1, StyleClass.h2),
				new R(20, 25, StyleClass.h2));
	}

	@Test
	public void overlapWithin() {
		// 012345678901234567890123456789
		//           1111111111
		//              22222
		addStyledRange(styleRanges, 10, 20, StyleClass.h1);
		addStyledRange(styleRanges, 13, 18, StyleClass.h2);
		assertStyleRanges(
				new R(10, 13, StyleClass.h1),
				new R(13, 18, StyleClass.h1, StyleClass.h2),
				new R(18, 20, StyleClass.h1));
	}

	@Test
	public void overlapWithinBegin() {
		// 012345678901234567890123456789
		//           1111111111
		//           22222222
		addStyledRange(styleRanges, 10, 20, StyleClass.h1);
		addStyledRange(styleRanges, 10, 18, StyleClass.h2);
		assertStyleRanges(
				new R(10, 18, StyleClass.h1, StyleClass.h2),
				new R(18, 20, StyleClass.h1));
	}

	@Test
	public void overlapWithinEnd() {
		// 012345678901234567890123456789
		//           1111111111
		//              2222222
		addStyledRange(styleRanges, 10, 20, StyleClass.h1);
		addStyledRange(styleRanges, 13, 20, StyleClass.h2);
		assertStyleRanges(
				new R(10, 13, StyleClass.h1),
				new R(13, 20, StyleClass.h1, StyleClass.h2));
	}

	@Test
	public void overlapWithinWhole() {
		// 012345678901234567890123456789
		//           1111111111
		//           2222222222
		addStyledRange(styleRanges, 10, 20, StyleClass.h1);
		addStyledRange(styleRanges, 10, 20, StyleClass.h2);
		assertStyleRanges(
				new R(10, 20, StyleClass.h1, StyleClass.h2));
	}

	@Test
	public void three1() {
		// 012345678901234567890123456789
		//      11111
		//           222222
		//         33333
		addStyledRange(styleRanges, 5, 10, StyleClass.h1);
		addStyledRange(styleRanges, 10, 15, StyleClass.h2);
		addStyledRange(styleRanges, 8, 13, StyleClass.h3);
		assertStyleRanges(
				new R(5, 8, StyleClass.h1),
				new R(8, 10, StyleClass.h1, StyleClass.h3),
				new R(10, 13, StyleClass.h2, StyleClass.h3),
				new R(13, 15, StyleClass.h2));
	}

	@Test
	public void three2() {
		// 012345678901234567890123456789
		//      11111
		//                22222
		//             33333
		addStyledRange(styleRanges, 5, 10, StyleClass.h1);
		addStyledRange(styleRanges, 15, 20, StyleClass.h2);
		addStyledRange(styleRanges, 12, 17, StyleClass.h3);
		assertStyleRanges(
				new R(5, 10, StyleClass.h1),
				new R(12, 15, StyleClass.h3),
				new R(15, 17, StyleClass.h2, StyleClass.h3),
				new R(17, 20, StyleClass.h2));
	}

	@Test
	public void three3() {
		// 012345678901234567890123456789
		//      11111
		//                22222
		//         3333333333
		addStyledRange(styleRanges, 5, 10, StyleClass.h1);
		addStyledRange(styleRanges, 15, 20, StyleClass.h2);
		addStyledRange(styleRanges, 8, 18, StyleClass.h3);
		assertStyleRanges(
				new R(5, 8, StyleClass.h1),
				new R(8, 10, StyleClass.h1, StyleClass.h3),
				new R(10, 15, StyleClass.h3),
				new R(15, 18, StyleClass.h2, StyleClass.h3),
				new R(18, 20, StyleClass.h2));
	}

	@Test
	public void three4() {
		// 012345678901234567890123456789
		//      11111
		//                22222
		//   333333333333333
		addStyledRange(styleRanges, 5, 10, StyleClass.h1);
		addStyledRange(styleRanges, 15, 20, StyleClass.h2);
		addStyledRange(styleRanges, 2, 17, StyleClass.h3);
		assertStyleRanges(
				new R(2, 5, StyleClass.h3),
				new R(5, 10, StyleClass.h1, StyleClass.h3),
				new R(10, 15, StyleClass.h3),
				new R(15, 17, StyleClass.h2, StyleClass.h3),
				new R(17, 20, StyleClass.h2));
	}

	@Test
	public void three5() {
		// 012345678901234567890123456789
		//      11111
		//                22222
		//           333
		addStyledRange(styleRanges, 5, 10, StyleClass.h1);
		addStyledRange(styleRanges, 15, 20, StyleClass.h2);
		addStyledRange(styleRanges, 10, 13, StyleClass.h3);
		assertStyleRanges(
				new R(5, 10, StyleClass.h1),
				new R(10, 13, StyleClass.h3),
				new R(15, 20, StyleClass.h2));
	}

	@Test
	public void three6() {
		// 012345678901234567890123456789
		//      11111
		//                22222
		//           33333
		addStyledRange(styleRanges, 5, 10, StyleClass.h1);
		addStyledRange(styleRanges, 15, 20, StyleClass.h2);
		addStyledRange(styleRanges, 10, 15, StyleClass.h3);
		assertStyleRanges(
				new R(5, 10, StyleClass.h1),
				new R(10, 15, StyleClass.h3),
				new R(15, 20, StyleClass.h2));
	}

	@Test
	public void three7() {
		// 012345678901234567890123456789
		//      11111
		//                22222
		// 333
		addStyledRange(styleRanges, 5, 10, StyleClass.h1);
		addStyledRange(styleRanges, 15, 20, StyleClass.h2);
		addStyledRange(styleRanges, 0, 3, StyleClass.h3);
		assertStyleRanges(
				new R(0, 3, StyleClass.h3),
				new R(5, 10, StyleClass.h1),
				new R(15, 20, StyleClass.h2));
	}

	@Test
	public void three8() {
		// 012345678901234567890123456789
		//      11111
		//                22222
		// 33333
		addStyledRange(styleRanges, 5, 10, StyleClass.h1);
		addStyledRange(styleRanges, 15, 20, StyleClass.h2);
		addStyledRange(styleRanges, 0, 5, StyleClass.h3);
		assertStyleRanges(
				new R(0, 5, StyleClass.h3),
				new R(5, 10, StyleClass.h1),
				new R(15, 20, StyleClass.h2));
	}

	@Test
	public void three9() {
		// 012345678901234567890123456789
		//      11111
		//                22222
		// 3333333333
		addStyledRange(styleRanges, 5, 10, StyleClass.h1);
		addStyledRange(styleRanges, 15, 20, StyleClass.h2);
		addStyledRange(styleRanges, 0, 10, StyleClass.h3);
		assertStyleRanges(
				new R(0, 5, StyleClass.h3),
				new R(5, 10, StyleClass.h1, StyleClass.h3),
				new R(15, 20, StyleClass.h2));
	}

	@Test
	public void three10() {
		// 012345678901234567890123456789
		//      11111
		//                22222
		// 3333333333333
		addStyledRange(styleRanges, 5, 10, StyleClass.h1);
		addStyledRange(styleRanges, 15, 20, StyleClass.h2);
		addStyledRange(styleRanges, 0, 13, StyleClass.h3);
		assertStyleRanges(
				new R(0, 5, StyleClass.h3),
				new R(5, 10, StyleClass.h1, StyleClass.h3),
				new R(10, 13, StyleClass.h3),
				new R(15, 20, StyleClass.h2));
	}

	@Test
	public void three11() {
		// 012345678901234567890123456789
		//      11111
		//                22222
		// 333333333333333
		addStyledRange(styleRanges, 5, 10, StyleClass.h1);
		addStyledRange(styleRanges, 15, 20, StyleClass.h2);
		addStyledRange(styleRanges, 0, 15, StyleClass.h3);
		assertStyleRanges(
				new R(0, 5, StyleClass.h3),
				new R(5, 10, StyleClass.h1, StyleClass.h3),
				new R(10, 15, StyleClass.h3),
				new R(15, 20, StyleClass.h2));
	}

	private void assertStyleRanges(R... expected) {
		try {
			assertEquals(expected.length, styleRanges.size());
			for (int i = 0; i < expected.length; i++) {
				StyleRange actual = styleRanges.get(i);
				assertEquals("begin", expected[i].begin, actual.begin);
				assertEquals("end", expected[i].end, actual.end);
				assertEquals("styleBits", expected[i].styleBits, actual.styleBits);
			}
		} catch (AssertionError ex) {
			System.err.println("---- actual ----");
			for (int i = 0; i < styleRanges.size(); i++) {
				StyleRange actual = styleRanges.get(i);
				System.err.printf("%d: %2d-%2d   0x%x\n", i, actual.begin, actual.end, actual.styleBits);
			}
			throw ex;
		}
	}

	private static class R
	{
		final int begin;		// inclusive
		final int end;			// inclusive
		final long styleBits;

		R(int begin, int end, StyleClass... styleClasses) {
			this.begin = begin;
			this.end = end;

			long styleBits = 0;
			for (StyleClass styleClass : styleClasses)
				styleBits |= 1 << styleClass.ordinal();
			this.styleBits = styleBits;
		}
	}
}
