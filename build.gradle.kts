import java.lang.Boolean
import java.util.Locale
import org.gradle.plugins.ide.eclipse.model.AbstractClasspathEntry
import org.gradle.plugins.ide.eclipse.model.AccessRule

val releaseVersion = "0.12"
val developmentVersion = "1.0"

version = if( Boolean.getBoolean( "release" ) ) releaseVersion else developmentVersion

// check required Java version
when( JavaVersion.current() ) {
	JavaVersion.VERSION_19 -> true
	else -> throw RuntimeException( "Java 19 required (running ${System.getProperty( "java.version" )})" )
}

// use Java 19 source/target compatibility
val javaCompatibility = 19

val osName = System.getProperty( "os.name" ).lowercase( Locale.ENGLISH )
val osArch = System.getProperty( "os.arch" )
val isWindows = osName.startsWith( "windows" )
val isMac = osName.startsWith( "mac" )
val isLinux = osName.startsWith( "linux" )

// log version, Gradle and Java versions
println()
println( "-------------------------------------------------------------------------------" )
println( "Markdown Writer FX Version: ${version}" )
println( "Gradle ${gradle.gradleVersion} at ${gradle.gradleHomeDir}" )
println( "Java ${System.getProperty( "java.version" )} ${System.getProperty( "java.vendor" )}" )
println()

plugins {
	java
	application
	id( "org.openjfx.javafxplugin" ) version "0.0.13"
	id( "org.beryx.runtime" ) version "1.13.0"
	eclipse
}

repositories {
	mavenCentral()
}

javafx {
	version = "19.0.2.1"
	modules = listOf( "javafx.controls", "javafx.swing", "javafx.web" )
}

dependencies {
	implementation( "org.fxmisc.richtext:richtextfx:0.11.0" )
	implementation( "com.miglayout:miglayout-javafx:11.0" )

	implementation( "de.jensd:fontawesomefx-fontawesome:4.7.0-9.1.2" )
	implementation( "org.controlsfx:controlsfx:11.1.2" )
	implementation( "fr.brouillard.oss:cssfx:11.5.1" )
	implementation( "org.apache.commons:commons-lang3:3.12.0" )
	implementation( "com.esotericsoftware.yamlbeans:yamlbeans:1.15" )

	val flexmarkVersion = "0.64.0"
	implementation( "com.vladsch.flexmark:flexmark:${flexmarkVersion}" )
	implementation( "com.vladsch.flexmark:flexmark-ext-abbreviation:${flexmarkVersion}" )
	implementation( "com.vladsch.flexmark:flexmark-ext-anchorlink:${flexmarkVersion}" )
	implementation( "com.vladsch.flexmark:flexmark-ext-aside:${flexmarkVersion}" )
	implementation( "com.vladsch.flexmark:flexmark-ext-autolink:${flexmarkVersion}" )
	implementation( "com.vladsch.flexmark:flexmark-ext-definition:${flexmarkVersion}" )
	implementation( "com.vladsch.flexmark:flexmark-ext-footnotes:${flexmarkVersion}" )
	implementation( "com.vladsch.flexmark:flexmark-ext-gfm-strikethrough:${flexmarkVersion}" )
	implementation( "com.vladsch.flexmark:flexmark-ext-gfm-tasklist:${flexmarkVersion}" )
	implementation( "com.vladsch.flexmark:flexmark-ext-tables:${flexmarkVersion}" )
	implementation( "com.vladsch.flexmark:flexmark-ext-toc:${flexmarkVersion}" )
	implementation( "com.vladsch.flexmark:flexmark-ext-wikilink:${flexmarkVersion}" )
	implementation( "com.vladsch.flexmark:flexmark-ext-yaml-front-matter:${flexmarkVersion}" )

	val commonmarkVersion = "0.21.0"
	implementation( "org.commonmark:commonmark:${commonmarkVersion}" )
	implementation( "org.commonmark:commonmark-ext-autolink:${commonmarkVersion}" )
	implementation( "org.commonmark:commonmark-ext-gfm-strikethrough:${commonmarkVersion}" )
	implementation( "org.commonmark:commonmark-ext-gfm-tables:${commonmarkVersion}" )
	implementation( "org.commonmark:commonmark-ext-heading-anchor:${commonmarkVersion}" )
	implementation( "org.commonmark:commonmark-ext-ins:${commonmarkVersion}" )
	implementation( "org.commonmark:commonmark-ext-yaml-front-matter:${commonmarkVersion}" )

	val languagetoolVersion = "6.0"
	implementation( "org.languagetool:language-all:${languagetoolVersion}" )
	implementation( "com.google.guava:guava:31.1-jre" ) // required for languagetool, which would otherwise use '31.1-android'

	testImplementation( "junit:junit:4.13.2" )
}

