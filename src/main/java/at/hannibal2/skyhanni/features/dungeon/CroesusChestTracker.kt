package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage.DungeonStorage.DungeonRunInfo
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.SackApi.getAmountInSacks
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.kuudra.KuudraCompleteEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi.DungeonChest
import at.hannibal2.skyhanni.features.dungeon.DungeonApi.inDungeon
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.getAmountInInventory
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import kotlin.time.Duration.Companion.days

@SkyHanniModule
object CroesusChestTracker {

    private val config get() = SkyHanniMod.feature.dungeon.chest

    private val patternGroup = RepoPattern.group("dungeon.croesus")

    private val croesusPattern by patternGroup.pattern("inventory", "Croesus")
    private val croesusEmptyPattern by patternGroup.pattern("empty", "§cNo treasures!")
    private val kismetPattern by patternGroup.pattern("kismet.reroll", "§aReroll Chest")
    private val kismetUsedPattern by patternGroup.pattern("kismet.used", "§aYou already rerolled a chest!")

    private val floorPattern by patternGroup.pattern("chest.floor", "§7Tier: §eFloor (?<floor>[IV]+)")
    private val masterPattern by patternGroup.pattern("chest.master", ".*Master.*")

    /**
     * REGEX-TEST: §eInfernal Tier
     */
    private val kuudraPattern by patternGroup.pattern("chest.kuudra", "§e(?<tier>Basic|Hot|Burning|Fiery|Infernal) Tier")

    /**
     * REGEX-TEST: §aNo more chests to open!
     */
    private val keyUsedPattern by patternGroup.pattern("chest.state.keyused", "§aNo more chests to open!")

    /**
     * REGEX-TEST: §7Opened Chest: §fWood
     */
    private val openedPattern by patternGroup.pattern("chest.state.opened", "§.Opened [cC]hest:.*")

    /**
     * REGEX-TEST: §cNo chests opened yet!
     */
    private val unopenedPattern by patternGroup.pattern("chest.state.unopened", "§cNo chests opened yet!")

    private const val KISMET_SLOT = 50
    private const val EMPTY_SLOT = 22
    private const val FRONT_ARROW_SLOT = 53
    private const val BACK_ARROW_SLOT = 45
    private const val MAX_CHESTS = 60

    private val kismetInternalName = "KISMET_FEATHER".toInternalName()

    private var inCroesusInventory = false
    private var croesusEmpty = false
    private var currentPage = 0
    private var pageSwitchable = false

    private var chestInventory: DungeonChest? = null

    private var currentRunIndex = 0

    private var kismetAmountCache = 0

    private var display: List<Renderable>? = null

    private val croesusChests get() = ProfileStorageData.profileSpecific?.dungeons?.runs

    @HandleEvent(priority = HandleEvent.LOW, onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!SkyHanniMod.feature.dungeon.croesusUnopenedChestTracker) return

