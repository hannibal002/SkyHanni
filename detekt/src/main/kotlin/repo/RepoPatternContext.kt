package repo

import org.jetbrains.kotlin.psi.KtPropertyDelegate
import repo.RepoPatternElement.Companion.asRepoPatternElement
import java.util.IdentityHashMap

class RepoPatternContext {
    private val cache = IdentityHashMap<KtPropertyDelegate, RepoPatternElement?>(2048)

    fun getRepoPatternElement(property: KtPropertyDelegate): RepoPatternElement? {
        val cachedValue = cache[property]

        if (cachedValue != null) {
            return cachedValue.takeUnless { it === RepoPatternElement.SENTINAL_VALUE }
        }

        val element = property.asRepoPatternElement()
        cache[property] = element ?: RepoPatternElement.SENTINAL_VALUE
        return element
    }
}
