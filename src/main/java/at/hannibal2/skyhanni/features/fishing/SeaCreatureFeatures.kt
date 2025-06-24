package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureFishEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.TimeLimitedSet
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.monster.EntitySlime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SeaCreatureFeatures {

    private val config get() = SkyHanniMod.feature.fishing.rareCatches
    private var lastRareCatch = SimpleTimeMark.farPast()
    private val rareSeaCreatures = TimeLimitedSet<Mob>(6.minutes)
    private val entityIds = TimeLimitedSet<Int>(6.minutes)

    @HandleEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!isEnabled()) return
        val mob = event.mob
        val creature = SeaCreatureManager.allFishingMobs[mob.name] ?: return
        if (!creature.rare) return

        rareSeaCreatures.add(mob)

        if (!config.highlight) return

        mob.highlight(LorenzColor.GREEN.toColor())
    }

    @HandleEvent
    fun onMobFirstSeen(event: MobEvent.FirstSeen.SkyblockMob) {
        if (!isEnabled()) return
        val mob = event.mob
        if (mob !in rareSeaCreatures) return
        val entity = mob.baseEntity
        val shouldNotify = entity.entityId !in entityIds
        entityIds.addIfAbsent(entity.entityId)
        val creature = SeaCreatureManager.allFishingMobs[mob.name] ?: return
        if (!creature.rare) return

        if (lastRareCatch.passedSince() < 1.seconds) return
        if (mob.name == "Water Hydra" && entity.health == (entity.baseMaxHealth.toFloat() / 2)) return
        if (config.alertOtherCatches && shouldNotify) {
            val text = if (config.creatureName) "${creature.displayName} NEARBY!"
            else "${creature.rarity.chatColorCode}RARE SEA CREATURE!"
            TitleManager.sendTitle(text, duration = 1.5.seconds)
            if (config.playSound) SoundUtils.playBeepSound()
        }
    }

    @HandleEvent
    fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        rareSeaCreatures.remove(event.mob)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSeaCreatureFish(event: SeaCreatureFishEvent) {
        if (!event.seaCreature.rare) return
        if (config.alertOwnCatches) {
            val text = if (config.creatureName) "${event.seaCreature.displayName}!"
            else "${event.seaCreature.rarity.chatColorCode}RARE CATCH!"
            TitleManager.sendTitle(text)
            if (config.playSound) SoundUtils.playBeepSound()
            lastRareCatch = SimpleTimeMark.now()
        }
        if (config.announceRareInParty && PartyApi.isInParty()) {
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
        rareSeaCreatures.clear()
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

    private val getEntityOutlineColor: (entity: Entity) -> Int? = { entity ->
        (entity as? EntityLivingBase)?.mob?.let { mob ->
            if (mob in rareSeaCreatures && entity.distanceToPlayer() < 30) {
                LorenzColor.GREEN.toColor().rgb
            } else null
        }
    }

    @JvmStatic
    fun isRareSeaCreature(entity: Entity): Boolean {
        return (entity as? EntityLivingBase)?.mob?.let { mob ->
            mob in rareSeaCreatures
        } ?: false
    }

    @JvmStatic
    fun isRareSeaCreatureBody(entity: Entity): Boolean {
        return entity is EntitySlime && isRareSeaCreature(entity)
    }
}
