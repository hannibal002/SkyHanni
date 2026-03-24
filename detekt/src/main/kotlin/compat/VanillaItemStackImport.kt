package at.hannibal2.skyhanni.detektrules.compat

import at.hannibal2.skyhanni.detektrules.SkyHanniRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtImportDirective

class VanillaItemStackImport(config: Config) : SkyHanniRule(config) {
    override val issue = Issue(
        "VanillaItemStackImport",
        Severity.Style,
        "Use SafeItemStack instead of the vanilla ItemStack import to avoid 'Components not bound yet' crashes.",
        Debt.FIVE_MINS,
    )

    override fun visitImportDirective(importDirective: KtImportDirective) {
        val filePath = importDirective.containingFile.virtualFile.path
        if (filePath.endsWith("SafeItemStack.kt")) return

        val importedFqName = importDirective.importedFqName?.asString() ?: return
        if (importedFqName == "net.minecraft.world.item.ItemStack") {
            importDirective.reportIssue(
                "Direct import of `net.minecraft.world.item.ItemStack` is forbidden. " +
                    "Use `at.hannibal2.skyhanni.utils.SafeItemStack` instead."
            )
        }

        super.visitImportDirective(importDirective)
    }
}
