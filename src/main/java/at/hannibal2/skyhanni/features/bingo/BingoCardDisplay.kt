package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.jsonobjects.repo.BingoJson.BingoTip
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.bingo.card.CommunityGoal
import at.hannibal2.skyhanni.features.bingo.card.PersonalGoal
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.onToggle
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiChat
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.days

class BingoCardDisplay {

    private var display = emptyList<String>()

    // TODO USE SH-REPO
    private val goalCompletePattern = "§6§lBINGO GOAL COMPLETE! §r§e(?<name>.*)".toPattern()

    private var lastBingoCardOpenTime = SimpleTimeMark.farPast()
    private var hasHiddenPersonalGoals = false

    init {
        update()
    }

    companion object {
        private const val MAX_PERSONAL_GOALS = 20
        private const val MAX_COMMUNITY_GOALS = 5

        val personalHiddenGoalPattern = ("§7This goal is currently §7§chidden§7! " +
            "It will be revealed §7to you when you complete it. §7 §7§eThe next hint will unlock in (?<time>.*)").toPattern()

        private val config get() = SkyHanniMod.feature.event.bingo.bingoCard
        private var displayMode = 0
        val personalGoals = mutableListOf<PersonalGoal>()
        private val communityGoals = mutableListOf<CommunityGoal>()

        fun command() {
            reload()
        }

        private fun reload() {
            personalGoals.clear()
            communityGoals.clear()
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

    private fun BingoTip.getDescriptionLine() = "§7" + note.joinToString(" ")

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.isBingoProfile) return
        if (!config.enabled) return
        if (event.inventoryName != "Bingo Card") return

        personalGoals.clear()
        communityGoals.clear()
        for (stack in event.inventoryItems.values) {
            val isPersonalGoal = stack.getLore().any { it.endsWith("Personal Goal") }
            val isCommunityGoal = stack.getLore().any { it.endsWith("Community Goal") }
            if (!isPersonalGoal && !isCommunityGoal) continue
            val name = stack.name?.removeColor() ?: continue
            val lore = stack.getLore()
            var index = 0
            val builder = StringBuilder()
            for (s in lore) {
                if (index > 1) {
                    if (s == "") break
                    builder.append(s)
                    builder.append(" ")
                }
                index++
            }
            var description = builder.toString()
            if (description.endsWith(" ")) {
                description = description.substring(0, description.length - 1)
            }
            if (description.startsWith("§7§7")) {
                description = description.substring(2)
            }

            val done = stack.getLore().any { it.contains("GOAL REACHED") }
            if (isPersonalGoal) {
                personalGoals.add(getPersonalGoal(name, description, done))
            } else {
                communityGoals.add(getCommunityGoal(name, description, done))
            }
        }
        lastBingoCardOpenTime = SimpleTimeMark.now()

        update()
    }

    private fun getPersonalGoal(
        name: String,
        description: String,
        done: Boolean
    ): PersonalGoal {
        var personalGoal = PersonalGoal(name, description, done)
        if (!done) {
            personalHiddenGoalPattern.matchMatcher(description) {
                BingoAPI.tips[name]?.let {
                    personalGoal = PersonalGoal(name, it.getDescriptionLine(), false)
                }
            }
        }
        return personalGoal
    }

    private fun getCommunityGoal(
        name: String,
        description: String,
        done: Boolean
    ): CommunityGoal {
        if (description == "§7This goal will be revealed §7when it hits Tier IV.") {
            BingoAPI.getCommunityTip(name)?.let {
                return CommunityGoal(name, it.getDescriptionLine(), done)
            }
        }
        return CommunityGoal(name, description, done)
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

        if (communityGoals.isEmpty()) {
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
        val goals = communityGoals.toMutableList()
        var hiddenGoals = 0
        for (goal in goals.toList()) {
            if (goal.description == "§7This goal will be revealed §7when it hits Tier IV.") {
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
        val todo = personalGoals.filter { !it.done }.toMutableList()
        val done = MAX_PERSONAL_GOALS - todo.size
        add("§6Personal Goals: ($done/$MAX_PERSONAL_GOALS done)")

        var hiddenGoals = 0
        var nextTip = 7.days
        for (goal in todo.toList()) {
            personalHiddenGoalPattern.matchMatcher(goal.description) {
                hiddenGoals++
                todo.remove(goal)
                val time = TimeUtils.getDuration(group("time").removeColor())
                if (time < nextTip) {
                    nextTip = time
                }
            }
        }

        todo.mapTo(this) { "  " + it.description }
        if (hiddenGoals > 0) {
            add("§7+ $hiddenGoals hidden personal goals.")
        }
        hasHiddenPersonalGoals = config.nextTipDuration.get() && nextTip != 7.days
        if (hasHiddenPersonalGoals) {
            val nextTipTime = lastBingoCardOpenTime + nextTip
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
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.isBingoProfile) return
        if (!config.enabled) return

        goalCompletePattern.matchMatcher(event.message) {
            val name = group("name")
            personalGoals.filter { it.displayName == name }
                .forEach {
                    it.done = true
                    update()
                }
        }
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
