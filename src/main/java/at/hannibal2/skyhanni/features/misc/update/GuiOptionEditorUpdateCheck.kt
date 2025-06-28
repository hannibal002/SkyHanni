package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.elements.GuiElementButton
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import kotlin.math.max

class GuiOptionEditorUpdateCheck(option: ProcessedOption) : GuiOptionEditor(option) {

    val button = GuiElementButton()
    val changelog = GuiElementButton().apply { text = "Show Changelog" }

    val currentVersion = SkyHanniMod.VERSION

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        val fr = context.minecraft.defaultFontRenderer

        context.pushMatrix()
        context.translate(x.toFloat() + 10, y.toFloat(), 1F)
        val adjustedWidth = width - 20
        val nextVersion = UpdateManager.getNextVersion()

        button.text = when (UpdateManager.updateState) {
            UpdateManager.UpdateState.AVAILABLE -> "Download update"
            UpdateManager.UpdateState.QUEUED -> "Downloading..."
            UpdateManager.UpdateState.DOWNLOADED -> "Downloaded"
            UpdateManager.UpdateState.NONE -> if (nextVersion == null) "Check for Updates" else "Up to date"
        }
        button.width = button.getWidth(context)
        button.render(context, getButtonPosition(adjustedWidth), 10)

        if (UpdateManager.updateState != UpdateManager.UpdateState.NONE) {
            changelog.width = changelog.getWidth(context)
            changelog.render(context, getChangelogPosition(adjustedWidth), 30)
        }

        val widthRemaining = adjustedWidth - max(button.width, changelog.width) - 10

        if (UpdateManager.updateState == UpdateManager.UpdateState.DOWNLOADED) {
            context.drawStringCenteredScaledMaxWidth(
                "§aThe update will be installed after your next restart.",
                fr,
                widthRemaining / 2F,
                40F,
                true,
                widthRemaining,
                -1,
            )
        }

        context.scale(2F, 2F, 1F)
        val sameVersion = currentVersion.equals(nextVersion, ignoreCase = true)
        context.drawStringCenteredScaledMaxWidth(
            "${if (UpdateManager.updateState == UpdateManager.UpdateState.NONE) "§a" else "§c"}$currentVersion" +
                if (nextVersion != null && !sameVersion) "➜ §a$nextVersion" else "",
            fr,
            widthRemaining / 4F,
            10F,
            true,
            widthRemaining / 2,
            -1,
        )

        context.popMatrix()
    }

    private fun getButtonPosition(width: Int) = width - button.width
    private fun getChangelogPosition(width: Int) = width - changelog.width
    override fun getHeight(): Int {
        return 55
    }

    override fun mouseInput(x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int): Boolean {
        fun isInside(width: Int, height: Int, def: GuiElementButton): Boolean {
            val inX = (mouseX - width - x) in (0..def.width)
            val inY = (mouseY - height - y) in (0..def.height)
            return MouseCompat.getEventButtonState() && inX && inY
        }

        if (isInside(getButtonPosition(width - 20), height = 10, button)) {
            when (UpdateManager.updateState) {
                UpdateManager.UpdateState.AVAILABLE -> UpdateManager.queueUpdate()
                UpdateManager.UpdateState.QUEUED -> {}
                UpdateManager.UpdateState.DOWNLOADED -> {}
                UpdateManager.UpdateState.NONE -> UpdateManager.checkUpdate()
            }
            return true
        }
        if (!isInside(getChangelogPosition(width - 20), height = 30, changelog)) return false

        if (UpdateManager.updateState != UpdateManager.UpdateState.NONE)
            UpdateManager.getNextVersion()?.let { ChangelogViewer.showChangelog(currentVersion, it) }
                ?: ErrorManager.logErrorStateWithData(
                    "Can't get Changelog because of internal error",
                    "UpdateManager.getNextVersion is null even though updateState is != NONE",
                    "state" to UpdateManager.updateState,
                )

        return true
    }

    override fun keyboardInput(): Boolean {
        return false
    }

    override fun fulfillsSearch(word: String): Boolean {
        return super.fulfillsSearch(word) || word in "download" || word in "update"
    }
}
