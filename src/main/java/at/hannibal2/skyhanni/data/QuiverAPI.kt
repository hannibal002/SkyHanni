package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpaceAndResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemBow
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

private val infityQuiverLevelMultiplier = 0.03f

enum class Arrows(val arrow: String, val internalName: NEUInternalName) {
    NONE("None", "NONE".asInternalName()),
    FLINT("Flint Arrow", "ARROW".asInternalName()),
    REINFORCED_IRON_ARROW("Reinforced Iron Arrow", "REINFORCED_IRON_ARROW".asInternalName()),
    GOLD_TIPPED_ARROW("Gold-tipped Arrow", "GOLD_TIPPED_ARROW".asInternalName()),
    REDSTONE_TIPPED_ARROW("Redstone-tipped Arrow", "REDSTONE_TIPPED_ARROW".asInternalName()),
    EMERALD_TIPPED_ARROW("Emerald-tipped Arrow", "EMERALD_TIPPED_ARROW".asInternalName()),
    BOUNCY_ARROW("Bouncy Arrow", "BOUNCY_ARROW".asInternalName()),
    ICY_ARROW("Icy Arrow", "ICY_ARROW".asInternalName()),
    ARMORSHRED_ARROW("Armorshred Arrow", "ARMORSHRED_ARROW".asInternalName()),
    EXPLOSIVE_ARROW("Explosive Arrow", "EXPLOSIVE_ARROW".asInternalName()),
    GLUE_ARROW("Glue Arrow", "GLUE_ARROW".asInternalName()),
    NANSORB_ARROW("Nansorb Arrow", "NANSORB_ARROW".asInternalName()),
}

object QuiverAPI {
    var currentArrow: Arrows? = null
    var currentAmount: Int = 0
    var arrowAmount: MutableMap<Arrows, Float> = mutableMapOf()

    private val selectPattern by RepoPattern.pattern("data.quiver.chat.select", "§aYou set your selected arrow type to §f(?<arrow>.*)§a!")
    private val fillUpJaxPattern by RepoPattern.pattern("data.quiver.chat.fillupjax", "§aJax forged §f(?<type>.*)§8 x(?<amount>.*) §afor §6(?<coins>.*) Coins§a!")
    private val fillUpPattern by RepoPattern.pattern("data.quiver.chat.fillup", "§aYou filled your quiver with §f(?<flintAmount>.*) §aextra arrows!")
    private val clearedPattern by RepoPattern.pattern("data.quiver.chat.cleared", "§aCleared your quiver!")

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        val message = event.message.trimWhiteSpaceAndResets().removeResets()

        selectPattern.matchMatcher(message) {
            val arrow = group("arrow")
            currentArrow = Arrows.entries.find { arrow.contains(it.arrow) } ?: Arrows.NONE
            currentAmount = arrowAmount[currentArrow]?.toInt() ?: 0

            saveArrowType()
        }

        fillUpJaxPattern.matchMatcher(message) {
            val type = group("type")
            val amount = group("amount").formatNumber().toFloat()

            val filledUpType = Arrows.entries.find { type.contains(it.arrow) } ?: return

            val existingAmount = arrowAmount[filledUpType] ?: 0f
            val newAmount = existingAmount + amount
            arrowAmount[filledUpType] = newAmount

            saveArrowAmount()
        }

        fillUpPattern.matchMatcher(message) {
            val flintAmount = group("flintAmount").formatNumber().toFloat()
            val existingAmount = arrowAmount[Arrows.FLINT] ?: 0.0f
            val newAmount = existingAmount + flintAmount

            arrowAmount[Arrows.FLINT] = newAmount

            saveArrowAmount()
        }

        clearedPattern.matchMatcher(message) {
            currentAmount = 0
            arrowAmount.clear()

            saveArrowAmount()
        }
    }

    @SubscribeEvent
    fun onInventoryFullyLoaded(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (event.inventoryName != "Quiver") return

        // clear to prevent duplicates
        currentAmount = 0
        arrowAmount.clear()

        val stacks = event.inventoryItems
        for (stack in stacks.values) {
            val lore = stack.getLore()
            if (lore.isEmpty()) continue

            val arrow = stack.getInternalNameOrNull() ?: continue
            val amount = stack.stackSize

            val arrowType = Arrows.entries.find { arrow == it.internalName } ?: continue
            val arrowAmount = amount + (this.arrowAmount[arrowType] ?: 0.0f)

            this.arrowAmount[arrowType] = arrowAmount
        }

        saveArrowAmount()
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    // Inspired by SkyblockFeatures - https://github.com/MrFast-js/SkyblockFeatures/
    fun onPlaySound(event: PlaySoundEvent) {
        val holdingBow = InventoryUtils.getItemInHand()?.item is ItemBow

        if (event.soundName == "random.bow" && holdingBow) {
            val arrowType = currentArrow ?: return
            val arrowAmount = QuiverAPI.arrowAmount[arrowType] ?: return
            if (arrowAmount <= 0) return

            val infiniteQuiverLevel = InventoryUtils.getItemInHand()?.getEnchantments()?.get("infinite_quiver") ?: 0

            val amountToRemove = {
                when (Minecraft.getMinecraft().thePlayer.isSneaking && infiniteQuiverLevel > 0) {
                    true -> 1.0f
                    false -> {
                        when (infiniteQuiverLevel) {
                            in 1..10 -> infityQuiverLevelMultiplier * infiniteQuiverLevel
                            else -> 1.0f
                        }
                    }
                }
            }

            this.arrowAmount[arrowType] = (arrowAmount - amountToRemove()).coerceAtLeast(0.0f)

            saveArrowAmount()
        }
    }

    // Handle Storage data
    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val config = ProfileStorageData.profileSpecific ?: return
        currentArrow = config.arrows.currentArrow ?: return
        arrowAmount = config.arrows.arrowAmount ?: return
        currentAmount = arrowAmount[currentArrow]?.toInt() ?: 0
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        val config = ProfileStorageData.profileSpecific ?: return
        currentArrow = config.arrows.currentArrow ?: return
        arrowAmount = config.arrows.arrowAmount ?: return
        currentAmount = arrowAmount[currentArrow]?.toInt() ?: 0
    }

    private fun saveArrowType() {
        val config = ProfileStorageData.profileSpecific ?: return
        config.arrows.currentArrow = currentArrow
    }

    private fun saveArrowAmount() {
        val config = ProfileStorageData.profileSpecific ?: return
        config.arrows.arrowAmount = arrowAmount

        if (arrowAmount.isNotEmpty())
            currentAmount = arrowAmount[currentArrow]!!.toInt()
    }
}
