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

package org.markdownwriterfx.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import org.markdownwriterfx.options.Options;

/**
 * @author Karl Tauber
 */
public class Addons
{
	private static ClassLoader addonsClassLoader;

	public static ClassLoader getAddonsClassLoader() {
		if( addonsClassLoader != null )
			return addonsClassLoader;

		ClassLoader cl = Thread.currentThread().getContextClassLoader();

		String addonsPath = Options.getAddonsPath();
		if( Utils.isNullOrEmpty( addonsPath ) )
			return cl;

		List<URL> urls = new ArrayList<>();
		for( String s : addonsPath.split( ";" ) ) {
			File file = new File( s );
			if( file.isFile() && file.getName().endsWith( ".jar" ) ) {
				// add JAR
				addUrl( urls, file );
			} else if( file.isDirectory() ) {
				// search for JARs
				File[] files = file.listFiles( (dir,name) -> name.endsWith( ".jar" ) );
				if( files != null && files.length > 0 ) {
					// add JARs
					for( File f : files )
						addUrl( urls, f );
				} else {
					// add classes directory
					addUrl( urls, file );
				}
			}
		}

		addonsClassLoader = new URLClassLoader( urls.toArray( URL[]::new ), cl );

		return addonsClassLoader;
	}

	private static void addUrl( List<URL> urls, File file ) {
		try {
			urls.add( file.toURI().toURL() );
		} catch( MalformedURLException ex ) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
	}
}
