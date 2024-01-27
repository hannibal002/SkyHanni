package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.jsonobjects.repo.ArrowTypeJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.ItemsJson
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
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
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpace
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemBow
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

private var infinityQuiverLevelMultiplier = 0.03f

object QuiverAPI {
    private val storage get() = ProfileStorageData.profileSpecific
    var currentArrow: ArrowType?
        get() = getArrowByNameOrNull(storage?.arrows?.currentArrow ?: "None")
        set(value) {
            storage?.arrows?.currentArrow = value?.toString() ?: return
        }
    var arrowAmount: MutableMap<ArrowType, Float>
        get() = storage?.arrows?.arrowAmount?.mapNotNull {
            val arrow = getArrowByNameOrNull(it.key.asInternalName()) ?: return@mapNotNull null
            arrow to it.value
        }?.toMap()?.toMutableMap() ?: mutableMapOf()
        set(value) {
            storage?.arrows?.arrowAmount = value.mapKeys {
                it.toString()
            }.toMutableMap()
        }
    var currentAmount: Int
        get() = arrowAmount[currentArrow]?.toInt() ?: 0
        set(value) {
            currentArrow?.let { arrow ->
                arrowAmount.let { amount ->
                    amount[arrow] = value.toFloat()
                }
            }
        }

    private var arrows: List<ArrowType> = listOf()

    private val SKELETON_MASTER_CHESTPLATE = "SKELETON_MASTER_CHESTPLATE".asInternalName()

    private val group = RepoPattern.group("data.quiver")
    private val chatGroup = group.group("chat")
    private val selectPattern by chatGroup.pattern("select", "§aYou set your selected arrow type to §f(?<arrow>.*)§a!")
    private val fillUpJaxPattern by chatGroup.pattern(
        "fillupjax",
        "(§.)*Jax forged (§.)*(?<type>.*)(§.)* x(?<amount>.*)( (§.)*for (§.)*(?<coins>.*) Coins)?(§.)*!"
    )
    private val fillUpPattern by chatGroup.pattern(
        "fillup",
        "§aYou filled your quiver with §f(?<flintAmount>.*) §aextra arrows!"
    )
    private val clearedPattern by chatGroup.pattern("cleared", "§aCleared your quiver!")
    private val arrowResetPattern by chatGroup.pattern("arrowreset", "§cYour favorite arrow has been reset!")
    private val addedToQuiverPattern by chatGroup.pattern(
        "addedtoquiver",
        "(§.)*You've added (§.)*(?<type>.*) x(?<amount>.*) (§.)*to your quiver!"
    )

    // Bows that don't use the players arrows
    private val fakeBowsPattern by group.pattern("fakebows", "^BOSS_SPIRIT_BOW$")
    private val quiverInventoryNamePattern by group.pattern("quivername", "^Quiver$")

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        val message = event.message.trimWhiteSpace().removeResets()

        selectPattern.matchMatcher(message) {
            val arrow = group("arrow")
            currentArrow = getArrowByNameOrNull(arrow) ?: return
            currentAmount = arrowAmount[currentArrow]?.toInt() ?: 0

            return
        }

        fillUpJaxPattern.matchMatcher(message) {
            val type = group("type")
            val amount = group("amount").formatNumber().toFloat()

            val filledUpType = getArrowByNameOrNull(type) ?: return

            arrowAmount.merge(filledUpType, amount, Float::plus)
            return
        }

        fillUpPattern.matchMatcher(message) {
            val flintAmount = group("flintAmount").formatNumber().toFloat()
            val existingAmount = arrowAmount[getArrowByNameOrNull("ARROW".asInternalName())] ?: 0f
            val newAmount = existingAmount + flintAmount

            arrowAmount[getArrowByNameOrFlint("ARROW".asInternalName())] = newAmount

            return
        }

        addedToQuiverPattern.matchMatcher(message) {
            val type = group("type")
            val amount = group("amount").formatNumber().toFloat()

            val filledUpType = getArrowByNameOrNull(type) ?: return

            val existingAmount = arrowAmount[filledUpType] ?: 0f
            val newAmount = existingAmount + amount
            arrowAmount[filledUpType] = newAmount

            return
        }

