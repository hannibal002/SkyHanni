package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.data.jsonobjects.repo.ItemsJson
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
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

private var infinityQuiverLevelMultiplier = 0.03f

object QuiverAPI {
    var currentArrow: QuiverArrowType? = null
    var currentAmount: Int = 0
    var arrowAmount: MutableMap<QuiverArrowType, Float> = mutableMapOf()

    private val group = RepoPattern.group("data.quiver.chat")
    private val selectPattern by group.pattern("select", "§aYou set your selected arrow type to §f(?<arrow>.*)§a!")
    private val fillUpJaxPattern by group.pattern(
        "fillupjax",
        "§aJax forged §f(?<type>.*)§8 x(?<amount>.*) §afor §6(?<coins>.*) Coins§a!"
    )
    private val fillUpPattern by group.pattern(
        "fillup",
        "§aYou filled your quiver with §f(?<flintAmount>.*) §aextra arrows!"
    )
    private val clearedPattern by group.pattern("cleared", "§aCleared your quiver!")

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        val message = event.message.trimWhiteSpaceAndResets().removeResets()

        selectPattern.matchMatcher(message) {
            val arrow = group("arrow")
            currentArrow = QuiverArrowType.entries.find { arrow.contains(it.arrow) } ?: QuiverArrowType.NONE
            currentAmount = arrowAmount[currentArrow]?.toInt() ?: 0

            saveArrowType()
        }

        fillUpJaxPattern.matchMatcher(message) {
            val type = group("type")
            val amount = group("amount").formatNumber().toFloat()

            val filledUpType = QuiverArrowType.entries.find { type.contains(it.arrow) } ?: return

            val existingAmount = arrowAmount[filledUpType] ?: 0f
            val newAmount = existingAmount + amount
            arrowAmount[filledUpType] = newAmount

            saveArrowAmount()
        }

        fillUpPattern.matchMatcher(message) {
            val flintAmount = group("flintAmount").formatNumber().toFloat()
            val existingAmount = arrowAmount[QuiverArrowType.FLINT] ?: 0.0f
            val newAmount = existingAmount + flintAmount

            arrowAmount[QuiverArrowType.FLINT] = newAmount

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
        if (arrowAmount.isNotEmpty())
            arrowAmount.clear()

        val stacks = event.inventoryItems
        for (stack in stacks.values) {
            val lore = stack.getLore()
            if (lore.isEmpty()) continue

            val arrow = stack.getInternalNameOrNull() ?: continue
            val amount = stack.stackSize

            val arrowType = QuiverArrowType.entries.find { arrow == it.internalName } ?: continue
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
                            in 1..10 -> 1 - (infinityQuiverLevelMultiplier * infiniteQuiverLevel)
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
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<ItemsJson>("Items")
        infinityQuiverLevelMultiplier = data.enchant_multiplier["infinite_quiver"] ?: 0.03f
    }

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

        if (arrowAmount != null)
            config.arrows.arrowAmount = arrowAmount

        if (arrowAmount.isNotEmpty())
            currentAmount = arrowAmount[currentArrow]?.toInt() ?: 0
    }
}
