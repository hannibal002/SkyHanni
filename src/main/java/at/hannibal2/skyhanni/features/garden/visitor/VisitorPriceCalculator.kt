package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.features.garden.CropType.Companion.getByNameOrNull
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Pure calculation helper for visitor economics.
 * No state, no events - just math.
 */
object VisitorPriceCalculator {

    private val greenThumb = "GREEN_THUMB;1".toInternalName()

    /**
     * Calculates the estimated value of copper based on Green Thumb I price.
     * Formula: Green Thumb price / 1500 (copper from Green Thumb)
     */
    fun getEstimatedCopperValue(): Double = greenThumb.getPrice() / 1500

    /**
     * Calculates cost per copper unit.
     * @return Price per copper as integer
     */
    fun calculatePricePerCopper(totalPrice: Double, copper: Int): Int {
        if (copper <= 0) return 0
        return (totalPrice / copper).toInt()
    }

    /**
     * Calculates total reward value based on copper amount.
     * @param copper Amount of copper offered
     * @return Estimated coin value of the copper
     */
    fun calculateTotalReward(copper: Int): Double {
        return copper * getEstimatedCopperValue()
    }

    /**
     * Calculates how long it would take to farm the required amount of an item.
     * @param internalName The item to farm
     * @param amount How many needed
     * @return Duration to farm, or null if not a farmable crop or no speed data
     */
    fun calculateFarmingTime(internalName: NeuInternalName, amount: Int): Duration? {
        val primitiveStack = NeuItems.getPrimitiveMultiplier(internalName)
        val rawName = primitiveStack.internalName.itemNameWithoutColor
        val cropType = getByNameOrNull(rawName) ?: return null
        val speed = cropType.getSpeed() ?: return null

        val cropAmount = primitiveStack.amount.toLong() * amount
        return (cropAmount / speed).seconds
    }

    /**
     * Calculates the total price for a shopping list item.
     * @param internalName The item
     * @param amount How many
     * @return Total bazaar/AH price
     */
    fun calculateItemPrice(internalName: NeuInternalName, amount: Int): Double {
        return internalName.getPrice() * amount
    }
}
