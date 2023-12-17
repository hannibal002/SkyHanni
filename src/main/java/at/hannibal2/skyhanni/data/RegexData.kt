package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

object RegexData {
    /** Examples:
    §d§l§ka§r §d§l§d§lMYTHIC ACCESSORY §d§l§ka
    §d§l§ka§r §d§l§d§lSHINY MYTHIC DUNGEON CHESTPLATE §d§l§ka
    §6§lSHINY LEGENDARY DUNGEON BOOTS
    §6§lLEGENDARY DUNGEON BOOTS
    §5§lEPIC BOOTS
    §f§lCOMMON
     **/
    val rarityLoreLinePattern by RepoPattern.pattern("item.lore.rarity.line", "^(?:§.){2,3}(?:.§. (?:§.){4})?(?:SHINY )?(?<Rarity>${LorenzRarity.entries.joinToString(separator = "|") { it.rawName }}) ?(?:DUNGEON )?(?<ItemCategory>[^§]*)(?: (?:§.){3}.)?$")

    val abiPhonePattern by RepoPattern.pattern("item.name.abiphone", ".{2}Abiphone .*")

    val enchantedBookPattern by RepoPattern.pattern("item.name.enchanted.book", ".{2}?Enchanted Book")
}
