package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.fortuneguide.pages.CropPage
import at.hannibal2.skyhanni.features.garden.fortuneguide.pages.OverviewPage
import at.hannibal2.skyhanni.features.garden.fortuneguide.pages.UpgradePage
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.guide.GuideGUI
import at.hannibal2.skyhanni.utils.guide.GuidePage
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import org.lwjgl.input.Mouse
import java.io.IOException

open class FFGuideGUI : GuideGUI<FFGuideGUI.FortuneGuidePage>(FortuneGuidePage.OVERVIEW) {

    override val sizeX = 360
    override val sizeY = 180

    companion object {

        var guiLeft = 0
        var guiTop = 0

        var currentCrop: CropType? = null

        // todo set this to what they have equip
        var currentPet = FarmingItems.ELEPHANT
        var currentArmor = 0
        var currentEquipment = 0

        var mouseX = 0
        var mouseY = 0
        var lastMouseScroll = 0
        var noMouseScrollFrames = 0
        var lastClickedHeight = 0

        var tooltipToDisplay = mutableListOf<String>()

        fun isInGui() = Minecraft.getMinecraft().currentScreen is FFGuideGUI

        fun FarmingItems.getItem(): ItemStack {
            val fortune = GardenAPI.storage?.fortune ?: return getFallbackItem(this)

            val farmingItems = fortune.farmingItems
            farmingItems[this]?.let { return it }

            val fallbackItem = getFallbackItem(this)
            farmingItems[this] = fallbackItem
            return fallbackItem
        }

        private val fallbackItems = mutableMapOf<FarmingItems, ItemStack>()

        fun getFallbackItem(item: FarmingItems) = fallbackItems.getOrPut(item) {
            val name = "§cNo saved ${item.name.lowercase().replace("_", " ")}"
            ItemStack(Blocks.barrier).setStackDisplayName(name)
        }

        fun isFallbackItem(item: ItemStack) = item.name!!.startsWith("§cNo saved ")
    }

    init {
        FFStats.loadFFData()
        FortuneUpgrades.generateGenericUpgrades()

        if (currentCrop != null) {
            for (item in FarmingItems.entries) {
                if (item.name == currentCrop?.name) {
                    FFStats.getCropStats(currentCrop!!, item.getItem())
                }
            }
        }


        // New Code
        pageList = mapOf(
            FortuneGuidePage.OVERVIEW to OverviewPage(),
            FortuneGuidePage.CROP to CropPage(),
            FortuneGuidePage.UPGRADES to UpgradePage(sizeX, sizeY),
        )
        verticalTabs = listOf(
            vTab(ItemStack(Items.gold_ingot), Renderable.string("§eBreakdown")) {
                currentPage = if (currentCrop == null) FortuneGuidePage.OVERVIEW else FortuneGuidePage.CROP
            },
            vTab(ItemStack(Items.map), Renderable.string("§eUpgrades")) {
                currentPage = FortuneGuidePage.UPGRADES
            }
        )
        horizontalTabs = buildList {
            add(hTab(ItemStack(Blocks.grass), Renderable.string("§eOverview")) {
                currentCrop = null

                // Double Click Logic
                if (it.isSelected()) {
                    verticalTabs.first { it != lastVerticalTabWrapper.tab }.fakeClick()
                }
            })
            for (crop in CropType.entries) {
                add(hTab(crop.icon, Renderable.string("§e${crop.cropName}")) {

                    currentCrop = crop
                    for (item in FarmingItems.entries) {
                        if (item.name == crop.name) {
                            FFStats.getCropStats(crop, item.getItem())
                            FortuneUpgrades.getCropSpecific(item.getItem())
                        }
                    }

                    // Double Click Logic
                    if (it.isSelected()) {
                        verticalTabs.first { it != lastVerticalTabWrapper.tab }.fakeClick()
                    }
                })
            }
        }
        horizontalTabs.firstOrNull()?.fakeClick()
        verticalTabs.firstOrNull()?.fakeClick()
    }

