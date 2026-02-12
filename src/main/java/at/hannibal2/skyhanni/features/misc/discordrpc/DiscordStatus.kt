package at.hannibal2.skyhanni.features.misc.discordrpc

// originally adapted from SkyblockAddons

import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.data.ActionBarStatsData
import at.hannibal2.skyhanni.data.BitsApi
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PurseApi
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.getCurrentMilestoneTier
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.getMaxTier
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.getMilestoneCounter
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.isMaxMilestone
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.percentToNextMilestone
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenApi.getCropType
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils
import at.hannibal2.skyhanni.features.misc.compacttablist.AdvancedPlayerList
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
import at.hannibal2.skyhanni.features.misc.pathfind.AreaNode
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.features.slayer.SlayerType
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.extraAttributes
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.TimeUtils.formatted
import at.hannibal2.skyhanni.utils.compat.getCompoundOrDefault
import at.hannibal2.skyhanni.utils.compat.getIntOrDefault
import kotlin.time.Duration.Companion.minutes

var lastKnownDisplayStrings: MutableMap<DiscordStatus, String> =
    mutableMapOf() // if the displayMessageSupplier is ever a placeholder, return from this instead


// There is no consistent way to get the full username of the owner of an island you are visiting (as far as I know)
// so this will be removed until/unless they add it back
//
// private fun getVisitingName(): String {
//     val tabData = TabListData.getTabList()
//     val ownerRegex = Regex(".*Owner: (\\w+).*")
//     for (line in tabData) {
//         val colorlessLine = line.removeColor()
//         if (ownerRegex.matches(colorlessLine)) {
//             return ownerRegex.find(colorlessLine)!!.groupValues[1]
//         }
//     }
//     return "Someone"
// }

var beenAfkFor = SimpleTimeMark.now()

private fun getCropMilestoneDisplay(): String {
    val crop = InventoryUtils.getItemInHand()?.getCropType()
    val cropCounter = crop?.getMilestoneCounter()
    val allowOverflow = GardenApi.config.cropMilestones.overflow.discordRPC
    val tier = crop?.getCurrentMilestoneTier()
    val progress = tier?.let {
        crop.percentToNextMilestone()?.formatPercentage()
    } ?: 100 // percentage to next milestone

    if (tier == null || cropCounter == null) return AutoStatus.CROP_MILESTONES.placeholderText

    val text = if (crop.isMaxMilestone() || (!allowOverflow && tier >= (getMaxTier()))) {
        "MAXED (${cropCounter.addSeparators()} crops)"
    } else {
        "Milestone $tier ($progress)"
    }
    return "${crop.cropName}: $text"
}

private fun getPetDisplay(): String = CurrentPetApi.currentPet?.getUserFriendlyName()
    ?: "No pet equipped"

enum class DiscordStatus(private val displayMessageSupplier: (() -> String?)) {

    NONE({ null }),

    LOCATION(
        {
            var location = SkyBlockUtils.graphArea ?: "invalid"
            val island = SkyBlockUtils.currentIsland

            if (location == "Your Island") location = "Private Island"
            lastKnownDisplayStrings[LOCATION] = when (island) {
                IslandType.PRIVATE_ISLAND_GUEST -> "Visiting an Island"

                // Some islands give no_area in graphArea, so they must be dealt with separately
                IslandType.PRIVATE_ISLAND -> "Private Island"
                IslandType.DUNGEON_HUB -> "Dungeon Hub"
                IslandType.BACKWATER_BAYOU -> "Backwater Bayou"
                IslandType.CATACOMBS -> "The Catacombs ${DungeonApi.dungeonFloor.orEmpty()}"
                IslandType.KUUDRA_ARENA -> "Kuudra Tier ${KuudraApi.kuudraTier ?: "Unknown"}"
                IslandType.MINESHAFT -> "Glacite Mineshafts"

                IslandType.GARDEN -> {
                    if (location.startsWith("Plot: ")) "Personal Garden ($location)" // Personal Garden (Plot: 8)
                    else "Personal Garden"
                }

                IslandType.GARDEN_GUEST -> {
                    // use getVisitingName() if a way to obtain the username of the player you're visiting becomes available
                    if (location.startsWith("Plot: ")) "Visiting a Garden ($location)"
                    else "Visiting a Garden"
                }

                else -> location.takeIf { it != "None" && it != "invalid" && it != AreaNode.NO_AREA }
                    ?: lastKnownDisplayStrings[LOCATION].orEmpty()
            }
            // Only display None if we don't have a last known area
            lastKnownDisplayStrings[LOCATION].takeIf { it?.isNotEmpty() == true } ?: "None"
        },
    ),