        if (inCroesusInventory && !croesusEmpty) {
            for ((run, slot) in InventoryUtils.getItemsInOpenChest()
                .mapNotNull { slot -> runSlots(slot.slotIndex, slot) }) {

                // If one chest is null every followup chest is null. Therefore, an early return is possible
                if (run.floor == null) return

                val state = run.openState ?: OpenedState.UNOPENED

                if (state != OpenedState.KEY_USED) {
                    slot.highlight(if (state == OpenedState.OPENED) LorenzColor.DARK_AQUA else LorenzColor.DARK_PURPLE)
                }
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if ((SkyHanniMod.feature.dungeon.croesusUnopenedChestTracker || config.showUsedKismets) &&
            croesusPattern.matches(event.inventoryName)
        ) {
            pageSetup(event)
            checkValidChests()

            if (croesusEmpty) {
                croesusChests?.forEach { it.setValuesNull() }
                return
            }

            // With null, since if an item is missing the chest will be set null
            checkChests(event.inventoryItemsWithNull)

            return
        }
        if (config.showUsedKismets || config.kismetStackSize) {
            kismetDungeonChestSetup(event)
        }
    }

    private fun kismetDungeonChestSetup(event: InventoryFullyOpenedEvent) {
        chestInventory = DungeonChest.getByInventoryName(event.inventoryName) ?: return
        if (config.kismetStackSize) {
            kismetAmountCache = getKismetAmount()
        }
        if (config.showUsedKismets) {
            val kismetItem = event.inventoryItems[KISMET_SLOT] ?: return
            if (config.showUsedKismets && kismetUsedPattern.matches(kismetItem.getLore().lastOrNull()))
                setKismetUsed()
        }
    }

    private fun checkChests(inventory: Map<Int, ItemStack?>) {
        for ((run, item) in inventory.mapNotNull { (key, value) -> runSlots(key, value) }) {
            if (item == null) {
                run.setValuesNull()
                continue
            }

            val lore = item.getLore()

            if (run.floor == null || run.floor == "F0") run.floor =
                (if (masterPattern.matches(item.displayName)) "M" else "F") + (
                    lore.firstNotNullOfOrNull {
                        floorPattern.matchMatcher(it) { group("floor").romanToDecimal() }
                    } ?: "0"
                    )
            if (run.floor == "F0" && kuudraPattern.matches(item.displayName)) run.floor =
                ("T" + KuudraApi.getKuudraRunTierNumber(lore.firstNotNullOfOrNull { kuudraPattern.matchMatcher(it) { group("tier") } }))
            run.openState = when {
                keyUsedPattern.anyMatches(lore) -> OpenedState.KEY_USED
                openedPattern.anyMatches(lore) -> OpenedState.OPENED
                unopenedPattern.anyMatches(lore) -> OpenedState.UNOPENED
                else -> ErrorManager.logErrorStateWithData(
                    "Croesus Chest couldn't be read correctly.",
                    "Open state check failed for chest.",
                    "run" to run,
                    "lore" to lore,
                ).run { null }
            }
        }
    }

    private fun pageSetup(event: InventoryFullyOpenedEvent) {
        inCroesusInventory = true
        pageSwitchable = true
        croesusEmpty = croesusEmptyPattern.matches(event.inventoryItems[EMPTY_SLOT]?.displayName)
        if (event.inventoryItems[BACK_ARROW_SLOT]?.item != Items.arrow) {
            currentPage = 0
        }
    }

    private fun DungeonRunInfo.setValuesNull() {
        floor = null
        openState = null
        kismetUsed = null
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inCroesusInventory = false
        chestInventory = null
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!config.showUsedKismets) return
        if (chestInventory != null && event.slotId == KISMET_SLOT) {
            setKismetUsed()
            return
        }
        if (inCroesusInventory && !croesusEmpty) {
            if (event.slot == null) return
            when (event.slotId) {
                FRONT_ARROW_SLOT -> if (pageSwitchable && event.slot.stack.isArrow()) {
                    pageSwitchable = false
                    currentPage++
                }

                BACK_ARROW_SLOT -> if (pageSwitchable && event.slot.stack.isArrow()) {
                    pageSwitchable = false
                    currentPage--
                }

                else -> croesusSlotMapToRun(event.slotId)?.let { currentRunIndex = it }
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!config.kismetStackSize) return
        if (chestInventory == null) return
        if (!kismetPattern.matches(event.stack.displayName)) return
        if (kismetUsedPattern.matches(event.stack.getLore().lastOrNull())) return
        event.stackTip = "§a$kismetAmountCache"
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderItemTipIsKismetable(event: RenderInventoryItemTipEvent) {
        if (!config.showUsedKismets) return
        if (!inCroesusInventory) return
        if (event.slot.slotIndex != event.slot.slotNumber) return
        val run = croesusSlotMapToRun(event.slot.slotIndex) ?: return
        if (!getKismetUsed(run)) return
        event.offsetY = -1
        event.offsetX = -9
        event.stackTip = "§a✔"
    }

    @HandleEvent
    fun onKuudraComplete(event: KuudraCompleteEvent) {
        addCroesusChest("T${event.kuudraTier}")
    }

    @HandleEvent
    fun onDungeonComplete(event: DungeonCompleteEvent) {
        if (event.floor == "E") return
        addCroesusChest(event.floor)
    }

    @Suppress("MaxLineLength", "ComplexCondition")
    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!((config.croesusOverlayKuudra && KuudraApi.inKuudra) || config.croesusOverlayDungeons && inDungeon() || (config.croesusOverlay && (SkyBlockUtils.graphArea == "Dungeon Hub" || SkyBlockUtils.graphArea == "Forgotten Skull")))) return
        val renderables = display ?: createRenderable().also { display = it }
        config.croesusOverlayPosition.renderRenderables(renderables, posLabel = "Croesus Overlay")
    }

