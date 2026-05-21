package at.hannibal2.skyhanni.test.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.SkyHanniConfig
import at.hannibal2.skyhanni.events.garden.visitor.VisitorOpenEvent
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorTooltip
import at.hannibal2.skyhanni.features.garden.visitor.VisitorApi
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import net.minecraft.world.item.Items
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GardenVisitorTooltipTest {

    private lateinit var oldConfig: SkyHanniConfig

    @BeforeEach
    fun setUp() {
        oldConfig = SkyHanniMod.feature
        SkyHanniMod.feature = SkyHanniConfig()
        SkyHanniMod.feature.garden.visitors.inventory.exactAmountAndTime = false
        SkyHanniMod.feature.garden.visitors.rewardWarning.notifyInChat = false
        itemNameCache()["§9Enchanted Sugar Cane"] = "ENCHANTED_SUGAR_CANE".toInternalName()
    }

    @AfterEach
    fun tearDown() {
        itemNameCache().remove("§9Enchanted Sugar Cane")
        SkyHanniMod.feature = oldConfig
    }

    @Test
    fun `visitor tooltip parses copper line with heart suffix`() {
        val offerItem = ItemUtils.createItemStack(Items.GREEN_TERRACOTTA, "§aAccept Offer", spacemanLore)
        val visitor = VisitorApi.Visitor(
            visitorName = "§cSpaceman",
            status = VisitorApi.VisitorStatus.NEW,
            offer = VisitorApi.VisitorOffer(offerItem),
        )

        assertDoesNotThrow {
            GardenVisitorTooltip.onVisitorOpen(VisitorOpenEvent(visitor))
        }

        assertNotNull(visitor.pricePerCopper)
    }

    companion object {
        private val spacemanLore = listOf(
            "§7Items Required:",
            " §9Enchanted Sugar Cane §8x9,639",
            "",
            "§7Rewards:",
            " §8+§3100k §7Farming XP",
            " §8+§275 §7Garden Experience",
            " §8+§c150 Copper §d❤",
            " §cSpace Helmet",
            " Visitors' Gratitude §d❤",
            " Astronaut Minion Skin §d❤",
            "",
            "§cMissing items!",
            "",
            "§7§8This visitor accepts any quantity of",
            "§8the required items until you've given",
            "§8the full amount!",
        )

        @Suppress("UNCHECKED_CAST")
        private fun itemNameCache(): MutableMap<String, NeuInternalName?> {
            val field = NeuInternalName::class.java.getDeclaredField("itemNameCache")
            field.isAccessible = true
            return field.get(null) as MutableMap<String, NeuInternalName?>
        }
    }
}
