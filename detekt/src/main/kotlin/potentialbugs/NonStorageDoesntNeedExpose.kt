package at.hannibal2.skyhanni.detektrules.potentialbugs

import at.hannibal2.skyhanni.detektrules.SkyHanniRule
import at.hannibal2.skyhanni.detektrules.potentialbugs.StorageNeedsExpose.Companion.CONFIG_PACKAGE
import at.hannibal2.skyhanni.detektrules.potentialbugs.StorageNeedsExpose.Companion.STORAGE_PACKAGE
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.rules.hasAnnotation
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isPrivate

class NonStorageDoesntNeedExpose(config: Config): SkyHanniRule(config) {
    override val issue = Issue(
        "NonStorageDoesntNeedExpose",
        Severity.Defect,
        "Config/storage properties that are not intended to store data should not be annotated with @Expose.",
        Debt.TEN_MINS,
    )

    override fun visitKtFile(file: KtFile) {
        val packageName = file.packageDirective?.fqName?.asString() ?: ""
        if (!packageName.startsWith(CONFIG_PACKAGE) && !packageName.startsWith(STORAGE_PACKAGE)) return
        super.visitKtFile(file)
    }

    override fun visitProperty(property: KtProperty) {
        if (property.hasAnnotation("Expose")) {
            // Reasons to NOT have Expose annotation:
            //  - The property is local
            //  - The property is private
            //  - The property has a getter
            //  - The property is annotated with ConfigEditorInfoText
            //  - The property is annotated with ConfigEditorButton
            //  - The property is annotated with Transient
            val hasExplicitGetter = property.getter?.hasBody() ?: false
            val doWeCare = property.isLocal || property.isPrivate() || hasExplicitGetter ||
                property.hasAnnotation("ConfigEditorInfoText") ||
                property.hasAnnotation("ConfigEditorButton") ||
                property.hasAnnotation("Transient")

            if (doWeCare) {
                property.reportIssue("@Expose annotation is not needed on property ${property.name}")
            }
        }

        return super.visitProperty(property)
    }
}
