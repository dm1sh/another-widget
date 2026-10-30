package me.everything.providers.android.calendar

import android.net.Uri
import android.provider.BaseColumns
import android.provider.CalendarContract
import me.everything.providers.core.Entity
import me.everything.providers.core.FieldMapping
import me.everything.providers.core.IgnoreMapping

/**
 * Instance entity
 *
 * @author sromku
 * // @see http
 * ://developer.android.com/reference/android/provider/CalendarContract.
 * Instances.html
 */
class Instance : Entity() {
    @FieldMapping(columnName = BaseColumns._ID, physicalType = FieldMapping.PhysicalType.Long)
    var id: Long = 0

    @FieldMapping(
        columnName = CalendarContract.Instances.EVENT_ID,
        physicalType = FieldMapping.PhysicalType.Long
    )
    var eventId: Long = 0

    @FieldMapping(
        columnName = CalendarContract.Instances.BEGIN,
        physicalType = FieldMapping.PhysicalType.Long
    )
    var begin: Long = 0

    @FieldMapping(
        columnName = CalendarContract.Instances.END,
        physicalType = FieldMapping.PhysicalType.Long
    )
    var end: Long = 0

    @FieldMapping(
        columnName = CalendarContract.Instances.START_DAY,
        physicalType = FieldMapping.PhysicalType.Int
    )
    var startDay: Long = 0

    @FieldMapping(
        columnName = CalendarContract.Instances.START_MINUTE,
        physicalType = FieldMapping.PhysicalType.Int
    )
    var startMinute: Long = 0

    @FieldMapping(
        columnName = CalendarContract.Instances.END_DAY,
        physicalType = FieldMapping.PhysicalType.Int
    )
    var endDay: Long = 0

    @FieldMapping(
        columnName = CalendarContract.Instances.END_MINUTE,
        physicalType = FieldMapping.PhysicalType.Int
    )
    var endMinute: Long = 0

    companion object {
        @IgnoreMapping
        var uri: Uri? = CalendarContract.Instances.CONTENT_URI
    }
}