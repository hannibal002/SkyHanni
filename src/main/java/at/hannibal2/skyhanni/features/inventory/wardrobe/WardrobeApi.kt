package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValueCalculator
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AnimatedSkinUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getExtraAttributes
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHelmetSkin
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat.Companion.isStainedGlassPane
import at.hannibal2.skyhanni.utils.compat.DyeCompat
import at.hannibal2.skyhanni.utils.compat.DyeCompat.Companion.isDye
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.renderables.animated.framed.ItemStackAnimatedFrame
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.annotations.Expose
import net.minecraft.world.item.ItemStack
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object WardrobeApi {

    val storage get() = ProfileStorageData.profileSpecific?.wardrobe

    private val patternGroup = RepoPattern.group("inventory.wardrobe")

    /**
     * REGEX-TEST: Wardrobe (2/2)
     */
    private val inventoryPattern by patternGroup.pattern(
        "inventory.name",
        "Wardrobe \\((?<currentPage>\\d+)/\\d+\\)",
    )

    /**
     * REGEX-TEST: §7Slot 4: §aEquipped
     */
    private val equippedSlotPattern by patternGroup.pattern(
        "equippedslot",
        "§7Slot \\d+: §aEquipped",
    )

    const val FIRST_SLOT = 36
    private const val FIRST_HELMET_SLOT = 0
    private const val FIRST_CHESTPLATE_SLOT = 9
    private const val FIRST_LEGGINGS_SLOT = 18
    private const val FIRST_BOOTS_SLOT = 27
    const val MAX_SLOT_PER_PAGE = 9
    const val MAX_PAGES = 3

    var slots = listOf<WardrobeSlot>()

    internal fun emptyArmor(): List<ItemStack?> = listOf(null, null, null, null)

    var currentSlot: Int?
        get() = storage?.currentSlot
        set(value) {
            storage?.currentSlot = value
        }

    var currentPage: Int? = null
    val wardrobeDetector = InventoryDetector(inventoryPattern)

    init {
        val list = mutableListOf<WardrobeSlot>()
        var id = 0

        for (page in 1..MAX_PAGES) {
            for (slot in 0 until MAX_SLOT_PER_PAGE) {
                val inventorySlot = FIRST_SLOT + slot
                val helmetSlot = FIRST_HELMET_SLOT + slot
                val chestplateSlot = FIRST_CHESTPLATE_SLOT + slot
                val leggingsSlot = FIRST_LEGGINGS_SLOT + slot
                val bootsSlot = FIRST_BOOTS_SLOT + slot
                list.add(WardrobeSlot(++id, page, inventorySlot, helmetSlot, chestplateSlot, leggingsSlot, bootsSlot))
            }
        }
        slots = list
    }

    private fun String.buildTextureItemStack(): ItemStack {
        val (uuid, texture) = this.split(":")
        return ItemUtils.createSkull("Animated Armor", uuid, texture)
    }

    // TODO at some point fix this idk
    private val skinsNoAnimation = setOf(
        "FERMENTO_ULTIMATE",
    )

    fun getArmorAnimatedFrames(stack: ItemStack): List<ItemStackAnimatedFrame>? {
        val skinInternalName = stack.getHelmetSkin()?.asString() ?: return null
        if (skinInternalName in skinsNoAnimation) return null
        val animJson = AnimatedSkinUtils.armorSkins[skinInternalName] ?: return null
        // Variant skins have multiple textures but no animation tick rate.
        // Pick the single texture that matches the item's stored variant index.
        if (animJson.ticks <= 0 && animJson.textures.size > 1) {
            val variantIndex = stack.getExtraAttributes()?.let { AnimatedSkinUtils.getVariantIndexOrNull(it) } ?: 0
            val texture = animJson.textures.getOrElse(variantIndex) { animJson.textures.first() }
            return listOf(ItemStackAnimatedFrame(texture.buildTextureItemStack(), 0))
        }
        return animJson.textures.map { ItemStackAnimatedFrame(it.buildTextureItemStack(), animJson.ticks) }
    }

    private fun getWardrobeItem(itemStack: ItemStack?) =
        if (itemStack == null || itemStack.isStainedGlassPane()) null else itemStack

    private fun getWardrobeSlotFromId(id: Int?) = slots.find { it.id == id }

    fun inWardrobe() = wardrobeDetector.isInside()

    fun createPriceLore(slot: WardrobeSlot) = buildList {
        if (slot.isEmpty()) return@buildList
        add("§aEstimated Armor Value:")
        var totalPrice = 0.0
        for (stack in slot.armor.filterNotNull().filter { it.getInternalNameOrNull() != null }) {
            EstimatedItemValueCalculator.getTotalPrice(stack)?.let { price ->
                add("  §7- ${stack.hoverName.formattedTextCompatLeadingWhiteLessResets()}: §6${price.shortFormat()}")
                totalPrice += price
            }
        }
        if (totalPrice != 0.0) add(" §aTotal Value: §6§l${totalPrice.shortFormat()} coins")
    }

    @HandleEvent(priority = HandleEvent.HIGH, onlyOnSkyblock = true)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        inventoryPattern.matchMatcher(event.inventoryName) {
            currentPage = group("currentPage").formatInt()
        } ?: return

        val itemsList = event.inventoryItems

        val allGrayDye = slots.all {
            itemsList[it.inventorySlot]?.isDye(DyeCompat.GRAY) == true || !it.isInCurrentPage()
        }

        if (allGrayDye) {
            val allSlotsEmpty = slots.filter { it.isInCurrentPage() }.all { slot ->
                (slot.inventorySlots.all { getWardrobeItem(itemsList[it]) == null })
            }
            if (allSlotsEmpty) {
                for (slot in slots.filter { it.isInCurrentPage() }) {
                    slot.getData()?.armor = emptyArmor()
                }
            } else return
        }

        val foundCurrentSlot = processSlots(slots, itemsList)
        if (!foundCurrentSlot && getWardrobeSlotFromId(currentSlot)?.page == currentPage) {
            currentSlot = null
        }
    }

    private fun processSlots(slots: List<WardrobeSlot>, itemsList: Map<Int, ItemStack>): Boolean {
        var foundCurrentSlot = false

        for (slot in slots.filter { it.isInCurrentPage() }) {
            slot.getData()?.armor = listOf(
                getWardrobeItem(itemsList[slot.helmetSlot]),
                getWardrobeItem(itemsList[slot.chestplateSlot]),
                getWardrobeItem(itemsList[slot.leggingsSlot]),
                getWardrobeItem(itemsList[slot.bootsSlot]),
            )
            if (equippedSlotPattern.matches(itemsList[slot.inventorySlot]?.hoverName.formattedTextCompatLeadingWhiteLessResets())) {
                currentSlot = slot.id
                foundCurrentSlot = true
            }
            slot.locked = (itemsList[slot.inventorySlot]?.isDye(DyeCompat.RED) == true)
            if (slot.locked) slots.forEach { if (it.id > slot.id) it.locked = true }
        }

        return foundCurrentSlot
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        DelayedRun.runDelayed(250.milliseconds) {
            if (!wardrobeDetector.isInside()) {
                currentPage = null
            }
        }
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Wardrobe")
        event.addIrrelevant {
            if (slots.isEmpty()) {
                add("No slots")
                return@addIrrelevant
            }

            for (slot in slots) {
                val slotInfo = buildString {
                    append("Slot ${slot.id}")
                    if (slot.favorite) append(" - Favorite: true")
                }
                if (slot.locked) add("$slotInfo is locked")
                else if (slot.isEmpty()) add("$slotInfo is empty")
                else {
                    add(slotInfo)
                    setOf("Helmet", "Chestplate", "Leggings", "Boots").forEachIndexed { id, armorName ->
                        slot.getData()?.armor?.get(id)?.hoverName?.formattedTextCompatLeadingWhiteLessResets()?.let { name ->
                            add("   $armorName: $name")
                        }
                    }
                }
            }
        }
    }

    class WardrobeData(
        @Expose val id: Int,
        @Expose var armor: List<ItemStack?>,
        @Expose var locked: Boolean,
        @Expose var favorite: Boolean,
    )
}
