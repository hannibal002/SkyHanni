package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.FakePlayer
import at.hannibal2.skyhanni.utils.compat.EnchantmentsCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.fakePlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.random.Random

@SkyHanniModule(devOnly = true)
object TestFakePlayer : RenderableTestSuite.TestRenderable("fakeplayer") {


    private val fakePlayer1 by lazy {
        Renderable.fakePlayer(createFakePlayer(), followMouse = true)
    }

    private val fakePlayer2 by lazy {
        Renderable.fakePlayer(createFakePlayer(), followMouse = true)
    }

    private val fakePlayer3 by lazy {
        Renderable.fakePlayer(createFakePlayer(), followMouse = true)
    }

    private val helmetList = setOf(
        Items.IRON_HELMET,
        Items.GOLDEN_HELMET,
        Items.DIAMOND_HELMET,
        Items.LEATHER_HELMET,
        Items.CHAINMAIL_HELMET,
    )

    private val chestplateList = setOf(
        Items.IRON_CHESTPLATE,
        Items.GOLDEN_CHESTPLATE,
        Items.DIAMOND_CHESTPLATE,
        Items.LEATHER_CHESTPLATE,
        Items.CHAINMAIL_CHESTPLATE,
    )

    private val leggingsList = setOf(
        Items.IRON_LEGGINGS,
        Items.GOLDEN_LEGGINGS,
        Items.DIAMOND_LEGGINGS,
        Items.LEATHER_LEGGINGS,
        Items.CHAINMAIL_LEGGINGS,
    )

    private val bootsList = setOf(
        Items.IRON_BOOTS,
        Items.GOLDEN_BOOTS,
        Items.DIAMOND_BOOTS,
        Items.LEATHER_BOOTS,
        Items.CHAINMAIL_BOOTS,
    )

    private fun createRandomArmorPiece(armorPieces: Set<Item>): ItemStack = ItemStack(armorPieces.random()).also {
        if (Random.nextBoolean()) it.enchant(
            EnchantmentsCompat.PROTECTION.enchantment, 1,
        )
    }

    private fun createFakePlayer(): FakePlayer {
        val fakePlayer = FakePlayer()

        val helmet = createRandomArmorPiece(helmetList)
        val chestplate = createRandomArmorPiece(chestplateList)
        val leggings = createRandomArmorPiece(leggingsList)
        val boots = createRandomArmorPiece(bootsList)

        val armor = listOf(helmet, chestplate, leggings, boots)
        for (equipment in Inventory.EQUIPMENT_SLOT_MAPPING.values) {
            val armorOrdinal = equipment.ordinal - 2
            if (armorOrdinal < 0 || armorOrdinal > 3) continue
            fakePlayer.equipment.set(equipment, armor.reversed()[armorOrdinal])
        }

        return fakePlayer
    }

    override fun renderable(): Renderable {
        return with(Renderable) {
            horizontal(
                fakePlayer1,
                fakePlayer2,
                fakePlayer3,
            )
        }
    }

}
