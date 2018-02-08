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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.languagetool.AnalyzedSentence;
import org.languagetool.InputSentence;
import org.languagetool.ResultCache;
import org.languagetool.SimpleInputSentence;
import org.languagetool.rules.RuleMatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * A ResultCache that supports invalidation (empty cache).
 *
 * @author Karl Tauber
 */
class ResultCacheEx
	extends ResultCache
{
	private final Cache<InputSentence, List<RuleMatch>> matchesCache;
	private final Cache<SimpleInputSentence, AnalyzedSentence> sentenceCache;

	private static Field inputSentenceTextField;
	private static Field simpleInputSentenceTextField;

	static {
		try {
			inputSentenceTextField = InputSentence.class.getDeclaredField("text");
			inputSentenceTextField.setAccessible(true);
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
		try {
			simpleInputSentenceTextField = SimpleInputSentence.class.getDeclaredField("text");
			simpleInputSentenceTextField.setAccessible(true);
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
	}

	ResultCacheEx(long maxSize, int expireAfter, TimeUnit timeUnit) {
		super(1);

		matchesCache = CacheBuilder.newBuilder()
			.maximumSize(maxSize / 2)
			.recordStats()
			.expireAfterAccess(expireAfter, timeUnit)
			.build();
		sentenceCache = CacheBuilder.newBuilder()
			.maximumSize(maxSize / 2)
			.recordStats()
			.expireAfterAccess(expireAfter, timeUnit)
			.build();
	}

	void invalidateAll() {
		matchesCache.invalidateAll();
		sentenceCache.invalidateAll();
	}

	void invalidate(String word) {
		invalidate(matchesCache, inputSentenceTextField, word);
		invalidate(sentenceCache, simpleInputSentenceTextField, word);
	}

	private static <T> void invalidate(Cache<T, ?> cache, Field textField, String word) {
		List<T> matchesKeys = findWordInKeys(cache, textField, word);
		if (matchesKeys != null)
			cache.invalidateAll(matchesKeys);
		else
			cache.invalidateAll();
	}

	private static <T> List<T> findWordInKeys(Cache<T, ?> cache, Field textField, String word) {
		if (textField == null)
			return null;

		List<T> result = new ArrayList<>();
		for (T key : cache.asMap().keySet()) {
			try {
				String text = (String) textField.get(key);
				if (text.contains(word))
					result.add(key);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
				return null;
			}
		}
		return result;
	}

	@Override
	public List<RuleMatch> getIfPresent(InputSentence key) {
		return matchesCache.getIfPresent(key);
	}

	@Override
	public AnalyzedSentence getIfPresent(SimpleInputSentence key) {
		return sentenceCache.getIfPresent(key);
	}

	@Override
	public void put(InputSentence key, List<RuleMatch> sentenceMatches) {
		matchesCache.put(key, sentenceMatches);
	}

	@Override
	public void put(SimpleInputSentence key, AnalyzedSentence aSentence) {
		sentenceCache.put(key, aSentence);
	}
}
