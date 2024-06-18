import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Needs to be installed in the local maven repository or have the bootstrap jar on the classpath
    id("org.jetbrains.kotlinx.spark.api")
    kotlin("jvm")
    application
    shadow
}

// run with `./gradlew run`
application {
    mainClass = "org.jetbrains.kotlinx.spark.examples.MainKt"

    // workaround for java 17
    applicationDefaultJvmArgs = listOf("--add-opens", "java.base/java.nio=ALL-UNNAMED")
}

kotlinSparkApi {
    enabled = true
    sparkifyAnnotationFqNames = listOf("org.jetbrains.kotlinx.spark.api.plugin.annotations.Sparkify")
}

group = Versions.groupID
version = Versions.project

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    Projects {
        implementation(
            kotlinSparkApi,
        )
    }

    Dependencies {

//        implementation(hadoopClient)

        // IMPORTANT!
        compileOnly(sparkSqlApi)
        implementation(sparkConnectClient)

        implementation(kotlinDateTime)

        compileOnly(scalaLibrary)
    }
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(17)//Versions.jvmLanguageVersion
    }
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(Versions.jvmTarget)
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
}
