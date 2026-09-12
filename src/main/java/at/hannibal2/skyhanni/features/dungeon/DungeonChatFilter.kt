@file:Suppress("MaxLineLength", "RepoPatternRegexTestMissing")

package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.chat.ChatConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPatternGroup
import java.util.regex.Pattern

private typealias MessageTypes = ChatConfig.DungeonMessageTypes

@SkyHanniModule
object DungeonChatFilter {

    private val config get() = SkyHanniMod.feature.chat

    private val patternGroup = RepoPatternGroup("dungeon-chat-filter")

    // <editor-fold desc="Patterns, Messages, and Maps">
    // TODO: Add regex tests
    private val endPatterns by patternGroup.list(
        "end",
        ".* unlocked .* Essence x.*!",
        " {4}.* Essence x.*",
        ".*Experience \\(Team Bonus\\)"
    )

    private val abilityPatterns by patternGroup.list(
        "ability",
        "Your Guided Sheep hit .* enemy for .* damage.",
        "BUFF! You were splashed by .* with Healing VIII!",
        "You were healed for .* health by .*!",
        "You gained .* HP worth of absorption for 3s from .*!",
        ".* picked up your .* Orb!",
        "This ability is on cooldown for .*s.",
        ".* healed you for .* health!",
        "Your bone plating reduced the damage you took by .*!",
        ".* formed a tether with you!",
        "Your tether with .* healed you for .* health.",
        "Your Implosion hit .* enemy for .* damage.",
        "Your Spirit Pet healed .* for .* health!",
        "Your Spirit Pet hit .* enemy for .* damage.",
        "You need at least .* mana to activate this!",
        "You were healed for .* health by .*'s Healing Bow and gained .* Strength for 10 seconds.",
        ".* granted you .* strength for 20 seconds!",
        "Your fairy healed yourself for .* health!",
        "Your fairy healed .* for .* health!",
        ".* fairy healed you for .* health!",
        "Guided Sheep is now available!",
        "Creeper Veil Activated!",
        "Creeper Veil De-activated!",
        "Rapid Fire is ready to use! Press DROP to activate it!",
        "Castle of Stone is ready to use! Press DROP to activate it!",
        "Ragnarok is ready to use! Press DROP to activate it!",
        "Thunderstorm is ready to use! Press DROP to activate it!"
    )

    private val damagePatterns by patternGroup.list(
        "damage",
        ".* used .* on you!",
        "The .* struck you for .* damage!",
        "The .* hit you for .* damage!",
        ".* struck you for .* damage.",
        ".* hit you for .* damage.",
        ".* hit you for .* true damage.",
        ".* exploded, hitting you for .* damage.",
        ".* hit you with .* for .* damage!",
        ".* struck you for .* damage!",
        ".* struck you for .*!",
        "The Mage's Magma burnt you for .* true damage.",
        "Your .* hit .* (?:enemy|enemies) for .* damage.",
        "Mute silenced you!"
    )

    private val notPossiblePatterns by patternGroup.list(
        "not_possible",
        "You cannot hit the silverfish while it's moving!",
        "You cannot move the silverfish in that direction!",
        "There are blocks in the way!",
        "This chest has already been searched!",
        "This lever has already been used.",
        "You cannot do that in this room!",
        "You do not have the key for this door!",
        "You have already opened this dungeon chest!",
        "You cannot use abilities in this room!",
        "A mystical force in this room prevents you from using that ability!"
    )

    private val buffPatterns by patternGroup.list(
        "buff",
        "DUNGEON BUFF! .* found a Blessing of .*!.*",
        "DUNGEON BUFF! You found a Blessing of .*!.*",
        "DUNGEON BUFF! A Blessing of .* was found! .*",
        "A Blessing of .* was picked up!",
        ".* has obtained Blessing of .*!",
        " {5}Granted you .* & .*x ${SkyblockStat.STRENGTH.hypixelIcon} Strength.",
        " {5}Also granted you .* & .*x ${SkyblockStat.CRIT_DAMAGE.hypixelIcon} Crit Damage.",
        " {5}(?:Grants|Granted) you .* Defense and .* Damage.",
        " {5}Granted you .*x HP and .*x ${SkyblockStat.HEALTH_REGEN.hypixelIcon} Health Regen.",
        " {5}(?:Grants|Granted) you .* Intelligence and .* Speed.",
        " {5}Granted you .* HP, .* Defense, .* Intelligence, and .* Strength.",
        "BUFF! You have gained Healing V!"
    )

