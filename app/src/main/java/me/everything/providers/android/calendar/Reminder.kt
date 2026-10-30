package me.everything.providers.android.calendar

import android.net.Uri
import android.provider.BaseColumns
import android.provider.CalendarContract.Reminders
import me.everything.providers.core.Entity
import me.everything.providers.core.EnumInt
import me.everything.providers.core.FieldMapping
import me.everything.providers.core.IgnoreMapping

/**
 * Reminder entity
 *
 * @author sromku
 * // @see http
 * ://developer.android.com/reference/android/provider/CalendarContract.
 * Reminders.html
 */
class Reminder : Entity() {
    @FieldMapping(columnName = BaseColumns._ID, physicalType = FieldMapping.PhysicalType.Long)
    var id: Long = 0

    @FieldMapping(columnName = Reminders.EVENT_ID, physicalType = FieldMapping.PhysicalType.Long)
    var eventId: Long = 0

    @FieldMapping(columnName = Reminders.MINUTES, physicalType = FieldMapping.PhysicalType.Int)
    var minutes: Int = 0

    @FieldMapping(
        columnName = Reminders.METHOD,
        physicalType = FieldMapping.PhysicalType.Int,
        logicalType = FieldMapping.LogicalType.EnumInt
    )
    var method: MethodType? = null

    enum class MethodType(var `val`: Int) : EnumInt {
        DEFAULT(0),
        ALERT(1),
        EMAIL(2),
        SMS(3),
        ALARM(4);

        companion object {
            fun fromVal(`val`: Int): MethodType {
                for (methodType in MethodType.entries) {
                    if (methodType.`val` == `val`) {
                        return methodType
                    }
                }
                return MethodType.DEFAULT
            }
        }
    }

    companion object {
        @IgnoreMapping
        var uri: Uri? = Reminders.CONTENT_URI
    }
}