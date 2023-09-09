package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PartyMemberOutlines {
    private val config get() = SkyHanniMod.feature.misc
    private val usernameRegex = "(?:(?:\\[[A-Z]+\\+{0,2}] )?([A-Za-z0-9_\\-\\*]+))"
    private val partyLeaveMessage = "^You left the party.$".toRegex()
    private val partyDisbandedMessage = "^The party was disbanded because all invites expired and the party was empty.$".toRegex()
    private val partyDisbandMessage = "$usernameRegex has disbanded the party!$".toRegex()
    private val joinedPartyMessage = "^You have joined $usernameRegex's party!$".toRegex()
    private val partyJoinMessage = "^$usernameRegex joined the party.".toRegex()
    private val partyLeftMessage = "^$usernameRegex has left the party.".toRegex()
    private val partyKickMessage = "^$usernameRegex has been removed from the party.".toRegex()
    private val partyListMessage = "^Party (?:Leaders|Moderators|Members): (?:$usernameRegex ● )+".toRegex()
    private val partyListMembers = "(?:$usernameRegex ● )".toRegex()
    private val notInParty = "^You are not currently in a party".toRegex()
    private val otherPartyMembers = "^You'll be partying with: ".toRegex()
    private val partyingWith = "(?:$usernameRegex(?:, |\$))".toRegex()

    private var partyMembers = mutableSetOf<String>()

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!isEnabled()) return

        val message = event.message.removeColor()

        if (message.matches(partyLeaveMessage) || message.matches(partyDisbandMessage) || message.matches(partyDisbandedMessage) || message.matches(notInParty)) {
            partyMembers.clear()
        } else if (message.matches(joinedPartyMessage)) {
            partyMembers.add(joinedPartyMessage.find(message)!!.groupValues[1])
        } else if (message.matches(partyJoinMessage)) {
            partyMembers.add(partyJoinMessage.find(message)!!.groupValues[1])
        } else if (message.matches(otherPartyMembers)) {
            val usernames = partyingWith.findAll(message).map { it.groupValues[1] }
            partyMembers.addAll(usernames)
        } else if (message.matches(partyKickMessage)) {
            partyMembers.remove(partyKickMessage.find(message)!!.groupValues[1])
        } else if (message.matches(partyLeftMessage)) {
            partyMembers.remove(partyLeftMessage.find(message)!!.groupValues[1])
        } else if (message.matches(partyListMessage)) {
            val usernames = partyListMembers.findAll(message).map { it.groupValues[1] }
            partyMembers = usernames.toMutableSet()
        }
    }

    @SubscribeEvent
    fun onRenderEntityOutlines(event: RenderEntityOutlineEvent) {
        if (isEnabled() && event.type === RenderEntityOutlineEvent.Type.XRAY) {
            event.queueEntitiesToOutline { entity -> getEntityOutlineColor(entity) }
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && !LorenzUtils.inDungeons && config.highlightPartyMembers

    private fun getEntityOutlineColor(entity: Entity): Int? {
        if (entity !is EntityOtherPlayerMP || !partyMembers.contains(entity.name)) return null

        return LorenzColor.GREEN.toColor().rgb
    }
}