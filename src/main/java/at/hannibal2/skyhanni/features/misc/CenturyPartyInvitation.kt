package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.hanni.data.mob.Mob
import at.hannibal2.hanni.data.mob.MobData
import at.hannibal2.hanni.events.ConfigLoadEvent
import at.hannibal2.hanni.events.ItemInHandChangeEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.test.command.ErrorManager
import at.hannibal2.hanni.utils.ConditionalUtils
import at.hannibal2.hanni.utils.DelayedRun
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.LorenzColor.Companion.toLorenzColor
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.collection.CollectionUtils.sublistAfter
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.item.ItemStack
import kotlin.time.Duration.Companion.milliseconds

@HanniModule
object CenturyPartyInvitation {

    private val config get() = HanniMod.feature.misc.centuryPartyInvitation

    private val playerColors = mutableMapOf<Mob, LorenzColor>()

    private var colorsNeeded = emptySet<LorenzColor>()
    private var inHand = false

    private val chatGroup = RepoPattern.group("misc.century-party-invitation")

    /**
     * REGEX-TEST: §r§8[§a136§8] §7_seagullz_ §e⛃§r
     */
    private val playerRankColorPattern by chatGroup.pattern(
        "nametag.player-color",
        ".*\\[§(?<color>.).*\\] .*",
    )

    /**
     * REGEX-TEST: §d§lPARTY! §r§7Iskc__§r§7§r§e's SkyBlock level color is §r§f§r§8[§r§f40§r§8] - [§r§f79§r§8] §r§fWhite§r§e!
     */
    private val chatPartyAddPattern by chatGroup.pattern(
        "chat-message.party-add",
        "§d§lPARTY! .* SkyBlock level color is .*\\[.*\\] - \\[.*\\] §r§(?<color>.).*§e!",
    )

    /**
     * REGEX-TEST: §aYou had already gained the bonus, so... at least everyone is now invited!
     */
    private val chatFoundAllPattern by chatGroup.pattern(
        "chat-message.found-all",
        "§aYou had already gained the bonus, so\\.\\.\\. at least everyone is now invited!",
    )

    /**
     * REGEX-TEST: §7Level colors missing:
     * REGEX-TEST: §7Level color missing:
     */
    private val itemMissingLineSeparatorPattern by chatGroup.pattern(
        "item-lore.missing-color-separator",
        "§7Level colors? missing:",
    )

    /**
     * REGEX-TEST: §8[§e80§8] - [§e119§8] §eYellow
     * REGEX-TEST: §8[§c440§8] - [§c479§8] §cRed
     */
    private val itemMissingColorLinePattern by chatGroup.pattern(
        "item-lore.missing-color-line",
        "§8\\[.*\\] - \\[.*\\] §(?<color>.).*",
    )

    @HandleEvent
    fun onWorldChange() {
        playerColors.clear()
    }

    @HandleEvent
    fun onItemInHandChange(event: ItemInHandChangeEvent) {
        if (!isEnabled()) return

        colorsNeeded = updateColorsNeeded()
        inHand = colorsNeeded.isNotEmpty()
        updateAllPlayers()
    }

    private fun updateColorsNeeded(): Set<LorenzColor> {
        val hand = InventoryUtils.getItemInHand() ?: return emptySet()
        if (hand.getInternalNameOrNull() != "CENTURY_PARTY_INVITATION".toInternalName()) return emptySet()

        val set = mutableSetOf<LorenzColor>()
        for (line in hand.getLore().sublistAfter({ itemMissingLineSeparatorPattern.matches(it) })) {
            readLine(line, hand)?.let {
                set.add(it)
            }
        }

        return set
    }

    private fun readLine(line: String, hand: ItemStack): LorenzColor? {
        val colorCode = itemMissingColorLinePattern.matchMatcher(line) {
            group("color")
        } ?: return null

        return colorCode.toCharArray().first().toLorenzColor() ?: run {
            ErrorManager.logErrorStateWithData(
                "Error reading Cenutry Party Invitation colors missing",
                "unknown color code detected",
                "colorCode" to colorCode,
                "line" to line,
                "lore" to hand.getLore(),
            )
            return null
        }
    }

    @HandleEvent
    fun onTick() {
        if (!isEnabled()) return
        for (mob in MobData.players) {
            if (mob !in playerColors) {
                addPlayer(mob)
            }
        }
    }

    private fun updateAllPlayers() {
        if (!inHand) return
        val correctPlayers = playerColors.filter { it.value in colorsNeeded }.keys

        val wrongColor = wrongColor()
        val correctColor = correctColor()
        for (mob in MobData.players) {
            val color = if (mob in correctPlayers) correctColor else wrongColor
            mob.setColor(color)
        }
    }

    private fun addPlayer(mob: Mob) {
        val displayName = mob.baseEntity.name
        val colorCode = playerRankColorPattern.matchMatcher(displayName) {
            group("color")
        } ?: run {
            return
        }

        val playerColor = colorCode.toCharArray().first().toLorenzColor() ?: run {
            ErrorManager.logErrorStateWithData(
                "Error reading Player rank color",
                "unknown color code detected",
                "colorCode" to colorCode,
                "displayName" to displayName,
                "name" to mob.name,
            )
            return
        }
        playerColors[mob] = playerColor

        val color = if (playerColor in colorsNeeded) correctColor() else wrongColor()
        mob.setColor(color)
    }

    @HandleEvent
    fun onSystemMessage(event: SystemMessageEvent) {
        if (!isEnabled()) return

        val message = event.message

        if (chatFoundAllPattern.matches(message) && inHand) {
            DelayedRun.runDelayed(500.milliseconds) {
                colorsNeeded = updateColorsNeeded()
                updateAllPlayers()
            }
        }

        val colorCode = chatPartyAddPattern.matchMatcher(message) {
            group("color")
        } ?: return

        val foundColor = colorCode.toCharArray().first().toLorenzColor() ?: run {
            ErrorManager.logErrorStateWithData(
                "Error reading rank color from chat",
                "unknown color code detected",
                "colorCode" to colorCode,
                "message" to message,
            )
            return
        }

        colorsNeeded = colorsNeeded.filter { it != foundColor }.toSet()
        updateAllPlayers()
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        with(config) {
            ConditionalUtils.onToggle(canColor, canNotColor) {
                if (isEnabled()) {
                    updateAllPlayers()
                }
            }
        }
    }

    private fun wrongColor() = config.canNotColor.get()

    private fun correctColor() = config.canColor.get()

    private fun Mob.setColor(color: ChromaColour) {
        highlight(color) { config.playerHighlighter && inHand }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.playerHighlighter
}
