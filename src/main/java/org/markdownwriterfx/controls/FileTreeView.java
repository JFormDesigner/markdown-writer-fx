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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

/**
 * A tree view of directories and files.
 *
 * @author Karl Tauber
 */
public class FileTreeView
	extends TreeView<File>
{
	public FileTreeView() {
		setCellFactory(treeView -> new FileTreeCell());
	}

	public List<File> getExpandedDirectories() {
		if (getRoot() == null)
			return Collections.emptyList();

		return items2files(findItems(item -> item.isExpanded()));
	}

	public void setExpandedDirectories(List<File> expandedDirectories) {
		if (getRoot() == null)
			return;

		HashSet<File> expandedDirectoriesSet = new HashSet<>(expandedDirectories);
		expandDirectories(getRoot(), expandedDirectoriesSet);
	}

	private void expandDirectories(TreeItem<File> item, HashSet<File> expandedDirectoriesSet) {
		getLoadedChildren(item).forEach(child -> {
			if (!child.isLeaf()) {
				if (expandedDirectoriesSet.contains(child.getValue()))
					child.setExpanded(true);

				expandDirectories(child, expandedDirectoriesSet);
			}
		});
	}

	public List<TreeItem<File>> findItems(Predicate<TreeItem<File>> predicate) {
		if (getRoot() == null)
			return Collections.emptyList();

		ArrayList<TreeItem<File>> items = new ArrayList<>();
		findItemsRecur(predicate, getRoot(), items);
		return items;
	}

	private void findItemsRecur(Predicate<TreeItem<File>> predicate, TreeItem<File> item, List<TreeItem<File>> items) {
		if (predicate.test(item))
			items.add(item);

		getLoadedChildren(item).forEach(child -> {
			findItemsRecur(predicate, child, items);
		});
	}

	private List<File> items2files(List<TreeItem<File>> items) {
		return items.stream()
			.map(item -> item.getValue())
			.collect(Collectors.toList());
	}

	private ObservableList<TreeItem<File>> getLoadedChildren(TreeItem<File> item) {
		return (item instanceof FileTreeItem && !item.isExpanded())
			? ((FileTreeItem)item).getLoadedChildren()
			: item.getChildren();
	}
}
