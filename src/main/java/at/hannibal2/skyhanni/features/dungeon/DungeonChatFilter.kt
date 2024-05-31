package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.chat.ChatConfig
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

private typealias MessageTypes = ChatConfig.DungeonMessageTypes

class DungeonChatFilter {

    private val config get() = SkyHanniMod.feature.chat

    /// <editor-fold desc="Patterns, Messages, and Maps">
    private val patternGroup = RepoPattern.group("dungeonchatfilter")
    private val endPatterns by patternGroup.list(
        "end",
        "(.*) §r§eunlocked §r§d(.*) Essence §r§8x(.*)§r§e!",
        " {4}§r§d(.*) Essence §r§8x(.*)",
        " Experience §r§b\\(Team Bonus\\)$",
    )
    private val abilityPatterns by patternGroup.list(
        "ability",
        "§7Your Guided Sheep hit §r§c(.*) §r§7enemy for §r§c(.*) §r§7damage.",
        "§a§lBUFF! §fYou were splashed by (.*) §fwith §r§cHealing VIII§r§f!",
        "§aYou were healed for (.*) health by (.*)§a!",
        "§aYou gained (.*) HP worth of absorption for 3s from §r(.*)§r§a!",
        "§c(.*) §r§epicked up your (.*) Orb!",
        "§cThis ability is on cooldown for (.*)s.",
        "§a§l(.*) healed you for (.*) health!",
        "§eYour bone plating reduced the damage you took by §r§c(.*)§r§e!",
        "(.*) §r§eformed a tether with you!",
        "§eYour tether with (.*) §r§ehealed you for §r§a(.*) §r§ehealth.",
        "§7Your Implosion hit §r§c(.*) §r§7enemy for §r§c(.*) §r§7damage.",
        "§eYour §r§6Spirit Pet §r§ehealed (.*) §r§efor §r§a(.*) §r§ehealth!",
        "§eYour §r§6Spirit Pet §r§ehit (.*) enemy for §r§c(.*) §r§edamage.",
        "§cYou need at least (.*) mana to activate this!",
        "§eYou were healed for §r§a(.*)§r§e health by §r(.*)§r§e's §r§9Healing Bow§r§e and gained §r§c\\+(.*) Strength§r§e for 10 seconds.",
        "(.*)§r§a granted you §r§c(.*) §r§astrength for §r§e20 §r§aseconds!",
        "§eYour fairy healed §r§ayourself §r§efor §r§a(.*) §r§ehealth!",
        "§eYour fairy healed §r(.*) §r§efor §r§a(.*) §r§ehealth!",
        "(.*) fairy healed you for §r§a(.*) §r§ehealth!",
        "§a§r§6Guided Sheep §r§ais now available!",
        "§dCreeper Veil §r§aActivated!",
        "§dCreeper Veil §r§cDe-activated!",
        "§6Rapid Fire§r§a is ready to use! Press §r§6§lDROP§r§a to activate it!",
        "§6Castle of Stone§r§a is ready to use! Press §r§6§lDROP§r§a to activate it!",
        "§6Ragnarok§r§a is ready to use! Press §r§6§lDROP§r§a to activate it!",
    )
    private val damagePatterns by patternGroup.list(
        "damage",
        "(.*) §r§aused §r(.*) §r§aon you!",
        "§cThe (.*)§r§c struck you for (.*) damage!",
        "§cThe (.*) hit you for (.*) damage!",
        "§7(.*) struck you for §r§c(.*)§r§7 damage.",
        "(.*) hit you for §r§c(.*)§r§7 damage.",
        "(.*) hit you for §r§c(.*)§r§7 true damage.",
        "§7(.*) exploded, hitting you for §r§c(.*)§r§7 damage.",
        "(.*)§r§c hit you with §r(.*) §r§cfor (.*) damage!",
        "(.*)§r§a struck you for §r§c(.*)§r§a damage!",
        "(.*)§r§c struck you for (.*)!",
        "§7The Mage's Magma burnt you for §r§c(.*)§r§7 true damage.",
        "§7Your (.*) hit §r§c(.*) §r§7(enemy|enemies) for §r§c(.*) §r§7damage.",
        "§cMute silenced you!",
    )
    private val notPossibleMessagesPatterns by patternGroup.list(
        "notpossiblemessages",
        "§cYou cannot hit the silverfish while it's moving!",
        "§cYou cannot move the silverfish in that direction!",
        "§cThere are blocks in the way!",
        "§cThis chest has already been searched!",
        "§cThis lever has already been used.",
        "§cYou cannot do that in this room!",
        "§cYou do not have the key for this door!",
        "§cYou have already opened this dungeon chest!",
        "§cYou cannot use abilities in this room!",
        "§cA mystical force in this room prevents you from using that ability!"
    )
    private val buffPatterns by patternGroup.list(
        "buff",
        "§6§lDUNGEON BUFF! (.*) §r§ffound a §r§dBlessing of (.*)§r§f!(.*)",
        "§6§lDUNGEON BUFF! §r§fYou found a §r§dBlessing of (.*)§r§f!(.*)",
        "§6§lDUNGEON BUFF! §r§fA §r§dBlessing of (.*)§r§f was found! (.*)",
        "§eA §r§a§r§dBlessing of (.*)§r§e was picked up!",
        "(.*) §r§ehas obtained §r§a§r§dBlessing of (.*)§r§e!",
        " {5}§r§7Granted you §r§a§r§a(.*)§r§7 & §r§a(.*)x §r§c❁ Strength§r§7.",
        " {5}§r§7Also granted you §r§a§r§a(.*)§r§7 & §r§a(.*)x §r§9☠ Crit Damage§r§7.",
        " {5}§r§7(Grants|Granted) you §r§a(.*) Defense §r§7and §r§a+(.*) Damage§r§7.",
        " {5}§r§7Granted you §r§a§r§a(.*)x HP §r§7and §r§a§r§a(.*)x §r§c❣ Health Regen§r§7.",
        " {5}§r§7(Grants|Granted) you §r§a(.*) Intelligence §r§7and §r§a+(.*)? Speed§r§7.",
        " {5}§r§7Granted you §r§a+(.*) HP§r§7, §r§a(.*) Defense§r§7, §r§a(.*) Intelligence§r§7, and §r§a(.*) Strength§r§7.",
        "§a§lBUFF! §fYou have gained §r§cHealing V§r§f!",
    )
    private val puzzlePatterns by patternGroup.list(
        "puzzle",
        "§a§lPUZZLE SOLVED! (.*) §r§ewasn't fooled by §r§c(.*)§r§e! §r§4G§r§co§r§6o§r§ed§r§a §r§2j§r§bo§r§3b§r§5!",
        "§a§lPUZZLE SOLVED! (.*) §r§etied Tic Tac Toe! §r§4G§r§co§r§6o§r§ed§r§a §r§2j§r§bo§r§3b§r§5!",
        "§4\\[STATUE] Oruo the Omniscient§r§f: §r(.*) §r§fthinks the answer is §r§6 . §r(.*)§r§f! §r§fLock in your party's answer in my Chamber!",
        "§4\\[STATUE] Oruo the Omniscient§r§f: §r§fThough I sit stationary in this prison that is §r§cThe Catacombs§r§f, my knowledge knows no bounds.",
        "§4\\[STATUE] Oruo the Omniscient§r§f: §r§fProve your knowledge by answering 3 questions and I shall reward you in ways that transcend time!",
        "§4\\[STATUE] Oruo the Omniscient§r§f: §r§fAnswer incorrectly, and your moment of ineptitude will live on for generations.",
        "§4\\[STATUE] Oruo the Omniscient§r§f: §r§f2 questions left... Then you will have proven your worth to me!",
        "§4\\[STATUE] Oruo the Omniscient§r§f: §r§fOne more question!",
        "§4\\[STATUE] Oruo the Omniscient§r§f: §r§fI bestow upon you all the power of a hundred years!",
        "§4\\[STATUE] Oruo the Omniscient§r§f: §r§fYou've already proven enough to me! No need to press more of my buttons!",
        "§4\\[STATUE] Oruo the Omniscient§r§f: §r§fI've had enough of you and your party fiddling with my buttons. Scram!",
        "§4\\[STATUE] Oruo the Omniscient§r§f: §r§fEnough! My buttons are not to be pressed with such lack of grace!",
    )
    private val ambienceMessagesPatterns by patternGroup.list(
        "ambiencemessages",
        "§5A shiver runs down your spine..."
    )
    private val reminderMessagesPatterns by patternGroup.list(
        "remindermessages",
        "§e§lRIGHT CLICK §r§7on §r§7a §r§8WITHER §r§7door§r§7 to open it. This key can only be used to open §r§a1§r§7 door!",
        "§e§lRIGHT CLICK §r§7on §r§7the §r§cBLOOD DOOR§r§7 to open it. This key can only be used to open §r§a1§r§7 door!"
    )
    private val pickupPatterns by patternGroup.list(
        "pickup",
        "(.*) §r§ehas obtained §r§a§r§9Superboom TNT§r§e!",
        "(.*) §r§ehas obtained §r§a§r§9Superboom TNT §r§8x2§r§e!",
        "§6§lRARE DROP! §r§9Hunk of Blue Ice §r§b\\(+(.*)% Magic Find!\\)",
        "(.*) §r§ehas obtained §r§a§r§6Revive Stone§r§e!",
        "(.*) §r§ffound a §r§dWither Essence§r§f! Everyone gains an extra essence!",
        "§d(.*) the Fairy§r§f: You killed me! Take this §r§6Revive Stone §r§fso that my death is not in vain!",
        "§d(.*) the Fairy§r§f: You killed me! I'll revive you so that my death is not in vain!",
        "§d(.*) the Fairy§r§f: You killed me! I'll revive your friend §r(.*) §r§fso that my death is not in vain!",
        "§d(.*) the Fairy§r§f: Have a great life!",
        "§c(.*) §r§eYou picked up a Ability Damage Orb from (.*) §r§ehealing you for §r§c(.*) §r§eand granting you +§r§c(.*)% §r§eAbility Damage for §r§b10 §r§eseconds.",
        "§c(.*) §r§eYou picked up a Damage Orb from (.*) §r§ehealing you for §r§c(.*) §r§eand granting you +§r§c(.*)% §r§eDamage for §r§b10 §r§eseconds.",
        "(.*) §r§ehas obtained §r§a§r§9Premium Flesh§r§e!",
        "§6§lRARE DROP! §r§9Beating Heart §r§b(.*)",
        "(.*) §r§ehas obtained §r§a§r§9Beating Heart§r§e!",
        "§fYou found a §r§dWither Essence§r§f! Everyone gains an extra essence!,"
    )
    private val startPatterns by patternGroup.list(
        "start",
        //§a[Berserk] §r§fMelee Damage §r§c48%§r§f -> §r§a88%
        //§a[Berserk] §r§fWalk Speed §r§c38§r§f -> §r§a68
        "§a(.*) §r§f(.*) §r§c(.*)§r§f -> §r§a(.*)",
        "§e\\[NPC] §bMort§f: §rHere, I found this map when I first entered the dungeon.",
        "§e\\[NPC] §bMort§f: §rYou should find it useful if you get lost.",
        "§e\\[NPC] §bMort§f: §rGood luck.",
        "§e\\[NPC] §bMort§f: §rTalk to me to change your class and ready up.",
    )
    private val preparePatterns by patternGroup.list(
        "prepare",
        "(.*) has started the dungeon countdown. The dungeon will begin in 1 minute.",
        "§e\\[NPC] §bMort§f: §rTalk to me to change your class and ready up.",
        "(.*)§a is now ready!",
        "§aDungeon starts in (.*) seconds.",
        "§aYour active Potion Effects have been paused and stored. They will be restored when you leave Dungeons! You are not allowed to use existing Potion Effects while in Dungeons.",
        "§aDungeon starts in 1 second.",
        "§aYou can no longer consume or splash any potions during the remainder of this Dungeon run!",
    )
    private val activePotionEffectsPattern by patternGroup.pattern(
        "activepotioneffects",
        "§aYour active Potion Effects"
    )

