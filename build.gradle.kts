import org.gradle.plugins.ide.eclipse.model.AbstractClasspathEntry
import org.gradle.plugins.ide.eclipse.model.AccessRule

version = "0.11"

// check required Java version
if( JavaVersion.current() < JavaVersion.VERSION_1_8 || JavaVersion.current() > JavaVersion.VERSION_11 )
	throw RuntimeException( "Java 8, 9, 10 or 11 required (running ${System.getProperty( "java.version" )})" )

// use Java version that currently runs Gradle for source/target compatibility
val javaCompatibility = JavaVersion.current()

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
	eclipse
}

repositories {
	jcenter()
}

dependencies {
	compile( "org.fxmisc.richtext:richtextfx:0.9.2" )
	compile( "com.miglayout:miglayout-javafx:5.2" )

	val fontawesomefxVersion = if( javaCompatibility >= JavaVersion.VERSION_1_9 ) "4.7.0-9.1.2" else "4.7.0-5"
	val controlsfxVersion = if( javaCompatibility >= JavaVersion.VERSION_1_9 ) "9.0.0" else "8.40.14"
	compile( "de.jensd:fontawesomefx-fontawesome:${fontawesomefxVersion}" )
	if( javaCompatibility == JavaVersion.VERSION_1_8 ) {
		// required since Gradle 5.0 because fontawesomefx-fontawesome-4.7.0-5.pom uses
		// scope "runtime" for its "fontawesomefx-commons" dependency
		// (fontawesomefx-fontawesome-4.7.0-9.pom uses scope "compile")
		// https://docs.gradle.org/5.0/userguide/upgrading_version_4.html#rel5.0:pom_compile_runtime_separation
		compile( "de.jensd:fontawesomefx-commons:8.15" )
	}
	compile( "org.controlsfx:controlsfx:${controlsfxVersion}" )
	compile( "org.fxmisc.cssfx:cssfx:1.0.0" )
	compile( "org.apache.commons:commons-lang3:3.8.1" )
	compile( "com.esotericsoftware.yamlbeans:yamlbeans:1.13" )

	val flexmarkVersion = "0.35.0"
	compile( "com.vladsch.flexmark:flexmark:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-abbreviation:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-anchorlink:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-aside:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-autolink:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-definition:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-footnotes:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-gfm-strikethrough:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-gfm-tasklist:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-tables:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-toc:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-wikilink:${flexmarkVersion}" )
	compile( "com.vladsch.flexmark:flexmark-ext-yaml-front-matter:${flexmarkVersion}" )

	val commonmarkVersion = "0.12.1"
	compile( "com.atlassian.commonmark:commonmark:${commonmarkVersion}" )
	compile( "com.atlassian.commonmark:commonmark-ext-autolink:${commonmarkVersion}" )
	compile( "com.atlassian.commonmark:commonmark-ext-gfm-strikethrough:${commonmarkVersion}" )
	compile( "com.atlassian.commonmark:commonmark-ext-gfm-tables:${commonmarkVersion}" )
	compile( "com.atlassian.commonmark:commonmark-ext-heading-anchor:${commonmarkVersion}" )
	compile( "com.atlassian.commonmark:commonmark-ext-ins:${commonmarkVersion}" )
	compile( "com.atlassian.commonmark:commonmark-ext-yaml-front-matter:${commonmarkVersion}" )

	if( javaCompatibility >= JavaVersion.VERSION_11 ) {
		val javafxVersion = "11.0.1"
		val osName = System.getProperty( "os.name" ).toLowerCase()
		val platform = if( osName.startsWith( "windows" ) ) "win" else if( osName.startsWith( "mac" ) ) "mac" else "linux"

		compile( "org.openjfx:javafx-base:${javafxVersion}:${platform}" )
		compile( "org.openjfx:javafx-controls:${javafxVersion}:${platform}" )
		compile( "org.openjfx:javafx-graphics:${javafxVersion}:${platform}" )
		compile( "org.openjfx:javafx-web:${javafxVersion}:${platform}" )
	}

	testCompile( "junit:junit:4.12" )
}

java {
	sourceCompatibility = javaCompatibility
	targetCompatibility = javaCompatibility
}

application {
	mainClassName = "org.markdownwriterfx.MarkdownWriterFXApp"
}

val jar: Jar by tasks
jar.manifest {
	attributes( mapOf(
		"Main-Class" to "org.markdownwriterfx.MarkdownWriterFXApp",
		"Class-Path" to configurations.compile.get().map { it.getName() }.joinToString( " " ),
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
