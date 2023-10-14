package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.isEnchanted
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHotPotatoCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeName
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.hasArtOfPeace
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isRecombobulated
import org.junit.jupiter.api.Test

class SkyHanniTest {

    /* BIG FAT TODO: find out how NEU has mutliple test files, and borrow their strategy.
     * this is a hotfix so that all JUnit-reliant test code remains intact without
     * everyone's jar files failing to compile because of some BS double block
     * registration nonsense during compiletime.
     */

    //ItemUtilsTest
    val items: MutableMap<String, Pair<String, Int>> = mutableMapOf(
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

    //ItemModifierTest
    @Test
    fun testUpgradeLevelMasterStars() {
        val itemStack = TestExportTools.getTestData(TestExportTools.Item, "10starnecronhead")
        assert(!itemStack.isRecombobulated())
        assert(itemStack.getReforgeName() == "ancient")
        assert(itemStack.getItemUuid() == "2810b7fe-33af-4dab-bb41-b4815f5847af")
        assert(itemStack.isEnchanted())
        assert(itemStack.getHotPotatoCount() == 15)
        assert(itemStack.getEnchantments()?.size == 11)
        assert(itemStack.hasArtOfPeace())
//        assert(itemStack.getDungeonStarCount() == 10)
    }
}