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

    private val petDropPattern by RepoPattern.pattern(
        "pet.petdropmessage",
        "(?<typeOfDrop>§6§lPET DROP!|§5§lGREAT CATCH! §r§bYou found a §r§7\\[Lvl 1]) (?:§r)?§(?<rarityColor>.)(?<petName>[^§(.]+)(?<magicFindOrFarmingFortune>.*)"
    )

    private val config get() = SkyHanniMod.feature.chat.petRarityDropMessage

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config) return

        petDropPattern.matchMatcher(event.message) {
            val typeOfDrop = group("typeOfDrop")
            val rarityColor = group("rarityColor")
            val petName = group("petName")
            val magicFindOrFarmingFortune = group("magicFindOrFarmingFortune")

            event.chatComponent = ChatComponentText(
                "$typeOfDrop §$rarityColor§l${colorCodeToRarity(rarityColor.first()).uppercase()} §$rarityColor$petName$magicFindOrFarmingFortune"
            )
        } ?: return
    }
}
