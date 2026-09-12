package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.compat.EntityCompat.getStandHelmet
import net.minecraft.world.entity.decoration.ArmorStand

// TODO: Figure out some way to hide the sugar cane itself, must be visual only change
@SkyHanniModule
object MagicJellyBeanHider {

    private val jellyBeanSkullTexture by SkullTextureHolder.texture("MAGIC_JELLYBEAN")

    @HandleEvent(onlyOnIsland = GARDEN)
    private fun onCheckRender(event: CheckRenderEntityEvent<ArmorStand>) {
        if (!isEnabled()) return

        val head = event.entity.getStandHelmet()?.getSkullTexture() ?: return
        if (head == jellyBeanSkullTexture) {
            event.cancel()
        }
    }

    private fun isEnabled() = SkyHanniMod.feature.garden.greenhouse.magicJellyBeanHider
}
