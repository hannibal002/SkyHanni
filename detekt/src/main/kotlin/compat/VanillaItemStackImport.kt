package compat

import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtImportDirective

class VanillaItemStackImport(config: Config) : SkyHanniRule(
    config,
    "Use SafeItemStack instead of the vanilla ItemStack import to avoid 'Components not bound yet' crashes.",
) {

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
