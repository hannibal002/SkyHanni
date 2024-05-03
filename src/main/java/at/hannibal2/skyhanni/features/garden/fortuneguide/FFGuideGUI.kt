package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.fortuneguide.pages.CropPage
import at.hannibal2.skyhanni.features.garden.fortuneguide.pages.OverviewPage
import at.hannibal2.skyhanni.features.garden.fortuneguide.pages.UpgradePage
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.guide.GuideGUI
import at.hannibal2.skyhanni.utils.guide.GuideTab
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import org.lwjgl.input.Mouse
import java.io.IOException

class FFGuideGUI : GuideGUI<FFGuideGUI.FortuneGuidePage>(FortuneGuidePage.OVERVIEW) {

    override val sizeX = 360
    override val sizeY = 180

    companion object {

        var guiLeft = 0
        var guiTop = 0

        var currentCrop: CropType? = null

        // todo set this to what they have equip
        val currentPet get() = FarmingItems.currentPet
        val currentArmor
            get() = when (FarmingItems.currentArmor) {
                null -> 0
                FarmingItems.HELMET -> 1
                FarmingItems.CHESTPLATE -> 2
                FarmingItems.LEGGINGS -> 3
                FarmingItems.BOOTS -> 4
                else -> -1
            }
        val currentEquipment
            get() = when (FarmingItems.currentEquip) {
                null -> 0
                FarmingItems.NECKLACE -> 1
                FarmingItems.CLOAK -> 2
                FarmingItems.BELT -> 3
                FarmingItems.BRACELET -> 4
                else -> -1
            }

        var mouseX = 0
        var mouseY = 0

        fun isInGui() = Minecraft.getMinecraft().currentScreen is FFGuideGUI

        private val fallbackItems = mutableMapOf<FarmingItems, ItemStack>()

        fun getFallbackItem(item: FarmingItems) = fallbackItems.getOrPut(item) {
            val name = "§cNo saved ${item.name.lowercase().replace("_", " ")}"
            ItemStack(Blocks.barrier).setStackDisplayName(name)
        }

        fun isFallbackItem(item: ItemStack) = item.name.startsWith("§cNo saved ")

        fun open() {
            CaptureFarmingGear.captureFarmingGear()
            SkyHanniMod.screenToOpen = FFGuideGUI()
        }

        fun updateDisplay() {
            with(Minecraft.getMinecraft().currentScreen) {
                if (this !is FFGuideGUI) return
                this.refreshPage()
            }
        }
    }

