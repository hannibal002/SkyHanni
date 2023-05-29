package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.fortuneguide.pages.*
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import org.lwjgl.input.Mouse
import java.io.IOException
import java.util.*

open class FFGuideGUI : GuiScreen() {
    companion object {
        val pages = mutableMapOf<FortuneGuidePage, FFGuidePage>()

        var guiLeft = 0
        var guiTop = 0
        var screenHeight = 0

        const val sizeX = 360
        const val sizeY = 180

        var selectedPage = FortuneGuidePage.OVERVIEW
        var breakdownMode = true
        var currentCrop: CropType? = null
        var currentPet = 0
        var currentArmor = 0
        var currentEquipment = 0

        var mouseX = 0
        var mouseY = 0

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
        pages[FortuneGuidePage.OVERVIEW] = OverviewPage()
        pages[FortuneGuidePage.CROP] = CropPage()
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

        if (selectedPage != FortuneGuidePage.OVERVIEW) {
            GuiRenderUtils.renderItemAndTip(
                FarmingItems.ELEPHANT.getItem(), guiLeft + 152, guiTop + 160, mouseX, mouseY,
                if (currentPet == 0) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
            )
            GuiRenderUtils.renderItemAndTip(
                FarmingItems.MOOSHROOM_COW.getItem(), guiLeft + 172, guiTop + 160, mouseX, mouseY,
                if (currentPet == 1) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
            )
            GuiRenderUtils.renderItemAndTip(
                FarmingItems.RABBIT.getItem(), guiLeft + 192, guiTop + 160, mouseX, mouseY,
                if (currentPet == 2) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
            )

            GuiRenderUtils.renderItemAndTip(
                FarmingItems.HELMET.getItem(), guiLeft + 162, guiTop + 80, mouseX, mouseY)
            GuiRenderUtils.renderItemAndTip(
                FarmingItems.CHESTPLATE.getItem(), guiLeft + 162, guiTop + 100, mouseX, mouseY)
            GuiRenderUtils.renderItemAndTip(
                FarmingItems.LEGGINGS.getItem(), guiLeft + 162, guiTop + 120, mouseX, mouseY)
            GuiRenderUtils.renderItemAndTip(
                FarmingItems.BOOTS.getItem(), guiLeft + 162, guiTop + 140, mouseX, mouseY)

            GuiRenderUtils.renderItemAndTip(
                FarmingItems.NECKLACE.getItem(), guiLeft + 182, guiTop + 80, mouseX, mouseY)
            GuiRenderUtils.renderItemAndTip(
                FarmingItems.CLOAK.getItem(), guiLeft + 182, guiTop + 100, mouseX, mouseY)
            GuiRenderUtils.renderItemAndTip(
                FarmingItems.BELT.getItem(), guiLeft + 182, guiTop + 120, mouseX, mouseY)
            GuiRenderUtils.renderItemAndTip(
                FarmingItems.BRACELET.getItem(), guiLeft + 182, guiTop + 140, mouseX, mouseY)

        } else {
            GuiRenderUtils.renderItemAndTip(
                FarmingItems.HELMET.getItem(), guiLeft + 142, guiTop + 5, mouseX, mouseY,
                if (currentArmor == 1) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
            )
            GuiRenderUtils.renderItemAndTip(
                FarmingItems.CHESTPLATE.getItem(), guiLeft + 162, guiTop + 5, mouseX, mouseY,
                if (currentArmor == 2) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
            )
            GuiRenderUtils.renderItemAndTip(
                FarmingItems.LEGGINGS.getItem(), guiLeft + 182, guiTop + 5, mouseX, mouseY,
                if (currentArmor == 3) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
            )
            GuiRenderUtils.renderItemAndTip(
                FarmingItems.BOOTS.getItem(), guiLeft + 202, guiTop + 5, mouseX, mouseY,
                if (currentArmor == 4) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
            )

            GuiRenderUtils.renderItemAndTip(
                FarmingItems.NECKLACE.getItem(), guiLeft + 262, guiTop + 5, mouseX, mouseY,
                if (currentEquipment == 1) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
            )
            GuiRenderUtils.renderItemAndTip(
                FarmingItems.CLOAK.getItem(), guiLeft + 282, guiTop + 5, mouseX, mouseY,
                if (currentEquipment == 2) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
            )
            GuiRenderUtils.renderItemAndTip(
                FarmingItems.BELT.getItem(), guiLeft + 302, guiTop + 5, mouseX, mouseY,
                if (currentEquipment == 3) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
            )
            GuiRenderUtils.renderItemAndTip(
                FarmingItems.BRACELET.getItem(), guiLeft + 322, guiTop + 5, mouseX, mouseY,
                if (currentEquipment == 4) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
            )

            GuiRenderUtils.renderItemAndTip(
                FarmingItems.ELEPHANT.getItem(), guiLeft + 152, guiTop + 130, mouseX, mouseY,
                if (currentPet == 0) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
            )
            GuiRenderUtils.renderItemAndTip(
                FarmingItems.MOOSHROOM_COW.getItem(), guiLeft + 172, guiTop + 130, mouseX, mouseY,
                if (currentPet == 1) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
            )
            GuiRenderUtils.renderItemAndTip(
                FarmingItems.RABBIT.getItem(), guiLeft + 192, guiTop + 130, mouseX, mouseY,
                if (currentPet == 2) 0xFFB3FFB3.toInt() else 0xFF43464B.toInt()
            )
        }

        GuiRenderUtils.drawStringCentered("§7SkyHanni", guiLeft + 325, guiTop + 170)
        GuiRenderUtils.drawStringCentered("§cIn beta! Report issues and suggestions on the discord", guiLeft + sizeX / 2, guiTop + sizeY + 10)

        pages[selectedPage]?.drawPage(mouseX, mouseY, partialTicks)

        GlStateManager.popMatrix()

        if (tooltipToDisplay.isNotEmpty()) {
            GuiRenderUtils.drawTooltip(tooltipToDisplay, mouseX, mouseY, height)
            tooltipToDisplay.clear()
        }
    }

