package at.hannibal2.skyhanni.features.combat.end.dragon

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import net.minecraft.world.entity.decoration.ArmorStand
import java.util.UUID
import kotlin.math.floor
import kotlin.time.Duration.Companion.seconds

/**
 * Works out what a finished dragon fight dropped and hands the result to [DragonProfitTracker].
 *
 * Visible drops are read from their floating item labels; everything the scan cannot see is
 * derived from the fight weight, which is why the numbers for fragments and pearls are estimates
 * rather than measurements.
 */
@SkyHanniModule
object DragonLootDetection {
    var finishedLoot = true

    private val scannedLootUUIDs = mutableSetOf<UUID>()
    private val dragonLoot = mutableMapOf<NeuInternalName, Int>()

    private val ENCHANTED_ENDER_PEARL = "ENCHANTED_ENDER_PEARL".toInternalName()
    private val ENDER_PEARL = "ENDER_PEARL".toInternalName()

    // This can probably be optimized to not use getEntities, but it's not a big issue right now
    @OptIn(AllEntitiesGetter::class)
    private fun scanForLoot() {
        val entities = EntityUtils.getEntities<ArmorStand>()

        scannedLootUUIDs.removeIf { uuid -> entities.none { it.uuid == uuid } }

        for (entity in entities) {
            val entityName = entity.name.formattedTextCompatLessResets()
            val amount: Int = entityName.split("§8x").last().toIntOrNull() ?: 1
            val internalName = NeuInternalName.fromItemNameOrNull(entityName) ?: continue
            if (internalName !in DragonProfitTracker.allowedItems.keys) continue
            if (entity.uuid in scannedLootUUIDs) continue

            ChatUtils.debug("Adding $internalName x$amount to dragon loot")
            dragonLoot.addOrPut(internalName, amount)
            scannedLootUUIDs.add(entity.uuid)
        }

        if (dragonLoot.isNotEmpty() && DragonWeight.weight >= 290) {
            var weight = DragonWeight.weight
            ChatUtils.debug("Weight: $weight")

            weight -= DragonProfitTracker.allowedItems[dragonLoot.keys.first()]?.weight ?: 0
            ChatUtils.debug("Weight: $weight after main drop (${dragonLoot.keys.first()})")

            calculateNonUniqueLoot(weight)
        } else if (DragonWeight.weight < 290) {
            ChatUtils.debug("Weight: ${DragonWeight.weight} < 290")
            calculateNonUniqueLoot(DragonWeight.weight)
        }
    }

    private fun calculateNonUniqueLoot(weightIn: Double) {
        var weight = weightIn
        val type = DragonProfitTracker.lastDragonKill ?: DragonType.UNKNOWN

        val fragmentWeight = 22
        val fragAmount = floor(weight / fragmentWeight)
        weight -= fragAmount * fragmentWeight
        ChatUtils.debug("Weight: $weight after frags(${fragAmount.toInt()} frags)")

        if (type != DragonType.UNKNOWN) dragonLoot.addOrPut(type.fragment, fragAmount.toInt())

        val enchantedEnderPearlWeight = 15
        var enchantedEnderPearlAmount = floor(weight / enchantedEnderPearlWeight)
        weight -= enchantedEnderPearlAmount * enchantedEnderPearlWeight
        enchantedEnderPearlAmount += getStandardEnchantedEnderPearlAmount(DragonProfitTracker.lastDragonPlacement ?: 0)

        ChatUtils.debug(
            "Weight: $weight after enchanted ender pearls (${enchantedEnderPearlAmount.toInt()} ender pearls)",
        )

        dragonLoot.addOrPut(ENCHANTED_ENDER_PEARL, enchantedEnderPearlAmount.toInt())

        val enderPearlWeight = 5
        var enderPearlAmount = floor(weight / enderPearlWeight)
        weight -= enderPearlAmount * enderPearlWeight
        enderPearlAmount += getStandardEnderPearlAmount(DragonProfitTracker.lastDragonPlacement ?: 0)

        ChatUtils.debug("Weight: $weight after ender pearls (${enderPearlAmount.toInt()} pearls)")

        dragonLoot.addOrPut(ENDER_PEARL, enderPearlAmount.toInt())

        DragonProfitTracker.addDragonLootFromList(type, dragonLoot.toList())

        dragonLoot.clear()
        finishedLoot = true
    }

    fun reset() {
        scannedLootUUIDs.clear()
        dragonLoot.clear()
    }

    private fun getStandardEnderPearlAmount(placement: Int) = when (placement) {
        1 -> 30
        2 -> 25
        3 -> 22
        4 -> 20
        5 -> 18
        6, 7, 8, 9, 10, 11, 12 -> 15
        13, 14, 15, 16, 17, 18 -> 12
        19, 20, 21, 22, 23, 24, 25 -> 10
        else -> 5
    }

    private fun getStandardEnchantedEnderPearlAmount(placement: Int) = when (placement) {
        1 -> 7
        2 -> 6
        3 -> 5
        4 -> 4
        5 -> 3
        6, 7, 8, 9, 10, 11, 12 -> 2
        13, 14, 15, 16, 17, 18 -> 1
        else -> 0
    }

    private var lastScanned = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onTick() {
        if (lastScanned.passedSince() >= 1.seconds && !DragonFightState.eggSpawned && !finishedLoot) {
            scanForLoot()
            lastScanned = SimpleTimeMark.now()
        }
    }
}
