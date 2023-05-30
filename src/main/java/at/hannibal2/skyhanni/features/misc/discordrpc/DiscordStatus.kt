package at.hannibal2.skyhanni.features.misc.discordrpc

// SkyblockAddons code, adapted for SkyHanni

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ActionBarStatsData
import at.hannibal2.skyhanni.data.GardenCropMilestones.Companion.getCounter
import at.hannibal2.skyhanni.data.GardenCropMilestones.Companion.getTierForCrops
import at.hannibal2.skyhanni.data.GardenCropMilestones.Companion.progressToNextLevel
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.garden.GardenAPI.getCropType
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.colorCodeToRarity
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.miscfeatures.PetInfoOverlay.getCurrentPet
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import java.util.function.Supplier
import java.util.regex.Pattern

var lastKnownDisplayStrings: MutableMap<DiscordStatus, String> =
    mutableMapOf() // if the displayMessageSupplier is ever a placeholder, return from this instead

enum class DiscordStatus(private val displayMessageSupplier: Supplier<String>?) {

    NONE(null),

    LOCATION({
        var location = LorenzUtils.skyBlockArea
        if (location == "Your Island") location = "Private Island"
        if (location != "None" && location != "invalid") {
            lastKnownDisplayStrings[LOCATION] = location
        }
        lastKnownDisplayStrings[LOCATION] ?: "None"// only display None if we don't have a last known area
        /**
         *    looks slightly weird if visiting someone else's island,
         *    I was thinking of using LorenzUtils.skyblockIsland to determine if they're visiting,
         *    but it takes too long to load, so we 'd have to put in some sort of artificial delay
         *    like what I did in DiscordRPCManager.onWorldChange.
         *    after that, use the tab-list "Owner:" line to get the person we're visiting, but I don't know
         *    if that'll work with coops, and you'd have to deal with color codes as well
         *    anyway, I'm pretty sure sba had "'s Island" without the name filled in this entire time,
         *    so I'd rather have [RANK] NameThatGetsCutOff for example than 's Island
         */
    }),

    PURSE({
        val scoreboard = ScoreboardData.sidebarLinesFormatted
        val purseRegex =
            Regex("""(?:Purse|Piggy): ([\d,]+)[\d.]*""") // Matches coins amount in purse or piggy, with optional decimal points
        val coins = scoreboard.firstOrNull { purseRegex.matches(it.removeColor()) }?.let {
            purseRegex.find(it.removeColor())?.groupValues?.get(1)
        }
        if (coins == "1") {
            lastKnownDisplayStrings[PURSE] = "1 Coin"
        } else if (coins != "") {
            lastKnownDisplayStrings[PURSE] = "$coins Coins"
        }
        lastKnownDisplayStrings[PURSE] ?: ""
    }),

    BITS({
        val scoreboard = ScoreboardData.sidebarLinesFormatted
        val bitsRegex = Regex("""Bits: ([\d|,]+)[\d|.]*""")
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
        var statString = ""
        for (item in groups.indices) {
            when (groups[item]) {
                "❤" -> statString = "❤${groups[item - 1]} "
                "❈ Defense" -> statString = "$statString❈${groups[item - 1]} "
                "✎" -> statString = "$statString✎${groups[item - 1]} "
            }
        }
        if (groups.isNotEmpty()) {
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
        fun formatNum(num: Int): Int {
            val rem = num % 10
            var returnNum = num - rem // floor()
            if (returnNum == 0) {
                returnNum = "0$num".toInt()
                /**
                 * and this is so that if the minute value is ever
                 * a single digit (0 after being floored), it displays as 00 because 12:0pm just looks bad
                 */
            }
            return returnNum
        }

        val date: SkyBlockTime = SkyBlockTime.now()
        val hour = if (date.hour > 12) date.hour - 12 else date.hour
        val timeOfDay = if (date.hour > 11) "pm" else "am" // hooray for 12-hour clocks
        "${SkyBlockTime.monthName(date.month)} ${date.day}${SkyBlockTime.daySuffix(date.day)}, $hour:${formatNum(date.minute)}$timeOfDay" // Early Winter 1st, 12:00pm
    }),

    PROFILE({
        HypixelData.profileName.firstLetterUppercase()
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
            if (match.matches()) {
                slayerName = match.group("name")
                slayerLevel = match.group("level")
            } else if (noColorLine == "Slay the boss!") bossAlive = "slaying"
            else if (noColorLine == "Boss slain!") bossAlive = "slain"
        }

        if (slayerLevel == "") "Planning to do a slayer quest"// selected slayer in rpc but hasn't started a quest
        else if (bossAlive == "spawning") "Spawning a $slayerName $slayerLevel boss."
        else if (bossAlive == "slaying") "Slaying a $slayerName $slayerLevel boss."
        else if (bossAlive == "slain") "Finished slaying a $slayerName $slayerLevel boss."
        else "Something went wrong with slayer detection!"
    }),

    CUSTOM({
        SkyHanniMod.feature.misc.discordRPC.customText.get() // custom field in the config
    }),

    AUTO({
        val slayerResult = SLAYER.displayMessageSupplier!!.get()
        val milestoneResult = try {
            CROP_MILESTONES.displayMessageSupplier!!.get()
        } catch (e: Exception) {
            "Unable to get milestone"
        }
        if (slayerResult != "Planning to do a slayer quest") slayerResult
        else if (milestoneResult != "Unable to get milestone" && milestoneResult != "Unknown Item" && milestoneResult != "") milestoneResult
        else {
            val statusNoAuto = DiscordStatus.values().toMutableList()
            statusNoAuto.remove(AUTO)
            statusNoAuto[SkyHanniMod.feature.misc.discordRPC.auto.get()].getDisplayString()
        }
    }),

    CROP_MILESTONES({
        val crop = InventoryUtils.getItemInHand()?.getCropType()
        val cropCounter = crop?.getCounter()
        val tier = cropCounter?.let { getTierForCrops(it) }

        val progress = tier?.let {
            LorenzUtils.formatPercentage(crop.progressToNextLevel())
        } ?: 100 // percentage to next milestone

        if (tier != null) {
            lastKnownDisplayStrings[CROP_MILESTONES] = tier.let { "${crop.cropName}: Milestone $it ($progress)" }
        }
        lastKnownDisplayStrings[CROP_MILESTONES] ?: ""
    }),

    PETS({
        ProfileStorageData.profileSpecific?.currentPet?.let {
            val colorCode = it.substring(1..2).first()
            val petName = it.substring(2)
            val petLevel = getCurrentPet().petLevel.currentLevel

            "[Lvl $petLevel] ${colorCodeToRarity(colorCode)} $petName"
        } ?: "No pet equipped"
    })
    ;

    fun getDisplayString(): String {
        if (displayMessageSupplier != null) {
            return displayMessageSupplier.get()
        }
        return ""
    }
}