    private val patternsMap: Map<MessageTypes, List<Pattern>> = mapOf(
        MessageTypes.PREPARE to preparePatterns,
        MessageTypes.START to startPatterns,
        MessageTypes.PICKUP to pickupPatterns,
        MessageTypes.BUFF to buffPatterns,
        MessageTypes.DAMAGE to damagePatterns,
        MessageTypes.ABILITY to abilityPatterns,
        MessageTypes.PUZZLE to puzzlePatterns,
        MessageTypes.END to endPatterns,
        MessageTypes.AMBIENCE to ambienceMessagesPatterns,
        MessageTypes.REMINDER to reminderMessagesPatterns,
        MessageTypes.NOT_POSSIBLE to notPossibleMessagesPatterns,

        )
    /// </editor-fold>

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.onHypixel || config.dungeonFilteredMessageTypes.isEmpty()) return

        // Workaround since the potion message gets always sent at that moment when SkyBlock is set as false
        if (!LorenzUtils.inSkyBlock && !activePotionEffectsPattern.matches(event.message)) return

        val blockReason = block(event.message)
        if (blockReason != "") {
            event.blockedReason = "dungeon_$blockReason"
        }
    }

    private fun block(message: String): String {
        when {
            message.isFiltered(MessageTypes.PREPARE) -> return "prepare"
            message.isFiltered(MessageTypes.START) -> return "start"
        }

        if (!DungeonAPI.inDungeon()) return ""

        return when {
            message.isFiltered(MessageTypes.AMBIENCE) -> "ambience"
            message.isFiltered(MessageTypes.PICKUP) -> "pickup"
            message.isFiltered(MessageTypes.REMINDER) -> "reminder"
            message.isFiltered(MessageTypes.BUFF) -> "buff"
            message.isFiltered(MessageTypes.NOT_POSSIBLE) -> "not_possible"
            message.isFiltered(MessageTypes.DAMAGE) -> "damage"
            message.isFiltered(MessageTypes.ABILITY) -> "ability"
            message.isFiltered(MessageTypes.PUZZLE) -> "puzzle"
            message.isFiltered(MessageTypes.END) -> "end"
            else -> ""
        }
    }

    private fun String.isFiltered(key: MessageTypes): Boolean {
        return config.dungeonFilteredMessageTypes.contains(key) && this.isPresent(key)
    }

    /**
     * Checks if the message is present in the list of patterns
     * @receiver The message to check
     * @param key The key of the list to check
     * @return True if the message is present in any of the maps
     * @see patternsMap
     */
    private fun String.isPresent(key: MessageTypes): Boolean {
        return (patternsMap[key] ?: emptyList()).any { it.matches(this) }
    }
}
