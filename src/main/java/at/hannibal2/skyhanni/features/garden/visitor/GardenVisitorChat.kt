package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorArrivalEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.player.RemotePlayer
import kotlin.time.Duration.Companion.seconds

/**
 * Handles chat filtering and visitor arrival notifications.
 * Filters spam messages and sends titles/chat messages when visitors arrive.
 */
@SkyHanniModule
object GardenVisitorChat {

    private val config get() = VisitorApi.config
    private val logger = LorenzLogger("garden/visitors/chat")

    private val patternGroup = RepoPattern.group("garden.visitor.chat")

    /**
     * REGEX-TEST: §a§r§aBanker Broadjaw §r§ehas arrived on your §r§aGarden§r§e!
     */
    private val visitorArrivePattern by patternGroup.pattern(
        "visitorarrive",
        ".* §r§ehas arrived on your §r§[ba]Garden§r§e!",
    )

    /**
     * REGEX-TEST: §e[NPC] §6Madame Eleanor Q. Goldsworth III§f: §r§fI'm here to put a value on your farm.
     * REGEX-TEST: §e[NPC] §aRhys§f: §r§fI found an unexplored cave while mining for titanium.
     */
    private val visitorChatMessagePattern by patternGroup.pattern(
        "visitorchat",
        "§e\\[NPC] (?<color>§.)?(?<name>.*)§f: §r.*",
    )

    /**
     * REGEX-TEST: §aYou gave some of the required items!
     */
    private val partialAcceptedPattern by patternGroup.pattern(
        "partialaccepted",
        "§aYou gave some of the required items!",
    )

    // TODO use event.chatComponent.string instead of event.message here
    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        handleArrivalMessage(event)
        handleVisitorMessage(event)
        handlePartialAccepted(event)
    }

    /**
     * Blocks the Hypixel arrival message if configured.
     */
    private fun handleArrivalMessage(event: SkyHanniChatEvent.Allow) {
        if (config.hypixelArrivedMessage && visitorArrivePattern.matcher(event.message).matches()) {
            event.blockedReason = "new_visitor_arrived"
        }
    }

    /**
     * Filters visitor chat messages to reduce spam.
     */
    private fun handleVisitorMessage(event: SkyHanniChatEvent.Allow) {
        // TODO use NpcChatEvent
        if (GardenApi.inGarden() && config.hideChat && hideVisitorMessage(event.message)) {
            event.blockedReason = "garden_visitor_message"
        }
    }

    /**
     * Reminds user to reopen visitor GUI after partial acceptance.
     */
    private fun handlePartialAccepted(event: SkyHanniChatEvent.Allow) {
        if (config.shoppingList.enabled) {
            partialAcceptedPattern.matchMatcher(event.message) {
                ChatUtils.chat("Talk to the visitor again to update the number of items needed!")
            }
        }
    }

    /**
     * Determines if a chat message should be hidden.
     * Hides messages from visitors but keeps messages from permanent NPCs like Jacob.
     */
    private fun hideVisitorMessage(message: String) = visitorChatMessagePattern.matchMatcher(message) {
        val color = group("color")
        if (color == null || color == "§e") return false // Non-visitor NPC, probably Jacob

        val name = group("name")
        if (name in setOf("Beth", "Maeve", "Spaceman")) return false

        val isInKnownVisitors = VisitorApi.getVisitorsMap().keys.any { it.removeColor() == name }

        return if (isInKnownVisitors) true
        else doesVisitorEntityExist(name)
    } ?: false

    /**
     * Checks if a visitor entity with the given name exists in the barn area.
     * Used as fallback when tab list hasn't updated yet.
     */
    private fun doesVisitorEntityExist(name: String) =
        EntityUtils.getEntitiesInBoundingBox<RemotePlayer>(GardenApi.barnArea).any {
            it.name.formattedTextCompatLessResets().trim().equals(name, true)
        }

    /**
     * Sends chat and title notifications when a visitor arrives.
     * Also triggers status update and item blinking effects.
     */
    @HandleEvent
    fun onVisitorArrival(event: VisitorArrivalEvent) {
        val visitor = event.visitor
        val name = visitor.visitorName

        GardenVisitorStatus.update()

        logger.log("New visitor detected: '$name'")

        if (SkyBlockUtils.lastWorldSwitch.passedSince() < 3.seconds) return

        sendArrivalNotification(visitor)
    }

    /**
     * Sends title and chat notifications based on config.
     */
    private fun sendArrivalNotification(visitor: VisitorApi.Visitor) {
        if (config.notificationTitle) {
            TitleManager.sendTitle("§eNew Visitor")
        }
        if (config.notificationChat) {
            val displayName = GardenVisitorColorNames.getColoredName(visitor.visitorName)
            ChatUtils.chat(
                componentBuilder {
                    append(displayName)
                    append(" is visiting your garden!")
                }
            )
        }
    }
}
