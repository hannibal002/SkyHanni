package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.fortuneguide.pages.CropPage
import at.hannibal2.skyhanni.features.garden.fortuneguide.pages.OverviewPage
import at.hannibal2.skyhanni.features.garden.fortuneguide.pages.UpgradePage
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.guide.GuideGui
import at.hannibal2.skyhanni.utils.guide.GuideTab
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks

class FFGuideGui : GuideGui<FFGuideGui.FortuneGuidePage>(FortuneGuidePage.OVERVIEW) {

    override val sizeX = 360
    override val sizeY = 255

    @SkyHanniModule
    companion object {

        fun isInGui() = Minecraft.getInstance().screen is FFGuideGui

        private fun open() {
            CaptureFarmingGear.captureFarmingGear()
            CaptureFarmingGear.removeInvalidItems()
            SkyHanniMod.screenToOpen = FFGuideGui()
        }

        fun updateDisplay() {
            with(Minecraft.getInstance().screen) {
                if (this !is FFGuideGui) return
                this.refreshPage()
            }
        }

        @HandleEvent
        fun onCommandRegistration(event: CommandRegistrationEvent) {
            event.registerBrigadier("ff") {
                description = "Opens the Farming Fortune Guide"
                literalCallback("old") {
                    if (!SkyBlockUtils.inSkyBlock) {
                        ChatUtils.userError("Join SkyBlock to open the fortune guide!")
                    } else {
                        ChatUtils.chat("The old ff guide is NOT updated, it will be missing many upgrades")
                        open()
                    }

                }
                simpleCallback {
                    if (!SkyBlockUtils.inSkyBlock) {
                        ChatUtils.userError("Join SkyBlock to open the fortune guide!")
                    } else {
                        val name = PlayerUtils.getName()
                        var profile = HypixelData.profileName
                        if (profile.isNotEmpty()) profile += "/"
                        ChatUtils.clickableLinkChat(
                            "§cSkyHannis /ff display is no longer being developed! " +
                                "§6Click §bhere §6to see your updated fortune progress and cheapest upgrades on elitebot.dev instead!",
                            "https://elitebot.dev/@$name/${profile}fortune?utm_source=SkyHanni#fortune"
                        )
                    }
                }
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
            vTab(ItemStack(Items.GOLD_INGOT), Renderable.text("§eBreakdown")) {
                currentPage = if (currentCrop == null) FortuneGuidePage.OVERVIEW else FortuneGuidePage.CROP
            },
            vTab(
                ItemStack(Items.MAP),
                Renderable.text("§eUpgrades"),
            ) {
                currentPage = FortuneGuidePage.UPGRADES
            },
        )
        horizontalTabs = buildList {
            add(
                hTab(ItemStack(Blocks.GRASS_BLOCK), Renderable.text("§eOverview")) {
                    currentCrop = null

                    it.pageSwitchHorizontal()
                },
            )
            for (crop in CropType.entries) {
                add(
                    hTab(crop.icon, Renderable.text("§e${crop.cropName}")) {
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
