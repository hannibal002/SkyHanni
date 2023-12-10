package at.hannibal2.skyhanni.data.mob

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.dev.DebugMob.HowToShow
import at.hannibal2.skyhanni.events.HypixelJoinEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.utils.LocationUtils.getTopCenter
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.MobUtils
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MobDebug {

    private val mobDebugConfig get() = SkyHanniMod.feature.dev.mobDebug.mobDetection

    private fun HowToShow.isHighlight() =
        this == HowToShow.ONLY_HIGHLIGHT || this == HowToShow.NAME_AND_HIGHLIGHT

    private fun HowToShow.isName() =
        this == HowToShow.ONLY_NAME || this == HowToShow.NAME_AND_HIGHLIGHT

    private fun MobData.MobSet.highlight(event: LorenzRenderWorldEvent, color: (Mob) -> (LorenzColor)) = this.forEach {
        event.drawFilledBoundingBox_nea(it.boundingBox.expandBlock(), color.invoke(it).toColor(), 0.3f)
    }

    private fun MobData.MobSet.showName(event: LorenzRenderWorldEvent) =
        this.filter { it.canBeSeen() }.map { it.boundingBox.getTopCenter() to it.name }.forEach {
            event.drawString(
                it.first.add(y = 0.5), "§5" + it.second, seeThroughBlocks = true
            )
        }

    @SubscribeEvent
    fun onWorldRenderDebug(event: LorenzRenderWorldEvent) {
        if (mobDebugConfig.skyblockMob.isHighlight()) {
            MobData.skyblockMobs.highlight(event) { if (it.mobType == Mob.Type.Boss) LorenzColor.DARK_GREEN else LorenzColor.GREEN }
        }
        if (mobDebugConfig.displayNPC.isHighlight()) {
            MobData.displayNPCs.highlight(event) { LorenzColor.RED }
        }
        if (mobDebugConfig.realPlayerHighlight) {
            MobData.players.highlight(event) { if (it.baseEntity is EntityPlayerSP) LorenzColor.CHROMA else LorenzColor.BLUE }
        }
        if (mobDebugConfig.summon.isHighlight()) {
            MobData.summoningMobs.highlight(event) { LorenzColor.YELLOW }
        }
        if (mobDebugConfig.special.isHighlight()) {
            MobData.special.highlight(event) { LorenzColor.AQUA }
        }
        if (mobDebugConfig.skyblockMob.isName()) {
            MobData.skyblockMobs.showName(event)
        }
        if (mobDebugConfig.displayNPC.isName()) {
            MobData.displayNPCs.showName(event)
        }
        if (mobDebugConfig.summon.isName()) {
            MobData.summoningMobs.showName(event)
        }
        if (mobDebugConfig.special.isName()) {
            MobData.special.showName(event)
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
