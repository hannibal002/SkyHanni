package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.decoration.ArmorStand
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Fixing the visitor detection problem with Anita and Jacob, as those two are on the garden twice when visiting.
 */
@SkyHanniModule
object NpcVisitorFix {
    private val staticVisitors = listOf("Jacob", "Anita")

    /**
     * REGEX-TEST: Changing Barn skin to Default!
     * REGEX-TEST: Changing Barn skin to Mansion Heights!
     */
    private val barnSkinChangePattern by RepoPattern.pattern(
        "garden.barn.skin.change.colorless",
        "Changing Barn skin to .*",
    )

    @HandleEvent(onlyOnIsland = GARDEN)
    private fun onInventoryOpen(event: InventoryOpenEvent) {
        val name = staticVisitors.firstOrNull { event.inventoryName.contains(it) } ?: return
        val nearest = findNametags(name).firstOrNull { it.distanceToPlayer() < 3 } ?: return
        DelayedRun.runDelayed(200.milliseconds) {
            saveStaticVisitor(name, nearest)
        }
    }

    private fun saveStaticVisitor(name: String, entity: ArmorStand) {
        if (lastVisitorOpen.passedSince() < 1.seconds) return

        val storage = GardenApi.storage ?: return

        val location = entity.getLorenzVec()
        storage.npcVisitorLocations[name]?.let {
            if (it.distance(location) < 1) return
        }

        storage.npcVisitorLocations[name] = location
        ChatUtils.chat("Saved $name NPC location. Real $name visitors are now getting detected correctly.")
    }

    private var lastVisitorOpen = SimpleTimeMark.farPast()

    @HandleEvent
    private fun onVisitorOpen() {
        lastVisitorOpen = SimpleTimeMark.now()
    }

    @HandleEvent
    private fun onSystemMessage(event: SystemMessageEvent.Allow) {
        if (barnSkinChangePattern.matches(event.cleanMessage)) {
            GardenApi.storage?.npcVisitorLocations?.clear()
        }
    }

    fun findNametag(visitorName: String): ArmorStand? {
        val nametags = findNametags(visitorName)
        if (nametags.isEmpty()) return null

        if (visitorName !in staticVisitors) {
            return nametags[0]
        }

        val staticLocation = GardenApi.storage?.npcVisitorLocations?.get(visitorName) ?: return null

        for (entity in nametags.toMutableList()) {
            val distance = entity.distanceTo(staticLocation)
            if (distance < 3) {
                nametags.remove(entity)
            }
        }

        return nametags.firstOrNull()
    }

    private fun findNametags(visitorName: String): MutableList<ArmorStand> {
        return EntityUtils.getEntitiesInBoundingBox<ArmorStand>(GardenApi.barnArea) {
            it.name.string.removeColor() == visitorName
        }.toMutableList()
    }
}
