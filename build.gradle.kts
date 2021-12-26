import java.lang.Boolean
import org.gradle.plugins.ide.eclipse.model.AbstractClasspathEntry
import org.gradle.plugins.ide.eclipse.model.AccessRule

val releaseVersion = "0.12"
val developmentVersion = "0.13-SNAPSHOT"
val richtextfxBranchVersion = "0.10.7"//0.13

version = if( Boolean.getBoolean( "release" ) ) releaseVersion else developmentVersion

// check required Java version
when( JavaVersion.current() ) {
	JavaVersion.VERSION_1_8,
	JavaVersion.VERSION_11,
	JavaVersion.VERSION_15 -> true
	else -> throw RuntimeException( "Java 8, 11 or 15 required (running ${System.getProperty( "java.version" )})" )
}

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
	mavenCentral()
	maven("https://maven.aliyun.com/repository/public/")
	maven("https://maven.aliyun.com/repository/spring/")
}

dependencies {
	// build RichTextFX from branch 'markdown-writer-fx' on https://github.com/JFormDesigner/RichTextFX
//	implementation( "org.fxmisc.richtext:richtextfx" ) {
//		version {
//			branch = "markdown-writer-fx-${richtextfxBranchVersion}"
//		}
//	}

	// https://mvnrepository.com/artifact/org.fxmisc.richtext/richtextfx
	implementation("org.fxmisc.richtext:richtextfx:0.10.7")

	implementation("com.miglayout:miglayout-javafx:11.0")

	val fontawesomefxVersion = if( javaCompatibility > JavaVersion.VERSION_1_8 ) "4.7.0-9.1.2" else "4.7.0-5"
	implementation( "de.jensd:fontawesomefx-fontawesome:${fontawesomefxVersion}" )
	if( javaCompatibility == JavaVersion.VERSION_1_8 ) {
		// required since Gradle 5.0 because fontawesomefx-fontawesome-4.7.0-5.pom uses
		// scope "runtime" for its "fontawesomefx-commons" dependency
		// (fontawesomefx-fontawesome-4.7.0-9.pom uses scope "compile")
		// https://docs.gradle.org/5.0/userguide/upgrading_version_4.html#rel5.0:pom_compile_runtime_separation
		implementation( "de.jensd:fontawesomefx-commons:8.15" )
	}

	val controlsfxVersion = if( javaCompatibility > JavaVersion.VERSION_1_8 ) "11.1.1" else "8.40.18"
	implementation( "org.controlsfx:controlsfx:${controlsfxVersion}" )
	implementation("org.fxmisc.cssfx:cssfx:11.2.2")
	implementation("org.apache.commons:commons-lang3:3.12.0")
	implementation( "com.esotericsoftware.yamlbeans:yamlbeans:1.15" )
	implementation("org.languagetool:language-en:5.5")

	val flexmarkVersion = "0.62.2"
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

	val commonmarkVersion = "0.18.1"
	implementation( "org.commonmark:commonmark:${commonmarkVersion}" )
	implementation( "org.commonmark:commonmark-ext-autolink:${commonmarkVersion}" )
	implementation( "org.commonmark:commonmark-ext-gfm-strikethrough:${commonmarkVersion}" )
	implementation( "org.commonmark:commonmark-ext-gfm-tables:${commonmarkVersion}" )
	implementation( "org.commonmark:commonmark-ext-heading-anchor:${commonmarkVersion}" )
	implementation( "org.commonmark:commonmark-ext-ins:${commonmarkVersion}" )
	implementation( "org.commonmark:commonmark-ext-yaml-front-matter:${commonmarkVersion}" )

	println("javaCompatibility version is $javaCompatibility")
	if( javaCompatibility >= JavaVersion.VERSION_11 ) {
		val javafxVersion = when( javaCompatibility ) {
			JavaVersion.VERSION_11 -> "11.0.2"
			JavaVersion.VERSION_15 -> "15.0.1"
			else -> throw RuntimeException( "JavaFX 8, 11 or 15 required (running ${javaCompatibility})" )
		}
		val osName = System.getProperty( "os.name" ).toLowerCase()
		val platform = when {
			osName.startsWith("windows") -> "win"
			osName.startsWith("mac") -> "mac"
			else -> "linux"
		}
		implementation( "org.openjfx:javafx-base:${javafxVersion}:${platform}" )
		implementation( "org.openjfx:javafx-controls:${javafxVersion}:${platform}" )
		implementation( "org.openjfx:javafx-graphics:${javafxVersion}:${platform}" )
		implementation( "org.openjfx:javafx-web:${javafxVersion}:${platform}" )
		implementation( "org.openjfx:javafx-media:${javafxVersion}:${platform}" )
	}

	// https://mvnrepository.com/artifact/org.slf4j/slf4j-api
	implementation("org.slf4j:slf4j-api:1.7.32")
	implementation("org.apache.logging.log4j:log4j:2.17.0")
	implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.16.0")


	testImplementation("junit:junit:4.13.2")
}

java {
	sourceCompatibility = javaCompatibility
	targetCompatibility = javaCompatibility
}

application {
	mainClass.set( "org.markdownwriterfx.Main" )
}

val jar: Jar by tasks
jar.manifest {
	attributes( mapOf(
		"Main-Class" to "org.markdownwriterfx.Main",
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
