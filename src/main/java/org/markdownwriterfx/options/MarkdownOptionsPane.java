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

package org.markdownwriterfx.options;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Label;
import org.markdownwriterfx.controls.FlagCheckBox;
import org.markdownwriterfx.controls.WebHyperlink;
import org.pegdown.Extensions;
import org.tbee.javafx.scene.layout.fxml.MigPane;

/**
 * Markdown options pane
 *
 * @author Karl Tauber
 */
class MarkdownOptionsPane
	extends MigPane
{
	private final IntegerProperty extensions = new SimpleIntegerProperty();

	MarkdownOptionsPane() {
		initComponents();

		smartsExtCheckBox.setFlag(Extensions.SMARTS);
		quotesExtCheckBox.setFlag(Extensions.QUOTES);
		abbreviationsExtCheckBox.setFlag(Extensions.ABBREVIATIONS);
		hardwrapsExtCheckBox.setFlag(Extensions.HARDWRAPS);
		autolinksExtCheckBox.setFlag(Extensions.AUTOLINKS);
		tablesExtCheckBox.setFlag(Extensions.TABLES);
		definitionListsExtCheckBox.setFlag(Extensions.DEFINITIONS);
		fencedCodeBlocksExtCheckBox.setFlag(Extensions.FENCED_CODE_BLOCKS);
		wikilinksExtCheckBox.setFlag(Extensions.WIKILINKS);
		strikethroughExtCheckBox.setFlag(Extensions.STRIKETHROUGH);
		anchorlinksExtCheckBox.setFlag(Extensions.ANCHORLINKS);
		suppressHtmlBlocksExtCheckBox.setFlag(Extensions.SUPPRESS_HTML_BLOCKS);
		suppressInlineHtmlExtCheckBox.setFlag(Extensions.SUPPRESS_INLINE_HTML);
		atxHeaderSpaceExtCheckBox.setFlag(Extensions.ATXHEADERSPACE);
		forceListItemParaExtCheckBox.setFlag(Extensions.FORCELISTITEMPARA);
		relaxedHrRulesExtCheckBox.setFlag(Extensions.RELAXEDHRULES);
		taskListItemsExtCheckBox.setFlag(Extensions.TASKLISTITEMS);
		extAnchorLinksExtCheckBox.setFlag(Extensions.EXTANCHORLINKS);

		extensions.bindBidirectional(smartsExtCheckBox.flagsProperty());
		extensions.bindBidirectional(quotesExtCheckBox.flagsProperty());
		extensions.bindBidirectional(abbreviationsExtCheckBox.flagsProperty());
		extensions.bindBidirectional(hardwrapsExtCheckBox.flagsProperty());
		extensions.bindBidirectional(autolinksExtCheckBox.flagsProperty());
		extensions.bindBidirectional(tablesExtCheckBox.flagsProperty());
		extensions.bindBidirectional(definitionListsExtCheckBox.flagsProperty());
		extensions.bindBidirectional(fencedCodeBlocksExtCheckBox.flagsProperty());
		extensions.bindBidirectional(wikilinksExtCheckBox.flagsProperty());
		extensions.bindBidirectional(strikethroughExtCheckBox.flagsProperty());
		extensions.bindBidirectional(anchorlinksExtCheckBox.flagsProperty());
		extensions.bindBidirectional(suppressHtmlBlocksExtCheckBox.flagsProperty());
		extensions.bindBidirectional(suppressInlineHtmlExtCheckBox.flagsProperty());
		extensions.bindBidirectional(atxHeaderSpaceExtCheckBox.flagsProperty());
		extensions.bindBidirectional(forceListItemParaExtCheckBox.flagsProperty());
		extensions.bindBidirectional(relaxedHrRulesExtCheckBox.flagsProperty());
		extensions.bindBidirectional(taskListItemsExtCheckBox.flagsProperty());
		extensions.bindBidirectional(extAnchorLinksExtCheckBox.flagsProperty());
	}

	void load() {
		extensions.set(Options.getMarkdownExtensions());
	}

	void save() {
		Options.setMarkdownExtensions(extensions.get());
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		smartsExtCheckBox = new FlagCheckBox();
		quotesExtCheckBox = new FlagCheckBox();
		abbreviationsExtCheckBox = new FlagCheckBox();
		WebHyperlink abbreviationsExtLink = new WebHyperlink();
		hardwrapsExtCheckBox = new FlagCheckBox();
		WebHyperlink hardwrapsExtLink = new WebHyperlink();
		autolinksExtCheckBox = new FlagCheckBox();
		WebHyperlink autolinksExtLink = new WebHyperlink();
		tablesExtCheckBox = new FlagCheckBox();
		WebHyperlink tablesExtLink = new WebHyperlink();
		Label tablesExtLabel = new Label();
		WebHyperlink tablesExtLink2 = new WebHyperlink();
		Label tablesExtLabel2 = new Label();
		definitionListsExtCheckBox = new FlagCheckBox();
		WebHyperlink definitionListsExtLink = new WebHyperlink();
		fencedCodeBlocksExtCheckBox = new FlagCheckBox();
		WebHyperlink fencedCodeBlocksExtLink = new WebHyperlink();
		Label fencedCodeBlocksExtLabel = new Label();
		WebHyperlink fencedCodeBlocksExtLink2 = new WebHyperlink();
		wikilinksExtCheckBox = new FlagCheckBox();
		strikethroughExtCheckBox = new FlagCheckBox();
		anchorlinksExtCheckBox = new FlagCheckBox();
		suppressHtmlBlocksExtCheckBox = new FlagCheckBox();
		suppressInlineHtmlExtCheckBox = new FlagCheckBox();
		atxHeaderSpaceExtCheckBox = new FlagCheckBox();
		forceListItemParaExtCheckBox = new FlagCheckBox();
		relaxedHrRulesExtCheckBox = new FlagCheckBox();
		taskListItemsExtCheckBox = new FlagCheckBox();
		extAnchorLinksExtCheckBox = new FlagCheckBox();

		//======== this ========
		setCols("[][fill]");
		setRows("[][][][][][][][][][][][][][][][][][]");

		//---- smartsExtCheckBox ----
		smartsExtCheckBox.setText("Beautify apostrophes, _ellipses (\"...\" and \". . .\") and dashes (\"--\" and \"---\")");
		add(smartsExtCheckBox, "cell 0 0");

		//---- quotesExtCheckBox ----
		quotesExtCheckBox.setText("Beautify single _quotes, double quotes and double angle quotes (\u00ab and \u00bb)");
		add(quotesExtCheckBox, "cell 0 1");

		//---- abbreviationsExtCheckBox ----
		abbreviationsExtCheckBox.setText("A_bbreviations in the way of");
		add(abbreviationsExtCheckBox, "cell 0 2");

		//---- abbreviationsExtLink ----
		abbreviationsExtLink.setText("Markdown Extra");
		abbreviationsExtLink.setUri("http://michelf.com/projects/php-markdown/extra/#abbr");
		add(abbreviationsExtLink, "cell 0 2,gapx 0");

		//---- hardwrapsExtCheckBox ----
		hardwrapsExtCheckBox.setText("_Newlines in paragraph-like content as real line breaks, see");
		add(hardwrapsExtCheckBox, "cell 0 3");

		//---- hardwrapsExtLink ----
		hardwrapsExtLink.setText("Github-flavoured-Markdown");
		hardwrapsExtLink.setUri("https://help.github.com/articles/writing-on-github/#markup");
		add(hardwrapsExtLink, "cell 0 3,gapx 0");

		//---- autolinksExtCheckBox ----
		autolinksExtCheckBox.setText("_Plain (undelimited) autolinks in the way of");
		add(autolinksExtCheckBox, "cell 0 4");

		//---- autolinksExtLink ----
		autolinksExtLink.setText("Github-flavoured-Markdown");
		autolinksExtLink.setUri("https://help.github.com/articles/github-flavored-markdown/#url-autolinking");
		add(autolinksExtLink, "cell 0 4,gapx 0");

		//---- tablesExtCheckBox ----
		tablesExtCheckBox.setText("_Tables similar to");
		add(tablesExtCheckBox, "cell 0 5");

		//---- tablesExtLink ----
		tablesExtLink.setText("MultiMarkdown");
		tablesExtLink.setUri("http://fletcher.github.io/MultiMarkdown-4/syntax.html#tables");
		add(tablesExtLink, "cell 0 5,gapx 0");

		//---- tablesExtLabel ----
		tablesExtLabel.setText("(like");
		add(tablesExtLabel, "cell 0 5,gapx 3");

		//---- tablesExtLink2 ----
		tablesExtLink2.setText("Markdown Extra");
		tablesExtLink2.setUri("https://michelf.ca/projects/php-markdown/extra/#table");
		add(tablesExtLink2, "cell 0 5,gapx 3 3");

		//---- tablesExtLabel2 ----
		tablesExtLabel2.setText(" tables, but with colspan support)");
		add(tablesExtLabel2, "cell 0 5,gapx 0");

		//---- definitionListsExtCheckBox ----
		definitionListsExtCheckBox.setText("_Definition lists in the way of");
		add(definitionListsExtCheckBox, "cell 0 6");

		//---- definitionListsExtLink ----
		definitionListsExtLink.setText("Markdown Extra");
		definitionListsExtLink.setUri("https://michelf.ca/projects/php-markdown/extra/#def-list");
		add(definitionListsExtLink, "cell 0 6,gapx 0");

		//---- fencedCodeBlocksExtCheckBox ----
		fencedCodeBlocksExtCheckBox.setText("_Fenced Code Blocks in the way of");
		add(fencedCodeBlocksExtCheckBox, "cell 0 7");

		//---- fencedCodeBlocksExtLink ----
		fencedCodeBlocksExtLink.setText("Markdown Extra");
		fencedCodeBlocksExtLink.setUri("http://michelf.com/projects/php-markdown/extra/#fenced-code-blocks");
		add(fencedCodeBlocksExtLink, "cell 0 7,gapx 0");

		//---- fencedCodeBlocksExtLabel ----
		fencedCodeBlocksExtLabel.setText("or");
		add(fencedCodeBlocksExtLabel, "cell 0 7,gapx 3");

		//---- fencedCodeBlocksExtLink2 ----
		fencedCodeBlocksExtLink2.setText("Github-flavoured-Markdown");
		fencedCodeBlocksExtLink2.setUri("https://help.github.com/articles/github-flavored-markdown/#fenced-code-blocks");
		add(fencedCodeBlocksExtLink2, "cell 0 7,gapx 3");

		//---- wikilinksExtCheckBox ----
		wikilinksExtCheckBox.setText("_Wiki-style links (\"[[wiki link]]\")");
		add(wikilinksExtCheckBox, "cell 0 8");

		//---- strikethroughExtCheckBox ----
		strikethroughExtCheckBox.setText("_Strikethrough");
		add(strikethroughExtCheckBox, "cell 0 9");

		//---- anchorlinksExtCheckBox ----
		anchorlinksExtCheckBox.setText("_Anchor links in headers");
		add(anchorlinksExtCheckBox, "cell 0 10");

		//---- suppressHtmlBlocksExtCheckBox ----
		suppressHtmlBlocksExtCheckBox.setText("Suppress the _output of HTML blocks");
		add(suppressHtmlBlocksExtCheckBox, "cell 0 11");

		//---- suppressInlineHtmlExtCheckBox ----
		suppressInlineHtmlExtCheckBox.setText("Suppress the o_utput of inline HTML elements");
		add(suppressInlineHtmlExtCheckBox, "cell 0 12");

		//---- atxHeaderSpaceExtCheckBox ----
		atxHeaderSpaceExtCheckBox.setText("Requires a space char after Atx # header prefixes, so that #dasdsdaf is not a header");
		add(atxHeaderSpaceExtCheckBox, "cell 0 13");

		//---- forceListItemParaExtCheckBox ----
		forceListItemParaExtCheckBox.setText("Force List and Definition Paragraph wrapping if it includes more than just a single paragraph");
		add(forceListItemParaExtCheckBox, "cell 0 14");

		//---- relaxedHrRulesExtCheckBox ----
		relaxedHrRulesExtCheckBox.setText("Allow horizontal rules without a blank line following them");
		add(relaxedHrRulesExtCheckBox, "cell 0 15");

		//---- taskListItemsExtCheckBox ----
		taskListItemsExtCheckBox.setText("GitHub style task list items");
		add(taskListItemsExtCheckBox, "cell 0 16");

		//---- extAnchorLinksExtCheckBox ----
		extAnchorLinksExtCheckBox.setText("Generate anchor links for headers using complete contents of the header");
		add(extAnchorLinksExtCheckBox, "cell 0 17");
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private FlagCheckBox smartsExtCheckBox;
	private FlagCheckBox quotesExtCheckBox;
	private FlagCheckBox abbreviationsExtCheckBox;
	private FlagCheckBox hardwrapsExtCheckBox;
	private FlagCheckBox autolinksExtCheckBox;
	private FlagCheckBox tablesExtCheckBox;
	private FlagCheckBox definitionListsExtCheckBox;
	private FlagCheckBox fencedCodeBlocksExtCheckBox;
	private FlagCheckBox wikilinksExtCheckBox;
	private FlagCheckBox strikethroughExtCheckBox;
	private FlagCheckBox anchorlinksExtCheckBox;
	private FlagCheckBox suppressHtmlBlocksExtCheckBox;
	private FlagCheckBox suppressInlineHtmlExtCheckBox;
	private FlagCheckBox atxHeaderSpaceExtCheckBox;
	private FlagCheckBox forceListItemParaExtCheckBox;
	private FlagCheckBox relaxedHrRulesExtCheckBox;
	private FlagCheckBox taskListItemsExtCheckBox;
	private FlagCheckBox extAnchorLinksExtCheckBox;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
