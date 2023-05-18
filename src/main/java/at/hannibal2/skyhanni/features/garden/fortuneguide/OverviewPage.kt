package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.utils.RenderUtils
import net.minecraft.client.Minecraft
import java.io.IOException

class OverviewPage: FFGuideGUI.FFGuidePage() {
    private val textLinesWithTooltip = mutableMapOf<Pair<String, String>, Pair<Int, Int>>()
    private val fr = Minecraft.getMinecraft().fontRendererObj

    private val del = "TODO"

    override fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float) {
        textLinesWithTooltip[Pair("§6Universal Fortune Overview", "§7§2Farming fortune that is universal for all crops\n"
        + "§2which doesn't not come from a tool")] =
            Pair(FFGuideGUI.guiLeft + 5,  FFGuideGUI.guiTop + 5)
        textLinesWithTooltip[Pair("§fSkyHanni", "")] =
            Pair(FFGuideGUI.guiLeft + 310,  FFGuideGUI.guiTop + 5)

        textLinesWithTooltip[Pair("§2Base Farming Fortune: 100FF", "§7§2Base Farming Fortune")] =
            Pair(FFGuideGUI.guiLeft + 10,  FFGuideGUI.guiTop + 25)

        textLinesWithTooltip[Pair("§2Farming Level: $del/240FF", "§7§2Fortune for gaining farming levels\n" +
                "§2You get 4 per level\n§2Your farming level:$del")] =
            Pair(FFGuideGUI.guiLeft + 10,  FFGuideGUI.guiTop + 45)

        textLinesWithTooltip[Pair("§2Garden Plots: $del/72FF", "§7§2Fortune for unlocking Garden plots\n" +
                "§2You get 3 per plot\n§2Your unlocked plots:$del")] =
            Pair(FFGuideGUI.guiLeft + 10,  FFGuideGUI.guiTop + 65)

        textLinesWithTooltip[Pair("§2Community Upgrades: $del/40FF", "§7§2Fortune for community shop upgrades\n" +
                "§2You get 4 per level\n§2Your upgrade level:$del")] =
            Pair(FFGuideGUI.guiLeft + 10,  FFGuideGUI.guiTop + 85)

        textLinesWithTooltip[Pair("§2Cake Buff: $del/5FF", "§7§2Fortune for eating cake\n" +
                "§2You get 5\n§2Time until it runs out:$del")] =
            Pair(FFGuideGUI.guiLeft + 10,  FFGuideGUI.guiTop + 105)
        // this prob needs more description
        textLinesWithTooltip[Pair("§2§nPet Total§r§2: ${del}FF", "§7§2Fortune from your pet")] =
            Pair(FFGuideGUI.guiLeft + 10,  FFGuideGUI.guiTop + 125)

        textLinesWithTooltip[Pair("§2Pet Item: $del/60FF", "§7§2Fortune from your pet's item\n" +
                "§2You have $del equipt\n§2It gains: ${del}FF\n§c$del Minos relic is not good, do not use it")] =
            Pair(FFGuideGUI.guiLeft + 10,  FFGuideGUI.guiTop + 165)

        FFGuideGUI.renderText(textLinesWithTooltip)
    }

    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        return false
    }
}