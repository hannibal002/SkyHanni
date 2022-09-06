package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.ResetEntityHurtEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import net.minecraft.client.Minecraft
import net.minecraft.entity.monster.EntitySilverfish
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class HighlightDungeonDeathmite {

    private var tick = 0
    private val deathmites = mutableListOf<EntitySilverfish>()

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return

        if (tick++ % 20 == 0) {
            find()
        }
    }

    @SubscribeEvent
    fun onRenderMobColored(event: RenderMobColoredEvent) {
        if (!isEnabled()) return
        val entity = event.entity

        if (entity in deathmites) {
            event.color = LorenzColor.DARK_RED.toColor().withAlpha(20)
        }
    }

    @SubscribeEvent
    fun onResetEntityHurtTime(event: ResetEntityHurtEvent) {
        if (!isEnabled()) return
        val entity = event.entity

        if (entity in deathmites) {
            event.shouldReset = true
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        deathmites.clear()
    }

    private fun find() {
        Minecraft.getMinecraft().theWorld.loadedEntityList.filterIsInstance<EntitySilverfish>()
            .filter { it !in deathmites && it.baseMaxHealth >= 1_000_000_000 }
            .forEach(deathmites::add)
    }

    private fun isEnabled(): Boolean {

        return LorenzUtils.inDungeons && SkyHanniMod.feature.dungeon.highlightDeathmites
    }
}