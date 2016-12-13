package org.markdownwriterfx.syntaxhighlighter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter
{
	public interface HighlightConsumer {
		void accept(int length, String style);
	}

	public static boolean highlight(String text, String language, HighlightConsumer consumer) {
		switch (language) {
			case "html":
			case "xml":
			case "mathml":
			case "svg":
				highlightHTML(text, consumer);
				return true;
		}
		return false;
	}

	//---- HTML ---------------------------------------------------------------

	// from richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/XMLEditor.java

	private static final Pattern XML_TAG = Pattern.compile("(?<ELEMENT>(</?\\h*)(\\w+)([^<>]*)(\\h*/?>))"
			+"|(?<ENTITY>&#?[\\da-z]{1,8};)"
			+"|(?<COMMENT><!--[^<>]+-->)");

	private static final Pattern ATTRIBUTES = Pattern.compile("(\\w+\\h*)(=)(\\h*\"[^\"]+\")");

	private static final int GROUP_ELEMENT = 1;
	private static final int GROUP_OPEN_BRACKET = 2;
	private static final int GROUP_ELEMENT_NAME = 3;
	private static final int GROUP_ATTRIBUTES_SECTION = 4;
	private static final int GROUP_CLOSE_BRACKET = 5;
	private static final int GROUP_ENTITY = 6;
	private static final int GROUP_COMMENT = 7;

	private static final int GROUP_ATTRIBUTE_NAME = 1;
	private static final int GROUP_EQUAL_SYMBOL = 2;
	private static final int GROUP_ATTRIBUTE_VALUE = 3;

	private static void highlightHTML(String text, HighlightConsumer consumer) {
		Matcher matcher = XML_TAG.matcher(text);
		int lastKwEnd = 0;
		while(matcher.find()) {

			consumer.accept(matcher.start() - lastKwEnd, null);
			if(matcher.group(GROUP_ELEMENT) != null) {
				String attributesText = matcher.group(GROUP_ATTRIBUTES_SECTION);

				consumer.accept(matcher.end(GROUP_OPEN_BRACKET) - matcher.start(GROUP_OPEN_BRACKET), "punctuation");
				consumer.accept(matcher.end(GROUP_ELEMENT_NAME) - matcher.end(GROUP_OPEN_BRACKET), "tag");

				if(!attributesText.isEmpty()) {

					lastKwEnd = 0;

					Matcher amatcher = ATTRIBUTES.matcher(attributesText);
					while(amatcher.find()) {
						consumer.accept(amatcher.start() - lastKwEnd, null);
						consumer.accept(amatcher.end(GROUP_ATTRIBUTE_NAME) - amatcher.start(GROUP_ATTRIBUTE_NAME), "attr-name");
						consumer.accept(amatcher.end(GROUP_EQUAL_SYMBOL) - amatcher.end(GROUP_ATTRIBUTE_NAME), "punctuation");
						consumer.accept(amatcher.end(GROUP_ATTRIBUTE_VALUE) - amatcher.end(GROUP_EQUAL_SYMBOL), "attr-value");
						lastKwEnd = amatcher.end();
					}
					if(attributesText.length() > lastKwEnd)
						consumer.accept(attributesText.length() - lastKwEnd, null);
				}

				lastKwEnd = matcher.end(GROUP_ATTRIBUTES_SECTION);

				consumer.accept(matcher.end(GROUP_CLOSE_BRACKET) - lastKwEnd, "punctuation");
			} else if(matcher.group(GROUP_ENTITY) != null) {
				consumer.accept(matcher.end() - matcher.start(), "entity");
			} else if(matcher.group(GROUP_COMMENT) != null) {
				consumer.accept(matcher.end() - matcher.start(), "comment");
			}
			lastKwEnd = matcher.end();
		}
		consumer.accept(text.length() - lastKwEnd, null);
	}
}
