package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.features.garden.fortuneguide.pages.*
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.ItemStack
import org.lwjgl.input.Mouse
import java.io.IOException
import java.util.*

open class FFGuideGUI : GuiScreen() {
    companion object {
        val pages = mutableMapOf<FortuneGuidePages, FFGuidePage>()

        var guiLeft = 0
        var guiTop = 0
        var screenHeight = 0

        const val sizeX = 360
        const val sizeY = 180

        var selectedPage = FortuneGuidePages.OVERVIEW
        var breakdownMode = true

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
                if (mouseX > x && mouseX < x + textWidth * scale && mouseY > y && mouseY < y + textHeight) {
                    val split = tooltip.split("\n")
                    for (tooltipLine in split) {
                        tooltipToDisplay.add(tooltipLine)
                    }
                }
            }
        }
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

        pages[selectedPage]?.drawPage(mouseX, mouseY, partialTicks)

        GlStateManager.popMatrix()

        if (tooltipToDisplay.isNotEmpty()) {
            RenderUtils.drawTooltip(tooltipToDisplay, mouseX, mouseY, height)
            tooltipToDisplay.clear()
        }
    }

    @Throws(IOException::class)
    override fun mouseClicked(originalX: Int, priginalY: Int, mouseButton: Int) {
        super.mouseClicked(originalX, priginalY, mouseButton)

        for (page in FortuneGuidePages.values()) {
            val x = guiLeft + (page.ordinal) * 30
            val y = guiTop - 28

            if (mouseX > x && mouseX < x + 25 && mouseY > y && mouseY < y + 28) {
                if (selectedPage != page) {
                    SoundUtils.playClickSound()
                    selectedPage = page
                    return
                }
            }
        }
        if (mouseX > guiLeft + 45 && mouseX < guiLeft + 125 && mouseY > guiTop + sizeY && mouseY < guiTop + sizeY + 15) {
            SoundUtils.playClickSound()
            breakdownMode = true
            pages[selectedPage]?.swapMode()
            return
        }
        if (mouseX > guiLeft + 130 && mouseX < guiLeft + 210 && mouseY > guiTop + sizeY && mouseY < guiTop + sizeY + 15) {
            SoundUtils.playClickSound()
            breakdownMode = false
            pages[selectedPage]?.swapMode()
            return
        }
    }

    private fun renderTabs() {
        for (page in FortuneGuidePages.values()) {
            val x = guiLeft + (page.ordinal) * 30
            val y = guiTop - 28
            drawRect(x, y, x + 25, y + 28, if (page == selectedPage) 0x50555555 else 0x50000000)

            RenderUtils.renderItemStack(page.icon, x + 5, y + 5)

            if (mouseX > x && mouseX < x + 25 && mouseY > y && mouseY < y + 25) {
                tooltipToDisplay.add(page.pageName)
            }
        }
    }

    enum class FortuneGuidePages(val pageName: String, val icon: ItemStack) {
        OVERVIEW("§eOverview", ItemStack(Blocks.grass)), //TODO want a better item for this
        WHEAT("§eWheat", ItemStack(Items.wheat)),
        CARROT("§eCarrot", ItemStack(Items.carrot)),
        POTATO("§ePotato", ItemStack(Items.potato)),
        NETHER_WART("§eNether Wart", ItemStack(Items.nether_wart)),
        PUMPKIN("§ePumpkin", ItemStack(Blocks.pumpkin)),
        MELON("§eMelon", ItemStack(Items.melon)),
        COCOA_BEANS("§eCocoa Beans", ItemStack(Items.dye, 1, EnumDyeColor.BROWN.dyeDamage)),
        SUGAR_CANE("§eSugar Cane", ItemStack(Items.reeds)),
        CACTUS("§eCactus", ItemStack(Blocks.cactus)),
        MUSHROOM("§eMushroom", ItemStack(Blocks.red_mushroom_block)),
    }

    abstract class FFGuidePage {
        abstract fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float)
        @Throws(IOException::class)
        open fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
            return false
        }

        abstract fun swapMode()
    }
}

