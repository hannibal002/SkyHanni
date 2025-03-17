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
        super.visitProperty(property)
        checkForMinecraftPlayer(property.initializer)
        checkForMinecraftWorld(property.initializer)
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        if (shouldIgnore(expression)) return
        super.visitDotQualifiedExpression(expression)
        checkForMinecraftPlayer(expression)
        checkForMinecraftWorld(expression)
    }

    private fun shouldIgnore(element: KtExpression): Boolean {
        val filePath = element.containingFile.virtualFile.path
        return filePath.contains("at\\hannibal2\\skyhanni\\utils\\compat")
    }

    private fun checkForMinecraftPlayer(element: KtExpression?) {
        if (element?.text?.contains("Minecraft.getMinecraft().thePlayer") == true) {
            println("filePath: ${element.containingFile.virtualFile.path}")
            element.reportIssue("Usage of Minecraft.getMinecraft().thePlayer detected. Please replace this with " +
                "`MinecraftCompat.localPlayer` instead.")
        }
    }

    private fun checkForMinecraftWorld(element: KtExpression?) {
        if (element?.text?.contains("Minecraft.getMinecraft().theWorld") == true) {
            println("filePath: ${element.containingFile.virtualFile.path}")
            element.reportIssue("Usage of Minecraft.getMinecraft().theWorld detected. Please replace this with " +
                "`MinecraftCompat.world` instead.")
        }
    }
}
