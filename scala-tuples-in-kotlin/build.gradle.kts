@file:Suppress("UnstableApiUsage")

import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.KotlinJvm
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    scala
    kotlin
    dokka
    mavenPublishBase
}

group = Versions.groupID
version = Versions.project

repositories {
    mavenCentral()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxHeapSize = "4g"
}

dependencies {
    Dependencies {
        implementation(
            kotlinStdLib,
        )
        testImplementation(
            kotest,
            atrium,
            kotlinTest,
        )
        compileOnly(scalaLibrary)
        testCompileOnly(scalaLibrary)
    }
}

kotlin {
    jvmToolchain {
        languageVersion = Versions.jvmLanguageVersion
    }
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(Versions.jvmTarget)
    }
}

tasks.withType<AbstractDokkaLeafTask> {
    dokkaSourceSets {
        all {
            sourceRoot(
                kotlin.sourceSets
                    .main.get()
                    .kotlin
                    .srcDirs
                    .first { it.path.endsWith("kotlin") }
            )
        }
    }
}

mavenPublishing {
    configure(KotlinJvm(Dokka("dokkaHtml")))
}


// Publishing of scala-tuples-in-kotlin can be skipped since it's only dependent on the Scala version
val skipScalaOnlyDependent = System.getProperty("skipScalaOnlyDependent").toBoolean()
tasks
    .filter { "publish" in it.name }
    .forEach { it.onlyIf { !skipScalaOnlyDependent } }

