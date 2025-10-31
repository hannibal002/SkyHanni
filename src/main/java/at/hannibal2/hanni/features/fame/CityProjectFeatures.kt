package at.hannibal2.hanni.features.fame

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.EntityMovementData
import at.hannibal2.hanni.data.IslandGraphs
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.ProfileStorageData
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.InventoryCloseEvent
import at.hannibal2.hanni.events.InventoryFullyOpenedEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.InventoryUtils.getUpperItems
import at.hannibal2.hanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.hanni.utils.ItemUtils
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.ItemUtils.repoItemName
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.NeuItems
import at.hannibal2.hanni.utils.NeuItems.getItemStack
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.NumberUtil.shortFormat
import at.hannibal2.hanni.utils.RegexUtils.firstMatcher
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.RenderUtils.highlight
import at.hannibal2.hanni.utils.RenderUtils.renderRenderable
import at.hannibal2.hanni.utils.SignUtils
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.TimeUtils
import at.hannibal2.hanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.hanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.hanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemStack
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@HanniModule
object CityProjectFeatures {

    private val config get() = HanniMod.feature.event.cityProject

    private var display: Renderable? = null
    private var inInventory = false
    private var lastReminderSend = SimpleTimeMark.farPast()

    private val patternGroup = RepoPattern.group("fame.projects")
    private val contributeAgainPattern by patternGroup.pattern(
        "contribute",
        "§7Contribute again: §e(?<time>.*)",
    )
    private val completedPattern by patternGroup.pattern(
        "completed",
        "§aProject is (?:being built|released)!",
    )

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!config.dailyReminder) return
        val playerSpecific = ProfileStorageData.playerSpecific ?: return
        if (ReminderUtils.isBusy()) return

        if (SkyBlockUtils.graphArea == "Community Center") return

        playerSpecific.nextCityProjectParticipationTime.let {
            if (it.isFarPast() || it.isInFuture()) return
        }
        if (lastReminderSend.passedSince() < 30.seconds) return
        lastReminderSend = SimpleTimeMark.now()

        ChatUtils.clickToActionOrDisable(
            "Daily City Project Reminder!",
            config::dailyReminder,
            actionName = "warp to Elizabeth",
            action = {
                HypixelCommands.warp("elizabeth")
                EntityMovementData.onNextTeleport(IslandType.HUB) {
                    IslandGraphs.pathFind(
                        LorenzVec(-1.7, 72.0, -102.0),
                        "§aCity Project",
                        condition = { config.dailyReminder },
                    )
                }
            },
        )
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {

        inInventory = false
        if (!inCityProject(event)) return
        inInventory = true

        if (config.showMaterials) {
            // internal name -> amount
            val materials = mutableMapOf<NeuInternalName, Int>()
            for ((_, item) in event.inventoryItems) {
                if (item.displayName != "§eContribute this component!") continue
                fetchMaterials(item, materials)
            }

            display = buildDisplay(materials)
        }

        if (config.showReady) {
            var nextTime = SimpleTimeMark.farFuture()
            val now = SimpleTimeMark.now()
            for ((_, item) in event.inventoryItems) {

                val lore = item.getLore()
                val completed = lore.lastOrNull()?.let { completedPattern.matches(it) } ?: false
                if (completed) continue
                contributeAgainPattern.firstMatcher(lore) {
                    val rawTime = group("time")
                    val duration = if (rawTime.contains("Soon!")) {
                        5.seconds
                    } else {
                        // hypixel rounds down to the next full minute, it shows "1m" when it is in fact 1-2 minutes, and "0m" for the last 60s
                        TimeUtils.getDuration(rawTime).let {
                            if (it < 1.hours) it + 1.minutes else it
                        }
                    }
                    val endTime = now + duration
                    if (endTime < nextTime) {
                        nextTime = endTime
                    }
                }
                if (item.displayName != "§eContribute this component!") continue
                nextTime = now
            }
            ProfileStorageData.playerSpecific?.nextCityProjectParticipationTime = nextTime
        }
    }

    private fun inCityProject(event: InventoryFullyOpenedEvent): Boolean {
        val lore = event.inventoryItems[4]?.getLore() ?: return false
        if (lore.isEmpty()) return false
        if (lore[0] != "§8City Project") return false
        return true
    }

    private fun buildDisplay(materials: MutableMap<NeuInternalName, Int>) = Renderable.vertical {
        addString("§7City Project Materials")

        if (materials.isEmpty()) {
            addString("§cNo Materials to contribute.")
        } else {
            for ((internalName, amount) in materials) {
                add(materialRow(internalName, amount))
            }
        }

    }

    private fun materialRow(internalName: NeuInternalName, amount: Int): Renderable {
        val stack = internalName.getItemStack()
        val name = internalName.repoItemName
        val price = internalName.getPrice() * amount

        return Renderable.horizontal {
            addString(" §7- ")
            addItemStack(stack)
            add(materialLink(name, amount))
            addString(" §7(§6${price.shortFormat()}§7)")
        }
    }

    private fun materialLink(name: String, amount: Int): Renderable = Renderable.optionalLink(
        "$name §ex${amount.addSeparators()}",
        {
            if (Minecraft.getMinecraft().currentScreen is GuiEditSign) {
                SignUtils.setTextIntoSign("$amount")
            } else {
                BazaarApi.searchForBazaarItem(name, amount)
            }
        },
    ) { inInventory && !NeuItems.neuHasFocus() }

    private fun fetchMaterials(item: ItemStack, materials: MutableMap<NeuInternalName, Int>) {
        var next = false
        val lore = item.getLore()
        val completed = lore.lastOrNull()?.let { completedPattern.matches(it) } ?: false
        if (completed) return
        // TODO: Refactor this loop to not have so many jumps
        @Suppress("LoopWithTooManyJumpStatements")
        for (line in lore) {
            if (line == "§7Cost") {
                next = true
                continue
            }
            if (!next) continue
            if (line == "" || line.contains("Bits")) break

            val (name, amount) = ItemUtils.readItemAmount(line) ?: continue
            val internalName = NeuInternalName.fromItemName(name)
            val old = materials.getOrPut(internalName) { 0 }
            materials[internalName] = old + amount
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!config.showMaterials) return
        if (!inInventory) return

        config.pos.renderRenderable(display, posLabel = "City Project Materials")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!config.showReady) return
        if (!inInventory) return

        if (event.gui !is GuiChest) return
        val chest = event.container as ContainerChest

        for ((slot, stack) in chest.getUpperItems()) {
            val lore = stack.getLore()
            if (lore.isEmpty()) continue
            val last = lore.last()
            if (last == "§eClick to contribute!") {
                slot.highlight(LorenzColor.YELLOW)
            }
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.cityProject", "event.cityProject")
    }
}
