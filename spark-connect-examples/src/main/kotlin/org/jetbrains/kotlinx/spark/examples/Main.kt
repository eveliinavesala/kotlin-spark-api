package org.jetbrains.kotlinx.spark.examples

import kotlinx.datetime.Clock
import org.apache.spark.sql.connect.client.REPLClassDirMonitor
import org.jetbrains.kotlinx.spark.api.plugin.annotations.Sparkify
import org.jetbrains.kotlinx.spark.api.showDS
import org.jetbrains.kotlinx.spark.api.toList
import org.jetbrains.kotlinx.spark.api.tuples.X
import org.jetbrains.kotlinx.spark.api.withSparkConnect
import scala.Tuple2

// run with `./gradlew runShadow` or set VM options: "--add-opens=java.base/java.nio=ALL-UNNAMED" in the IDE
fun main() =
    withSparkConnect("sc://localhost") {
        val classFinder = REPLClassDirMonitor("/mnt/data/Projects/kotlin-spark-api/spark-connect-examples/build/classes")
        spark.registerClassFinder(classFinder)
        // make jar first, preferably a fat jar with shadow, but be careful it doesn't contain scala depencencies
        spark.addArtifact("/mnt/data/Projects/kotlin-spark-api/spark-connect-examples/build/libs/spark-connect-examples-2.0.0-SNAPSHOT-all.jar")

        val data =
            listOf(
                Person("Alice", 25, (Clock.System.now()), "Alice" X Address("1 Main St", "Springfield", "IL", 62701)),
                Person("Bob", 30, (Clock.System.now()), "Bob" X Address("2 Main St", "Springfield", "IL", 62701)),
                Person(
                    "Charlie",
                    35,
                    (Clock.System.now()),
                    "Charlie" X Address("3 Main St", "Springfield", "IL", 62701),
                ),
            )

        val ds = data.toDS().showDS()



        ds
            .filter { it.age > 26 }
            .toList<Person>()
            .forEach {
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
    val birthDate: kotlinx.datetime.Instant,
    val tuple: Tuple2<String, Address>,
)
//
//class InstantUdt : UserDefinedType<kotlinx.datetime.Instant>() {
//    override fun userClass(): Class<kotlinx.datetime.Instant> = kotlinx.datetime.Instant::class.java
//
//    override fun deserialize(datum: Any?): kotlinx.datetime.Instant? =
//        when (datum) {
//            null -> null
//            is Long -> kotlinx.datetime.Instant.fromEpochMilliseconds(datum.microseconds.inWholeMilliseconds)
//
//            else -> throw IllegalArgumentException("Unsupported datum: $datum")
//        }
//
//    override fun serialize(obj: kotlinx.datetime.Instant?): Long? =
//        obj?.toEpochMilliseconds()?.milliseconds?.inWholeMicroseconds
//
//    override fun sqlType(): DataType = InternalRow
//}