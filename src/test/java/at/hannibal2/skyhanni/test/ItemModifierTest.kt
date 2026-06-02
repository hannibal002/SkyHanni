package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.ItemUtils.hasEnchantGlint
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHypixelEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHotPotatoCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeModifier
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.hasArtOfPeace
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isRecombobulated
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

//? if >= 26.1
import org.junit.jupiter.api.Disabled

@ExtendWith(BootstrapExtension::class)
//? if >= 26.1 {
@Disabled(
    "ItemModifierTest uses 1.8.9 NBT test data. MC 26.1 requires data-pack-bound registry components " +
    "before ItemStack can be created, which is not available in the test environment.",
)
//?}
class ItemModifierTest {

    @Test
    fun testUpgradeLevelMasterStars() {
        val itemStack = TestExportTools.getTestData(TestExportTools.Item, "10starnecronhead")
        assert(!itemStack.isRecombobulated())
        assert(itemStack.getReforgeModifier() == "ancient")
        assert(itemStack.getItemUuid() == "2810b7fe-33af-4dab-bb41-b4815f5847af")
        assert(itemStack.hasEnchantGlint())
        assert(itemStack.getHotPotatoCount() == 15)
        assert(itemStack.getHypixelEnchantments()?.size == 11)
        assert(itemStack.hasArtOfPeace())
        //assert(itemStack.getDungeonStarCount() == 10)
    }
}
