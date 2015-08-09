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
 * Encapsulates a spell checker match (e.g. RuleMatch) and
 * updates its fromPos and toPos immediately on text changes by the user,
 * which keeps existing spell match highlights in place while the user types.
 * Spell checking is done deferred and in a background thread.
 *
 * @author Karl Tauber
 */
abstract class SpellMatch
{
	private int fromPos;
	private int toPos;
	private boolean valid = true;

	SpellMatch(int fromPos, int toPos) {
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

	boolean isError() {
		return true;
	}

	abstract String getMessage();

	void updateOffsets(int position, int inserted, int removed) {
		//TODO
	}

	@Override
	public String toString() {
		return (valid ? "" : "INVALID ") + fromPos + "-" + toPos + ": " + getMessage();
	}
}
