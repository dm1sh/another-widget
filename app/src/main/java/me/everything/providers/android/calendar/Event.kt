package me.everything.providers.android.calendar

import android.annotation.TargetApi
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.CalendarContract
import me.everything.providers.core.FieldMapping
import me.everything.providers.core.IgnoreMapping
import me.everything.providers.core.Entity

/**
 * Event entity
 *
 * @author sromku
 * // @see http
 * ://developer.android.com/reference/android/provider/CalendarContract.
 * Events.html
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class Event : Entity() {
    @FieldMapping(columnName = BaseColumns._ID, physicalType = FieldMapping.PhysicalType.Long)
    var id: Long = 0

    @FieldMapping(
        columnName = CalendarContract.Events.ALLOWED_REMINDERS,
        physicalType = FieldMapping.PhysicalType.String
    )
    var allowedReminders: String? = null

    @FieldMapping(
        columnName = CalendarContract.Events.CALENDAR_ACCESS_LEVEL,
        physicalType = FieldMapping.PhysicalType.Int
    )
    var calendarAccessLevel: Int = 0

    @FieldMapping(
        columnName = CalendarContract.Events.CALENDAR_COLOR,
        physicalType = FieldMapping.PhysicalType.Int
    )
    var calendarColor: Int = 0

    @FieldMapping(
        columnName = CalendarContract.Events.CALENDAR_DISPLAY_NAME,
        physicalType = FieldMapping.PhysicalType.String
    )
    var displayName: String? = null

    @FieldMapping(
        columnName = CalendarContract.Events.CALENDAR_TIME_ZONE,
        physicalType = FieldMapping.PhysicalType.String
    )
    var calendarTimeZone: String? = null

    @FieldMapping(
        columnName = CalendarContract.Events.CAN_MODIFY_TIME_ZONE,
        physicalType = FieldMapping.PhysicalType.Int,
        logicalType = FieldMapping.LogicalType.Boolean
    )
    var canModifyTimeZone: Boolean = false

    @FieldMapping(
        columnName = CalendarContract.Events.CAN_ORGANIZER_RESPOND,
        physicalType = FieldMapping.PhysicalType.Int,
        logicalType = FieldMapping.LogicalType.Boolean
    )
    var canOrginizerRespond: Boolean = false

    @FieldMapping(
        columnName = CalendarContract.Events.MAX_REMINDERS,
        physicalType = FieldMapping.PhysicalType.Int
    )
    var maxReminders: Int = 0

    @FieldMapping(
        columnName = CalendarContract.Events.OWNER_ACCOUNT,
        physicalType = FieldMapping.PhysicalType.String
    )
    var ownerAccount: String? = null

    @FieldMapping(
        columnName = CalendarContract.Events.VISIBLE,
        physicalType = FieldMapping.PhysicalType.Int,
        logicalType = FieldMapping.LogicalType.Boolean
    )
    var visible: Boolean = false

    @FieldMapping(
        columnName = CalendarContract.Events.ACCOUNT_NAME,
        physicalType = FieldMapping.PhysicalType.String
    )
    var accountName: String? = null

    @FieldMapping(
        columnName = CalendarContract.Events.ACCOUNT_TYPE,
        physicalType = FieldMapping.PhysicalType.String
    )
    var accountType: String? = null

    @FieldMapping(
        columnName = CalendarContract.Events.DELETED,
        physicalType = FieldMapping.PhysicalType.Int,
        logicalType = FieldMapping.LogicalType.Boolean
    )
    var deleted: Boolean = false

    @FieldMapping(
        columnName = CalendarContract.Events._SYNC_ID,
        physicalType = FieldMapping.PhysicalType.String
    )
    var syncId: String? = null

    /*
     * --- Event specific fields ---
     */
    @FieldMapping(
        columnName = CalendarContract.Events.ACCESS_LEVEL,
        physicalType = FieldMapping.PhysicalType.Int
    )
    var accessLevel: Int = 0

    @FieldMapping(
        columnName = CalendarContract.Events.ALL_DAY,
        physicalType = FieldMapping.PhysicalType.Int,
        logicalType = FieldMapping.LogicalType.Boolean
    )
    var allDay: Boolean = false

    @FieldMapping(
        columnName = CalendarContract.Events.AVAILABILITY,
        physicalType = FieldMapping.PhysicalType.Int
    )
    var availability: Int = 0

    @FieldMapping(
        columnName = CalendarContract.Events.CALENDAR_ID,
        physicalType = FieldMapping.PhysicalType.Long
    )
    var calendarId: Long = 0

    @FieldMapping(
        columnName = CalendarContract.Events.DESCRIPTION,
        canUpdate = true,
        physicalType = FieldMapping.PhysicalType.String
    )
    var description: String? = null

    @FieldMapping(
        columnName = CalendarContract.Events.DTEND,
        physicalType = FieldMapping.PhysicalType.Long
    )
    var dTend: Long = 0

    @FieldMapping(
        columnName = CalendarContract.Events.DTSTART,
        physicalType = FieldMapping.PhysicalType.Long
    )
    var dTStart: Long = 0

    @FieldMapping(
        columnName = CalendarContract.Events.DURATION,
        physicalType = FieldMapping.PhysicalType.String
    )
    var duration: String? = null

    @FieldMapping(
        columnName = CalendarContract.Events.EVENT_COLOR,
        canUpdate = true,
        physicalType = FieldMapping.PhysicalType.Int
    )
    var eventColor: Int = 0

    @FieldMapping(
        columnName = CalendarContract.Events.EVENT_END_TIMEZONE,
        physicalType = FieldMapping.PhysicalType.String
    )
    var eventEndTimeZone: String? = null

    @FieldMapping(
        columnName = CalendarContract.Events.EVENT_LOCATION,
        canUpdate = true,
        physicalType = FieldMapping.PhysicalType.String
    )
    var eventLocation: String? = null

    @FieldMapping(
        columnName = CalendarContract.Events.EVENT_TIMEZONE,
        physicalType = FieldMapping.PhysicalType.String
    )
    var eventTimeZone: String? = null

    @FieldMapping(
        columnName = CalendarContract.Events.EXDATE,
        physicalType = FieldMapping.PhysicalType.String
    )
    var eventExDate: String? = null

    @FieldMapping(
        columnName = CalendarContract.Events.EXRULE,
        physicalType = FieldMapping.PhysicalType.String
    )
    var eventExRule: String? = null

    @FieldMapping(
        columnName = CalendarContract.Events.GUESTS_CAN_INVITE_OTHERS,
        physicalType = FieldMapping.PhysicalType.Int
    )
    var guestCanInviteOthers: Int = 0

    @FieldMapping(
        columnName = CalendarContract.Events.GUESTS_CAN_MODIFY,
        physicalType = FieldMapping.PhysicalType.Int,
        logicalType = FieldMapping.LogicalType.Boolean
    )
    var guestCanModify: Boolean = false

    @FieldMapping(
        columnName = CalendarContract.Events.GUESTS_CAN_SEE_GUESTS,
        physicalType = FieldMapping.PhysicalType.Int,
        logicalType = FieldMapping.LogicalType.Boolean
    )
    var guestCanSeeQuests: Boolean = false

    @FieldMapping(
        columnName = CalendarContract.Events.HAS_ALARM,
        physicalType = FieldMapping.PhysicalType.Int,
        logicalType = FieldMapping.LogicalType.Boolean
    )
    var hasAlarm: Boolean = false

    @FieldMapping(
        columnName = CalendarContract.Events.HAS_ATTENDEE_DATA,
        physicalType = FieldMapping.PhysicalType.Int,
        logicalType = FieldMapping.LogicalType.Boolean
    )
    var hasAttendeeData: Boolean = false

    @FieldMapping(
        columnName = CalendarContract.Events.HAS_EXTENDED_PROPERTIES,
        physicalType = FieldMapping.PhysicalType.Int,
        logicalType = FieldMapping.LogicalType.Boolean
    )
    var hasExtendedProperties: Boolean = false

    @FieldMapping(
        columnName = CalendarContract.Events.LAST_DATE,
        physicalType = FieldMapping.PhysicalType.Long
    )
    var lastDate: Long = 0

    @FieldMapping(
        columnName = CalendarContract.Events.ORGANIZER,
        physicalType = FieldMapping.PhysicalType.String
    )
    var organizer: String? = null

    @FieldMapping(
        columnName = CalendarContract.Events.ORIGINAL_ALL_DAY,
        physicalType = FieldMapping.PhysicalType.Int,
        logicalType = FieldMapping.LogicalType.Boolean
    )
    var originalAllDay: Boolean = false

    @FieldMapping(
        columnName = CalendarContract.Events.ORIGINAL_ID,
        physicalType = FieldMapping.PhysicalType.String
    )
    var originalId: String? = null

    @FieldMapping(
        columnName = CalendarContract.Events.ORIGINAL_INSTANCE_TIME,
        physicalType = FieldMapping.PhysicalType.Long
    )
    var originalInstanceTime: Long = 0

    @FieldMapping(
        columnName = CalendarContract.Events.ORIGINAL_SYNC_ID,
        physicalType = FieldMapping.PhysicalType.String
    )
    var originalSyncId: String? = null

    @FieldMapping(
        columnName = CalendarContract.Events.RDATE,
        physicalType = FieldMapping.PhysicalType.String
    )
    var rDate: String? = null

    @FieldMapping(
        columnName = CalendarContract.Events.RRULE,
        physicalType = FieldMapping.PhysicalType.String
    )
    var rRule: String? = null

    @FieldMapping(
        columnName = CalendarContract.Events.SELF_ATTENDEE_STATUS,
        physicalType = FieldMapping.PhysicalType.String
    )
    var selfAttendeeStatus: String? = null

    @FieldMapping(
        columnName = CalendarContract.Events.STATUS,
        physicalType = FieldMapping.PhysicalType.String
    )
    var status: String? = null

    @FieldMapping(
        columnName = CalendarContract.Events.TITLE,
        canUpdate = true,
        physicalType = FieldMapping.PhysicalType.String
    )
    var title: String? = null

    companion object {
        @IgnoreMapping
        var uri: Uri? = CalendarContract.Events.CONTENT_URI
    }
}