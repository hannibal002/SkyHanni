package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpaceAndResets
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.random.Random

object PartyAPI {
    val partyMembers = mutableListOf<String>()

    fun listMembers() {
        val size = partyMembers.size
        if (size == 0) {
            LorenzUtils.chat("§e[SkyHanni] No tracked party members!")
            return
        }
        LorenzUtils.chat("§a[SkyHanni] Tracked party members §7($size) §f:")
        for (member in partyMembers) {
            LorenzUtils.chat(" §a- §7$member")
        }

        if (Random.nextDouble() < 0.1) {
            OSUtils.openBrowser("https://www.youtube.com/watch?v=iANP7ib7CPA")
            LorenzUtils.hoverableChat("§7Are You Ready To Party?", listOf("§b~Spongebob"))
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        val message = event.message.trimWhiteSpaceAndResets().removeResets()

        // new member joined
        "§eYou have joined (?<name>.*)'s §eparty!".toPattern().matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            if (!partyMembers.contains(name)) partyMembers.add(name)
        }
        "(?<name>.*) §ejoined the party.".toPattern().matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            if (!partyMembers.contains(name)) partyMembers.add(name)
        }
        "§eYou'll be partying with: (?<names>.*)".toPattern().matchMatcher(message) {
            for (name in group("names").split(", ")) {
                val playerName = name.cleanPlayerName()
                if (!partyMembers.contains(playerName)) partyMembers.add(playerName)
            }
        }

        // one member got removed
        "(?<name>.*) §ehas left the party.".toPattern().matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            partyMembers.remove(name)
        }
        "(?<name>.*) §ehas been removed from the party.".toPattern().matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            partyMembers.remove(name)
        }
        "§eKicked (?<name>.*) because they were offline.".toPattern().matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            partyMembers.remove(name)
        }
        "(?<name>.*) §ewas removed from your party because they disconnected.".toPattern().matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            partyMembers.remove(name)
        }
        "The party was transferred to .* because (?<name>.*) left".toPattern().matchMatcher(message.removeColor()) {
            val name = group("name").cleanPlayerName()
            partyMembers.remove(name)
        }

        // party disbanded
        ".* §ehas disbanded the party!".toPattern().matchMatcher(message) {
            partyMembers.clear()
        }
        "§eYou have been kicked from the party by .* §e".toPattern().matchMatcher(message) {
            partyMembers.clear()
        }
        if (message == "§eYou left the party." ||
            message == "§cThe party was disbanded because all invites expired and the party was empty." ||
            message == "§cYou are not currently in a party."
        ) {
            partyMembers.clear()
        }

        // party list
        "§6Party Members \\(\\d+\\)".toPattern().matchMatcher(message.removeResets()) {
            partyMembers.clear()
        }

        "Party (?:Leader|Moderators|Members): (?<names>.*)".toPattern().matchMatcher(message.removeColor()) {
            for (name in group("names").split(" ● ")) {
                val playerName = name.replace(" ●", "").cleanPlayerName()
                if (playerName == Minecraft.getMinecraft().thePlayer.name) continue
                if (!partyMembers.contains(playerName)) partyMembers.add(playerName)
            }
        }
    }
}