    private fun createRenderable(): List<Renderable> {
        return buildList {
            add(
                Renderable.text("Chests: ${chestCountColor(croesusChests?.size ?: 0)}/${MAX_CHESTS}"),
            )
        }
    }

    private fun chestCountColor(size: Int): String {
        return when {
            size >= 45 -> "§4"
            size >= 30 -> "§c"
            size >= 15 -> "§e"
            size >= 0 -> "§6"
            else -> "§0"
        } + size.toString()
    }

    private fun checkValidChests() {
        var removalNum = 0
        val iterator = croesusChests?.iterator()
        if (iterator != null) {
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (next.floor == null) {
                    iterator.remove()
                    removalNum++
                }
                val sinceRun = next.runTime?.passedSince() ?: 0.days // purely exists for pre-addition runs
                if (sinceRun > 3.days) {
                    iterator.remove()
                    ChatUtils.debug("Chest Removed due to Time Expiring.")
                }
            }
        }
        ChatUtils.debug("$removalNum null Croesus chests removed")
    }


    private fun addCroesusChest(floorOrTier: String) {
        croesusChests?.add(0, DungeonRunInfo(floorOrTier, SimpleTimeMark.now()))
        checkValidChests()
        currentRunIndex = 0
        if ((croesusChests?.size ?: 0) > MAX_CHESTS) {
            croesusChests?.dropLast(1)
        }

        if (config.croesusLimit && getLastActiveChest() >= 55) {
            ChatUtils.chat("You are close to the Croesus Limit. Please open your chests!")
        }
    }

    private fun Int.getRun() = getRun0(this)

    private fun getRun0(run: Int = currentRunIndex) = croesusChests?.takeIf { run < it.size }?.get(run)

    private fun setKismetUsed() {
        getRun0()?.kismetUsed = true
    }

    private fun getKismetUsed(runIndex: Int) = getRun0(runIndex)?.kismetUsed ?: false

    private fun getKismetAmount() = kismetInternalName.getAmountInSacks() + kismetInternalName.getAmountInInventory()

    private fun croesusSlotMapToRun(slotId: Int) = when (slotId) {
        in 10..16 -> slotId - 10 // 0 - 6
        in 19..25 -> slotId - 12 // 7 - 13
        in 28..34 -> slotId - 14 // 14 - 20
        in 37..43 -> slotId - 16 // 21 - 27
        else -> null
    }?.let { it + currentPage * 28 }

    private fun ItemStack.isArrow() = this.item == Items.arrow

    private inline fun <reified T> runSlots(slotId: Int, any: T) =
        croesusSlotMapToRun(slotId)?.getRun()?.let { it to any }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetkismet") {
            description = "Resets the saved values of the applied kismet feathers in Croesus"
            category = CommandCategory.USERS_RESET
            simpleCallback {
                croesusChests?.let {
                    it.clear()
                    it.addAll(generateMaxChest())
                    ChatUtils.chat("Kismet State was Reset!")
                }
            }
        }
    }

    @JvmStatic
    fun generateMaxChestAsList(): MutableList<DungeonRunInfo> = generateMaxChest().toMutableList()
    private fun generateMaxChest(): Sequence<DungeonRunInfo> = generateSequence { DungeonRunInfo() }.take(MAX_CHESTS)

    private fun getLastActiveChest(includeDungeonKey: Boolean = false): Int = (
        croesusChests?.indexOfLast {
            it.floor != null &&
                (it.openState == OpenedState.UNOPENED || (includeDungeonKey && it.openState == OpenedState.OPENED))
        } ?: -1
        ) + 1

    enum class OpenedState {
        UNOPENED,
        OPENED,
        KEY_USED,
    }
}
