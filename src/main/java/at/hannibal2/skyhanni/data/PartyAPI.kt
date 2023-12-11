package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpaceAndResets
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.random.Random

object PartyAPI {
    // TODO USE SH-REPO
    private val youJoinedPartyPattern = "§eYou have joined (?<name>.*)'s §eparty!".toPattern()
    private val othersJoinedPartyPattern = "(?<name>.*) §ejoined the party.".toPattern()
    // §dParty Finder §f> §bSkirtwearer §ejoined the group! (§3Combat Level 46§e)
    private val kuudraFinderJoinPattern = "§dParty Finder §f> (?<name>.*?) §ejoined the group! \\(§[a-fA-F0-9]+Combat Level (\\d+)§e\\)".toPattern()
    // §dParty Finder §f> §bSkirtwearer §ejoined the dungeon group! (§bArcher Level 22§e)
    private val dungeonFinderJoinPattern = "§dParty Finder §f> (?<name>.*?) §ejoined the dungeon group! \\(§[a-fA-F0-9].* Level \\d+§[a-fA-F0-9]\\)".toPattern()
    private val othersInThePartyPattern = "§eYou'll be partying with: (?<names>.*)".toPattern()
    private val otherLeftPattern = "(?<name>.*) §ehas left the party.".toPattern()
    private val otherKickedPattern = "(?<name>.*) §ehas been removed from the party.".toPattern()
    private val otherOfflineKickedPattern = "§eKicked (?<name>.*) because they were offline.".toPattern()
    private val otherDisconnectedPattern =
        "(?<name>.*) §ewas removed from your party because they disconnected.".toPattern()
    private val transferPattern = "The party was transferred to .* because (?<name>.*) left".toPattern()
    private val disbandedPattern = ".* §ehas disbanded the party!".toPattern()
    private val kickedPattern = "§eYou have been kicked from the party by .* §e".toPattern()
    private val partyMembersStartPattern = "§6Party Members \\(\\d+\\)".toPattern()
    private val partyMemberListPattern = "Party (?:Leader|Moderators|Members): (?<names>.*)".toPattern()

    val partyMembers = mutableListOf<String>()

    fun listMembers() {
        val size = partyMembers.size
        if (size == 0) {
            LorenzUtils.chat("No tracked party members!")
            return
        }
        LorenzUtils.chat("Tracked party members §7($size) §f:", prefixColor = "§a")
        for (member in partyMembers) {
            LorenzUtils.chat(" §a- §7$member", false)
        }

        if (Random.nextDouble() < 0.1) {
            OSUtils.openBrowser("https://www.youtube.com/watch?v=iANP7ib7CPA")
            LorenzUtils.hoverableChat("§7Are You Ready To Party?", listOf("§b~Spongebob"), prefix = false)
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        val message = event.message.trimWhiteSpaceAndResets().removeResets()

        // new member joined

        youJoinedPartyPattern.matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            if (!partyMembers.contains(name)) partyMembers.add(name)
        }
        othersJoinedPartyPattern.matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            if (!partyMembers.contains(name)) partyMembers.add(name)
        }
        othersInThePartyPattern.matchMatcher(message) {
            for (name in group("names").split(", ")) {
                val playerName = name.cleanPlayerName()
                if (!partyMembers.contains(playerName)) partyMembers.add(playerName)
            }
        }
        kuudraFinderJoinPattern.matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            if (name == LorenzUtils.getPlayerName()) return@matchMatcher
            if (!partyMembers.contains(name)) partyMembers.add(name)
        }
        dungeonFinderJoinPattern.matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            if (name == LorenzUtils.getPlayerName()) return@matchMatcher
            if (!partyMembers.contains(name)) partyMembers.add(name)
        }

        // one member got removed
        otherLeftPattern.matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            partyMembers.remove(name)
        }
        otherKickedPattern.matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            partyMembers.remove(name)
        }
        otherOfflineKickedPattern.matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            partyMembers.remove(name)
        }
        otherDisconnectedPattern.matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            partyMembers.remove(name)
        }
        transferPattern.matchMatcher(message.removeColor()) {
            val name = group("name").cleanPlayerName()
            partyMembers.remove(name)
        }

        // party disbanded
        disbandedPattern.matchMatcher(message) {
            partyMembers.clear()
        }
        kickedPattern.matchMatcher(message) {
            partyMembers.clear()
        }
        if (message == "§eYou left the party." ||
            message == "§cThe party was disbanded because all invites expired and the party was empty." ||
            message == "§cYou are not currently in a party."
        ) {
            partyMembers.clear()
        }

        // party list
        partyMembersStartPattern.matchMatcher(message.removeResets()) {
            partyMembers.clear()
        }

        partyMemberListPattern.matchMatcher(message.removeColor()) {
            for (name in group("names").split(" ● ")) {
                val playerName = name.replace(" ●", "").cleanPlayerName()
                if (playerName == LorenzUtils.getPlayerName()) continue
                if (!partyMembers.contains(playerName)) partyMembers.add(playerName)
            }
        }
    }
}
