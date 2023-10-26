package at.hannibal2.skyhanni.features.misc.discordrpc

// SkyblockAddons code, adapted for SkyHanni with some additions and fixes

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ActionBarStatsData
import at.hannibal2.skyhanni.data.GardenCropMilestones.getCounter
import at.hannibal2.skyhanni.data.GardenCropMilestones.getTierForCropCount
import at.hannibal2.skyhanni.data.GardenCropMilestones.isMaxed
import at.hannibal2.skyhanni.data.GardenCropMilestones.progressToNextLevel
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.features.garden.GardenAPI.getCropType
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.colorCodeToRarity
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData.Companion.getTabList
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.TimeUtils.formatted
import io.github.moulberry.notenoughupdates.miscfeatures.PetInfoOverlay.getCurrentPet
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import java.util.function.Supplier
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.minutes

var lastKnownDisplayStrings: MutableMap<DiscordStatus, String> =
    mutableMapOf() // if the displayMessageSupplier is ever a placeholder, return from this instead

val purseRegex = Regex("""(?:Purse|Piggy): ([\d,]+)[\d.]*""")
val motesRegex = Regex("""Motes: ([\d,]+)""")
val bitsRegex = Regex("""Bits: ([\d|,]+)[\d|.]*""")

val stackingEnchants = mapOf(
    "compact" to mapOf(
        "levels" to listOf(0, 100, 500, 1500, 5000, 15000, 50000, 150000, 500000, 1000000),
        "nbtNum" to "compact_blocks"
    ),
    "cultivating" to mapOf(
        "levels" to listOf(
            0,
            1000,
            5000,
            25000,
            100000,
            300000,
            1500000,
            5000000,
            20000000,
            100000000
        ), "nbtNum" to "farmed_cultivating"
    ),
    "expertise" to mapOf(
        "levels" to listOf(0, 50, 100, 250, 500, 1000, 2500, 5500, 10000, 15000),
        "nbtNum" to "expertise_kills"
    ),
    "hecatomb" to mapOf(
        "levels" to listOf(0, 2, 5, 10, 20, 30, 40, 60, 80, 100),
        "nbtNum" to "hecatomb_s_runs"
    ),
    "champion" to mapOf(
        "levels" to listOf(
            0,
            50000,
            100000,
            250000,
            500000,
            1000000,
            1500000,
            2000000,
            2500000,
            3000000
        ), "nbtNum" to "champion_combat_xp"
    )
) // nbtNum is the id of the enchantment in the nbt data

private fun getVisitingName(): String {
    val tabData = getTabList()
    val ownerRegex = Regex(".*Owner: (\\w+).*")
    for (line in tabData) {
        val colorlessLine = line.removeColor()
        if (ownerRegex.matches(colorlessLine)) {
            return ownerRegex.find(colorlessLine)!!.groupValues[1]
        }
    }
    return "Someone"
}

var beenAfkFor = SimpleTimeMark.now()

enum class DiscordStatus(private val displayMessageSupplier: Supplier<String>?) {

    NONE(null),

    LOCATION({
        var location = LorenzUtils.skyBlockArea
        val island = LorenzUtils.skyBlockIsland

        if (location == "Your Island") location = "Private Island"
        when {
            island == IslandType.PRIVATE_ISLAND_GUEST -> lastKnownDisplayStrings[LOCATION] =
                "${getVisitingName()}'s Island"

            island == IslandType.GARDEN -> {
                if (location.startsWith("Plot: ")) {
                    lastKnownDisplayStrings[LOCATION] = "Personal Garden ($location)" // Personal Garden (Plot: 8)
                } else {
                    lastKnownDisplayStrings[LOCATION] = "Personal Garden"
                }
            }

            island == IslandType.GARDEN_GUEST -> {
                lastKnownDisplayStrings[LOCATION] = "${getVisitingName()}'s Garden"
                if (location.startsWith("Plot: ")) {
                    lastKnownDisplayStrings[LOCATION] = "${lastKnownDisplayStrings[LOCATION]} ($location)"
                } // "MelonKingDe's Garden (Plot: 8)"
            }

            location != "None" && location != "invalid" -> {
                lastKnownDisplayStrings[LOCATION] = location
            }
        }
        lastKnownDisplayStrings[LOCATION] ?: "None"// only display None if we don't have a last known area
    }),

    PURSE({
        val scoreboard = ScoreboardData.sidebarLinesFormatted
        // Matches coins amount in purse or piggy, with optional decimal points
        val coins = scoreboard.firstOrNull { purseRegex.matches(it.removeColor()) }?.let {
            purseRegex.find(it.removeColor())?.groupValues?.get(1) ?: ""
        }
        val motes = scoreboard.firstOrNull { motesRegex.matches(it.removeColor()) }?.let {
            motesRegex.find(it.removeColor())?.groupValues?.get(1) ?: ""
        }
        lastKnownDisplayStrings[PURSE] = when {
            coins == "1" -> "1 Coin"
            coins != "" && coins != null -> "$coins Coins"
            motes == "1" -> "1 Mote"
            motes != "" && motes != null -> "$motes Motes"

            else -> lastKnownDisplayStrings[PURSE] ?: ""
        }
        lastKnownDisplayStrings[PURSE] ?: ""
    }),

