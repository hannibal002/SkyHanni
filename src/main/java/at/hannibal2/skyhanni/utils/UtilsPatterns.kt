package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

object UtilsPatterns {
    private val patternGroup = RepoPattern.group("utils")

    /** Examples:
    §d§l§ka§r §d§l§d§lMYTHIC ACCESSORY §d§l§ka
    §d§l§ka§r §d§l§d§lSHINY MYTHIC DUNGEON CHESTPLATE §d§l§ka
    §6§lSHINY LEGENDARY DUNGEON BOOTS
    §6§lLEGENDARY DUNGEON BOOTS
    §5§lEPIC BOOTS
    §f§lCOMMON
     **/
    val rarityLoreLinePattern by patternGroup.pattern("item.lore.rarity.line", "^(?:§.){2,3}(?:.§. (?:§.){4})?(?:SHINY )?(?<Rarity>${LorenzRarity.entries.joinToString(separator = "|") { it.rawName }}) ?(?:DUNGEON )?(?<ItemCategory>[^§]*)(?: (?:§.){3}.)?$")

    val abiPhonePattern by patternGroup.pattern("item.name.abiphone", ".{2}Abiphone .*")

    val enchantedBookPattern by patternGroup.pattern("item.name.enchanted.book", ".{2}?Enchanted Book")
}
