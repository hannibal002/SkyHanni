package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.event.hoppity.HoppityUnclaimedEggsConfig.UnclaimedEggsOrder.SOONEST_FIRST
import at.hannibal2.skyhanni.data.mob.MobFilter.isRealPlayer
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.entity.EntityTransparencyActiveEvent
import at.hannibal2.skyhanni.events.entity.EntityTransparencyTickEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.api.ApiUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import net.minecraft.world.entity.player.Player

@SkyHanniModule
object HoppityEggDisplayManager {

    private val config get() = HoppityEggsManager.config
    private val unclaimedEggsConfig get() = config.unclaimedEggs

    private var display = listOf<Renderable>()

    private fun canChangeTransparency(entity: Player): Boolean {
        if (entity.isLocalPlayer) return false
        if (!entity.isRealPlayer()) return false

        val shouldHidePlayer = HoppityEggLocator.sharedEggLocation?.let { entity.distanceTo(it) < 4.0 }
            ?: HoppityEggLocator.possibleEggLocations.any { entity.distanceTo(it) < 4.0 }

        return config.playerTransparency < 100 && shouldHidePlayer
    }

    @HandleEvent
    fun onEntityTransparencyActive(event: EntityTransparencyActiveEvent) {
        event.setActive(HoppityEggLocator.isEnabled() && config.playerTransparency < 100)
    }

    @HandleEvent
    fun onEntityTransparencyTick(event: EntityTransparencyTickEvent<Player>) {
        if (canChangeTransparency(event.entity)) {
            event.newTransparency = config.playerTransparency
        }
    }

    @HandleEvent(SecondPassedEvent::class)
    fun onSecondPassed() {
        display = updateDisplay()
    }

    private fun updateDisplay(): List<Renderable> {
        if (!HoppityEggsManager.isActive()) return emptyList()
        if (!unclaimedEggsConfig.enabled) return emptyList()
        if (ReminderUtils.isBusy() && !unclaimedEggsConfig.showWhileBusy) return emptyList()

        val displayList: List<String> = buildList {
            add("§bUnclaimed Eggs:")
            HoppityEggType.resettingEntries.filter {
                it.hasRemainingSpawns() || // Only show eggs that have future spawns
                    !it.isClaimed() // Or eggs that have not been claimed
            }.let { entries ->
                if (unclaimedEggsConfig.displayOrder == SOONEST_FIRST) entries.sortedBy { it.timeUntil }
                else entries.sortedWith(compareBy<HoppityEggType> { it.altDay }.thenBy { it.resetsAt })
            }.forEach {
                val (color, timeFormat) = if (it.hasRemainingSpawns()) {
                    it.mealColor to it.timeUntil.format()
                } else {
                    "§c" to (HoppityApi.getEventEndMark()?.timeUntil()?.format() ?: "???")
                }
                add("§7 - ${it.formattedName}$color $timeFormat")
            }

            if (!unclaimedEggsConfig.showCollectedLocationCount || !SkyBlockUtils.inSkyBlock) return@buildList

            val totalEggs = HoppityEggLocations.islandLocations.size
            if (totalEggs > 0) {
                val collectedEggs = if (ApiUtils.isLegacyHoppityLocationCountingDisabled()) {
                    HoppityEggLocations.islandCollectedLocations.count { it in HoppityEggLocations.islandLocations }
                } else {
                    HoppityEggLocations.islandCollectedLocations.size.coerceAtMost(HoppityEggLocations.islandLocations.size)
                }
                val percentage = collectedEggs.toDouble() / totalEggs.toDouble()
                val collectedFormat = formatEggsCollected(percentage)
                add("§7Locations: $collectedFormat$collectedEggs§7/§a$totalEggs")
            }
        }.map { CFApi.partyModeReplace(it) }

        if (displayList.size == 1) return emptyList()

        val container = Renderable.vertical(displayList.map(StringRenderable::from))
        return listOf(
            if (unclaimedEggsConfig.warpClickEnabled) Renderable.clickable(
                container,
                tips = listOf("§eClick to ${"/warp ${unclaimedEggsConfig.warpClickDestination}".trim()}!"),
                onLeftClick = { HypixelCommands.warp(unclaimedEggsConfig.warpClickDestination) },
            ) else container,
        )
    }

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { HoppityEggsManager.isActive() },
            onRender = {
                unclaimedEggsConfig.position.renderRenderables(display, posLabel = "Hoppity Eggs")
            },
        )
    }

    private fun formatEggsCollected(collectedEggs: Double): String =
        when {
            collectedEggs < 0.3 -> "§c"
            collectedEggs < 0.6 -> "§6"
            collectedEggs < 0.9 -> "§e"
            else -> "§a"
        }
}
