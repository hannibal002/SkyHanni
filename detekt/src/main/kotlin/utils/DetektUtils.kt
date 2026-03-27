package utils

import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isPrivate

object DetektUtils {

    fun KtAnnotated.hasAnnotation(name: String): Boolean =
        annotationEntries.any { it.shortName?.asString() == name }

    // Skip:
    //  - Local properties
    //  - Private properties
    //  - Properties with getters
    //  - Properties with @Transient annotation
    fun KtProperty.doWeNeedToCheckConfigProp(): Boolean {
        val hasExplicitGetter = getter?.hasBody() ?: false
        val isTransient = hasAnnotation("Transient")

        return !isLocal && !isPrivate() && !hasExplicitGetter && !isTransient
    }

}
