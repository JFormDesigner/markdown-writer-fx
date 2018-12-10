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

package org.markdownwriterfx.projects;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import org.markdownwriterfx.options.Options;
import org.markdownwriterfx.util.Utils;
import com.esotericsoftware.yamlbeans.YamlConfig.WriteClassName;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;

/**
 * Project settings.
 *
 * @author Karl Tauber
 */
public class ProjectSettings
{
	private static final String FILENAME = ".markdownwriterfx";

	private static final WeakHashMap<File, ProjectSettings> settingsMap = new WeakHashMap<>();

	private final File settingsFile;
	private ProjectPreferences options;

	public static ProjectSettings get(File project) {
		return settingsMap.computeIfAbsent(project, p -> new ProjectSettings(p));
	}

	private ProjectSettings(File project) {
		this.settingsFile = new File(project, FILENAME);

		load();
	}

	public Preferences getOptions() {
		return options;
	}

	/**
	 * For internal use only.
	 */
	public void enableOptions(boolean enable) {
		if (enable)
			options = new ProjectPreferences();
		else {
			options = null;
			settingsFile.delete();
		}
	}

	@SuppressWarnings("unchecked")
	private void load() {
		if (!settingsFile.isFile())
			return;

		try {
			// read project settings
			byte[] bytes = Files.readAllBytes(settingsFile.toPath());
			String yaml = new String(bytes, StandardCharsets.UTF_8);

			// decode YAML
			YamlReader yamlReader = new YamlReader(yaml);
			try {
				Object object = yamlReader.read();
				if (object instanceof Map) {
					Object o = ((Map<String, Object>)object).get("options");
					if (o instanceof Map) {
						options = new ProjectPreferences();
						for (Map.Entry<String, Object> e : ((Map<String, Object>)o).entrySet()) {
							if (e.getValue() instanceof String)
								options.valuesMap.put(e.getKey(), (String) e.getValue());
						}
					}
				}
			} finally {
				yamlReader.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private void save() {
		// save maps to YAML to get clean output
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("options", options.valuesMap);

		try {
			// encode YAML
			StringWriter writer = new StringWriter(1000);
			YamlWriter yamlWriter = new YamlWriter(writer);
			try {
				yamlWriter.getConfig().writeConfig.setWriteClassname(WriteClassName.NEVER);
				yamlWriter.write(map);
			} finally {
				yamlWriter.close();
			}

			String yaml = writer.toString();

			// use line separator that is specified in the options
			String sysLineSep = System.getProperty("line.separator");
			String optLineSep = Options.getLineSeparator();
			if (optLineSep != null && !Utils.safeEquals(sysLineSep, optLineSep))
				yaml = yaml.replace(sysLineSep, optLineSep);

			// write project settings
			Files.write(settingsFile.toPath(), yaml.getBytes(StandardCharsets.UTF_8));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	//---- class ProjectPreferences -------------------------------------------

	private class ProjectPreferences
		extends AbstractPreferences
	{
		final LinkedHashMap<String, String> valuesMap = new LinkedHashMap<>();
		private Runnable autoFlushRunnable;

		ProjectPreferences() {
			super(null, "");
		}

		@Override
		protected String[] keysSpi() throws BackingStoreException {
			return valuesMap.keySet().toArray(new String[valuesMap.size()]);
		}

		@Override
		protected String getSpi(String key) {
			return valuesMap.get(key);
		}

		@Override
		protected void putSpi(String key, String value) {
			valuesMap.put(key, value);
			autoFlush();
		}

		@Override
		protected void removeSpi(String key) {
			valuesMap.remove(key);
			autoFlush();
		}

		@Override
		protected String[] childrenNamesSpi() throws BackingStoreException {
			return new String[0];
		}

		@Override
		protected AbstractPreferences childSpi(String name) {
			throw new IllegalStateException();
		}

		@Override
		protected void removeNodeSpi() throws BackingStoreException {
		}

		@Override
		protected void syncSpi() throws BackingStoreException {
		}

		@Override
		protected void flushSpi() throws BackingStoreException {
			save();
		}

		private void autoFlush() {
			if (autoFlushRunnable != null)
				return;

			autoFlushRunnable = () -> {
				autoFlushRunnable = null;
				save();
			};
			Platform.runLater(autoFlushRunnable);
		}
	}
}
