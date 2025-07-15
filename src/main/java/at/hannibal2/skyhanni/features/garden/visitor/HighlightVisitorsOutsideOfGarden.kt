package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.visitor.VisitorConfig.VisitorBlockBehaviour
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenVisitor
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.getSkinTexture
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C02PacketUseEntity.Action

@SkyHanniModule
object HighlightVisitorsOutsideOfGarden {

    private var visitorJson = mapOf<String?, List<GardenVisitor>>()

    private val config get() = VisitorApi.config

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        visitorJson = event.getConstant<GardenJson>(
            "Garden", GardenJson::class.java,
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
        val island = SkyBlockUtils.currentIsland.islandData?.apiName ?: return false
        val possibleJsons = visitorJson[island] ?: return false
        val skinOrType = getSkinOrTypeFor(entity)
        return possibleJsons.any {
            (it.position == null || it.position.distance(entity.position.toLorenzVec()) < 1) &&
                it.skinOrType == skinOrType
        }
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!config.highlightVisitors) return
        val color = LorenzColor.DARK_RED.toColor().addAlpha(50)
        EntityUtils.getEntities<EntityLivingBase>()
            .filter { it !is EntityArmorStand && isVisitor(it) }
            .forEach {
                RenderLivingEntityHelper.setEntityColor(it, color) { config.highlightVisitors }
            }
    }

    private val shouldBlock
        get() = when (config.blockInteracting) {
            VisitorBlockBehaviour.DONT -> false
            VisitorBlockBehaviour.ALWAYS -> true
            VisitorBlockBehaviour.ONLY_ON_BINGO -> SkyBlockUtils.isBingoProfile
        }

    private fun isVisitorNearby(location: LorenzVec) =
        EntityUtils.getEntitiesNearby<EntityLivingBase>(location, 2.0).any { isVisitor(it) }

    @HandleEvent(onlyOnSkyblock = true)
    fun onClickEntity(event: EntityClickEvent) {
        if (!shouldBlock) return
        if (MinecraftCompat.localPlayer.isSneaking) return
        val entity = event.clickedEntity
        if (isVisitor(entity) || (entity is EntityArmorStand && isVisitorNearby(entity.getLorenzVec()))) {
            if (event.action != Action.INTERACT_AT) {
                ChatUtils.chatAndOpenConfig(
                    "Blocked you from interacting with a visitor. Sneak to bypass or click here to change settings.",
                    VisitorApi.config::blockInteracting,
                )
            }
            event.cancel()
        }
    }
}
