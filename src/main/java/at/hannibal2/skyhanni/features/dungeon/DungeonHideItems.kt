package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.EntityMovementData
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.EntityMoveEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.ResetEntityHurtEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.item.EntityItem
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonHideItems {

    private val config get() = SkyHanniMod.feature.dungeon.objectHider

    private val hideParticles = mutableMapOf<EntityArmorStand, Long>()
    private val movingSkeletonSkulls = mutableMapOf<EntityArmorStand, Long>()

    private val blessingTexture =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTkzZTIwNjg2MTc4NzJjNTQyZWNkYTFkMjdkZjRlY2U5MWM2OTk5MDdiZjMyN2M0ZGRiODUzMDk0MTJkMzkzOSJ9fX0="

    private val reviveStoneTexture =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjZhNzZjYzIyZTdjMmFiOWM1NDBkMTI0NGVhZGJhNTgxZjVkZDllMThmOWFkYWNmMDUyODBhNWI0OGI4ZjYxOCJ9fX0K"

    private val premiumFleshTexture =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWE3NWU4YjA0NGM3MjAxYTRiMmU4NTZiZTRmYzMxNmE1YWFlYzY2NTc2MTY5YmFiNTg3MmE4ODUzNGI4MDI1NiJ9fX0K"

    private val abilityOrbTexture =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTAxZTA0MGNiMDFjZjJjY2U0NDI4MzU4YWUzMWQyZTI2NjIwN2M0N2NiM2FkMTM5NzA5YzYyMDEzMGRjOGFkNCJ9fX0="
    private val supportOrbTexture =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTMxYTRmYWIyZjg3ZGI1NDMzMDEzNjUxN2I0NTNhYWNiOWQ3YzBmZTc4NDMwMDcwOWU5YjEwOWNiYzUxNGYwMCJ9fX0="
    private val damageOrbTexture =
        "eyJ0aW1lc3RhbXAiOjE1NzQ5NTEzMTkwNDQsInByb2ZpbGVJZCI6IjE5MjUyMWI0ZWZkYjQyNWM4OTMxZjAyYTg0OTZlMTFiIiwicHJvZmlsZU5hbWUiOiJTZXJpYWxpemFibGUiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2FiODZkYTJlMjQzYzA1ZGMwODk4YjBjYzVkM2U2NDg3NzE3MzE3N2UwYTIzOTQ0MjVjZWMxMDAyNTljYjQ1MjYifX19"

    private val healerFairyTexture =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTZjM2UzMWNmYzY2NzMzMjc1YzQyZmNmYjVkOWE0NDM0MmQ2NDNiNTVjZDE0YzljNzdkMjczYTIzNTIifX19"


    private fun isSkeletonSkull(entity: EntityArmorStand): Boolean {
        val itemStack = entity.inventory[4]
        if (itemStack != null && itemStack.cleanName() == "Skeleton Skull") {
            return true
        }

        return false
    }

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!LorenzUtils.inDungeons) return

        val entity = event.entity

        if (entity is EntityItem) {
            val stack = entity.entityItem
            if (config.hideReviveStone && stack.cleanName() == "Revive Stone") {
                event.isCanceled = true
            }

            if (config.hideJournalEntry && stack.cleanName() == "Journal Entry") {
                event.isCanceled = true
            }
        }

        if (entity !is EntityArmorStand) return

        if (config.hideSuperboomTNT) {
            if (entity.name.startsWith("§9Superboom TNT")) {
                event.isCanceled = true
            }

            val itemStack = entity.inventory[4]
            if (itemStack != null && itemStack.cleanName() == "Superboom TNT") {
                event.isCanceled = true
                hideParticles[entity] = System.currentTimeMillis()
            }
        }

        if (config.hideBlessing) {
            if (entity.name.startsWith("§dBlessing of ")) {
                event.isCanceled = true
            }

            val itemStack = entity.inventory[4]
            if (itemStack != null && itemStack.getSkullTexture() == blessingTexture) {
                event.isCanceled = true
            }
        }

        if (config.hideReviveStone) {
            if (entity.name == "§6Revive Stone") {
                event.isCanceled = true
            }

            val itemStack = entity.inventory[4]
            if (itemStack != null && itemStack.getSkullTexture() == reviveStoneTexture) {
                event.isCanceled = true
                hideParticles[entity] = System.currentTimeMillis()
            }
        }

        if (config.hidePremiumFlesh) {
            if (entity.name == "§9Premium Flesh") {
                event.isCanceled = true
                hideParticles[entity] = System.currentTimeMillis()
            }

            val itemStack = entity.inventory[4]
            if (itemStack != null && itemStack.getSkullTexture() == premiumFleshTexture) {
                event.isCanceled = true
            }
        }

        if (isSkeletonSkull(entity)) {
            EntityMovementData.addToTrack(entity)
            if (config.hideSkeletonSkull) {
                val lastMove = movingSkeletonSkulls.getOrDefault(entity, 0)
                if (lastMove + 100 > System.currentTimeMillis()) {
                    return
                }
                event.isCanceled = true
            }
        }

        if (config.hideHealerOrbs) {
            when {
                entity.name.startsWith("§c§lDAMAGE §e") -> event.isCanceled = true
                entity.name.startsWith("§c§lABILITY DAMAGE §e") -> event.isCanceled = true
                entity.name.startsWith("§a§lDEFENSE §e") -> event.isCanceled = true
            }

            val itemStack = entity.inventory[4]
            if (itemStack != null) {
                when (itemStack.getSkullTexture()) {
                    abilityOrbTexture,
                    supportOrbTexture,
                    damageOrbTexture,
                    -> {
                        event.isCanceled = true
                        hideParticles[entity] = System.currentTimeMillis()
                        return
                    }
                }
            }
        }

        if (config.hideHealerFairy) {
            val itemStack = entity.inventory[0]
            if (itemStack != null && itemStack.getSkullTexture() == healerFairyTexture) {
                event.isCanceled = true
                return
            }
        }
    }

    @SubscribeEvent
    fun onReceivePacket(event: ReceiveParticleEvent) {
        if (!LorenzUtils.inDungeons) return
        if (!config.hideSuperboomTNT && !config.hideReviveStone) return

        val packetLocation = event.location
        for (armorStand in hideParticles.filter { it.value + 100 > System.currentTimeMillis() }.map { it.key }) {
            val distance = packetLocation.distance(armorStand.getLorenzVec())
            if (distance < 2) {
                if (event.type == EnumParticleTypes.FIREWORKS_SPARK) {
                    event.isCanceled = true
                }
                if (event.type == EnumParticleTypes.REDSTONE) {
                    event.isCanceled = true
                }
            }
        }
    }

    @SubscribeEvent
    fun onEntityMove(event: EntityMoveEvent) {
        if (!LorenzUtils.inDungeons) return

        val entity = event.entity
        if (entity !is EntityArmorStand) return

        if (isSkeletonSkull(entity)) {
            movingSkeletonSkulls[entity] = System.currentTimeMillis()
        }
    }

    @SubscribeEvent
    fun onRenderMobColored(event: RenderMobColoredEvent) {
        if (!LorenzUtils.inDungeons) return
        if (!SkyHanniMod.feature.dungeon.highlightSkeletonSkull) return
        val entity = event.entity
        if (entity is EntityArmorStand && isSkeletonSkull(entity)) {
            val lastMove = movingSkeletonSkulls.getOrDefault(entity, 0)
            if (lastMove + 200 > System.currentTimeMillis()) {
                event.color = LorenzColor.GOLD.toColor().withAlpha(60)
            }
        }
    }

    @SubscribeEvent
    fun onResetEntityHurtTime(event: ResetEntityHurtEvent) {
        if (!LorenzUtils.inDungeons) return
        if (!SkyHanniMod.feature.dungeon.highlightSkeletonSkull) return
        val entity = event.entity
        if (entity is EntityArmorStand && isSkeletonSkull(entity)) {
            val lastMove = movingSkeletonSkulls.getOrDefault(entity, 0)
            if (lastMove + 200 > System.currentTimeMillis()) {
                event.shouldReset = true
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        hideParticles.clear()
        movingSkeletonSkulls.clear()
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
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