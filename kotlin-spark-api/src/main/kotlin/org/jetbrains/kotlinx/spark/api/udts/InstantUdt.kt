package org.jetbrains.kotlinx.spark.api.udts

import kotlinx.datetime.Instant
import org.apache.spark.sql.types.DataType
import org.apache.spark.sql.types.`TimestampType$`
import org.apache.spark.sql.types.UserDefinedType
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds

class InstantUdt : UserDefinedType<Instant>() {
    override fun userClass(): Class<Instant> = Instant::class.java

    override fun deserialize(datum: Any?): Instant? =
        when (datum) {
            null -> null
            is Long -> Instant.fromEpochMilliseconds(datum.microseconds.inWholeMilliseconds)

            else -> throw IllegalArgumentException("Unsupported datum: $datum")
        }

    override fun serialize(obj: Instant?): Long? = obj?.toEpochMilliseconds()?.milliseconds?.inWholeMicroseconds

    override fun sqlType(): DataType = `TimestampType$`.`MODULE$`
}
