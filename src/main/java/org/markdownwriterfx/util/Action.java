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

package org.markdownwriterfx.util;

import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCombination;
import de.jensd.fx.glyphs.GlyphIcons;

/**
 * Simple action class
 *
 * @author Karl Tauber
 */
public class Action
{
	public final String text;
	public final KeyCombination accelerator;
	public final GlyphIcons icon;
	public final EventHandler<ActionEvent> action;
	public final ObservableBooleanValue disable;
	public final BooleanProperty selected;

	public Action(String text, String accelerator, GlyphIcons icon,
		EventHandler<ActionEvent> action)
	{
		this(text, accelerator, icon, action, null, null);
	}

	public Action(String text, String accelerator, GlyphIcons icon,
		EventHandler<ActionEvent> action, ObservableBooleanValue disable)
	{
		this(text, accelerator, icon, action, disable, null);
	}

	public Action(String text, String accelerator, GlyphIcons icon,
		EventHandler<ActionEvent> action, ObservableBooleanValue disable,
		BooleanProperty selected)
	{
		this.text = text;
		this.accelerator = (accelerator != null) ? KeyCombination.valueOf(accelerator) : null;
		this.icon = icon;
		this.action = action;
		this.disable = disable;
		this.selected = selected;
	}
}
