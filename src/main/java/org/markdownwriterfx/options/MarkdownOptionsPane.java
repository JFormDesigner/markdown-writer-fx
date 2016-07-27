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

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import org.markdownwriterfx.Messages;
import org.markdownwriterfx.controls.ExtensionCheckBox;
import org.markdownwriterfx.controls.FlagCheckBox;
import org.markdownwriterfx.controls.WebHyperlink;
import org.tbee.javafx.scene.layout.fxml.MigPane;
import com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tables.TablesExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension;

/**
 * Markdown options pane
 *
 * @author Karl Tauber
 */
class MarkdownOptionsPane
	extends MigPane
{
	private final ListProperty<String> extensions = new SimpleListProperty<>();

	MarkdownOptionsPane() {
		initComponents();

		autolinksExtCheckBox.setExtensionClass(AutolinkExtension.class);
		abbreviationsExtCheckBox.setExtensionClass(AbbreviationExtension.class);
		anchorlinksExtCheckBox.setExtensionClass(AnchorLinkExtension.class);
		emojiExtCheckBox.setExtensionClass(EmojiExtension.class);
		footnotesExtCheckBox.setExtensionClass(FootnoteExtension.class);
		strikethroughExtCheckBox.setExtensionClass(StrikethroughExtension.class);
		tablesExtCheckBox.setExtensionClass(TablesExtension.class);
		tocExtCheckBox.setExtensionClass(TocExtension.class);
		taskListsExtCheckBox.setExtensionClass(TaskListExtension.class);
		wikiLinksExtCheckBox.setExtensionClass(WikiLinkExtension.class);
		yamlFrontMatterExtCheckBox.setExtensionClass(YamlFrontMatterExtension.class);

/*
		hardwrapsExtCheckBox.setFlag(Extensions.HARDWRAPS);
		definitionListsExtCheckBox.setFlag(Extensions.DEFINITIONS);
		fencedCodeBlocksExtCheckBox.setFlag(Extensions.FENCED_CODE_BLOCKS);
		suppressHtmlBlocksExtCheckBox.setFlag(Extensions.SUPPRESS_HTML_BLOCKS);
		suppressInlineHtmlExtCheckBox.setFlag(Extensions.SUPPRESS_INLINE_HTML);
*/

		extensions.bindBidirectional(autolinksExtCheckBox.extensionsProperty());
		extensions.bindBidirectional(abbreviationsExtCheckBox.extensionsProperty());
		extensions.bindBidirectional(anchorlinksExtCheckBox.extensionsProperty());
		extensions.bindBidirectional(emojiExtCheckBox.extensionsProperty());
		extensions.bindBidirectional(footnotesExtCheckBox.extensionsProperty());
		extensions.bindBidirectional(strikethroughExtCheckBox.extensionsProperty());
		extensions.bindBidirectional(tablesExtCheckBox.extensionsProperty());
		extensions.bindBidirectional(tocExtCheckBox.extensionsProperty());
		extensions.bindBidirectional(taskListsExtCheckBox.extensionsProperty());
		extensions.bindBidirectional(wikiLinksExtCheckBox.extensionsProperty());
		extensions.bindBidirectional(yamlFrontMatterExtCheckBox.extensionsProperty());

/*
		extensions.bindBidirectional(hardwrapsExtCheckBox.flagsProperty());
		extensions.bindBidirectional(definitionListsExtCheckBox.flagsProperty());
		extensions.bindBidirectional(fencedCodeBlocksExtCheckBox.flagsProperty());
		extensions.bindBidirectional(suppressHtmlBlocksExtCheckBox.flagsProperty());
		extensions.bindBidirectional(suppressInlineHtmlExtCheckBox.flagsProperty());
*/
	}

	void load() {
		extensions.set(FXCollections.observableArrayList(Options.getMarkdownExtensions()));
	}

	void save() {
		Options.setMarkdownExtensions(extensions.get().sorted().toArray(new String[0]));
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		abbreviationsExtCheckBox = new ExtensionCheckBox();
		anchorlinksExtCheckBox = new ExtensionCheckBox();
		autolinksExtCheckBox = new ExtensionCheckBox();
		emojiExtCheckBox = new ExtensionCheckBox();
		WebHyperlink emojiExtLink = new WebHyperlink();
		footnotesExtCheckBox = new ExtensionCheckBox();
		strikethroughExtCheckBox = new ExtensionCheckBox();
		tablesExtCheckBox = new ExtensionCheckBox();
		tocExtCheckBox = new ExtensionCheckBox();
		taskListsExtCheckBox = new ExtensionCheckBox();
		wikiLinksExtCheckBox = new ExtensionCheckBox();
		yamlFrontMatterExtCheckBox = new ExtensionCheckBox();
		definitionListsExtCheckBox = new ExtensionCheckBox();
		WebHyperlink definitionListsExtLink = new WebHyperlink();
		fencedCodeBlocksExtCheckBox = new FlagCheckBox();
		WebHyperlink fencedCodeBlocksExtLink = new WebHyperlink();
		Label fencedCodeBlocksExtLabel = new Label();
		WebHyperlink fencedCodeBlocksExtLink2 = new WebHyperlink();
		hardwrapsExtCheckBox = new FlagCheckBox();
		WebHyperlink hardwrapsExtLink = new WebHyperlink();
		suppressHtmlBlocksExtCheckBox = new FlagCheckBox();
		suppressInlineHtmlExtCheckBox = new FlagCheckBox();

		//======== this ========
		setCols("[]");
		setRows("[][][][][][][][][][][][][][][][][]");

		//---- abbreviationsExtCheckBox ----
		abbreviationsExtCheckBox.setText(Messages.get("MarkdownOptionsPane.abbreviationsExtCheckBox.text"));
		add(abbreviationsExtCheckBox, "cell 0 0");

		//---- anchorlinksExtCheckBox ----
		anchorlinksExtCheckBox.setText(Messages.get("MarkdownOptionsPane.anchorlinksExtCheckBox.text"));
		add(anchorlinksExtCheckBox, "cell 0 1");

		//---- autolinksExtCheckBox ----
		autolinksExtCheckBox.setText(Messages.get("MarkdownOptionsPane.autolinksExtCheckBox.text"));
		add(autolinksExtCheckBox, "cell 0 2");

		//---- emojiExtCheckBox ----
		emojiExtCheckBox.setText(Messages.get("MarkdownOptionsPane.emojiExtCheckBox.text"));
		add(emojiExtCheckBox, "cell 0 3");

		//---- emojiExtLink ----
		emojiExtLink.setText(Messages.get("MarkdownOptionsPane.emojiExtLink.text"));
		emojiExtLink.setUri("http://www.emoji-cheat-sheet.com/");
		add(emojiExtLink, "cell 0 3,gapx 0");

		//---- footnotesExtCheckBox ----
		footnotesExtCheckBox.setText(Messages.get("MarkdownOptionsPane.footnotesExtCheckBox.text"));
		add(footnotesExtCheckBox, "cell 0 4");

		//---- strikethroughExtCheckBox ----
		strikethroughExtCheckBox.setText(Messages.get("MarkdownOptionsPane.strikethroughExtCheckBox.text"));
		add(strikethroughExtCheckBox, "cell 0 5");

		//---- tablesExtCheckBox ----
		tablesExtCheckBox.setText(Messages.get("MarkdownOptionsPane.tablesExtCheckBox.text"));
		add(tablesExtCheckBox, "cell 0 6");

		//---- tocExtCheckBox ----
		tocExtCheckBox.setText(Messages.get("MarkdownOptionsPane.tocExtCheckBox.text"));
		add(tocExtCheckBox, "cell 0 7");

		//---- taskListsExtCheckBox ----
		taskListsExtCheckBox.setText(Messages.get("MarkdownOptionsPane.taskListsExtCheckBox.text"));
		add(taskListsExtCheckBox, "cell 0 8");

		//---- wikiLinksExtCheckBox ----
		wikiLinksExtCheckBox.setText(Messages.get("MarkdownOptionsPane.wikiLinksExtCheckBox.text"));
		add(wikiLinksExtCheckBox, "cell 0 9");

		//---- yamlFrontMatterExtCheckBox ----
		yamlFrontMatterExtCheckBox.setText(Messages.get("MarkdownOptionsPane.yamlFrontMatterExtCheckBox.text"));
		add(yamlFrontMatterExtCheckBox, "cell 0 10");

		//---- definitionListsExtCheckBox ----
		definitionListsExtCheckBox.setText(Messages.get("MarkdownOptionsPane.definitionListsExtCheckBox.text"));
		definitionListsExtCheckBox.setVisible(false);
		add(definitionListsExtCheckBox, "cell 0 12");

		//---- definitionListsExtLink ----
		definitionListsExtLink.setText(Messages.get("MarkdownOptionsPane.definitionListsExtLink.text"));
		definitionListsExtLink.setUri("https://michelf.ca/projects/php-markdown/extra/#def-list");
		definitionListsExtLink.setVisible(false);
		add(definitionListsExtLink, "cell 0 12,gapx 0");

		//---- fencedCodeBlocksExtCheckBox ----
		fencedCodeBlocksExtCheckBox.setText(Messages.get("MarkdownOptionsPane.fencedCodeBlocksExtCheckBox.text"));
		fencedCodeBlocksExtCheckBox.setVisible(false);
		add(fencedCodeBlocksExtCheckBox, "cell 0 13");

		//---- fencedCodeBlocksExtLink ----
		fencedCodeBlocksExtLink.setText(Messages.get("MarkdownOptionsPane.fencedCodeBlocksExtLink.text"));
		fencedCodeBlocksExtLink.setUri("http://michelf.com/projects/php-markdown/extra/#fenced-code-blocks");
		fencedCodeBlocksExtLink.setVisible(false);
		add(fencedCodeBlocksExtLink, "cell 0 13,gapx 0");

		//---- fencedCodeBlocksExtLabel ----
		fencedCodeBlocksExtLabel.setText(Messages.get("MarkdownOptionsPane.fencedCodeBlocksExtLabel.text"));
		fencedCodeBlocksExtLabel.setVisible(false);
		add(fencedCodeBlocksExtLabel, "cell 0 13,gapx 3");

		//---- fencedCodeBlocksExtLink2 ----
		fencedCodeBlocksExtLink2.setText(Messages.get("MarkdownOptionsPane.fencedCodeBlocksExtLink2.text"));
		fencedCodeBlocksExtLink2.setUri("https://help.github.com/articles/github-flavored-markdown/#fenced-code-blocks");
		fencedCodeBlocksExtLink2.setVisible(false);
		add(fencedCodeBlocksExtLink2, "cell 0 13,gapx 3");

		//---- hardwrapsExtCheckBox ----
		hardwrapsExtCheckBox.setText(Messages.get("MarkdownOptionsPane.hardwrapsExtCheckBox.text"));
		hardwrapsExtCheckBox.setVisible(false);
		add(hardwrapsExtCheckBox, "cell 0 14");

		//---- hardwrapsExtLink ----
		hardwrapsExtLink.setText(Messages.get("MarkdownOptionsPane.hardwrapsExtLink.text"));
		hardwrapsExtLink.setUri("https://help.github.com/articles/writing-on-github/#markup");
		hardwrapsExtLink.setVisible(false);
		add(hardwrapsExtLink, "cell 0 14,gapx 0");

		//---- suppressHtmlBlocksExtCheckBox ----
		suppressHtmlBlocksExtCheckBox.setText(Messages.get("MarkdownOptionsPane.suppressHtmlBlocksExtCheckBox.text"));
		suppressHtmlBlocksExtCheckBox.setVisible(false);
		add(suppressHtmlBlocksExtCheckBox, "cell 0 15");

		//---- suppressInlineHtmlExtCheckBox ----
		suppressInlineHtmlExtCheckBox.setText(Messages.get("MarkdownOptionsPane.suppressInlineHtmlExtCheckBox.text"));
		suppressInlineHtmlExtCheckBox.setVisible(false);
		add(suppressInlineHtmlExtCheckBox, "cell 0 16");
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private ExtensionCheckBox abbreviationsExtCheckBox;
	private ExtensionCheckBox anchorlinksExtCheckBox;
	private ExtensionCheckBox autolinksExtCheckBox;
	private ExtensionCheckBox emojiExtCheckBox;
	private ExtensionCheckBox footnotesExtCheckBox;
	private ExtensionCheckBox strikethroughExtCheckBox;
	private ExtensionCheckBox tablesExtCheckBox;
	private ExtensionCheckBox tocExtCheckBox;
	private ExtensionCheckBox taskListsExtCheckBox;
	private ExtensionCheckBox wikiLinksExtCheckBox;
	private ExtensionCheckBox yamlFrontMatterExtCheckBox;
	private ExtensionCheckBox definitionListsExtCheckBox;
	private FlagCheckBox fencedCodeBlocksExtCheckBox;
	private FlagCheckBox hardwrapsExtCheckBox;
	private FlagCheckBox suppressHtmlBlocksExtCheckBox;
	private FlagCheckBox suppressInlineHtmlExtCheckBox;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
