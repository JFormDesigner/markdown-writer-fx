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
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.markdownwriterfx.util.Utils;

/**
 * The {@link TreeItem} type used with the {@link FileTreeView} control.
 *
 * Refreshes its children recursively when expanding this item.
 *
 * @author Karl Tauber
 */
public class FileTreeItem
	extends TreeItem<File>
{
	private static final Comparator<File> FILE_COMPARATOR = (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName());
	private static final Comparator<TreeItem<File>> ITEM_COMPARATOR = (i1, i2) -> FILE_COMPARATOR.compare(i1.getValue(), i2.getValue());

	private final FilenameFilter filter;

	private boolean leaf;
	private boolean leafInitialized;
	private boolean childrenInitialized;
	private boolean expandedListenerAdded;

	public FileTreeItem(File file) {
		this(file, null);
	}

	public FileTreeItem(File file, FilenameFilter filter) {
		super(file);
		this.filter = filter;
	}

	@Override
	public boolean isLeaf() {
		if (!leafInitialized) {
			leafInitialized = true;
			leaf = getValue().isFile();

			// add expanded listener only to non-leafs (to safe memory)
			if (!leaf && !expandedListenerAdded) {
				expandedListenerAdded = true;
				expandedProperty().addListener((observable, oldExpanded, newExpanded) -> {
					if (newExpanded)
						refresh();
				});
			}
		}
		return leaf;
	}

	@Override
	public ObservableList<TreeItem<File>> getChildren() {
		if (!childrenInitialized) {
			childrenInitialized = true;

			File f = getValue();
			if (f.isDirectory()) {
				File[] files = f.listFiles(filter);
				if (files != null) {
					Arrays.sort(files, FILE_COMPARATOR);
					ArrayList<TreeItem<File>> children = new ArrayList<>();
					for (File file : files)
						children.add(new FileTreeItem(file, filter));
					super.getChildren().setAll(children);
				}
			}
		}
		return super.getChildren();
	}

	public ObservableList<TreeItem<File>> getLoadedChildren() {
		return super.getChildren();
	}

	public void refresh() {
		if (leafInitialized) {
			// check whether file has changed from directory to normal file or vice versa
			boolean oldLeaf = leaf;
			leafInitialized = false;
			boolean newLeaf = isLeaf();
			if (newLeaf != oldLeaf) {
				childrenInitialized = false;
				super.getChildren().clear();
				return;
			}
		}

		if (!childrenInitialized || isLeaf())
			return;

		// get current files
		ObservableList<TreeItem<File>> children = super.getChildren();
		File f = getValue();
		File[] newFiles = f.isDirectory() ? f.listFiles(filter) : null;
		if (newFiles == null || newFiles.length == 0) {
			children.clear();
			return;
		}

		// determine added and removed files
		HashSet<File> addedFiles = new HashSet<>(Arrays.asList(newFiles));
		ArrayList<TreeItem<File>> removedFiles = new ArrayList<>();
		for (TreeItem<File> item : children) {
			if (!addedFiles.remove(item.getValue()))
				removedFiles.add(item);
		}

		// remove files
		if (!removedFiles.isEmpty())
			children.removeAll(removedFiles);

		// add files
		for (File file : addedFiles)
			Utils.addSorted(children, new FileTreeItem(file, filter), ITEM_COMPARATOR);

		// refresh loaded children
		for (TreeItem<File> item : children) {
			if (item instanceof FileTreeItem)
				((FileTreeItem)item).refresh();
		}
	}
}
