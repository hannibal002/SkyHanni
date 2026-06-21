package at.hannibal2.skyhanni.config.storage

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import com.google.gson.annotations.Expose
import java.util.UUID

class SeenContributorStorage {
    // uuid to first seen timestamp
    @Expose
    val seenContributors: MutableMap<UUID, SimpleTimeMark> = mutableMapOf()

    // Records of timestamps when a Skyhanni contributor was recognized
    @Expose
    val contributorMentions: MutableList<SimpleTimeMark> = mutableListOf()
}
