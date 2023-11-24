package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.garden.visitor.VisitorConfig.VisitorBlockBehaviour
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.getSkinTexture
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.jsonobjects.GardenJson
import at.hannibal2.skyhanni.utils.toLorenzVec
import io.github.moulberry.notenoughupdates.util.SBInfo
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HighlightVisitorsOutsideOfGarden {
    private var visitorJson = mapOf<String?, List<GardenJson.GardenVisitor>>()

    private val config get() = SkyHanniMod.feature.garden.visitors

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        visitorJson = event.getConstant<GardenJson>(
            "Garden", GardenJson::class.java
        ).visitors.values.groupBy {
            it.mode
        }
        for (list in visitorJson.values) {
            for (visitor in list) {
                visitor.skinOrType = visitor.skinOrType?.replace("\\n", "")?.replace("\n", "")
            }
        }
    }

    private fun getSkinOrTypeFor(entity: Entity): String {
        if (entity is EntityPlayer) {
            return entity.getSkinTexture() ?: "no skin"
        }
        return entity.javaClass.simpleName
    }

    private fun isVisitor(entity: Entity): Boolean {
        val mode = SBInfo.getInstance().getLocation()
        val possibleJsons = visitorJson[mode] ?: return false
        val skinOrType = getSkinOrTypeFor(entity)
        return possibleJsons.any {
            (it.position == null || it.position!!.distance(entity.position.toLorenzVec()) < 1)
                && it.skinOrType == skinOrType
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!config.highlightVisitors) return
        if (!event.repeatSeconds(1)) return
        EntityUtils.getEntities<EntityLivingBase>()
            .filter { it !is EntityArmorStand && isVisitor(it) }
            .forEach {
                RenderLivingEntityHelper.setEntityColor(
                    it,
                    LorenzColor.DARK_RED.toColor().withAlpha(50)
                ) { config.highlightVisitors }
            }
    }

    private val shouldBlock
        get() = when (config.blockInteracting) {
            VisitorBlockBehaviour.DONT -> false
            VisitorBlockBehaviour.ALWAYS -> true
            VisitorBlockBehaviour.ONLY_ON_BINGO -> LorenzUtils.isBingoProfile
            null -> false
        }

    private fun isVisitorNearby(location: LorenzVec) =
        EntityUtils.getEntitiesNearby<EntityLivingBase>(location, 2.0).any { isVisitor(it) }

    @SubscribeEvent
    fun onClickEntity(event: PacketEvent.SendEvent) {
        if (!shouldBlock) return
        val world = Minecraft.getMinecraft().theWorld ?: return
        val player = Minecraft.getMinecraft().thePlayer ?: return
        if (player.isSneaking) return
        val packet = event.packet as? C02PacketUseEntity ?: return
        val entity = packet.getEntityFromWorld(world) ?: return
        if (isVisitor(entity) || (entity is EntityArmorStand && isVisitorNearby(entity.getLorenzVec()))) {
            event.isCanceled = true
            if (packet.action == C02PacketUseEntity.Action.INTERACT) {
                LorenzUtils.clickableChat(
                    "Blocked you from interacting with a visitor. Sneak to bypass or click here to change settings.",
                    "/sh block interacting with visitors"
                )
            }
        }
    }
}
