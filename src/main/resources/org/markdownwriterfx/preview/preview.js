/*
 * Copyright (c) 2016 Karl Tauber <karl at jformdesigner dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
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

var preview = {

	scrollTo: function(value) {
		window.scrollTo(0, (document.body.scrollHeight - window.innerHeight) * value);
	},

	highlightTags: ['P', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6',
		'UL', 'OL', 'LI', 'DL', 'DT', 'DD',
		'TABLE', 'THEAD', 'TBODY', 'TR', 'TH', 'TD',
		'BLOCKQUOTE', 'PRE'],

	highlightedNodes: [],

	highlightNodesAt: function(offset) {
		// remove previous highlights
		for (node of this.highlightedNodes)
			node.classList.remove('mwfx-editor-selection');
		this.highlightedNodes = [];

		// find nodes to highlight
		var result = []
		this.findNodesAt(document.body, offset, result);
		if (result.length == 0)
			return;

		// highlight nodes
		for (node of result.reverse()) {
			if (this.highlightTags.includes(node.tagName)) {
				node.classList.add('mwfx-editor-selection');
				this.highlightedNodes = [node];
				break;
			}
		}
	},

	findNodesAt: function(node, offset, result) {
		if (node.dataset.pos != null) {
			// get value of data-pos attribute
			var pos = node.dataset.pos.split(':');
			var start = pos[0];
			var end = pos[1];
			if (offset >= start && offset <= end)
				result.push(node);

			if (offset > end)
				return;
		}

		var children = node.children
		for (var i=0; i < children.length; i++)
			this.findNodesAt(children[i], offset, result);
	},
};
