package org.jetbrains.kotlinx.spark.api.udts

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.apache.spark.sql.types.DataType
import org.apache.spark.sql.types.`TimestampNTZType$`
import org.apache.spark.sql.types.UserDefinedType
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds


class LocalDateTimeUdt : UserDefinedType<LocalDateTime>() {

    override fun userClass(): Class<LocalDateTime> = LocalDateTime::class.java
    override fun deserialize(datum: Any?): LocalDateTime? =
        when (datum) {
            null -> null
            is Long ->
                Instant.fromEpochMilliseconds(datum.microseconds.inWholeMilliseconds)
                    .toLocalDateTime(TimeZone.UTC)

            else -> throw IllegalArgumentException("Unsupported datum: $datum")
        }

    override fun serialize(obj: LocalDateTime?): Long? =
        obj?.toInstant(TimeZone.UTC)?.toEpochMilliseconds()?.milliseconds?.inWholeMicroseconds

    override fun sqlType(): DataType = `TimestampNTZType$`.`MODULE$`
}