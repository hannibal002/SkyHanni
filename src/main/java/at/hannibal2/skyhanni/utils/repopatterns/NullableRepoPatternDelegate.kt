package at.hannibal2.skyhanni.utils.repopatterns

import java.util.regex.Pattern
import kotlin.reflect.KProperty

class NullableRepoPatternDelegate(
    private val delegate: RepoPattern?,
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Pattern? {
        return delegate?.getValue(thisRef, property)
    }
}
