package at.hannibal2.skyhanni.features.combat.end

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.entity.item.EntityArmorStand
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ProfitPerDragon {
    private val scannedLootUUIDs = mutableSetOf<UUID>()
    private val dragonLoot = mutableMapOf<NeuInternalName, Int>()

    private var scannedForLoot: Int = 0

    private fun scanForLoot() {
        val entities = EntityUtils.getEntities<EntityArmorStand>()

        scannedLootUUIDs.forEach { uuid ->
            if (entities.none { it.uniqueID == uuid }) {
                scannedLootUUIDs.remove(uuid)
            }
        }

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

                DragonProfitTracker.addDragonLoot(
                    DragonProfitTracker.lastDragonKill ?: DragonType.UNKNOWN,
                    internalNameFromEntityName,
                    amount
                )

                dragonLoot.addOrPut(internalNameFromEntityName, amount)

                scannedLootUUIDs.add(entity.uniqueID)
                scannedForLoot += 1
            }
        }

        // Time for all armor stands to spawn
        if (dragonLoot.isNotEmpty() && scannedForLoot >= 4) {
            DragonProfitTracker.addDragonLootFromList(DragonProfitTracker.lastDragonKill ?: DragonType.UNKNOWN, dragonLoot.toList())

            scannedForLoot = 0
            dragonLoot.clear()
        }
    }

    fun reset() {
        scannedLootUUIDs.clear()
        dragonLoot.clear()
        scannedForLoot = 0
    }

    private var lastScanned = SimpleTimeMark.farPast()

    @HandleEvent
    fun onTick(e: SkyHanniTickEvent) {
        if (lastScanned.passedSince() >= 1.seconds && !DragonFeatures.egg) {
            scanForLoot()
            lastScanned = SimpleTimeMark.now()
        }
    }
}
