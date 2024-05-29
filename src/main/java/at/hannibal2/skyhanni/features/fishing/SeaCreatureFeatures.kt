package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorConfig
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent
import at.hannibal2.skyhanni.events.SeaCreatureFishEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.TimeLimitedSet
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SeaCreatureFeatures {

    private val config get() = SkyHanniMod.feature.fishing.rareCatches
    private val damageIndicatorConfig get() = SkyHanniMod.feature.combat.damageIndicator
    private var rareSeaCreatures = listOf<EntityLivingBase>()
    private var lastRareCatch = SimpleTimeMark.farPast()
    private var armorStandIds = TimeLimitedSet<Int>(6.minutes)

    @SubscribeEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!isEnabled()) return
        val creature = SeaCreatureManager.allFishingMobs[event.mob.name] ?: return
        if (!creature.rare) return

        if (config.highlight && !(damageIndicatorConfig.enabled &&
                DamageIndicatorConfig.BossCategory.SEA_CREATURES in damageIndicatorConfig.bossesToShow)
        ) {
            event.mob.highlight(LorenzColor.GREEN.toColor())
            rareSeaCreatures += event.mob.baseEntity
        }
        val id = event.mob.armorStand?.entityId ?: return
        if (armorStandIds.contains(id)) return
        armorStandIds.add(id)

        if (lastRareCatch.passedSince() < 1.seconds) return
        if (event.mob.name == "Water Hydra" && event.mob.baseEntity.health == (event.mob.baseEntity.baseMaxHealth.toFloat() / 2)) return

        if (config.alertOtherCatches) {
            val text = if (config.creatureName) "${creature.displayName} NEARBY!"
            else "${creature.rarity.chatColorCode}RARE SEA CREATURE!"
            LorenzUtils.sendTitle(text, 1.5.seconds, 3.6, 7f)
            if (config.playSound) SoundUtils.playBeepSound()
        }
    }

    @SubscribeEvent
    fun onMobDeSpawn(event: MobEvent.DeSpawn.SkyblockMob) {
        rareSeaCreatures.filter { it != event.mob.baseEntity }
    }

    @SubscribeEvent
    fun onSeaCreatureFish(event: SeaCreatureFishEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.alertOwnCatches) return

        if (event.seaCreature.rare) {
            val text = if (config.creatureName) "${event.seaCreature.displayName}!"
            else "${event.seaCreature.rarity.chatColorCode}RARE CATCH!"
            LorenzUtils.sendTitle(text, 3.seconds, 2.8, 7f)
            if (config.playSound) SoundUtils.playBeepSound()
            lastRareCatch = SimpleTimeMark.now()
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        rareSeaCreatures = listOf()
        armorStandIds.clear()
    }

    @SubscribeEvent
    fun onRenderEntityOutlines(event: RenderEntityOutlineEvent) {
        if (isEnabled() && config.highlight && event.type === RenderEntityOutlineEvent.Type.XRAY) {
            event.queueEntitiesToOutline(getEntityOutlineColor)
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "fishing.rareSeaCreatureHighlight", "fishing.rareCatches.highlight")
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && !DungeonAPI.inDungeon() && !LorenzUtils.inKuudraFight

    private val getEntityOutlineColor: (entity: Entity) -> Int? = { entity ->
        if (entity is EntityLivingBase && entity in rareSeaCreatures && entity.distanceToPlayer() < 30) {
            LorenzColor.GREEN.toColor().rgb
        } else null
    }
}
