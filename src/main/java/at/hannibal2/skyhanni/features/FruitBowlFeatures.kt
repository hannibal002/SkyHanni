package at.hannibal2.skyhanni.features

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SpecialColor.toSpecialColor
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sortedDesc
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sublistAfter
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object FruitBowlFeatures {

    private val config get() = SkyHanniMod.feature.misc.fruitBowl

    private val clickedPlayers = mutableSetOf<String>()
    private val highlightedPlayers = mutableSetOf<Mob>()
    private val duplicateNames = mutableMapOf<String, Int>()

    private var namesMissing = emptySet<String>()
    private var inHand = false
    private var display = emptyList<Renderable>()
    private val FRUIT_BOWL = "FRUIT_BOWL".toInternalName()

    private val chatGroup = RepoPattern.group("misc.fruit-bowl")

    /**
     * REGEX-TEST: FRUIT BOWL! [MVP+] L1nker01's profile name is Grapes!
     */
    private val chatClickedPlayerPattern by chatGroup.pattern(
        "chat-message.clicked-player",
        "FRUIT BOWL! (?<player>.*) profile name is (?<profile>[a-zA-Z]*)!",
    )

    /**
     * REGEX-TEST: You already found this profile name in your bowl!
     */
    private val chatDuplicatePattern by chatGroup.pattern(
        "chat-message.duplicate",
        "You already found this profile name in your bowl!",
    )

    /**
     * REGEX-TEST: Grapes has been added to the fruit bowl!
     */
    private val chatFoundMissingPattern by chatGroup.pattern(
        "chat-message.found-missing",
        "(?<profile>[a-zA-Z]*) has been added to the fruit bowl!",
    )

    /**
     * REGEX-TEST: §2§lFRUITALICIOUS! §r§aYou completed your fruit bowl!
     */
    private val chatFoundAllPattern by chatGroup.pattern(
        "chat-message.found-all",
        "§2§lFRUITALICIOUS! §r§aYou completed your fruit bowl!",
    )

    /**
     * REGEX-TEST: §7Names missing:
     * REGEX-TEST: §7Name missing:
     */
    private val itemMissingLineSeparatorPattern by chatGroup.pattern(
        "item-lore.missing-names-separator",
        "§7Names? missing:",
    )

    /**
     * REGEX-TEST: §7§bPapaya§7
     * REGEX-TEST: §bLime§7, §bMango§7
     * REGEX-TEST: §7§bBlueberry§7, §bCucumber§7, §bGrapes§7, §bKiwi§7, §bLime§7, §bMango§7,
     */
    private val itemMissingNameLinePattern by chatGroup.pattern(
        "item-lore.missing-name-line",
        "§b(?<name>[a-zA-Z]*)",
    )

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        highlightedPlayers.clear()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onItemInHandChange(event: ItemInHandChangeEvent) {
        if (!config.display && !config.playerHighlighter) return

        namesMissing = updateNamesMissing()
        inHand = namesMissing.isNotEmpty()
        updateAllPlayers()
    }

    private fun updateNamesMissing(): Set<String> {
        val hand = InventoryUtils.getItemInHand() ?: return emptySet()
        if (hand.getInternalNameOrNull() != FRUIT_BOWL) return emptySet()

        val lore = hand.getLore().sublistAfter({ itemMissingLineSeparatorPattern.matches(it) }, amount = 20)
        return lore.flatMap {
            buildList {
                val matcher = itemMissingNameLinePattern.matcher(it)
                while (matcher.find()) {
                    add(matcher.group("name"))
                }
            }
        }.toSet()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick(event: SkyHanniTickEvent) {
        if (!config.playerHighlighter) return
        for (mob in MobData.players) {
            if (mob !in highlightedPlayers) {
                addPlayer(mob)
            }
        }
    }

    private fun updateAllPlayers() {
        if (!inHand) return
        if (!config.playerHighlighter) return

        val wrongColor = wrongColor()
        val correctColor = correctColor()
        for (mob in MobData.players) {

            val color = if (mob.name !in clickedPlayers) correctColor else wrongColor
            mob.setColor(color)
        }

        updateDisplay()
    }

    private fun updateDisplay() {
        display = buildList {
            addString("§e§lFruit Bowl Stats")

            addString("§7Missing names: §a${namesMissing.size}")
            for (profile in namesMissing) {
                addString(" §8- §b$profile")
            }

            val totalDuplicates = duplicateNames.values.sum()
            addString("§7Duplicate names: §a${totalDuplicates.addSeparators()}")
            for ((profile, amount) in duplicateNames.sortedDesc()) {
                addString(" §8- §a${amount.addSeparators()}x §b$profile")
            }
        }
    }

    private fun addPlayer(mob: Mob) {
        highlightedPlayers.add(mob)

        val alreadyClicked = mob.name in clickedPlayers
        val color = if (!alreadyClicked) correctColor() else wrongColor()
        mob.setColor(color)
    }

    private var lastClick: PlayerWithProfile? = null

    private class PlayerWithProfile(val playerName: String, val profileName: String) {
        operator fun component1() = playerName
        operator fun component2() = profileName
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSystemMessage(event: SystemMessageEvent) {
        if (!inHand) return

        val message = event.message.removeColor()

        if (chatFoundAllPattern.matches(message) && inHand) {
            DelayedRun.runDelayed(500.milliseconds) {
                namesMissing = updateNamesMissing()
                clickedPlayers.clear()
                updateAllPlayers()
            }
        }

        chatClickedPlayerPattern.matchMatcher(message) {
            val player = group("player").cleanPlayerName()
            val profile = group("profile")
            lastClick = PlayerWithProfile(player, profile)
        }

        if (chatDuplicatePattern.matches(message)) {
            val (player, profile) = lastClick ?: return
            lastClick = null
            if (player in clickedPlayers) return

            clickedPlayers.add(player)
            duplicateNames.addOrPut(profile, 1)
            updateAllPlayers()
        }
        chatFoundMissingPattern.matchMatcher(message) {
            val (player, profile) = lastClick ?: return
            clickedPlayers.add(player)
            namesMissing = namesMissing.filter { it != profile }.toSet()
            updateAllPlayers()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onConfigLoad(event: ConfigLoadEvent) {
        with(config) {
            ConditionalUtils.onToggle(canColor, canNotColor) {
                if (playerHighlighter) {
                    updateAllPlayers()
                }
            }
        }
    }

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { SkyBlockUtils.inSkyBlock && inHand && config.display },
            onRender = {
                config.position.renderRenderables(display, posLabel = "Fruit Bowl Stats")
            },
        )
    }

    private fun wrongColor() = config.canNotColor.get().toSpecialColor()

    private fun correctColor() = config.canColor.get().toSpecialColor()

    private fun Mob.setColor(color: Color) {
        highlight(color) { config.playerHighlighter && inHand }
    }
}