    init {
        FFStats.loadFFData()
        FortuneUpgrades.generateGenericUpgrades()

        currentCrop?.farmingItem?.let { item ->
            FFStats.getCropStats(currentCrop!!, item.getItem())
        }

        // New Code

        FarmingItems.setDefaultPet()

        pageList = mapOf(
            FortuneGuidePage.OVERVIEW to OverviewPage(sizeX, sizeY),
            FortuneGuidePage.CROP to CropPage(sizeX, sizeY),
            FortuneGuidePage.UPGRADES to UpgradePage(sizeX, sizeY),
        )
        verticalTabs = listOf(
            vTab(ItemStack(Items.gold_ingot), Renderable.string("§eBreakdown")) {
                currentPage = if (currentCrop == null) FortuneGuidePage.OVERVIEW else FortuneGuidePage.CROP
            },
            vTab(ItemStack(Items.map), Renderable.string("§eUpgrades")) {
                currentPage = FortuneGuidePage.UPGRADES
            })
        horizontalTabs = buildList {
            add(
                hTab(ItemStack(Blocks.grass), Renderable.string("§eOverview")) {
                    currentCrop = null

                    it.pageSwitchHorizontal()
                }
            )
            for (crop in CropType.entries) {
                add(
                    hTab(crop.icon, Renderable.string("§e${crop.cropName}")) {
                        currentCrop = crop

                        val item = crop.farmingItem
                        FFStats.getCropStats(crop, item.getItemOrNull())
                        FortuneUpgrades.getCropSpecific(item.getItemOrNull())

                        it.pageSwitchHorizontal()
                    }
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

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)
        guiLeft = (width - sizeX) / 2
        guiTop = (height - sizeY) / 2
    }

    override fun handleMouseInput() {
        super.handleMouseInput()

        if (Mouse.getEventButtonState()) {
            mouseClickEvent()
        }
    }

    @Throws(IOException::class)
    fun mouseClickEvent() {
        var x = guiLeft + 15
        var y = guiTop - 28

        /* if (this.currentPage != FortuneGuidePage.UPGRADES) {
            if (currentCrop == null) {
                when {
                    isMouseInRect(guiLeft + 142, guiTop + 130) && currentPet != FarmingItems.ELEPHANT -> {
                        SoundUtils.playClickSound()
                        currentPet = FarmingItems.ELEPHANT
                        FFStats.getTotalFF()
                    }

                    isMouseInRect(guiLeft + 162, guiTop + 130) && currentPet != FarmingItems.MOOSHROOM_COW -> {
                        SoundUtils.playClickSound()
                        currentPet = FarmingItems.MOOSHROOM_COW
                        FFStats.getTotalFF()
                    }

                    isMouseInRect(guiLeft + 182, guiTop + 130) && currentPet != FarmingItems.RABBIT -> {
                        SoundUtils.playClickSound()
                        currentPet = FarmingItems.RABBIT
                        FFStats.getTotalFF()
                    }

                    isMouseInRect(guiLeft + 202, guiTop + 130) && currentPet != FarmingItems.BEE -> {
                        SoundUtils.playClickSound()
                        currentPet = FarmingItems.BEE
                        FFStats.getTotalFF()
                    }

                    isMouseInRect(guiLeft + 142, guiTop + 5) -> {
                        SoundUtils.playClickSound()
                        currentArmor = if (currentArmor == 1) 0 else 1
                    }

                    isMouseInRect(guiLeft + 162, guiTop + 5) -> {
                        SoundUtils.playClickSound()
                        currentArmor = if (currentArmor == 2) 0 else 2
                    }

                    isMouseInRect(guiLeft + 182, guiTop + 5) -> {
                        SoundUtils.playClickSound()
                        currentArmor = if (currentArmor == 3) 0 else 3
                    }

                    isMouseInRect(guiLeft + 202, guiTop + 5) -> {
                        SoundUtils.playClickSound()
                        currentArmor = if (currentArmor == 4) 0 else 4
                    }

                    isMouseInRect(guiLeft + 262, guiTop + 5) -> {
                        SoundUtils.playClickSound()
                        currentEquipment = if (currentEquipment == 1) 0 else 1
                    }

                    isMouseInRect(guiLeft + 282, guiTop + 5) -> {
                        SoundUtils.playClickSound()
                        currentEquipment = if (currentEquipment == 2) 0 else 2
                    }

                    isMouseInRect(guiLeft + 302, guiTop + 5) -> {
                        SoundUtils.playClickSound()
                        currentEquipment = if (currentEquipment == 3) 0 else 3
                    }

                    isMouseInRect(guiLeft + 322, guiTop + 5) -> {
                        SoundUtils.playClickSound()
                        currentEquipment = if (currentEquipment == 4) 0 else 4
                    }
                }
            } else {
                when {
                    isMouseInRect(guiLeft + 142, guiTop + 160) && currentPet != FarmingItems.ELEPHANT -> {
                        SoundUtils.playClickSound()
                        currentPet = FarmingItems.ELEPHANT
                        FFStats.getTotalFF()
                    }

                    isMouseInRect(guiLeft + 162, guiTop + 160) && currentPet != FarmingItems.MOOSHROOM_COW -> {
                        SoundUtils.playClickSound()
                        currentPet = FarmingItems.MOOSHROOM_COW
                        FFStats.getTotalFF()
                    }

                    isMouseInRect(guiLeft + 182, guiTop + 160) && currentPet != FarmingItems.RABBIT -> {
                        SoundUtils.playClickSound()
                        currentPet = FarmingItems.RABBIT
                        FFStats.getTotalFF()
                    }

                    isMouseInRect(guiLeft + 202, guiTop + 160) && currentPet != FarmingItems.BEE -> {
                        SoundUtils.playClickSound()
                        currentPet = FarmingItems.BEE
                        FFStats.getTotalFF()
                    }
                }
            }
        } */
    }

    private fun isMouseInRect(left: Int, top: Int) = isMouseIn(left, top, 16, 16)

    private fun isMouseIn(x: Int, y: Int, width: Int, height: Int) =
        GuiRenderUtils.isPointInRect(mouseX, mouseY, x, y, width, height)

    enum class FortuneGuidePage {
        OVERVIEW,
        CROP,
        UPGRADES,
    }
}
