package at.hannibal2.skyhanni.features.bingo.card

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.bingo.BingoCardUpdateEvent
import at.hannibal2.skyhanni.features.bingo.BingoAPI
import at.hannibal2.skyhanni.features.bingo.card.goals.BingoGoal
import at.hannibal2.skyhanni.features.bingo.card.nextstephelper.BingoNextStepHelper
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.onToggle
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

class BingoCardDisplay {

    private var display = emptyList<Renderable>()

    private var hasHiddenPersonalGoals = false

    init {
        update()
    }

    companion object {
        private const val MAX_PERSONAL_GOALS = 20
        private const val MAX_COMMUNITY_GOALS = 5

        private val config get() = SkyHanniMod.feature.event.bingo.bingoCard
        private var displayMode = 0

        fun command() {
            reload()
        }

        private fun reload() {
            BingoAPI.bingoGoals.clear()
        }

        fun toggleCommand() {
            if (!LorenzUtils.isBingoProfile) {
                LorenzUtils.userError("This command only works on a bingo profile!")
                return
            }
            if (!config.enabled) {
                LorenzUtils.userError("Bingo Card is disabled in the config!")
                return
            }
            toggleMode()
        }

        private fun toggleMode() {
            displayMode++
            if (displayMode == 3) {
                displayMode = 0
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (event.repeatSeconds(1)) {
            if (hasHiddenPersonalGoals) {
                update()
            }
        }
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay(): MutableList<Renderable> {
        val newList = mutableListOf<Renderable>()

        if (BingoAPI.bingoGoals.isEmpty()) {
            newList.add(Renderable.string("§6Bingo Goals:"))
            newList.add(Renderable.clickAndHover("§cOpen the §e/bingo §ccard.",
                listOf("Click to run §e/bingo"),
                onClick = {
                    LorenzUtils.sendCommandToServer("bingo")
                }
            ))
        } else {
            if (!config.hideCommunityGoals.get()) {
                newList.addCommunityGoals()
            }
            newList.addPersonalGoals()
        }
        return newList
    }

    private var lastClick = SimpleTimeMark.farPast()

    private fun MutableList<Renderable>.addCommunityGoals() {
        add(Renderable.string("§6Community Goals:"))
        val goals = BingoAPI.communityGoals.toMutableList()
        var hiddenGoals = 0
        for (goal in goals.toList()) {
            if (goal.hiddenGoalData.unknownTip) {
                hiddenGoals++
                goals.remove(goal)
            }
        }

        addGoals(goals) { it.description.removeColor() + if (it.done) " §aDONE" else " " }

        if (hiddenGoals > 0) {
            val name = StringUtils.canBePlural(hiddenGoals, "goal", "goals")
            add(Renderable.string("§7+ $hiddenGoals more §cunknown §7community $name."))
        }
        add(Renderable.string(" "))
    }

    private fun MutableList<Renderable>.addPersonalGoals() {
        val todo = BingoAPI.personalGoals.filter { !it.done }.toMutableList()
        val done = MAX_PERSONAL_GOALS - todo.size
        add(Renderable.string("§6Personal Goals: ($done/$MAX_PERSONAL_GOALS done)"))

        var hiddenGoals = 0
        var nextTip = 14.days
        for (goal in todo.toList()) {
            val hiddenGoalData = goal.hiddenGoalData
            if (hiddenGoalData.unknownTip) {
                hiddenGoals++
                todo.remove(goal)
                hiddenGoalData.nextHintTime?.let {
                    if (it < nextTip) {
                        nextTip = it
                    }
                }
            }
        }

        addGoals(todo) { it.description.removeColor() }

        if (hiddenGoals > 0) {
            val name = StringUtils.canBePlural(hiddenGoals, "goal", "goals")
            add(Renderable.string("§7+ $hiddenGoals more §cunknown §7$name."))
        }
        hasHiddenPersonalGoals = config.nextTipDuration.get() && nextTip != 14.days
        if (hasHiddenPersonalGoals) {
            val nextTipTime = BingoAPI.lastBingoCardOpenTime + nextTip
            if (nextTipTime.isInPast()) {
                add(Renderable.string("§eThe next hint got unlocked already!"))
                add(Renderable.string("§eOpen the bingo card to update!"))
            } else {
                val until = nextTipTime.timeUntil()
                add(Renderable.string("§eThe next hint will unlock in §b${until.format(maxUnits = 2)}"))
            }
        }
    }

    private fun MutableList<Renderable>.addGoals(goals: MutableList<BingoGoal>, format: (BingoGoal) -> String) {
        val editDisplay = canEditDisplay()
        val showOnlyHighlighted = goals.count { it.highlight } > 0

        val filter = showOnlyHighlighted && !editDisplay
        val finalGoal = if (filter) {
            goals.filter { it.highlight }
        } else goals

        finalGoal.mapTo(this) {
            val currentlyHighlighted = it.highlight
            val highlightColor = if (currentlyHighlighted && editDisplay) "§e" else "§7"
            val display = "  $highlightColor" + format(it)

            if (editDisplay) {
                val clickName = if (currentlyHighlighted) "remove" else "add"
                Renderable.clickAndHover(
                    display,
                    listOf(
                        "§a" + it.displayName,
                        "",
                        "§eClick to $clickName this goal as highlight!",
                    ),
                    onClick = {
                        if (lastClick.passedSince() < 300.milliseconds) return@clickAndHover
                        lastClick = SimpleTimeMark.now()

                        it.highlight = !currentlyHighlighted
                        update()
                    }
                )
            } else {
                Renderable.string(display)
            }
        }
        if (filter) {
            val missing = goals.size - finalGoal.size
            add(Renderable.string("  §8+ $missing not highlighted goals."))
        }
    }

    private var highlightedMaps = mutableMapOf<String, Boolean>()

    var BingoGoal.highlight: Boolean
        get() = highlightedMaps[displayName] ?: false
        set(value) {
            highlightedMaps[displayName] = value
        }

    private var lastSneak = false
    private var inventoryOpen = false

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!LorenzUtils.isBingoProfile) return
        if (!config.enabled) return

        val currentlyOpen = canEditDisplay()
        if (inventoryOpen != currentlyOpen) {
            inventoryOpen = currentlyOpen
            update()
        }

        if (config.quickToggle && ItemUtils.isSkyBlockMenuItem(InventoryUtils.getItemInHand())) {
            val sneaking = Minecraft.getMinecraft().thePlayer.isSneaking
            if (lastSneak != sneaking) {
                lastSneak = sneaking
                if (sneaking) {
                    toggleMode()
                }
            }
        }
        if (!config.stepHelper && displayMode == 1) {
            displayMode = 2
        }
        if (displayMode == 0) {
            if (Minecraft.getMinecraft().currentScreen !is GuiChat) {
                config.bingoCardPos.renderRenderables(display, posLabel = "Bingo Card")
            }
        } else if (displayMode == 1) {
            config.bingoCardPos.renderStrings(BingoNextStepHelper.currentHelp, posLabel = "Bingo Card")
        }
    }

    private fun canEditDisplay() =
        Minecraft.getMinecraft().currentScreen is GuiInventory || InventoryUtils.openInventoryName() == "Bingo Card"

    @SubscribeEvent
    fun onBingoCardUpdate(event: BingoCardUpdateEvent) {
        if (!config.enabled) return
        update()
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.hideCommunityGoals.onToggle { update() }
        config.nextTipDuration.onToggle { update() }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "bingo", "event.bingo")
    }
}