    BITS({
        val scoreboard = ScoreboardData.sidebarLinesFormatted
        val bits = scoreboard.firstOrNull { bitsRegex.matches(it.removeColor()) }?.let {
            bitsRegex.find(it.removeColor())?.groupValues?.get(1)
        }

        when (bits) {
            "1" -> "1 Bit"
            null -> "0 Bits"
            else -> "$bits Bits"
        }
    }),

    STATS({
        val groups = ActionBarStatsData.groups
        val statString = if (!RiftAPI.inRift()) {
            "❤${groups["health"]} ❈${groups["defense"]} ✎${groups["mana"]}"
        } else {
            "${groups["riftTime"]}ф ✎${groups["mana"]}"
        }
        if (groups["mana"] != "") {
            lastKnownDisplayStrings[STATS] = statString
        }
        lastKnownDisplayStrings[STATS] ?: ""
    }),

    ITEM({
        InventoryUtils.getItemInHand()?.let {
            String.format("Holding ${it.displayName.removeColor()}")
        } ?: "No item in hand"
    }),

    TIME({
        SkyBlockTime.now().formatted()
    }),

    PROFILE({
        val player = LorenzUtils.getPlayerName()

        val tabData = getTabList()
        val levelRegex = Regex("""\[(\d{1,3})] $player""")
        var sbLevel = ""
// SkyBlock Level: [999] on Lemon
        for (line in tabData) {
            if (line.contains(player)) {
                val colorlessLine = line.removeColor()
                sbLevel = levelRegex.find(colorlessLine)!!.groupValues[1]
                break
            }
        }

        var profile = "SkyBlock Level: [$sbLevel] on "

        profile += when {
            HypixelData.ironman -> "♲"
            HypixelData.bingo -> "Ⓑ"
            HypixelData.stranded -> "☀"
            else -> ""
        }

        val fruit = HypixelData.profileName.firstLetterUppercase()
        if (fruit == "") profile =
            lastKnownDisplayStrings[PROFILE] ?: "SkyBlock Level: [$sbLevel]" // profile fruit hasn't loaded in yet
        else profile += fruit

        lastKnownDisplayStrings[PROFILE] = profile
        profile
    }),

    SLAYER({
        var slayerName = ""
        var slayerLevel = ""
        var bossAlive = "spawning"
        val slayerRegex =
            Pattern.compile("(?<name>(?:\\w| )*) (?<level>[IV]+)") // Samples: Revenant Horror I; Tarantula Broodfather IV

        for (line in ScoreboardData.sidebarLinesFormatted) {
            val noColorLine = line.removeColor()
            val match = slayerRegex.matcher(noColorLine)
            when {
                match.matches() -> {
                    slayerName = match.group("name")
                    slayerLevel = match.group("level")
                }

                noColorLine == "Slay the boss!" -> bossAlive = "slaying"
                noColorLine == "Boss slain!" -> bossAlive = "slain"
            }
        }

        when {
            slayerLevel == "" -> AutoStatus.SLAYER.placeholderText // selected slayer in rpc but hasn't started a quest
            bossAlive == "spawning" -> "Spawning a $slayerName $slayerLevel boss."
            bossAlive == "slaying" -> "Slaying a $slayerName $slayerLevel boss."
            bossAlive == "slain" -> "Finished slaying a $slayerName $slayerLevel boss."
            else -> "Something went wrong with slayer detection!"
        }
    }),

    CUSTOM({
        SkyHanniMod.feature.misc.discordRPC.customText.get() // custom field in the config
    }),

    AUTO({
        var autoReturn = ""
        for (statusID in SkyHanniMod.feature.misc.discordRPC.autoPriority) { // for every dynamic that the user wants to see...
            val autoStatus = AutoStatus.entries[statusID]
            val result =
                autoStatus.correspondingDiscordStatus.getDisplayString() // get what would happen if we were to display it
            if (result != autoStatus.placeholderText) { // if that value is useful, display it
                autoReturn = result
                break
            }
        }
        if (autoReturn == "") { // if we didn't find any useful information, display the fallback
            val statusNoAuto = DiscordStatus.entries.toMutableList()
            statusNoAuto.remove(AUTO)
            autoReturn = statusNoAuto[SkyHanniMod.feature.misc.discordRPC.auto.get()].getDisplayString()
        }
        autoReturn
    }),

