package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.fortuneguide.pages.CropPage
import at.hannibal2.skyhanni.features.garden.fortuneguide.pages.OverviewPage
import at.hannibal2.skyhanni.features.garden.fortuneguide.pages.UpgradePage
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.guide.GuideGui
import at.hannibal2.skyhanni.utils.guide.GuideTab
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

class FFGuideGui : GuideGui<FFGuideGui.FortuneGuidePage>(FortuneGuidePage.OVERVIEW) {

    override val sizeX = 360
    override val sizeY = 225

    @SkyHanniModule
    companion object {

        @JvmStatic
        fun onCommand() {
            if (!SkyBlockUtils.inSkyBlock) {
                ChatUtils.userError("Join SkyBlock to open the fortune guide!")
            } else {
                open()
            }
        }

        fun isInGui() = Minecraft.getMinecraft().currentScreen is FFGuideGui

        fun open() {
            CaptureFarmingGear.captureFarmingGear()
            CaptureFarmingGear.removeInvalidItems()
            SkyHanniMod.screenToOpen = FFGuideGui()
        }

        fun updateDisplay() {
            with(Minecraft.getMinecraft().currentScreen) {
                if (this !is FFGuideGui) return
                this.refreshPage()
            }
        }

        @HandleEvent
        fun onCommandRegistration(event: CommandRegistrationEvent) {
            event.registerBrigadier("ff") {
                description = "Opens the Farming Fortune Guide"
                callback { onCommand() }
            }
        }
    }

    /** Value for which crop page is active */
    private var currentCrop: CropType? = null

    init {
        FFStats.loadFFData()
        FortuneUpgrades.generateGenericUpgrades()

        FarmingItemType.setDefaultPet()

        pageList = mapOf(
            FortuneGuidePage.OVERVIEW to OverviewPage(sizeX, sizeY),
            FortuneGuidePage.CROP to CropPage({ currentCrop ?: error("current crop is null") }, sizeX, sizeY),
            FortuneGuidePage.UPGRADES to UpgradePage({ currentCrop }, sizeX, sizeY - 2),
        )
        verticalTabs = listOf(
            vTab(ItemStack(Items.gold_ingot), StringRenderable("§eBreakdown")) {
                currentPage = if (currentCrop == null) FortuneGuidePage.OVERVIEW else FortuneGuidePage.CROP
            },
            vTab(
                ItemStack(Items.map),
                StringRenderable("§eUpgrades"),
            ) {
                currentPage = FortuneGuidePage.UPGRADES
            },
        )
        horizontalTabs = buildList {
            add(
                hTab(ItemStack(Blocks.grass), StringRenderable("§eOverview")) {
                    currentCrop = null

                    it.pageSwitchHorizontal()
                },
            )
            for (crop in CropType.entries) {
                add(
                    hTab(crop.icon, StringRenderable("§e${crop.cropName}")) {
                        currentCrop = crop

                        it.pageSwitchHorizontal()
                    },
                )
            }
        }
        horizontalTabs.firstOrNull()?.fakeClick()
        verticalTabs.firstOrNull()?.fakeClick()
    }

    private fun GuideTab.pageSwitchHorizontal() {
        if (isSelected()) {
            verticalTabs.first { it != lastVerticalTabWrapper.tab }.fakeClick() // Double Click Logic
        } else {
            lastVerticalTabWrapper.tab?.fakeClick() // First Click Logic
        }
    }

    enum class FortuneGuidePage {
        OVERVIEW,
        CROP,
        UPGRADES,
    }
}
