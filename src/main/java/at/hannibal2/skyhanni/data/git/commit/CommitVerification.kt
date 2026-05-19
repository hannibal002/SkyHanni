package at.hannibal2.skyhanni.data.git.commit

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.time.Instant

data class CommitVerification(
    @Expose val verified: Boolean,
    @Expose val reason: String,
    @Expose val signature: String? = null,
    @Expose val payload: String? = null,
    @Expose @field:SerializedName("verified_at") private val verifiedAtString: String? = null,
) {
    val verifiedAt: SimpleTimeMark? get() = verifiedAtString?.let {
        Instant.parse(it).toEpochMilli().asTimeMark()
    }
}
