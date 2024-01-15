package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.jsonobjects.repo.ArrowTypeJson
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
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpaceAndResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemBow
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

private var infinityQuiverLevelMultiplier = 0.03f

object QuiverAPI {
    var currentArrow: ArrowType? = null
    var currentAmount: Int = 0
    var arrowAmount: MutableMap<ArrowType, Float> = mutableMapOf()
    private var arrows: List<ArrowType> = listOf()

    private val fakeBows = listOf("BOSS_SPIRIT_BOW".asInternalName())

    private val group = RepoPattern.group("data.quiver.chat")
    private val selectPattern by group.pattern("select", "§aYou set your selected arrow type to §f(?<arrow>.*)§a!")
    private val fillUpJaxPattern by group.pattern(
        "fillupjax",
        "(§.)*Jax forged (§.)*(?<type>.*)(§.)* x(?<amount>.*)( (§.)*for (§.)*(?<coins>.*) Coins)?(§.)*!"
    )
    private val fillUpPattern by group.pattern(
        "fillup",
        "§aYou filled your quiver with §f(?<flintAmount>.*) §aextra arrows!"
    )
    private val clearedPattern by group.pattern("cleared", "§aCleared your quiver!")
    private val arrowResetPattern by group.pattern("arrowreset", "§cYour favorite arrow has been reset!")
    private val addedToQuiverPattern by group.pattern(
        "addedtoquiver",
        "(§.)*You've added (§.)*(?<type>.*) x(?<amount>.*) (§.)*to your quiver!"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        val message = event.message.trimWhiteSpaceAndResets()

        selectPattern.matchMatcher(message) {
            val arrow = group("arrow")
            currentArrow = getByNameOrNull(arrow) ?: return
            currentAmount = arrowAmount[currentArrow]?.toInt() ?: 0

            return saveArrowType()
        }

        fillUpJaxPattern.matchMatcher(message) {
            val type = group("type")
            val amount = group("amount").formatNumber().toFloat()

            val filledUpType = getByNameOrNull(type) ?: return

            val existingAmount = arrowAmount[filledUpType] ?: 0f
            val newAmount = existingAmount + amount
            arrowAmount[filledUpType] = newAmount

            return saveArrowAmount()
        }

        fillUpPattern.matchMatcher(message) {
            val flintAmount = group("flintAmount").formatNumber().toFloat()
            val existingAmount = arrowAmount[getByNameOrNull("ARROW".asInternalName())] ?: 0f
            val newAmount = existingAmount + flintAmount

            arrowAmount[getByNameOrFlint("ARROW".asInternalName())] = newAmount

            return saveArrowAmount()
        }

        addedToQuiverPattern.matchMatcher(message) {
            val type = group("type")
            val amount = group("amount").formatNumber().toFloat()

            val filledUpType = getByNameOrNull(type) ?: return

            val existingAmount = arrowAmount[filledUpType] ?: 0f
            val newAmount = existingAmount + amount
            arrowAmount[filledUpType] = newAmount

            return saveArrowAmount()
        }

        clearedPattern.matchMatcher(message) {
            currentAmount = 0
            arrowAmount.clear()

            return saveArrowAmount()
        }

        arrowResetPattern.matchMatcher(message) {
            currentArrow = getByNameOrNull("NONE".asInternalName()) ?: return
            currentAmount = 0

            saveArrowType()
            return saveArrowAmount()
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

            val arrowType = getByNameOrNull(arrow) ?: continue
            val arrowAmount = stack.stackSize + this.arrowAmount.getOrDefault(arrowType,0.0f) 

            this.arrowAmount[arrowType] = arrowAmount
        }

        saveArrowAmount()
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    // Inspired by SkyblockFeatures - https://github.com/MrFast-js/SkyblockFeatures/
    fun onPlaySound(event: PlaySoundEvent) {
        val holdingBow = InventoryUtils.getItemInHand()?.item is ItemBow && !fakeBows.contains(
            InventoryUtils.getItemInHand()?.getInternalNameOrNull()
        )

        // check if sound location is more than configAmount block away from player
        val soundLocation = event.distanceToPlayer
        if (soundLocation > SkyHanniMod.feature.dev.bowSoundDistance) return

        if (event.soundName == "random.bow" && holdingBow) {
            val arrowType = currentArrow ?: return
            val arrowAmount = QuiverAPI.arrowAmount[arrowType] ?: return
            if (arrowAmount <= 0) return

            if (InventoryUtils.getChestplate()
                    ?.getInternalNameOrNull() == "SKELETON_MASTER_CHESTPLATE".asInternalName()
            ) return

            val infiniteQuiverLevel = InventoryUtils.getItemInHand()?.getEnchantments()?.get("infinite_quiver") ?: 0

            val amountToRemove = {
                when (Minecraft.getMinecraft().thePlayer.isSneaking) {
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

    fun hasBowInInvetory(): Boolean {
        return InventoryUtils.getItemsInOwnInventory().any { it.item is ItemBow }
    }

    fun getByNameOrNull(name: String): ArrowType? {
        return arrows.firstOrNull { it.arrow == name }
    }

    fun getByNameOrNull(internalName: NEUInternalName): ArrowType? {
        return arrows.firstOrNull { it.internalName == internalName.asString() }
    }

    fun getByNameOrFlint(name: String): ArrowType {
        return getByNameOrNull(name) ?: getByNameOrNull("ARROW".asInternalName())!!
    }

    fun getByNameOrFlint(internalName: NEUInternalName): ArrowType {
        return getByNameOrNull(internalName) ?: getByNameOrNull("ARROW".asInternalName())!!
    }


    // Handle Storage data
    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val itemData = event.getConstant<ItemsJson>("Items")
        infinityQuiverLevelMultiplier = itemData.enchant_multiplier["infinite_quiver"] ?: 0.03f

        val arrowData = event.getConstant<ArrowTypeJson>("ArrowTypes")
        arrows = arrowData.arrows.map { ArrowType(it.value.arrow, it.key) }
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val config = ProfileStorageData.profileSpecific ?: return
        currentArrow = getByNameOrNull(config.arrows.currentArrow)
        arrowAmount = config.arrows.arrowAmount.map {
            val arrow = getByNameOrNull(it.key.asInternalName()) ?: return@map null
            arrow to it.value
        }.filterNotNull().toMap().toMutableMap()
        currentAmount = arrowAmount[currentArrow]?.toInt() ?: 0
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        val config = ProfileStorageData.profileSpecific ?: return
        currentArrow = getByNameOrNull(config.arrows.currentArrow)
        arrowAmount = config.arrows.arrowAmount.map {
            val arrow = getByNameOrNull(it.key.asInternalName()) ?: return@map null
            arrow to it.value
        }.filterNotNull().toMap().toMutableMap()
        currentAmount = arrowAmount[currentArrow]?.toInt() ?: 0
    }

    private fun saveArrowType() {
        val config = ProfileStorageData.profileSpecific ?: return
        config.arrows.currentArrow = currentArrow.toString()
    }

    private fun saveArrowAmount() {
        val config = ProfileStorageData.profileSpecific ?: return

        config.arrows.arrowAmount = arrowAmount.map {
            it.key.toString() to it.value
        }.toMap().toMutableMap()

        if (arrowAmount.isNotEmpty())
            currentAmount = arrowAmount[currentArrow]?.toInt() ?: 0
    }
}
