package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileApiDataLoadedEvent
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.addSuffix
import at.hannibal2.skyhanni.utils.NumberUtil.ordinal
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class TrophyFishMessages {

    private val map = mutableMapOf<String, Int>()
    private val trophyFishPattern = Regex("§6§lTROPHY FISH! §r§bYou caught an? §r(?<displayName>§[0-9a-f](?:§k)?[\\w ].+)§r§r§r §r§l§r(?<displayRarity>§[0-9a-f]§l\\w+)§r§b\\.")

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

        val groups = trophyFishPattern.matchEntire(message)?.groups ?: return
        val displayName = groups["displayName"]!!.value.replace("§k", "")
        val displayRarity = groups["displayRarity"]!!.value

        val name = displayName
            .replace("Obfuscated", "Obfuscated Fish")
            .replace("[- ]".toRegex(), "")
            .lowercase()
            .removeColor()

        val rarity = displayRarity.lowercase().removeColor()

        val amount = map.getOrDefault("${rarity}_${name}", 0) + 1
        map[name] = amount
        event.blockedReason = "trophy_fish"

        if (SkyHanniMod.feature.fishing.trophyDesign == 0 && amount == 1) {
            LorenzUtils.chat("§6TROPHY FISH! §c§lFIRST §r$displayRarity $displayName")
            return
        }

        if (rarity == "bronze" && amount != 1) {
            if (SkyHanniMod.feature.fishing.trophyFishBronzeHider) return
        }
        if (rarity == "silver" && amount != 1) {
            if (SkyHanniMod.feature.fishing.trophyFishSilverHider) return
        }

        val trophyMessage = when (SkyHanniMod.feature.fishing.trophyDesign) {
            0 -> "§7$amount. §r$displayRarity $displayName"
            1 -> "§bYou caught a $displayName $displayRarity§b. §7(${amount.addSeparators()})"
            2 -> "§bYou caught your ${amount.addSeparators()}${amount.ordinal()} $displayRarity $displayName§b."
            else -> return
        }

        LorenzUtils.chat("§6§lTROPHY FISH! $trophyMessage")
    }
}