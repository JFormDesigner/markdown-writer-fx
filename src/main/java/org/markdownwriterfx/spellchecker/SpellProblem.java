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

import java.util.List;
import org.languagetool.rules.ITSIssueType;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.spelling.SpellingCheckRule;

/**
 * Encapsulates a spell checker match (RuleMatch) and
 * updates its fromPos and toPos immediately on text changes by the user,
 * which keeps existing spell range highlights in place while the user types.
 * Spell checking is done deferred and in a background thread.
 *
 * @author Karl Tauber
 */
class SpellProblem
	extends SpellRange
{
	private final RuleMatch ruleMatch;

	SpellProblem(int offset, RuleMatch ruleMatch) {
		super(offset + ruleMatch.getFromPos(), offset + ruleMatch.getToPos());
		this.ruleMatch = ruleMatch;
	}

	RuleMatch getRuleMatch() {
		return ruleMatch;
	}

	Rule getRule() {
		return ruleMatch.getRule();
	}

	boolean isError() {
		return ruleMatch.getRule().getLocQualityIssueType() == ITSIssueType.Misspelling;
	}

	boolean isTypo() {
		return ruleMatch.getRule() instanceof SpellingCheckRule;
	}

	String getMessage() {
		return ruleMatch.getMessage();
	}

	List<String> getSuggestedReplacements() {
		return ruleMatch.getSuggestedReplacements();
	}
}
