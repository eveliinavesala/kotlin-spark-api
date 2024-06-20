/*-
 * =LICENSE=
 * Kotlin Spark API: API for Spark 3.2+ (Scala 2.12)
 * ----------
 * Copyright (C) 2019 - 2022 JetBrains
 * ----------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =LICENSEEND=
 */
@file:Suppress("UsePropertyAccessSyntax")

package org.jetbrains.kotlinx.spark.api.jupyter

import org.apache.spark.sql.connect.client.Artifact
import org.apache.spark.sql.connect.client.`Artifact$`
import org.apache.spark.sql.connect.client.ClassFinder
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlinx.jupyter.api.CodePreprocessor
import org.jetbrains.kotlinx.jupyter.api.KotlinKernelHost
import org.jetbrains.kotlinx.jupyter.api.Notebook
import org.jetbrains.kotlinx.jupyter.api.VariableDeclaration
import org.jetbrains.kotlinx.jupyter.api.declare
import org.jetbrains.kotlinx.spark.api.SparkSession
import org.jetbrains.kotlinx.spark.api.asScalaIterator
import org.jetbrains.kotlinx.spark.api.jupyter.Properties.Companion.remoteName
import org.jetbrains.kotlinx.spark.api.jupyter.Properties.Companion.sparkPropertiesName
import org.jetbrains.kotlinx.spark.api.map
import scala.collection.Iterator
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.reflect.typeOf

/**
 * Spark connect!
 *
 * %use spark
 */
