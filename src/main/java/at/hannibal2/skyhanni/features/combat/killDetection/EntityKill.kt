package at.hannibal2.skyhanni.features.combat.killDetection

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.EntityData.Companion.currentSkyblockMobs
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.SkyblockMobDeathEvent
import at.hannibal2.skyhanni.events.SkyblockMobKillEvent
import at.hannibal2.skyhanni.events.onMobHitEvent
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.SkyblockMobUtils
import at.hannibal2.skyhanni.utils.SkyblockMobUtils.rayTraceForSkyblockMobs
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object EntityKill {

    private var mobHitList = mutableSetOf<SkyblockMobUtils.SkyblockMob>()
    val config get() = SkyHanniMod.feature.dev.mobKillDetection


    @SubscribeEvent
    fun onTick2(event: LorenzTickEvent) {
        if (config.LogMobHitList) {
            if (config.LogMobHitListId) {
                val id = mobHitList.map { it.baseEntity.entityId }
                LorenzDebug.log("Hit List: $id")
            } else {
                LorenzDebug.log("Hit List: $mobHitList")
            }
        }
    }

    @SubscribeEvent
    fun onEntityLivingDeath(event: SkyblockMobDeathEvent) {
        mobHitList.firstOrNull { it == event.entity }
            ?.let {
                SkyblockMobKillEvent(it, false).postAndCatch()
            }
    }

    @SubscribeEvent
    fun onSkyblockMobKill(event: SkyblockMobKillEvent) {
        if (config.ShowNameOfKilledMob) LorenzDebug.chatAndLog("Mob Name: ${event.mob.name}")
        mobHitList.remove(event.mob)
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        mobHitList.clear() //TODO Clear before the Kill trigger is activated
    }

    fun addToMobHitList(entity: Entity, trigger: hitTrigger) {
        mobHitList.firstOrNull { it == entity }?.let {
            if (checkIfBlacklisted(it.name, trigger)) return
            onMobHitEvent(it, trigger, false).postAndCatch()
            return
        }
        val mob = SkyblockMobUtils.createSkyblockMob(entity)
        if (checkIfBlacklisted(mob.name, trigger)) return
        mobHitList.add(mob)
        onMobHitEvent(mob, trigger, true).postAndCatch()
    }

    val magicBlacklist = setOf<String>() //Get it from repo

    private fun checkIfBlacklisted(name: String, trigger: hitTrigger): Boolean { //TODO complete the Blacklist
        if (trigger.isMagic()) {
            return magicBlacklist.contains(name)
        }
        if (trigger == hitTrigger.Cleave) {
            return name.contains("Star Sentry") // Do not know if more exist
        }
        return false
    }


    @SubscribeEvent
    fun onWorldRender(event: LorenzRenderWorldEvent) {
        if (config.skyblockMobHighlight) {
            currentSkyblockMobs.forEach {
                event.drawFilledBoundingBox_nea(it.entityBoundingBox.expandBlock(), LorenzColor.GREEN.toColor(), 0.3f)
            }
        }
        if (config.skyblockMobHitHighlight) {
            mobHitList.forEach {
                event.drawFilledBoundingBox_nea(
                    it.baseEntity.entityBoundingBox.expandBlock(), LorenzColor.LIGHT_PURPLE.toColor
                        (), 0.6f
                )
            }
        }
        if (config.skyblockMobHighlightRayHit) {
            rayTraceForSkyblockMobs(Minecraft.getMinecraft().thePlayer, event.partialTicks)?.forEach {
                event.drawFilledBoundingBox_nea(it.entityBoundingBox.expandBlock(), LorenzColor.YELLOW.toColor(), 0.5f)
            }
        }
    }

}
