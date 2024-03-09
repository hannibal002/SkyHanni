package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.TrophyFishJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.TrophyFishJson.TrophyFishInfo
import at.hannibal2.skyhanni.events.NeuProfileDataLoadedEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.StringUtils.splitLines
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object TrophyFishManager {
    private val config get() = SkyHanniMod.feature.fishing.trophyFishing

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<TrophyFishJson>("TrophyFish")
        trophyFishInfo = data.trophy_fish
    }

    val fishes: MutableMap<String, MutableMap<TrophyRarity, Int>>?
        get() = ProfileStorageData.profileSpecific?.crimsonIsle?.trophyFishes

    private var loadedNeu = false

    @SubscribeEvent
    fun onNeuProfileDataLoaded(event: NeuProfileDataLoadedEvent) {
        if (!LorenzUtils.inSkyBlock || !config.loadFromNeuPV) return
        try {
            // This is alrady the current player data
            val profileInfo = event.neuEvent.profileInfo ?: return
            val neuData = profileInfo.get("trophy_fish") ?: return

            // this event gets sent always with the first profile data found, does not work with profile switches.
            // to correctly load tropy fish data from another profile, the user needs to restart on the new profile and run /pv again.
            if (loadedNeu) return
            loadedNeu = true

            val savedFishes = fishes ?: return

            var changed = false
            for ((neuName, neuValue) in neuData.asJsonObject.entrySet()) {
                val rarity = TrophyRarity.getByName(neuName) ?: continue
                val name = neuName.split("_").dropLast(1).joinToString("")
                val saved = savedFishes[name] ?: continue

                val current = saved[rarity] ?: 0
                val newValue = neuValue.asInt
                if (newValue > current) {
                    saved[rarity] = newValue
                    ChatUtils.debug("Updated trophy fishing data from NEU PV:  $name $rarity: $current -> $neuValue")
                    changed = true
                }
            }
            if (changed) {
                ChatUtils.chat("Updated Trophy Fishing data via NEU PV!")
            }
        } catch (t: Throwable) {
            throw Exception("Failed to load trophy fishing data from NEU PV.", t)
        }
    }

    private var trophyFishInfo = mapOf<String, TrophyFishInfo>()

    fun getInfo(internalName: String) = trophyFishInfo[internalName]

    fun getInfoByName(name: String) = trophyFishInfo.values.find { it.displayName == name }

    private fun formatCount(counts: Map<TrophyRarity, Int>, rarity: TrophyRarity): String {
        val count = counts.getOrDefault(rarity, 0)
        return if (count > 0) "§6${count.addSeparators()}" else "§c✖"
    }

    fun TrophyFishInfo.getFilletValue(rarity: TrophyRarity): Int {
        if (fillet == null) {
            ErrorManager.logErrorStateWithData(
                "Error trying to read trophy fish info",
                "fillet in TrophyFishInfo is null",
                "displayName" to displayName,
                "TrophyFishInfo" to this,
            )
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
