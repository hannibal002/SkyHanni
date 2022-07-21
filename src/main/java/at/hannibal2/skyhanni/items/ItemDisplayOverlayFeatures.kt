package at.hannibal2.skyhanni.items

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import at.hannibal2.skyhanni.utils.LorenzUtils.matchRegex
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ItemDisplayOverlayFeatures {

    @SubscribeEvent
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.Post) {
        val item = event.stack ?: return

        if (!LorenzUtils.inSkyblock || item.stackSize != 1) return

        val stackTip = getStackTip(item)

        if (stackTip.isNotEmpty()) {
            GlStateManager.disableLighting()
            GlStateManager.disableDepth()
            GlStateManager.disableBlend()
            event.fr.drawStringWithShadow(
                stackTip,
                (event.x + 17 - event.fr.getStringWidth(stackTip)).toFloat(),
                (event.y + 9).toFloat(),
                16777215
            )
            GlStateManager.enableLighting()
            GlStateManager.enableDepth()
        }

    }

    private fun getStackTip(item: ItemStack): String {
        val name = item.cleanName()

        if (SkyHanniMod.feature.items.displayMasterStarNumber) {
            when (name) {
                "First Master Star" -> return "1"
                "Second Master Star" -> return "2"
                "Third Master Star" -> return "3"
                "Fourth Master Star" -> return "4"
                "Fifth Master Star" -> return "5"
            }
        }

        if (SkyHanniMod.feature.items.displayMasterSkullNumber) {
            if (name.matchRegex("(.*)Master Skull - Tier .")) {
                return name.substring(name.length - 1)
            }
        }

        if (SkyHanniMod.feature.items.displayDungeonHeadFloor) {
            if (name.contains("Golden ") || name.contains("Diamond ")) {
                when {
                    name.contains("Bonzo") -> return "1"
                    name.contains("Scarf") -> return "2"
                    name.contains("Professor") -> return "3"
                    name.contains("Thorn") -> return "4"
                    name.contains("Livid") -> return "5"
                    name.contains("Sadan") -> return "6"
                    name.contains("Necron") -> return "7"
                }
            }
        }

        if (SkyHanniMod.feature.items.displayNewYearCakeNumber) {
            if (name.startsWith("New Year Cake (")) {
                return "§b" + name.between("(Year ", ")")
            }
        }

        if (SkyHanniMod.feature.items.displayPetLevel) {
            if (ItemUtils.isPet(name)) {
                val level = name.between("Lvl ", "] ").toInt()
                if (level != ItemUtils.maxPetLevel(name)) {
                    return "$level"
                }
            }
        }

        if (SkyHanniMod.feature.items.displaySackName) {
            if (ItemUtils.isSack(name)) {
                //TODO fix this and replace other
//                val sackName = grabSackName(name)
                val split = name.split(" ")
                val sackName = split[split.size - 2]
                return (if (name.contains("Enchanted")) "§5" else "") + sackName.substring(0, 2)
            }
        }

        if (SkyHanniMod.feature.items.displayMinionTier) {
            if (name.contains(" Minion ")) {
                if (item.getLore().any { it.contains("Place this minion") }) {
                    val array = name.split(" ")
                    val last = array[array.size - 1]
                    return last.romanToDecimal().toString()
                }
            }
        }

        return ""
    }

//    private fun grabSackName(name: String): String {
//        val split = name.split(" ")
//        val text = split[0]
//        for (line in arrayOf("Large", "Medium", "Small", "Enchanted")) {
//            if (text == line) return grabSackName(name.substring(text.length + 1))
//        }
//        return text
//    }
}