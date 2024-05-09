package at.hannibal2.skyhanni.features.garden.fortuneguide.pages

import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.features.garden.fortuneguide.FarmingItems
import at.hannibal2.skyhanni.features.garden.fortuneguide.FortuneUpgrade
import at.hannibal2.skyhanni.features.garden.fortuneguide.FortuneUpgrades
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.guide.GuideScrollPage
import at.hannibal2.skyhanni.utils.renderables.Renderable
import java.text.DecimalFormat

class UpgradePage(sizeX: Int, sizeY: Int, paddingX: Int = 15, paddingY: Int = 7) : GuideScrollPage(
    sizeX,
    sizeY,
    paddingX,
    paddingY,
    marginY = 10,
    hasHeader = true,
) {
    override fun onEnter() {
        FFGuideGUI.currentCrop?.let {
            FortuneUpgrades.getCropSpecific(it.farmingItem.getItemOrNull())
        } ?: {
            FortuneUpgrades.getCropSpecific(null) // TODO
        }

        FarmingItems.resetClickState()
        update(
            content = buildList {
                add(header())
                val upgradeList = if (FFGuideGUI.currentCrop == null)
                    FortuneUpgrades.genericUpgrades
                else
                    FortuneUpgrades.cropSpecificUpgrades
                addAll(upgradeList.map { upgrade -> upgrade.print() })
            }
        )
    }

    private fun header() = listOf("Upgrade", "", "Item", "FF", "Cost/FF", "Total").map {
        Renderable.string(
            it,
            0.9,
            horizontalAlign = HorizontalAlignment.CENTER
        )
    }

    private fun FortuneUpgrade.print() = buildList {
        add(
            Renderable.wrappedString(
                description,
                136,
                0.75,
                verticalAlign = VerticalAlignment.CENTER
            )
        )
        add(
            Renderable.itemStackWithTip(
                requiredItem.getItemStack(),
                8.0 / 9.0,
                verticalAlign = VerticalAlignment.CENTER
            )
        )
        add(
            Renderable.wrappedString(
                requiredItem.itemName.let { if (itemQuantity == 1) it else "$it §fx$itemQuantity" }, // TODO wtf
                70,
                0.75,
                verticalAlign = VerticalAlignment.CENTER
            )
        )
        add(
            Renderable.string(
                "§a${DecimalFormat("0.##").format(fortuneIncrease)}",
                horizontalAlign = HorizontalAlignment.CENTER,
                verticalAlign = VerticalAlignment.CENTER
            )
        ) // TODO cleaner formating
        add(
            Renderable.string(
                "§6" + costPerFF?.let { NumberUtil.format(it) },
                horizontalAlign = HorizontalAlignment.CENTER,
                verticalAlign = VerticalAlignment.CENTER
            )
        )
        add(
            Renderable.string(
                "§6" + cost?.let { NumberUtil.format(it) },
                horizontalAlign = HorizontalAlignment.CENTER,
                verticalAlign = VerticalAlignment.CENTER
            )
        )
    }
}

