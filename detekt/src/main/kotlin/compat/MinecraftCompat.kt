package compat

import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtProperty

class MinecraftCompat(config: Config) : SkyHanniRule(config, "Ensure you are using the MinecraftCompat methods") {
    private val minecraftReplacements = mapOf(
        "level" to "MinecraftCompat.localWorldOrNull",
        "player" to "MinecraftCompat.localPlayerOrNull",
        "user" to "MinecraftCompat.localUser",
        "screen" to "MinecraftCompat.screen",
        "gui" to "MinecraftCompat.hud",
        "options.hideGui" to "MinecraftCompat.hideGui",
        "levelRenderer.allChanged()" to "MinecraftCompat.reloadChunks()",
    )

    private val replacements = buildList {
        listOf(
            "Minecraft.getInstance()",
            "mc",
            "client",
        ).forEach { prefix ->
            minecraftReplacements.forEach { (access, replacement) ->
                add(
                    Regex(
                        """\b${Regex.escape("$prefix.$access")}(?=\.|\(|$)"""
                    ) to replacement
                )
            }
        }
    }

    private val allowedPaths = listOf(
        "at\\hannibal2\\skyhanni\\utils\\compat",
        "at/hannibal2/skyhanni/utils/compat",
        "at\\hannibal2\\skyhanni\\test",
        "at/hannibal2/skyhanni/test"
    )

    override fun visitProperty(property: KtProperty) {
        if (shouldIgnore(property)) return
        checkMinecraftCompat(property.initializer)
        super.visitProperty(property)
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        if (shouldIgnore(expression)) return
        checkMinecraftCompat(expression)
        super.visitDotQualifiedExpression(expression)
    }

    private fun shouldIgnore(element: KtExpression): Boolean {
        if (element.parent is KtImportDirective) return true

        val filePath = element.containingFile.virtualFile.path
        return allowedPaths.any { filePath.contains(it) }
    }

    private fun checkMinecraftCompat(element: KtExpression?) {
        val text = element?.text ?: return

        replacements.forEach { (regex, replacement) ->
            val match = regex.find(text) ?: return@forEach

            element.reportIssue(
                "Usage of `${match.value}` detected. Please replace this with `$replacement` instead.",
            )
            return
        }
    }
}
