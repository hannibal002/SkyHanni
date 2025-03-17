package at.hannibal2.skyhanni.detektrules.compat

import at.hannibal2.skyhanni.detektrules.SkyHanniRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty

class MinecraftCompat(config: Config) : SkyHanniRule(config) {
    override val issue = Issue(
        "MinecraftCompat",
        Severity.Style,
        "Ensure you are using the MinecraftCompat methods",
        Debt.FIVE_MINS,
    )

    override fun visitProperty(property: KtProperty) {
        if (shouldIgnore(property)) return
        if (checkForMinecraftPlayer(property.initializer)) return
        if (checkForMinecraftWorld(property.initializer)) return
        super.visitProperty(property)
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        if (shouldIgnore(expression)) return
        if (checkForMinecraftPlayer(expression)) return
        if (checkForMinecraftWorld(expression)) return
        super.visitDotQualifiedExpression(expression)
    }

    private fun shouldIgnore(element: KtExpression): Boolean {
        val filePath = element.containingFile.virtualFile.path
        return filePath.contains("at\\hannibal2\\skyhanni\\utils\\compat") ||
            filePath.contains("at/hannibal2/skyhanni/utils/compat")
    }

    private fun checkForMinecraftPlayer(element: KtExpression?): Boolean {
        if (element?.text?.contains("Minecraft.getMinecraft().thePlayer") == true) {
            element.reportIssue("Usage of Minecraft.getMinecraft().thePlayer detected. Please replace this with " +
                "`MinecraftCompat.localPlayer` instead.")
            return true
        }
        return false
    }

    private fun checkForMinecraftWorld(element: KtExpression?): Boolean {
        if (element?.text?.contains("Minecraft.getMinecraft().theWorld") == true) {
            element.reportIssue("Usage of Minecraft.getMinecraft().theWorld detected. Please replace this with " +
                "`MinecraftCompat.world` instead.")
            return true
        }
        return false
    }
}
