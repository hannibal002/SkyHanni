package at.hannibal2.skyhanni.features.combat.end

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ProfitPerDragon {
    private var scannedLootUUIDs = mutableSetOf<UUID>()
    private var dragonLoot = mutableSetOf<NeuInternalName>()

    private fun scanForLoot() {
        val entities = EntityUtils.getEntities<EntityArmorStand>()
        entities.forEach { entity ->
            val entityName = entity.name
            val amount: Int = entityName.split("§8x").last().toIntOrNull() ?: 1
            val internalNameFromEntityName = NeuInternalName.fromItemNameOrNull(entityName)

            if (internalNameFromEntityName in DragonProfitTracker.allowedItems) {
                if (internalNameFromEntityName == null) {
                    ChatUtils.debug("Could not find internal name for entity name: $entityName")
                    return@forEach
                }
                if (entity.uniqueID in scannedLootUUIDs) return@forEach

                ChatUtils.chat("Found loot: $entityName, ${entity.uniqueID}, $internalNameFromEntityName")

                DragonProfitTracker.addDragonLoot(DragonProfitTracker.lastDragonKill ?: DragonType.UNKNOWN, internalNameFromEntityName, amount)

                scannedLootUUIDs.add(entity.uniqueID)
            }
        }
    }

    fun reset() {
        scannedLootUUIDs.clear()
        dragonLoot.clear()
    }

    private var lastScanned = SimpleTimeMark.farPast()

    @HandleEvent
    fun onTick(e: SkyHanniTickEvent) {
        if (lastScanned.passedSince() >= 1.seconds) {
            scanForLoot()
            lastScanned = SimpleTimeMark.now()
        }
    }
}
