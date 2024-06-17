/*-
 * =LICENSE=
 * Kotlin Spark API
 * ----------
 * Copyright (C) 2019 - 2020 JetBrains
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

/**
 * This file contains the main entry points and wrappers for the Kotlin Spark API.
 */

@file:Suppress("UsePropertyAccessSyntax")

package org.jetbrains.kotlinx.spark.api

import org.apache.hadoop.conf.Configuration
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.Row
import org.apache.spark.sql.SparkSession.Builder
import org.apache.spark.sql.UDFRegistration
import org.jetbrains.kotlinx.spark.api.tuples.*

/**
 * This wrapper over [SparkSession] which provides several additional methods to create [org.apache.spark.sql.Dataset].
 *
 *  @param spark The current [SparkSession] to wrap
 */
class KSparkSession(
    val spark: SparkSession,
) {
    val sparkVersion = /*$"\""+spark+"\""$*/ /*-*/ "nope"

    /** Utility method to create dataset from list. */
    inline fun <reified T> List<T>.toDS(): Dataset<T> = toDS(spark)

    /** Utility method to create dataframe from list. */
    inline fun <reified T> List<T>.toDF(vararg colNames: String): Dataset<Row> = toDF(spark, *colNames)

    /** Utility method to create dataset from [Array]. */
    inline fun <reified T> Array<T>.toDS(): Dataset<T> = toDS(spark)

    /** Utility method to create dataframe from [Array]. */
    inline fun <reified T> Array<T>.toDF(vararg colNames: String): Dataset<Row> = toDF(spark, *colNames)

    /** Utility method to create dataset from vararg arguments. */
    inline fun <reified T> dsOf(vararg arg: T): Dataset<T> = spark.dsOf(*arg)

    /** Creates new empty dataset of type [T]. */
    inline fun <reified T> emptyDataset(): Dataset<T> = spark.emptyDataset(kotlinEncoderFor<T>())

    /** Utility method to create dataframe from *array or vararg arguments */
    inline fun <reified T> dfOf(vararg arg: T): Dataset<Row> = spark.dfOf(*arg)

    /**Utility method to create dataframe from *array or vararg arguments with given column names */
    inline fun <reified T> dfOf(
        colNames: Array<String>,
        vararg arg: T,
    ): Dataset<Row> = spark.dfOf(colNames, *arg)

    /**
     * A collection of methods for registering user-defined functions (UDF).
     *
     * The following example registers a UDF in Kotlin:
     * ```Kotlin
     *   sparkSession.udf.register("myUDF") { arg1: Int, arg2: String -> arg2 + arg1 }
     * ```
     *
     * @note The user-defined functions must be deterministic. Due to optimization,
     * duplicate invocations may be eliminated or the function may even be invoked more times than
     * it is present in the query.
     */
    val udf: UDFRegistration get() = spark.udf()

    inline fun <RETURN, reified NAMED_UDF : NamedUserDefinedFunction<RETURN, *>> NAMED_UDF.register(): NAMED_UDF =
        this@KSparkSession.udf.register(namedUdf = this)

    inline fun <RETURN, reified NAMED_UDF : NamedUserDefinedFunction<RETURN, *>> UserDefinedFunction<RETURN, NAMED_UDF>.register(
        name: String,
    ): NAMED_UDF = this@KSparkSession.udf.register(name = name, udf = this)
}

/**
 * The entry point to programming Spark with the Dataset and DataFrame API.
 *
 * @see org.apache.spark.sql.SparkSession
 */
typealias SparkSession = org.apache.spark.sql.SparkSession

/** Log levels for spark. */
enum class SparkLogLevel {
    ALL,
    DEBUG,
    ERROR,
    FATAL,
    INFO,
    OFF,
    TRACE,
    WARN,
}

/**
 * Wrapper for spark creation which allows setting different spark params.
 *
 * @param props spark options, value types are runtime-checked for type-correctness
 * @param master Sets the Spark master URL to connect to, such as "local" to run locally, "local[4]" to
 *  run locally with 4 cores, or "spark://master:7077" to run on a Spark standalone cluster. By default, it
 *  tries to get the system value "spark.master", otherwise it uses "local[*]"
 * @param appName Sets a name for the application, which will be shown in the Spark web UI.
 *  If no application name is set, a randomly generated name will be used.
 * @param logLevel Control our logLevel. This overrides any user-defined log settings.
 * @param func function which will be executed in context of [KSparkSession] (it means that `this` inside block will point to [KSparkSession])
 */
@JvmOverloads
inline fun withSparkConnect(
    remote: String = "sc://localhost",
    props: Map<String, Any> = emptyMap(),
    func: KSparkSession.() -> Unit,
) {
    val defaultArgs = mapOf(
        "spark.sql.legacy.allowUntypedScalaUDF" to true,
    )

    val builder =
        SparkSession
            .builder()
            .remote(remote)
            .apply {
                (defaultArgs + props).forEach {
                    when (val value = it.value) {
                        is String -> config(it.key, value)
                        is Boolean -> config(it.key, value)
                        is Long -> config(it.key, value)
                        is Double -> config(it.key, value)
                        else ->
                            throw IllegalArgumentException(
                                "Cannot set property ${it.key} because value $value of unsupported type ${value::class}",
                            )
                    }
                }
            }
    withSparkConnect(builder, func)
}

/**
 * Wrapper for spark creation which allows setting different spark params.
 *
 * @param builder A [SparkSession.Builder] object, configured how you want.
 * @param logLevel Control our logLevel. This overrides any user-defined log settings.
 * @param func function which will be executed in context of [KSparkSession] (it means that `this` inside block will point to [KSparkSession])
 */
inline fun withSparkConnect(
    builder: Builder,
    func: KSparkSession.() -> Unit,
) {
    builder
        .getOrCreate()
        .apply {
            KSparkSession(this).apply {
                func()
                spark.stop()
            }
        }
}

// calling org.apache.spark.deploy.`SparkHadoopUtil$`.`MODULE$`.get().conf()
private fun getDefaultHadoopConf(): Configuration {
    val klass = Class.forName("org.apache.spark.deploy.SparkHadoopUtil$")
    val moduleField = klass.getField("MODULE$").also { it.isAccessible = true }
    val module = moduleField.get(null)
    val getMethod = klass.getMethod("get").also { it.isAccessible = true }
    val sparkHadoopUtil = getMethod.invoke(module)
    val confMethod = sparkHadoopUtil.javaClass.getMethod("conf").also { it.isAccessible = true }
    val conf = confMethod.invoke(sparkHadoopUtil) as Configuration

    return conf
}
