package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.EntityMovementData
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.item.EntityItem
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonHideItems {

    private val hideParticles = mutableMapOf<EntityArmorStand, Long>()
    private val movingSkeletonSkulls = mutableMapOf<EntityArmorStand, Long>()

    private val blessingTexture =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZT" +
                "kzZTIwNjg2MTc4NzJjNTQyZWNkYTFkMjdkZjRlY2U5MWM2OTk5MDdiZjMyN2M0ZGRiODUzMDk0MTJkMzkzOSJ9fX0="

    private val reviveStoneTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJ" +
            "lcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjZhNzZjYzIyZTdjMmFiOWM1NDBkMTI0NGVhZGJhNTgxZ" +
            "jVkZDllMThmOWFkYWNmMDUyODBhNWI0OGI4ZjYxOCJ9fX0K"

    private val premiumFleshTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0" +
            "L3RleHR1cmUvMWE3NWU4YjA0NGM3MjAxYTRiMmU4NTZiZTRmYzMxNmE1YWFlYzY2NTc2MTY5YmFiNTg3MmE4ODUzNGI4MDI1NiJ9fX0K"

    private fun isSkeletonSkull(entity: EntityArmorStand): Boolean {
        val itemStack = entity.inventory[4]
        if (itemStack != null) {
            if (itemStack.cleanName() == "Skeleton Skull") {
                return true
            }
        }

        return false
    }

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!LorenzUtils.inDungeons) return

        val entity = event.entity

        if (entity is EntityItem) {
            val stack = entity.entityItem
            if (SkyHanniMod.feature.dungeon.hideReviveStone) {
                if (stack.cleanName() == "Revive Stone") {
                    event.isCanceled = true
                }
            }

            if (SkyHanniMod.feature.dungeon.hideJournalEntry) {
                if (stack.cleanName() == "Journal Entry") {
                    event.isCanceled = true
                }
            }
        }

        if (entity !is EntityArmorStand) return

        if (SkyHanniMod.feature.dungeon.hideSuperboomTNT) {
            if (entity.name.startsWith("§9Superboom TNT")) {
                event.isCanceled = true
            }

            val itemStack = entity.inventory[4]
            if (itemStack != null) {
                if (itemStack.cleanName() == "Superboom TNT") {
                    event.isCanceled = true
                    hideParticles[entity] = System.currentTimeMillis()
                }
            }
        }

        if (SkyHanniMod.feature.dungeon.hideBlessing) {
            if (entity.name.startsWith("§dBlessing of ")) {
                event.isCanceled = true
            }

            val itemStack = entity.inventory[4]
            if (itemStack != null) {
                if (itemStack.getSkullTexture() == blessingTexture) {
                    event.isCanceled = true
                }
            }
        }

        if (SkyHanniMod.feature.dungeon.hideReviveStone) {
            if (entity.name == "§6Revive Stone") {
                event.isCanceled = true
            }

            val itemStack = entity.inventory[4]
            if (itemStack != null) {
                if (itemStack.getSkullTexture() == reviveStoneTexture) {
                    event.isCanceled = true
                    hideParticles[entity] = System.currentTimeMillis()
                }
            }
        }

        if (SkyHanniMod.feature.dungeon.hidePremiumFlesh) {
            if (entity.name == "§9Premium Flesh") {
                event.isCanceled = true
                hideParticles[entity] = System.currentTimeMillis()
            }

            val itemStack = entity.inventory[4]
            if (itemStack != null) {
                if (itemStack.getSkullTexture() == premiumFleshTexture) {
                    event.isCanceled = true
                }
            }
        }

        if (isSkeletonSkull(entity)) {
            EntityMovementData.addToTrack(entity)
            if (SkyHanniMod.feature.dungeon.hideSkeletonSkull) {
                val lastMove = movingSkeletonSkulls.getOrDefault(entity, 0)
                if (lastMove + 100 > System.currentTimeMillis()) {
                    return
                }
                event.isCanceled = true
            }
        }
    }

    @SubscribeEvent
    fun onReceivePacket(event: PlayParticleEvent) {
        if (!LorenzUtils.inDungeons) return
        if (!SkyHanniMod.feature.dungeon.hideSuperboomTNT && !SkyHanniMod.feature.dungeon.hideReviveStone) return

            val packetLocation = event.location
            for (armorStand in hideParticles.filter { it.value + 100 > System.currentTimeMillis() }.map { it.key }) {
                val distance = packetLocation.distance(armorStand.getLorenzVec())
                if (distance < 2) {
                    //only hiding white "sparkling" particles
                    if (event.type.particleID == 3) {
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
        if (entity is EntityArmorStand) {
            if (isSkeletonSkull(entity)) {
                val lastMove = movingSkeletonSkulls.getOrDefault(entity, 0)
                if (lastMove + 100 > System.currentTimeMillis()) {
                    event.color = LorenzColor.GOLD.toColor().withAlpha(60)
                }
            }
        }
    }

    @SubscribeEvent
    fun onResetEntityHurtTime(event: ResetEntityHurtEvent) {
        if (!LorenzUtils.inDungeons) return
        if (!SkyHanniMod.feature.dungeon.highlightSkeletonSkull) return
        val entity = event.entity
        if (entity is EntityArmorStand) {
            if (isSkeletonSkull(entity)) {
                val lastMove = movingSkeletonSkulls.getOrDefault(entity, 0)
                if (lastMove + 100 > System.currentTimeMillis()) {
                    event.shouldReset = true
                }
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        hideParticles.clear()
        movingSkeletonSkulls.clear()
    }
}