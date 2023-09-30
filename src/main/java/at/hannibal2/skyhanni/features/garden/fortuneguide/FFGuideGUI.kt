package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.fortuneguide.pages.CropPage
import at.hannibal2.skyhanni.features.garden.fortuneguide.pages.OverviewPage
import at.hannibal2.skyhanni.features.garden.fortuneguide.pages.UpgradePage
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import org.lwjgl.input.Mouse
import java.io.IOException

open class FFGuideGUI : GuiScreen() {
    companion object {
        val pages = mutableMapOf<FortuneGuidePage, FFGuidePage>()

        var guiLeft = 0
        var guiTop = 0
        var screenHeight = 0

        const val sizeX = 360
        const val sizeY = 180

        var selectedPage = FortuneGuidePage.OVERVIEW
        var currentCrop: CropType? = null

        //todo set this to what they have equip
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
            val fortune = GardenAPI.config?.fortune ?: return getFallbackItem(this)

            val farmingItems = fortune.farmingItems
            farmingItems[this]?.let { return it }

            val fallbackItem = getFallbackItem(this)
            farmingItems[this] = fallbackItem
            return fallbackItem
        }

        fun getFallbackItem(item: FarmingItems): ItemStack =
            ItemStack(Blocks.barrier).setStackDisplayName("§cNo saved ${item.name.lowercase().replace("_", " ")}")
    }

    init {
        FFStats.loadFFData()
        FortuneUpgrades.generateGenericUpgrades()

        pages[FortuneGuidePage.OVERVIEW] = OverviewPage()
        pages[FortuneGuidePage.CROP] = CropPage()
        pages[FortuneGuidePage.UPGRADES] = UpgradePage()

        if (currentCrop != null) {
            for (item in FarmingItems.entries) {
                if (item.name == currentCrop?.name) {
                    FFStats.getCropStats(currentCrop!!, item.getItem())
                }
            }
        }
    }

    override fun drawScreen(unusedX: Int, unusedY: Int, partialTicks: Float) {
        super.drawScreen(unusedX, unusedY, partialTicks)
        drawDefaultBackground()
        screenHeight = height
        guiLeft = (width - sizeX) / 2
        guiTop = (height - sizeY) / 2

        mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth
        mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1

        GlStateManager.pushMatrix()
        drawRect(guiLeft, guiTop, guiLeft + sizeX, guiTop + sizeY, 0x50000000)
        renderTabs()

        if (selectedPage == FortuneGuidePage.UPGRADES) {
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
        pages[selectedPage]?.drawPage(mouseX, mouseY, partialTicks)

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
        if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x, y, 25, 28)) {
            SoundUtils.playClickSound()
            if (currentCrop != null) {
                currentCrop = null
                if (selectedPage != FortuneGuidePage.UPGRADES) {
                    selectedPage = FortuneGuidePage.OVERVIEW
                }
            } else {
                if (selectedPage == FortuneGuidePage.UPGRADES) {
                    selectedPage = FortuneGuidePage.OVERVIEW
                } else {
                    selectedPage = FortuneGuidePage.UPGRADES
                }
            }
        }
        for (crop in CropType.entries) {
            x += 30
            if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x, y, 25, 28)) {
                SoundUtils.playClickSound()
                if (currentCrop != crop) {
                    currentCrop = crop
                    if (selectedPage == FortuneGuidePage.OVERVIEW) {
                        selectedPage = FortuneGuidePage.CROP
                    }
                    for (item in FarmingItems.entries) {
                        if (item.name == crop.name) {
                            FFStats.getCropStats(crop, item.getItem())
                            FortuneUpgrades.getCropSpecific(item.getItem())
                        }
                    }
                } else {
                    if (selectedPage == FortuneGuidePage.CROP) {
                        selectedPage = FortuneGuidePage.UPGRADES
                        for (item in FarmingItems.entries) {
                            if (item.name == crop.name) {
                                FortuneUpgrades.getCropSpecific(item.getItem())
                            }
                        }
                    } else {
                        selectedPage = FortuneGuidePage.CROP
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
        if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x, y, 28, 25) && selectedPage != FortuneGuidePage.CROP && selectedPage != FortuneGuidePage.OVERVIEW) {
                SoundUtils.playClickSound()
                selectedPage = if (currentCrop == null) {
                    FortuneGuidePage.OVERVIEW
                } else {
                    FortuneGuidePage.CROP
                }
        }
        y += 30
        if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x, y, 28, 25) && selectedPage != FortuneGuidePage.UPGRADES) {
                selectedPage = FortuneGuidePage.UPGRADES
                SoundUtils.playClickSound()
        }

        if (selectedPage != FortuneGuidePage.UPGRADES) {
            if (currentCrop == null) {
                if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 142, guiTop + 130, 16, 16) &&
                    currentPet != FarmingItems.ELEPHANT
                ) {
                    SoundUtils.playClickSound()
                    currentPet = FarmingItems.ELEPHANT
                    FFStats.getTotalFF()
                } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 162, guiTop + 130, 16, 16) &&
                    currentPet != FarmingItems.MOOSHROOM_COW
                ) {
                    SoundUtils.playClickSound()
                    currentPet = FarmingItems.MOOSHROOM_COW
                    FFStats.getTotalFF()
                } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 182, guiTop + 130, 16, 16) &&
                    currentPet != FarmingItems.RABBIT
                ) {
                    SoundUtils.playClickSound()
                    currentPet = FarmingItems.RABBIT
                    FFStats.getTotalFF()
                } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 202, guiTop + 130, 16, 16) &&
                    currentPet != FarmingItems.BEE
                ) {
                    SoundUtils.playClickSound()
                    currentPet = FarmingItems.BEE
                    FFStats.getTotalFF()
                } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 142, guiTop + 5, 16, 16)) {
                    SoundUtils.playClickSound()
                    currentArmor = if (currentArmor == 1) 0 else 1
                } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 162, guiTop + 5, 16, 16)) {
                    SoundUtils.playClickSound()
                    currentArmor = if (currentArmor == 2) 0 else 2
                } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 182, guiTop + 5, 16, 16)) {
                    SoundUtils.playClickSound()
                    currentArmor = if (currentArmor == 3) 0 else 3
                } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 202, guiTop + 5, 16, 16)) {
                    SoundUtils.playClickSound()
                    currentArmor = if (currentArmor == 4) 0 else 4
                } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 262, guiTop + 5, 16, 16)) {
                    SoundUtils.playClickSound()
                    currentEquipment = if (currentEquipment == 1) 0 else 1
                } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 282, guiTop + 5, 16, 16)) {
                    SoundUtils.playClickSound()
                    currentEquipment = if (currentEquipment == 2) 0 else 2
                } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 302, guiTop + 5, 16, 16)) {
                    SoundUtils.playClickSound()
                    currentEquipment = if (currentEquipment == 3) 0 else 3
                } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 322, guiTop + 5, 16, 16)) {
                    SoundUtils.playClickSound()
                    currentEquipment = if (currentEquipment == 4) 0 else 4
                }
            } else {
                if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 142, guiTop + 160, 16, 16) &&
                    currentPet != FarmingItems.ELEPHANT
                ) {
                    SoundUtils.playClickSound()
                    currentPet = FarmingItems.ELEPHANT
                    FFStats.getTotalFF()
                } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 162, guiTop + 160, 16, 16) &&
                    currentPet != FarmingItems.MOOSHROOM_COW
                ) {
                    SoundUtils.playClickSound()
                    currentPet = FarmingItems.MOOSHROOM_COW
                    FFStats.getTotalFF()
                } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 182, guiTop + 160, 16, 16) &&
                    currentPet != FarmingItems.RABBIT
                ) {
                    SoundUtils.playClickSound()
                    currentPet = FarmingItems.RABBIT
                    FFStats.getTotalFF()
                } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 202, guiTop + 160, 16, 16) &&
                    currentPet != FarmingItems.BEE
                ) {
                    SoundUtils.playClickSound()
                    currentPet = FarmingItems.BEE
                    FFStats.getTotalFF()
                }
            }
        } else {
            if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft, guiTop, sizeX, sizeY)) {
                lastClickedHeight = mouseY
            }
        }
    }

    private fun renderTabs() {
        var x = guiLeft + 15
        var y = guiTop - 28
        val selectedColor = 0x50000000
        val notSelectedColor = 0x50303030
        drawRect(x, y, x + 25, y + 28, if (currentCrop == null) selectedColor else notSelectedColor)
        GuiRenderUtils.renderItemStack(ItemStack(Blocks.grass), x + 5, y + 5)
        if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x, y, 25, 28)) {
            tooltipToDisplay.add("§eOverview")
        }

        for (crop in CropType.entries) {
            x += 30
            drawRect(x, y, x + 25, y + 28, if (currentCrop == crop) selectedColor else notSelectedColor)
            GuiRenderUtils.renderItemStack(crop.icon, x + 5, y + 5)
            if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x, y, 25, 28)) {
                tooltipToDisplay.add("§e${crop.cropName}")
            }
        }

        x = guiLeft - 28
        y = guiTop + 15

        drawRect(
            x, y,
            x + 28, y + 25,
            if (selectedPage != FortuneGuidePage.UPGRADES) selectedColor else notSelectedColor
        )
        GuiRenderUtils.renderItemStack(ItemStack(Items.gold_ingot), x + 5, y + 5)
        if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x, y, 28, 25)) {
            tooltipToDisplay.add("§eBreakdown")
        }
        y += 30
        drawRect(
            x, y,
            x + 28, y + 25,
            if (selectedPage == FortuneGuidePage.UPGRADES) selectedColor else notSelectedColor
        )
        GuiRenderUtils.renderItemStack(ItemStack(Items.map), x + 5, y + 5)
        if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x, y, 28, 25)) {
            tooltipToDisplay.add("§eUpgrades")
        }
    }

    enum class FortuneGuidePage {
        OVERVIEW,
        CROP,
        UPGRADES
    }

    abstract class FFGuidePage {
        abstract fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float)
    }
}