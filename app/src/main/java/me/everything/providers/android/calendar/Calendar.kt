package me.everything.providers.android.calendar

import android.net.Uri
import android.provider.BaseColumns
import android.provider.CalendarContract.Calendars
import me.everything.providers.core.Entity
import me.everything.providers.core.FieldMapping
import me.everything.providers.core.IgnoreMapping


class Calendar : Entity() {
    @FieldMapping(columnName = BaseColumns._ID, physicalType = FieldMapping.PhysicalType.Long)
    var id: Long = 0

    @FieldMapping(columnName = Calendars.NAME, physicalType = FieldMapping.PhysicalType.String)
    var name: String? = null

    @FieldMapping(
        columnName = Calendars.ALLOWED_REMINDERS,
        physicalType = FieldMapping.PhysicalType.String
    )
    var allowedReminders: String? = null

    @FieldMapping(
        columnName = Calendars.CALENDAR_ACCESS_LEVEL,
        physicalType = FieldMapping.PhysicalType.Int
    )
    var calendarAccessLevel: Int = 0

    @FieldMapping(
        columnName = Calendars.CALENDAR_COLOR,
        canUpdate = true,
        physicalType = FieldMapping.PhysicalType.Int
    )
    var calendarColor: Int = 0

    @FieldMapping(
        columnName = Calendars.CALENDAR_DISPLAY_NAME,
        canUpdate = true,
        physicalType = FieldMapping.PhysicalType.String
    )
    var displayName: String? = null

    @FieldMapping(
        columnName = Calendars.CALENDAR_TIME_ZONE,
        physicalType = FieldMapping.PhysicalType.String
    )
    var calendarTimeZone: String? = null

    @FieldMapping(
        columnName = Calendars.CAN_MODIFY_TIME_ZONE,
        physicalType = FieldMapping.PhysicalType.Int,
        logicalType = FieldMapping.LogicalType.Boolean
    )
    var canModifyTimeZone: Boolean = false

    @FieldMapping(
        columnName = Calendars.CAN_ORGANIZER_RESPOND,
        physicalType = FieldMapping.PhysicalType.Int,
        logicalType = FieldMapping.LogicalType.Boolean
    )
    var canOrginizerRespond: Boolean = false

    @FieldMapping(
        columnName = Calendars.MAX_REMINDERS,
        physicalType = FieldMapping.PhysicalType.Int
    )
    var maxReminders: Int = 0

    @FieldMapping(
        columnName = Calendars.OWNER_ACCOUNT,
        physicalType = FieldMapping.PhysicalType.String
    )
    var ownerAccount: String? = null

    @FieldMapping(columnName = Calendars.SYNC_EVENTS, physicalType = FieldMapping.PhysicalType.Int)
    var syncEvents: Int = 0

    @FieldMapping(
        columnName = Calendars.VISIBLE,
        physicalType = FieldMapping.PhysicalType.Int,
        logicalType = FieldMapping.LogicalType.Boolean
    )
    var visible: Boolean = false

    @FieldMapping(
        columnName = Calendars.ACCOUNT_NAME,
        physicalType = FieldMapping.PhysicalType.String
    )
    var accountName: String? = null

    @FieldMapping(
        columnName = Calendars.ACCOUNT_TYPE,
        physicalType = FieldMapping.PhysicalType.String
    )
    var accountType: String? = null

    @FieldMapping(
        columnName = Calendars.CAN_PARTIALLY_UPDATE,
        physicalType = FieldMapping.PhysicalType.Int,
        logicalType = FieldMapping.LogicalType.Boolean
    )
    var canPartiallyUpdate: Boolean = false

    @FieldMapping(
        columnName = Calendars.DELETED,
        physicalType = FieldMapping.PhysicalType.Int,
        logicalType = FieldMapping.LogicalType.Boolean
    )
    var deleted: Boolean = false

    @FieldMapping(columnName = Calendars.DIRTY, physicalType = FieldMapping.PhysicalType.Long)
    var dirty: Long = 0

    @FieldMapping(columnName = Calendars._SYNC_ID, physicalType = FieldMapping.PhysicalType.String)
    var syncId: String? = null

    @FieldMapping(
        columnName = Calendars.CALENDAR_LOCATION,
        physicalType = FieldMapping.PhysicalType.String
    )
    var location: String? = null

    @FieldMapping(
        columnName = Calendars.DEFAULT_SORT_ORDER,
        physicalType = FieldMapping.PhysicalType.String
    )
    @IgnoreMapping
    var sortOrder: String? = null

    companion object {
        @IgnoreMapping
        var uri: Uri? = Calendars.CONTENT_URI
    }
}
