package at.hannibal2.skyhanni.detektrules.utils

import io.gitlab.arturbosch.detekt.rules.hasAnnotation
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isPrivate

object DetektUtils {

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
