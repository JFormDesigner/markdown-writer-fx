/*
 * Copyright (c) 2018 Karl Tauber <karl at jformdesigner dot com>
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
import java.util.concurrent.TimeUnit;
import org.languagetool.ResultCache;
import com.google.common.cache.Cache;

/**
 * A ResultCache that supports invalidation (empty cache).
 *
 * @author Karl Tauber
 */
class ResultCacheEx
	extends ResultCache
{
	ResultCacheEx(long maxSize, int expireAfter, TimeUnit timeUnit) {
		super(maxSize, expireAfter, timeUnit);
	}

	void invalidateAll() {
		getMatchesCache().invalidateAll();
		getSentenceCache().invalidateAll();
	}

	void invalidate(String word) {
		invalidate(getMatchesCache(), word);
		invalidate(getSentenceCache(), word);
	}

	private static <T> void invalidate(Cache<T, ?> cache, String word) {
		List<T> matchesKeys = findWordInKeys(cache, word);
		if (matchesKeys != null)
			cache.invalidateAll(matchesKeys);
		else
			cache.invalidateAll();
	}

	private static <T> List<T> findWordInKeys(Cache<T, ?> cache, String word) {
		List<T> result = new ArrayList<>();
		for (T key : cache.asMap().keySet()) {
			// assume that InputSentence.toString() and SimpleInputSentence.toString()
			// return the text of the sentence
			String text = key.toString();
			if (text.contains(word))
				result.add(key);
		}
		return result;
	}
}
