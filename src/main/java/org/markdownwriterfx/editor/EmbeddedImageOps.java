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

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.fxmisc.richtext.model.SegmentOps;

/**
 * @author Karl Tauber
 */
class EmbeddedImageOps
	implements SegmentOps<EmbeddedImage, Collection<String>>
{
	private final EmbeddedImage emptySeg = new EmbeddedImage(null, null, "", null);

	@Override
	public int length(EmbeddedImage seg) {
		return seg.text.length();
	}

	@Override
	public char charAt(EmbeddedImage seg, int index) {
		return (seg == emptySeg) ? '\0' : seg.text.charAt(index);
	}

	@Override
	public String getText(EmbeddedImage seg) {
		return seg.text;
	}

	@Override
	public EmbeddedImage subSequence(EmbeddedImage seg, int start, int end) {
		return (seg == emptySeg) ? emptySeg : new EmbeddedImage(seg.basePath, seg.node, seg.text.substring(start, end), seg.style);
	}

	@Override
	public EmbeddedImage subSequence(EmbeddedImage seg, int start) {
		return (seg == emptySeg) ? emptySeg : new EmbeddedImage(seg.basePath, seg.node, seg.text.substring(start), seg.style);
	}

	@Override
	public Collection<String> getStyle(EmbeddedImage seg) {
		return (seg.style != null) ? seg.style : Collections.emptyList();
	}

	@Override
	public EmbeddedImage setStyle(EmbeddedImage seg, Collection<String> style) {
		return (seg == emptySeg) ? emptySeg : new EmbeddedImage(seg.basePath, seg.node, seg.text, style);
	}

	@Override
	public Optional<EmbeddedImage> join(EmbeddedImage currentSeg, EmbeddedImage nextSeg) {
		return Optional.empty();
	}

	@Override
	public EmbeddedImage createEmpty() {
		return emptySeg;
	}
}
