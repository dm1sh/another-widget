package me.everything.providers.android.calendar

import android.net.Uri
import android.provider.BaseColumns
import android.provider.CalendarContract.Attendees
import me.everything.providers.core.Entity
import me.everything.providers.core.FieldMapping
import me.everything.providers.core.IgnoreMapping

/**
 * Attendee entity
 *
 * @author sromku
 * // @see http
 * ://developer.android.com/reference/android/provider/CalendarContract.
 * Attendees.html
 */
class Attendee : Entity() {
    @FieldMapping(columnName = BaseColumns._ID, physicalType = FieldMapping.PhysicalType.Long)
    var id: Long = 0

    @FieldMapping(columnName = Attendees.EVENT_ID, physicalType = FieldMapping.PhysicalType.Long)
    var eventId: Long = 0

    @FieldMapping(
        columnName = Attendees.ATTENDEE_NAME,
        physicalType = FieldMapping.PhysicalType.String
    )
    var name: String? = null

    @FieldMapping(
        columnName = Attendees.ATTENDEE_EMAIL,
        physicalType = FieldMapping.PhysicalType.String
    )
    var email: String? = null

    @FieldMapping(
        columnName = Attendees.ATTENDEE_RELATIONSHIP,
        physicalType = FieldMapping.PhysicalType.Int
    )
    var relationship: Int = 0

    @FieldMapping(
        columnName = Attendees.ATTENDEE_TYPE,
        physicalType = FieldMapping.PhysicalType.Int
    )
    var type: Int = 0

    @FieldMapping(
        columnName = Attendees.ATTENDEE_STATUS,
        physicalType = FieldMapping.PhysicalType.String
    )
    var status: String? = null

    companion object {
        @IgnoreMapping
        var uri: Uri? = Attendees.CONTENT_URI
    }
}
