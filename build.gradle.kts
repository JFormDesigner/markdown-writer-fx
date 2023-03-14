import java.lang.Boolean
import org.gradle.plugins.ide.eclipse.model.AbstractClasspathEntry
import org.gradle.plugins.ide.eclipse.model.AccessRule

val releaseVersion = "0.12"
val developmentVersion = "0.13-SNAPSHOT"

version = if( Boolean.getBoolean( "release" ) ) releaseVersion else developmentVersion

// check required Java version
when( JavaVersion.current() ) {
	JavaVersion.VERSION_19 -> true
	else -> throw RuntimeException( "Java 19 required (running ${System.getProperty( "java.version" )})" )
}

// use Java 19 source/target compatibility
val javaCompatibility = 19

// log version, Gradle and Java versions
println()
println( "-------------------------------------------------------------------------------" )
println( "Markdown Writer FX Version: ${version}" )
println( "Gradle ${gradle.gradleVersion} at ${gradle.gradleHomeDir}" )
println( "Java ${System.getProperty( "java.version" )}" )
println()

plugins {
	java
	application
	id( "org.openjfx.javafxplugin" ) version "0.0.13"
	eclipse
}

repositories {
	mavenCentral()
}

javafx {
	version = "19.0.2.1"
	modules = listOf( "javafx.controls", "javafx.web" )
}

dependencies {
	implementation( "org.fxmisc.richtext:richtextfx:0.11.0" )
	implementation( "com.miglayout:miglayout-javafx:11.0" )

	implementation( "de.jensd:fontawesomefx-fontawesome:4.7.0-9.1.2" )
	implementation( "org.controlsfx:controlsfx:11.1.2" )
	implementation( "fr.brouillard.oss:cssfx:11.5.1" )
	implementation( "org.apache.commons:commons-lang3:3.12.0" )
	implementation( "com.esotericsoftware.yamlbeans:yamlbeans:1.15" )
	implementation( "org.languagetool:language-en:5.2" )

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
