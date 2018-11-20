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

package org.markdownwriterfx;

/**
 * Used to check whether extensions used in MarkdownExtensions.properties
 * still exist when upgrading to a newer flexmark-java version.
 *
 * @author Karl Tauber
 */
public class FlexMarkExtensionsTest
{
	public static void main(String[] args) {
		com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension.create();
		com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension.create();
		com.vladsch.flexmark.ext.aside.AsideExtension.create();
		com.vladsch.flexmark.ext.autolink.AutolinkExtension.create();
		com.vladsch.flexmark.ext.definition.DefinitionExtension.create();
		com.vladsch.flexmark.ext.footnotes.FootnoteExtension.create();
		com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension.create();
		com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension.create();
		com.vladsch.flexmark.ext.tables.TablesExtension.create();
		com.vladsch.flexmark.ext.toc.TocExtension.create();
		com.vladsch.flexmark.ext.wikilink.WikiLinkExtension.create();
		com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension.create();
	}
}
