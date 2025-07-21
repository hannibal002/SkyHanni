package at.hannibal2.skyhanni.features.rift.everywhere

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.jsonobjects.repo.EnigmaSoulsJson
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.features.rift.area.dreadfarm.WoodenButtonsHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.getAllItems
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SpecialColor.toSpecialColor
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraft.inventory.ContainerChest

@SkyHanniModule
object EnigmaSoulWaypoints {

    private val config get() = RiftApi.config.enigmaSoulWaypoints
    private var inInventory = false
    var soulLocations = mapOf<String, Map<String, LorenzVec>>()
    private val trackedSouls = mutableMapOf<String, MutableList<String>>()
    private val inventoryUnfound = mutableListOf<String>()
    private var adding = true

    private val item by lazy {
        val neuItem = "SKYBLOCK_ENIGMA_SOUL".toInternalName().getItemStack()
        ItemUtils.createItemStack(
            neuItem.item,
            "§5Toggle Missing",
            "§7Click here to toggle",
            "§7the waypoints for each",
            "§7missing souls on this page",
        )
    }

    private val patternGroup = RepoPattern.group("rift.everywhere.enigma-souls")

    /**
     * REGEX-TEST: Enigma: Tough Bark
     * REGEX-TEST: ✔ Enigma: Woods Flower Pot
     */
    private val enigmaTitlePattern by patternGroup.pattern(
        "title",
        "(?:✔ )?Enigma: (?<name>.+)",
    )

    /**
     * REGEX-TEST: ✖ Not completed yet!
     */
    private val notCompletedPattern by patternGroup.pattern(
        "not-completed",
        "✖ Not completed yet!",
    )

    /**
     * REGEX-TEST: To Rift Guide ➜ Wyld Woods
     */
    private val guideAreaPattern by patternGroup.pattern(
        "guide-area",
        "To Rift Guide ➜ (?<area>.+)",
    )

    /**
     * REGEX-TEST: SOUL! You unlocked an Enigma Soul!
     * REGEX-TEST: You have already found that Enigma Soul!
     */
    private val foundPattern by patternGroup.pattern(
        "found",
        "SOUL! You unlocked an Enigma Soul!|You have already found that Enigma Soul!",
    )

    @HandleEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!isEnabled()) return

        if (inventoryUnfound.isEmpty()) return
        if (event.inventory is ContainerLocalMenu && inInventory && event.slot == 31) {
            event.replace(item)
        }
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        inInventory = false
        if (!event.inventoryName.contains("Enigma Souls")) return
        inInventory = true

        for (stack in event.inventoryItems.values) {
            stack.getLore().lastOrNull()?.let {
                if (notCompletedPattern.matches(it.removeColor())) {
                    enigmaTitlePattern.matchMatcher(stack.displayName.removeColor()) {
                        inventoryUnfound.add(group("name"))
                    }
                }
            }
        }
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
        inventoryUnfound.clear()
        adding = true
    }

    @HandleEvent(priority = HandleEvent.HIGH)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!inInventory || !isEnabled()) return

        val area = getSelectedArea() ?: return

        if (event.slotId == 31 && inventoryUnfound.isNotEmpty()) {
            event.makePickblock()
            if (inventoryUnfound.contains("Buttons")) {
                RiftApi.trackingButtons = !RiftApi.trackingButtons
            }
            if (adding) {
                trackedSouls.getOrPut(area) { mutableListOf() }.addAll(inventoryUnfound)
                adding = false
            } else {
                trackedSouls[area]?.removeAll(inventoryUnfound)
                adding = true
            }
        }

        if (event.slot?.stack == null) return

        val name = enigmaTitlePattern.matchMatcher(event.slot.stack.displayName.removeColor()) {
            group("name")
        } ?: return
        event.makePickblock()
        if (soulLocations[area]?.contains(name) != true) return

        if (name == "Buttons") {
            RiftApi.trackingButtons = !RiftApi.trackingButtons
        }

        if (trackedSouls[area]?.contains(name) != true) {
            ChatUtils.chat("§5Tracking the $name Enigma Soul!", prefixColor = "§5")
            if (config.showPathFinder) {
                soulLocations[area]?.get(name)?.let {
                    if (!(name == "Buttons" && WoodenButtonsHelper.showButtons())) {
                        IslandGraphs.pathFind(
                            it,
                            "$name Enigma Soul",
                            config.color.toSpecialColor(),
                            condition = { config.showPathFinder },
                        )
                    }
                }
            }
            trackedSouls.getOrPut(area) { mutableListOf() }.add(name)
        } else {
            trackedSouls[area]?.remove(name)
            ChatUtils.chat("§5No longer tracking the $name Enigma Soul!", prefixColor = "§5")
            IslandGraphs.stop()
        }
    }

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled() || !inInventory) return

        if (event.gui !is GuiChest) return
        val chest = event.container as ContainerChest

        val area = getSelectedArea() ?: return
        val tracked = trackedSouls[area] ?: return

        for ((slot, stack) in chest.getAllItems()) {
            enigmaTitlePattern.matchMatcher(stack.displayName.removeColor()) {
                if (group("name") in tracked) {
                    slot.highlight(LorenzColor.DARK_PURPLE)
                }
            }
        }
        if (!adding) {
            chest.inventorySlots[31].highlight(LorenzColor.DARK_PURPLE)
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        for ((area, souls) in trackedSouls) {
            for (name in souls) {
                soulLocations[area]?.get(name)?.let { position ->
                    event.drawWaypointFilled(position, config.color.toSpecialColor(), seeThroughBlocks = true, beacon = true)
                    event.drawDynamicText(position.up(), "§5${name.removeSuffix(" Soul")} Soul", 1.5)
                }
            }
        }
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<EnigmaSoulsJson>("EnigmaSouls")
        val areas = data.areas
        soulLocations = buildMap {
            for ((area, souls) in areas) {
                this[area] = buildMap {
                    for (soul in souls) {
                        this[soul.name] = soul.position
                    }
                }
            }
        }
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return
        if (foundPattern.matches(event.message.removeColor().trim())) {
            hideClosestSoul()
        }
    }

    private fun getSelectedArea(): String? = InventoryUtils.getSlotAtIndex(40)?.stack?.getLore()?.firstOrNull()?.let {
        guideAreaPattern.matchMatcher(it.removeColor()) {
            group("area")
        }
    }

    private fun hideClosestSoul() {
        var closestSoul = ""
        var closestArea = ""
        var closestDistance = 8.0

        for ((area, souls) in soulLocations) {
            for ((name, position) in souls) {
                if (position.distanceToPlayer() < closestDistance) {
                    closestSoul = name
                    closestArea = area
                    closestDistance = position.distanceToPlayer()
                }
            }
        }
        if (trackedSouls[closestArea]?.contains(closestSoul) == true) {
            trackedSouls[closestArea]?.remove(closestSoul)
            ChatUtils.chat("§5Found the $closestSoul Enigma Soul!", prefixColor = "§5")
            if (closestSoul == "Buttons") {
                RiftApi.trackingButtons = false
            }
        }
    }

    fun isEnabled() = RiftApi.inRift() && config.enabled
}
