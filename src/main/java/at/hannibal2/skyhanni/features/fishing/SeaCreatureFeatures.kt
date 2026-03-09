package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.title.TitleManager
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
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import at.hannibal2.skyhanni.utils.compat.findHealthReal
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import java.awt.Color
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SeaCreatureFeatures {

    private val config get() = SkyHanniMod.feature.fishing.rareCatches
    private val entityIds = TimeLimitedSet<Int>(6.minutes)

    @HandleEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!isEnabled()) return
        val mob = event.mob

        if (!config.highlight) return

        if (SeaCreatureSettings.getConfig(mob)?.shouldHighlight == true) mob.highlight(LorenzColor.GREEN.toChromaColor())
    }

    @HandleEvent
    fun onSkyblockMobFirstSeen(event: MobEvent.FirstSeen.SkyblockMob) {
        if (!isEnabled()) return
        val mob = event.mob
        val seaCreature = mob.seaCreature ?: return
        val entity = mob.baseEntity
        val shouldNotify = entity.id !in entityIds
        entityIds.addIfAbsent(entity.id)
        if (seaCreature.isOwn) return

        if (mob.name == "Water Hydra" && entity.findHealthReal() == (entity.baseMaxHealth.toFloat() / 2)) return
        if (config.alertOtherCatches && shouldNotify && SeaCreatureSettings.getConfig(mob)?.shouldNotifyForNonOwn == true) {
            val text = if (config.creatureName) "${seaCreature.displayName} NEARBY!"
            else "${seaCreature.rarity.chatColorCode}RARE SEA CREATURE!"
            TitleManager.sendTitle(text, duration = 1.5.seconds)
            if (config.playSound) SoundUtils.playBeepSound()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSeaCreatureFish(event: SeaCreatureFishEvent) {
        val fishedSCSettings = SeaCreatureSettings.getConfig(event.seaCreature) ?: return
        if (config.alertOwnCatches && fishedSCSettings.shouldSelfNotifyOnCatch == true) {
            val text = if (config.creatureName) "${event.seaCreature.displayName}!"
            else "${event.seaCreature.rarity.chatColorCode}RARE CATCH!"
            TitleManager.sendTitle(text)
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
    fun onWorldChange() {
        entityIds.clear()
    }

    @HandleEvent
    fun onRenderEntityOutlines(event: RenderEntityOutlineEvent) {
        if (isEnabled() && config.highlight && event.type === RenderEntityOutlineEvent.Type.NO_XRAY) {
            event.queueEntitiesToOutline(getEntityOutlineColor)
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "fishing.rareSeaCreatureHighlight", "fishing.rareCatches.highlight")
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
