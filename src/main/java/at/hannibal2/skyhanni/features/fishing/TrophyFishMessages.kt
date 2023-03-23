package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileApiDataLoadedEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.ordinal
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class TrophyFishMessages {
    private var hasLoadedTrophyFish = false
    private val fishAmounts = mutableMapOf<String, Int>()
    private val trophyFishPattern =
        Regex("§6§lTROPHY FISH! §r§bYou caught an? §r(?<displayName>§[0-9a-f](?:§k)?[\\w -]+)§r§r§r §r§l§r(?<displayRarity>§[0-9a-f]§l\\w+)§r§b\\.")

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        hasLoadedTrophyFish = false
    }

    @SubscribeEvent
    fun onProfileDataLoad(event: ProfileApiDataLoadedEvent) {
        if (hasLoadedTrophyFish) return
        val profileData = event.profileData

        fishAmounts.clear()
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
            val fish = rarity + "_" + displayName
            fishAmounts[fish] = amount
//            LorenzDebug.log("loaded trophy: $fish = $amount")
            hasLoadedTrophyFish = true
        }
    }

    @SubscribeEvent
    fun onStatusBar(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock || !SkyHanniMod.feature.fishing.trophyCounter) return

        val match = trophyFishPattern.matchEntire(event.message)?.groups ?: return
        val displayName = match["displayName"]!!.value.replace("§k", "")
        val displayRarity = match["displayRarity"]!!.value

        val name = displayName.replace("Obfuscated", "Obfuscated Fish")
            .replace("[- ]".toRegex(), "").lowercase().removeColor()
        val rarity = displayRarity.lowercase().removeColor()

        val fish = "${rarity}_${name}"
        val amount = fishAmounts.getOrDefault(fish, 0) + 1
        fishAmounts[fish] = amount
        event.blockedReason = "trophy_fish"

        if (SkyHanniMod.feature.fishing.trophyDesign == 0 && amount == 1) {
            LorenzUtils.chat("§6§lTROPHY FISH! §c§lFIRST §r$displayRarity $displayName")
            return
        }

        if (SkyHanniMod.feature.fishing.trophyFishBronzeHider && rarity == "bronze" && amount != 1) return
        if (SkyHanniMod.feature.fishing.trophyFishSilverHider && rarity == "silver" && amount != 1) return

        val trophyMessage = "§6§lTROPHY FISH! " + when (SkyHanniMod.feature.fishing.trophyDesign) {
            0 -> "§7$amount. §r$displayRarity $displayName"
            1 -> "§bYou caught a $displayName $displayRarity§b. §7(${amount.addSeparators()})"
            else -> "§bYou caught your ${amount.addSeparators()}${amount.ordinal()} $displayRarity $displayName§b."
        }

        Minecraft.getMinecraft().ingameGUI.chatGUI.printChatMessageWithOptionalDeletion(
            ChatComponentText(trophyMessage),
            if (SkyHanniMod.feature.fishing.trophyFishDuplicateHider) fish.hashCode() else 0
        )
    }
}