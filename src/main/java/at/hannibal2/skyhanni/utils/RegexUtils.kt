package at.hannibal2.skyhanni.utils

import java.util.regex.Matcher
import java.util.regex.Pattern

object RegexUtils {
    inline fun <T> Pattern.matchMatcher(text: String, consumer: Matcher.() -> T) =
        matcher(text).let { if (it.matches()) consumer(it) else null }

    inline fun <T> Pattern.findMatcher(text: String, consumer: Matcher.() -> T) =
        matcher(text).let { if (it.find()) consumer(it) else null }

    inline fun <T> Sequence<String>.matchFirst(pattern: Pattern, consumer: Matcher.() -> T): T? =
        toList().matchFirst(pattern, consumer)

    inline fun <T> List<String>.matchFirst(pattern: Pattern, consumer: Matcher.() -> T): T? {
        for (line in this) {
            pattern.matcher(line).let { if (it.matches()) return consumer(it) }
        }
        return null
    }

    inline fun <T> List<String>.matchAll(pattern: Pattern, consumer: Matcher.() -> T): T? {
        for (line in this) {
            pattern.matcher(line).let { if (it.find()) consumer(it) }
        }
        return null
    }

    inline fun <T> List<Pattern>.matchMatchers(text: String, consumer: Matcher.() -> T): T? {
        for (pattern in iterator()) {
            pattern.matchMatcher<T>(text) {
                return consumer()
            }
        }
        return null
    }

    fun Pattern.matches(string: String?): Boolean = string?.let { matcher(it).matches() } ?: false
    fun Pattern.find(string: String?) = string?.let { matcher(it).find() } ?: false

    fun Pattern.anyMatches(list: List<String>?): Boolean = list?.any { this.matches(it) } ?: false
    fun Pattern.anyMatches(list: Sequence<String>?): Boolean = anyMatches(list?.toList())

    /**
     * Get the group, otherwise, return null
     * @param groupName The group name in the pattern
     */
    fun Matcher.groupOrNull(groupName: String): String? = runCatching { this.group(groupName) }.getOrNull()

    fun Matcher.hasGroup(groupName: String): Boolean = groupOrNull(groupName) != null
}
