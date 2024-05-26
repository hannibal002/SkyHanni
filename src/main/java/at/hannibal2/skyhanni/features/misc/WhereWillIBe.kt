package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.WinterAPI
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.pow
import kotlin.random.Random

object WhereWillIBe {
    private val nonIslands = listOf(IslandType.NONE, IslandType.UNKNOWN, IslandType.MINESHAFT)
    private val privateOrGuest =
        listOf(IslandType.PRIVATE_ISLAND_GUEST, IslandType.PRIVATE_ISLAND, IslandType.GARDEN_GUEST)
    private val onceOnlyIslands =
        listOf(IslandType.DARK_AUCTION, IslandType.KUUDRA_ARENA, IslandType.CATACOMBS, IslandType.THE_RIFT)

    @SubscribeEvent
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        if (!LorenzUtils.onHypixel) return
        if (event.message.lowercase() != "/wherewillibe") return
        event.cancel()
        val islandsAsList = IslandType.entries.toList().filter { it !in nonIslands }.toMutableList()
        if (SkyBlockTime.now().month != 12) islandsAsList.remove(IslandType.WINTER)
        var lastUsedMillis = SimpleTimeMark.now().toMillis()
        var lastIsland = IslandType.NONE
        var chosenIsland = IslandType.NONE
        ChatUtils.chat("§aYour future servers:", false)
        repeat(Random.nextInt(2, 11)) {
            val chosenIslands =
                if (Random.nextBoolean())
                    islandsAsList.filter { it !in privateOrGuest }
                else
                    islandsAsList
            val randomMillis =
                Random.nextLong(
                    lastUsedMillis,
                    lastUsedMillis + Random.nextLong(
                        10000,
                        2.0.pow(26).toLong()
                    )
                )
            lastUsedMillis = randomMillis
            while (chosenIsland == lastIsland)
                chosenIsland = chosenIslands.shuffled().first()
            lastIsland = chosenIsland
            if (chosenIsland in onceOnlyIslands)
                islandsAsList.remove(chosenIsland)
            val randomServerID = "${miniOrMega(chosenIsland)}${randServerNumber()}${oneOrTwoLetters()}"
            val randomIsland = "SkyBlock (${chosenIsland.displayName})"
            ChatUtils.chat("§e${formattedDate(Date(randomMillis))} - $randomServerID - $randomIsland", false)
        }
    }

    private fun miniOrMega(chosenIsland: IslandType): String =
        if (chosenIsland == IslandType.HUB)
            listOf("mini", "mega").random()
        else
            "mini"

    private fun randServerNumber(): Int =
        Random.nextInt(
            10,
            401
        )

    private fun oneOrTwoLetters(charRange: CharRange = ('A'..'Z')): String =
        if (Random.nextBoolean())
            "${charRange.random()}"
        else
            "${charRange.random()}${charRange.random()}"

    private fun formattedDate(date: Date): String =
        SimpleDateFormat(
            "HH:mm:ss dd-MM-yy"
        ).format(
            date
        )
}
