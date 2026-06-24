package repo

import RepoPatternElement
import RepoPatternElement.Companion.asRepoPatternElement
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import java.util.IdentityHashMap

class RepoPatternContext {
    private object NullValue

    private val cache = IdentityHashMap<KtPropertyDelegate, Any>(2048)

    fun getRepoPatternElement(property: KtPropertyDelegate): RepoPatternElement? {
        val cachedValue = cache[property]

        if (cachedValue != null) {
            @Suppress("UNCHECKED_CAST")
            return if (cachedValue === NullValue) {
                null
            } else {
                cachedValue as RepoPatternElement
            }
        }

        val element = property.asRepoPatternElement()
        cache[property] = element ?: NullValue
        return element
    }
}
