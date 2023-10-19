package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.StringUtils.splitLines
import at.hannibal2.skyhanni.utils.jsonobjects.TrophyFishJson
import at.hannibal2.skyhanni.utils.jsonobjects.TrophyFishJson.TrophyFishInfo
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object TrophyFishManager {

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        try {
            val data = event.getConstant<TrophyFishJson>("TrophyFish") ?: throw Exception()
            trophyFishInfo = data.trophy_fish
            SkyHanniMod.repo.successfulConstants.add("TrophyFish")
        } catch (_: Exception) {
            SkyHanniMod.repo.unsuccessfulConstants.add("TrophyFish")
        }
    }

    val fishes: MutableMap<String, MutableMap<TrophyRarity, Int>>?
        get() = ProfileStorageData.profileSpecific?.crimsonIsle?.trophyFishes

    private var trophyFishInfo = mapOf<String, TrophyFishInfo>()

    fun getInfo(internalName: String) = trophyFishInfo[internalName]

    fun getInfoByName(name: String) = trophyFishInfo.values.find { it.displayName == name }

    private fun formatCount(counts: Map<TrophyRarity, Int>, rarity: TrophyRarity): String {
        val count = counts.getOrDefault(rarity, 0)
        return if (count > 0) "§6${count.addSeparators()}" else "§c✖"
    }

    fun TrophyFishInfo.getFilletValue(rarity: TrophyRarity): Int {
        if (fillet == null) {
            ErrorManager.logError(Error("fillet is null for '$displayName'"), "Error trying to read trophy fish info")
            return -1
        }
        return fillet.getOrDefault(rarity, -1)
    }

    fun TrophyFishInfo.getTooltip(counts: Map<TrophyRarity, Int>): ChatStyle {
        val bestFishObtained = counts.keys.maxOrNull() ?: TrophyRarity.BRONZE
        val rateString = if (rate != null) "§8[§7$rate%§8]" else ""
        val display = """
            |$displayName $rateString
            |${description.splitLines(150)}
            |
            |${TrophyRarity.DIAMOND.formattedString}: ${formatCount(counts, TrophyRarity.DIAMOND)}
            |${TrophyRarity.GOLD.formattedString}: ${formatCount(counts, TrophyRarity.GOLD)}
            |${TrophyRarity.SILVER.formattedString}: ${formatCount(counts, TrophyRarity.SILVER)}
            |${TrophyRarity.BRONZE.formattedString}: ${formatCount(counts, TrophyRarity.BRONZE)}
            |
            |§7Total: ${bestFishObtained.formatCode}${counts.values.sum().addSeparators()}
        """.trimMargin()
        return ChatStyle().setChatHoverEvent(
            HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(display))
        )
    }
}