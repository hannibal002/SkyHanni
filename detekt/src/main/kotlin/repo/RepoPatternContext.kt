package repo

import org.jetbrains.kotlin.psi.KtPropertyDelegate
import repo.RepoPatternElement.Companion.asRepoPatternElement
import java.util.IdentityHashMap

class RepoPatternContext {
    private val cache = IdentityHashMap<KtPropertyDelegate, RepoPatternElement?>()

    // This intentionally caches null values, so that we don't have to reparse the same property delegate multiple times.
    fun getRepoPatternElement(property: KtPropertyDelegate): RepoPatternElement? {
        if (cache.containsKey(property)) {
            return cache[property]
        }

        val element = property.asRepoPatternElement()
        cache[property] = element
        return element
    }
}
