package me.everything.providers.android.calendar

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract.Attendees
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Instances
import android.provider.CalendarContract.Reminders
import me.everything.providers.core.AbstractProvider
import me.everything.providers.core.Data


class CalendarProvider(context: Context?) : AbstractProvider(context) {
    val calendars: Data<Calendar?>?
        /**
         * Get calendars.
         *
         * @return List of calendars
         */
        get() {
            val calendars: Data<Calendar?>? =
                getContentTableData(Calendar.uri, Calendar::class.java)
            return calendars
        }

    /**
     * Get specific calendar.
     *
     * @param calendarId
     */
    fun getCalendar(calendarId: Long): Calendar? {
        val selection = "(" + Calendars._ID + " = ?)"
        val selectionArgs: Array<String> = arrayOf(calendarId.toString())
        val calendar: Calendar? =
            getContentRowData(Calendar.uri, selection, selectionArgs, null, Calendar::class.java)
        return calendar
    }

    /**
     * Get events of specific calendar.
     *
     * @param calendarId
     * @return List of events which relates to given calendar.
     */
    fun getEvents(calendarId: Long): Data<Event?>? {
        val selection = "(" + Events.CALENDAR_ID + " = ?)"
        val selectionArgs: Array<String> = arrayOf<String>(calendarId.toString())
        val events: Data<Event?>? =
            getContentTableData(Event.uri, selection, selectionArgs, null, Event::class.java)
        return events
    }

    /**
     * Get events of specific calendar.
     *
     * @param eventId
     * @return List of events which relates to given calendar.
     */
    fun getEvent(eventId: Long): Event? {
        val selection = "(" + Events._ID + " = ?)"
        val selectionArgs: Array<String> = arrayOf<String>(eventId.toString())
        val event: Event? =
            getContentRowData(Event.uri, selection, selectionArgs, null, Event::class.java)
        return event
    }

    /**
     * Get instances from date to date.
     *
     * @param begin - start time, end - end time
     * @return List of instances.
     */
    fun getInstances(begin: Long, end: Long): Data<Instance?>? {
        val builder: Uri.Builder = Instance.uri!!.buildUpon()
        ContentUris.appendId(builder, begin)
        ContentUris.appendId(builder, end)
        val uri = builder.build()
        val instances: Data<Instance?>? = getContentTableData(uri, Instance::class.java)
        return instances
    }

    /**
     * Get instances of given event from date to date.
     *
     * @param eventId
     * @return List of instances.
     */
    fun getInstances(eventId: Long, begin: Long, end: Long): Data<Instance?>? {
        val selection = "(" + Instances.EVENT_ID + " = ?)"
        val selectionArgs: Array<String> = arrayOf<String>(eventId.toString())
        val builder: Uri.Builder = Instance.uri!!.buildUpon()
        ContentUris.appendId(builder, begin)
        ContentUris.appendId(builder, end)
        val uri = builder.build()
        val instances: Data<Instance?>? =
            getContentTableData(uri, selection, selectionArgs, null, Instance::class.java)
        return instances
    }

    /**
     * Get attendees of passed event.
     *
     * @param eventId
     * @return List of attendees.
     */
    fun getAttendees(eventId: Long): Data<Attendee?>? {
        val selection = "(" + Attendees.EVENT_ID + "=?)"
        val selectionArgs: Array<String> = arrayOf<String>(eventId.toString())
        val attendees: Data<Attendee?>? =
            getContentTableData(Attendee.uri, selection, selectionArgs, null, Attendee::class.java)
        return attendees
    }

    /**
     * Get reminders of given event.
     *
     * @param eventId
     * @return List of reminders.
     */
    fun getReminders(eventId: Long): Data<Reminder?>? {
        val selection = "(" + Reminders.EVENT_ID + "=?)"
        val selectionArgs: Array<String> = arrayOf<String>(eventId.toString())
        val reminders: Data<Reminder?>? =
            getContentTableData(Reminder.uri, selection, selectionArgs, null, Reminder::class.java)
        return reminders
    }

    /**
     * Update calendar.
     *
     * @param calendar
     * @return number of updated rows
     */
    fun update(calendar: Calendar?): Int {
        return updateTableRow(Calendar.uri, calendar)
    }

    /**
     * Update event
     *
     * @param event
     * @return number of updated rows
     */
    fun update(event: Event?): Int {
        return updateTableRow(Event.uri, event)
    }

    /**
     * Update instance
     *
     * @param instance
     * @return
     */
    fun update(instance: Instance?): Int {
        return updateTableRow(Instance.uri, instance)
    }

    /**
     * Update reminder
     *
     * @param reminder
     * @return
     */
    fun update(reminder: Reminder?): Int {
        return updateTableRow(Reminder.uri, reminder)
    }

    /**
     * Update attendee
     *
     * @param attendee
     * @return
     */
    fun update(attendee: Attendee?): Int {
        return updateTableRow(Attendee.uri, attendee)
    }
}