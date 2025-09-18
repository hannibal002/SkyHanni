package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.chat.TabCompletionEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HypixelCommands

@SkyHanniModule
object ShortenCallCommand {

    private val config get() = SkyHanniMod.feature.misc.commands

    // These are supposed to be in constants in that repo hannibal owns I guess
    private val calls = mutableListOf(
        "alchemist",
        "anita",
        "aranya",
        "blacksmith",
        "brynmor",
        "builder",
        "ahone",
        "dean",
        "duncan",
        "dusk",
        "einary",
        "elizabeth",
        "elle",
        "fann",
        "fearmongerer",
        "fred",
        "geo",
        "george",
        "hoppity",
        "igrupan",
        "jacob",
        "jax",
        "jotraeline",
        "jotraelinegreatforge", // same as jotraeline
        "forge", //same as  jotraeline
        "kat",
        "kaus",
        "kiara",
        "kuudra",
        "kuudragatekeeper", //same as kuudra
        "lumber",
        "maddox",
        "slayer", //same as  maddox
        "maxwell",
        "mort",
        "odger",
        "ophelia",
        "oringo",
        "pablo",
        "plumberjoe",
        "plumber", // same as plumberjoe
        "mismyla",
        "nyx",
        "roddy",
        "rollim",
        "rusty",
        "shifty",
        "sirih",
        "sirius",
        "stjerry",
        "suus",
        "tiathefairy",
        "tia", // same as tia the fairy
        "tomioka",
        "trevor",
        "trinity",
        "vincent",
        "walter",
        "woolweaver",
        "wool", // same as woolweaver
        "zog",
        "bingo",
        "alixer"
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        if (!config.shortenCall) return

        val message = event.message
        if (!message.startsWith("/")) return

        val command = message.lowercase().removePrefix("/").trimEnd()

        if (command in calls) {
            event.cancel()
            HypixelCommands.call(command)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTabComplete(event: TabCompletionEvent) {
        if (!config.shortenCall) return

        if (event.leftOfCursor.contains(" ")) return

        val lastWord = event.lastWord.lowercase().removePrefix("/")
        val matchingCalls = calls.filter { it.startsWith(lastWord) }.map { "/$it" }

        event.addSuggestions(matchingCalls)
    }
}
