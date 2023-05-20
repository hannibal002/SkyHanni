package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.fortuneguide.pages.*
import at.hannibal2.skyhanni.utils.NEUItems
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
    private val pet1 = GardenAPI.config?.fortune?.farmingItems?.get(18)?.let { NEUItems.loadNBTData(it) }
    private val pet2 = GardenAPI.config?.fortune?.farmingItems?.get(19)?.let { NEUItems.loadNBTData(it) }
    private val pet3 = GardenAPI.config?.fortune?.farmingItems?.get(20)?.let { NEUItems.loadNBTData(it) }
    private val helmet = GardenAPI.config?.fortune?.farmingItems?.get(13)?.let { NEUItems.loadNBTData(it) }
    private val chestplate = GardenAPI.config?.fortune?.farmingItems?.get(12)?.let { NEUItems.loadNBTData(it) }
    private val leggings = GardenAPI.config?.fortune?.farmingItems?.get(11)?.let { NEUItems.loadNBTData(it) }
    private val boots = GardenAPI.config?.fortune?.farmingItems?.get(10)?.let { NEUItems.loadNBTData(it) }
    private val necklace = GardenAPI.config?.fortune?.farmingItems?.get(14)?.let { NEUItems.loadNBTData(it) }
    private val cloak = GardenAPI.config?.fortune?.farmingItems?.get(15)?.let { NEUItems.loadNBTData(it) }
    private val belt = GardenAPI.config?.fortune?.farmingItems?.get(16)?.let { NEUItems.loadNBTData(it) }
    private val bracelet = GardenAPI.config?.fortune?.farmingItems?.get(17)?.let { NEUItems.loadNBTData(it) }

    companion object {
        val pages = mutableMapOf<FortuneGuidePages, FFGuidePage>()

        var guiLeft = 0
        var guiTop = 0
        var screenHeight = 0

        const val sizeX = 360
        const val sizeY = 180

        var selectedPage = FortuneGuidePages.OVERVIEW
        var breakdownMode = true
        var currentPet = 0
        var currentMode = 0 // 0 = reg, 1 = armor, 2 = equipment

        var mouseX = 0
        var mouseY = 0

        var tooltipToDisplay = mutableListOf<String>()

        fun renderText(map: MutableMap<Pair<String, String>, Pair<Int, Int>>, scale: Float = .7f) {
            for (line in map) {
                val inverse = 1 /scale
                val str = line.key.first
                val tooltip = line.key.second
                val x = line.value.first
                val y = line.value.second

                val textWidth: Int = Minecraft.getMinecraft().fontRendererObj.getStringWidth(str) + 6
                val textHeight = 14
                GlStateManager.scale(scale, scale, scale)
                RenderUtils.drawString(str, (x + 3) * inverse, (y + 2) * inverse)
                GlStateManager.scale(inverse , inverse, inverse)
                if (tooltip == "") continue
                if (RenderUtils.isPointInRect(mouseX, mouseY, x, y, (textWidth * scale).toInt(), textHeight)) {
                    val split = tooltip.split("\n")
                    for (tooltipLine in split) {
                        tooltipToDisplay.add(tooltipLine)
                    }
                }
            }
        }

        fun isInGui() = Minecraft.getMinecraft().currentScreen is FFGuideGUI
    }

    init {
        pages[FortuneGuidePages.OVERVIEW] = OverviewPage()
        pages[FortuneGuidePages.WHEAT] = WheatPage()
        pages[FortuneGuidePages.CARROT] = CarrotPage()
        pages[FortuneGuidePages.POTATO] = PotatoPage()
        pages[FortuneGuidePages.PUMPKIN] = PumpkinPage()
        pages[FortuneGuidePages.SUGAR_CANE] = CanePage()
        pages[FortuneGuidePages.MELON] = MelonPage()
        pages[FortuneGuidePages.CACTUS] = CactusPage()
        pages[FortuneGuidePages.COCOA_BEANS] = CocoaPage()
        pages[FortuneGuidePages.MUSHROOM] = MushroomPage()
        pages[FortuneGuidePages.NETHER_WART] = WartPage()
    }

