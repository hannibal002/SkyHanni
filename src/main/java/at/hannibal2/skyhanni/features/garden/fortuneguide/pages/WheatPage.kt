package at.hannibal2.skyhanni.features.garden.fortuneguide.pages

import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import java.io.IOException


class WheatPage: FFGuideGUI.FFGuidePage() {
    private val textLinesWithTooltip = mutableMapOf<Pair<String, String>, Pair<Int, Int>>()
    private val del = "TODO"

    override fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val tool = GardenAPI.config?.fortune?.farmingTools?.get(0)?.split("\n")?.toMutableList()
        textLinesWithTooltip[Pair("§fSkyHanni", "")] =
            Pair(FFGuideGUI.guiLeft + 310,  FFGuideGUI.guiTop + 5)
        if (!LorenzUtils.inSkyBlock) {
            textLinesWithTooltip[Pair("§c Join Skyblock for this display to work!", "")] =
                Pair(FFGuideGUI.guiLeft + 5,  FFGuideGUI.guiTop + 5)
            FFGuideGUI.renderText(textLinesWithTooltip)
            return
        }
        textLinesWithTooltip[Pair("§fSkyHanni", "")] =
            Pair(FFGuideGUI.guiLeft + 310,  FFGuideGUI.guiTop + 5)
        if (FFGuideGUI.breakdownMode) {
            if (tool != null) {
                if (tool.size > 2) {
                    val toolName = tool[0]
                    val toolItemID = tool[1]
                    tool.removeAt(1)
                    RenderUtils.drawScaledItem(tool, FFGuideGUI.guiLeft, FFGuideGUI.guiTop + 15, FFGuideGUI.screenHeight)
                }
            }
            textLinesWithTooltip[Pair("§6Total Wheat Fortune: ${del}FF", "§7§2Base Farming Fortune: ${del}FF\n" +
                    "§2Wheat Specific Fortune ${del}FF")] = Pair(FFGuideGUI.guiLeft + 130,  FFGuideGUI.guiTop + 25)
            textLinesWithTooltip[Pair("§2Crop Upgrade: ${del}/45FF", "§7§2Fortune from Desk crop upgrades" +
                "§2You get 5 per level\n§2Your unlocked level:$del")] = Pair(FFGuideGUI.guiLeft + 130, FFGuideGUI.guiTop + 45)
            textLinesWithTooltip[Pair("§2Harvesting: $del/75FF", "§7§2You get 12.55 per level" +
                "§2Your harvesting enchantment level: $del")] = Pair(FFGuideGUI.guiLeft + 130, FFGuideGUI.guiTop + 65)
            textLinesWithTooltip[Pair("§2Dedication: $del/92FF", "§7§2You get $del per crop milestone level" +
                "§2Tour crop milestone: $del\n§2Your dedication enchantment level: $del")] = Pair(FFGuideGUI.guiLeft + 130, FFGuideGUI.guiTop + 85)
            textLinesWithTooltip[Pair("§2Turbo-Wheat: $del/25FF", "§7§2You get 5 per level" +
                "§2Your Turbo-Wheat enchantment level: $del")] = Pair(FFGuideGUI.guiLeft + 130, FFGuideGUI.guiTop + 105)
            textLinesWithTooltip[Pair("§2Talisman Bonus: $del/30FF", "§7§2You get 10 per tier" +
                    "§2Your talisman tier: $del")] = Pair(FFGuideGUI.guiLeft + 130, FFGuideGUI.guiTop + 125)

            textLinesWithTooltip[Pair("§2Tool bonus: $del/50FF", del)] =
                Pair(FFGuideGUI.guiLeft + 130, FFGuideGUI.guiTop + 145)
            textLinesWithTooltip[Pair("§2Farming for Dummies: $del/5FF", del)] =
                Pair(FFGuideGUI.guiLeft + 130, FFGuideGUI.guiTop + 165)
            textLinesWithTooltip[Pair("§2Reforge: $del/20FF", del)] =
                Pair(FFGuideGUI.guiLeft + 250, FFGuideGUI.guiTop + 45)
            textLinesWithTooltip[Pair("§2Cultivating: $del/10FF", del)] =
                Pair(FFGuideGUI.guiLeft + 250, FFGuideGUI.guiTop + 65)
            textLinesWithTooltip[Pair("§2Logarithmic Counter: $del/96FF", del)] =
                Pair(FFGuideGUI.guiLeft + 250, FFGuideGUI.guiTop + 85)
            textLinesWithTooltip[Pair("§2Collection Analyst: $del/96FF", del)] =
                Pair(FFGuideGUI.guiLeft + 250, FFGuideGUI.guiTop + 105)
        } else {
            textLinesWithTooltip[Pair("§6Missing Wheat Fortune: ${del}FF", "§7§2Total cost to max: ${del}FF")] =
                Pair(FFGuideGUI.guiLeft + 130,  FFGuideGUI.guiTop + 25)
            //TODO order by price and get price
            textLinesWithTooltip[Pair("§2Upgrade to harvesting 6 for $del coins, grants 12.5FF, $del coins / FF", "")] =
                Pair(FFGuideGUI.guiLeft + 10, FFGuideGUI.guiTop + 45)
            textLinesWithTooltip[Pair("§2Recombobulate tool for $del coins, grants ${del}FF, $del coins / FF", "")] =
                Pair(FFGuideGUI.guiLeft + 10, FFGuideGUI.guiTop + 60)
        }

        FFGuideGUI.renderText(textLinesWithTooltip)
    }

    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        return false
    }

    override fun swapMode() {
        textLinesWithTooltip.clear()
    }
}