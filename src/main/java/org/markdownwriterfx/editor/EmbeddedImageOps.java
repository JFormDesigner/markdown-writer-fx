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

import org.fxmisc.richtext.model.NodeSegmentOpsBase;

/**
 * @author Karl Tauber
 */
class EmbeddedImageOps<S>
	extends NodeSegmentOpsBase<EmbeddedImage, S>
{
	EmbeddedImageOps() {
		super(new EmbeddedImage(null, null, ""));
	}

	@Override
	public int length(EmbeddedImage seg) {
		return seg.text.length();
	}

	@Override
	public char realCharAt(EmbeddedImage seg, int index) {
		return seg.text.charAt(index);
	}

	@Override
	public String realGetText(EmbeddedImage seg) {
		return seg.text;
	}

	@Override
	public EmbeddedImage realSubSequence(EmbeddedImage seg, int start, int end) {
		return (start == 0 && end == seg.text.length())
			? seg
			: new EmbeddedImage(seg.basePath, seg.node, seg.text.substring(start, end));
	}

	@Override
	public EmbeddedImage realSubSequence(EmbeddedImage seg, int start) {
		return (start == 0)
			? seg
			: new EmbeddedImage(seg.basePath, seg.node, seg.text.substring(start));
	}
}
