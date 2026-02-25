package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import java.awt.Color

class GuiOptionEditorDeprecatedVersion(option: ProcessedOption) : GuiOptionEditor(option) {

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        if (!isDiscontinued()) return
        val versionData = UpdateManager.discontinuedVersions[PlatformUtils.MC_VERSION]?.configInfo ?: return
        val fr = context.minecraft.defaultFontRenderer
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), y + 2f, Color.RED.rgb)
        context.drawColoredRect(x.toFloat(), y + 55f - 2, (x + width).toFloat(), y + 55f, Color.RED.rgb)
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + 2).toFloat(), y + 55f, Color.RED.rgb)
        context.drawColoredRect(x + width.toFloat() - 2, y.toFloat(), (x + width).toFloat(), y + 55f, Color.RED.rgb)

        for ((index, s) in versionData.withIndex()) {
            context.drawStringScaledMaxWidth(s.asStructuredText(), fr, x + 10, y + 10 + 11 * index, false, width - 20, -1)
        }

    }

    override fun getHeight(): Int {
        return if (isDiscontinued()) 55 else 0
    }

    override fun fulfillsSearch(word: String): Boolean {
        return isDiscontinued()
    }

    private fun isDiscontinued(): Boolean {
        return UpdateManager.discontinuedVersions[PlatformUtils.MC_VERSION]?.configInfo != null
    }
}
