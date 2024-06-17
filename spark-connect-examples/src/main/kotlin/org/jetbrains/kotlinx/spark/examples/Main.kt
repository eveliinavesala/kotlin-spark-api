package org.jetbrains.kotlinx.spark.examples

import org.apache.spark.sql.connect.client.REPLClassDirMonitor
import org.jetbrains.kotlinx.spark.api.plugin.annotations.Sparkify
import org.jetbrains.kotlinx.spark.api.showDS
import org.jetbrains.kotlinx.spark.api.toList
import org.jetbrains.kotlinx.spark.api.tuples.X
import org.jetbrains.kotlinx.spark.api.tuples.t
import org.jetbrains.kotlinx.spark.api.withSparkConnect
import scala.Tuple2
import java.time.LocalDate

// run with `./gradlew run` or set VM options: "--add-opens=java.base/java.nio=ALL-UNNAMED" in the IDE
fun main() =
    withSparkConnect("sc://localhost") {
        val classFinder = REPLClassDirMonitor("/mnt/data/Projects/kotlin-spark-api/spark-connect-examples/build/classes")
        spark.registerClassFinder(classFinder)
        spark.addArtifact("/mnt/data/Projects/kotlin-spark-api/spark-connect-examples/build/libs/spark-connect-examples-2.0.0-SNAPSHOT.jar")

        val data =
            listOf(
                Person("Alice", 25, LocalDate.of(1996, 1, 1), "Alice" X Address("1 Main St", "Springfield", "IL", 62701)),
                Person("Bob", 30, LocalDate.of(1991, 1, 1), "Bob" X Address("2 Main St", "Springfield", "IL", 62701)),
                Person("Charlie", 35, LocalDate.of(1986, 1, 1), "Charlie" X Address("3 Main St", "Springfield", "IL", 62701)),
            )

        val ds = data.toDS().showDS()

        ds.toList<Person>().forEach {
            println(it)
        }
    }

@Sparkify
data class Address(
    val street: String,
    val city: String,
    val state: String,
    val zip: Int,
)

@Sparkify
data class Person(
    val name: String,
    val age: Int,
    val birthDate: LocalDate,
    val tuple: Tuple2<String, Address>,
)