    CROP_MILESTONES({
        val crop = InventoryUtils.getItemInHand()?.getCropType()
        val cropCounter = crop?.getCounter()
        val tier = cropCounter?.let { getTierForCropCount(it, crop) }

        val progress = tier?.let {
            LorenzUtils.formatPercentage(crop.progressToNextLevel())
        } ?: 100 // percentage to next milestone

        if (tier != null) {
            "${crop.cropName}: ${if (!crop.isMaxed()) "Milestone $tier ($progress)" else "MAXED (${cropCounter.addSeparators()} crops collected)"}"
        } else AutoStatus.CROP_MILESTONES.placeholderText
    }),

    PETS({
        ProfileStorageData.profileSpecific?.currentPet?.let {
            val colorCode = it.substring(1..2).first()
            val petName = it.substring(2)
            val petLevel = getCurrentPet()?.petLevel?.currentLevel ?: "?"

            "[Lvl $petLevel] ${colorCodeToRarity(colorCode)} $petName"
        } ?: "No pet equipped"
    }),

    // Dynamic-only
    STACKING({
        // Logic for getting the currently held stacking enchant is from Skytils, except for getExtraAttributes() which they got from BiscuitDevelopment

        fun getExtraAttributes(item: ItemStack?): NBTTagCompound? {
            return if (item == null || !item.hasTagCompound()) {
                null
            } else item.getSubCompound("ExtraAttributes", false)
        }

        val itemInHand = InventoryUtils.getItemInHand()
        val itemName = itemInHand?.displayName?.removeColor() ?: ""

        val extraAttributes = getExtraAttributes(itemInHand)

        fun getProgressPercent(amount: Int, levels: List<Int>): String {
            var currentLevel = 0
            var percent = "MAXED"
            for (level in levels.indices) {
                if (amount > levels[level]) {
                    currentLevel++
                    continue
                }
                percent = if (amount.toDouble() == 0.0) {
                    ""
                } else {
                    LorenzUtils.formatPercentage((amount.toDouble() - levels[level - 1]) / (levels[level] - levels[level - 1]))
                }
                break
            }
            return percent
        }

        var stackingReturn = AutoStatus.STACKING.placeholderText
        if (extraAttributes != null) {
            val enchantments = extraAttributes.getCompoundTag("enchantments")
            var stackingEnchant = ""
            for (enchant in stackingEnchants.keys) {
                if (extraAttributes.hasKey(stackingEnchants[enchant]?.get("nbtNum").toString())) {
                    stackingEnchant = enchant
                    break
                }
            }
            val levels = stackingEnchants[stackingEnchant]?.get("levels") as? List<Int> ?: listOf(0)
            val level = enchantments.getInteger(stackingEnchant)
            val amount = extraAttributes.getInteger(stackingEnchants[stackingEnchant]?.get("nbtNum").toString())
            val stackingPercent = getProgressPercent(amount, levels)

            stackingReturn =
                if (stackingPercent == "" || amount == 0) AutoStatus.STACKING.placeholderText // outdated info is useless for AUTO
                else "$itemName: ${stackingEnchant.firstLetterUppercase()} $level ($stackingPercent)" // Hecatomb 100: (55.55%)
        }
        stackingReturn

    }),

    DUNGEONS({
        if (!DungeonAPI.inDungeon()) {
            AutoStatus.DUNGEONS.placeholderText
        } else {
            val boss = DungeonAPI.getCurrentBoss()
            if (boss == null) {
                "Unknown dungeon boss"
            } else {
                val floor = DungeonAPI.dungeonFloor ?: AutoStatus.DUNGEONS.placeholderText
                val amountKills = DungeonAPI.bossStorage?.get(boss)?.addSeparators() ?: "Unknown"
                val time = DungeonAPI.getTime()
                "$floor Kills: $amountKills ($time)"
            }
        }
    }),

    AFK({
        if (beenAfkFor.passedSince() > 5.minutes) "AFK for ${
            beenAfkFor.passedSince().format(maxUnits = 1, longName = true)
        }"
        else AutoStatus.AFK.placeholderText
    })
    ;

    fun getDisplayString(): String {
        if (displayMessageSupplier != null) {
            return displayMessageSupplier.get()
        }
        return ""
    }

}

enum class AutoStatus(val placeholderText: String, val correspondingDiscordStatus: DiscordStatus) {
    CROP_MILESTONES("Not farming!", DiscordStatus.CROP_MILESTONES),
    SLAYER("Planning to do a slayer quest", DiscordStatus.SLAYER),
    STACKING("Stacking placeholder (should never be visible)", DiscordStatus.STACKING),
    DUNGEONS("Dungeons placeholder (should never be visible)", DiscordStatus.DUNGEONS),
    AFK("This person is not afk (should never be visible)", DiscordStatus.AFK),
    ;
}
