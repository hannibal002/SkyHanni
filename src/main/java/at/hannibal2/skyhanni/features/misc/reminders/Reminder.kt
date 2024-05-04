package at.hannibal2.skyhanni.features.misc.reminders

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import com.google.gson.annotations.Expose

data class Reminder(
    @Expose var reason: String,
    @Expose var remindAt: Long,
    @Expose val id: Int
) {
    override fun toString(): String {
        return "($id) reason: $reason in: ${timeUntil()}"
    }

    fun timeUntil() = SimpleTimeMark(remindAt).timeUntil().format()
}
