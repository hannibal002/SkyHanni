package at.hannibal2.skyhanni.features.bingo.card

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.bingo.BingoCardUpdateEvent
import at.hannibal2.skyhanni.features.bingo.BingoAPI
import at.hannibal2.skyhanni.features.bingo.card.nextstephelper.BingoNextStepHelper
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.onToggle
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiChat
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.days

class BingoCardDisplay {

    private var display = emptyList<String>()

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

    private fun drawDisplay(): MutableList<String> {
        val newList = mutableListOf<String>()

        if (BingoAPI.bingoGoals.isEmpty()) {
            newList.add("§6Bingo Goals:")
            newList.add("§cOpen the §e/bingo §ccard.")
        } else {
            if (!config.hideCommunityGoals.get()) {
                newList.addCommunityGoals()
            }
            newList.addPersonalGoals()
        }
        return newList
    }

    private fun MutableList<String>.addCommunityGoals() {
        add("§6Community Goals:")
        val goals = BingoAPI.communityGoals.toMutableList()
        var hiddenGoals = 0
        for (goal in goals.toList()) {
            if (goal.hiddenGoalData.unknownTip) {
                hiddenGoals++
                goals.remove(goal)
            }
        }

        goals.mapTo(this) { "  " + it.description + if (it.done) " §aDONE" else "" }
        if (hiddenGoals > 0) {
            add("§7+ $hiddenGoals hidden community goals.")
        }
        add(" ")
    }

    private fun MutableList<String>.addPersonalGoals() {
        val todo = BingoAPI.personalGoals.filter { !it.done }.toMutableList()
        val done = MAX_PERSONAL_GOALS - todo.size
        add("§6Personal Goals: ($done/$MAX_PERSONAL_GOALS done)")

        var hiddenGoals = 0
        var nextTip = 7.days
        for (goal in todo.toList()) {
            val hiddenGoalData = goal.hiddenGoalData
            if (hiddenGoalData.unknownTip) {
                hiddenGoals++
                todo.remove(goal)
            }

            hiddenGoalData.nextHintTime?.let {
                if (it < nextTip) {
                    nextTip = it
                }
            }
        }

        todo.mapTo(this) { "  " + it.description }
        if (hiddenGoals > 0) {
            val name = StringUtils.canBePlural(hiddenGoals, "goal", "goals")
            add("§7+ $hiddenGoals more unknown $name.")
        }
        hasHiddenPersonalGoals = config.nextTipDuration.get() && nextTip != 7.days
        if (hasHiddenPersonalGoals) {
            val nextTipTime = BingoAPI.lastBingoCardOpenTime + nextTip
            if (nextTipTime.isInPast()) {
                add("§eThe next hint got unlocked already!")
                add("§eOpen the bingo card to update!")
            } else {
                val until = nextTipTime.timeUntil()
                add("§eThe next hint will unlock in §b${until.format(maxUnits = 2)}")
            }
        }
    }

    private var lastSneak = false

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.isBingoProfile) return
        if (!config.enabled) return

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
                config.bingoCardPos.renderStrings(display, posLabel = "Bingo Card")
            }
        } else if (displayMode == 1) {
            config.bingoCardPos.renderStrings(BingoNextStepHelper.currentHelp, posLabel = "Bingo Card")
        }
    }

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
