package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.EntityMovementData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.RenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.SpecialColor.toSpecialColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToEye
import net.minecraft.entity.EntityLivingBase
import java.awt.Color

@SkyHanniModule
object DungeonMobManager {

    private val config get() = SkyHanniMod.feature.dungeon.objectHighlighter
    private val starredConfig get() = config.starred
    private val fel get() = config.fel

    private val staredInvisible = mutableSetOf<Mob>()
    val starredVisibleMobs = mutableSetOf<Mob>()
    private val felOnTheGround = mutableSetOf<Mob>()
    private val felMoving = mutableSetOf<Mob>()

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        onToggle(
            starredConfig.highlight,
            starredConfig.colour,
        ) {
            val color = if (starredConfig.highlight.get()) getStarColor() else null
            MobData.skyblockMobs.filter { it.hasStar }.forEach {
                handleStar0(it, color)
            }
            if (!starredConfig.highlight.get()) {
                staredInvisible.clear()
            }
        }
    }

    @HandleEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (event.mob.mobType != Mob.Type.DUNGEON) return
        handleStar(event.mob)
        handleFel(event.mob)
    }

    @HandleEvent
    fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        if (event.mob.mobType != Mob.Type.DUNGEON) return
        if (starredConfig.highlight.get()) {
            staredInvisible.remove(event.mob)
        }
        handleFelDespawn(event.mob)
        starredVisibleMobs.remove(event.mob)
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onEntityMove(event: EntityMoveEvent<EntityLivingBase>) {
        val mob = event.entity.mob ?: return
        if (felOnTheGround.remove(mob)) {
            felMoving.add(mob)
            if (mob.hasStar && starredConfig.highlight.get()) {
                mob.highlight(getStarColor())
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onTick() {
        handleInvisibleStar()
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (fel.line) {
            val color = getFelColor()
            felOnTheGround.filter { it.canBeSeen(30) }.forEach {
                event.drawLineToEye(
                    it.baseEntity.getLorenzVec().add(y = 0.15),
                    color,
                    3,
                    true,
                )
            }
        }

        val color = when {
            fel.highlight.get() -> getFelColor()
            starredConfig.highlight.get() -> getStarColor()
            else -> return
        }

        felOnTheGround.forEach { mob ->
            event.drawWaypointFilled(
                mob.baseEntity.getLorenzVec().add(-0.5, -0.23, -0.5),
                color,
                seeThroughBlocks = false,
                beacon = false,
                extraSize = -0.2,
                minimumAlpha = 0.8f,
                inverseAlphaScale = true,
            )
        }
    }

    private fun getFelColor() = fel.colour.get().toSpecialColor()

    private fun handleStar(mob: Mob) {
        if (!starredConfig.highlight.get()) return
        if (!mob.hasStar) return
        handleStar0(mob, getStarColor())
    }

    private fun handleInvisibleStar() {
        if (!starredConfig.highlight.get()) return
        staredInvisible.removeIf {
            val visible = !it.isInvisible()
            if (visible) {
                it.highlight(getStarColor())
            }
            visible
        }
    }

    private fun getStarColor(): Color = starredConfig.colour.get().toSpecialColor()

    private fun handleStar0(mob: Mob, colour: Color?) {
        if (mob.name == "Fels") {
            if (mob in felMoving) {
                mob.highlight(colour)
            }
            return
        }
        if (mob.isInvisible()) {
            staredInvisible.add(mob)
            return
        }
        mob.highlight(colour)
        starredVisibleMobs.add(mob)
    }

    private fun handleFel(mob: Mob) {
        if (mob.name != "Fels") return
        if (mob in felMoving) return
        felOnTheGround.add(mob)
        EntityMovementData.addToTrack(mob)
    }

    private fun handleFelDespawn(mob: Mob) {
        felOnTheGround.remove(mob)
        felMoving.remove(mob)
    }
}