        clearedPattern.matchMatcher(message) {
            currentAmount = 0
            arrowAmount.clear()

            return
        }

        arrowResetPattern.matchMatcher(message) {
            currentArrow = getArrowByNameOrNull("NONE".asInternalName()) ?: return
            currentAmount = 0

            return
        }
    }

    @SubscribeEvent
    fun onInventoryFullyLoaded(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (!quiverInventoryNamePattern.matches(event.inventoryName)) return

        // clear to prevent duplicates
        currentAmount = 0
        arrowAmount.clear()

        val stacks = event.inventoryItems
        for (stack in stacks.values) {
            val lore = stack.getLore()
            if (lore.isEmpty()) continue

            val arrow = stack.getInternalNameOrNull() ?: continue

            val arrowType = getArrowByNameOrNull(arrow) ?: continue
            val arrowAmount = stack.stackSize + this.arrowAmount.getOrDefault(arrowType, 0.0f)

            this.arrowAmount[arrowType] = arrowAmount
        }
    }

    /*
     Modified method to remove arrows from SkyblockFeatures QuiverOverlay
     Original method source:
     https://github.com/MrFast-js/SkyblockFeatures/blob/ae4bf0b91ed0fb17114d9cdaccaa9aef9a6c8d01/src/main/java/mrfast/sbf/features/overlays/QuiverOverlay.java#L127

     Changes made:
     - Added "fake bows" check
     - Added "infinite quiver" check
     - Added "sneaking" check
     - Added "bow sound distance" check
     - Added "skeleton master chestplate" check
    */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onPlaySound(event: PlaySoundEvent) {
        if (!isEnabled()) return
        if (event.soundName != "random.bow") return

        val holdingBow = InventoryUtils.getItemInHand()?.item is ItemBow
            && !fakeBowsPattern.matches(InventoryUtils.getItemInHand()?.getInternalNameOrNull()?.asString() ?: "")

        if (!holdingBow) return

        // check if sound location is more than configAmount block away from player
        val soundLocation = event.distanceToPlayer
        if (soundLocation > SkyHanniMod.feature.dev.bowSoundDistance) return

        val arrowType = currentArrow ?: return
        val arrowAmount = QuiverAPI.arrowAmount[arrowType] ?: return
        if (arrowAmount <= 0) return

        if (InventoryUtils.getChestplate()
                // The chestplate has the ability to not use arrows
                // https://hypixel-skyblock.fandom.com/wiki/Skeleton_Master_Armor
                ?.getInternalNameOrNull() == SKELETON_MASTER_CHESTPLATE
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
    }

    fun hasBowInInventory(): Boolean {
        return InventoryUtils.getItemsInOwnInventory().any { it.item is ItemBow }
    }

    fun getArrowByNameOrNull(name: String): ArrowType? {
        return arrows.firstOrNull { it.arrow == name }
    }

    fun getArrowByNameOrNull(internalName: NEUInternalName): ArrowType? {
        return arrows.firstOrNull { it.internalName == internalName.asString() }
    }

    fun getArrowByNameOrFlint(name: String): ArrowType {
        return getArrowByNameOrNull(name) ?: getArrowByNameOrNull("ARROW".asInternalName())!!
    }

    fun getArrowByNameOrFlint(internalName: NEUInternalName): ArrowType {
        return getArrowByNameOrNull(internalName) ?: getArrowByNameOrNull("ARROW".asInternalName())!!
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && storage != null


    // Load arrows from repo
    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val itemData = event.getConstant<ItemsJson>("Items")
        infinityQuiverLevelMultiplier = itemData.enchant_multiplier["infinite_quiver"] ?: 0.03f

        val arrowData = event.getConstant<ArrowTypeJson>("ArrowTypes")
        arrows = arrowData.arrows.map { ArrowType(it.value.arrow, it.key) }
    }
}
