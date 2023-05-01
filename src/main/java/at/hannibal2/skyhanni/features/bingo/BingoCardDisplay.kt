package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.bingo.card.CommunityGoal
import at.hannibal2.skyhanni.features.bingo.card.PersonalGoal
import at.hannibal2.skyhanni.utils.InventoryUtils.getInventoryName
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class BingoCardDisplay {

    private val MAX_PERSONAL_GOALS = 20
    private val MAX_COMMUNITY_GOALS = 5

    private var tick = 0
    private var display = listOf<String>()
    private val config get() = SkyHanniMod.feature.bingo.bingoCard
    private val goalCompletePattern = "§6§lBINGO GOAL COMPLETE! §r§e(?<name>.*)".toPattern()

    init {
        update()
    }

    companion object {
        val personalGoals = mutableListOf<PersonalGoal>()
        private val communityGoals = mutableListOf<CommunityGoal>()

        private var dirty = true

        fun command() {
            reload()
        }

        private fun reload() {
            personalGoals.clear()
            communityGoals.clear()
            dirty = true
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!LorenzUtils.isBingoProfile) return
        if (!config.enabled) return
        if (event.phase != TickEvent.Phase.START) return

        tick++
        if (tick % 5 != 0) return

        val gui = Minecraft.getMinecraft().currentScreen
        if (gui !is GuiChest) {
            dirty = true
            return
        }

        val chest = gui.inventorySlots as ContainerChest
        if (chest.getInventoryName() == "Bingo Card") {
            if (dirty) {
                readBingoCard(gui)
            }
        }
    }

    private fun readBingoCard(gui: GuiChest) {
        personalGoals.clear()
        communityGoals.clear()

        for (slot in gui.inventorySlots.inventorySlots) {
            val stack = slot?.stack ?: continue
            val personalGoal = stack.getLore().any { it.endsWith("Personal Goal") }
            val communityGoal = stack.getLore().any { it.endsWith("Community Goal") }
            if (!personalGoal && !communityGoal) continue
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
            if (personalGoal) {
                personalGoals.add(PersonalGoal(name, description, done))
            } else {
                communityGoals.add(CommunityGoal(name, description, done))
            }
        }

        val a = personalGoals.size
        val b = communityGoals.size
        if (a == MAX_PERSONAL_GOALS && b == MAX_COMMUNITY_GOALS) {
            dirty = false

            update()
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
                newList.add("§6Community Goals:")
                communityGoals.mapTo(newList) { "  " + it.description + if (it.done) " §aDONE" else "" }
                newList.add(" ")
            }

            val todo = personalGoals.filter { !it.done }
            val done = MAX_PERSONAL_GOALS - todo.size
            newList.add("§6Personal Goals: ($done/$MAX_PERSONAL_GOALS done)")
            todo.mapTo(newList) { "  " + it.description }
        }
        return newList
    }

    private var lastSneak = false
    private var displayMode = 0

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!LorenzUtils.isBingoProfile) return
        if (!config.enabled) return

        val stack = Minecraft.getMinecraft().thePlayer.heldItem //TODO into ItemUtils or InventoryUtils
        if (ItemUtils.isSkyBlockMenuItem(stack)) {
            val sneaking = Minecraft.getMinecraft().thePlayer.isSneaking
            if (lastSneak != sneaking) {
                lastSneak = sneaking
                if (sneaking) {
                    displayMode++
                    if (displayMode == 3) {
                        displayMode = 0
                    }
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
        config.hideCommunityGoals.whenChanged { _, _ -> update() }
    }
}