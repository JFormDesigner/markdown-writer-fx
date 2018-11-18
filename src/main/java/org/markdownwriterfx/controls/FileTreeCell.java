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
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import org.markdownwriterfx.util.Utils;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

/**
 * The {@link TreeCell} type used with the {@link FileTreeView} control.
 *
 * @author Karl Tauber
 */
public class FileTreeCell
	extends TreeCell<File>
{
	public FileTreeCell() {
		// expand/collapse tree item with single click
		setOnMouseReleased(event -> {
			TreeItem<File> treeItem = getTreeItem();
			if (treeItem != null &&
				!treeItem.isLeaf() &&
				(getDisclosureNode() == null || !getDisclosureNode().getBoundsInParent().contains(event.getX(), event.getY())))
			{
				treeItem.setExpanded(!treeItem.isExpanded());
			}

			if (getTreeView() instanceof FileTreeView)
				((FileTreeView)getTreeView()).handleClicks(treeItem, event.getButton(), event.getClickCount());
		});
	}

	@Override
	protected void updateItem(File file, boolean empty) {
		super.updateItem(file, empty);

		String text = null;
		Node graphic = null;
		if (!empty && file != null) {
			text = file.getName();
			graphic = FontAwesomeIconFactory.get().createIcon(getTreeItem().isLeaf()
				? fileIcon(text)
				: FontAwesomeIcon.FOLDER_ALT);
		}
		setText(text);
		setGraphic(graphic);
	}

	private FontAwesomeIcon fileIcon(String name) {
		return Utils.isImage(name) ? FontAwesomeIcon.FILE_IMAGE_ALT : FontAwesomeIcon.FILE_TEXT_ALT;
	}
}
