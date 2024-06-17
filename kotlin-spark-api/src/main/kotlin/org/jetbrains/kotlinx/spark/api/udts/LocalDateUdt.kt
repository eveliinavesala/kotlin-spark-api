package org.jetbrains.kotlinx.spark.api.udts

import kotlinx.datetime.LocalDate
import org.apache.spark.sql.types.DataType
import org.apache.spark.sql.types.`DateType$`
import org.apache.spark.sql.types.UserDefinedType

class LocalDateUdt : UserDefinedType<LocalDate>() {
    override fun userClass(): Class<LocalDate> = LocalDate::class.java

    override fun deserialize(datum: Any?): LocalDate? =
        when (datum) {
            null -> null
            is Int -> LocalDate.fromEpochDays(datum)
            else -> throw IllegalArgumentException("Unsupported datum: $datum")
        }

    override fun serialize(obj: LocalDate?): Int? = obj?.toEpochDays()

    override fun sqlType(): DataType = `DateType$`.`MODULE$`
}
