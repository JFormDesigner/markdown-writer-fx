/*
 * Copyright (c) 2023 Karl Tauber <karl at jformdesigner dot com>
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

/**
 * @author Karl Tauber
 */
module org.markdownwriterfx {
	exports org.markdownwriterfx;
	exports org.markdownwriterfx.addons;

	// open images, CSS, etc
	opens org.markdownwriterfx;
	opens org.markdownwriterfx.editor;

	// Java
	requires java.prefs;

	// JavaFX
	requires javafx.controls;
	requires javafx.web;

	// RichTextFX
	requires org.fxmisc.richtext;
	requires org.fxmisc.flowless;
	requires org.fxmisc.undo;
	requires wellbehavedfx;

	// misc
	requires com.miglayout.javafx;
	requires de.jensd.fx.glyphs.commons;
	requires de.jensd.fx.glyphs.fontawesome;
	requires org.controlsfx.controls;
	requires fr.brouillard.oss.cssfx;
	requires org.apache.commons.lang3;
	requires yamlbeans;

	// LanguageTool
//	requires languagetool.core;
//	requires language.en;

	// FlexMark
	requires flexmark;
	requires flexmark.util.ast;
	requires flexmark.util.misc;
	requires flexmark.util.sequence;
	requires flexmark.ext.abbreviation;
	requires flexmark.ext.anchorlink;
	requires flexmark.ext.aside;
	requires flexmark.ext.autolink;
	requires flexmark.ext.definition;
	requires flexmark.ext.footnotes;
	requires flexmark.ext.gfm.strikethrough;
	requires flexmark.ext.gfm.tasklist;
	requires flexmark.ext.tables;
	requires flexmark.ext.toc;
	requires flexmark.ext.wikilink;
	requires flexmark.ext.yaml.front.matter;

	// CommonMark
	requires org.commonmark;
	requires org.commonmark.ext.autolink;
	requires org.commonmark.ext.gfm.strikethrough;
	requires org.commonmark.ext.gfm.tables;
	requires org.commonmark.ext.heading.anchor;
	requires org.commonmark.ext.ins;
	requires org.commonmark.ext.front.matter;

	// ServiceLoader
	uses org.markdownwriterfx.addons.MarkdownSyntaxHighlighterAddon;
	uses org.markdownwriterfx.addons.PreviewRendererAddon;
	uses org.markdownwriterfx.addons.PreviewViewAddon;
	uses org.markdownwriterfx.addons.SmartFormatAddon;
	uses org.markdownwriterfx.addons.SpellCheckerAddon;
}
