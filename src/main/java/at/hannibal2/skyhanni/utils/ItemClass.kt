package at.hannibal2.skyhanni.utils


import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
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
    COSMETIC,
    NECKLACE,
    CHESTPLATE,
    HOE,
    CLOAK,
    GLOVES,
    LEGGINGS,
    BOOTS,
    TRAVEL_SCROLL,
    PORTAL,
    PET_ITEM,
    TROPHY_FISH,
    MEMENTO,
    BAIT,
    AXE,
    DEPLOYABLE,
    HATCCESSORY,
    ARROW,
    WAND,
    LONGSWORD,
    GAUNTLET,
    EMPTY,
    ABIPHONE,
    PET,
    BRACELET,
    PICKAXE,
    RIFT_TIMECHARM,
    SHOVEL,
    ENCHANTED_BOOK,
    DRILL,
    SHEARS,
    FISHING_WEAPON,
    ARROW_POISON,
    ;

    companion object {

        /* Examples:
        §d§l§ka§r §d§l§d§lMYTHIC ACCESSORY §d§l§ka
        §d§l§ka§r §d§l§d§lSHINY MYTHIC DUNGEON CHESTPLATE §d§l§ka
        §6§lSHINY LEGENDARY DUNGEON BOOTS
        §6§lLEGENDARY DUNGEON BOOTS
        §5§lEPIC BOOTS
        §f§lCOMMON
         */

        val regex = "^(?:§.){2,3}(?:.§. (?:§.){4})?(?:SHINY )?(?<Rarity>${LorenzRarity.entries.joinToString(separator = "|") { it.rawName }}) ?(?:DUNGEON )?(?<ItemClass>[^§]*)(?: (?:§.){3}.)?$".toPattern()
        val abiPhoneRegex = ".{2}Abiphone .*".toPattern()
        val enchantedBookRegex = ".{2}?Enchanted Book".toPattern()

        fun readItemClass(itemStack: ItemStack): ItemClass? {
            for (line in itemStack.getLore().reversed()) {
                regex.matchMatcher(line) {
                    val itemClass = group("ItemClass")
                    try {
                        return if (itemClass.isEmpty()) {
                            val name = itemStack.name ?: ""
                            when {
                                abiPhoneRegex.matches(name) -> ABIPHONE
                                ItemUtils.isPet(name) -> PET // TODO fix
                                enchantedBookRegex.matches(name) -> ENCHANTED_BOOK
                                else -> EMPTY
                            }
                        } else {
                            ItemClass.valueOf(itemClass.replace(" ", "_"))
                        }
                    } catch (e: IllegalArgumentException) {
                        LorenzDebug.log("Missing Item Class: '$itemClass'")
                        return null
                    }

                }
            }
            return null
        }
    }
}
