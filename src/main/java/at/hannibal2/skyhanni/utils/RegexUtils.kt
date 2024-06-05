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

    fun List<Pattern>.allMatches(list: List<String>): List<String> = list.filter { line -> any { it.matches(line) } }
    fun List<Pattern>.anyMatches(list: List<String>?): Boolean = list?.any { line -> any { it.matches(line) } } ?: false

    fun Pattern.matches(string: String?): Boolean = string?.let { matcher(it).matches() } ?: false
    fun Pattern.find(string: String?) = string?.let { matcher(it).find() } ?: false

    fun Pattern.anyMatches(list: List<String>?): Boolean = list?.any { matches(it) } ?: false
    fun Pattern.anyMatches(list: Sequence<String>?): Boolean = anyMatches(list?.toList())

    fun Pattern.matchGroup(text: String, groupName: String): String? = matchMatcher(text) { groupOrNull(groupName) }

    fun Pattern.matchGroups(text: String, vararg groups: String): List<String?>? =
        matchMatcher(text) { groups.toList().map { groupOrNull(it) } }

    fun Pattern.firstMatches(list: List<String>): String? = list.firstOrNull { matches(it) }
    fun Pattern.allMatches(list: List<String>): List<String> = list.filter { matches(it) }

    /**
     * Get the group, otherwise, return null
     * @param groupName The group name in the pattern
     */
    fun Matcher.groupOrNull(groupName: String): String? = runCatching { group(groupName) }.getOrNull()

    fun Matcher.hasGroup(groupName: String): Boolean = groupOrNull(groupName) != null

    fun List<String>.indexOfFirstMatch(pattern: Pattern): Int? {
        for ((index, line) in withIndex()) {
            pattern.matcher(line).let { if (it.matches()) return index }
        }
        return null
    }
}
