package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getMinecraftId
import net.minecraft.nbt.NBTTagCompound

object CopyItemCommand {

    fun command() {
        try {
            val resultList = mutableListOf<String>()
            val itemStack = InventoryUtils.getItemInHand() ?: return
            resultList.add("ITEM LORE")
            resultList.add("display name: '" + itemStack.displayName.toString() + "'")
            val itemID = itemStack.getInternalName().asString()
            resultList.add("internalName: '$itemID'")
            resultList.add("minecraft id: '" + itemStack.getMinecraftId() + "'")
            resultList.add("lore:")
            for (line in itemStack.getLore()) {
                resultList.add(" '$line'")
            }
            resultList.add("")
            resultList.add("getTagCompound")
            if (itemStack.hasTagCompound()) {
                val tagCompound = itemStack.tagCompound
                recurseTag(tagCompound, "  ", resultList)
            }

            val string = resultList.joinToString("\n")
            OSUtils.copyToClipboard(string)
            LorenzUtils.chat("§e[SkyHanni] Item info copied into the clipboard!")
        } catch (_: Throwable) {
            LorenzUtils.chat("§c[SkyHanni] No item in hand!")
        }
    }

    private fun recurseTag(compound: NBTTagCompound, text: String, list: MutableList<String>) {
        for (s in compound.keySet) {
            if (s == "Lore") continue
            val tag = compound.getTag(s)

            if (tag !is NBTTagCompound) {
                list.add("${text}${s}: $tag")
            } else {
                val element = compound.getCompoundTag(s)
                list.add("${text}${s}:")
                recurseTag(element, "$text  ", list)
            }
        }
    }

}