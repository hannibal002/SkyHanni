package at.hannibal2.skyhanni.features.misc.discordrpc

// SkyblockAddons code, adapted for SkyHanni

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HyPixelData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import java.util.function.Supplier

enum class DiscordStatus(private val displayMessageSupplier: Supplier<String>?) { // implements "ButtonSelect:SelectItem". no idea how to translate that into skyhanni


    NONE(null),

    LOCATION({
        val location = LorenzUtils.skyBlockArea
        if (location ==  "Your Island") {
            "Private Island"
        }
        else {
            location // looks slightly weird if visiting someone else's island, I was thinking of using LorenzUtils.skyblockIsland to determine if they're visitng but it takes too long to load, so we'd have to put in some sort of artificial delay like what I did in DiscordRPCManager.onWorldChange.
            // after that, use the tablist "Owner:" line to get the person we're visiting but i don't know if that'll work with coops and you'd have to deal with color codes as well
            // then again, I'm pretty sure sba had "'s Island" without the name filled in this entire time so I'd rather have [RANK] NameThatGetsCutOff for example than 's Island
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

        if (coins ==  "1") {
            "1 Coin"
        }
        else {
            "$coins Coins"
        }
    }),

    BITS({
       val scoreboard = ScoreboardData.sidebarLinesFormatted
       var bits = ""

        for (line in scoreboard) {
            if (line.startsWith("Bits: ")) {
                bits = line.subSequence(8 until line.length).toString()
            }
        }

        if (bits ==  "1") {
            "1 Bit"
        }
        else {
            "$bits Bits"
        }
    }),

    // I'm not doing zealot counter. Who even farms those anymore?

    ITEM({
        val player: net.minecraft.client.entity.EntityPlayerSP = net.minecraft.client.Minecraft.getMinecraft().thePlayer
        if (player.heldItem !=  null) {
            String.format("Holding ${player.heldItem.displayName.removeColor()}")
        }
        else {
            "No item in hand"
        }
    }),

    TIME({

        fun formatNum(num: Int): Int {
            val rem = num % 10
            var returnNum = num - rem // we're so fancy with our precision down to the nearest minute that eventually gets floor()ed away anyway
            if (returnNum ==  0) {
                returnNum = "0$num".toInt() // and this is so that if the minute value is ever a single digit (0 after being floored), it displays as 00 because 12:0pm just looks bad
            }
            return returnNum
        }

        val date: SkyBlockTime = SkyBlockTime.now()
        val hour = if (date.hour > 12) date.hour - 12 else date.hour
        val timeOfDay = if (date.hour > 11) "pm" else "am" // hooray for 12-hour clocks
        val dateString = "${SkyBlockTime.monthName(date.month)} ${date.day}${SkyBlockTime.daySuffix(date.day)}, $hour:${formatNum(date.minute)}$timeOfDay" // Early Winter 1st, 12:00pm
        if (dateString !=  "") {
            dateString
        }
        else {
            ""
        }
    }),

    PROFILE({
        HyPixelData.profileName.firstLetterUppercase()
    }),

    CUSTOM({
        SkyHanniMod.feature.misc.custom // custom field in the config
    })

    // Someone else can do stats (requires actionbar shenanigans), slayers (requires scoreboard: regex + ScoreboardData.sidebarLinesFormatted), or auto (requires slayers) if they want to
    // See SkyblockAddons code for reference
    ;


    fun getDisplayString(currentEntry: DiscordStatusEntry): String {
        DiscordRPCManager.currentEntry = currentEntry
        if (displayMessageSupplier != null) {
            return displayMessageSupplier.get()
        }
        return ""
    }


//    @Override
//    fun getName(): String {
//        return title
//    }
//
//    @Override
//    fun getDescription(): String {
//        return description
//    }
}