package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.EntityMovementData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.ParticleEvent
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.holdingSkullTexture
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import at.hannibal2.skyhanni.utils.compat.EntityCompat.getStandHelmet
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.item.ItemEntity
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object DungeonHideItems {

    private val config get() = SkyHanniMod.feature.dungeon.objectHider

    private val hideParticles = TimeLimitedSet<ArmorStand>(
        expireAfterWrite = 100.milliseconds,
        useWeakKeys = true
    )
    private val movingSkeletonSkulls = TimeLimitedCache<ArmorStand, SimpleTimeMark>(
        expireAfterWrite = 200.milliseconds,
        useWeakKeys = true
    )

    private val SOUL_WEAVER_HIDER by SkullTextureHolder.texture("DUNGEONS_SOUL_WEAVER")
    private val BLESSING_TEXTURE by SkullTextureHolder.texture("DUNGEONS_BLESSING")
    private val REVIVE_STONE_TEXTURE by SkullTextureHolder.texture("DUNGEONS_REVIVE_STONE")
    private val PREMIUM_FLESH_TEXTURE by SkullTextureHolder.texture("DUNGEONS_PREMIUM_FLESH")
    private val ABILITY_ORB_TEXTURE by SkullTextureHolder.texture("DUNGEONS_ABILITY_ORB")
    private val SUPPORT_ORB_TEXTURE by SkullTextureHolder.texture("DUNGEONS_SUPPORT_ORB")
    private val DAMAGE_ORB_TEXTURE by SkullTextureHolder.texture("DUNGEONS_DAMAGE_ORB")
    private val HEALER_FAIRY_TEXTURE by SkullTextureHolder.texture("DUNGEONS_HEALER_FAIRY")

    private val patternGroup = RepoPattern.group("dungeon.item-hider")

    /**
     * REGEX-TEST: Revive Stone
     */
    private val reviveStonePattern by patternGroup.pattern("revive-stone", "^Revive Stone$")

    /**
     * REGEX-TEST: Journal Entry
     */
    private val journalEntryPattern by patternGroup.pattern("journal-entry", "^Journal Entry$")

    /**
     * REGEX-TEST: Premium Flesh
     */
    private val premiumFleshPattern by patternGroup.pattern("premium-flesh", "^Premium Flesh$")

    /**
     * REGEX-TEST: Skeleton Skull
     */
    private val skeletonSkullPattern by patternGroup.pattern("skeleton-skull", "^Skeleton Skull$")

    /**
     * REGEX-TEST: Superboom TNT
     */
    private val superboomTntPattern by patternGroup.pattern("superboom-tnt", "^Superboom TNT.*$")

    /**
     * REGEX-TEST: Blessing of Time
     */
    private val blessingPattern by patternGroup.pattern("blessing", "^Blessing of .*$")

    /**
     * REGEX-TEST: DAMAGE 30s
     */
    private val damageOrbPattern by patternGroup.pattern("healer-orbs.damage", "^DAMAGE .*$")

    /**
     * REGEX-TEST: ABILITY DAMAGE 30s
     */
    private val abilityDamageOrbPattern by patternGroup.pattern("healer-orbs.ability-damage", "^ABILITY DAMAGE .*$")

    /**
     * REGEX-TEST: DEFENSE 30s
     */
    private val defenseOrbPattern by patternGroup.pattern("healer-orbs.defense", "^DEFENSE .*$")

    private fun String?.matchesTexture(texture: String?) = texture != null && this == texture

    private fun isSkeletonSkull(headName: String?): Boolean =
        headName != null && skeletonSkullPattern.matches(headName)

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    private fun onCheckRender(event: CheckRenderEntityEvent<Entity>) {
        val shouldCancel = when (val entity = event.entity) {
            is ItemEntity -> onRenderItem(entity)
            is ArmorStand -> onRenderArmorStand(entity)
            else -> return
        }
        if (shouldCancel) {
            event.cancel()
        }
    }

    private fun onRenderItem(entity: ItemEntity): Boolean {
        val stack = entity.item
        val stackName = stack.cleanName

        return when {
            config.hideReviveStone && reviveStonePattern.matches(stackName) -> true
            config.hideJournalEntry && journalEntryPattern.matches(stackName) -> true
            else -> false
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun onRenderArmorStand(entity: ArmorStand): Boolean {
        val head = entity.getStandHelmet()
        val skullTexture = head?.getSkullTexture()
        val headName = head?.cleanName
        val entityName = entity.cleanName
        val skeletonHead = isSkeletonSkull(headName)
        if (skeletonHead) {
            EntityMovementData.addToTrack(entity)
        }

        return when {
            config.hideSuperboomTNT && headName != null && superboomTntPattern.matches(headName) -> {
                hideParticles.add(entity)
                true
            }
            config.hideSuperboomTNT && superboomTntPattern.matches(entityName) -> true

            config.hideBlessing && skullTexture.matchesTexture(BLESSING_TEXTURE) -> true
            config.hideBlessing && blessingPattern.matches(entityName) -> true

            config.hideReviveStone && skullTexture.matchesTexture(REVIVE_STONE_TEXTURE) -> {
                hideParticles.add(entity)
                true
            }
            config.hideReviveStone && reviveStonePattern.matches(entityName) -> true

            config.hidePremiumFlesh && skullTexture.matchesTexture(PREMIUM_FLESH_TEXTURE) -> true
            config.hidePremiumFlesh && premiumFleshPattern.matches(entityName) -> {
                hideParticles.add(entity)
                true
            }

            config.hideSkeletonSkull && skeletonHead -> {
                val lastMove = movingSkeletonSkulls[entity] ?: SimpleTimeMark.farPast()
                lastMove.passedSince() >= 100.milliseconds
            }

            config.hideHealerOrbs && (
                skullTexture.matchesTexture(ABILITY_ORB_TEXTURE) ||
                    skullTexture.matchesTexture(SUPPORT_ORB_TEXTURE) ||
                    skullTexture.matchesTexture(DAMAGE_ORB_TEXTURE)
                ) -> {
                hideParticles.add(entity)
                true
            }
            config.hideHealerOrbs && (
                damageOrbPattern.matches(entityName) ||
                    abilityDamageOrbPattern.matches(entityName) ||
                    defenseOrbPattern.matches(entityName)
                ) -> true

            config.hideHealerFairy && entity.holdingSkullTexture(HEALER_FAIRY_TEXTURE) -> true

            config.hideSoulweaverSkulls && skullTexture.matchesTexture(SOUL_WEAVER_HIDER) -> true

            else -> false
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    private fun onParticle(event: ParticleEvent) {
        if (!config.hideSuperboomTNT && !config.hideReviveStone &&
            !config.hidePremiumFlesh && !config.hideHealerOrbs
        ) return

        val packetLocation = event.location
        for (armorStand in hideParticles) {
            val distance = packetLocation.distance(armorStand.getLorenzVec())
            if (distance < 2) {
                if (event.type == ParticleTypes.FIREWORK || event.type == ParticleTypes.DUST) {
                    event.cancel()
                }
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    private fun onArmorStandMove(event: EntityMoveEvent<ArmorStand>) {
        val entity = event.entity
        val headName = entity.getStandHelmet()?.cleanName

        if (isSkeletonSkull(headName)) {
            movingSkeletonSkulls[entity] = SimpleTimeMark.now()
            RenderLivingEntityHelper.setEntityColor(
                entity,
                LorenzColor.GOLD.toColor().addAlpha(60),
            ) { shouldColorMovingSkull(entity) }
        }
    }

    private fun shouldColorMovingSkull(entity: ArmorStand): Boolean =
        SkyHanniMod.feature.dungeon.highlightSkeletonSkull &&
            movingSkeletonSkulls[entity]?.let {
                it.passedSince() < 200.milliseconds
            } ?: false

    @HandleEvent
    private fun onWorldChange() {
        hideParticles.clear()
        movingSkeletonSkulls.clear()
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "dungeon.hideSuperboomTNT", "dungeon.objectHider.hideSuperboomTNT")
        event.move(3, "dungeon.hideBlessing", "dungeon.objectHider.hideBlessing")
        event.move(3, "dungeon.hideReviveStone", "dungeon.objectHider.hideReviveStone")
        event.move(3, "dungeon.hidePremiumFlesh", "dungeon.objectHider.hidePremiumFlesh")
        event.move(3, "dungeon.hideJournalEntry", "dungeon.objectHider.hideJournalEntry")
        event.move(3, "dungeon.hideSkeletonSkull", "dungeon.objectHider.hideSkeletonSkull")
        event.move(3, "dungeon.hideHealerOrbs", "dungeon.objectHider.hideHealerOrbs")
        event.move(3, "dungeon.hideHealerFairy", "dungeon.objectHider.hideHealerFairy")
    }
}
