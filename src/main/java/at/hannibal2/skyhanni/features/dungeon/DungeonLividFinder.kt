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
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.RecalculatingValue
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.RenderUtils.exactBoundingBox
import at.hannibal2.skyhanni.utils.RenderUtils.exactLocation
import at.hannibal2.skyhanni.utils.RenderUtils.exactPlayerEyeLocation
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.block.BlockStainedGlass
import net.minecraft.client.Minecraft
import net.minecraft.potion.Potion
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object DungeonLividFinder {

    private val config get() = SkyHanniMod.feature.dungeon.lividFinder
    private val blockLocation = LorenzVec(6, 109, 43)
    private val repoGroup = RepoPattern.group("dungeon.livid")

    /**
     * REGEX-TEST: §e﴾ §c§lLivid§r§r §a7M§c❤ §e﴿
     */
    private val lividColor by repoGroup.pattern(
        "color",
        "§e﴾ §(?<color>.).+",
    )

    private val isBlind = RecalculatingValue(100.milliseconds, ::isCurrentlyBlind)

    var livid: Mob? = null
    private var fakeLivids = mutableSetOf<Mob>()
    private var color: LorenzColor? = null

    @SubscribeEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!inLividBossRoom()) return
        val mob = event.mob
        if (mob.name != "Livid" && mob.name != "Real Livid") return

        val lividColor = mob.getLividColor() ?: return

        if (lividColor == color) {
            livid = mob
            if (config.enabled) mob.highlight(lividColor.toColor())
        } else fakeLivids += mob
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!inLividBossRoom() || color != null) return
        color = blockLocation.getBlockStateAt().getValue(BlockStainedGlass.COLOR).toLorenzColor()
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
    }

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!inLividBossRoom()) return
        if (!config.hideWrong) return
        if (event.entity.mob in fakeLivids) event.cancel()
    }

    private fun isCurrentlyBlind() = Minecraft.getMinecraft().thePlayer.getActivePotionEffect(Potion.blindness).duration > 10

    private fun Mob.getLividColor(): LorenzColor? = lividColor.matchMatcher(baseEntity.name) {
        group("color")
    }?.firstOrNull()?.toLorenzColor()

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!inLividBossRoom()) return
        if (!config.enabled) return
        if (isBlind.getValue()) return

        val entity = livid?.baseEntity ?: return
        val location = event.exactLocation(entity)
        val boundingBox = event.exactBoundingBox(entity)
        val lorenzColor = color ?: return

        event.drawDynamicText(location, lorenzColor.getChatColor() + "Livid", 1.5)

        if (location.distanceSqToPlayer() < 50) return

        val color = lorenzColor.toColor()
        event.draw3DLine(event.exactPlayerEyeLocation(), location.add(0.5, 0.0, 0.5), color, 3, true)
        event.drawFilledBoundingBox_nea(boundingBox, color, 0.5f)
    }

    private fun inLividBossRoom() = DungeonAPI.inBossRoom && DungeonAPI.getCurrentBoss() == DungeonFloor.F5
}
