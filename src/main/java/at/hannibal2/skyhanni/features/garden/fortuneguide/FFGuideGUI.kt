package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.fortuneguide.pages.*
import at.hannibal2.skyhanni.utils.RenderUtils
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
        var currentPet = 0
        var currentArmor = 0
        var currentEquipment = 0

        var mouseX = 0
        var mouseY = 0

        var tooltipToDisplay = mutableListOf<String>()
        val textLinesWithTooltip = mutableMapOf<Pair<String, String>, Pair<Int, Int>>()

        fun isInGui() = Minecraft.getMinecraft().currentScreen is FFGuideGUI

        var cakeBuffTime = -1L

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
        pages[FortuneGuidePage.WHEAT] = WheatPage()
        pages[FortuneGuidePage.CARROT] = CarrotPage()
        pages[FortuneGuidePage.POTATO] = PotatoPage()
        pages[FortuneGuidePage.PUMPKIN] = PumpkinPage()
        pages[FortuneGuidePage.SUGAR_CANE] = CanePage()
        pages[FortuneGuidePage.MELON] = MelonPage()
        pages[FortuneGuidePage.CACTUS] = CactusPage()
        pages[FortuneGuidePage.COCOA_BEANS] = CocoaPage()
        pages[FortuneGuidePage.MUSHROOM] = MushroomPage()
        pages[FortuneGuidePage.NETHER_WART] = WartPage()

        GardenAPI.config?.fortune?.let {
            cakeBuffTime = it.cakeExpiring
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

        //todo fix this up
//        drawRect(
//            guiLeft, guiTop + sizeY + 3, guiLeft + 40,
//            guiTop + sizeY + 15, 0x50000000
//        )
//        RenderUtils.drawStringCentered("§6Mode:", guiLeft + 20, guiTop + sizeY + 9)
//
//        drawRect(
//            guiLeft + 45, guiTop + sizeY + 3, guiLeft + 125,
//            guiTop + sizeY + 15, if (breakdownMode) 0x50555555 else 0x50000000
//        )
//        RenderUtils.drawStringCentered("§6Breakdown", guiLeft + 85, guiTop + sizeY + 9)
//
//        drawRect(
//            guiLeft + 130, guiTop + sizeY + 3, guiLeft + 210,
//            guiTop + sizeY + 15, if (!breakdownMode) 0x50555555 else 0x50000000
//        )
//        RenderUtils.drawStringCentered("§6Improvements", guiLeft + 170, guiTop + sizeY + 9)

        if (breakdownMode) {
            if (selectedPage != FortuneGuidePage.OVERVIEW) {
                RenderUtils.renderItemAndTip(
                    FarmingItems.ELEPHANT.getItem(), guiLeft + 152, guiTop + 160, mouseX, mouseY,
                    if (currentPet == 0) 0xFF00FF00.toInt() else 0xFF43464B.toInt()
                )
                RenderUtils.renderItemAndTip(
                    FarmingItems.MOOSHROOM_COW.getItem(), guiLeft + 172, guiTop + 160, mouseX, mouseY,
                    if (currentPet == 1) 0xFF00FF00.toInt() else 0xFF43464B.toInt()
                )
                RenderUtils.renderItemAndTip(
                    FarmingItems.RABBIT.getItem(), guiLeft + 192, guiTop + 160, mouseX, mouseY,
                    if (currentPet == 2) 0xFF00FF00.toInt() else 0xFF43464B.toInt()
                )

                RenderUtils.renderItemAndTip(
                    FarmingItems.HELMET.getItem(), guiLeft + 162, guiTop + 80, mouseX, mouseY)
                RenderUtils.renderItemAndTip(
                    FarmingItems.CHESTPLATE.getItem(), guiLeft + 162, guiTop + 100, mouseX, mouseY)
                RenderUtils.renderItemAndTip(
                    FarmingItems.LEGGINGS.getItem(), guiLeft + 162, guiTop + 120, mouseX, mouseY)
                RenderUtils.renderItemAndTip(
                    FarmingItems.BOOTS.getItem(), guiLeft + 162, guiTop + 140, mouseX, mouseY)

                RenderUtils.renderItemAndTip(
                    FarmingItems.NECKLACE.getItem(), guiLeft + 182, guiTop + 80, mouseX, mouseY)
                RenderUtils.renderItemAndTip(
                    FarmingItems.CLOAK.getItem(), guiLeft + 182, guiTop + 100, mouseX, mouseY)
                RenderUtils.renderItemAndTip(
                    FarmingItems.BELT.getItem(), guiLeft + 182, guiTop + 120, mouseX, mouseY)
                RenderUtils.renderItemAndTip(
                    FarmingItems.BRACELET.getItem(), guiLeft + 182, guiTop + 140, mouseX, mouseY)

            } else {
                RenderUtils.renderItemAndTip(
                    FarmingItems.HELMET.getItem(), guiLeft + 142, guiTop + 5, mouseX, mouseY,
                    if (currentArmor == 1) 0xFF00FF00.toInt() else 0xFF43464B.toInt()
                )
                RenderUtils.renderItemAndTip(
                    FarmingItems.CHESTPLATE.getItem(), guiLeft + 162, guiTop + 5, mouseX, mouseY,
                    if (currentArmor == 2) 0xFF00FF00.toInt() else 0xFF43464B.toInt()
                )
                RenderUtils.renderItemAndTip(
                    FarmingItems.LEGGINGS.getItem(), guiLeft + 182, guiTop + 5, mouseX, mouseY,
                    if (currentArmor == 3) 0xFF00FF00.toInt() else 0xFF43464B.toInt()
                )
                RenderUtils.renderItemAndTip(
                    FarmingItems.BOOTS.getItem(), guiLeft + 202, guiTop + 5, mouseX, mouseY,
                    if (currentArmor == 4) 0xFF00FF00.toInt() else 0xFF43464B.toInt()
                )

                RenderUtils.renderItemAndTip(
                    FarmingItems.NECKLACE.getItem(), guiLeft + 262, guiTop + 5, mouseX, mouseY,
                    if (currentEquipment == 1) 0xFF00FF00.toInt() else 0xFF43464B.toInt()
                )
                RenderUtils.renderItemAndTip(
                    FarmingItems.CLOAK.getItem(), guiLeft + 282, guiTop + 5, mouseX, mouseY,
                    if (currentEquipment == 2) 0xFF00FF00.toInt() else 0xFF43464B.toInt()
                )
                RenderUtils.renderItemAndTip(
                    FarmingItems.BELT.getItem(), guiLeft + 302, guiTop + 5, mouseX, mouseY,
                    if (currentEquipment == 3) 0xFF00FF00.toInt() else 0xFF43464B.toInt()
                )
                RenderUtils.renderItemAndTip(
                    FarmingItems.BRACELET.getItem(), guiLeft + 322, guiTop + 5, mouseX, mouseY,
                    if (currentEquipment == 4) 0xFF00FF00.toInt() else 0xFF43464B.toInt()
                )

                RenderUtils.renderItemAndTip(
                    FarmingItems.ELEPHANT.getItem(), guiLeft + 152, guiTop + 130, mouseX, mouseY,
                    if (currentPet == 0) 0xFF00FF00.toInt() else 0xFF43464B.toInt()
                )
                RenderUtils.renderItemAndTip(
                    FarmingItems.MOOSHROOM_COW.getItem(), guiLeft + 172, guiTop + 130, mouseX, mouseY,
                    if (currentPet == 1) 0xFF00FF00.toInt() else 0xFF43464B.toInt()
                )
                RenderUtils.renderItemAndTip(
                    FarmingItems.RABBIT.getItem(), guiLeft + 192, guiTop + 130, mouseX, mouseY,
                    if (currentPet == 2) 0xFF00FF00.toInt() else 0xFF43464B.toInt()
                )
            }
        }

        RenderUtils.drawStringCentered("§7SkyHanni", guiLeft + 325, guiTop + 170)
        RenderUtils.drawStringCentered("§cIn beta! Report issues and suggestions on the discord", guiLeft + 10, guiTop + sizeY + 10)

        pages[selectedPage]?.drawPage(mouseX, mouseY, partialTicks)

        renderText(tooltipToDisplay)

        GlStateManager.popMatrix()

        if (tooltipToDisplay.isNotEmpty()) {
            RenderUtils.drawTooltip(tooltipToDisplay, mouseX, mouseY, height)
            tooltipToDisplay.clear()
        }
    }

    private fun renderText(output: MutableList<String>, scale: Float = .7f) {
        for (line in textLinesWithTooltip) {
            val inverse = 1 / scale
            val str = line.key.first
            val tooltip = line.key.second
            val x = line.value.first
            val y = line.value.second

            val textWidth: Int = Minecraft.getMinecraft().fontRendererObj.getStringWidth(str) + 6
            val textHeight = 14
            GlStateManager.scale(scale, scale, scale)
            RenderUtils.drawString(str, (x + 3) * inverse, (y + 2) * inverse)
            GlStateManager.scale(inverse, inverse, inverse)
            if (tooltip == "") continue
            if (RenderUtils.isPointInRect(mouseX, mouseY, x, y, (textWidth * scale).toInt(), textHeight)) {
                val split = tooltip.split("\n")
                for (tooltipLine in split) {
                    output.add(tooltipLine)
                }
            }
        }
    }

    @Throws(IOException::class)
    override fun mouseClicked(originalX: Int, originalY: Int, mouseButton: Int) {
        super.mouseClicked(originalX, originalY, mouseButton)

        for (page in FortuneGuidePage.values()) {
            val x = guiLeft + (page.ordinal) * 30 + 15
            val y = guiTop - 28

            if (RenderUtils.isPointInRect(mouseX, mouseY, x, y, 25, 28)) {
                if (selectedPage != page) {
                    SoundUtils.playClickSound()
                    swapMode()
                    selectedPage = page
                    swapMode()
                }
            }
        }
//        if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 45, guiTop + sizeY, 80, 15) && !breakdownMode) {
//            SoundUtils.playClickSound()
//            breakdownMode = true
//            swapMode()
//        }
//        if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 130, guiTop + sizeY, 80, 15) && breakdownMode) {
//            SoundUtils.playClickSound()
//            breakdownMode = false
//            swapMode()
//        }
        if (selectedPage == FortuneGuidePage.OVERVIEW) {
            if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 152, guiTop + 130, 16, 16) && currentPet != 0) {
                SoundUtils.playClickSound()
                currentPet = 0
                FFStats.totalFF(FFStats.elephantFF)
            } else if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 172, guiTop + 130, 16, 16) && currentPet != 1
            ) {
                SoundUtils.playClickSound()
                currentPet = 1
                FFStats.totalFF(FFStats.mooshroomFF)
            } else if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 192, guiTop + 130, 16, 16) && currentPet != 2
            ) {
                SoundUtils.playClickSound()
                currentPet = 2
                FFStats.totalFF(FFStats.rabbitFF)
            } else if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 142, guiTop + 5, 16, 16)) {
                SoundUtils.playClickSound()
                currentArmor = if (currentArmor == 1) 0 else 1
            } else if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 162, guiTop + 5, 16, 16)) {
                SoundUtils.playClickSound()
                currentArmor = if (currentArmor == 2) 0 else 2
            } else if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 182, guiTop + 5, 16, 16)) {
                SoundUtils.playClickSound()
                currentArmor = if (currentArmor == 3) 0 else 3
            } else if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 202, guiTop + 5, 16, 16)) {
                SoundUtils.playClickSound()
                currentArmor = if (currentArmor == 4) 0 else 4
            } else if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 262, guiTop + 5, 16, 16)) {
                SoundUtils.playClickSound()
                currentEquipment = if (currentEquipment == 1) 0 else 1
            } else if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 282, guiTop + 5, 16, 16)) {
                SoundUtils.playClickSound()
                currentEquipment = if (currentEquipment == 2) 0 else 2
            } else if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 302, guiTop + 5, 16, 16)) {
                SoundUtils.playClickSound()
                currentEquipment = if (currentEquipment == 3) 0 else 3
            } else if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 322, guiTop + 5, 16, 16)) {
                SoundUtils.playClickSound()
                currentEquipment = if (currentEquipment == 4) 0 else 4
            }

        } else {
            if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 152, guiTop + 160, 16, 16) && currentPet != 0) {
                SoundUtils.playClickSound()
                currentPet = 0
                FFStats.totalFF(FFStats.elephantFF)
            } else if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 172, guiTop + 160, 16, 16) && currentPet != 1
            ) {
                SoundUtils.playClickSound()
                currentPet = 1
                FFStats.totalFF(FFStats.mooshroomFF)
            } else if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 192, guiTop + 160, 16, 16) && currentPet != 2
            ) {
                SoundUtils.playClickSound()
                currentPet = 2
                FFStats.totalFF(FFStats.rabbitFF)
            }
        }
    }

    private fun renderTabs() {
        for (page in FortuneGuidePage.values()) {
            val x = guiLeft + (page.ordinal) * 30 + 15
            val y = guiTop - 28
            drawRect(x, y, x + 25, y + 28, if (page == selectedPage) 0x50555555 else 0x50000000)

            if (page.crop != null) {
                RenderUtils.renderItemStack(page.crop.icon, x + 5, y + 5)
            } else RenderUtils.renderItemStack(ItemStack(Blocks.grass), x + 5, y + 5)

            if (RenderUtils.isPointInRect(mouseX, mouseY, x, y, 25, 25)) {
                tooltipToDisplay.add(page.pageName)
            }
        }
    }

    private fun swapMode() {
        textLinesWithTooltip.clear()
    }

    enum class FortuneGuidePage(val crop: CropType?) {
        OVERVIEW(null),
        WHEAT(CropType.WHEAT),
        CARROT(CropType.CARROT),
        POTATO(CropType.POTATO),
        NETHER_WART(CropType.NETHER_WART),
        PUMPKIN(CropType.PUMPKIN),
        MELON(CropType.MELON),
        COCOA_BEANS(CropType.COCOA_BEANS),
        SUGAR_CANE(CropType.SUGAR_CANE),
        CACTUS(CropType.CACTUS),
        MUSHROOM(CropType.MUSHROOM),
        ;

        val pageName = crop?.let { "§e" + crop.cropName } ?: "§eOverview"
    }

    abstract class FFGuidePage {
        abstract fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float)
    }
}

