package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonChatFilter {

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.onHypixel) return

        // Workaround since the potion message gets always sent in that moment when SkyBlock is set as false
        if (!LorenzUtils.inSkyBlock && !event.message.startsWith("§aYour active Potion Effects")) return
        if (!SkyHanniMod.feature.chat.dungeonMessages) return

        val blockReason = block(event.message)
        if (blockReason != "") {
            event.blockedReason = "dungeon_$blockReason"
        }
    }

    private fun block(message: String): String {
        when {
            isPrepare(message) -> return "prepare"
            isStart(message) -> return "start"
        }

        if (!LorenzUtils.inDungeons) return ""

        return when {
            isUnsortedBlockedMessage(message) -> "unsorted"
            isPickup(message) -> "pickup"
            isReminder(message) -> "reminder"
            isBuff(message) -> "buff"
            isNotPossible(message) -> "not_possible"
            isDamage(message) -> "damage"
            isAbility(message) -> "ability"
            isPuzzle(message) -> "puzzle"
            isEnd(message) -> "end"

            else -> ""
        }
    }

    private fun isEnd(message: String): Boolean = when {
        message.matchRegex("(.*) §r§eunlocked §r§d(.*) Essence §r§8x(.*)§r§e!") -> true
        message.matchRegex("    §r§d(.*) Essence §r§8x(.*)") -> true
        message.endsWith(" Experience §r§b(Team Bonus)") -> true
        else -> false
    }

    private fun isAbility(message: String): Boolean = when {
        message == "§a§r§6Guided Sheep §r§ais now available!" -> true
        message.matchRegex("§7Your Guided Sheep hit §r§c(.*) §r§7enemy for §r§c(.*) §r§7damage.") -> true
        message == "§6Rapid Fire§r§a is ready to use! Press §r§6§lDROP§r§a to activate it!" -> true
        message == "§6Castle of Stone§r§a is ready to use! Press §r§6§lDROP§r§a to activate it!" -> true
        message == "§6Ragnarok§r§a is ready to use! Press §r§6§lDROP§r§a to activate it!" -> true


        message.matchRegex("§a§lBUFF! §fYou were splashed by (.*) §fwith §r§cHealing VIII§r§f!") -> true
        message.matchRegex("§aYou were healed for (.*) health by (.*)§a!") -> true
        message.matchRegex("§aYou gained (.*) HP worth of absorption for 3s from §r(.*)§r§a!") -> true
        message.matchRegex("§c(.*) §r§epicked up your (.*) Orb!") -> true
        message.matchRegex("§cThis ability is on cooldown for (.*)s.") -> true
        message.matchRegex("§a§l(.*) healed you for (.*) health!") -> true
        message.matchRegex("§eYour bone plating reduced the damage you took by §r§c(.*)§r§e!") -> true
        message.matchRegex("(.*) §r§eformed a tether with you!") -> true
        message.matchRegex("§eYour tether with (.*) §r§ehealed you for §r§a(.*) §r§ehealth.") -> true
        message.matchRegex("§7Your Implosion hit §r§c(.*) §r§7enemy for §r§c(.*) §r§7damage.") -> true

        message.matchRegex("§eYour §r§6Spirit Pet §r§ehealed (.*) §r§efor §r§a(.*) §r§ehealth!") -> true
        message.matchRegex("§eYour §r§6Spirit Pet §r§ehit (.*) enemy for §r§c(.*) §r§edamage.") -> true

        message == "§dCreeper Veil §r§aActivated!" -> true
        message == "§dCreeper Veil §r§cDe-activated!" -> true
        message.matchRegex("§cYou need at least (.*) mana to activate this!") -> true

        message.matchRegex(
            "§eYou were healed for §r§a(.*)§r§e health by §r(.*)§r§e's §r§9Healing Bow§r§e and " + "gained §r§c\\+(.*) Strength§r§e for 10 seconds."
        ) -> true

        message.matchRegex("(.*)§r§a granted you §r§c(.*) §r§astrength for §r§e20 §r§aseconds!") -> true

        message.matchRegex("§eYour fairy healed §r§ayourself §r§efor §r§a(.*) §r§ehealth!") -> true
        message.matchRegex("§eYour fairy healed §r(.*) §r§efor §r§a(.*) §r§ehealth!") -> true
        message.matchRegex("(.*) fairy healed you for §r§a(.*) §r§ehealth!") -> true

        else -> false
    }

    private fun isDamage(message: String): Boolean = when {
        message == "§cMute silenced you!" -> true
        message.matchRegex("(.*) §r§aused §r(.*) §r§aon you!") -> true
        message.matchRegex("§cThe (.*)§r§c struck you for (.*) damage!") -> true
        message.matchRegex("§cThe (.*) hit you for (.*) damage!") -> true
        message.matchRegex("§7(.*) struck you for §r§c(.*)§r§7 damage.") -> true
        message.matchRegex("(.*) hit you for §r§c(.*)§r§7 damage.") -> true
        message.matchRegex("(.*) hit you for §r§c(.*)§r§7 true damage.") -> true
        message.matchRegex("§7(.*) exploded, hitting you for §r§c(.*)§r§7 damage.") -> true
        message.matchRegex("(.*)§r§c hit you with §r(.*) §r§cfor (.*) damage!") -> true
        message.matchRegex("(.*)§r§a struck you for §r§c(.*)§r§a damage!") -> true
        message.matchRegex("(.*)§r§c struck you for (.*)!") -> true

        message.matchRegex("§7The Mage's Magma burnt you for §r§c(.*)§r§7 true damage.") -> true

        message.matchRegex("§7Your (.*) hit §r§c(.*) §r§7(enemy|enemies) for §r§c(.*) §r§7damage.") -> true
        else -> false
    }

    private fun isNotPossible(message: String): Boolean = when (message) {
        "§cYou cannot hit the silverfish while it's moving!",
        "§cYou cannot move the silverfish in that direction!",
        "§cThere are blocks in the way!",
        "§cThis chest has already been searched!",
        "§cThis lever has already been used.",
        "§cYou cannot do that in this room!",
        "§cYou do not have the key for this door!",
        "§cYou have already opened this dungeon chest!",
        "§cYou cannot use abilities in this room!",
        "§cA mystical force in this room prevents you from using that ability!" -> true

        else -> false
    }

    private fun isBuff(message: String): Boolean = when {
        message.matchRegex("§6§lDUNGEON BUFF! (.*) §r§ffound a §r§dBlessing of (.*)§r§f!(.*)") -> true
        message.matchRegex("§6§lDUNGEON BUFF! §r§fYou found a §r§dBlessing of (.*)§r§f!(.*)") -> true
        message.matchRegex("§6§lDUNGEON BUFF! §r§fA §r§dBlessing of (.*)§r§f was found! (.*)") -> true
        message.matchRegex("§eA §r§a§r§dBlessing of (.*)§r§e was picked up!") -> true
        message.matchRegex("(.*) §r§ehas obtained §r§a§r§dBlessing of (.*)§r§e!") -> true
        message.matchRegex("     §r§7Granted you §r§a§r§a(.*)§r§7 & §r§a(.*)x §r§c❁ Strength§r§7.") -> true
        message.matchRegex("     §r§7Also granted you §r§a§r§a(.*)§r§7 & §r§a(.*)x §r§9☠ Crit Damage§r§7.") -> true
        message.matchRegex("     §r§7(Grants|Granted) you §r§a(.*) Defense §r§7and §r§a+(.*) Damage§r§7.") -> true
        message.matchRegex("     §r§7Granted you §r§a§r§a(.*)x HP §r§7and §r§a§r§a(.*)x §r§c❣ Health Regen§r§7.") -> true
        message.matchRegex("     §r§7(Grants|Granted) you §r§a(.*) Intelligence §r§7and §r§a+(.*)? Speed§r§7.") -> true
        message.matchRegex("     §r§7Granted you §r§a+(.*) HP§r§7, §r§a(.*) Defense§r§7, §r§a(.*) Intelligence§r§7, and §r§a(.*) Strength§r§7.") -> true
        message == "§a§lBUFF! §fYou have gained §r§cHealing V§r§f!" -> true
        else -> false
    }

    private fun isPuzzle(message: String): Boolean = when {
        message.matchRegex("§a§lPUZZLE SOLVED! (.*) §r§ewasn't fooled by §r§c(.*)§r§e! §r§4G§r§co§r§6o§r§ed§r§a §r§2j§r§bo§r§3b§r§5!") -> true
        message.matchRegex("§a§lPUZZLE SOLVED! (.*) §r§etied Tic Tac Toe! §r§4G§r§co§r§6o§r§ed§r§a §r§2j§r§bo§r§3b§r§5!") -> true
        message == "§4[STATUE] Oruo the Omniscient§r§f: §r§fThough I sit stationary in this prison that is §r§cThe Catacombs§r§f, my knowledge knows no bounds." -> true
        message == "§4[STATUE] Oruo the Omniscient§r§f: §r§fProve your knowledge by answering 3 questions and I shall reward you in ways that transcend time!" -> true
        message == "§4[STATUE] Oruo the Omniscient§r§f: §r§fAnswer incorrectly, and your moment of ineptitude will live on for generations." -> true

        message == "§4[STATUE] Oruo the Omniscient§r§f: §r§f2 questions left... Then you will have proven your worth to me!" -> true

        message == "§4[STATUE] Oruo the Omniscient§r§f: §r§fOne more question!" -> true
        message == "§4[STATUE] Oruo the Omniscient§r§f: §r§fI bestow upon you all the power of a hundred years!" -> true
        message == "§4[STATUE] Oruo the Omniscient§r§f: §r§fYou've already proven enough to me! No need to press more of my buttons!" -> true
        message == "§4[STATUE] Oruo the Omniscient§r§f: §r§fI've had enough of you and your party fiddling with my buttons. Scram!" -> true
        message == "§4[STATUE] Oruo the Omniscient§r§f: §r§fEnough! My buttons are not to be pressed with such lack of grace!" -> true
        message.matchRegex("§4\\[STATUE] Oruo the Omniscient§r§f: §r(.*) §r§fthinks the answer is §r§6 . §r(.*)§r§f! §r§fLock in your party's answer in my Chamber!") -> true
        else -> false
    }

    //TODO sort out and filter separately
    private fun isUnsortedBlockedMessage(message: String): Boolean = when {
        message.matchRegex("(.*) §r§ehas obtained §r§a§r§9Beating Heart§r§e!") -> true
        message == "§5A shiver runs down your spine..." -> true

        else -> false
    }

    private fun isReminder(message: String): Boolean = when (message) {
        "§e§lRIGHT CLICK §r§7on §r§7a §r§8WITHER §r§7door§r§7 to open it. This key can only be used to open §r§a1§r§7 door!",
        "§e§lRIGHT CLICK §r§7on §r§7the §r§cBLOOD DOOR§r§7 to open it. This key can only be used to open §r§a1§r§7 door!" -> true

        else -> false
    }

    private fun isPickup(message: String): Boolean = when {
        message.matchRegex("(.*) §r§ehas obtained §r§a§r§9Superboom TNT§r§e!") -> true
        message.matchRegex("(.*) §r§ehas obtained §r§a§r§9Superboom TNT §r§8x2§r§e!") -> true
        message.matchRegex("§6§lRARE DROP! §r§9Hunk of Blue Ice §r§b\\(+(.*)% Magic Find!\\)") -> true
        message.matchRegex("(.*) §r§ehas obtained §r§a§r§6Revive Stone§r§e!") -> true
        message.matchRegex("(.*) §r§ffound a §r§dWither Essence§r§f! Everyone gains an extra essence!") -> true
        message == "§fYou found a §r§dWither Essence§r§f! Everyone gains an extra essence!" -> true
        message.matchRegex("§d(.*) the Fairy§r§f: You killed me! Take this §r§6Revive Stone §r§fso that my death is not in vain!") -> true
        message.matchRegex("§d(.*) the Fairy§r§f: You killed me! I'll revive you so that my death is not in vain!") -> true
        message.matchRegex("§d(.*) the Fairy§r§f: You killed me! I'll revive your friend §r(.*) §r§fso that my death is not in vain!") -> true
        message.matchRegex("§d(.*) the Fairy§r§f: Have a great life!") -> true
        message.matchRegex(
            "§c(.*) §r§eYou picked up a Ability Damage Orb from (.*) §r§ehealing you for §r§c(.*) §r§eand granting you +§r§c(.*)% §r§eAbility Damage for §r§b10 §r§eseconds."
        ) -> true

        message.matchRegex(
            "§c(.*) §r§eYou picked up a Damage Orb from (.*) §r§ehealing you for §r§c(.*) §r§eand granting you +§r§c(.*)% §r§eDamage for §r§b10 §r§eseconds."
        ) -> true

        message.matchRegex("(.*) §r§ehas obtained §r§a§r§9Premium Flesh§r§e!") -> true
        message.matchRegex("§6§lRARE DROP! §r§9Beating Heart §r§b(.*)") -> true
        else -> false
    }

    private fun isStart(message: String): Boolean = when {
        message == "§e[NPC] §bMort§f: §rHere, I found this map when I first entered the dungeon." -> true
        message == "§e[NPC] §bMort§f: §rYou should find it useful if you get lost." -> true
        message == "§e[NPC] §bMort§f: §rGood luck." -> true
        message == "§e[NPC] §bMort§f: §rTalk to me to change your class and ready up." -> true

        //§a[Berserk] §r§fMelee Damage §r§c48%§r§f -> §r§a88%
        //§a[Berserk] §r§fWalk Speed §r§c38§r§f -> §r§a68
        message.matchRegex("§a(.*) §r§f(.*) §r§c(.*)§r§f -> §r§a(.*)") -> true
        else -> false
    }

    private fun isPrepare(message: String): Boolean = when {
        message == "§aYour active Potion Effects have been paused and stored. They will be restored when you leave Dungeons! You are not allowed to use existing Potion Effects while in Dungeons." -> true
        message.matchRegex("(.*) has started the dungeon countdown. The dungeon will begin in 1 minute.") -> true
        message.matchRegex("§e[NPC] §bMort§f: §rTalk to me to change your class and ready up.") -> true
        message.matchRegex("(.*)§a is now ready!") -> true
        message.matchRegex("§aDungeon starts in (.*) seconds.") -> true
        message == "§aDungeon starts in 1 second." -> true
        message == "§aYou can no longer consume or splash any potions during the remainder of this Dungeon run!" -> true
        else -> false
    }
}