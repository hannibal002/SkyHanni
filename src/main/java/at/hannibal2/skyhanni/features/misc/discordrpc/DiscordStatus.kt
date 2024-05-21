package at.hannibal2.skyhanni.features.misc.discordrpc

// SkyblockAddons code, adapted for SkyHanni with some additions and fixes

import at.hannibal2.skyhanni.data.ActionBarStatsData
import at.hannibal2.skyhanni.data.GardenCropMilestones.getCounter
import at.hannibal2.skyhanni.data.GardenCropMilestones.getTierForCropCount
import at.hannibal2.skyhanni.data.GardenCropMilestones.isMaxed
import at.hannibal2.skyhanni.data.GardenCropMilestones.progressToNextLevel
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PetAPI
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenAPI.getCropType
import at.hannibal2.skyhanni.features.misc.compacttablist.AdvancedPlayerList
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.colorCodeToRarity
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.StringUtils.findMatcher
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.matchFirst
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.TimeUtils.formatted
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.github.moulberry.notenoughupdates.miscfeatures.PetInfoOverlay.getCurrentPet
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import kotlin.time.Duration.Companion.minutes

var lastKnownDisplayStrings: MutableMap<DiscordStatus, String> =
    mutableMapOf() // if the displayMessageSupplier is ever a placeholder, return from this instead

private val patternGroup = RepoPattern.group("discordstatus.new")

// Samples: Revenant Horror I; Tarantula Broodfather IV
private val slayerPattern by patternGroup.pattern(
    "slayer",
    "(?<name>(?:\\w| )*) (?<level>[IV]+)"
)
private val ownerPattern by patternGroup.pattern(
    "owner",
    "Owner: (?<owner>\\w+)"
)
private val bitsPattern by patternGroup.pattern(
    "bits",
    "Bits: (?<bits>[\\d|,]+)[\\d|.]*"
)
private val motesPattern by patternGroup.pattern(
    "motes",
    "Motes: (?<motes>[\\d,]+)"
)
private val pursePattern by patternGroup.pattern(
    "purse",
    "(?:Purse|Piggy): (?<purse>[\\d,]+)[\\d.]*"
)


private fun getVisitingName(): String {
    val tabData = TabListData.getTabList()

    for (line in tabData) {
        val colorlessLine = line.removeColor()
        ownerPattern.findMatcher(colorlessLine) {
            return group("owner")
        }
    }
    return "Someone"
}

var beenAfkFor = SimpleTimeMark.now()

fun getPetDisplay(): String = PetAPI.currentPet?.let {
    val colorCode = it.substring(1..2).first()
    val petName = it.substring(2)
    val petLevel = getCurrentPet()?.petLevel?.currentLevel ?: "?"

    "[Lvl $petLevel] ${colorCodeToRarity(colorCode)} $petName"
} ?: "No pet equipped"

private fun getCropMilestoneDisplay(): String {
    val crop = InventoryUtils.getItemInHand()?.getCropType()
    val cropCounter = crop?.getCounter()
    val allowOverflow = GardenAPI.config.cropMilestones.overflow.discordRPC
    val tier = cropCounter?.let { getTierForCropCount(it, crop, allowOverflow) }
    val progress = tier?.let {
        LorenzUtils.formatPercentage(crop.progressToNextLevel(allowOverflow))
    } ?: 100 // percentage to next milestone

    if (tier == null) return AutoStatus.CROP_MILESTONES.placeholderText

    val text = if (crop.isMaxed(allowOverflow)) {
        "MAXED (${cropCounter.addSeparators()} crops)"
    } else {
        "Milestone $tier ($progress)"
    }
    return "${crop.cropName}: $text"
}

enum class DiscordStatus(private val displayMessageSupplier: (() -> String?)) {

    NONE({ null }),

    LOCATION({
        var location = LorenzUtils.skyBlockArea?.removeColor() ?: "invalid"
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

        val coins = scoreboard.map { it.removeColor() }.matchFirst(pursePattern) {
            group("purse")
        }
        val motes = scoreboard.map { it.removeColor() }.matchFirst(motesPattern) {
            group("motes")
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
        val bits = scoreboard.map { it.removeColor() }.matchFirst(bitsPattern) {
            group("bits")
        }

        when (bits) {
            "1" -> "1 Bit"
            null -> "0 Bits"
            else -> "$bits Bits"
        }
    }),

    STATS({
        val statString = if (!RiftAPI.inRift()) {
            "❤${ActionBarStatsData.HEALTH.value} ❈${ActionBarStatsData.DEFENSE.value} ✎${ActionBarStatsData.MANA.value}"
        } else {
            "${ActionBarStatsData.RIFT_TIME.value}ф ✎${ActionBarStatsData.MANA.value}"
        }
        if (ActionBarStatsData.MANA.value != "") {
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
        val sbLevel = AdvancedPlayerList.tabPlayerData[LorenzUtils.getPlayerName()]?.sbLevel?.toString() ?: "?"
        var profile = "SkyBlock Level: [$sbLevel] on "

        profile += when {

            LorenzUtils.isIronmanProfile -> "♲"
            LorenzUtils.isBingoProfile -> "Ⓑ"
            LorenzUtils.isStrandedProfile -> "☀"
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


        for (line in ScoreboardData.sidebarLinesFormatted) {
            val noColorLine = line.removeColor()
            val match = slayerPattern.matcher(noColorLine)
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
        DiscordRPCManager.config.customText.get() // custom field in the config
    }),

    AUTO({
        var autoReturn = ""
        for (statusID in DiscordRPCManager.config.autoPriority) { // for every dynamic that the user wants to see...
            // TODO, change functionality to use enum rather than ordinals
            val autoStatus = AutoStatus.entries[statusID.ordinal]
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
            autoReturn = statusNoAuto[DiscordRPCManager.config.auto.get().ordinal].getDisplayString()
        }
        autoReturn
    }),

    CROP_MILESTONES({ getCropMilestoneDisplay() }),

    PETS({ getPetDisplay() }),

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
            var percent = "MAXED"
            for (level in levels.indices) {
                if (amount > levels[level]) {
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
            for (enchant in DiscordRPCManager.stackingEnchants) {
                if (extraAttributes.hasKey(enchant.value.statName)) {
                    stackingEnchant = enchant.key
                    break
                }
            }
            val levels = DiscordRPCManager.stackingEnchants[stackingEnchant]?.levels ?: listOf(0)
            val level = enchantments.getInteger(stackingEnchant)
            val amount = extraAttributes.getInteger(DiscordRPCManager.stackingEnchants[stackingEnchant]?.statName)
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
        if (beenAfkFor.passedSince() > 5.minutes) {
            val format = beenAfkFor.passedSince().format(maxUnits = 1, longName = true)
            "AFK for $format"
        } else AutoStatus.AFK.placeholderText
    })
    ;

    fun getDisplayString(): String = displayMessageSupplier() ?: ""
}

enum class AutoStatus(val placeholderText: String, val correspondingDiscordStatus: DiscordStatus) {
    CROP_MILESTONES("Not farming!", DiscordStatus.CROP_MILESTONES),
    SLAYER("Planning to do a slayer quest", DiscordStatus.SLAYER),
    STACKING("Stacking placeholder (should never be visible)", DiscordStatus.STACKING),
    DUNGEONS("Dungeons placeholder (should never be visible)", DiscordStatus.DUNGEONS),
    AFK("This person is not afk (should never be visible)", DiscordStatus.AFK),
    ;
}
