package repo

import org.jetbrains.kotlin.psi.KtPropertyDelegate
import repo.RepoPatternElement.Companion.asRepoPatternElement
import java.util.concurrent.ConcurrentHashMap

class RepoPatternContext {
    private val cache = ConcurrentHashMap<IdentityCharacteristics<KtPropertyDelegate>, RepoPatternElement>(2048)

    fun getRepoPatternElement(property: KtPropertyDelegate): RepoPatternElement? {
        val identityKey = IdentityCharacteristics(property)

        val result = cache.computeIfAbsent(identityKey) {
            property.asRepoPatternElement() ?: RepoPatternElement.SENTINEL_VALUE
        }

        return result.takeUnless { it === RepoPatternElement.SENTINEL_VALUE }
    }

    private class IdentityCharacteristics<T>(val value: T) {
        override fun equals(other: Any?): Boolean {
            if (other !is IdentityCharacteristics<*>) return false
            return this.value === other.value
        }

        override fun hashCode(): Int {
            return System.identityHashCode(value)
        }
    }
}
