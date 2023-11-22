package at.hannibal2.skyhanni.features.garden.fortuneguide.pages

import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.features.garden.fortuneguide.FortuneUpgrades
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.MathHelper
import java.text.DecimalFormat

class UpgradePage : FFGuideGUI.FFGuidePage() {
    private var pageScroll = 0
    private var scrollVelocity = 0.0
    private val maxNoInputFrames = 100
    private var listLength = 0

    override fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val adjustedY = FFGuideGUI.guiTop + 20 + pageScroll
        val inverseScale = 1 / 0.75f

        GlStateManager.scale(0.75f, 0.75f, 1f)
        GuiRenderUtils.drawString(
            "Upgrade",
            (FFGuideGUI.guiLeft + 45) * inverseScale,
            (FFGuideGUI.guiTop + 5) * inverseScale
        )
        GuiRenderUtils.drawString(
            "Item",
            (FFGuideGUI.guiLeft + 190) * inverseScale,
            (FFGuideGUI.guiTop + 5) * inverseScale
        )
        GuiRenderUtils.drawString(
            "FF increase",
            (FFGuideGUI.guiLeft + 240) * inverseScale,
            (FFGuideGUI.guiTop + 5) * inverseScale
        )
        GuiRenderUtils.drawString(
            "Cost/FF",
            (FFGuideGUI.guiLeft + 290) * inverseScale,
            (FFGuideGUI.guiTop + 5) * inverseScale
        )
        GuiRenderUtils.drawString(
            "Total",
            (FFGuideGUI.guiLeft + 330) * inverseScale,
            (FFGuideGUI.guiTop + 5) * inverseScale
        )

        val upgradeList =
            if (FFGuideGUI.currentCrop == null) FortuneUpgrades.genericUpgrades else FortuneUpgrades.cropSpecificUpgrades
        listLength = upgradeList.size
        for ((index, upgrade) in upgradeList.withIndex()) {
            if (adjustedY + 25 * index < FFGuideGUI.guiTop + 20) continue
            if (adjustedY + 25 * index > FFGuideGUI.guiTop + 160) continue
            val upgradeItem = upgrade.requiredItem.let { NEUItems.getItemStack(it) }
            var formattedUpgrade = upgradeItem.nameWithEnchantment ?: return
            if (adjustedY + 25 * index - 5 < FFGuideGUI.lastClickedHeight && FFGuideGUI.lastClickedHeight < adjustedY + 25 * index + 10) {
                FFGuideGUI.lastClickedHeight = 0
                BazaarApi.searchForBazaarItem(formattedUpgrade, upgrade.itemQuantity)
            }
            if (upgrade.itemQuantity != 1) {
                formattedUpgrade = "$formattedUpgrade §fx${upgrade.itemQuantity}"
            }
            GuiRenderUtils.drawTwoLineString(
                upgrade.description,
                (FFGuideGUI.guiLeft + 15) * inverseScale,
                (adjustedY + 25 * index) * inverseScale
            )
            GuiRenderUtils.renderItemAndTip(
                FFGuideGUI.tooltipToDisplay,
                upgradeItem,
                (FFGuideGUI.guiLeft + 155) * inverseScale,
                (adjustedY + 25 * index - 5) * inverseScale,
                mouseX * inverseScale,
                mouseY * inverseScale,
                0x00FFFFFF
            )
            GuiRenderUtils.drawString(
                formattedUpgrade,
                (FFGuideGUI.guiLeft + 180) * inverseScale,
                (adjustedY + 25 * index) * inverseScale
            )
            GuiRenderUtils.drawString(
                "§a${DecimalFormat("0.##").format(upgrade.fortuneIncrease)}",
                (FFGuideGUI.guiLeft + 270) * inverseScale,
                (adjustedY + 25 * index) * inverseScale
            )
            GuiRenderUtils.drawString(
                "§6" + upgrade.costPerFF?.let { NumberUtil.format(it) },
                (FFGuideGUI.guiLeft + 300) * inverseScale,
                (adjustedY + 25 * index) * inverseScale
            )
            GuiRenderUtils.drawString(
                ("§6" + upgrade.cost?.let { NumberUtil.format(it) }),
                (FFGuideGUI.guiLeft + 335) * inverseScale,
                (adjustedY + 25 * index) * inverseScale
            )
        }
        GlStateManager.scale(inverseScale, inverseScale, 1f)
        scrollScreen()
    }

    private fun scrollScreen() {
        scrollVelocity += FFGuideGUI.lastMouseScroll / 48.0
        scrollVelocity *= 0.95
        pageScroll += scrollVelocity.toInt() + FFGuideGUI.lastMouseScroll / 24

        FFGuideGUI.noMouseScrollFrames++

        if (FFGuideGUI.noMouseScrollFrames >= maxNoInputFrames) {
            scrollVelocity *= 0.75
        }

        if (pageScroll > 0) {
            pageScroll = 0
        }

        pageScroll = MathHelper.clamp_int(pageScroll, -(listLength * 15 - 15), 0)
        FFGuideGUI.lastMouseScroll = 0
    }
}