    @Throws(IOException::class)
    override fun mouseClicked(originalX: Int, originalY: Int, mouseButton: Int) {
        super.mouseClicked(originalX, originalY, mouseButton)

        val y = guiTop - 28
        var x = guiLeft + 15
        if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x, y, 25, 28)) {
            if (selectedPage != FortuneGuidePage.OVERVIEW) {
                SoundUtils.playClickSound()
                selectedPage = FortuneGuidePage.OVERVIEW
                currentCrop = null
            }
        }
        for (crop in CropType.values()) {
            x += 30
            if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x, y, 25, 28)) {
                if (currentCrop != crop) {
                    currentCrop = crop
                    if (selectedPage != FortuneGuidePage.CROP) {
                        selectedPage = FortuneGuidePage.CROP
                    }
                    for (item in FarmingItems.values()) {
                        if (item.name == crop.name) {
                            FFStats.getCropStats(crop, item.getItem())
                        }
                    }
                }
            }
        }

        if (selectedPage == FortuneGuidePage.OVERVIEW) {
            if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 152, guiTop + 130, 16, 16) && currentPet != 0) {
                SoundUtils.playClickSound()
                currentPet = 0
                FFStats.getTotalFF()
            } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 172, guiTop + 130, 16, 16) && currentPet != 1
            ) {
                SoundUtils.playClickSound()
                currentPet = 1
                FFStats.getTotalFF()
            } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 192, guiTop + 130, 16, 16) && currentPet != 2
            ) {
                SoundUtils.playClickSound()
                currentPet = 2
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
            if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 152, guiTop + 160, 16, 16) && currentPet != 0) {
                SoundUtils.playClickSound()
                currentPet = 0
                FFStats.getTotalFF()
            } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 172, guiTop + 160, 16, 16) && currentPet != 1
            ) {
                SoundUtils.playClickSound()
                currentPet = 1
                FFStats.getTotalFF()
            } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 192, guiTop + 160, 16, 16) && currentPet != 2
            ) {
                SoundUtils.playClickSound()
                currentPet = 2
                FFStats.getTotalFF()
            }
        }
    }

    private fun renderTabs() {
        val y = guiTop - 28
        var x = guiLeft + 15
        drawRect(x, y, x + 25, y + 28, if (selectedPage == FortuneGuidePage.OVERVIEW) 0x50555555 else 0x50000000)
        GuiRenderUtils.renderItemStack(ItemStack(Blocks.grass), x + 5, y + 5)
        if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x, y, 25, 25)) {
            tooltipToDisplay.add("§eOverview")
        }

        for (crop in CropType.values()) {
            x += 30
            drawRect(x, y, x + 25, y + 28, if (currentCrop == crop) 0x50555555 else 0x50000000)
            GuiRenderUtils.renderItemStack(crop.icon, x + 5, y + 5)
            if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x, y, 25, 25)) {
                tooltipToDisplay.add("§e${crop.name}")
            }
        }
    }

    enum class FortuneGuidePage {
        OVERVIEW,
        CROP
    }

    abstract class FFGuidePage {
        abstract fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float)
    }
}