//    override fun onGuiClosed() {
//        super.onGuiClosed()
//    }

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

        //TODO these look bad, but i'm not sure if I want to make a dropdown instead, or have items as that would be harder to understand
        drawRect(guiLeft, guiTop + sizeY + 3, guiLeft + 40,
            guiTop + sizeY + 15, 0x50000000)
        RenderUtils.drawStringCentered("§6Mode:", guiLeft + 20, guiTop + sizeY + 9)

        drawRect(guiLeft + 45, guiTop + sizeY + 3, guiLeft + 125,
            guiTop + sizeY + 15, if (breakdownMode) 0x50555555 else 0x50000000)
        RenderUtils.drawStringCentered("§6Breakdown", guiLeft + 85, guiTop + sizeY + 9)

        drawRect(guiLeft + 130, guiTop + sizeY + 3, guiLeft + 210,
            guiTop + sizeY + 15, if (!breakdownMode) 0x50555555 else 0x50000000)
        RenderUtils.drawStringCentered("§6Improvements", guiLeft + 170, guiTop + sizeY + 9)

        if (selectedPage != FortuneGuidePages.OVERVIEW) {
            when (currentPet) {
                0 ->  RenderUtils.renderItemAndTip(pet1, guiLeft + 172, guiTop + 160, mouseX, mouseY)
                1 ->  RenderUtils.renderItemAndTip(pet2, guiLeft + 172, guiTop + 160, mouseX, mouseY)
                2 ->  RenderUtils.renderItemAndTip(pet3, guiLeft + 172, guiTop + 160, mouseX, mouseY)
            }

            RenderUtils.renderItemAndTip(helmet, guiLeft + 162, guiTop + 80, mouseX, mouseY)
            RenderUtils.renderItemAndTip(chestplate, guiLeft + 162, guiTop + 100, mouseX, mouseY)
            RenderUtils.renderItemAndTip(leggings, guiLeft + 162, guiTop + 120, mouseX, mouseY)
            RenderUtils.renderItemAndTip(boots, guiLeft + 162, guiTop + 140, mouseX, mouseY)
            RenderUtils.renderItemAndTip(necklace, guiLeft + 182, guiTop + 80, mouseX, mouseY)
            RenderUtils.renderItemAndTip(cloak, guiLeft + 182, guiTop + 100, mouseX, mouseY)
            RenderUtils.renderItemAndTip(belt, guiLeft + 182, guiTop + 120, mouseX, mouseY)
            RenderUtils.renderItemAndTip(bracelet, guiLeft + 182, guiTop + 140, mouseX, mouseY)
        } else {
            if (currentMode == 0) {
                RenderUtils.renderItemAndTip(pet1, guiLeft + 152, guiTop + 85, mouseX, mouseY,
                    if (currentPet == 0) 0xFF00FF00.toInt() else 0xFF43464B.toInt())
                RenderUtils.renderItemAndTip(pet2, guiLeft + 172, guiTop + 85, mouseX, mouseY,
                    if (currentPet == 1) 0xFF00FF00.toInt() else 0xFF43464B.toInt())
                RenderUtils.renderItemAndTip(pet3, guiLeft + 192, guiTop + 85, mouseX, mouseY,
                    if (currentPet == 2) 0xFF00FF00.toInt() else 0xFF43464B.toInt())

                RenderUtils.renderItemAndTip(helmet, guiLeft + 25, guiTop + 85, mouseX, mouseY)
                RenderUtils.renderItemAndTip(chestplate, guiLeft + 45, guiTop + 85, mouseX, mouseY)
                RenderUtils.renderItemAndTip(leggings, guiLeft + 65, guiTop + 85, mouseX, mouseY)
                RenderUtils.renderItemAndTip(boots, guiLeft + 85, guiTop + 85, mouseX, mouseY)

                RenderUtils.renderItemAndTip(necklace, guiLeft + 260, guiTop + 85, mouseX, mouseY)
                RenderUtils.renderItemAndTip(cloak, guiLeft + 280, guiTop + 85, mouseX, mouseY)
                RenderUtils.renderItemAndTip(belt, guiLeft + 300, guiTop + 85, mouseX, mouseY)
                RenderUtils.renderItemAndTip(bracelet, guiLeft + 320, guiTop + 85, mouseX, mouseY)
            }
        }

        RenderUtils.drawStringCentered("§7SkyHanni", guiLeft + 334, guiTop + sizeY + 9)

        pages[selectedPage]?.drawPage(mouseX, mouseY, partialTicks)

        GlStateManager.popMatrix()

        if (tooltipToDisplay.isNotEmpty()) {
            RenderUtils.drawTooltip(tooltipToDisplay, mouseX, mouseY, height)
            tooltipToDisplay.clear()
        }
    }

    @Throws(IOException::class)
    override fun mouseClicked(originalX: Int, originalY: Int, mouseButton: Int) {
        super.mouseClicked(originalX, originalY, mouseButton)

        for (page in FortuneGuidePages.values()) {
            val x = guiLeft + (page.ordinal) * 30 + 15
            val y = guiTop - 28

            if (RenderUtils.isPointInRect(mouseX, mouseY, x, y, 25, 28)) {
                if (selectedPage != page) {
                    SoundUtils.playClickSound()
                    selectedPage = page
                }
            }
        }
        if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 45, guiTop + sizeY, 80, 15) && !breakdownMode) {
            SoundUtils.playClickSound()
            breakdownMode = true
            pages[selectedPage]?.swapMode()
        }
        if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 130, guiTop + sizeY, 80, 15) && breakdownMode) {
            SoundUtils.playClickSound()
            breakdownMode = false
            pages[selectedPage]?.swapMode()
        }
        if (selectedPage == FortuneGuidePages.OVERVIEW) {
            if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 152, guiTop + 85, 16, 16) && currentPet != 0) {
                SoundUtils.playClickSound()
                currentPet = 0
            } else if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 172, guiTop + 85, 16, 16) && currentPet != 1) {
                SoundUtils.playClickSound()
                currentPet = 1
            } else if (RenderUtils.isPointInRect(mouseX, mouseY, guiLeft + 192, guiTop + 85, 16, 16) && currentPet != 2) {
                SoundUtils.playClickSound()
                currentPet = 2
            }




        }
    }

    private fun renderTabs() {
        for (page in FortuneGuidePages.values()) {
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

    enum class FortuneGuidePages(val pageName: String, val crop: CropType?) {
        OVERVIEW("§eOverview", null),
        WHEAT("§eWheat", CropType.WHEAT),
        CARROT("§eCarrot", CropType.CARROT),
        POTATO("§ePotato", CropType.POTATO),
        NETHER_WART("§eNether Wart", CropType.NETHER_WART),
        PUMPKIN("§ePumpkin", CropType.PUMPKIN),
        MELON("§eMelon", CropType.MELON),
        COCOA_BEANS("§eCocoa Beans", CropType.COCOA_BEANS),
        SUGAR_CANE("§eSugar Cane", CropType.SUGAR_CANE),
        CACTUS("§eCactus", CropType.CACTUS),
        MUSHROOM("§eMushroom", CropType.MUSHROOM),
    }

    abstract class FFGuidePage {
        abstract fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float)

        abstract fun swapMode()
    }
}

