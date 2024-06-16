package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.RecalculatingValue
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.RenderUtils.exactBoundingBox
import at.hannibal2.skyhanni.utils.RenderUtils.exactLocation
import at.hannibal2.skyhanni.utils.RenderUtils.exactPlayerEyeLocation
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import net.minecraft.block.BlockStainedGlass
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.init.Blocks
import net.minecraft.potion.Potion
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object DungeonLividFinder {

    private val config get() = SkyHanniMod.feature.dungeon.lividFinder
    private val blockLocation = LorenzVec(6, 109, 43)

    private val isBlind = RecalculatingValue(2.ticks, ::isCurrentlyBlind)

    var livid: Mob? = null
    var lividArmorStandId: Int? = null
    private var fakeLivids = mutableSetOf<Mob>()

    private var color: LorenzColor? = null

    @SubscribeEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!inLividBossRoom()) return
        val mob = event.mob
        if (mob.name != "Livid" && mob.name != "Real Livid") return
        if (mob.baseEntity !is EntityOtherPlayerMP) return

        val lividColor = color ?: return
        if (mob.isLividColor(lividColor)) {
            livid = mob
            // When the real livid dies at the same time as a fake livid, Hypixel despawns the player entity,
            // and makes it impossible to get the mob of the real livid again.
            lividArmorStandId = mob.armorStand?.entityId
            if (config.enabled) mob.highlight(lividColor.toColor())
        } else {
            fakeLivids += mob
        }
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!inLividBossRoom()) return
        val block = blockLocation.getBlockStateAt()
        if (block.block != Blocks.wool) return
        color = block.getValue(BlockStainedGlass.COLOR).toLorenzColor()
    }

    @SubscribeEvent
    fun onMobDeSpawn(event: MobEvent.DeSpawn.SkyblockMob) {
        when (event.mob) {
            livid -> livid = null
            in fakeLivids -> fakeLivids -= event.mob
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        color = null
        lividArmorStandId = null
    }

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!inLividBossRoom() || !config.hideWrong) return
        if (livid == null && lividArmorStandId == null) return // in case livid detection fails, don't hide anything
        if (event.entity.mob in fakeLivids) event.cancel()
    }

    private fun isCurrentlyBlind() =
        Minecraft.getMinecraft().thePlayer?.getActivePotionEffect(Potion.blindness)?.duration?.let { it > 10 } ?: false

    private fun Mob.isLividColor(color: LorenzColor): Boolean {
        val chatColor = color.getChatColor()
        return armorStand?.name?.startsWith("${chatColor}﴾ ${chatColor}§lLivid") ?: false
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!inLividBossRoom() || !config.enabled) return
        if (isBlind.getValue()) return

        val entity = livid?.baseEntity ?: lividArmorStandId?.let { EntityUtils.getEntityByID(it) } ?: return
        val lorenzColor = color ?: return

        val location = event.exactLocation(entity)
        val boundingBox = event.exactBoundingBox(entity)

        event.drawDynamicText(location, lorenzColor.getChatColor() + "Livid", 1.5)

        if (location.distanceSqToPlayer() < 50) return

        val color = lorenzColor.toColor()
        event.draw3DLine(event.exactPlayerEyeLocation(), location.add(0.5, 0.0, 0.5), color, 3, true)
        event.drawFilledBoundingBox_nea(boundingBox, color, 0.5f)
    }

    private fun inLividBossRoom() = DungeonAPI.inBossRoom && DungeonAPI.getCurrentBoss() == DungeonFloor.F5
}