    PURSE(
        {
            val coins = PurseApi.getPurse()
            val motes = CustomScoreboardUtils.getMotes().formatInt() // TODO put this in RiftApi instead of CustomScoreboardUtils

            if (RiftApi.inRift()) {
                "${motes.addSeparators()} ${StringUtils.pluralize(motes, "Mote")}"
            } else {
                "${coins.addSeparators()} ${StringUtils.pluralize(coins.toInt(), "Coin")}"
            }
        },
    ),

    BITS(
        {
            when (val bits = BitsApi.bits) {
                1 -> "1 Bit"
                else -> "$bits Bits"
            }
        },
    ),

    STATS(
        {
            val statString = if (!RiftApi.inRift()) {
                "❤${ActionBarStatsData.HEALTH.value} ❈${ActionBarStatsData.DEFENSE.value} ✎${ActionBarStatsData.MANA.value}"
            } else {
                "${ActionBarStatsData.RIFT_TIME.value}ф ✎${ActionBarStatsData.MANA.value}"
            }
            if (ActionBarStatsData.MANA.value != "") {
                lastKnownDisplayStrings[STATS] = statString
            }
            lastKnownDisplayStrings[STATS].orEmpty()
        },
    ),

    ITEM(
        {
            val heldItem = InventoryUtils.getItemInHand()
            val heldItemName = heldItem?.hoverName?.string?.removeColor()

            if (heldItem == null || heldItemName == "Air") "No item in hand"
            else String.format(java.util.Locale.US, "Holding $heldItemName")
        },
    ),

    TIME(
        {
            SkyBlockTime.now().formatted().removeColor()
        },
    ),

    PROFILE(
        {
            val sbLevel = AdvancedPlayerList.tabPlayerData[PlayerUtils.getName()]?.sbLevel?.toString() ?: "?"
            val profile = buildString {
                append("SkyBlock Level: [$sbLevel] on ")

                append(
                    when {
                        SkyBlockUtils.isIronmanProfile -> "♲"
                        SkyBlockUtils.isBingoProfile -> "Ⓑ"
                        SkyBlockUtils.isStrandedProfile -> "☀"
                        else -> ""
                    }
                )

                // the profileName could be an empty string if the profile fruit hasn't loaded in yet, which is ok
                append(HypixelData.profileName.firstLetterUppercase())
            }

            lastKnownDisplayStrings[PROFILE] = profile
            profile
        },
    ),

    SLAYER(
        {
            val article = if (SlayerApi.activeType == SlayerType.INFERNO) "an" else "a"
            when (SlayerApi.state) {
                SlayerApi.ActiveQuestState.GRINDING -> "Spawning $article ${SlayerApi.activeType?.displayName}"
                SlayerApi.ActiveQuestState.BOSS_FIGHT -> "Slaying $article ${SlayerApi.activeType?.displayName}"
                SlayerApi.ActiveQuestState.SLAIN -> "Finished slaying $article ${SlayerApi.activeType?.displayName}"
                SlayerApi.ActiveQuestState.FAILED -> "Lost to $article ${SlayerApi.activeType?.displayName}"
                else -> AutoStatus.SLAYER.placeholderText
            }
        },
    ),

    CUSTOM(
        {
            DiscordRPCManager.config.customText.get() // custom field in the config
        },
    ),

