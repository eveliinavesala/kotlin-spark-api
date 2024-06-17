package org.jetbrains.kotlinx.spark.api.udts

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.toKotlinDatePeriod
import org.apache.spark.sql.types.UserDefinedType
import org.apache.spark.sql.types.YearMonthIntervalType
import java.time.Period

/**
 * NOTE: Just like java.time.DatePeriod, this is truncated to months.
 */
class DatePeriodUdt : UserDefinedType<DatePeriod>() {
    override fun userClass(): Class<DatePeriod> = DatePeriod::class.java

    override fun deserialize(datum: Any?): DatePeriod? =
        when (datum) {
            null -> null
            is Int -> Period.ofMonths(datum).toKotlinDatePeriod()
            else -> throw IllegalArgumentException("Unsupported datum: $datum")
        }

    override fun serialize(obj: DatePeriod?): Int? =
        obj?.let {
            it.years * 12 + it.months
        }

    override fun sqlType(): YearMonthIntervalType = YearMonthIntervalType.apply()
}
