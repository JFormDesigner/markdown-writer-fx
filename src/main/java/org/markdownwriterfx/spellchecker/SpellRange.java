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

/**
 * Range that updates its fromPos and toPos immediately on text changes by the user,
 * which keeps existing spell range highlights in place while the user types.
 * Spell checking is done deferred and in a background thread.
 *
 * @author Karl Tauber
 */
class SpellRange
{
	private int fromPos;
	private int toPos;
	private boolean valid = true;

	SpellRange(int fromPos, int toPos) {
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	final int getFromPos() {
		return fromPos;
	}

	final int getToPos() {
		return toPos;
	}

	final boolean isValid() {
		return valid;
	}

	void updateOffsets(int position, int inserted, int removed) {
		if (position > toPos)
			return; // changed area is after this range

		int diff = inserted - removed;

		if (position + removed <= fromPos) {
			// changed area is before this range
			fromPos += diff;
			toPos += diff;
		} else if (position >= fromPos) {
			// changed area starts within this range
			if( position + removed <= toPos ) {
				// changed area is within this range
				toPos += diff;
			} else {
				// changed area starts within this range and ends after it
				// --> the new text does not belong to this range
				toPos = position;
			}
		} else { // position < fromPos
			// changed area starts before this range
			if( position + removed <= toPos ) {
				// changed area starts before this range and ends within it
				// --> the new text does not belong to this range
				fromPos = position + inserted;
				toPos += diff;
			} else {
				// changed area fully replaces the position
				// --> make position invalid
				valid = false;
			}
		}
	}

	@Override
	public String toString() {
		return (valid ? "" : "INVALID ") + fromPos + "-" + toPos;
	}
}