    AUTO(
        {
            var autoReturn = ""
            for (statusID in DiscordRPCManager.config.autoPriority) { // for every dynamic that the user wants to see...
                val autoStatus = statusID.associatedAutoStatus
                val result =
                    autoStatus.correspondingDiscordStatus.getDisplayString() // get what would happen if we were to display it
                if (result != autoStatus.placeholderText) { // if that value is useful, display it
                    autoReturn = result
                    break
                }
            }
            if (autoReturn == "") { // if we didn't find any useful information, display the fallback
                val fallbackID = DiscordRPCManager.config.auto.get().ordinal
                autoReturn = if (fallbackID == AUTO.ordinal) {
                    NONE.getDisplayString()
                } else {
                    DiscordStatus.entries[fallbackID].getDisplayString()
                }
            }
            autoReturn
        },
    ),

    CROP_MILESTONES({ getCropMilestoneDisplay() }),

    PETS({ getPetDisplay() }),

    // Dynamic-only
    STACKING(
        {
            // Logic for getting the currently held stacking enchant is from Skytils
            val itemInHand = InventoryUtils.getItemInHand()
            val itemName = itemInHand?.hoverName?.string?.removeColor().orEmpty()

            fun getProgressPercent(amount: Int, levels: List<Int>): String {
                var percent = "MAXED"
                for (level in levels.indices) {
                    if (amount > levels[level]) {
                        continue
                    }
                    percent = if (amount.toDouble() == 0.0) {
                        ""
                    } else {
                        ((amount.toDouble() - levels[level - 1]) / (levels[level] - levels[level - 1])).formatPercentage()
                    }
                    break
                }
                return percent
            }

            val extraAttributes = itemInHand?.extraAttributes
            var stackingReturn = AutoStatus.STACKING.placeholderText
            if (extraAttributes != null) {
                val enchantments = extraAttributes.getCompoundOrDefault("enchantments")
                var stackingEnchant = ""
                for (enchant in EstimatedItemValue.stackingEnchants) {
                    if (extraAttributes.contains(enchant.value.statName)) {
                        stackingEnchant = enchant.key
                        break
                    }
                }
                val levels = EstimatedItemValue.stackingEnchants[stackingEnchant]?.levels ?: listOf(0)
                val level = enchantments.getIntOrDefault(stackingEnchant)
                val amount = extraAttributes.getIntOrDefault(EstimatedItemValue.stackingEnchants[stackingEnchant]?.statName)
                val stackingPercent = getProgressPercent(amount, levels)

                stackingReturn =
                    if (stackingPercent == "" || amount == 0) AutoStatus.STACKING.placeholderText // outdated info is useless for AUTO
                    else "$itemName: ${stackingEnchant.firstLetterUppercase()} $level ($stackingPercent)" // Hecatomb 100: (55.55%)
            }
            stackingReturn

        },
    ),

    DUNGEONS(
        {
            if (!DungeonApi.inDungeon()) {
                AutoStatus.DUNGEONS.placeholderText
            } else {
                val boss = DungeonApi.getCurrentBoss()
                if (boss == null) {
                    "Unknown dungeon boss"
                } else {
                    val floor = DungeonApi.dungeonFloor ?: AutoStatus.DUNGEONS.placeholderText
                    val amountKills = DungeonApi.bossStorage?.get(boss)?.addSeparators() ?: "Unknown"
                    val time = DungeonApi.time
                    "$floor Kills: $amountKills ($time)"
                }
            }
        },
    ),

    AFK(
        {
            if (beenAfkFor.passedSince() > 5.minutes) {
                val format = beenAfkFor.passedSince().format(maxUnits = 1, longName = true)
                "AFK for $format"
            } else AutoStatus.AFK.placeholderText
        },
    )
    ;

    fun getDisplayString(): String = displayMessageSupplier().orEmpty()
}

enum class AutoStatus(val placeholderText: String, val correspondingDiscordStatus: DiscordStatus) {
    CROP_MILESTONES("Not farming!", DiscordStatus.CROP_MILESTONES),
    SLAYER("Planning to do a slayer quest", DiscordStatus.SLAYER),
    STACKING("Stacking placeholder (should never be visible)", DiscordStatus.STACKING),
    DUNGEONS("Dungeons placeholder (should never be visible)", DiscordStatus.DUNGEONS),
    AFK("This person is not afk (should never be visible)", DiscordStatus.AFK),
}
