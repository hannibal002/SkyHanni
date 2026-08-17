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
import at.hannibal2.skyhanni.utils.compat.EntityCompat.getStandHelmet
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.item.ItemEntity
import kotlin.time.Duration.Companion.milliseconds

// TODO: Add repo pattern tests
@Suppress("RepoPatternRegexTestMissing")
@SkyHanniModule
object DungeonHideItems {

    private val config get() = SkyHanniMod.feature.dungeon.objectHider

    private val hideParticles = mutableMapOf<ArmorStand, SimpleTimeMark>()
    private val movingSkeletonSkulls = mutableMapOf<ArmorStand, SimpleTimeMark>()

    private val SOUL_WEAVER_HIDER by SkullTextureHolder.texture("DUNGEONS_SOUL_WEAVER")
    private val BLESSING_TEXTURE by SkullTextureHolder.texture("DUNGEONS_BLESSING")
    private val REVIVE_STONE_TEXTURE by SkullTextureHolder.texture("DUNGEONS_REVIVE_STONE")
    private val PREMIUM_FLESH_TEXTURE by SkullTextureHolder.texture("DUNGEONS_PREMIUM_FLESH")
    private val ABILITY_ORB_TEXTURE by SkullTextureHolder.texture("DUNGEONS_ABILITY_ORB")
    private val SUPPORT_ORB_TEXTURE by SkullTextureHolder.texture("DUNGEONS_SUPPORT_ORB")
    private val DAMAGE_ORB_TEXTURE by SkullTextureHolder.texture("DUNGEONS_DAMAGE_ORB")
    private val HEALER_FAIRY_TEXTURE by SkullTextureHolder.texture("DUNGEONS_HEALER_FAIRY")

    private val patternGroup = RepoPattern.group("dungeon.item-hider")

    private val reviveStonePattern by patternGroup.pattern("revive-stone", "^Revive Stone$")
    private val journalEntryPattern by patternGroup.pattern("journal-entry", "^Journal Entry$")
    private val superboomTntPattern by patternGroup.pattern("superboom-tnt", "^Superboom TNT.*$")
    private val blessingPattern by patternGroup.pattern("blessing", "^Blessing of .*$")
    private val premiumFleshPattern by patternGroup.pattern("premium-flesh", "^Premium Flesh$")
    private val skeletonSkullPattern by patternGroup.pattern("skeleton-skull", "^Skeleton Skull$")
    private val damageOrbPattern by patternGroup.pattern("healer-orbs.damage", "^DAMAGE .*$")
    private val abilityDamageOrbPattern by patternGroup.pattern("healer-orbs.ability-damage", "^ABILITY DAMAGE .*$")
    private val defenseOrbPattern by patternGroup.pattern("healer-orbs.defense", "^DEFENSE .*$")

    private fun String?.matchesTexture(texture: String?) = texture != null && this == texture

    private fun isSkeletonSkull(headName: String?): Boolean = headName != null && skeletonSkullPattern.matches(headName)

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    private fun onCheckRender(event: CheckRenderEntityEvent<Entity>) {
        val entity = event.entity

        if (entity is ItemEntity) {
            val stack = entity.item
            val stackName = stack.cleanName

            if (config.hideReviveStone && reviveStonePattern.matches(stackName)) {
                event.cancel()
            }

            if (config.hideJournalEntry && journalEntryPattern.matches(stackName)) {
                event.cancel()
            }
            return
        }

        if (entity !is ArmorStand) return

        val head = entity.getStandHelmet()
        val skullTexture = head?.getSkullTexture()
        val headName = head?.cleanName
        val entityName by lazy { entity.cleanName }

        if (config.hideSuperboomTNT) {
            if (headName != null && superboomTntPattern.matches(headName)) {
                event.cancel()
                hideParticles[entity] = SimpleTimeMark.now()
            } else if (superboomTntPattern.matches(entityName)) {
                event.cancel()
            }
        }

        if (config.hideBlessing) {
            if (skullTexture.matchesTexture(BLESSING_TEXTURE)) {
                event.cancel()
            } else if (blessingPattern.matches(entityName)) {
                event.cancel()
            }
        }

        if (config.hideReviveStone) {
            if (skullTexture.matchesTexture(REVIVE_STONE_TEXTURE)) {
                event.cancel()
                hideParticles[entity] = SimpleTimeMark.now()
            } else if (reviveStonePattern.matches(entityName)) {
                event.cancel()
            }
        }

        if (config.hidePremiumFlesh) {
            if (skullTexture.matchesTexture(PREMIUM_FLESH_TEXTURE)) {
                event.cancel()
            } else if (premiumFleshPattern.matches(entityName)) {
                event.cancel()
                hideParticles[entity] = SimpleTimeMark.now()
            }
        }

        if (isSkeletonSkull(headName)) {
            EntityMovementData.addToTrack(entity)
            if (config.hideSkeletonSkull) {
                val lastMove = movingSkeletonSkulls[entity] ?: SimpleTimeMark.farPast()
                if (lastMove.passedSince() < 100.milliseconds) return
                event.cancel()
            }
        }

        if (config.hideHealerOrbs) {
            if (
                skullTexture.matchesTexture(ABILITY_ORB_TEXTURE) ||
                skullTexture.matchesTexture(SUPPORT_ORB_TEXTURE) ||
                skullTexture.matchesTexture(DAMAGE_ORB_TEXTURE)
            ) {
                event.cancel()
                hideParticles[entity] = SimpleTimeMark.now()
                return
            }

            if (
                damageOrbPattern.matches(entityName) ||
                abilityDamageOrbPattern.matches(entityName) ||
                defenseOrbPattern.matches(entityName)
            ) {
                event.cancel()
            }
        }

        if (config.hideHealerFairy) {
            if (entity.holdingSkullTexture(HEALER_FAIRY_TEXTURE)) {
                event.cancel()
                return
            }
        }

        if (config.hideSoulweaverSkulls) {
            if (skullTexture.matchesTexture(SOUL_WEAVER_HIDER)) {
                event.cancel()
                return
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    private fun onParticle(event: ParticleEvent) {
        if (!config.hideSuperboomTNT && !config.hideReviveStone) return

        val packetLocation = event.location
        for (armorStand in hideParticles.filterValues { it.passedSince() < 100.milliseconds }.keys) {
            val distance = packetLocation.distance(armorStand.getLorenzVec())
            if (distance < 2) {
                if (event.type == ParticleTypes.FIREWORK) {
                    event.cancel()
                }
                if (event.type == ParticleTypes.DUST) {
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

    private fun shouldColorMovingSkull(entity: Entity) =
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
