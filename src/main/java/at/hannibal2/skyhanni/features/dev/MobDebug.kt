package at.hannibal2.skyhanni.features.dev

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.Mob
import at.hannibal2.skyhanni.data.MobData
import at.hannibal2.skyhanni.events.HypixelJoinEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.MobUtils
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MobDebug {

    private val mobDebugConfig get() = SkyHanniMod.feature.dev.mobDebug.mobDetection

    @SubscribeEvent
    fun onWorldRenderDebug(event: LorenzRenderWorldEvent) {
        if (mobDebugConfig.skyblockMobHighlight) {
            MobData.skyblockMobs.forEach {
                val color = if (it.mobType == Mob.Type.Boss) LorenzColor.DARK_GREEN else LorenzColor.GREEN
                event.drawFilledBoundingBox_nea(it.boundingBox.expandBlock(), color.toColor(), 0.3f)
            }
        }
        if (mobDebugConfig.displayNPCHighlight) {
            MobData.displayNPCs.forEach {
                event.drawFilledBoundingBox_nea(it.boundingBox.expandBlock(), LorenzColor.RED.toColor(), 0.3f)
            }
        }
        if (mobDebugConfig.realPlayerHighlight) {
            MobData.players.filterNot { it.baseEntity is EntityPlayerSP }.forEach {
                event.drawFilledBoundingBox_nea(it.boundingBox.expandBlock(), LorenzColor.BLUE.toColor(), 0.3f)
            }
        }
        if (mobDebugConfig.summonHighlight) {
            MobData.summoningMobs.forEach {
                event.drawFilledBoundingBox_nea(it.boundingBox.expandBlock(), LorenzColor.YELLOW.toColor(), 0.3f)
            }
        }
        if (mobDebugConfig.skyblockMobShowName) {
            MobData.skyblockMobs.forEach {
                event.drawString(
                    it.baseEntity.getLorenzVec().add(y = 2.5), "§5" + it.name
                )
            }
        }
        if (mobDebugConfig.displayNPCShowName) {
            MobData.displayNPCs.forEach {
                event.drawString(
                    it.baseEntity.getLorenzVec().add(y = 2.5), "§d" + it.name
                )
            }
        }
        if (mobDebugConfig.showRayHit) {
            MobUtils.rayTraceForSkyblockMob(Minecraft.getMinecraft().thePlayer, event.partialTicks)?.let {
                event.drawFilledBoundingBox_nea(it.boundingBox.expandBlock(), LorenzColor.GOLD.toColor(), 0.5f)
            }
        }
    }

    @SubscribeEvent
    fun onJoin(event: HypixelJoinEvent) {
        MobDevTracker.loadFromFile()
    }

    @SubscribeEvent
    fun onExit(event: IslandChangeEvent) {
        MobDevTracker.saveToFile()
        // counter.reset()
    }
}
