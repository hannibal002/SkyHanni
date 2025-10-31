package at.hannibal2.hanni.features.bingo.card

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.config.commands.CommandCategory
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.bingo.BingoCardUpdateEvent
import at.hannibal2.hanni.features.bingo.BingoApi
import at.hannibal2.hanni.features.bingo.card.goals.BingoGoal
import at.hannibal2.hanni.features.bingo.card.nextstephelper.BingoNextStepHelper
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.ConditionalUtils.onToggle
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.InventoryDetector
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils
import at.hannibal2.hanni.utils.RenderUtils.renderRenderable
import at.hannibal2.hanni.utils.RenderUtils.renderRenderables
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils
import at.hannibal2.hanni.utils.StringUtils.removeColor
import at.hannibal2.hanni.utils.TimeUtils.format
import at.hannibal2.hanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.hanni.utils.compat.MinecraftCompat
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.hanni.utils.renderables.primitives.text
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiChat
import kotlin.time.Duration.Companion.days

@HanniModule
object BingoCardDisplay {

    private const val MAX_PERSONAL_GOALS = 20
    private val config get() = HanniMod.feature.event.bingo.bingoCard
    private val patternGroup = RepoPattern.group("bingo.card.display")
    private val bingoCardInventoryPattern by patternGroup.pattern("inventory", "Bingo Card")
    private val bingoCardInventoryDetector = InventoryDetector(bingoCardInventoryPattern) { dirty = true }

    private var hasHiddenPersonalGoals = false
    private var displayCache: List<Renderable> = emptyList()
    private var dirty = true

    // Todo use an enum for the display modes, what does 0 mean?
    private var displayMode = 0

    private fun reload() {
        ChatUtils.chat("Reloaded bingo goals")
        BingoApi.bingoGoals.clear()
    }

    private fun toggleCommand() {
        if (!SkyBlockUtils.isBingoProfile) {
            ChatUtils.userError("This command only works on a bingo profile!")
            return
        }
        if (!config.enabled) {
            ChatUtils.userError("Bingo Card is disabled in the config!")
            return
        }
        toggleMode()
        ChatUtils.chat("Toggled Bingo Card Visibility!")
    }

    private fun toggleMode() {
        displayMode++
        if (displayMode == 3) {
            displayMode = 0
        }
    }

    private fun isEnabled() = config.enabled && SkyBlockUtils.isBingoProfile

    @HandleEvent
    fun onSecondPassed() {
        if (!isEnabled()) return
        if (hasHiddenPersonalGoals) dirty = true
    }

    @HandleEvent
    fun onBingoCardUpdate(event: BingoCardUpdateEvent) {
        if (!isEnabled()) return
        dirty = true
    }

    @HandleEvent
    fun onConfigLoad() {
        config.hideCommunityGoals.onToggle { dirty = true }
        config.nextTipDuration.onToggle { dirty = true }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shbingotoggle") {
            description = "Toggle the bingo card display mode"
            category = CommandCategory.USERS_ACTIVE
            callback { toggleCommand() }
        }
        event.registerBrigadier("shreloadbingodata") {
            description = "Reloads the bingo card data"
            category = CommandCategory.DEVELOPER_DEBUG
            simpleCallback { reload() }
        }
    }

    // todo use RenderDisplayHelper
    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return

        if (dirty) {
            displayCache = drawDisplay()
            dirty = false
        }

        if (config.quickToggle && ItemUtils.isSkyBlockMenuItem(InventoryUtils.getItemInHand())) {
            val sneaking = MinecraftCompat.localPlayer.isSneaking
            if (lastSneak != sneaking) {
                lastSneak = sneaking
                if (sneaking) {
                    toggleMode()
                }
            }
        }
        if (!config.stepHelper && displayMode == 1) displayMode = 2
        if (displayMode == 0 && Minecraft.getMinecraft().currentScreen !is GuiChat) {
            config.bingoCardPos.renderRenderables(displayCache, posLabel = "Bingo Card")
        } else if (displayMode == 1) {
            val helpRenderable = Renderable.vertical(
                BingoNextStepHelper.currentHelp.map { Renderable.text(it) }
            )
            config.bingoCardPos.renderRenderable(helpRenderable, posLabel = "Bingo Card")
        }
    }

    private fun drawDisplay(): List<Renderable> = buildList {
        if (BingoApi.bingoGoals.isEmpty()) {
            addString("§6Bingo Goals:")
            add(
                Renderable.clickable(
                    "§cOpen the §e/bingo §ccard.",
                    tips = listOf("Click to run §e/bingo"),
                    onLeftClick = {
                        HypixelCommands.bingo()
                    },
                ),
            )
        } else {
            if (!config.hideCommunityGoals.get()) addCommunityGoals()
            addPersonalGoals()
        }
    }

    private fun MutableList<Renderable>.addCommunityGoals() {
        addString("§6Community Goals:")
        val goals = BingoApi.communityGoals.toMutableList()
        var hiddenGoals = 0
        for (goal in goals.toList()) {
            if (goal.hiddenGoalData.unknownTip) {
                hiddenGoals++
                goals.remove(goal)
            }
        }

        addGoals(goals) {
            val percentageFormat = percentageFormat(it)
            val name = it.description.removeColor()
            "$name$percentageFormat"
        }

        if (hiddenGoals > 0) {
            val name = StringUtils.pluralize(hiddenGoals, "goal")
            addString("§7+ $hiddenGoals more §cunknown §7community $name.")
        }
        addString(" ")
    }

    private fun percentageFormat(it: BingoGoal) = it.communtyGoalPercentage?.let {
        " " + BingoApi.getCommunityPercentageColor(it)
    }.orEmpty()

    private fun MutableList<Renderable>.addPersonalGoals() {
        val todo = BingoApi.personalGoals.filter { !it.done }.toMutableList()
        val done = MAX_PERSONAL_GOALS - todo.size
        addString("§6Personal Goals: ($done/$MAX_PERSONAL_GOALS done)")

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
            val name = StringUtils.pluralize(hiddenGoals, "goal")
            addString("§7+ $hiddenGoals more §cunknown §7$name.")
        }
        hasHiddenPersonalGoals = config.nextTipDuration.get() && nextTip != 14.days
        if (hasHiddenPersonalGoals) {
            val nextTipTime = BingoApi.lastBingoCardOpenTime + nextTip
            if (nextTipTime.isInPast()) {
                addString("§eThe next hint got unlocked already!")
                addString("§eOpen the bingo card to update!")
            } else {
                val until = nextTipTime.timeUntil()
                addString("§eThe next hint will unlock in §b${until.format(maxUnits = 2)}")
            }
        }
    }

    private fun MutableList<Renderable>.addGoals(goals: MutableList<BingoGoal>, format: (BingoGoal) -> String) {
        val editDisplay = bingoCardInventoryDetector.isInside()
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
                Renderable.clickable(
                    display,
                    tips = buildList {
                        add("§a" + it.displayName)
                        for (s in it.guide) {
                            add(s)
                        }
                        add("")
                        add("§eClick to $clickName this goal as highlight!")
                    },
                    onLeftClick = {
                        it.highlight = !currentlyHighlighted
                        it.displayName
                        dirty = true
                    },
                )
            } else {
                Renderable.text(display)
            }
        }
        if (filter) {
            val missing = goals.size - finalGoal.size
            addString("  §8+ $missing not highlighted goals.")
        }
    }

    private var lastSneak = false
    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "bingo", "event.bingo")
    }
}
