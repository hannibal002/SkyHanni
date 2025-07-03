package at.hannibal2.skyhanni.data.hotx

import java.util.regex.Pattern

interface RepoPatternEnum {
    val basePath: String
    val patternId: String
        get() = (this as Enum<*>).name.lowercase().replace("_", ".")
}

interface ChatRepoPatternEnum : RepoPatternEnum {
    val chatPatternRaw: String
    val chatPattern: Pattern
}

interface ItemRepoPatternEnum : RepoPatternEnum {
    val itemPatternRaw: String
    val itemPattern: Pattern
}
