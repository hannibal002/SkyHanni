package at.hannibal2.skyhanni.features.summonings

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object SummoningMobManager {

    private val config get() = SkyHanniMod.feature.combat.summonings
    private var mobs = mutableSetOf<Mob>()

    @SubscribeEvent
    fun onMobSpawn(event: MobEvent.Spawn.Summon) {
        if (event.mob.owner?.ownerName != LorenzUtils.getPlayerName()) return

        mobs += event.mob
        if (config.summoningMobColored) event.mob.highlight(LorenzColor.GREEN.toColor())
    }

    @SubscribeEvent
    fun onMobDeSpawn(event: MobEvent.DeSpawn.Summon) {
        mobs -= event.mob
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLiving(event: SkyHanniRenderEntityEvent.Specials.Pre<EntityArmorStand>) {
        if (!LorenzUtils.inSkyBlock || !config.summoningMobHideNametag) return
        if (event.entity.mob !in mobs) return
        event.cancel()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock || !config.summoningMobDisplay) return
        if (mobs.isEmpty()) return

        val list = buildList {
            add("Summoning mobs: " + mobs.size)
            mobs.onEachIndexed { index, mob ->
                add("#${index + 1} §6${mob.name} §6${mob.baseEntity.health}/${mob.baseEntity.maxHealth}§c❤")
            }
        }.map { Renderable.string(it) }

        val renderable = Renderable.verticalContainer(list)
        config.summoningMobDisplayPos.renderRenderable(renderable, posLabel = "Summoning Mob Display")
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "summonings", "combat.summonings")
    }
}
