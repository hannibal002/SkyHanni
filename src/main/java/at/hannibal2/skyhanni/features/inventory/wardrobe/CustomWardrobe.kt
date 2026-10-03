package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.FakePlayer
import at.hannibal2.skyhanni.utils.ItemUtils.removeEnchants
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.fakePlayer
import net.minecraft.world.entity.player.Inventory
import java.awt.Color

object CustomWardrobe : AbstractCustomWardrobe(ArmorWardrobeApi, "Custom Wardrobe") {

    override var onlyFavorites: Boolean
        get() = config.onlyFavorites
        set(value) {
            config.onlyFavorites = value
        }

    override fun isFeatureEnabled() = config.enabled

    override fun jumpToConfig() {
        config::enabled.jumpToEditor()
    }

    override fun createSlotContentRenderable(
        slot: WardrobeSlot,
        playerWidth: Double,
        containerHeight: Int,
        containerWidth: Int,
    ): Renderable {
        val fakePlayer = FakePlayer.fromLocalPlayerOrThrow()
        var scale = playerWidth

        for (equipment in Inventory.EQUIPMENT_SLOT_MAPPING.values) {
            val armorOrdinal = equipment.ordinal - 2
            if (armorOrdinal !in 0..3) continue
            var stack = slot.armor.reversed()[armorOrdinal]?.copy()?.removeEnchants()
            if (stack == null) stack = SafeItemStack.EMPTY
            fakePlayer.equipment.set(equipment, stack)
        }

        val playerColor = if (!slot.isInCurrentPage()) {
            scale *= 0.9
            Color.GRAY.addAlpha(100)
        } else null

        return Renderable.fakePlayer(
            fakePlayer,
            followMouse = config.eyesFollowMouse,
            width = containerWidth,
            height = containerHeight,
            entityScale = scale.toInt(),
            padding = 0,
            color = playerColor,
        )
    }
}
