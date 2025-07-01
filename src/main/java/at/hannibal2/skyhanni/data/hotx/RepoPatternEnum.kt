package at.hannibal2.skyhanni.data.hotx

import at.hannibal2.skyhanni.data.hotx.RepoEnumHelper.toPatternName
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.util.regex.Pattern

interface RepoPatternEnum {
    val basePath: String
    val patternName: String
        get() = (this as Enum<*>).toPatternName()
}

interface ChatRepoPatternEnum : RepoPatternEnum {
    val chatPatternRaw: String
    val chatPattern: Pattern get() =
        RepoPattern.pattern(
            "$basePath.chat.$patternName",
            chatPatternRaw
        ).value
}

interface ItemRepoPatternEnum : RepoPatternEnum {
    val itemPatternRaw: String
    val itemPattern: Pattern get() =
        RepoPattern.pattern(
            "$basePath.item.$patternName",
            itemPatternRaw
        ).value
}

private object RepoEnumHelper {
    fun Enum<*>.toPatternName() = name.lowercase().replace("_", ".")
}
