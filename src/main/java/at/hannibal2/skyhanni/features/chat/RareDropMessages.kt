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
        "(?<message1>§6§lPET DROP!|§5§lGREAT CATCH! §r§bYou found a §r§7\\[Lvl 1]) (?:§r)?§(?<rarityColor>.)(?<petName>[^§(.]+)(?<message2>.*)"
    )

    val config get() = SkyHanniMod.feature.chat.petRarityDropMessage

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config) return;
        var rarityColor = ""
        var petName = ""
        var message1 = ""
        var message2 = ""

        petDropPattern.matchMatcher(event.message) {
            message1 = group("message1")
            rarityColor = group("rarityColor")
            petName = group("petName")
            message2 = group("message2")
        } ?: return

        event.chatComponent = ChatComponentText(
            "$message1 §r§$rarityColor§l${colorCodeToRarity(rarityColor.first()).uppercase()} §r§$rarityColor$petName$message2"
        )
    }
}
