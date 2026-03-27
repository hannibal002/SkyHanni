package at.hannibal2.skyhanni.detektrules.potentialbugs

import at.hannibal2.skyhanni.detektrules.SkyHanniRule
import at.hannibal2.skyhanni.detektrules.utils.DetektUtils.doWeNeedToCheckConfigProp
import dev.detekt.api.Config
import dev.detekt.api.Debt
import dev.detekt.api.Issue
import dev.detekt.api.Severity
import dev.detekt.rules.hasAnnotation
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty

class StorageNeedsExpose(config: Config) : SkyHanniRule(config) {
    override val issue = Issue(
        "StorageNeedsExpose",
        Severity.Defect,
        "Config/storage properties that are intended to store data should be annotated with @Expose.",
        Debt.TEN_MINS,
    )

    companion object {
        const val STORAGE_PACKAGE = "at.hannibal2.skyhanni.config.storage"
        const val CONFIG_PACKAGE = "at.hannibal2.skyhanni.config.features"
    }

    override fun visitKtFile(file: KtFile) {
        val packageName = file.packageDirective?.fqName?.asString() ?: ""
        if (!packageName.startsWith(CONFIG_PACKAGE) && !packageName.startsWith(STORAGE_PACKAGE)) return
        super.visitKtFile(file)
    }

    private fun checkProperty(property: KtProperty) {
        if (!property.doWeNeedToCheckConfigProp()) return
        if (property.hasAnnotation("Expose")) return

        // If the property is not annotated with @Expose, report it
        if (property.hasAnnotation("ConfigOption")) {
            // Valid reasons to not have the @Expose annotation on a config option:
            //  - Has the ConfigEditorInfoText annotation
            //  - Has the ConfigEditorButton annotation
            if (property.hasAnnotation("ConfigEditorInfoText")) return
            if (property.hasAnnotation("ConfigEditorButton")) return
        }

        return property.reportIssue("@Expose annotation is missing from property ${property.name}")
    }

    override fun visitProperty(property: KtProperty) {
        checkProperty(property)
        super.visitProperty(property)
    }
}