@Suppress("UNUSED_VARIABLE", "LocalVariableName")
class SparkIntegration(notebook: Notebook, options: MutableMap<String, String?>) :
    Integration(notebook, options),
    CodePreprocessor,
    ClassFinder {
    override val usingProperties: Array<String>
        get() = super.usingProperties + remoteName

    override val dependencies: Array<String> =
        arrayOf(
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion",
            "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion",
            "org.apache.spark:spark-sql-api_$scalaCompatVersion:$sparkVersion",
            "org.apache.spark:spark-connect-client-jvm_$scalaCompatVersion:$sparkVersion",
            "org.scala-lang:scala-library:$scalaVersion",
            "org.scala-lang:scala-reflect:$scalaVersion",
            "commons-io:commons-io:2.11.0",
        )

    override val imports: Array<String> =
        arrayOf(
            "org.jetbrains.kotlinx.spark.api.plugin.annotations.*",
            "org.jetbrains.kotlinx.spark.api.*",
            "org.jetbrains.kotlinx.spark.api.tuples.*",
            *(1..22).map { "scala.Tuple$it" }.toTypedArray(),
            "org.apache.spark.sql.functions.*",
            "org.apache.spark.*",
            "org.apache.spark.sql.*",
            "org.apache.spark.api.java.*",
            "scala.collection.Seq",
            "java.io.Serializable",
        )

    private val dumpedClasses = ClassCache()

    override fun findClasses(): Iterator<Artifact> =
        dumpedClasses
            .map {
                try {
                    `Artifact$`.`MODULE$`.newClassArtifact(it.fileName, Artifact.LocalFile(it))
                } catch (e: Exception) {
                    throw RuntimeException("Error while creating class artifact for $it", e)
                }
            }.iterator()
            .asScalaIterator()

    override fun KotlinKernelHost.onLoaded() {
        val _0 = execute("""%dumpClassesForSpark""")

        properties {
            putIfAbsent(remoteName, "sc://localhost")
            putIfAbsent("spark.sql.legacy.allowUntypedScalaUDF", "true")
//            getOrPut("spark.sql.codegen.wholeStage") { "false" }
            putIfAbsent("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem::class.java.name)
            putIfAbsent("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem::class.java.name)
        }

        declare(
            VariableDeclaration(
                name = ::dumpedClasses.name,
                value = dumpedClasses,
                type = typeOf<ClassCache>(),
            ),
        )

        @Language("kts")
        val _1 =
            listOf(
                """
                val spark = org.jetbrains.kotlinx.spark.api.SparkSession
                    .builder()
                    .remote("${properties[remoteName]}")
                    .apply {
                        ${
                    buildString {
                        val sparkProps = properties.filterKeys { it !in usingProperties }
                        println("received properties: $properties, providing Spark with: $sparkProps")

                        sparkProps.forEach { (key, value) ->
                            appendLine("config(\"${key}\", \"$value\")")
                        }
                    }
                }
                     }
                    .getOrCreate()
                """.trimIndent(),
                """
                println("Spark Connect session (Spark: $sparkVersion, Scala: $scalaCompatVersion, v: $version, remote: ${properties[remoteName]}) has been started and is running. No `withSparkConnect { }` necessary, you can access `spark` directly.")
                """.trimIndent(),
                """
                inline fun <reified T> List<T>.toDS(): Dataset<T> = toDS(spark)
                """.trimIndent(),
                """
                inline fun <reified T> List<T>.toDF(vararg colNames: String): Dataset<Row> = toDF(spark, *colNames)
                """.trimIndent(),
                """
                inline fun <reified T> Array<T>.toDS(): Dataset<T> = toDS(spark)
                """.trimIndent(),
                """
                inline fun <reified T> Array<T>.toDF(vararg colNames: String): Dataset<Row> = toDF(spark, *colNames)
                """.trimIndent(),
                """
                inline fun <reified T> dsOf(vararg arg: T): Dataset<T> = spark.dsOf(*arg)
                """.trimIndent(),
                """
                inline fun <reified T> dfOf(vararg arg: T): Dataset<Row> = spark.dfOf(*arg)
                """.trimIndent(),
                """
                inline fun <reified T> emptyDataset(): Dataset<T> = spark.emptyDataset(kotlinEncoderFor<T>())
                """.trimIndent(),
                """
                inline fun <reified T> dfOf(colNames: Array<String>, vararg arg: T): Dataset<Row> = spark.dfOf(colNames, *arg)
                """.trimIndent(),
                """
                val udf: UDFRegistration get() = spark.udf()
                """.trimIndent(),
                """
                inline fun <RETURN, reified NAMED_UDF : NamedUserDefinedFunction<RETURN, *>> NAMED_UDF.register(): NAMED_UDF = spark.udf().register(namedUdf = this)
                """.trimIndent(),
                """
                inline fun <RETURN, reified NAMED_UDF : NamedUserDefinedFunction<RETURN, *>> UserDefinedFunction<RETURN, NAMED_UDF>.register(name: String): NAMED_UDF = spark.udf().register(name = name, udf = this)
                """.trimIndent(),
                """
                /** This function is run automatically at the beginning of each cell to make its .class contents available to Spark. */
                fun dumpClassesToSpark() {
                    val outputFiles = java.io.File(System.getProperty("spark.repl.class.outputDir"))
                            .listFiles { it -> it.extension == "class" }
                            ?.map { it.toPath() }
                    if (outputFiles != null) {
                        dumpedClasses += outputFiles
                        if ($sparkPropertiesName.debug) println("Dumped classes: ${'$'}outputFiles")
                    }
                }
                """.trimIndent(),
            ).map(::execute)

        spark = execute("spark").value as SparkSession

        // Add all jars in the classpath to Spark as artifacts
        buildList {
            var current: ClassLoader? = execute("this::class.java.classLoader").value as ClassLoader
            while (current != null) {
                add(current)
                current = current.parent
            }
        }.filterIsInstance<URLClassLoader>()
            .flatMap { it.getURLs().map { it.path } }
            .filter { it.endsWith(".jar") }
            .forEach {
                try {
                    spark!!.addArtifact(it)
                } catch (e: Exception) {
                    if (properties.debug) println("Error while adding artifact $it: $e")
                }
            }

        spark!!.registerClassFinder(this@SparkIntegration)
        notebook.codePreprocessorsProcessor.register(this@SparkIntegration)
    }

    /**
     * Makes it so that `dumpClassesToSpark()` is run automatically
     * at the beginning of each cell.
     */
    override fun process(
        code: String,
        host: KotlinKernelHost,
    ): CodePreprocessor.Result =
        CodePreprocessor.Result(
            code
                .lines()
                .toMutableList()
                .also {
                    it.add(
                        index = it.indexOfLast { it.startsWith("import ") } + 1,
                        element = "dumpClassesToSpark()",
                    )
                }.joinToString("\n"),
        )
}

/**
 * Spark connect .class cache designed to keep the newest class files (unique by name) with their
 * given path.
 */
class ClassCache : Iterable<Path> {
    private val cache: MutableMap<String, Path> = mutableMapOf()

    fun add(path: Path) {
        require(path.extension == "class") { "Must be a .class file" }
        val name = path.fileName.toString()
        cache[name] = path
    }

    fun addAll(paths: Iterable<Path>) = paths.forEach { add(it) }

    operator fun plusAssign(path: Path) = add(path)

    operator fun plusAssign(path: Iterable<Path>) = addAll(path)

    override fun iterator(): kotlin.collections.Iterator<Path> = cache.values.iterator()
}
