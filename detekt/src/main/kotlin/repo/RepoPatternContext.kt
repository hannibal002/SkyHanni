package repo

import org.jetbrains.kotlin.psi.KtPropertyDelegate
import repo.RepoPatternElement.Companion.asRepoPatternElement
import java.util.IdentityHashMap

class RepoPatternContext {
    private val cache = IdentityHashMap<KtPropertyDelegate, RepoPatternElement>()

    fun getRepoPatternElement(property: KtPropertyDelegate): RepoPatternElement? =
        cache.getOrPut(property) {
            property.asRepoPatternElement() ?: return null
        }

    data class RepoPatternTestContext(
        val element: RepoPatternElement,
        val variableName: String,
        val rawPattern: String,
    )

    // This is since IntelliJ keep complaining about duplicated code
    fun getRepoPatternElementSplat(delegate: KtPropertyDelegate): RepoPatternTestContext? {
        val element = getRepoPatternElement(delegate) ?: return null
        val rawPattern = element.rawPattern

        if (!rawPattern.needsRegexTest()) return null

        val tests = element.regexTests
        if (tests.isEmpty()) return null

        return RepoPatternTestContext(
            element = element,
            variableName = element.variableName,
            rawPattern = rawPattern,
        )
    }


    private fun String.needsRegexTest(): Boolean {
        return regexConstructs.containsMatchIn(this)
    }

    private val regexConstructs = Regex("""(?<!\\)[.*+(){}\[|?]""")
}