    override fun drawScreen(unusedX: Int, unusedY: Int, partialTicks: Float) {
        super.drawScreen(unusedX, unusedY, partialTicks)
        guiLeft = (width - sizeX) / 2
        guiTop = (height - sizeY) / 2

        mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth
        mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1

        if (this.currentPage == FortuneGuidePage.UPGRADES) {
            //
        } else {
            GuiRenderUtils.drawStringCentered("§7SkyHanni", guiLeft + 325, guiTop + 170)
            if (currentCrop == null) {
                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.HELMET.getItem(), guiLeft + 142, guiTop + 5, mouseX, mouseY,
                    if (currentArmor == 1) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
                )
                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.CHESTPLATE.getItem(), guiLeft + 162, guiTop + 5, mouseX, mouseY,
                    if (currentArmor == 2) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
                )
                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.LEGGINGS.getItem(), guiLeft + 182, guiTop + 5, mouseX, mouseY,
                    if (currentArmor == 3) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
                )
                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.BOOTS.getItem(), guiLeft + 202, guiTop + 5, mouseX, mouseY,
                    if (currentArmor == 4) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
                )

                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.NECKLACE.getItem(), guiLeft + 262, guiTop + 5, mouseX, mouseY,
                    if (currentEquipment == 1) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
                )
                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.CLOAK.getItem(), guiLeft + 282, guiTop + 5, mouseX, mouseY,
                    if (currentEquipment == 2) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
                )
                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.BELT.getItem(), guiLeft + 302, guiTop + 5, mouseX, mouseY,
                    if (currentEquipment == 3) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
                )
                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.BRACELET.getItem(), guiLeft + 322, guiTop + 5, mouseX, mouseY,
                    if (currentEquipment == 4) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
                )

                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.ELEPHANT.getItem(), guiLeft + 142, guiTop + 130, mouseX, mouseY,
                    if (currentPet == FarmingItems.ELEPHANT) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
                )
                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.MOOSHROOM_COW.getItem(), guiLeft + 162, guiTop + 130, mouseX, mouseY,
                    if (currentPet == FarmingItems.MOOSHROOM_COW) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
                )
                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.RABBIT.getItem(), guiLeft + 182, guiTop + 130, mouseX, mouseY,
                    if (currentPet == FarmingItems.RABBIT) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
                )
                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.BEE.getItem(), guiLeft + 202, guiTop + 130, mouseX, mouseY,
                    if (currentPet == FarmingItems.BEE) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
                )
            } else {
                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.ELEPHANT.getItem(), guiLeft + 142, guiTop + 160, mouseX, mouseY,
                    if (currentPet == FarmingItems.ELEPHANT) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
                )
                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.MOOSHROOM_COW.getItem(), guiLeft + 162, guiTop + 160, mouseX, mouseY,
                    if (currentPet == FarmingItems.MOOSHROOM_COW) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
                )
                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.RABBIT.getItem(), guiLeft + 182, guiTop + 160, mouseX, mouseY,
                    if (currentPet == FarmingItems.RABBIT) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
                )
                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.BEE.getItem(), guiLeft + 202, guiTop + 160, mouseX, mouseY,
                    if (currentPet == FarmingItems.BEE) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
                )

                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.HELMET.getItem(), guiLeft + 162, guiTop + 80, mouseX, mouseY
                )
                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.CHESTPLATE.getItem(), guiLeft + 162, guiTop + 100, mouseX, mouseY
                )
                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.LEGGINGS.getItem(), guiLeft + 162, guiTop + 120, mouseX, mouseY
                )
                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.BOOTS.getItem(), guiLeft + 162, guiTop + 140, mouseX, mouseY
                )

                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.NECKLACE.getItem(), guiLeft + 182, guiTop + 80, mouseX, mouseY
                )
                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.CLOAK.getItem(), guiLeft + 182, guiTop + 100, mouseX, mouseY
                )
                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.BELT.getItem(), guiLeft + 182, guiTop + 120, mouseX, mouseY
                )
                GuiRenderUtils.renderItemAndTip(
                    tooltipToDisplay,
                    FarmingItems.BRACELET.getItem(), guiLeft + 182, guiTop + 140, mouseX, mouseY
                )
            }
        }

        GlStateManager.popMatrix()

        if (tooltipToDisplay.isNotEmpty()) {
            GuiRenderUtils.drawTooltip(tooltipToDisplay, mouseX, mouseY, height)
            tooltipToDisplay.clear()
        }
    }

    override fun handleMouseInput() {
        super.handleMouseInput()

        if (Mouse.getEventButtonState()) {
            mouseClickEvent()
        }
        if (!Mouse.getEventButtonState() && Mouse.getEventDWheel() != 0) {
            lastMouseScroll = Mouse.getEventDWheel()
            noMouseScrollFrames = 0
        }
    }

    @Throws(IOException::class)
    fun mouseClickEvent() {
        var x = guiLeft + 15
        var y = guiTop - 28
        /* if (isMouseIn(x, y, 25, 28)) {
            // SoundUtils.playClickSound()
            if (currentCrop != null) {
                currentCrop = null
                if (this.currentPage != FortuneGuidePage.UPGRADES) {
                    this.currentPage = FortuneGuidePage.OVERVIEW
                }
            } else {
                if (this.currentPage == FortuneGuidePage.UPGRADES) {
                    this.currentPage = FortuneGuidePage.OVERVIEW
                } else {
                    this.currentPage = FortuneGuidePage.UPGRADES
                }
            }
        }
        for (crop in CropType.entries) {
            x += 30
            if (isMouseIn(x, y, 25, 28)) {
                // SoundUtils.playClickSound()
                if (currentCrop != crop) {
                    currentCrop = crop
                    if (this.currentPage == FortuneGuidePage.OVERVIEW) {
                        this.currentPage = FortuneGuidePage.CROP
                    }
                    for (item in FarmingItems.entries) {
                        if (item.name == crop.name) {
                            FFStats.getCropStats(crop, item.getItem())
                            FortuneUpgrades.getCropSpecific(item.getItem())
                        }
                    }
                } else {
                    if (this.currentPage == FortuneGuidePage.CROP) {
                        this.currentPage = FortuneGuidePage.UPGRADES
                        for (item in FarmingItems.entries) {
                            if (item.name == crop.name) {
                                FortuneUpgrades.getCropSpecific(item.getItem())
                            }
                        }
                    } else {
                        this.currentPage = FortuneGuidePage.CROP
                        for (item in FarmingItems.entries) {
                            if (item.name == crop.name) {
                                FFStats.getCropStats(crop, item.getItem())
                            }
                        }
                    }
                }
            }
        }

        x = guiLeft - 28
        y = guiTop + 15
        if (isMouseIn(x, y, 28, 25) &&
            this.currentPage != FortuneGuidePage.CROP && this.currentPage != FortuneGuidePage.OVERVIEW
        ) {
            SoundUtils.playClickSound()
            this.currentPage = if (currentCrop == null) {
                FortuneGuidePage.OVERVIEW
            } else {
                FortuneGuidePage.CROP
            }
        }
        y += 30
        if (isMouseIn(x, y, 28, 25) && this.currentPage != FortuneGuidePage.UPGRADES) {
            this.currentPage = FortuneGuidePage.UPGRADES
            SoundUtils.playClickSound()
        } */

        if (this.currentPage != FortuneGuidePage.UPGRADES) {
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
        } else {
            if (isMouseIn(guiLeft, guiTop, sizeX, sizeY)) {
                lastClickedHeight = mouseY
            }
        }
    }

    private fun isMouseInRect(left: Int, top: Int) = isMouseIn(left, top, 16, 16)

    private fun isMouseIn(x: Int, y: Int, width: Int, height: Int) =
        GuiRenderUtils.isPointInRect(mouseX, mouseY, x, y, width, height)

    enum class FortuneGuidePage {
        OVERVIEW,
        CROP,
        UPGRADES
    }

    abstract class FFGuidePage : GuidePage() {
        override fun onSwitch() {}
    }
}
