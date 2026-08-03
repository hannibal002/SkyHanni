package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureFishEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.fishing.SeaCreatureDetectionApi.seaCreature
import at.hannibal2.skyhanni.features.fishing.seaCreatureXMLGui.SeaCreatureSettings
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import at.hannibal2.skyhanni.utils.compat.EntityCompat.findHealthReal
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import java.awt.Color
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SeaCreatureFeatures {

    private val config get() = SkyHanniMod.feature.fishing.rareCatches
    private val entityIds = TimeLimitedSet<Int>(6.minutes)
    private var display: Renderable? = null
    private var displayStopRender = SimpleTimeMark.farPast()

    @HandleEvent
    private fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!isEnabled()) return
        val mob = event.mob

        if (!config.highlight) return

        if (SeaCreatureSettings.getConfig(mob)?.shouldHighlight == true) mob.highlight(LorenzColor.GREEN.toChromaColor())
    }

    @HandleEvent
    private fun onSkyblockMobFirstSeen(event: MobEvent.FirstSeen.SkyblockMob) {
        if (!isEnabled()) return
        val mob = event.mob
        val seaCreature = mob.seaCreature ?: return
        val entity = mob.baseEntity
        val shouldNotify = entity.id !in entityIds
        entityIds.addIfAbsent(entity.id)
        if (seaCreature.isOwn) return

        if (mob.name == "Water Hydra" && entity.findHealthReal() == (entity.baseMaxHealth.toFloat() / 2)) return
        if (config.alertOtherCatches && shouldNotify && SeaCreatureSettings.getConfig(mob)?.shouldNotifyForNonOwn == true) {
            val text = componentBuilder {
                if (config.creatureName) {
                    append(seaCreature.displayName)
                    append(" NEARBY!")
                } else {
                    append(seaCreature.rarity.chatColorCode)
                    append("RARE SEA CREATURE!")
                }
            }
            sendTitle(text, duration = 1.5.seconds)
            if (config.playSound) SoundUtils.playBeepSound()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onSeaCreatureFish(event: SeaCreatureFishEvent) {
        val fishedSCSettings = SeaCreatureSettings.getConfig(event.seaCreature) ?: return
        if (config.alertOwnCatches && fishedSCSettings.shouldSelfNotifyOnCatch == true) {
            val text = componentBuilder {
                if (config.creatureName) {
                    append(event.seaCreature.displayName)
                    append("!")
                } else {
                    append(event.seaCreature.rarity.chatColorCode)
                    append("RARE CATCH!")
                }
            }
            sendTitle(text)
            if (config.playSound) SoundUtils.playBeepSound()
        }
        if (config.announceRareInParty && PartyApi.isInParty() && fishedSCSettings.shouldShareInChat == true) {
            val name = event.seaCreature.name
            val message = buildString {
                if (event.doubleHook) append("DOUBLE HOOK: ")
                append("I caught ${StringUtils.optionalAn(name)} $name!")
            }
            HypixelCommands.partyChat(message)
        }
    }

    @HandleEvent
    private fun onWorldChange() {
        entityIds.clear()
        display = null
    }

    @HandleEvent
    private fun onRenderEntityOutlines(event: RenderEntityOutlineEvent) {
        if (isEnabled() && config.highlight && event.type === RenderEntityOutlineEvent.Type.NO_XRAY) {
            event.queueEntitiesToOutline(getEntityOutlineColor)
        }
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "fishing.rareSeaCreatureHighlight", "fishing.rareCatches.highlight")
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (displayStopRender.isInPast()) {
            display = null
            return
        }
        val display = display ?: return
        config.position.renderRenderable(display, posLabel = "Rare Sea Creature Catch")
    }

    fun sendTitle(
        titleText: Component,
        duration: Duration = 5.seconds,
    ) {
        display = Renderable.text(titleText, scale = 2.0)
        displayStopRender = SimpleTimeMark.now() + duration
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && !DungeonApi.inDungeon() && !KuudraApi.inKuudra

    private val getEntityOutlineColor: (entity: Entity) -> Color? = { entity ->
        (entity as? LivingEntity)?.mob?.let { mob ->
            if (SeaCreatureSettings.getConfig(mob)?.shouldHighlight == true && entity.distanceToPlayer() < 30) {
                LorenzColor.GREEN.toColor()
            } else null
        }
    }
}
