package at.hannibal2.skyhanni.features.combat.end

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.entity.item.EntityArmorStand
import java.util.UUID
import kotlin.math.floor
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ProfitPerDragon {
    private val scannedLootUUIDs = mutableSetOf<UUID>()
    private val dragonLoot = mutableMapOf<NeuInternalName, Int>()

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

            if (internalNameFromEntityName in DragonProfitTracker.allowedItems.keys) {
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
            }
        }

        // Time for all armor stands to spawn
        if (dragonLoot.isNotEmpty()) {
            var weight = DragonFeatures.weight
            ChatUtils.debug("Weight: $weight")

            weight -= DragonProfitTracker.allowedItems[dragonLoot.keys.first()] ?: 0
            ChatUtils.debug("Weight: $weight after main drop")

            val dragType = DragonProfitTracker.lastDragonKill ?: DragonType.UNKNOWN

            val fragAmount = floor(weight / 22)
            weight -= fragAmount * 22
            ChatUtils.debug("Weight: $weight after frags")

            dragonLoot.addOrPut("${dragType}_FRAGMENT".toInternalName(), fragAmount.toInt())

            val enchantedEnderPearlAmount = floor(weight / 15)
            weight -= enchantedEnderPearlAmount * 15
            ChatUtils.debug("Weight: $weight after enchanted ender pearls")

            dragonLoot.addOrPut("ENCHANTED_ENDER_PEARL".toInternalName(), enchantedEnderPearlAmount.toInt())

            val enderPearlAmount = floor(weight / 5)
            weight -= enderPearlAmount * 5
            ChatUtils.debug("Weight: $weight after ender pearls")

            dragonLoot.addOrPut("ENDER_PEARL".toInternalName(), enderPearlAmount.toInt())

            DragonProfitTracker.addDragonLootFromList(dragType, dragonLoot.toList())

            dragonLoot.clear()
        }
    }

    fun reset() {
        scannedLootUUIDs.clear()
        dragonLoot.clear()
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
