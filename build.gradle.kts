import org.gradle.plugins.ide.eclipse.model.AbstractClasspathEntry
import org.gradle.plugins.ide.eclipse.model.AccessRule

version = "0.10"

plugins {
	java
	application
	eclipse
}

repositories {
	jcenter()
}

dependencies {
	compile( "org.fxmisc.richtext:richtextfx:0.9.1" )
	compile( "com.miglayout:miglayout-javafx:5.2" )
	compile( "de.jensd:fontawesomefx-fontawesome:4.7.0-5" )
	compile( "org.controlsfx:controlsfx:8.40.14" )
	compile( "org.fxmisc.cssfx:cssfx:1.0.0" )
	compile( "org.apache.commons:commons-lang3:3.7" )

	val flexmarkVersion = "0.32.20"
	compile( "com.vladsch.flexmark:flexmark:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-abbreviation:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-anchorlink:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-aside:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-autolink:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-definition:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-footnotes:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-gfm-strikethrough:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-gfm-tables:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-gfm-tasklist:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-toc:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-wikilink:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-yaml-front-matter:${flexmarkVersion}" )

	val commonmarkVersion = "0.11.0"
	compile( "com.atlassian.commonmark:commonmark:${commonmarkVersion}" )
	compile( "com.atlassian.commonmark:commonmark-ext-autolink:${commonmarkVersion}" )
	compile( "com.atlassian.commonmark:commonmark-ext-gfm-strikethrough:${commonmarkVersion}" )
	compile( "com.atlassian.commonmark:commonmark-ext-gfm-tables:${commonmarkVersion}" )
	compile( "com.atlassian.commonmark:commonmark-ext-heading-anchor:${commonmarkVersion}" )
	compile( "com.atlassian.commonmark:commonmark-ext-ins:${commonmarkVersion}" )
	compile( "com.atlassian.commonmark:commonmark-ext-yaml-front-matter:${commonmarkVersion}" )

	testCompile( "junit:junit:4.12" )
}

java {
	sourceCompatibility = JavaVersion.VERSION_1_8
	targetCompatibility = JavaVersion.VERSION_1_8
}

application {
	mainClassName = "org.markdownwriterfx.MarkdownWriterFXApp"
}

val jar: Jar by tasks
jar.manifest {
	attributes( mapOf(
		"Main-Class" to "org.markdownwriterfx.MarkdownWriterFXApp",
		"Class-Path" to configurations.compile.map { it.getName() }.joinToString( " " ),
		"Implementation-Version" to version ) )
}

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
