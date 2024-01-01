package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpaceAndResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.random.Random

object PartyAPI {
    private val youJoinedPartyPattern by RepoPattern.pattern(
        "data.party.you.joined",
        "§eYou have joined (?<name>.*)'s §eparty!"
    )
    private val othersJoinedPartyPattern by RepoPattern.pattern(
        "data.party.others.joined",
        "(?<name>.*) §ejoined the party\\."
    )
    private val othersInThePartyPattern by RepoPattern.pattern(
        "data.party.others.inparty",
        "§eYou'll be partying with: (?<names>.*)"
    )
    private val otherLeftPattern by RepoPattern.pattern(
        "data.party.others.left",
        "(?<name>.*) §ehas left the party\\."
    )
    private val otherKickedPattern by RepoPattern.pattern(
        "data.party.others.kicked",
        "(?<name>.*) §ehas been removed from the party\\."
    )
    private val otherOfflineKickedPattern by RepoPattern.pattern(
        "data.party.others.offline",
        "§eKicked (?<name>.*) because they were offline\\."
    )
    private val otherDisconnectedPattern by RepoPattern.pattern(
        "data.party.others.disconnect",
        "(?<name>.*) §ewas removed from your party because they disconnected\\."
    )
    private val transferPattern by RepoPattern.pattern(
        "data.party.others.transfer",
        "The party was transferred to .* because (?<name>.*) left"
    )
    private val disbandedPattern by RepoPattern.pattern(
        "data.party.others.disband",
        ".* §ehas disbanded the party!"
    )
    private val kickedPattern by RepoPattern.pattern(
        "data.party.you.kicked",
        "§eYou have been kicked from the party by .* §e"
    )
    private val partyMembersStartPattern by RepoPattern.pattern(
        "data.party.members.start",
        "§6Party Members \\(\\d+\\)"
    )

    private val partyMemberListPattern by RepoPattern.pattern(
        "data.party.members.list",
        "Party (?:Leader|Moderators|Members): (?<names>.*)"
    )

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
