package at.hannibal2.skyhanni.features.misc.reminders

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.TimeUtils

object ReminderManager {
    private val storage get() = SkyHanniMod.feature.storage.reminderData

    private fun createReminder(reason: String, remindAt: Long) {
        val reminder = Reminder(reason, remindAt, storage.currentReminderId + 1)
        storage.currentReminderId += 1
        storage.reminders.add(reminder)
    }

    fun command(args: Array<String>) {
        //todo remove temp thing
        if (args.size == 1) {
            if (args[0] == "print") storage.reminders.forEach { ChatUtils.chat(it.toString()) }
            return
        }

        if (args.size < 2) {
            ChatUtils.userError("/shremind [time] [reason]")
            return
        }

        val time = TimeUtils.getDuration(args.first())
        val reason = args.drop(1).joinToString(" ")
        val remindAt = time.inWholeMilliseconds + System.currentTimeMillis()

        createReminder(reason, remindAt)
    }
}
