package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileApiDataLoadedEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class TrophyFishMessages {

    private val map = mutableMapOf<String, Int>()

    @SubscribeEvent
    fun onProfileDataLoad(event: ProfileApiDataLoadedEvent) {
        val profileData = event.profileData

        map.clear()
        val trophyFishes = profileData["trophy_fish"].asJsonObject
        for ((rawName, value) in trophyFishes.entrySet()) {
            val rarity = when {
                rawName.endsWith("_bronze") -> "bronze"
                rawName.endsWith("_silver") -> "silver"
                rawName.endsWith("_gold") -> "gold"
                rawName.endsWith("_diamond") -> "diamond"
                else -> continue
            }
            val text = rawName.replace("_", "")
            val displayName = text.substring(0, text.length - rarity.length)

            val amount = value.asInt

//            LorenzDebug.log("$rarity: $displayName: $amount")
            val name = rarity + "_" + displayName
            map[name] = amount
//            LorenzDebug.log("loaded trophy: $name = $amount")
        }
    }

    @SubscribeEvent
    fun onStatusBar(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.fishing.trophyCounter) return

        val message = event.message
        //TODO replace logic with regex
        if (!message.startsWith("§6§lTROPHY FISH! §r§bYou caught a")) return

        var displayName =
            if (message.contains(" a §r")) message.between(" a §r", "§r §r") else message.between(" an §r", "§r §r")
        if (displayName.contains("§k")) {
            displayName = displayName.replace("§k", "")
            displayName = displayName.replace("Obfuscated", "Obfuscated Fish")
        }
        val rarity = message.between("§r §r", "§b.").lowercase().replace("§l", "")

        val name = (rarity + "_" + displayName).removeColor().lowercase().replace(" ", "").replace("-", "")
        val amount = map.getOrDefault(name, 0) + 1
        map[name] = amount
        event.blockedReason = "trophy_fish"

        if (amount == 1) {
            LorenzUtils.chat("§6TROPHY FISH! §c§lFIRST §r$rarity $displayName")
            return
        }

        if (rarity.contains("bronze")) {
            if (SkyHanniMod.feature.fishing.trophyFishBronzeHider) return
        }
        if (rarity.contains("silver")) {
            if (SkyHanniMod.feature.fishing.trophyFishSilverHider) return
        }

        LorenzUtils.chat("§6TROPHY FISH! §7$amount. §r$rarity $displayName")
    }
}