package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.colorCodeToRarity
import at.hannibal2.skyhanni.utils.StringUtils.matchMatchers
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class RareDropMessages {

    /**
     * REGEX-TEST: §6§lPET DROP! §r§5Baby Yeti §r§b(+§r§b168% §r§b✯ Magic Find§r§b)
     * REGEX-TEST: §6§lPET DROP! §r§5Slug §6(§6+1300☘)
     */
    private val petDroppedPattern by RepoPattern.pattern(
        "pet.petdroppedmessage",
        "(?<start>(?:§.)*PET DROP! )(?:§.)*§(?<rarityColor>.)(?<petName>[^§(.]+)(?<end> .*)"
    )

    /**
     * REGEX-TEST: §5§lGREAT CATCH! §r§bYou found a §r§7[Lvl 1] §r§aGuardian§r§b.
     */
    private val petFishedPattern by RepoPattern.pattern(
        "pet.petfishedmessage",
        "(?<start>(?:§.)*GREAT CATCH! (?:§.)*You found a (?:§.)*\\[Lvl 1] )(?:§.)*§(?<rarityColor>.)(?<petName>[^§(.]+)(?<end>.*)"
    )

    /**
     * REGEX-TEST: §aYou claimed a §5Tarantula Pet§a! §r§aYou can manage your Pets in the §r§fPets Menu§r§a in your §r§fSkyBlock Menu§r§a.
     */
    private val petClaimedPattern by RepoPattern.pattern(
        "pet.petclaimedmessage",
        "(?<start>(?:§.)*You claimed a )(?:§.)*§(?<rarityColor>.)(?<petName>[^§(.]+)(?<end>.*)"
    )

    /**
     * REGEX-TEST: §b[MVP§r§c+§r§b] Empa_§r§f §r§ehas obtained §r§a§r§7[Lvl 1] §r§6Bal§r§e!
     */
    private val petObtainedPattern by RepoPattern.pattern(
        "pet.petobtainedmessage",
        "(?<start>(?:§.)*(?:\\[.*])? ?(?:§.)?[a-zA-Z0-9_]{2,16}(?:§.)* (?:§.)*has obtained (?:§.)*\\[Lvl 1] )(?:§.)*§(?<rarityColor>.)(?<petName>[^§(.]+)(?<end>.*)"
    )

    private val patterns = listOf(
        petDroppedPattern, petFishedPattern, petClaimedPattern, petObtainedPattern
    )

    private val config get() = SkyHanniMod.feature.chat.petRarityDropMessage

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config) return

        patterns.matchMatchers(event.message) {
            var start = group("start")
            val rarityColor = group("rarityColor")
            val rarityName = colorCodeToRarity(rarityColor.first()).uppercase()
            val petName = group("petName")
            val end = group("end")
            if (start.endsWith("a ") && rarityName.matches("(?i)[aeiou].*".toRegex()))
                start = start.replace(" $".toRegex(), "n ")

            event.chatComponent = ChatComponentText(
                "$start§$rarityColor§l$rarityName §$rarityColor$petName$end"
            )
        }
    }
}
