package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileApiDataLoadedEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addOrPut
import at.hannibal2.skyhanni.utils.LorenzUtils.sumAllValues
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.ordinal
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class TrophyFishMessages {
    private var hasLoadedTrophyFish = false
    private val fishes get() = ProfileStorageData.profileSpecific?.crimsonIsle?.trophyFishes
    private val trophyFishPattern =
        Regex("§6§lTROPHY FISH! §r§bYou caught an? §r(?<displayName>§[0-9a-f](?:§k)?[\\w -]+)§r§r§r §r§l§r(?<displayRarity>§[0-9a-f]§l\\w+)§r§b\\.")
    private val config get() = SkyHanniMod.feature.fishing

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        hasLoadedTrophyFish = false
    }

    @SubscribeEvent
    fun onProfileDataLoad(event: ProfileApiDataLoadedEvent) {
        if (hasLoadedTrophyFish) return
        val trophyFishes = fishes ?: return
        val profileData = event.profileData
        trophyFishes.clear()
        for ((rawName, value) in profileData["trophy_fish"].asJsonObject.entrySet()) {
            val rarity = getByName(rawName) ?: continue
            val text = rawName.replace("_", "")
            val displayName = text.substring(0, text.length - rarity.name.length)

            val amount = value.asInt
            val rarities = trophyFishes.getOrPut(displayName) { mutableMapOf() }
            rarities[rarity] = amount
            hasLoadedTrophyFish = true
        }
    }

    @SubscribeEvent
    fun onStatusBar(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock || !config.trophyCounter) return

        val match = trophyFishPattern.matchEntire(event.message)?.groups ?: return
        val displayName = match["displayName"]!!.value.replace("§k", "")
        val displayRarity = match["displayRarity"]!!.value

        val fishName = displayName.replace("Obfuscated", "Obfuscated Fish")
            .replace("[- ]".toRegex(), "").lowercase().removeColor()
        val rawRarity = displayRarity.lowercase().removeColor()
        val rarity = getByName(rawRarity) ?: return

        val trophyFishes = fishes ?: return
        val rarities = trophyFishes.getOrPut(fishName) { mutableMapOf() }
        val amount = rarities.addOrPut(rarity, 1)
        event.blockedReason = "trophy_fish"

        if (config.trophyDesign == 0 && amount == 1) {
            LorenzUtils.chat("§6§lTROPHY FISH! §c§lFIRST §r$displayRarity $displayName")
            return
        }

        if (config.trophyFishBronzeHider && rarity == TrophyRarity.BRONZE && amount != 1) return
        if (config.trophyFishSilverHider && rarity == TrophyRarity.SILVER && amount != 1) return
        val totalText = if (config.trophyFishTotalAmount) {
            val total = rarities.sumAllValues()
            " §7(${total.addSeparators()}. total)"
        } else ""

        val trophyMessage = "§6§lTROPHY FISH! " + when (config.trophyDesign) {
            0 -> "§7$amount. §r$displayRarity $displayName$totalText"
            1 -> "§bYou caught a $displayName $displayRarity§b. §7(${amount.addSeparators()})$totalText"
            else -> "§bYou caught your ${amount.addSeparators()}${amount.ordinal()} $displayRarity $displayName§b.$totalText"
        }

        Minecraft.getMinecraft().ingameGUI.chatGUI.printChatMessageWithOptionalDeletion(
            ChatComponentText(trophyMessage),
            if (config.trophyFishDuplicateHider) (fishName + rarity).hashCode() else 0
        )
    }

    fun getByName(rawName: String) = TrophyRarity.values().firstOrNull { rawName.uppercase().endsWith(it.name) }

    data class TrophyFish(val rarities: MutableMap<TrophyRarity, Int> = mutableMapOf())
}
