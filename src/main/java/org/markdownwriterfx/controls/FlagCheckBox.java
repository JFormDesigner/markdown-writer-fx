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

package org.markdownwriterfx.controls;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.CheckBox;

/**
 * CheckBox that toggles a bit in an integer.
 *
 * @author Karl Tauber
 */
public class FlagCheckBox
	extends CheckBox
{
	public FlagCheckBox() {
		setOnAction(e -> {
			if (isSelected())
				setFlags(getFlags() | getFlag());
			else
				setFlags(getFlags() & ~getFlag());
		});

		flags.addListener((obs, oldFlags, newFlags) -> {
			setSelected((newFlags.intValue() & getFlag()) != 0);
		});
	}

	// 'flag' property
	private final IntegerProperty flag = new SimpleIntegerProperty();
	public int getFlag() { return flag.get(); }
	public void setFlag(int flag) { this.flag.set(flag); }
	public IntegerProperty flagProperty() { return flag; }

	// 'flags' property
	private final IntegerProperty flags = new SimpleIntegerProperty();
	public int getFlags() { return flags.get(); }
	public void setFlags(int flags) { this.flags.set(flags); }
	public IntegerProperty flagsProperty() { return flags; }
}
