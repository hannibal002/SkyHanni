package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.elements.GuiElementButton
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.internal.TextRenderUtils
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Mouse

class GuiOptionEditorUpdateCheck(option: ProcessedOption) : GuiOptionEditor(option) {

    val button = GuiElementButton("", -1) {}
    val changelog = GuiElementButton("Show Changelog", -1) { }

    val currentVersion = SkyHanniMod.VERSION

    override fun render(context: RenderContext?, x: Int, y: Int, width: Int) {
        val fr = Minecraft.getMinecraft().fontRendererObj

        GlStateManager.pushMatrix()
        GlStateManager.translate(x.toFloat() + 10, y.toFloat(), 1F)
        val adjustedWidth = width - 20
        val nextVersion = UpdateManager.getNextVersion()

        button.text = when (UpdateManager.updateState) {
            UpdateManager.UpdateState.AVAILABLE -> "Download update"
            UpdateManager.UpdateState.QUEUED -> "Downloading..."
            UpdateManager.UpdateState.DOWNLOADED -> "Downloaded"
            UpdateManager.UpdateState.NONE -> if (nextVersion == null) "Check for Updates" else "Up to date"
        }
        button.render(getButtonPosition(adjustedWidth), 10)

        if (UpdateManager.updateState != UpdateManager.UpdateState.NONE) {
            changelog.render(getChangelogPosition(width), 30)
        }

        val widthRemaining = width - button.width - 10

        if (UpdateManager.updateState == UpdateManager.UpdateState.DOWNLOADED) {
            TextRenderUtils.drawStringCenteredScaledMaxWidth(
                "§aThe update will be installed after your next restart.",
                fr,
                widthRemaining / 2F,
                40F,
                true,
                widthRemaining,
                -1,
            )
        }

        GlStateManager.scale(2F, 2F, 1F)
        val sameVersion = currentVersion.equals(nextVersion, ignoreCase = true)
        TextRenderUtils.drawStringCenteredScaledMaxWidth(
            "${if (UpdateManager.updateState == UpdateManager.UpdateState.NONE) "§a" else "§c"}$currentVersion" +
                if (nextVersion != null && !sameVersion) "➜ §a$nextVersion" else "",
            fr,
            widthRemaining / 4F,
            10F,
            true,
            widthRemaining / 2,
            -1,
        )

        GlStateManager.popMatrix()
    }

    private fun getButtonPosition(width: Int) = width - button.width
    private fun getChangelogPosition(width: Int) = width - changelog.width
    override fun getHeight(): Int {
        return 55
    }

    override fun mouseInput(x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int): Boolean {
        val adjustedWidth = width - 20
        if (MouseCompat.getEventButtonState() &&
            (mouseX - getButtonPosition(adjustedWidth) - x) in (0..button.width) &&
            (mouseY - 10 - y) in (0..button.height)
        ) {
            when (UpdateManager.updateState) {
                UpdateManager.UpdateState.AVAILABLE -> UpdateManager.queueUpdate()
                UpdateManager.UpdateState.QUEUED -> {}
                UpdateManager.UpdateState.DOWNLOADED -> {}
                UpdateManager.UpdateState.NONE -> UpdateManager.checkUpdate()
            }
            return true
        }
        if (Mouse.getEventButtonState() &&
            (mouseX - getChangelogPosition(width) - x) in (0..changelog.width) &&
            (mouseY - 30 - y) in (0..changelog.height)
        ) {
            if (UpdateManager.updateState != UpdateManager.UpdateState.NONE) {
                val version = UpdateManager.getNextVersion() ?: run {
                    ErrorManager.skyHanniError("Can't get Changelog because of internal error", "state" to UpdateManager.updateState)
                    return true
                }
                ChangelogViewer.showChangelog(currentVersion, version)
            }
            return true
        }
        return false
    }

    override fun keyboardInput(): Boolean {
        return false
    }

    override fun fulfillsSearch(word: String): Boolean {
        return super.fulfillsSearch(word) || word in "download" || word in "update"
    }
}
