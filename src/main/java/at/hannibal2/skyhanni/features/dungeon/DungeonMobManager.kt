package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.exactPlayerEyeLocation
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

class DungeonMobManager {

    private val config get() = SkyHanniMod.feature.dungeon.objectHighlighter
    private val stared get() = config.stared
    private val fel get() = config.fel

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        onToggle(
            stared.highlight,
            stared.colour,
            //stared.showOutline
        ) {
            val color = if (stared.highlight.get()) null else stared.colour.get().toChromaColor()
            MobData.skyblockMobs.filter { it.hasStar }.forEach {
                handleStar0(it, color)
            }
            if (!stared.highlight.get()) {
                staredInvisible.clear()
            }
        }
        onToggle(
            fel.highlight,
            fel.colour
        ) {
            if (fel.highlight.get()) {
                if (felOnTheGround.isEmpty()) {
                    MobData.skyblockMobs.forEach(::handleFel)
                }
            } else {
                felOnTheGround.clear()
            }
        }
    }

    @SubscribeEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (event.mob.mobType != Mob.Type.DUNGEON) return
        handleStar(event.mob)
        handleFel(event.mob)
    }

    @SubscribeEvent
    fun onMobDeSpawn(event: MobEvent.DeSpawn.SkyblockMob) {
        if (event.mob.mobType != Mob.Type.DUNGEON) return
        if (stared.highlight.get()) {
            staredInvisible.remove(event.mob)
        }
        handleFelDespawn(event.mob)
    }

    @SubscribeEvent
    fun onLorenzTick(event: LorenzTickEvent) {
        if (!IslandType.CATACOMBS.isInIsland()) return
        handleInvisibleStar()
    }

    private val staredInvisible = mutableSetOf<Mob>()

    private fun handleStar(mob: Mob) {
        if (!stared.highlight.get()) return
        if (!mob.hasStar) return
        handleStar0(mob, stared.colour.get().toChromaColor())
    }

    private fun handleInvisibleStar() {
        if (!stared.highlight.get()) return
        if (staredInvisible.isEmpty()) return
        staredInvisible.removeIf {
            val visible = !it.isInvisible()
            if (visible) {
                it.highlight(stared.colour.get().toChromaColor())
            }
            visible
        }
    }

    private fun handleStar0(mob: Mob, colour: Color?) {
        if (mob.isInvisible()) {
            staredInvisible.add(mob)
            return
        }
        mob.highlight(colour)
    }

    private val felOnTheGround = mutableSetOf<Mob>()

    private fun handleFel(mob: Mob) {
        if (!fel.highlight.get()) return
        if (mob.name != "Fels") return
        if (!mob.isInvisible()) return
        felOnTheGround.add(mob)
    }

    @SubscribeEvent
    fun onLorenzRenderWorld(event: LorenzRenderWorldEvent) {
        if (!fel.highlight.get()) return
        if (fel.line) {
            felOnTheGround.filter { it.canBeSeen() }.forEach {
                event.draw3DLine(
                    it.baseEntity.getLorenzVec().add(y = 0.15),
                    event.exactPlayerEyeLocation(),
                    fel.colour.get().toChromaColor(),
                    3,
                    true
                )
            }
        }

        felOnTheGround.removeIf { mob ->
            event.drawWaypointFilled(
                mob.baseEntity.getLorenzVec().add(-0.5, -0.23, -0.5),
                fel.colour.get().toChromaColor(),
                false,
                false,
                extraSize = -0.2,
                minimumAlpha = 0.8f,
                inverseAlphaScale = true
            )
            !mob.isInvisible()
        }
    }

    private fun handleFelDespawn(mob: Mob) {
        if (!fel.highlight.get()) return
        felOnTheGround.remove(mob)
    }
}