tasks.compileJava {
	options.release.set( javaCompatibility )
}

application {
	mainClass.set( "org.markdownwriterfx.MarkdownWriterFXApp" )
}

/*
val jar: Jar by tasks
jar.manifest {
	attributes( mapOf(
		"Main-Class" to "org.markdownwriterfx.MarkdownWriterFXApp",
		"Class-Path" to configurations.compile.get().map { it.getName() }.joinToString( " " ),
		"Implementation-Version" to version ) )
}
 */

distributions {
	getByName( "main" ) {
		contents {
			from( "LICENSE", "README.md", "CHANGES.md" )
			into( "images" ) {
				from( "images" )
			}
		}
	}
}

runtime {
	options.set( listOf( "--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages" ) )
	modules.set( listOf(
		"java.prefs",
		"java.desktop",
		"jdk.jfr",
		"java.xml",
		"jdk.unsupported",
		"java.net.http",
		"jdk.jsobject",
		"jdk.xml.dom",
		"java.logging",

		// this requires that Gradle is running on a JDK that includes JavaFX
		// e.g. BellSoft Liberica JDK (package 'Full JDK') or Azul Zulu JDK (package 'JDK FX')
		"javafx.controls",
		"javafx.swing", // for addons
		"javafx.web",
	) )

	jpackage {
		imageName = "Markdown Writer FX $version"

		val icon = if( isWindows ) "images/markdown-writer-fx.ico"
			else if( isMac ) "images/markdown-writer-fx.icns"
			else "images/markdown-writer-fx-256.png"
		imageOptions = listOf( "--icon", icon )

		if( isWindows )
			skipInstaller = true
		else if( isMac )
			installerType = "dmg"
		else if( isLinux )
			skipInstaller = true
	}
}

tasks {
	assembleDist {
		dependsOn( "distAppZip" )
		dependsOn( "distMacDmg" )
	}

	register<Zip>( "distAppZip" ) {
		group = "distribution"
		dependsOn( "jpackage" )
		onlyIf { isWindows || isLinux }

		archiveFileName.set( "markdown-writer-fx-$version-${ if( isWindows ) "win" else "linux" }.zip" )

		from( layout.buildDirectory.dir( "jpackage" ) ) {
			// exclude JavaFX jars because JavaFX is included in JRE built by jlink
			exclude( "**/app/javafx-*.jar" )
		}
	}

	register<Copy>( "distMacDmg" ) {
		group = "distribution"
		dependsOn( "jpackage" )
		onlyIf { isMac }

		from( layout.buildDirectory.file( "jpackage/markdown-writer-fx-$version.dmg" ) )
		rename( "markdown-writer-fx-$version.dmg", "markdown-writer-fx-$version-mac-$osArch.dmg" )
		into( layout.buildDirectory.dir( "distributions" ) )
	}

	// disable some tasks
	distTar { onlyIf{ false } }
	distZip { onlyIf{ false } }
}

//---- eclipse ----------------------------------------------------------------

eclipse {
	classpath {
		file {
			whenMerged.add( object: Action<org.gradle.plugins.ide.eclipse.model.Classpath> {
				override fun execute( classpath: org.gradle.plugins.ide.eclipse.model.Classpath ) {
					val jre = classpath.entries.find {
						it is AbstractClasspathEntry &&
							it.path.contains("org.eclipse.jdt.launching.JRE_CONTAINER")
					} as AbstractClasspathEntry

					// make JavaFX API accessible in Eclipse project
					// (when refreshing Gradle project in buildship)
					jre.accessRules.add(AccessRule("accessible", "javafx/**"))
					jre.accessRules.add(AccessRule("accessible", "com/sun/javafx/**"))

					// remove trailing slash from jre path
					if (jre.path.endsWith("/"))
						jre.path = jre.path.substring(0, jre.path.length - 1)
				}
			} )
		}
	}
}
