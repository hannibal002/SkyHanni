package at.hannibal2.skyhanni.data.git.commit

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.time.Instant

data class ShortCommitAuthor(
    @Expose val name: String,
    @Expose val email: String,
    @Expose @field:SerializedName("date") private val dateString: String,
) {
    val date: SimpleTimeMark get() = Instant.parse(dateString).toEpochMilli().asTimeMark()
}
