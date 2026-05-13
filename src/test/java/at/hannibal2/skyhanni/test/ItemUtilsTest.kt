package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.CachedItemData.Companion.cachedData
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ItemUtilsTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun bootstrapMinecraftRegistries() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
    }

    // <editor-fold desc="Item Amount Test">
    private val items: MutableMap<String, Pair<String, Int>> = mutableMapOf(
        "§5Hoe of Greatest Tilling" to Pair("§5Hoe of Greatest Tilling", 1),
        "§fSilver medal §8x2" to Pair("§fSilver medal", 2),
        "§aJacob's Ticket §8x32" to Pair("§aJacob's Ticket", 32),
        "§9Delicate V" to Pair("§9Delicate V", 1),
        "  §81x §9Enchanted Sugar Cane" to Pair("§9Enchanted Sugar Cane", 1),
        "§6Gold medal" to Pair("§6Gold medal", 1),
        " §8+§319k §7Farming XP" to Pair("§7Farming XP", 19_000),
        " §8+§215 §7Garden Experience" to Pair("§7Garden Experience", 15),
        " §8+§c21 Copper" to Pair("Copper", 21),
        " §8+§b10 Bits" to Pair("Bits", 10),
        " §8+§37.2k §7Farming XP" to Pair("§7Farming XP", 7_200),
    )
    // </editor-fold>

    @Test
    fun testReadItemAmount() {
        for ((itemString, expected) in items) {
            val results = ItemUtils.readItemAmount(itemString)
            assert(results != null) {
                "Could not read item '$itemString'"
            }
            assert(results?.equals(expected) == true) {
                "'${results.toString()}' does not match '$expected'"
            }
        }
    }

    // <editor-fold desc="Item Category & Rarity Test">
    @Test
    fun testItemWithoutCategory() {
        assertItemCategoryAndRarity(
            item = Items.CAKE,
            displayName = "§dComplete Century Cake Bundle",
            lore = categorylessLore,
            internalName = "FULL_CENTURY_CAKE_PACK",
            expectedRarity = LorenzRarity.MYTHIC,
            expectedCategory = ItemCategory.NONE,
        )
    }

    @Test
    fun testRecombobulatedItemWithoutCategory() {
        assertItemCategoryAndRarity(
            item = Items.CAKE,
            displayName = "§bComplete Century Cake Bundle",
            lore = categorylessLore.dropLast(1) + "a DIVINE a",
            internalName = "FULL_CENTURY_CAKE_PACK",
            expectedRarity = LorenzRarity.DIVINE,
            expectedCategory = ItemCategory.NONE,
        )
    }

    @Test
    fun testItemWithCategory() {
        assertItemCategoryAndRarity(
            item = Items.DIAMOND_HOE,
            displayName = "§5Euclid's Wheat Hoe Mk. III",
            lore = categoryLore,
            internalName = "THEORETICAL_HOE_WHEAT_3",
            expectedRarity = LorenzRarity.EPIC,
            expectedCategory = ItemCategory.FARMING_TOOL,
        )
    }

    @Test
    fun testRecombobulatedItemWithCategory() {
        assertItemCategoryAndRarity(
            item = Items.DIAMOND_HOE,
            displayName = "§5Euclid's Wheat Hoe Mk. III",
            lore = categoryLore.dropLast(1) + "a LEGENDARY FARMING TOOL a",
            internalName = "THEORETICAL_HOE_WHEAT_3",
            expectedRarity = LorenzRarity.LEGENDARY,
            expectedCategory = ItemCategory.FARMING_TOOL,
        )
    }

    @Test
    fun testDungeonItemWithoutCategory() {
        assertItemCategoryAndRarity(
            item = Items.STICK,
            displayName = "§6Necron's Handle",
            lore = dungeonCategorylessLore,
            internalName = "NECRON_HANDLE",
            expectedRarity = LorenzRarity.LEGENDARY,
            expectedCategory = ItemCategory.NONE,
        )
    }

    @Test
    fun testRecombobulatedDungeonItemWithoutCategory() {
        assertItemCategoryAndRarity(
            item = Items.STICK,
            displayName = "§6Necron's Handle",
            lore = dungeonCategorylessLore.dropLast(1) + "a MYTHIC DUNGEON ITEM a",
            internalName = "NECRON_HANDLE",
            expectedRarity = LorenzRarity.MYTHIC,
            expectedCategory = ItemCategory.NONE,
        )
    }

    @Test
    fun testDungeonItemWithCategory() {
        assertItemCategoryAndRarity(
            item = Items.IRON_SWORD,
            displayName = "§6Hyperion",
            lore = dungeonCategoryLore,
            internalName = "HYPERION",
            expectedRarity = LorenzRarity.LEGENDARY,
            expectedCategory = ItemCategory.SWORD,
        )
    }

    @Test
    fun testRecombobulatedDungeonItemWithCategory() {
        assertItemCategoryAndRarity(
            item = Items.IRON_SWORD,
            displayName = "§6Hyperion",
            lore = dungeonCategoryLore.dropLast(1) + "a MYTHIC DUNGEON SWORD a",
            internalName = "HYPERION",
            expectedRarity = LorenzRarity.MYTHIC,
            expectedCategory = ItemCategory.SWORD,
        )
    }

    private fun assertItemCategoryAndRarity(
        item: Item,
        displayName: String,
        lore: List<String>,
        internalName: String,
        expectedRarity: LorenzRarity,
        expectedCategory: ItemCategory,
    ) {
        val stack = ItemUtils.createItemStack(item, displayName, lore)
        stack.cachedData.lastInternalName = internalName.toInternalName()
        stack.cachedData.lastInternalNameFetchTime = SimpleTimeMark.now()

        assertEquals(expectedRarity, stack.getItemRarityOrNull())
        assertEquals(expectedCategory, stack.getItemCategoryOrNull())
    }

    private val categorylessLore = listOf(
        "Consumable",
        "",
        "Consume to unwrap a century cake",
        "of every color!",
        "",
        "Right-click to consume!",
        "",
        "MYTHIC",
    )

    private val categoryLore = listOf(
        "Farming Tool",
        "",
        "Wheat Fortune: +4",
        "Farming Wisdom: +1",
        "",
        "Level 1 -> 2   (0/1k)",
        "                          0%",
        "",
        "This item can be reforged!",
        "\u2763 Requires Farming Skill 20.",
        "EPIC FARMING TOOL",
    )

    private val dungeonCategorylessLore = listOf(
        "Right-click to view recipes!",
        "",
        "LEGENDARY DUNGEON ITEM",
    )

    private val dungeonCategoryLore = listOf(
        "Gear Score: 615",
        "Damage: +260",
        "Strength: +150",
        "Ferocity: +30",
        "Intelligence: +350",
        "Gemstones: [\u270e] [\u2694]",
        "",
        "Deals +50% damage to \u2620 Wither mobs.",
        "Grants +1 \u2741 Damage and +2 \u270e",
        "Intelligence per Catacombs level.",
        "",
        "Right-click to use your class ability!",
        "",
        "This item can be reforged!",
        "\u2763 Requires The Catacombs Floor VII Completion.",
        "LEGENDARY DUNGEON SWORD",
    )
    // </editor-fold>
}
