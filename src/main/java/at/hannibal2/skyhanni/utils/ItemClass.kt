package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.item.ItemStack

enum class ItemClass {
    SWORD,
    FISHING_ROD,
    BOW,
    SHORT_BOW,
    REFORGE_STONE,
    ACCESSORY,
    HELMET,
    VACUUM,
    BELT,
    POWER_STONE,
    ITEM,
    ;

    companion object {

        val regex = ".{4}(?:Shiny )?(?<Rarity>${LorenzRarity.entries.joinToString(separator = "|") { it.rawName }}) (?:DUNGEON )?(?<ItemClass>.*)".toPattern()

        fun readItemClass(itemStack: ItemStack): ItemClass? {
            for (line in itemStack.getLore().reversed()) {
                regex.matchMatcher(line) {
                    try {
                        return ItemClass.valueOf(group("ItemClass").replace(" ", "_"))
                    } catch (e: IllegalArgumentException) {
                        throw NotImplementedError("Missing Item Class: '${group("ItemClass")}'")
                    }

                }
            }
            return null
        }
    }
}
