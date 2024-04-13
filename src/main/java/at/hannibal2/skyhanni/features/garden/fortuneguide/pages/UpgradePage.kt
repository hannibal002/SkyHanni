package at.hannibal2.skyhanni.features.garden.fortuneguide.pages

import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
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
    hasHeader = true,
) {
    override fun onEnter() = update(
        content = buildList {
            add(listOf("Upgrade", "", "Item", "FF", "Cost/FF", "Total").map {
                Renderable.string(
                    it,
                    0.9,
                    horizontalAlign = HorizontalAlignment.CENTER
                )
            })
            val upgradeList =
                if (FFGuideGUI.currentCrop == null) FortuneUpgrades.genericUpgrades else FortuneUpgrades.cropSpecificUpgrades
            addAll(upgradeList.map { upgrade ->
                buildList {
                    add(
                        Renderable.wrappedString(
                            upgrade.description,
                            136,
                            0.75,
                            verticalAlign = VerticalAlignment.CENTER
                        )
                    )
                    add(
                        Renderable.itemStackWithTip(
                            upgrade.requiredItem.getItemStack(),
                            8.0 / 9.0,
                            verticalAlign = VerticalAlignment.CENTER
                        )
                    ) // TODO fix tooltip + use NeuInternalNames
                    add(Renderable.wrappedString(upgrade.requiredItem.getItemStack().itemName.let { if (upgrade.itemQuantity == 1) it else "$it §fx${upgrade.itemQuantity}" }
                        ?: "", 70, 0.75, verticalAlign = VerticalAlignment.CENTER)) // TODO wtf
                    add(
                        Renderable.string(
                            "§a${DecimalFormat("0.##").format(upgrade.fortuneIncrease)}",
                            horizontalAlign = HorizontalAlignment.CENTER,
                            verticalAlign = VerticalAlignment.CENTER
                        )
                    ) // TODO cleaner formating
                    add(
                        Renderable.string(
                            "§6" + upgrade.costPerFF?.let { NumberUtil.format(it) },
                            horizontalAlign = HorizontalAlignment.CENTER,
                            verticalAlign = VerticalAlignment.CENTER
                        )
                    )
                    add(
                        Renderable.string(
                            "§6" + upgrade.cost?.let { NumberUtil.format(it) },
                            horizontalAlign = HorizontalAlignment.CENTER,
                            verticalAlign = VerticalAlignment.CENTER
                        )
                    )
                }
            })
        }
    )
}

