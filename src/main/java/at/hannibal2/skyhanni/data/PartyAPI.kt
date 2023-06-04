package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PartyAPI {
    companion object {
        val partyMembers = mutableListOf<String>()
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        val message = event.message
        // new member joined
        "§eYou have joined §r(?<name>.*)'s §r§eparty!".toPattern().matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            partyMembers.add(name)
            println("partyMembers: $partyMembers")
        }
        "(?<name>.*) §r§ejoined the party.".toPattern().matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            partyMembers.add(name)
            println("partyMembers: $partyMembers")
        }
        "§eYou'll be partying with: §r(?<names>.*)".toPattern().matchMatcher(message) {
            for (name in group("names").split(", ")) {
                partyMembers.add(name.cleanPlayerName())
            }
            println("partyMembers: $partyMembers")
        }

        // one member got removed
        "(?<name>.*) §r§ehas left the party.".toPattern().matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            partyMembers.remove(name)
            println("partyMembers: $partyMembers")
        }
        "(?<name>.*) §r§ehas been removed from the party.".toPattern().matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            partyMembers.remove(name)
            println("partyMembers: $partyMembers")
        }
        "(?<name>.*) neuberddo§r§e because they were offline.".toPattern().matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            partyMembers.remove(name)
            println("partyMembers: $partyMembers")
        }

        // party disbanded
        ".* §r§ehas disbanded the party!".toPattern().matchMatcher(message) {
            partyMembers.clear()
            println("partyMembers: $partyMembers")
        }
        "§eYou have been kicked from the party by §r.* §r§e".toPattern().matchMatcher(message) {
            partyMembers.clear()
            println("partyMembers: $partyMembers")
        }
        if (message == "§eYou left the party." ||
            message == "§cThe party was disbanded because all invites expired and the party was empty."
        ) {
            partyMembers.clear()
            println("partyMembers: $partyMembers")
        }
    }
}
