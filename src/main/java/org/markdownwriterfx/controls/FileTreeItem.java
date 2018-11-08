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

package org.markdownwriterfx.controls;

import java.io.File;
import java.util.ArrayList;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

/**
 * The {@link TreeItem} type used with the {@link FileTreeView} control.
 *
 * @author Karl Tauber
 */
public class FileTreeItem
	extends TreeItem<File>
{
	private boolean leaf;
	private boolean leafInitialized;
	private boolean childrenInitialized;

	public FileTreeItem(File file) {
		super(file);
	}

	@Override
	public boolean isLeaf() {
		if (!leafInitialized) {
			leafInitialized = true;
			leaf = getValue().isFile();
		}
		return leaf;
	}

	@Override
	public ObservableList<TreeItem<File>> getChildren() {
		if (!childrenInitialized) {
			childrenInitialized = true;

			File f = getValue();
			if (f.isDirectory()) {
				File[] files = f.listFiles();
				if (files != null) {
					ArrayList<TreeItem<File>> children = new ArrayList<>();
					for (File file : files)
						children.add(new FileTreeItem(file));
					super.getChildren().setAll(children);
				}
			}
		}
		return super.getChildren();
	}
}
