package at.hannibal2.skyhanni.features.hunting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.entity.EntityRemovedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.getEntities
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesNearby
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToIgnoreY
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToCrosshair
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.animal.frog.Frog
import net.minecraft.world.entity.animal.frog.FrogVariants
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
object LotumHelper {

    private val config get() = SkyHanniMod.feature.hunting.lotumHelperConfig

    private const val LOTUM_NAME = "Lotum"
    private const val LOTUM_NAME_TAG_RANGE = 6.0
    private const val LOTUM_FROG_HORIZONTAL_RANGE = 1.5
    private val trackedLotums = mutableSetOf<ArmorStand>()
    private val highlightedLotums = mutableSetOf<Frog>()

    @HandleEvent(onlyOnIsland = IslandType.LOTUS_ATOLL)
    fun onEntityClick(event: EntityClickEvent) {
        if (!config.enabled) return
        trackedLotums += event.clickedEntity.findLotumNameTag() ?: return
    }

    @HandleEvent(onlyOnIsland = IslandType.LOTUS_ATOLL)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.enabled) {
            trackedLotums.clear()
            return
        }
        removeInvalidLotums()

        val closestLotum = trackedLotums.minByOrNull { it.distanceToPlayer() } ?: return

        event.drawLineToCrosshair(
            closestLotum.getLorenzVec(),
            LorenzColor.GREEN.toChromaColor(),
            3,
            false,
        )
    }

    @OptIn(AllEntitiesGetter::class)
    @HandleEvent(onlyOnIsland = IslandType.LOTUS_ATOLL)
    fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(5)) return
        removeInvalidLotums()
        removeInvalidHighlightedLotums()
        if (!config.highlightLotums) return

        getEntities<ArmorStand>()
            .filter { it.isLotumName() }
            .mapNotNull { it.findLotumFrog() }
            .forEach(::highlightLotum)
    }

    private fun highlightLotum(lotum: Frog) {
        if (!highlightedLotums.add(lotum)) return
        RenderLivingEntityHelper.setEntityColor(
            lotum,
            LorenzColor.GREEN.toColor().addAlpha(80),
        ) { config.highlightLotums && IslandType.LOTUS_ATOLL.isInIsland() && lotum.isAlive }
    }

    private fun removeInvalidLotums() = trackedLotums.removeIf { !it.isAlive || !it.isLotumName() }

    private fun removeInvalidHighlightedLotums() {
        val iterator = highlightedLotums.iterator()
        while (iterator.hasNext()) {
            val lotum = iterator.next()
            if (!lotum.isConfirmedLotum()) {
                iterator.remove()
                RenderLivingEntityHelper.removeEntityColor(lotum)
            }
        }
    }

    private fun removeHighlightedLotum(lotum: Frog) {
        if (highlightedLotums.remove(lotum)) RenderLivingEntityHelper.removeEntityColor(lotum)
    }

    private fun Entity.findLotumNameTag(): ArmorStand? =
        (this as? ArmorStand)?.takeIf { it.isLotumName() } ?: nearbyLotumNameTag()

    private fun ArmorStand.findLotumFrog(): Frog? {
        val nameTagLocation = getLorenzVec()
        return nameTagLocation.getEntitiesNearby<Frog>(LOTUM_NAME_TAG_RANGE) {
            it.variant.`is`(FrogVariants.TEMPERATE) &&
                it.getLorenzVec().y < nameTagLocation.y &&
                it.distanceToIgnoreY(nameTagLocation) < LOTUM_FROG_HORIZONTAL_RANGE
        }.minByOrNull { it.distanceTo(this) }
    }

    private fun Frog.isConfirmedLotum(): Boolean =
        isAlive && nearbyLotumNameTag()?.findLotumFrog() == this

    private fun Entity.nearbyLotumNameTag(): ArmorStand? =
        getLorenzVec().getEntitiesNearby<ArmorStand>(LOTUM_NAME_TAG_RANGE)
            .filter { it.isLotumName() }
            .minByOrNull { it.distanceTo(this) }

    private fun Entity.isLotumName() = cleanName().contains(LOTUM_NAME)

    @HandleEvent(onlyOnIsland = IslandType.LOTUS_ATOLL)
    fun onEntityRemoved(event: EntityRemovedEvent<Entity>) {
        trackedLotums.remove(event.entity)
        (event.entity as? Frog)?.let(::removeHighlightedLotum)
    }

    @HandleEvent(WorldChangeEvent::class)
    fun onWorldChange() {
        trackedLotums.clear()
        highlightedLotums.clear()
    }
}