    private val puzzlePatterns by patternGroup.list(
        "puzzle",
        "PUZZLE SOLVED! .* wasn't fooled by .*! Good job!",
        "PUZZLE SOLVED! .* tied Tic Tac Toe! Good job!",
        "\\[STATUE] Oruo the Omniscient: .* thinks the answer is .*! Lock in your party's answer in my Chamber!",
        "\\[STATUE] Oruo the Omniscient: Though I sit stationary in this prison that is The Catacombs, my knowledge knows no bounds.",
        "\\[STATUE] Oruo the Omniscient: Prove your knowledge by answering 3 questions and I shall reward you in ways that transcend time!",
        "\\[STATUE] Oruo the Omniscient: Answer incorrectly, and your moment of ineptitude will live on for generations.",
        "\\[STATUE] Oruo the Omniscient: 2 questions left... Then you will have proven your worth to me!",
        "\\[STATUE] Oruo the Omniscient: One more question!",
        "\\[STATUE] Oruo the Omniscient: I bestow upon you all the power of a hundred years!",
        "\\[STATUE] Oruo the Omniscient: You've already proven enough to me! No need to press more of my buttons!",
        "\\[STATUE] Oruo the Omniscient: I've had enough of you and your party fiddling with my buttons. Scram!",
        "\\[STATUE] Oruo the Omniscient: Enough! My buttons are not to be pressed with such lack of grace!"
    )

    private val ambiencePatterns by patternGroup.list(
        "ambience",
        "A shiver runs down your spine..."
    )

    private val reminderPatterns by patternGroup.list(
        "reminder",
        "RIGHT CLICK on a WITHER door to open it. This key can only be used to open 1 door!",
        "RIGHT CLICK on the BLOOD DOOR to open it. This key can only be used to open 1 door!"
    )

    private val pickupPatterns by patternGroup.list(
        "pickup",
        ".* has obtained Superboom TNT!",
        ".* has obtained Superboom TNT x2!",
        "RARE DROP! Hunk of Blue Ice \\(.*%? Magic Find!\\)",
        ".* has obtained Revive Stone!",
        ".* found a Wither Essence! Everyone gains an extra essence!",
        ".* the Fairy: You killed me! Take this Revive Stone so that my death is not in vain!",
        ".* the Fairy: You killed me! I'll revive you so that my death is not in vain!",
        ".* the Fairy: You killed me! I'll revive your friend .* so that my death is not in vain!",
        ".* the Fairy: Have a great life!",
        ".* You picked up a .* Orb from .* healing you for .* and granting you .*% .* for 10 seconds.",
        ".* has obtained Premium Flesh!",
        "RARE DROP! Beating Heart .*",
        ".* has obtained Beating Heart!",
        "You found a Wither Essence! Everyone gains an extra essence!"
    )

    /**
     * REGEX-TEST: [Berserk] Melee Damage 48% -> 88%
     * REGEX-TEST: [Berserk] Walk Speed 38 -> 68
     */
    private val startPatterns by patternGroup.list(
        "start",
        ".* .* \\d+%? -> \\d+%?",
        "\\[NPC] Mort: Here, I found this map when I first entered the dungeon.",
        "\\[NPC] Mort: You should find it useful if you get lost.",
        "\\[NPC] Mort: Good luck.",
        "\\[NPC] Mort: Talk to me to change your class and ready up."
    )

    private val preparePatterns by patternGroup.list(
        "prepare",
        ".* has started the dungeon countdown. The dungeon will begin in 1 minute.",
        "\\[NPC] Mort: Talk to me to change your class and ready up.",
        ".* is now ready!",
        "Dungeon starts in .* seconds.",
        "Your active Potion Effects have been paused and stored. They will be restored when you leave Dungeons! You are not allowed to use existing Potion Effects while in Dungeons.",
        "Dungeon starts in 1 second.",
        "You can no longer consume or splash any potions during the remainder of this Dungeon run!"
    )

    private fun getPatterns(type: MessageTypes): List<Pattern> = when (type) {
        PREPARE -> preparePatterns
        START -> startPatterns
        AMBIENCE -> ambiencePatterns
        PICKUP -> pickupPatterns
        REMINDER -> reminderPatterns
        BUFF -> buffPatterns
        NOT_POSSIBLE -> notPossiblePatterns
        DAMAGE -> damagePatterns
        ABILITY -> abilityPatterns
        PUZZLE -> puzzlePatterns
        END -> endPatterns
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (config.dungeonFilteredMessageTypes.isEmpty()) return
        val blockReason = block(event.cleanMessage) ?: return
        event.blockedReason = "dungeon_$blockReason"
    }

    private fun block(message: String): String? {
        return MessageTypes.entries.firstOrNull { message.isFiltered(it) }?.blockReason
    }

    private fun String.isFiltered(key: MessageTypes): Boolean = config.dungeonFilteredMessageTypes.contains(key) && isPresent(key)

    private fun String.isPresent(key: MessageTypes): Boolean = getPatterns(key).anyMatches(this)
}
