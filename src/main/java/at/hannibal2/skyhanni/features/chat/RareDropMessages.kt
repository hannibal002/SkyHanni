package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.colorCodeToRarity
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class RareDropMessages {

    /**
     * REGEX-TEST: §5§lGREAT CATCH! §r§bYou found a §r§7[Lvl 1] §r§aGuardian§r§b.
     * REGEX-TEST: §6§lPET DROP! §r§5Baby Yeti §r§b(+§r§b168% §r§b✯ Magic Find§r§b)
     * REGEX-TEST: §6§lPET DROP! §r§5Slug §6(§6+1300☘)
     * REGEX-TEST: §aYou claimed a §5Tarantula Pet§a! §r§aYou can manage your Pets in the §r§fPets Menu§r§a in your §r§fSkyBlock Menu§r§a.
     * REGEX-TEST: §b[MVP§r§c+§r§b] Empa_§r§f §r§ehas obtained §r§a§r§7[Lvl 1] §r§6Bal§r§e!
     */
    private val petDropPattern by RepoPattern.pattern(
        "pet.petdropmessage",
        "(?<typeOfDrop>(?:§.)*PET DROP!|(?:§.)*GREAT CATCH! (?:§.)*You found a (?:§.)*\\[Lvl 1]|(?:§.)*You claimed an?|(?:§.)*(?:\\[.*])? ?(?:§.)?[a-zA-Z0-9_]{2,16}(?:§.)* (?:§.)*has obtained (?:§.)*\\[Lvl 1]) (?:§r)?§(?<rarityColor>.)(?<petName>[^§(.]+)(?<magicFindOrFarmingFortune>.*)"
    )

    private val config get() = SkyHanniMod.feature.chat.petRarityDropMessage

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config) return

        petDropPattern.matchMatcher(event.message) {
            var typeOfDrop = group("typeOfDrop")
            val rarityColor = group("rarityColor")
            val rarityName = colorCodeToRarity(rarityColor.first()).uppercase()
            val petName = group("petName")
            val magicFindOrFarmingFortune = group("magicFindOrFarmingFortune")
            typeOfDrop = if (typeOfDrop.endsWith(" a") && rarityName[0] in listOf('A','E','I','O','U'))
                typeOfDrop + "n" else typeOfDrop

            event.chatComponent = ChatComponentText(
                "$typeOfDrop §$rarityColor§l$rarityName §$rarityColor$petName$magicFindOrFarmingFortune"
            )
        } ?: return
    }
}
