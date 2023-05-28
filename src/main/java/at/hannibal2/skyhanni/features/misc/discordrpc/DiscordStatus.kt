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

enum class DiscordStatus(private val displayMessageSupplier: Supplier<String>?) {

    NONE(null),

    LOCATION({
        val location = LorenzUtils.skyBlockArea
        if (location == "Your Island") {
            "Private Island"
        } else {
            location
            /**
             * looks slightly weird if visiting someone else's island,
             *    I was thinking of using LorenzUtils . skyblockIsland to determine if they're visiting,
             *    but it takes too long to load, so we 'd have to put in some sort of artificial delay
             *    like what I did in DiscordRPCManager.onWorldChange.
             *    after that, use the tab-list "Owner:" line to get the person we're visiting, but I don't know if
             *    that'll work with coops, and you'd have to deal with color codes as well
             *    as again, I'm pretty sure sba had "'s Island" without the name filled in this entire time,
             *    so I 'd rather have [RANK] NameThatGetsCutOff for example than 's Island
             */
        }
    }),

    PURSE({
        val scoreboard = ScoreboardData.sidebarLinesFormatted
        var coins = ""

        for (line in scoreboard) {
            if (line.startsWith("Purse: ") || line.startsWith("Piggy: ")) {
                coins = line.subSequence(9 until line.length).toString()
            }
        }

        if (coins == "1") "1 Coin" else "$coins Coins"
    }),

    BITS({
        var bits = ""
        for (line in ScoreboardData.sidebarLinesFormatted) {
            if (line.startsWith("Bits: ")) {
                bits = line.subSequence(8 until line.length).toString()
            }
        }

        when (bits) {
            "1" -> "1 Bit"
            "" -> "0 Bits"
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
        statString
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

        tier?.let { "${crop.cropName}: Milestone $it ($progress)" } ?: ""
    }),

    PETS({
        ProfileStorageData.profileSpecific?.currentPet?.let {
            val colorCode = it.substring(1..2).first()
            val petName = it.substring(2)
            val petLevel = getCurrentPet().petLevel.currentLevel

            "[Lvl $petLevel] ${colorCodeToRarity(colorCode)} $petName"
        } ?: ""
    })
    ;

    fun getDisplayString(): String {
        if (displayMessageSupplier != null) {
            return displayMessageSupplier.get()
        }
        return ""
    }
}
