/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-12.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.event

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.modules.events.Event
import pl.szczodrzynski.edziennik.data.db.modules.subjects.Subject
import pl.szczodrzynski.edziennik.data.db.modules.teams.Team
import pl.szczodrzynski.edziennik.data.db.modules.timetable.Lesson
import pl.szczodrzynski.edziennik.data.db.modules.timetable.LessonFull
import pl.szczodrzynski.edziennik.databinding.DialogEventManualV2Binding
import pl.szczodrzynski.edziennik.utils.TextInputDropDown
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.models.Week
import kotlin.coroutines.CoroutineContext

class EventManualV2Dialog(
        val activity: AppCompatActivity,
        val profileId: Int,
        val defaultLesson: LessonFull? = null,
        val defaultDate: Date? = null,
        val defaultTime: Time? = null,
        val defaultType: Int? = null,
        val editingEvent: Event? = null,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {

    companion object {
        private const val TAG = "EventManualDialog"
    }

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val app by lazy { activity.application as App }
    private lateinit var b: DialogEventManualV2Binding
    private lateinit var dialog: AlertDialog
    private lateinit var event: Event
    private var defaultLoaded = false

    init { run {
        job = Job()
        onShowListener?.invoke(TAG)
        b = DialogEventManualV2Binding.inflate(activity.layoutInflater)
        dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.dialog_event_manual_title)
                .setView(b.root)
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .setPositiveButton(R.string.save) { _, _ -> saveEvent() }
                .setOnDismissListener {
                    onDismissListener?.invoke(TAG)
                }
                .show()

        event = editingEvent?.clone() ?: Event().also { event ->
            event.profileId = profileId
            /*defaultDate?.let {
                event.eventDate = it
                b.date = it
            }
            defaultTime?.let {
                event.startTime = it
                b.time = it
            }
            defaultType?.let {
                event.type = it
            }*/
        }

        loadLists()
    }}

    private fun loadLists() { launch {
        val deferred = async(Dispatchers.Default) {
            // get the team list
            val teams = app.db.teamDao().getAllNow(profileId)
            b.teamDropdown.clear()
            b.teamDropdown += TextInputDropDown.Item(
                    -1,
                    activity.getString(R.string.dialog_event_manual_no_team),
                    ""
            )
            b.teamDropdown += teams.map { TextInputDropDown.Item(it.id, it.name, tag = it) }

            // get the subject list
            val subjects = app.db.subjectDao().getAllNow(profileId)
            b.subjectDropdown.clear()
            b.subjectDropdown += TextInputDropDown.Item(
                    -1,
                    activity.getString(R.string.dialog_event_manual_no_subject),
                    ""
            )
            b.subjectDropdown += subjects.map { TextInputDropDown.Item(it.id, it.longName, tag = it) }

            // get the teacher list
            val teachers = app.db.teacherDao().getAllNow(profileId)
            b.teacherDropdown.clear()
            b.teacherDropdown += TextInputDropDown.Item(
                    -1,
                    activity.getString(R.string.dialog_event_manual_no_teacher),
                    ""
            )
            b.teacherDropdown += teachers.map { TextInputDropDown.Item(it.id, it.fullName, tag = it) }
        }
        deferred.await()

        b.teamDropdown.isEnabled = true
        b.subjectDropdown.isEnabled = true
        b.teacherDropdown.isEnabled = true

        // copy IDs from event being edited
        editingEvent?.let {
            b.teamDropdown.select(it.teamId)
            b.subjectDropdown.select(it.subjectId)
            b.teacherDropdown.select(it.teacherId)
        }

        // copy IDs from the LessonFull
        defaultLesson?.let {
            b.teamDropdown.select(it.displayTeamId)
            b.subjectDropdown.select(it.displaySubjectId)
            b.teacherDropdown.select(it.displayTeacherId)
        }

        loadDates()
    }}

    private fun loadDates() { launch {
        val date = Date.getToday()
        val today = date.value
        var weekDay = date.weekDay

        val deferred = async(Dispatchers.Default) {
            val dates = mutableListOf<TextInputDropDown.Item>()
            // item choosing the next lesson of specific subject
            b.subjectDropdown.selected?.let {
                if (it.tag is Subject) {
                    dates += TextInputDropDown.Item(
                            -it.id,
                            activity.getString(R.string.dialog_event_manual_date_next_lesson, it.tag.longName)
                    )
                }
            }

            // TODAY
            dates += TextInputDropDown.Item(
                    date.value.toLong(),
                    activity.getString(R.string.dialog_event_manual_date_today, date.formattedString),
                    tag = date.clone()
            )

            // TOMORROW
            if (weekDay < 4) {
                date.stepForward(0, 0, 1)
                weekDay++
                dates += TextInputDropDown.Item(
                        date.value.toLong(),
                        activity.getString(R.string.dialog_event_manual_date_tomorrow, date.formattedString),
                        tag = date.clone()
                )
            }
            // REMAINING SCHOOL DAYS OF THE CURRENT WEEK
            while (weekDay < 4) {
                date.stepForward(0, 0, 1) // step one day forward
                weekDay++
                dates += TextInputDropDown.Item(
                        date.value.toLong(),
                        activity.getString(R.string.dialog_event_manual_date_this_week, Week.getFullDayName(weekDay), date.formattedString),
                        tag = date.clone()
                )
            }
            // go to next week Monday
            date.stepForward(0, 0, -weekDay + 7)
            weekDay = 0
            // ALL SCHOOL DAYS OF THE NEXT WEEK
            while (weekDay < 4) {
                dates += TextInputDropDown.Item(
                        date.value.toLong(),
                        activity.getString(R.string.dialog_event_manual_date_next_week, Week.getFullDayName(weekDay), date.formattedString),
                        tag = date.clone()
                )
                date.stepForward(0, 0, 1) // step one day forward
                weekDay++
            }
            dates += TextInputDropDown.Item(
                    -1L,
                    activity.getString(R.string.dialog_event_manual_date_other)
            )
            dates
        }

        val dates = deferred.await()
        b.dateDropdown.clear().append(dates)

        editingEvent?.let {
            b.dateDropdown.select(it.eventDate.value.toLong())
        }

        defaultLesson?.let {
            b.dateDropdown.select(it.displayDate?.value?.toLong())
        }

        if (b.dateDropdown.selected == null) {
            b.dateDropdown.select(today.toLong())
        }

        b.dateDropdown.isEnabled = true

        b.dateDropdown.setOnChangeListener { item ->
            when {
                // next lesson with specified subject
                item.id < -1 -> {
                    app.db.timetableDao().getNextWithSubject(profileId, Date.getToday(), -item.id).observeOnce(activity, Observer {
                        val lessonDate = it?.displayDate ?: return@Observer
                        b.dateDropdown.selected = TextInputDropDown.Item(
                                lessonDate.value.toLong(),
                                lessonDate.formattedString,
                                tag = lessonDate
                        )
                        // TODO load correct hour when selecting next lesson
                        b.dateDropdown.updateText()
                        it.let {
                            b.teamDropdown.select(it.displayTeamId)
                            b.subjectDropdown.select(it.displaySubjectId)
                            b.teacherDropdown.select(it.displayTeacherId)
                        }
                        defaultLoaded = false
                        loadHours()
                    })
                    return@setOnChangeListener false
                }
                // custom date
                item.id == -1L -> {
                    MaterialDatePicker.Builder
                            .datePicker()
                            .setSelection((b.dateDropdown.selectedId?.let { Date.fromValue(it.toInt()) } ?: Date.getToday()).inMillis)
                            .build()
                            .apply {
                                addOnPositiveButtonClickListener {
                                    val dateSelected = Date.fromMillis(it)
                                    b.dateDropdown.selected = TextInputDropDown.Item(
                                            dateSelected.value.toLong(),
                                            dateSelected.formattedString,
                                            tag = dateSelected
                                    )
                                    b.dateDropdown.updateText()
                                    loadHours()
                                }
                                show(this@EventManualV2Dialog.activity.supportFragmentManager, "MaterialDatePicker")
                            }

                    return@setOnChangeListener false
                }
                // a specific date
                else -> {
                    b.dateDropdown.select(item)
                    loadHours()
                }
            }
            return@setOnChangeListener true
        }

        loadHours()
    }}

    private fun loadHours() {
        b.timeDropdown.isEnabled = false
        // get the selected date
        val date = b.dateDropdown.selectedId?.let { Date.fromValue(it.toInt()) } ?: return
        // get all lessons for selected date
        app.db.timetableDao().getForDate(profileId, date).observeOnce(activity, Observer { lessons ->
            val hours = mutableListOf<TextInputDropDown.Item>()
            // add All day time choice
            hours += TextInputDropDown.Item(
                    0L,
                    activity.getString(R.string.dialog_event_manual_all_day)
            )
            lessons.forEach { lesson ->
                if (lesson.type == Lesson.TYPE_NO_LESSONS) {
                    // indicate there are no lessons this day
                    hours += TextInputDropDown.Item(
                            -2L,
                            activity.getString(R.string.dialog_event_manual_no_lessons)
                    )
                    return@forEach
                }
                // create the lesson caption
                val text = listOfNotEmpty(
                        lesson.displayStartTime?.stringHM ?: "",
                        lesson.displaySubjectName?.let {
                            when {
                                lesson.type == Lesson.TYPE_CANCELLED -> it.asStrikethroughSpannable()
                                lesson.type != Lesson.TYPE_NORMAL -> it.asItalicSpannable()
                                else -> it
                            }
                        } ?: ""
                )
                // add an item with LessonFull as the tag
                hours += TextInputDropDown.Item(
                        lesson.displayStartTime?.value?.toLong() ?: -1,
                        text.concat(" "),
                        tag = lesson
                )
            }
            b.timeDropdown.clear().append(hours)

            if (defaultLoaded) {
                b.timeDropdown.deselect()
                // select the TEAM_CLASS if possible
                b.teamDropdown.items.singleOrNull {
                    it.tag is Team && it.tag.type == Team.TYPE_CLASS
                }?.let {
                    b.teamDropdown.select(it)
                } ?: b.teamDropdown.deselect()

                // clear subject, teacher selection
                b.subjectDropdown.deselect()
                b.teacherDropdown.deselect()
            }
            else {
                editingEvent?.let {
                    b.timeDropdown.select(it.startTime?.value?.toLong())
                }

                defaultLesson?.let {
                    b.timeDropdown.select(it.displayStartTime?.value?.toLong())
                }
            }
            defaultLoaded = true
            b.timeDropdown.isEnabled = true

            // attach a listener to time dropdown
            b.timeDropdown.setOnChangeListener { item ->
                when {
                    // custom start hour
                    item.id == -1L -> {

                        return@setOnChangeListener false
                    }
                    // no lessons this day
                    item.id == -2L -> {
                        b.timeDropdown.deselect()
                        return@setOnChangeListener false
                    }
                    // selected a specific lesson
                    else -> {
                        if (item.tag is LessonFull) {
                            // update team, subject, teacher dropdowns,
                            // using the LessonFull from item tag
                            b.teamDropdown.deselect()
                            b.subjectDropdown.deselect()
                            b.teacherDropdown.deselect()
                            item.tag.displayTeamId?.let { 
                                b.teamDropdown.select(it)
                            }
                            item.tag.displaySubjectId?.let {
                                b.subjectDropdown.select(it)
                            }
                            item.tag.displayTeacherId?.let {
                                b.teacherDropdown.select(it)
                            }
                        }
                    }
                }
                return@setOnChangeListener true
            }
        })
    }

    private fun saveEvent() {

    }
}