package at.hannibal2.skyhanni.data.bingo

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import com.google.gson.annotations.Expose

data class BingoApiResponseJson(
    @Expose val success: Boolean,
    @Expose val lastUpdated: SimpleTimeMark,
    @Expose val id: Int,
    @Expose val name: String,
    @Expose val start: SimpleTimeMark,
    @Expose val end: SimpleTimeMark,
    @Expose val modifier: BingoModifier,
    @Expose val goals: List<BingoGoalJson>,
)

@Suppress("unused")
enum class BingoModifier {
    NORMAL,
    EXTREME,
    SECRET,
}

data class BingoGoalJson(
    @Expose val id: String,
    @Expose val name: String,
    @Expose val tiers: List<Int>?,
    @Expose val lore: String?,
    @Expose val fullLore: List<String>?,
    @Expose val progress: Int?,
    @Expose val requiredAmount: Int?,
)
