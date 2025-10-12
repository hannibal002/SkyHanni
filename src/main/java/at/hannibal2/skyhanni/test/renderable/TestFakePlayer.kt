package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.FakePlayer
import at.hannibal2.skyhanni.utils.compat.EnchantmentsCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.fakePlayer
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import kotlin.random.Random
//#if MC > 1.8.9
//$$ import net.minecraft.entity.player.PlayerInventory
//#endif

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
        Items.iron_helmet,
        Items.golden_helmet,
        Items.diamond_helmet,
        Items.leather_helmet,
        Items.chainmail_helmet,
    )

    private val chestplateList = setOf(
        Items.iron_chestplate,
        Items.golden_chestplate,
        Items.diamond_chestplate,
        Items.leather_chestplate,
        Items.chainmail_chestplate,
    )

    private val leggingsList = setOf(
        Items.iron_leggings,
        Items.golden_leggings,
        Items.diamond_leggings,
        Items.leather_leggings,
        Items.chainmail_leggings,
    )

    private val bootsList = setOf(
        Items.iron_boots,
        Items.golden_boots,
        Items.diamond_boots,
        Items.leather_boots,
        Items.chainmail_boots,
    )

    private fun createRandomArmorPiece(armorPieces: Set<Item>): ItemStack = ItemStack(armorPieces.random()).also {
        if (Random.nextBoolean()) it.addEnchantment(
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
        //#if MC < 1.21.5
        fakePlayer.inventory.armorInventory = armor.toTypedArray()
        //#else
        //$$ for (equipment in PlayerInventory.EQUIPMENT_SLOTS.values) {
        //$$     val armorOrdinal = equipment.ordinal - 2
        //$$     if (armorOrdinal < 0 || armorOrdinal > 3) continue
        //$$     fakePlayer.inventory.equipment.put(equipment, armor.reversed()[armorOrdinal])
        //$$ }
        //#endif

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
