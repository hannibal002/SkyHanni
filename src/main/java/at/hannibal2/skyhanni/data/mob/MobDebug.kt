package at.hannibal2.skyhanni.data.mob

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.dev.DebugMobConfig.HowToShow
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.CopyNearbyEntitiesCommand.getMobInfo
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils.getTopCenter
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.MobUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.client.player.LocalPlayer

@SkyHanniModule
object MobDebug {
    private val config get() = SkyHanniMod.feature.dev.mobDebug.mobDetection

    private var lastRayHit: Mob? = null

    private fun HowToShow.isHighlight() =
        equalsOneOf(HowToShow.ONLY_HIGHLIGHT, HowToShow.NAME_AND_HIGHLIGHT)

    private fun HowToShow.isName() =
        equalsOneOf(HowToShow.ONLY_NAME, HowToShow.NAME_AND_HIGHLIGHT)

    private fun Mob.shouldShow(ignoreRayHit: Boolean = false): Boolean {
        if (!isFullyInvisible()) return true
        if (!PlatformUtils.isDevEnvironment) return false
        return ignoreRayHit || this == lastRayHit
    }

    private fun MobData.MobSet.highlight(
        event: SkyHanniRenderWorldEvent,
        color: (Mob) -> (ChromaColour),
    ) {
        filter { it.shouldShow() }.forEach { mob ->
            event.drawFilledBoundingBox(mob.boundingBox.expandBlock(), color.invoke(mob), 0.3f)
        }
    }

    private fun MobData.MobSet.showName(event: SkyHanniRenderWorldEvent) {
        filter { it.shouldShow() && it.canBeSeen() }
            .map { it.boundingBox.getTopCenter() to it.name }
            .forEach { (location, text) ->
                event.drawString(location.up(0.5), "§5$text", seeThroughBlocks = true)
            }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (config.showRayHit || config.showInvisible) {
            lastRayHit = MobUtils.raycastForMobs(MinecraftCompat.localPlayerOrThrow, event.partialTicks)
                ?.firstOrNull { it.shouldShow(ignoreRayHit = true) }
        }

        if (config.skyblockMob.isHighlight()) {
            MobData.skyblockMobs.highlight(event) {
                (if (it.category == MobCategory.BOSS) LorenzColor.DARK_GREEN else LorenzColor.GREEN)
                    .toChromaColor()
            }
        }
        if (config.displayNPC.isHighlight()) {
            MobData.displayNpcs.highlight(event) { LorenzColor.RED.toChromaColor() }
        }
        if (config.realPlayerHighlight) {
            MobData.players.highlight(event) {
                (if (it.baseEntity is LocalPlayer) LorenzColor.CHROMA else LorenzColor.BLUE)
                    .toChromaColor()
            }
        }
        if (config.summon.isHighlight()) {
            MobData.summoningMobs.highlight(event) { LorenzColor.YELLOW.toChromaColor() }
        }
        if (config.special.isHighlight()) {
            MobData.special.highlight(event) { LorenzColor.AQUA.toChromaColor() }
        }
        if (config.skyblockMob.isName()) {
            MobData.skyblockMobs.showName(event)
        }
        if (config.displayNPC.isName()) {
            MobData.displayNpcs.showName(event)
        }
        if (config.summon.isName()) {
            MobData.summoningMobs.showName(event)
        }
        if (config.special.isName()) {
            MobData.special.showName(event)
        }
        if (config.showRayHit) {
            lastRayHit?.let {
                event.drawFilledBoundingBox(
                    it.boundingBox.expandBlock(),
                    LorenzColor.GOLD.toChromaColor(),
                    0.5f,
                )
            }
        }
    }

    @HandleEvent
    fun onMobEvent(event: MobEvent) {
        if (!config.logEvents) return
        val text = "Mob ${if (event is MobEvent.Spawn) "Spawn" else "Despawn"}: ${
            getMobInfo(event.mob).joinToString(", ")
        }"
        MobData.logger.log(text)
        LorenzDebug.log(text)
        ChatUtils.debug(text)
    }
}
